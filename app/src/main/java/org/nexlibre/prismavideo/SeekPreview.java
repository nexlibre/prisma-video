package org.nexlibre.prismavideo;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** At most one pending preview; stale work cannot overwrite the latest position. */
public final class SeekPreview implements AutoCloseable {
 private final Context context;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final AtomicInteger generation=new AtomicInteger();
 private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1),new ThreadPoolExecutor.DiscardOldestPolicy());
 private volatile boolean closed;
 public SeekPreview(Context context){this.context=context.getApplicationContext();}
 public void request(VideoItem item,long position,ImageView view){if(closed)return;int token=generation.incrementAndGet();worker.execute(()->{if(closed||token!=generation.get())return;Bitmap bitmap=null;MediaMetadataRetriever retriever=new MediaMetadataRetriever();try{retriever.setDataSource(context,item.uri);if(Build.VERSION.SDK_INT>=27)bitmap=retriever.getScaledFrameAtTime(Math.max(0,position)*1000,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,240,135);else{Bitmap full=retriever.getFrameAtTime(Math.max(0,position)*1000,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);if(full!=null){bitmap=Bitmap.createScaledBitmap(full,240,135,true);if(full!=bitmap)full.recycle();}}}catch(Exception e){}finally{try{retriever.release();}catch(Exception e){}}final Bitmap frame=bitmap;main.post(()->{if(!closed&&token==generation.get()&&frame!=null)view.setImageBitmap(frame);});});}
 public void cancel(){generation.incrementAndGet();worker.getQueue().clear();}
 public void close(){closed=true;cancel();worker.shutdownNow();main.removeCallbacksAndMessages(null);}
}
