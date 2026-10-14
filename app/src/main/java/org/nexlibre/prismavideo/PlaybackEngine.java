package org.nexlibre.prismavideo;

import android.content.Context;
import android.net.Uri;
import android.view.Surface;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
import java.util.ArrayList;
import java.util.List;

/** Main-thread-only playback core. Persistence, focus and queue belong to the service. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class PlaybackEngine {
 public interface Events { void ready(); void ended(); void failed(String message); void changed(); }
 public static final class AudioTrack {
  public final String label;
  public final boolean selected;
  private final Tracks.Group group;
  private final int index;
  private AudioTrack(Tracks.Group group,int index,String label){this.group=group;this.index=index;this.label=label;selected=group.isTrackSelected(index);}
 }
 private final ExoPlayer player;
 private List<androidx.media3.common.text.Cue> cues=java.util.Collections.emptyList();
 private boolean ready;
 public PlaybackEngine(Context context,Events events){
  player=new ExoPlayer.Builder(context,new DefaultRenderersFactory(context).setEnableDecoderFallback(true)).build();
  player.setWakeMode(C.WAKE_MODE_LOCAL);
  player.setAudioAttributes(new androidx.media3.common.AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),false);
  player.setSeekParameters(SeekParameters.EXACT);
  player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT,true).build());
  player.addListener(new Player.Listener(){
   @Override public void onPlaybackStateChanged(int state){
    if(state==Player.STATE_READY&&!ready){ready=true;events.ready();}
    else if(state==Player.STATE_ENDED)events.ended();
   }
   @Override public void onCues(androidx.media3.common.text.CueGroup group){cues=group.cues;events.changed();}
   @Override public void onIsPlayingChanged(boolean playing){events.changed();}
   @Override public void onTracksChanged(Tracks tracks){events.changed();}
   @Override public void onVideoSizeChanged(androidx.media3.common.VideoSize size){events.changed();}
   @Override public void onPlayerError(PlaybackException error){events.failed("No se pudo reproducir el archivo ("+error.getErrorCodeName()+"). Comprueba el acceso y la compatibilidad del códec.");}
  });
 }
 public void open(Uri uri,Surface surface){player.setVideoSurface(surface);player.setMediaItem(MediaItem.fromUri(uri));player.prepare();}
 public void setSurface(Surface surface){player.setVideoSurface(surface);}
 public void play(){player.play();}
 public void pause(){player.pause();}
 public boolean isPlaying(){return player.isPlaying();}
 public int position(){return (int)Math.min(Integer.MAX_VALUE,Math.max(0,player.getCurrentPosition()));}
 public int duration(){long duration=player.getDuration();return duration==C.TIME_UNSET?0:(int)Math.min(Integer.MAX_VALUE,Math.max(0,duration));}
 public void seek(long position){player.seekTo(position);}
 public void setSpeed(float speed){player.setPlaybackSpeed(speed);}
 public void setVolume(float volume){player.setVolume(volume);}
 public int audioSessionId(){return player.getAudioSessionId();}
 public int videoWidth(){return player.getVideoSize().width;}
 public int videoHeight(){return player.getVideoSize().height;}
 public List<AudioTrack> audioTracks(){return tracks(C.TRACK_TYPE_AUDIO,"Audio");}
 public List<AudioTrack> subtitleTracks(){return tracks(C.TRACK_TYPE_TEXT,"Subtítulo");}
 private List<AudioTrack> tracks(int type,String prefix){List<AudioTrack> result=new ArrayList<>();
  for(Tracks.Group group:player.getCurrentTracks().getGroups())if(group.getType()==type)
   for(int i=0;i<group.length;i++)if(group.isTrackSupported(i)){Format format=group.getTrackFormat(i);String label=format.label!=null?format.label:format.language!=null?format.language:"Sin idioma";result.add(new AudioTrack(group,i,prefix+" "+(result.size()+1)+" · "+label));}
  return result;
 }
 public List<androidx.media3.common.text.Cue> cues(){return cues;}
 public void disableSubtitles(){cues=java.util.Collections.emptyList();player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT,true).build());}
 public void selectSubtitleTrack(AudioTrack track){selectAudioTrack(track);player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT,false).build());}
 public void setAudioOnly(boolean value){player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_VIDEO,value).build());}

 public void selectAudioTrack(AudioTrack track){
  if(!player.getCurrentTracks().getGroups().contains(track.group))throw new IllegalArgumentException("Stale track");
  player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setOverrideForType(new TrackSelectionOverride(track.group.getMediaTrackGroup(),track.index)).build());
 }
 public void release(){player.release();}
}
