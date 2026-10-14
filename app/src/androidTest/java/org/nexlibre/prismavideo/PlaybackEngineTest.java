package org.nexlibre.prismavideo;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Smoke tests of local demux, audio decoding and lifecycle; not visual/HDR certification. */
@RunWith(AndroidJUnit4.class)
public class PlaybackEngineTest {
 private void main(Runnable task){InstrumentationRegistry.getInstrumentation().runOnMainSync(task);}
 @Test public void localMp4LoadsSeeksPlaysAndPauses() throws Exception {
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  File file=new File(context.getCacheDir(),"prisma-smoke.mp4");
  try(InputStream source=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("smoke.mp4.b64");ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
   byte[] buffer=new byte[4096];int count;while((count=source.read(buffer))!=-1)bytes.write(buffer,0,count);
   try(FileOutputStream output=new FileOutputStream(file)){output.write(Base64.decode(bytes.toByteArray(),Base64.DEFAULT));}
  }
  CountDownLatch ready=new CountDownLatch(1),ended=new CountDownLatch(1);
  AtomicReference<String> error=new AtomicReference<>();AtomicReference<PlaybackEngine> engine=new AtomicReference<>();
  try{
   main(()->{engine.set(new PlaybackEngine(context,new PlaybackEngine.Events(){
    public void ready(){ready.countDown();}public void ended(){ended.countDown();}
    public void failed(String message){error.set(message);ready.countDown();ended.countDown();}public void changed(){}
   }));engine.get().open(Uri.fromFile(file),null);});
   assertTrue("Preparation timed out",ready.await(20,TimeUnit.SECONDS));assertNull(error.get());
   main(()->{assertTrue(engine.get().duration()>=1800);assertFalse(engine.get().audioTracks().isEmpty());
    engine.get().selectAudioTrack(engine.get().audioTracks().get(0));assertFalse(engine.get().subtitleTracks().isEmpty());engine.get().selectSubtitleTrack(engine.get().subtitleTracks().get(0));engine.get().seek(500);engine.get().setSpeed(1.25f);engine.get().play();});
   Thread.sleep(500);
   main(()->{assertTrue(engine.get().position()>500);assertFalse(engine.get().cues().isEmpty());assertTrue(engine.get().cues().get(0).text.toString().contains("Prisma"));engine.get().pause();assertFalse(engine.get().isPlaying());engine.get().seek(1000);engine.get().play();});
   assertTrue("Completion timed out",ended.await(20,TimeUnit.SECONDS));assertNull(error.get());
  }finally{main(()->{if(engine.get()!=null)engine.get().release();});file.delete();}
 }
 @Test public void inaccessibleFileReportsError() throws Exception {
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  CountDownLatch failed=new CountDownLatch(1);AtomicReference<PlaybackEngine> engine=new AtomicReference<>();AtomicReference<String> error=new AtomicReference<>();
  try{
   main(()->{engine.set(new PlaybackEngine(context,new PlaybackEngine.Events(){
    public void ready(){}public void ended(){}public void changed(){}public void failed(String message){error.set(message);failed.countDown();}
   }));engine.get().open(Uri.fromFile(new File(context.getCacheDir(),"missing-prisma-test.mp4")),null);});
   assertTrue("No playback error reported",failed.await(20,TimeUnit.SECONDS));assertNotNull(error.get());
  }finally{main(()->{if(engine.get()!=null)engine.get().release();});}
 }
}
