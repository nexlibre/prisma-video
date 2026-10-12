import org.nexlibre.prismavideo.SubtitleTrack;
import java.io.*;
import java.nio.charset.*;

public final class SubtitleTrackTest {
 private static void eq(String expected,String actual){if(!expected.equals(actual))throw new AssertionError("Expected: ["+expected+"] actual: ["+actual+"]");}
 public static void main(String[] args)throws Exception {
  SubtitleTrack srt=SubtitleTrack.parse("\uFEFF1\r\n00:00:01,000 --> 00:00:03,000\r\nHola <i>mundo</i>\r\n\r\n2\r\n00:00:02,000 --> 00:00:04,000\r\nSegunda &amp; tercera\r\n");
  eq("",srt.at(999));eq("Hola mundo",srt.at(1000));eq("Hola mundo\nSegunda & tercera",srt.at(2000));eq("Segunda & tercera",srt.at(3000));eq("",srt.at(4000));eq("Hola mundo",srt.at(1500));
  SubtitleTrack vtt=SubtitleTrack.parse("WEBVTT\n\nNOTE invisible\nnot a cue\n\ncue-id\n01:00.000 --> 01:02.250 align:start\n<v Ana>Una línea\ny otra\n\nlong\n01:02:03.000 --> 01:02:04.000\nHora completa\n");
  eq("Una línea\ny otra",vtt.at(60000));eq("",vtt.at(62250));eq("Hora completa",vtt.at(3723000));
  String encoded="1\n00:00:00,100 --> 00:00:01,000\nCanción\n";
  eq("Canción",SubtitleTrack.read(new ByteArrayInputStream(encoded.getBytes(StandardCharsets.UTF_16))).at(500));
  eq("Canción",SubtitleTrack.read(new ByteArrayInputStream(encoded.getBytes(Charset.forName("windows-1252")))).at(500));
  SubtitleTrack unsorted=SubtitleTrack.parse("2\n00:00:05,000 --> 00:00:06,000\nDespués\n\n1\n00:00:01,000 --> 00:00:02,000\nAntes\n");eq("Antes",unsorted.at(1000));eq("Después",unsorted.at(5000));
  boolean malformed=false;try{SubtitleTrack.parse("1\n00:00:65,000 --> 00:00:66,000\ninválido\n");}catch(IOException e){malformed=true;}if(!malformed)throw new AssertionError("Invalid timestamp accepted");
  boolean tooLarge=false;try{SubtitleTrack.read(new InputStream(){int remaining=5*1024*1024+1;public int read(){return remaining-->0?'x':-1;}});}catch(IOException e){tooLarge=true;}if(!tooLarge)throw new AssertionError("Size limit not enforced");
  System.out.println("PASS: SRT, VTT, overlaps, seeking, boundaries, UTF-16, Windows-1252, sorting, invalid timestamps and size limit");
 }
}
