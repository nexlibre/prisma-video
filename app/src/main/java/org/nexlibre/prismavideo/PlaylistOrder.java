package org.nexlibre.prismavideo;

import java.util.List;

/** Operations preserve user order and never duplicate a video. */
public final class PlaylistOrder {
 private PlaylistOrder() {}
 public static boolean add(List<String> items, String uri) {
  if (uri == null || uri.isEmpty() || items.contains(uri)) return false;
  return items.add(uri);
 }
 public static void move(List<String> items, String uri, int position) {
  if (!items.remove(uri)) return;
  items.add(Math.max(0, Math.min(position, items.size())), uri);
 }
}
