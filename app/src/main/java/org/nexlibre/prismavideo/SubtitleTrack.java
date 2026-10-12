package org.nexlibre.prismavideo;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

/** Local SRT/WebVTT text. No network, HTML execution or external resources. */
public final class SubtitleTrack {
 public static final class Cue { public final long start,end; public final String text; Cue(long s,long e,String t){start=s;end=e;text=t;} }
 private final List<Cue> cues;
 private final long[] latestEnd;
 private SubtitleTrack(List<Cue> cues){this.cues=cues;latestEnd=new long[cues.size()];long max=0;for(int i=0;i<cues.size();i++){max=Math.max(max,cues.get(i).end);latestEnd[i]=max;}}
 public int size(){return cues.size();}
 public static SubtitleTrack read(InputStream input) throws IOException {
  if(input==null)throw new IOException("No se puede abrir el archivo");
  ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
  while((n=input.read(buffer))!=-1){if(out.size()+n>5*1024*1024)throw new IOException("El subtítulo supera 5 MB");out.write(buffer,0,n);}
  byte[] bytes=out.toByteArray();String text;
  if(bytes.length>=2&&((bytes[0]&255)==255&&(bytes[1]&255)==254||(bytes[0]&255)==254&&(bytes[1]&255)==255))text=new String(bytes,StandardCharsets.UTF_16);
  else try{text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();}catch(CharacterCodingException e){text=new String(bytes,Charset.forName("windows-1252"));}
  return parse(text);
 }
 public static SubtitleTrack parse(String source)throws IOException {
  String[] blocks=source.replace("\uFEFF","").replace("\r\n","\n").replace('\r','\n').split("\n[ \\t]*\n+");
  Pattern timing=Pattern.compile("(?m)^\\s*((?:\\d{1,3}:)?\\d{2}:\\d{2}[.,]\\d{3})\\s*-->\\s*((?:\\d{1,3}:)?\\d{2}:\\d{2}[.,]\\d{3})[^\\n]*\\n");
  List<Cue> cues=new ArrayList<>();
  for(String block:blocks){String trimmed=block.trim();if(trimmed.startsWith("NOTE")||trimmed.startsWith("STYLE")||trimmed.startsWith("REGION"))continue;Matcher m=timing.matcher(block);if(!m.find())continue;
   try{long start=timestamp(m.group(1)),end=timestamp(m.group(2));String value=block.substring(m.end()).trim().replaceAll("<[^>]*>","").replace("&nbsp;"," ").replace("&lt;","<").replace("&gt;",">").replace("&quot;","\"").replace("&#39;","'").replace("&amp;","&");if(value.length()>4096)throw new IOException("Línea de subtítulo demasiado larga");if(end>start&&!value.isEmpty()){cues.add(new Cue(start,end,value));if(cues.size()>50000)throw new IOException("Demasiadas líneas de subtítulos");}}catch(NumberFormatException e){}
  }
  cues.sort(Comparator.comparingLong(c->c.start));if(cues.isEmpty())throw new IOException("No hay líneas SRT o VTT válidas");return new SubtitleTrack(Collections.unmodifiableList(cues));
 }
 private static long timestamp(String value){String[] p=value.replace(',','.').split(":");int i=p.length-1;String[] sec=p[i].split("\\.");long seconds=Long.parseLong(sec[0]);long minutes=Long.parseLong(p[i-1]);long hours=i>=2?Long.parseLong(p[i-2]):0;if(seconds>59||minutes>59)throw new NumberFormatException();return ((hours*60+minutes)*60+seconds)*1000+Long.parseLong(sec[1]);}
 public String at(long position){int lo=0,hi=cues.size();while(lo<hi){int mid=(lo+hi)>>>1;if(cues.get(mid).start<=position)lo=mid+1;else hi=mid;}List<String> lines=new ArrayList<>();for(int i=lo-1;i>=0&&latestEnd[i]>position;i--){Cue c=cues.get(i);if(c.end>position)lines.add(c.text);}Collections.reverse(lines);return String.join("\n",lines);}
}
