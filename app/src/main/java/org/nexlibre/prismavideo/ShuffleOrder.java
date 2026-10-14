package org.nexlibre.prismavideo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Every other queue entry is visited once before a new shuffled cycle begins. */
public final class ShuffleOrder {
 private final Random random;
 private final List<Integer> remaining=new ArrayList<>();
 private int size=-1;
 public ShuffleOrder(Random random){this.random=random;}
 public void reset(){remaining.clear();size=-1;}
 public int next(int count,int current){
  if(count<=1)return 0;
  if(size!=count||remaining.isEmpty()){
   remaining.clear();size=count;
   for(int i=0;i<count;i++)if(i!=current)remaining.add(i);
   Collections.shuffle(remaining,random);
  }
  remaining.remove(Integer.valueOf(current));
  if(remaining.isEmpty()){reset();return next(count,current);}
  return remaining.remove(remaining.size()-1);
 }
}
