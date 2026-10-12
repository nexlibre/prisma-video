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
 public MediaPlayer player;
 public final List<VideoItem> queue=new ArrayList<>();
 public int index=0, mode=0, volume=100; // 0 all, 1 one, 2 shuffle
 public boolean prepared=false, audioOnly=false, boostAvailable=true;
 private LoudnessEnhancer enhancer;
 private MediaSession session;
 private AudioManager audio;
 private AudioFocusRequest focus;
 private boolean resumeAfterFocus=false;
 private final Random random=new Random();
 private Surface surface;
 private int generation=0;
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
   if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){resumeAfterFocus=playing();pause();}
   else if(change==AudioManager.AUDIOFOCUS_LOSS){resumeAfterFocus=false;pause();}
   else if(change==AudioManager.AUDIOFOCUS_GAIN){applyVolume();if(resumeAfterFocus){resumeAfterFocus=false;play();}}
   else if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK&&prepared)player.setVolume(.2f,.2f);
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
 public int position(){return prepared?player.getCurrentPosition():0;}
 public int duration(){return prepared?player.getDuration():0;}
 public void setQueue(List<VideoItem> items,int selected){savePosition();queue.clear();queue.addAll(items);index=selected;open();}
 public void setSurface(Surface value){surface=value;if(player!=null)player.setSurface(audioOnly?null:surface);}
 public void clearSurface(Surface value){if(surface==value)setSurface(null);}
 public void setAudioOnly(boolean value){audioOnly=value;setSurface(surface);changed();}
 private void releasePlayer(){savePosition();prepared=false;generation++;if(enhancer!=null){enhancer.release();enhancer=null;}if(player!=null){player.release();player=null;}}
 private void open(){
  releasePlayer();VideoItem item=current();loadedItem=item;loopA=loopB=-1;if(item==null)return;recordHistory(item);
  final int token=generation;
  player=new MediaPlayer();player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build());
  player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
  try {
   player.setDataSource(this,item.uri);player.setSurface(audioOnly?null:surface);
   player.setOnPreparedListener(p->{if(token!=generation)return;prepared=true;
    try{enhancer=new LoudnessEnhancer(p.getAudioSessionId());boostAvailable=true;}catch(RuntimeException e){boostAvailable=false;volume=Math.min(volume,100);}
    applyVolume();applySpeed();long resume=settings.getBoolean("resume",true)?history.getLong("position:"+item.uri,0):0;if(resume>0&&resume<duration()-2000)seek(resume);play();changed();
   });
   player.setOnCompletionListener(p->{history.edit().remove("position:"+item.uri).apply();if(sleepAtEnd){sleepAtEnd=false;pause();}else if(mode==1){seek(0);play();}else if(settings.getBoolean("autoplay",true))next();else pause();});
   player.setOnErrorListener((p,w,e)->{releasePlayer();audio.abandonAudioFocusRequest(focus);changed();if(listener!=null)listener.error("No se pudo reproducir este archivo. Puede estar dañado o usar un códec no compatible.");return true;});
   player.prepareAsync();changed();
  }catch(Exception e){releasePlayer();changed();if(listener!=null)listener.error("No se puede abrir el vídeo. Comprueba el acceso al archivo.");}
 }
 public void play(){if(!prepared)return;if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED)return;player.start();changed();}
 public void pause(){if(prepared)player.pause();savePosition();changed();}
 public void toggle(){if(playing())pause();else play();}
 public void seek(long ms){if(prepared)player.seekTo(Math.max(0,Math.min(ms,duration())),MediaPlayer.SEEK_CLOSEST);}
 public void next(){if(queue.isEmpty())return;index=mode==2&&queue.size()>1?(index+1+random.nextInt(queue.size()-1))%queue.size():(index+1)%queue.size();open();}
 public void previous(){if(queue.isEmpty())return;index=(index+queue.size()-1)%queue.size();open();}
 public void setMode(int m){mode=m;settings.edit().putInt("mode",m).apply();changed();}
 public void setVolume(int percent){volume=Math.max(0,Math.min(percent,boostAvailable?200:100));settings.edit().putInt("volume",volume).apply();applyVolume();changed();}
 private void applyVolume(){if(player==null||!prepared)return;
  float level=Math.min(volume,100)/100f;player.setVolume(level,level);
  if(enhancer!=null)try{enhancer.setTargetGain(volume>100?(int)Math.round(2000*Math.log10(volume/100d)):0);enhancer.setEnabled(volume>100);}catch(RuntimeException e){boostAvailable=false;volume=Math.min(volume,100);enhancer.release();enhancer=null;if(listener!=null)listener.error("Este dispositivo no admite amplificación. Se mantiene el volumen hasta 100 %.");}
 }
 public boolean setSpeed(float value){float old=speed;speed=Math.max(.25f,Math.min(3f,value));if(!applySpeed()){speed=old;return false;}settings.edit().putFloat("speed",speed).apply();changed();return true;}
 private boolean applySpeed(){if(!prepared)return true;boolean wasPlaying=playing();try{if(!wasPlaying)player.setVolume(0,0);player.setPlaybackParams(new PlaybackParams().setSpeed(speed).setPitch(1f));if(!wasPlaying){player.pause();applyVolume();}return true;}catch(RuntimeException e){speed=1f;applyVolume();if(listener!=null)listener.error("Este archivo o dispositivo no admite esa velocidad.");return false;}}
 public void setSleep(int minutes,boolean atEnd){clock.removeCallbacks(sleep);sleepAtEnd=atEnd;sleepDeadline=minutes>0?SystemClock.elapsedRealtime()+minutes*60000L:0;if(minutes>0)clock.postDelayed(sleep,minutes*60000L);changed();}
 public void markA(){loopA=position();loopB=-1;changed();}
 public boolean markB(){long position=position();if(loopA<0||position-loopA<500)return false;loopB=position;changed();return true;}
 public void clearLoop(){loopA=loopB=-1;changed();}
 public void savePosition(){if(loadedItem==null||!prepared)return;int position=position();if(position>=duration()-2000)position=0;history.edit().putLong("position:"+loadedItem.uri,position).apply();}
 private void recordHistory(VideoItem item){try{JSONArray old=new JSONArray(history.getString("items","[]")),next=new JSONArray();JSONObject row=new JSONObject();row.put("uri",item.uri.toString());row.put("title",item.title);row.put("folder",item.folder);row.put("duration",item.duration);next.put(row);for(int i=0;i<old.length()&&next.length()<30;i++){JSONObject v=old.getJSONObject(i);if(!v.getString("uri").equals(item.uri.toString()))next.put(v);}history.edit().putString("items",next.toString()).apply();}catch(JSONException e){history.edit().putString("items","[]").apply();}}
 public List<VideoItem> recent(){List<VideoItem> result=new ArrayList<>();try{JSONArray items=new JSONArray(history.getString("items","[]"));for(int i=0;i<items.length();i++){JSONObject v=items.getJSONObject(i);result.add(new VideoItem(android.net.Uri.parse(v.getString("uri")),v.getString("title"),v.optString("folder"),v.optLong("duration")));}}catch(JSONException e){}return result;}
 public void clearHistory(){history.edit().clear().apply();loadedItem=null;}
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
 public void stopPlayback(){setSleep(0,false);releasePlayer();queue.clear();audio.abandonAudioFocusRequest(focus);stopForeground(STOP_FOREGROUND_REMOVE);if(listener!=null)listener.changed();}
 @Override public void onDestroy(){clock.removeCallbacksAndMessages(null);releasePlayer();audio.abandonAudioFocusRequest(focus);unregisterReceiver(noisy);session.release();super.onDestroy();}
}
