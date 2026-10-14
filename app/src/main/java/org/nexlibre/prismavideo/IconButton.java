package org.nexlibre.prismavideo;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;

/** Vector controls with independent touch targets and spoken action labels. */
public final class IconButton extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private String glyph;
 private final int color;
 private final boolean ring;
 public IconButton(Context context,String glyph,String label,int color,boolean ring,Runnable action){
  super(context);this.glyph=glyph;this.color=color;this.ring=ring;
  setContentDescription(label);setFocusable(true);setClickable(true);
  GradientDrawable mask=new GradientDrawable();mask.setShape(GradientDrawable.OVAL);mask.setColor(0xFFFFFFFF);
  setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),null,mask));
  setOnClickListener(v->action.run());
 }
 public void setGlyph(String glyph,String label){this.glyph=glyph;setContentDescription(label);invalidate();}
 private void line(Canvas c,float x1,float y1,float x2,float y2){c.drawLine(x1,y1,x2,y2,paint);}
 private void path(Canvas c,float...xy){Path p=new Path();p.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)p.lineTo(xy[i],xy[i+1]);c.drawPath(p,paint);}
 private void triangle(Canvas c,float x1,float y1,float x2,float y2,float x3,float y3){Path p=new Path();p.moveTo(x1,y1);p.lineTo(x2,y2);p.lineTo(x3,y3);p.close();paint.setStyle(Paint.Style.FILL);c.drawPath(p,paint);paint.setStyle(Paint.Style.STROKE);}
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);float density=getResources().getDisplayMetrics().density;
  paint.setColor(color);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStyle(Paint.Style.STROKE);
  if(ring){paint.setStrokeWidth(1.7f*density);c.drawCircle(getWidth()/2f,getHeight()/2f,Math.min(getWidth(),getHeight())*.38f,paint);}
  float size=(ring?25:23)*density;c.save();c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(size/24,size/24);paint.setStrokeWidth(1.8f);
  switch(glyph){
   case "check":path(c,4,12,10,18,21,5);break;
   case "close":line(c,5,5,19,19);line(c,19,5,5,19);break;
   case "drag":line(c,5,8,19,8);line(c,5,16,19,16);break;
   case "playlist":line(c,3,6,17,6);line(c,3,12,17,12);line(c,3,18,11,18);line(c,19,15,19,21);line(c,16,18,22,18);break;
   case "history":c.drawArc(5,4,21,20,-150,310,false,paint);path(c,2,4,2,10,8,10);path(c,13,7,13,12,17,14);break;
   case "heart":{Path p=new Path();p.moveTo(12,20);p.cubicTo(1,13,1,5,6,4);p.cubicTo(9,3,11,5,12,7);p.cubicTo(13,5,15,3,18,4);p.cubicTo(23,5,23,13,12,20);c.drawPath(p,paint);break;}
   case "sort":line(c,4,6,20,6);line(c,4,12,15,12);line(c,4,18,10,18);break;
   case "lock":c.drawRoundRect(5,10,19,21,2,2,paint);c.drawArc(8,2,16,14,180,180,false,paint);line(c,12,14,12,17);break;
   case "unlock":c.drawRoundRect(5,10,19,21,2,2,paint);c.drawArc(8,2,16,14,175,160,false,paint);line(c,12,14,12,17);break;
   case "pip":c.drawRoundRect(3,4,21,20,2,2,paint);c.drawRect(12,12,19,18,paint);break;
   case "subtitles":c.drawRoundRect(3,5,21,19,2,2,paint);line(c,6,13,10,13);line(c,14,13,18,13);line(c,6,16,18,16);break;
   case "play":triangle(c,8,5,19,12,8,19);break;
   case "pause":line(c,8.5f,5,8.5f,19);line(c,15.5f,5,15.5f,19);break;
   case "previous":line(c,5,5,5,19);triangle(c,18,5,7,12,18,19);break;
   case "next":line(c,19,5,19,19);triangle(c,6,5,17,12,6,19);break;
   case "back":path(c,15,5,8,12,15,19);break;
   case "fullscreen":path(c,9,4,4,4,4,9);path(c,15,4,20,4,20,9);path(c,4,15,4,20,9,20);path(c,15,20,20,20,20,15);break;
   case "exit_fullscreen":path(c,4,9,9,9,9,4);path(c,15,4,15,9,20,9);path(c,4,15,9,15,9,20);path(c,15,20,15,15,20,15);break;
   case "more":paint.setStyle(Paint.Style.FILL);c.drawCircle(5,12,1.6f,paint);c.drawCircle(12,12,1.6f,paint);c.drawCircle(19,12,1.6f,paint);break;
   case "repeat":case "repeat_one":path(c,4,10,4,7,19,7);path(c,16,4,19,7,16,10);path(c,20,14,20,17,5,17);path(c,8,14,5,17,8,20);if(glyph.equals("repeat_one")){paint.setStrokeWidth(1.4f);path(c,10,11,12,10,12,14);line(c,10,14,14,14);}break;
   case "shuffle":path(c,3,6,7,6,17,18,21,18);path(c,18,15,21,18,18,21);path(c,3,18,7,18,10,14);path(c,14,10,17,6,21,6);path(c,18,3,21,6,18,9);break;
   case "headphones":c.drawArc(4,4,20,20,180,180,false,paint);c.drawRoundRect(3,11,7,20,1.5f,1.5f,paint);c.drawRoundRect(17,11,21,20,1.5f,1.5f,paint);break;
   case "video":c.drawRoundRect(3,5,21,19,2,2,paint);triangle(c,10,8,16,12,10,16);break;
   case "volume":path(c,3,9,7,9,12,5,12,19,7,15,3,15,3,9);c.drawArc(10,6,22,18,-55,110,false,paint);c.drawArc(13,9,19,15,-55,110,false,paint);break;
   case "refresh":c.drawArc(4,4,20,20,40,290,false,paint);path(c,15,3,20,5,20,0);break;
   case "folder":path(c,3,19,3,5,10,5,12,8,21,8,21,19,3,19);break;
   case "settings":c.drawCircle(12,12,7,paint);c.drawCircle(12,12,2.5f,paint);for(int i=0;i<8;i++){double a=i*Math.PI/4;line(c,12+(float)Math.cos(a)*7,12+(float)Math.sin(a)*7,12+(float)Math.cos(a)*10,12+(float)Math.sin(a)*10);}break;
  }
  c.restore();
 }
}
