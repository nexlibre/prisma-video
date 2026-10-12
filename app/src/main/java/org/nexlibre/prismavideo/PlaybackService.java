package org.nexlibre.prismavideo;

import android.app.*;
import android.content.*;
import android.media.*;
import android.media.audiofx.LoudnessEnhancer;
import android.media.session.*;
import android.os.*;
import android.view.Surface;
import java.util.*;

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
 private final BroadcastReceiver noisy=new BroadcastReceiver(){ public void onReceive(Context c,Intent i){ pause(); } };
 @Override public void onCreate() {
  super.onCreate(); audio=(AudioManager)getSystemService(AUDIO_SERVICE);
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
 public void setQueue(List<VideoItem> items,int selected){queue.clear();queue.addAll(items);index=selected;open();}
 public void setSurface(Surface value){surface=value;if(player!=null)player.setSurface(audioOnly?null:surface);}
 public void setAudioOnly(boolean value){audioOnly=value;setSurface(surface);changed();}
 private void releasePlayer(){prepared=false;generation++;if(enhancer!=null){enhancer.release();enhancer=null;}if(player!=null){player.release();player=null;}}
 private void open(){
  releasePlayer();VideoItem item=current();if(item==null)return;
  final int token=generation;
  player=new MediaPlayer();player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build());
  player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
  try {
   player.setDataSource(this,item.uri);player.setSurface(audioOnly?null:surface);
   player.setOnPreparedListener(p->{if(token!=generation)return;prepared=true;
    try{enhancer=new LoudnessEnhancer(p.getAudioSessionId());boostAvailable=true;}catch(RuntimeException e){boostAvailable=false;volume=Math.min(volume,100);}
    applyVolume();play();changed();
   });
   player.setOnCompletionListener(p->{if(mode==1){seek(0);play();}else next();});
   player.setOnErrorListener((p,w,e)->{releasePlayer();audio.abandonAudioFocusRequest(focus);changed();if(listener!=null)listener.error("No se pudo reproducir este archivo. Puede estar dañado o usar un códec no compatible.");return true;});
   player.prepareAsync();changed();
  }catch(Exception e){releasePlayer();changed();if(listener!=null)listener.error("No se puede abrir el vídeo. Comprueba el acceso al archivo.");}
 }
 public void play(){if(!prepared)return;if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED)return;player.start();changed();}
 public void pause(){if(prepared)player.pause();changed();}
 public void toggle(){if(playing())pause();else play();}
 public void seek(long ms){if(prepared)player.seekTo((int)Math.max(0,Math.min(ms,duration())));}
 public void next(){if(queue.isEmpty())return;index=mode==2&&queue.size()>1?(index+1+random.nextInt(queue.size()-1))%queue.size():(index+1)%queue.size();open();}
 public void previous(){if(queue.isEmpty())return;index=(index+queue.size()-1)%queue.size();open();}
 public void setMode(int m){mode=m;changed();}
 public void setVolume(int percent){volume=Math.max(0,Math.min(percent,boostAvailable?200:100));applyVolume();changed();}
 private void applyVolume(){if(player==null||!prepared)return;
  float level=Math.min(volume,100)/100f;player.setVolume(level,level);
  if(enhancer!=null)try{enhancer.setTargetGain(volume>100?(int)Math.round(2000*Math.log10(volume/100d)):0);enhancer.setEnabled(volume>100);}catch(RuntimeException e){boostAvailable=false;volume=Math.min(volume,100);enhancer.release();enhancer=null;if(listener!=null)listener.error("Este dispositivo no admite amplificación. Se mantiene el volumen hasta 100 %.");}
 }
 private void changed(){notification();if(listener!=null)listener.changed();}
 private PendingIntent command(String action,int id){return PendingIntent.getService(this,id,new Intent(this,PlaybackService.class).setAction(action),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);}
 private void notification(){
  if(session==null||current()==null)return;
  VideoItem item=current();
  session.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS|PlaybackState.ACTION_SEEK_TO|PlaybackState.ACTION_STOP).setState(playing()?PlaybackState.STATE_PLAYING:PlaybackState.STATE_PAUSED,position(),playing()?1:0).build());
  session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,item==null?"Prisma Video":item.title).putLong(MediaMetadata.METADATA_KEY_DURATION,duration()).build());
  Notification n=new Notification.Builder(this,"playback").setSmallIcon(org.nexlibre.prismavideo.R.drawable.ic_app).setContentTitle(item==null?"Prisma Video":item.title).setContentText(audioOnly?"Solo audio":"Vídeo local").setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).setOngoing(playing()).setOnlyAlertOnce(true)
   .addAction(new Notification.Action.Builder(null,playing()?"Pausar":"Reproducir",command("toggle",1)).build()).addAction(new Notification.Action.Builder(null,"Siguiente",command("next",2)).build()).addAction(new Notification.Action.Builder(null,"Cerrar",command("stop",3)).build()).setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0,1,2)).build();
  startForeground(9,n);
 }
 public void stopPlayback(){releasePlayer();queue.clear();audio.abandonAudioFocusRequest(focus);stopForeground(STOP_FOREGROUND_REMOVE);if(listener!=null)listener.changed();}
 @Override public void onDestroy(){releasePlayer();audio.abandonAudioFocusRequest(focus);unregisterReceiver(noisy);session.release();super.onDestroy();}
}
