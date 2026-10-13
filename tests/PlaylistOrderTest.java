import java.util.*;
import org.nexlibre.prismavideo.PlaylistOrder;

public class PlaylistOrderTest {
 public static void main(String[] args) {
  List<String> videos=new ArrayList<>(Arrays.asList("a","b","c"));
  if(PlaylistOrder.add(videos,"b") || PlaylistOrder.add(videos,"") || PlaylistOrder.add(videos,null)) throw new AssertionError("Duplicates/invalid URI");
  PlaylistOrder.move(videos,"a",2);
  check(videos,"b","c","a");
  PlaylistOrder.move(videos,"a",0);
  check(videos,"a","b","c");
  PlaylistOrder.move(videos,"missing",1);
  check(videos,"a","b","c");
  PlaylistOrder.move(videos,"b",100);
  check(videos,"a","c","b");
  PlaylistOrder.move(videos,"c",-1);
  check(videos,"c","a","b");
  if(!PlaylistOrder.add(videos,"d")) throw new AssertionError("Addition failed");
  check(videos,"c","a","b","d");
  List<String> copy=new ArrayList<>(videos);
  PlaylistOrder.move(copy,"d",0);
  check(videos,"c","a","b","d");
  System.out.println("Playlist order tests passed");
 }
 private static void check(List<String> actual,String... expected) {
  if(!actual.equals(Arrays.asList(expected))) throw new AssertionError(actual.toString());
 }
}
