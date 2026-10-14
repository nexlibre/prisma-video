package org.nexlibre.prismavideo;

import android.app.*;
import android.content.*;
import android.media.*;
import android.media.audiofx.LoudnessEnhancer;
import android.media.session.*;
import android.os.*;
import android.view.Surface;
import java.util.*;
import org.json.*;

public class PlaybackService extends Service {
 public interface Listener { void changed(); void error(String message); }
 public final class LocalBinder extends Binder { public PlaybackService get() { return PlaybackService.this; } }
 private final LocalBinder binder=new LocalBinder();
 public Listener listener;
 private PlaybackEngine player;
 public final List<VideoItem> queue=new ArrayList<>();
 public int index=0, mode=0, volume=100; // 0 all, 1 one, 2 shuffle
 public boolean prepared=false, audioOnly=false, boostAvailable=true;
 private LoudnessEnhancer enhancer;
 private android.media.audiofx.Equalizer equalizer;
 private MediaSession session;
 private AudioManager audio;
 private AudioFocusRequest focus;
  private final ShuffleOrder shuffle=new ShuffleOrder(new Random());
 private Surface surface;
 private int generation=0;
 private boolean playRequested=false;
 public float speed=1f;
 public long loopA=-1,loopB=-1,sleepDeadline=0;
 public boolean sleepAtEnd=false;
 private VideoItem loadedItem;
 private android.content.SharedPreferences settings,history;
 private final Handler clock=new Handler(Looper.getMainLooper());
 private int checkpoint=0;
 private final Runnable pulse=new Runnable(){public void run(){
  if(prepared){if(playing()&&loopA>=0&&loopB>loopA&&position()>=loopB)seek(loopA);if(++checkpoint>=25){checkpoint=0;savePosition();}}
  clock.postDelayed(this,200);
 }};
 private final Runnable sleep=()->{sleepDeadline=0;sleepAtEnd=false;pause();if(listener!=null)listener.error("Temporizador: reproducción pausada.");};
 private final BroadcastReceiver noisy=new BroadcastReceiver(){ public void onReceive(Context c,Intent i){ pause(); } };
 @Override public void onCreate() {
  super.onCreate();settings=getSharedPreferences("settings",MODE_PRIVATE);history=getSharedPreferences("history",MODE_PRIVATE);speed=settings.getFloat("speed",1f);mode=settings.getInt("mode",0);volume=settings.getInt("volume",100);clock.post(pulse);audio=(AudioManager)getSystemService(AUDIO_SERVICE);
  focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build()).setOnAudioFocusChangeListener(change->{
   if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT||change==AudioManager.AUDIOFOCUS_LOSS||change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){pause();}
   else if(change==AudioManager.AUDIOFOCUS_GAIN)applyVolume();
  }).build();
  if(Build.VERSION.SDK_INT>=33)registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),Context.RECEIVER_NOT_EXPORTED);else registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
  ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("playback","Reproducción",NotificationManager.IMPORTANCE_LOW));
  session=new MediaSession(this,"PrismaVideo");
  session.setCallback(new MediaSession.Callback(){
   public void onPlay(){play();} public void onPause(){pause();} public void onSkipToNext(){next();} public void onSkipToPrevious(){previous();} public void onSeekTo(long pos){seek(pos);} public void onStop(){stopPlayback();}
  }); session.setActive(true);
 }
 @Override public IBinder onBind(Intent intent){return binder;}
 @Override public int onStartCommand(Intent intent,int flags,int id){
  if(current()==null){stopSelf();return START_NOT_STICKY;}
  notification();
  if(intent!=null){String action=intent.getAction(); if("toggle".equals(action))toggle(); else if("next".equals(action))next(); else if("stop".equals(action)){stopPlayback();stopSelf();}}
  return START_NOT_STICKY;
 }
 public VideoItem current(){return queue.isEmpty()?null:queue.get(index);}
 public boolean playing(){return prepared&&player!=null&&player.isPlaying();}
 public int position(){return prepared?player.position():0;}
 public int duration(){return prepared?player.duration():0;}
 public void setQueue(List<VideoItem> items,int selected){savePosition();if(items.isEmpty()||selected<0||selected>=items.size())return;queue.clear();queue.addAll(items);shuffle.reset();index=selected;open();}
 public void setSurface(Surface value){surface=value;if(player!=null)player.setSurface(audioOnly?null:surface);}
 public void clearSurface(Surface value){if(surface==value)setSurface(null);}
 public void setAudioOnly(boolean value){audioOnly=value;if(player!=null)player.setAudioOnly(value);setSurface(surface);changed();}
 private void releasePlayer(){savePosition();prepared=false;generation++;if(enhancer!=null){enhancer.release();enhancer=null;}if(equalizer!=null){equalizer.release();equalizer=null;}if(player!=null){player.release();player=null;}}
 private void open(){
  releasePlayer();playRequested=true;VideoItem item=current();loadedItem=item;if(item!=null)speed=history.getFloat("speed:"+item.uri,settings.getFloat("speed",1f));loopA=loopB=-1;if(item==null)return;recordHistory(item);
  final int token=generation;
  player=new PlaybackEngine(this,new PlaybackEngine.Events(){
   public void ready(){if(token!=generation)return;prepared=true;
    try{enhancer=new LoudnessEnhancer(player.audioSessionId());boostAvailable=true;}catch(RuntimeException e){boostAvailable=false;volume=Math.min(volume,100);}
    restoreTracks();selectEqualizerPreset(settings.getInt("equalizerPreset",-1));applyVolume();applySpeed();long resume=settings.getBoolean("resume",true)?history.getLong("position:"+item.uri,0):0;if(resume>0&&resume<duration()-2000)seek(resume);if(playRequested)play();changed();
   }
   public void ended(){if(token!=generation)return;history.edit().remove("position:"+item.uri).apply();if(sleepAtEnd){sleepAtEnd=false;pause();}else if(mode==1){seek(0);play();}else if(settings.getBoolean("autoplay",true))next();else pause();}
   public void failed(String message){if(token!=generation)return;playRequested=false;releasePlayer();audio.abandonAudioFocusRequest(focus);changed();if(listener!=null)listener.error(message);}
   public void changed(){if(token==generation)PlaybackService.this.changed();}
  });
  try{player.setAudioOnly(audioOnly);player.open(item.uri,audioOnly?null:surface);changed();}
  catch(RuntimeException e){playRequested=false;releasePlayer();changed();if(listener!=null)listener.error("No se puede abrir el vídeo. Comprueba el acceso al archivo.");}
 }
 public int videoWidth(){return player==null?0:player.videoWidth();}
 public int videoHeight(){return player==null?0:player.videoHeight();}
 public List<PlaybackEngine.AudioTrack> audioTracks(){return player==null?Collections.emptyList():player.audioTracks();}
 public void selectAudioTrack(PlaybackEngine.AudioTrack track){if(player!=null){List<PlaybackEngine.AudioTrack> tracks=player.audioTracks();for(int i=0;i<tracks.size();i++)if(tracks.get(i).label.equals(track.label)){history.edit().putInt("audioTrack:"+current().uri,i).apply();break;}player.selectAudioTrack(track);}}
 public List<PlaybackEngine.AudioTrack> subtitleTracks(){return player==null?Collections.emptyList():player.subtitleTracks();}
 public List<androidx.media3.common.text.Cue> cues(){return player==null?Collections.emptyList():player.cues();}
 public void selectSubtitleTrack(PlaybackEngine.AudioTrack track,int index){if(player!=null){player.selectSubtitleTrack(track);history.edit().putInt("textTrack:"+current().uri,index).apply();changed();}}
 public void disableSubtitles(){if(player!=null){player.disableSubtitles();history.edit().putInt("textTrack:"+current().uri,-1).apply();changed();}}
 private void restoreTracks(){List<PlaybackEngine.AudioTrack> tracks=player.audioTracks();int audioIndex=history.getInt("audioTrack:"+current().uri,-1);if(audioIndex>=0&&audioIndex<tracks.size())player.selectAudioTrack(tracks.get(audioIndex));tracks=player.subtitleTracks();int textIndex=history.getInt("textTrack:"+current().uri,-1);if(textIndex>=0&&textIndex<tracks.size())player.selectSubtitleTrack(tracks.get(textIndex));}


 public void play(){playRequested=true;if(!prepared){if(player==null&&current()!=null)open();return;}if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){playRequested=false;return;}player.play();changed();}
 public void pause(){playRequested=false;if(player!=null)player.pause();savePosition();changed();}
 public void toggle(){if(playRequested)pause();else play();}
 public void seek(long ms){if(prepared)player.seek(Math.max(0,Math.min(ms,duration())));}
 public void moveQueue(int from,int to){if(from<0||from>=queue.size()||to<0||to>=queue.size())return;VideoItem active=current();VideoItem moved=queue.remove(from);queue.add(to,moved);index=queue.indexOf(active);shuffle.reset();changed();}
 public void removeQueue(int target){if(target<0||target>=queue.size())return;if(queue.size()==1){stopPlayback();return;}boolean active=target==index;queue.remove(target);if(target<index)index--;if(index>=queue.size())index=0;shuffle.reset();if(active)open();else changed();}
 public List<String> equalizerPresets(){List<String> result=new ArrayList<>();if(player==null||!prepared)return result;try{if(equalizer==null)equalizer=new android.media.audiofx.Equalizer(0,player.audioSessionId());for(short i=0;i<equalizer.getNumberOfPresets();i++)result.add(equalizer.getPresetName(i));}catch(RuntimeException e){if(equalizer!=null){equalizer.release();equalizer=null;}}return result;}
 public boolean selectEqualizerPreset(int preset){try{if(preset<0){if(equalizer!=null)equalizer.setEnabled(false);}else{equalizerPresets();if(equalizer==null||preset>=equalizer.getNumberOfPresets())return false;equalizer.usePreset((short)preset);equalizer.setEnabled(true);}settings.edit().putInt("equalizerPreset",preset).apply();return true;}catch(RuntimeException e){return false;}}
 public void next(){if(queue.isEmpty())return;index=mode==2&&queue.size()>1?shuffle.next(queue.size(),index):(index+1)%queue.size();open();}
 public void previous(){if(queue.isEmpty())return;index=(index+queue.size()-1)%queue.size();open();}
 public void setMode(int m){mode=m;shuffle.reset();settings.edit().putInt("mode",m).apply();changed();}
 public void setVolume(int percent){volume=Math.max(0,Math.min(percent,boostAvailable?200:100));settings.edit().putInt("volume",volume).apply();applyVolume();changed();}
 private void applyVolume(){if(player==null||!prepared)return;
  float level=Math.min(volume,100)/100f;player.setVolume(level);
  if(enhancer!=null)try{enhancer.setTargetGain(volume>100?(int)Math.round(2000*Math.log10(volume/100d)):0);enhancer.setEnabled(volume>100);}catch(RuntimeException e){boostAvailable=false;volume=Math.min(volume,100);enhancer.release();enhancer=null;if(listener!=null)listener.error("Este dispositivo no admite amplificación. Se mantiene el volumen hasta 100 %.");}
 }
 public boolean setSpeed(float value){float old=speed;speed=Math.max(.25f,Math.min(3f,value));if(!applySpeed()){speed=old;return false;}settings.edit().putFloat("speed",speed).apply();if(current()!=null)history.edit().putFloat("speed:"+current().uri,speed).apply();changed();return true;}
 private boolean applySpeed(){if(player==null)return true;try{player.setSpeed(speed);return true;}catch(RuntimeException e){if(listener!=null)listener.error("Este archivo o dispositivo no admite esa velocidad.");return false;}}
 public void setSleep(int minutes,boolean atEnd){clock.removeCallbacks(sleep);sleepAtEnd=atEnd;sleepDeadline=minutes>0?SystemClock.elapsedRealtime()+minutes*60000L:0;if(minutes>0)clock.postDelayed(sleep,minutes*60000L);changed();}
 public void markA(){loopA=position();loopB=-1;changed();}
 public boolean markB(){long position=position();if(loopA<0||position-loopA<500)return false;loopB=position;changed();return true;}
 public void clearLoop(){loopA=loopB=-1;changed();}
 public void savePosition(){if(loadedItem==null||!prepared)return;int position=position();if(duration()>0&&position>=duration()-2000)position=0;history.edit().putLong("position:"+loadedItem.uri,position).apply();}
 private void recordHistory(VideoItem item){try{JSONArray old=new JSONArray(history.getString("items","[]")),next=new JSONArray();JSONObject row=new JSONObject();row.put("uri",item.uri.toString());row.put("title",item.title);row.put("folder",item.folder);row.put("duration",item.duration);next.put(row);for(int i=0;i<old.length()&&next.length()<30;i++){JSONObject v=old.getJSONObject(i);if(!v.getString("uri").equals(item.uri.toString()))next.put(v);}history.edit().putString("items",next.toString()).apply();}catch(JSONException e){history.edit().putString("items","[]").apply();}}
 public List<VideoItem> recent(){List<VideoItem> result=new ArrayList<>();try{JSONArray items=new JSONArray(history.getString("items","[]"));for(int i=0;i<items.length();i++){JSONObject v=items.getJSONObject(i);result.add(new VideoItem(android.net.Uri.parse(v.getString("uri")),v.getString("title"),v.optString("folder"),v.optLong("duration")));}}catch(JSONException e){}return result;}
 public void renameVideo(android.net.Uri uri,String title){for(int i=0;i<queue.size();i++){VideoItem v=queue.get(i);if(v.uri.equals(uri))queue.set(i,new VideoItem(uri,title,v.folder,v.duration,v.size,v.width,v.height,v.dateAdded,v.mimeType));}try{JSONArray items=new JSONArray(history.getString("items","[]"));for(int i=0;i<items.length();i++)if(items.getJSONObject(i).getString("uri").equals(uri.toString()))items.getJSONObject(i).put("title",title);history.edit().putString("items",items.toString()).apply();}catch(JSONException e){}changed();}
 public void forgetVideo(android.net.Uri uri){VideoItem active=current();if(active!=null&&active.uri.equals(uri))stopPlayback();else{queue.removeIf(v->v.uri.equals(uri));shuffle.reset();if(active!=null)index=queue.indexOf(active);}try{JSONArray old=new JSONArray(history.getString("items","[]")),next=new JSONArray();for(int i=0;i<old.length();i++)if(!uri.toString().equals(old.getJSONObject(i).getString("uri")))next.put(old.getJSONObject(i));history.edit().putString("items",next.toString()).remove("position:"+uri).remove("speed:"+uri).remove("audioTrack:"+uri).remove("textTrack:"+uri).apply();}catch(JSONException e){history.edit().remove("position:"+uri).remove("speed:"+uri).remove("audioTrack:"+uri).remove("textTrack:"+uri).apply();}}
 public void clearHistory(){android.content.SharedPreferences.Editor editor=history.edit().remove("items");for(String key:history.getAll().keySet())if(key.startsWith("position:"))editor.remove(key);editor.apply();loadedItem=null;}
 private void changed(){notification();if(listener!=null)listener.changed();}
 private PendingIntent command(String action,int id){return PendingIntent.getService(this,id,new Intent(this,PlaybackService.class).setAction(action),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);}
 private void notification(){
  if(session==null||current()==null)return;
  VideoItem item=current();
  session.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS|PlaybackState.ACTION_SEEK_TO|PlaybackState.ACTION_STOP).setState(playing()?PlaybackState.STATE_PLAYING:PlaybackState.STATE_PAUSED,position(),playing()?speed:0).build());
  session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,item==null?"Prisma Video":item.title).putLong(MediaMetadata.METADATA_KEY_DURATION,duration()).build());
  Notification n=new Notification.Builder(this,"playback").setSmallIcon(org.nexlibre.prismavideo.R.drawable.ic_app).setContentTitle(item==null?"Prisma Video":item.title).setContentText(audioOnly?"Solo audio":"Vídeo local").setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).setOngoing(playing()).setOnlyAlertOnce(true)
   .addAction(new Notification.Action.Builder(null,playing()?"Pausar":"Reproducir",command("toggle",1)).build()).addAction(new Notification.Action.Builder(null,"Siguiente",command("next",2)).build()).addAction(new Notification.Action.Builder(null,"Cerrar",command("stop",3)).build()).setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0,1,2)).build();
  startForeground(9,n);
 }
 public void stopPlayback(){playRequested=false;setSleep(0,false);releasePlayer();queue.clear();audio.abandonAudioFocusRequest(focus);stopForeground(STOP_FOREGROUND_REMOVE);if(listener!=null)listener.changed();}
 @Override public void onDestroy(){clock.removeCallbacksAndMessages(null);releasePlayer();audio.abandonAudioFocusRequest(focus);unregisterReceiver(noisy);session.release();super.onDestroy();}
}
