package org.nexlibre.prismavideo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;

/** All palettes share the same controls and capabilities. Legacy night/accent keys remain valid. */
public final class ThemeEngine {
 public final int background,foreground,muted,panel,accent;
 public final boolean dark;
 public ThemeEngine(Context context,SharedPreferences settings){
  int theme=settings.getInt("theme",settings.getBoolean("night",true)?0:1);dark=theme!=1;
  background=Color.parseColor(theme==3?"#0A0D18":dark?"#000000":"#F7F8F6");foreground=Color.parseColor(dark?"#F2F4F3":"#19211F");muted=Color.parseColor(dark?"#A1AAA6":"#66736E");panel=Color.parseColor(theme==3?"#181D32":dark?"#111614":"#E9EFEC");
  int custom=Math.max(0,Math.min(4,settings.getInt("accent",0)));int[] colors=dark?new int[]{0xFFA8D5C1,0xFFFFD08A,0xFFACCDFF,0xFFC6B8FF,0xFFFFB2AD}:new int[]{0xFF24654C,0xFF945500,0xFF285998,0xFF6651AA,0xFF99433D};
  int chosen=theme==3&&custom==0?0xFFC6B8FF:colors[custom];if(theme==2&&Build.VERSION.SDK_INT>=31)chosen=context.getColor(android.R.color.system_accent1_300);accent=chosen;
 }
}
