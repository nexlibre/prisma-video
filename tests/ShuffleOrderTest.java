import org.nexlibre.prismavideo.ShuffleOrder;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ShuffleOrderTest {
 public static void main(String[] args){
  ShuffleOrder order=new ShuffleOrder(new Random(42));
  for(int count:new int[]{2,3,100,10000}){
   order.reset();int current=0;Set<Integer> visited=new HashSet<>();visited.add(current);
   for(int i=1;i<count;i++){int next=order.next(count,current);if(next==current||!visited.add(next))throw new AssertionError("Repeated before cycle ended");current=next;}
   int next=order.next(count,current);if(next==current||next<0||next>=count)throw new AssertionError("Invalid cycle transition");
  }
  if(order.next(1,0)!=0)throw new AssertionError("Singleton");
  order.reset();if(order.next(2,0)!=1||order.next(2,1)!=0)throw new AssertionError("Two entries");
  System.out.println("ShuffleOrderTest: passed");
 }
}
