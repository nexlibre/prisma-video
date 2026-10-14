package org.nexlibre.prismavideo;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class InterfaceTest {
 private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
 private final UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
 private void grant() throws Exception {
  androidx.test.uiautomator.Configurator.getInstance().setWaitForIdleTimeout(0);device.wakeUp();device.executeShellCommand("wm dismiss-keyguard");
  String packageName=context.getPackageName();
  device.executeShellCommand("pm grant "+packageName+" "+(Build.VERSION.SDK_INT>=33?"android.permission.READ_MEDIA_VIDEO":Manifest.permission.READ_EXTERNAL_STORAGE));
  if(Build.VERSION.SDK_INT<=28)device.executeShellCommand("pm grant "+packageName+" android.permission.WRITE_EXTERNAL_STORAGE");
  if(Build.VERSION.SDK_INT>=33)device.executeShellCommand("pm grant "+packageName+" android.permission.POST_NOTIFICATIONS");
  if(Build.VERSION.SDK_INT>=34)device.executeShellCommand("pm grant "+packageName+" android.permission.READ_MEDIA_VISUAL_USER_SELECTED");
 }
 @org.junit.Rule public final org.junit.rules.TestWatcher diagnostics=new org.junit.rules.TestWatcher(){@Override protected void failed(Throwable error,org.junit.runner.Description test){try{File folder=new File(context.getExternalFilesDir(null),"screenshots");folder.mkdirs();device.dumpWindowHierarchy(new File(folder,"failed-"+test.getMethodName()+".xml"));device.takeScreenshot(new File(folder,"failed-"+test.getMethodName()+".png"));}catch(Exception ignored){}saveDiagnostics();}};
 private void clickText(String label){assertTrue("Missing "+label,device.wait(Until.hasObject(By.text(label)),10000));device.findObject(By.text(label)).click();}
 private void screenshot(String name){File folder=new File(context.getExternalFilesDir(null),"screenshots");folder.mkdirs();try{device.dumpWindowHierarchy(new File(folder,name+".xml"));}catch(Exception ignored){}assertTrue(device.takeScreenshot(new File(folder,name+"-api"+Build.VERSION.SDK_INT+".png")));captureQa(name);saveDiagnostics();}
 private void captureQa(String name){try{File preview=new File(context.getExternalFilesDir(null),"screenshots/"+name+"-preview.png");if(!device.takeScreenshot(preview,.35f,90))return;try(InputStream input=new java.io.FileInputStream(preview);ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int count;while((count=input.read(buffer))!=-1)bytes.write(buffer,0,count);String encoded=Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP);for(int offset=0;offset<encoded.length();offset+=3000)android.util.Log.i("PRISMA_QA","IMG "+name+" "+offset+" "+encoded.substring(offset,Math.min(encoded.length(),offset+3000)));}}catch(Exception ignored){}}
 private void saveDiagnostics(){try{device.executeShellCommand("mkdir -p /sdcard/Download/PrismaTestScreenshots");device.executeShellCommand("cp -r /sdcard/Android/data/org.nexlibre.prismavideo/files/screenshots/. /sdcard/Download/PrismaTestScreenshots/");}catch(Exception ignored){}}

 @Test public void allThemesAndSelectionAreReachable() throws Exception {
  grant();try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   screenshot("library-initial");
   for(String theme:new String[]{"AMOLED Black","Light Minimal","Dynamic Color · Android 12+","Prisma Signature"}){
    assertTrue(device.wait(Until.hasObject(By.desc("Ajustes")),10000));device.findObject(By.desc("Ajustes")).click();clickText("Apariencia");clickText("Temas");clickText(theme);assertTrue(device.wait(Until.hasObject(By.text("Prisma Video")),10000));screenshot(theme.replaceAll("[^A-Za-z]",""));
   }
   device.findObject(By.desc("Vistas y filtros")).click();clickText("Seleccionar varios vídeos");assertTrue(device.wait(Until.hasObject(By.desc("Salir de selección")),10000));device.findObject(By.desc("Salir de selección")).click();assertTrue(device.wait(Until.gone(By.desc("Salir de selección")),5000));
  }
 }
 @Test public void localVideoControlsAndPipAreReachable() throws Exception {
  grant();byte[] video;
  try(InputStream input=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("smoke.mp4.b64");ByteArrayOutputStream encoded=new ByteArrayOutputStream()){
   byte[] buffer=new byte[4096];int count;while((count=input.read(buffer))!=-1)encoded.write(buffer,0,count);video=Base64.decode(encoded.toByteArray(),Base64.DEFAULT);
  }
  Uri uri;
  if(Build.VERSION.SDK_INT>=29){ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.DISPLAY_NAME,"Prisma-interface-test.mp4");values.put(MediaStore.MediaColumns.MIME_TYPE,"video/mp4");values.put(MediaStore.MediaColumns.RELATIVE_PATH,"Movies/PrismaTests");values.put(MediaStore.MediaColumns.IS_PENDING,1);uri=context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,values);assertNotNull(uri);try(OutputStream output=context.getContentResolver().openOutputStream(uri)){output.write(video);}values.clear();values.put(MediaStore.MediaColumns.IS_PENDING,0);context.getContentResolver().update(uri,values,null,null);}
  else{File folder=new File(android.os.Environment.getExternalStorageDirectory(),"Movies/PrismaTests");folder.mkdirs();File file=new File(folder,"Prisma-interface-test.mp4");try(FileOutputStream output=new FileOutputStream(file)){output.write(video);}java.util.concurrent.CountDownLatch scanned=new java.util.concurrent.CountDownLatch(1);java.util.concurrent.atomic.AtomicReference<Uri> result=new java.util.concurrent.atomic.AtomicReference<>();android.media.MediaScannerConnection.scanFile(context,new String[]{file.getPath()},new String[]{"video/mp4"},(path,value)->{result.set(value);scanned.countDown();});assertTrue(scanned.await(20,java.util.concurrent.TimeUnit.SECONDS));uri=result.get();assertNotNull(uri);}
  Intent intent=new Intent(context,MainActivity.class).setAction(Intent.ACTION_VIEW).setData(uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(intent)){
   screenshot("player-initial");
   assertTrue("No video controls",device.wait(Until.hasObject(By.desc("Más opciones de reproducción")),15000));
   assertTrue("Video did not start",device.wait(Until.hasObject(By.desc("Pausar")),15000));device.findObject(By.desc("Pausar")).click();assertTrue(device.wait(Until.hasObject(By.desc("Reproducir")),5000));screenshot("player");
   if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)){
    java.util.concurrent.atomic.AtomicReference<MainActivity> target=new java.util.concurrent.atomic.AtomicReference<>();scenario.onActivity(target::set);assertTrue(device.wait(Until.hasObject(By.desc("Pantalla en pantalla")),5000));device.findObject(By.desc("Pantalla en pantalla")).click();java.util.concurrent.atomic.AtomicBoolean entered=new java.util.concurrent.atomic.AtomicBoolean();for(int attempt=0;attempt<50&&!entered.get();attempt++){InstrumentationRegistry.getInstrumentation().runOnMainSync(()->entered.set(target.get().isInPictureInPictureMode()));if(!entered.get())Thread.sleep(100);}assertTrue("PiP did not enter",entered.get());screenshot("pip");
   }
  }finally{if(Build.VERSION.SDK_INT>=29)context.getContentResolver().delete(uri,null,null);else context.getContentResolver().delete(uri,null,null);}
 }
}
