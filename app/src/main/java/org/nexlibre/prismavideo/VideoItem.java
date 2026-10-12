package org.nexlibre.prismavideo;
import android.net.Uri;
public final class VideoItem {
 public final Uri uri; public final String title, folder; public final long duration;
 public VideoItem(Uri uri, String title, String folder, long duration) { this.uri=uri; this.title=title; this.folder=folder; this.duration=duration; }
}
