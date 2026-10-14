package org.nexlibre.prismavideo;
import android.net.Uri;
public final class VideoItem {
 public final Uri uri;public final String title,folder,mimeType;public final long duration,size,dateAdded;public final int width,height;
 public VideoItem(Uri uri,String title,String folder,long duration){this(uri,title,folder,duration,0,0,0,0,null);}
 public VideoItem(Uri uri,String title,String folder,long duration,long size,int width,int height,long dateAdded,String mimeType){this.uri=uri;this.title=title==null?"Vídeo":title;this.folder=folder;this.duration=duration;this.size=size;this.width=width;this.height=height;this.dateAdded=dateAdded;this.mimeType=mimeType;}
}
