package org.nexlibre.prismavideo;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.LruCache;
import android.util.Size;
import android.widget.ImageView;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Bounded queue and byte-sized cache; recycled views never receive stale thumbnails. */
public final class ThumbnailLoader implements AutoCloseable {
 private final ContentResolver resolver;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ThreadPoolExecutor workers=new ThreadPoolExecutor(2,2,10,TimeUnit.SECONDS,new ArrayBlockingQueue<>(64),new ThreadPoolExecutor.DiscardPolicy());
 private final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(8*1024*1024){protected int sizeOf(String key,Bitmap value){return value.getAllocationByteCount();}};
 private volatile boolean closed;
 public ThumbnailLoader(ContentResolver resolver){this.resolver=resolver;}
 public void load(VideoItem item,ImageView view){
  String key=item.uri.toString();view.setTag(key);view.setImageDrawable(null);Bitmap existing=cache.get(key);
  if(existing!=null){view.setImageBitmap(existing);return;}if(closed)return;
  workers.execute(()->{Bitmap bitmap=null;try{
   if(Build.VERSION.SDK_INT>=29)bitmap=resolver.loadThumbnail(item.uri,new Size(256,144),null);
   else bitmap=MediaStore.Video.Thumbnails.getThumbnail(resolver,ContentUris.parseId(item.uri),MediaStore.Video.Thumbnails.MINI_KIND,null);
  }catch(Exception e){}final Bitmap loaded=bitmap;if(loaded!=null&&!closed){cache.put(key,loaded);main.post(()->{if(!closed&&key.equals(view.getTag()))view.setImageBitmap(loaded);});}});
 }
 public void invalidate(){cache.evictAll();}
 public void close(){closed=true;workers.shutdownNow();main.removeCallbacksAndMessages(null);cache.evictAll();}
}
