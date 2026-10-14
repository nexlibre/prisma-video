package org.nexlibre.prismavideo;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;
import java.util.ArrayList;
import java.util.List;

/** Read on a worker thread. Never caches unauthorized MediaStore rows. */
public final class LibraryRepository {
 private final ContentResolver resolver;
 public LibraryRepository(ContentResolver resolver){this.resolver=resolver;}
 public List<VideoItem> load(){
  List<VideoItem> result=new ArrayList<>();
  String path=Build.VERSION.SDK_INT>=29?MediaStore.MediaColumns.RELATIVE_PATH:MediaStore.MediaColumns.DATA;
  String[] columns={MediaStore.Video.Media._ID,MediaStore.MediaColumns.DISPLAY_NAME,path,MediaStore.Video.Media.DURATION,MediaStore.MediaColumns.SIZE,MediaStore.Video.Media.WIDTH,MediaStore.Video.Media.HEIGHT,MediaStore.MediaColumns.DATE_ADDED,MediaStore.MediaColumns.MIME_TYPE};
  try(Cursor cursor=resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,columns,null,null,MediaStore.MediaColumns.DATE_ADDED+" DESC")){
   if(cursor!=null)while(cursor.moveToNext()){
    if(Thread.currentThread().isInterrupted())break;
    String folder=cursor.getString(2);if(folder==null)folder="";
    if(Build.VERSION.SDK_INT<29){int end=folder.lastIndexOf('/');folder=end>=0?folder.substring(0,end):"";}
    result.add(new VideoItem(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,cursor.getLong(0)),cursor.getString(1),normalize(folder),cursor.getLong(3),cursor.getLong(4),cursor.getInt(5),cursor.getInt(6),cursor.getLong(7),cursor.getString(8)));
   }
  }
  return result;
 }
 public static String normalize(String path){
  String value=path.trim().replace('\\','/');while(value.startsWith("/"))value=value.substring(1);
  if(value.startsWith("storage/emulated/0/"))value=value.substring(19);
  if(value.startsWith("sdcard/"))value=value.substring(7);
  while(value.endsWith("/"))value=value.substring(0,value.length()-1);return value;
 }
}
