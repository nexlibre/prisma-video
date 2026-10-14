package org.nexlibre.prismavideo;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Uses the platform frame clock and respects the system's disabled animation scale. */
public final class MotionEngine {
 private MotionEngine(){}
 public static void visibility(View view,boolean visible,boolean enabled){
  if(view==null)return;view.animate().cancel();view.animate().withEndAction(null);
  if(!enabled||!ValueAnimator.areAnimatorsEnabled()){view.setAlpha(1);view.setVisibility(visible?View.VISIBLE:View.GONE);return;}
  if(visible){if(view.getVisibility()==View.VISIBLE&&view.getAlpha()==1)return;view.setAlpha(0);view.setVisibility(View.VISIBLE);view.animate().alpha(1).setDuration(160).setInterpolator(new DecelerateInterpolator()).start();}
  else{if(view.getVisibility()==View.GONE)return;view.animate().alpha(0).setDuration(140).withEndAction(()->{view.setVisibility(View.GONE);view.setAlpha(1);}).start();}
 }
 public static void enter(View view,boolean enabled){if(!enabled||!ValueAnimator.areAnimatorsEnabled())return;view.setAlpha(0);view.animate().alpha(1).setDuration(180).setInterpolator(new DecelerateInterpolator()).start();}
}
