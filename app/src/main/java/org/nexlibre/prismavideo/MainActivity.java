package org.nexlibre.prismavideo;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity implements PlaybackService.Listener {
 private PlaybackService playback;
 private boolean bound=false, night=true, playerPage=false, fullscreen=false, dragging=false, stopped=false;
 private LinearLayout root, rows, controls;
 private FrameLayout stage;
 private SurfaceView surface;
 private TextView title, time, feedback, audioLabel;
 private Button playButton, modeButton, audioButton;
 private SeekBar timeline;
 private EditText search;
 private final List<VideoItem> videos=new ArrayList<>(), visible=new ArrayList<>();
 private final Set<String> excluded=new HashSet<>();
 private final ExecutorService scanner=Executors.newSingleThreadExecutor();
 private final Handler handler=new Handler(Looper.getMainLooper());
 private android.content.SharedPreferences prefs;
 private int bg, fg, muted, panel, accent, scanToken=0;
 private Uri pendingUri;
 private final Runnable hideFeedback=()->{if(feedback!=null)feedback.setVisibility(View.GONE);};
 private final Runnable hideControls=()->{if(playerPage&&playback!=null&&playback.playing()&&controls!=null)controls.setVisibility(View.GONE);};
 private final Runnable tick=new Runnable(){public void run(){if(playerPage&&playback!=null){if(!dragging){timeline.setMax(Math.max(1,playback.duration()));timeline.setProgress(playback.position());}time.setText(clock(playback.position())+" / "+clock(playback.duration()));}handler.postDelayed(this,500);}};
 private final ServiceConnection connection=new ServiceConnection(){
  public void onServiceConnected(ComponentName n,IBinder binder){playback=((PlaybackService.LocalBinder)binder).get();playback.listener=MainActivity.this;bound=true;
   if(pendingUri!=null){openExternal(pendingUri);pendingUri=null;}else if(playback.current()!=null)showPlayer();
  }
  public void onServiceDisconnected(ComponentName n){bound=false;playback=null;showLibrary();}
 };
 @Override public void onCreate(Bundle state){
  super.onCreate(state);prefs=getSharedPreferences("settings",MODE_PRIVATE);night=prefs.getBoolean("night",true);excluded.addAll(prefs.getStringSet("excluded",Collections.emptySet()));
  
  pendingUri=getIntent().getData();showLibrary();bindService(new Intent(this,PlaybackService.class),connection,BIND_AUTO_CREATE);handler.post(tick);permission();
 }
 private int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
 private void palette(){bg=Color.parseColor(night?"#000000":"#F7F8F6");fg=Color.parseColor(night?"#F2F4F3":"#19211F");muted=Color.parseColor(night?"#939C98":"#66736E");panel=Color.parseColor(night?"#111614":"#E9EFEC");accent=Color.parseColor(night?"#A8D5C1":"#24654C");getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);getWindow().getDecorView().setSystemUiVisibility(night?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);}
 private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
 private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
 private Button button(String value,Runnable action){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextColor(accent);b.setTextSize(13);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(44));b.setPadding(dp(12),0,dp(12),0);b.setBackground(shape(panel,18));b.setOnClickListener(v->action.run());return b;}
 private void addButton(LinearLayout row,String value,Runnable action){Button b=button(value,action);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);p.setMargins(dp(3),0,dp(3),0);row.addView(b,p);}
 private void base(){palette();root=column();root.setBackgroundColor(bg);root.setPadding(dp(20),dp(16),dp(20),dp(12));root.setFitsSystemWindows(true);setContentView(root);}
 private void showLibrary(){
  playerPage=false;fullscreen=false;WindowManager.LayoutParams windowParams=getWindow().getAttributes();windowParams.screenBrightness=-1;getWindow().setAttributes(windowParams);getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  if(playback!=null)playback.setSurface(null);base();
  LinearLayout top=row();TextView logo=text("Prisma Video",26,fg);logo.setTypeface(null,Typeface.BOLD);top.addView(logo,new LinearLayout.LayoutParams(0,dp(56),1));top.addView(button("Ajustes",this::settings));root.addView(top);
  TextView subtitle=text("Tus vídeos. Sin distracciones.",14,muted);root.addView(subtitle);
  search=new EditText(this);search.setSingleLine(true);search.setTextColor(fg);search.setHintTextColor(muted);search.setHint("Buscar vídeos");search.setTextSize(16);search.setPadding(dp(16),0,dp(16),0);search.setBackground(shape(panel,16));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(52));sp.setMargins(0,dp(24),0,dp(12));root.addView(search,sp);
  search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){renderRows();}public void afterTextChanged(android.text.Editable e){}});
  LinearLayout tools=row();addButton(tools,"Actualizar",this::permission);addButton(tools,"Abrir archivo",this::pickFile);root.addView(tools);
  ListView list=new ListView(this);list.setDivider(null);list.setAdapter(adapter);list.setOnItemClickListener((p,v,pos,id)->startVideo(new ArrayList<>(visible),pos));root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
  rows=column();root.addView(rows);renderRows();scan();
 }
 private final BaseAdapter adapter=new BaseAdapter(){
  public int getCount(){return visible.size();}public Object getItem(int pos){return visible.get(pos);}public long getItemId(int pos){return pos;}
  public View getView(int pos,View old,android.view.ViewGroup parent){VideoItem video=visible.get(pos);LinearLayout r=row();r.setPadding(0,dp(13),0,dp(13));TextView icon=text("▶",22,accent);icon.setGravity(Gravity.CENTER);icon.setBackground(shape(panel,14));r.addView(icon,new LinearLayout.LayoutParams(dp(52),dp(52)));LinearLayout details=column();details.setPadding(dp(14),0,0,0);TextView name=text(video.title,16,fg);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);details.addView(name);details.addView(text(clock(video.duration)+"  ·  "+video.folder,12,muted));r.addView(details,new LinearLayout.LayoutParams(0,-2,1));return r;}
 };
 private void renderRows(){if(playerPage||search==null||rows==null)return;String q=search.getText().toString().toLowerCase(Locale.ROOT);visible.clear();for(VideoItem v:videos)if(!blocked(v.folder)&&v.title.toLowerCase(Locale.ROOT).contains(q))visible.add(v);adapter.notifyDataSetChanged();rows.removeAllViews();
  if(visible.isEmpty())rows.addView(text("No hay vídeos visibles. Revisa los permisos y las carpetas excluidas.",14,muted));
  if(playback!=null&&playback.current()!=null)rows.addView(button("Continuar · "+playback.current().title,this::showPlayer));
 }
 private String normalize(String path){String p=path.trim().replace('\\','/');while(p.startsWith("/"))p=p.substring(1);if(p.startsWith("storage/emulated/0/"))p=p.substring(19);if(p.startsWith("sdcard/"))p=p.substring(7);while(p.endsWith("/"))p=p.substring(0,p.length()-1);return p;}
 private boolean blocked(String folder){String f=normalize(folder);for(String x:excluded)if(f.equals(normalize(x))||f.startsWith(normalize(x)+"/"))return true;return false;}
 private String mediaPermission(){return Build.VERSION.SDK_INT>=33?"android.permission.READ_MEDIA_VIDEO":Manifest.permission.READ_EXTERNAL_STORAGE;}
 private void permission(){ArrayList<String> wanted=new ArrayList<>();if(checkSelfPermission(mediaPermission())!=PackageManager.PERMISSION_GRANTED)wanted.add(mediaPermission());if(Build.VERSION.SDK_INT>=34&&checkSelfPermission("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")!=PackageManager.PERMISSION_GRANTED)wanted.add("android.permission.READ_MEDIA_VISUAL_USER_SELECTED");if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED)wanted.add("android.permission.POST_NOTIFICATIONS");if(!wanted.isEmpty())requestPermissions(wanted.toArray(new String[0]),8);else scan();}
 @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);scan();}
 private void scan(){final int token=++scanToken;scanner.execute(()->{ArrayList<VideoItem> result=new ArrayList<>();String path=Build.VERSION.SDK_INT>=29?MediaStore.MediaColumns.RELATIVE_PATH:MediaStore.MediaColumns.DATA;
  try(Cursor c=getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Video.Media._ID,MediaStore.MediaColumns.DISPLAY_NAME,path,MediaStore.Video.Media.DURATION},null,null,MediaStore.MediaColumns.DATE_ADDED+" DESC")){
   if(c!=null)while(c.moveToNext()){String folder=c.getString(2);if(folder==null)folder="";if(Build.VERSION.SDK_INT<29){int end=folder.lastIndexOf('/');folder=end>=0?folder.substring(0,end):"";}result.add(new VideoItem(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,c.getLong(0)),c.getString(1),normalize(folder),c.getLong(3)));}
  }catch(SecurityException e){}catch(Exception e){handler.post(()->toast("No se pudo actualizar la biblioteca."));}
  handler.post(()->{if(isFinishing()||isDestroyed()||token!=scanToken)return;videos.clear();videos.addAll(result);renderRows();});
 });}
 private void pickFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("video/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,20);}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==20&&result==RESULT_OK&&data!=null){Uri uri=data.getData();try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException e){}if(playback==null)pendingUri=uri;else openExternal(uri);}}
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(intent.getData()!=null){if(playback==null)pendingUri=intent.getData();else openExternal(intent.getData());}else if(playback!=null&&playback.current()!=null)showPlayer();}
 private void openExternal(Uri uri){String name="Vídeo";try(Cursor c=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())name=c.getString(0);}catch(Exception e){}startVideo(Collections.singletonList(new VideoItem(uri,name,"Archivo seleccionado",0)),0);}
 private void startVideo(List<VideoItem> queue,int selected){if(playback==null){toast("Preparando reproductor…");return;}startForegroundService(new Intent(this,PlaybackService.class));playback.setQueue(queue,selected);showPlayer();}
 private void showPlayer(){if(playback==null||playback.current()==null)return;playerPage=true;base();root.setPadding(0,0,0,0);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);if(fullscreen)getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
  LinearLayout top=row();top.addView(button("‹",this::showLibrary));title=text(playback.current().title,16,fg);title.setMaxLines(2);title.setPadding(dp(12),0,dp(8),0);top.addView(title,new LinearLayout.LayoutParams(0,dp(56),1));top.addView(button("⛶",()->{fullscreen=!fullscreen;if(fullscreen)getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);setRequestedOrientation(fullscreen?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE:android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);}));
  FrameLayout playerFrame=new FrameLayout(this);root.addView(playerFrame,new LinearLayout.LayoutParams(-1,-1));
  stage=new FrameLayout(this);stage.setBackgroundColor(Color.BLACK);playerFrame.addView(stage,new FrameLayout.LayoutParams(-1,-1));surface=new SurfaceView(this);stage.addView(surface,new FrameLayout.LayoutParams(-1,-1));
  surface.getHolder().addCallback(new SurfaceHolder.Callback(){public void surfaceCreated(SurfaceHolder h){playback.setSurface(h.getSurface());fitVideo();}public void surfaceChanged(SurfaceHolder h,int f,int w,int t){fitVideo();}public void surfaceDestroyed(SurfaceHolder h){if(playback!=null)playback.setSurface(null);}});
  audioLabel=text("♫\nSolo audio",26,Color.WHITE);audioLabel.setGravity(Gravity.CENTER);stage.addView(audioLabel,new FrameLayout.LayoutParams(-1,-1));
  feedback=text("",20,Color.WHITE);feedback.setGravity(Gravity.CENTER);feedback.setBackground(shape(0xCC17221C,18));FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(-2,dp(64),Gravity.CENTER);stage.addView(feedback,fp);feedback.setVisibility(View.GONE);
  View touch=new View(this);stage.addView(touch,new FrameLayout.LayoutParams(-1,-1));gestures(touch);
  controls=column();controls.setPadding(dp(16),dp(8),dp(16),dp(12));controls.setBackgroundColor(night?0xE6000000:0xE6F7F8F6);playerFrame.addView(controls,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));top.setPadding(dp(12),dp(4),dp(12),0);top.setBackgroundColor(night?0xCC000000:0xCCF7F8F6);playerFrame.addView(top,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
  time=text("",12,muted);controls.addView(time);timeline=new SeekBar(this);timeline.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));timeline.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));controls.addView(timeline);
  timeline.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){if(user)time.setText(clock(p)+" / "+clock(playback.duration()));}public void onStartTrackingTouch(SeekBar s){dragging=true;handler.removeCallbacks(hideControls);}public void onStopTrackingTouch(SeekBar s){playback.seek(s.getProgress());dragging=false;scheduleHide();}});
  LinearLayout primary=row();addButton(primary,"− "+step()+" s",()->{playback.seek(playback.position()-step()*1000L);scheduleHide();});playButton=button("▶",()->{playback.toggle();scheduleHide();});primary.addView(playButton,new LinearLayout.LayoutParams(0,dp(52),1));addButton(primary,"+ "+step()+" s",()->{playback.seek(playback.position()+step()*1000L);scheduleHide();});controls.addView(primary);
  LinearLayout secondary=row();addButton(secondary,"Anterior",playback::previous);addButton(secondary,"Siguiente",playback::next);controls.addView(secondary);
  LinearLayout options=row();modeButton=button("",()->{playback.setMode((playback.mode+1)%3);scheduleHide();});options.addView(modeButton,new LinearLayout.LayoutParams(0,dp(48),1));audioButton=button("",()->{playback.setAudioOnly(!playback.audioOnly);scheduleHide();});options.addView(audioButton,new LinearLayout.LayoutParams(0,dp(48),1));options.addView(button("Volumen",this::volumeDialog));controls.addView(options);changed();scheduleHide();
 }
 private void fitVideo(){if(surface==null||stage==null||playback==null||!playback.prepared)return;int w=playback.player.getVideoWidth(),h=playback.player.getVideoHeight();if(w<=0||h<=0)return;int sw=stage.getWidth(),sh=stage.getHeight();if(sw<=0||sh<=0)return;double ratio=Math.min((double)sw/w,(double)sh/h);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams((int)(w*ratio),(int)(h*ratio),Gravity.CENTER);if(surface.getLayoutParams().width!=p.width||surface.getLayoutParams().height!=p.height)surface.setLayoutParams(p);}
 private void scheduleHide(){handler.removeCallbacks(hideControls);handler.postDelayed(hideControls,4000);}
 private int step(){return prefs.getInt("seekSeconds",5);}
 private void hud(String value){feedback.setText("  "+value+"  ");feedback.setVisibility(View.VISIBLE);handler.removeCallbacks(hideFeedback);handler.postDelayed(hideFeedback,1200);}
 private void gestures(View touch){
  GestureDetector detector=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
   public boolean onDown(MotionEvent e){return true;}
   public boolean onSingleTapConfirmed(MotionEvent e){controls.setVisibility(controls.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);scheduleHide();return true;}
   public boolean onDoubleTap(MotionEvent e){long delta=(e.getX()<stage.getWidth()/2?-1:1)*step()*1000L;playback.seek(playback.position()+delta);hud((delta<0?"−":"+")+step()+" s");return true;}
  });
  touch.setOnTouchListener(new View.OnTouchListener(){float x,y,brightness;int startPosition,startVolume,direction;boolean moving;
   public boolean onTouch(View v,MotionEvent e){
    if(e.getAction()==MotionEvent.ACTION_DOWN){x=e.getX();y=e.getY();startPosition=playback.position();startVolume=playback.volume;brightness=getWindow().getAttributes().screenBrightness;if(brightness<0)try{brightness=android.provider.Settings.System.getInt(getContentResolver(),android.provider.Settings.System.SCREEN_BRIGHTNESS)/255f;}catch(Exception ex){brightness=.5f;}moving=false;direction=0;handler.removeCallbacks(hideControls);}
    if(e.getAction()==MotionEvent.ACTION_MOVE){float dx=e.getX()-x,dy=y-e.getY();if(!moving&&Math.max(Math.abs(dx),Math.abs(dy))>dp(24)){moving=true;direction=Math.abs(dx)>Math.abs(dy)?1:2;detector.onTouchEvent(MotionEvent.obtain(e.getDownTime(),e.getEventTime(),MotionEvent.ACTION_CANCEL,e.getX(),e.getY(),0));}
     if(moving&&direction==1){long target=Math.max(0,Math.min(playback.duration(),startPosition+(long)(dx/Math.max(1,stage.getWidth())*120000)));hud(clock(target));}
     else if(moving&&x<stage.getWidth()/2){float value=Math.max(.02f,Math.min(1,brightness+dy/Math.max(1,stage.getHeight())));WindowManager.LayoutParams p=getWindow().getAttributes();p.screenBrightness=value;getWindow().setAttributes(p);hud("Brillo "+Math.round(value*100)+" %");}
     else if(moving){playback.setVolume(startVolume+(int)(dy/Math.max(1,stage.getHeight())*200));hud("Volumen "+playback.volume+" %");}
    }
    if(e.getAction()==MotionEvent.ACTION_UP&&moving){if(direction==1)playback.seek(startPosition+(long)((e.getX()-x)/Math.max(1,stage.getWidth())*120000));scheduleHide();return true;}
    if(e.getAction()==MotionEvent.ACTION_CANCEL){moving=false;scheduleHide();return true;}
    if(!moving)detector.onTouchEvent(e);return true;
   }
  });
 }
 private void volumeDialog(){LinearLayout box=column();box.setPadding(dp(24),dp(12),dp(24),dp(8));TextView label=text("Volumen "+playback.volume+" %",18,fg);box.addView(label);SeekBar bar=new SeekBar(this);bar.setMax(playback.boostAvailable?200:100);bar.setProgress(playback.volume);box.addView(bar);box.addView(text(playback.boostAvailable?"Más de 100 % activa el amplificador. Reduce el nivel si escuchas distorsión.":"Este dispositivo no admite el amplificador.",14,muted));bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){if(user){playback.setVolume(p);label.setText("Volumen "+playback.volume+" %");}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});new AlertDialog.Builder(this,night?AlertDialog.THEME_DEVICE_DEFAULT_DARK:AlertDialog.THEME_DEVICE_DEFAULT_LIGHT).setTitle("Volumen y amplificación").setView(box).setPositiveButton("Listo",(d,w)->scheduleHide()).show();}
 private void settings(){String[] choices={"Tema: "+(night?"AMOLED":"Día"),"Doble toque: "+step()+" segundos","Carpetas excluidas"};new AlertDialog.Builder(this,night?AlertDialog.THEME_DEVICE_DEFAULT_DARK:AlertDialog.THEME_DEVICE_DEFAULT_LIGHT).setTitle("Ajustes").setItems(choices,(d,w)->{if(w==0){night=!night;prefs.edit().putBoolean("night",night).apply();if(playerPage)showPlayer();else showLibrary();}else if(w==1){seekSettings();}else folders();}).setNegativeButton("Cerrar",null).show();}
 private void seekSettings(){EditText input=new EditText(this);input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);input.setText(String.valueOf(step()));input.setSelectAllOnFocus(true);AlertDialog d=new AlertDialog.Builder(this).setTitle("Segundos por salto").setMessage("Elige de 1 a 120 segundos para el doble toque y los botones.").setView(input).setPositiveButton("Guardar",null).setNegativeButton("Cancelar",null).create();d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{int value=Integer.parseInt(input.getText().toString());if(value<1||value>120)throw new NumberFormatException();prefs.edit().putInt("seekSeconds",value).apply();d.dismiss();if(playerPage)showPlayer();}catch(NumberFormatException e){input.setError("Introduce un número de 1 a 120");}}));d.show();}
 private void folders(){TreeSet<String> paths=new TreeSet<>(excluded);for(VideoItem v:videos)if(!v.folder.isEmpty())paths.add(v.folder);String[] names=paths.toArray(new String[0]);boolean[] checked=new boolean[names.length];for(int i=0;i<names.length;i++)checked[i]=excluded.contains(names[i]);new AlertDialog.Builder(this,night?AlertDialog.THEME_DEVICE_DEFAULT_DARK:AlertDialog.THEME_DEVICE_DEFAULT_LIGHT).setTitle("Excluir carpetas y subcarpetas").setMultiChoiceItems(names,checked,(d,i,c)->{if(c)excluded.add(names[i]);else excluded.remove(names[i]);saveFolders();}).setPositiveButton("Listo",(d,w)->renderRows()).setNeutralButton("Añadir ruta",(d,w)->manualFolder()).setNegativeButton("Cerrar",null).show();}
 private void manualFolder(){EditText input=new EditText(this);input.setSingleLine();input.setHint("Movies/Privados");input.setPadding(dp(24),dp(16),dp(24),dp(16));AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Ruta de carpeta").setMessage("Escribe la ruta relativa al almacenamiento, por ejemplo Movies/Privados. Incluye sus subcarpetas.").setView(input).setPositiveButton("Excluir",null).setNegativeButton("Cancelar",null).create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String path=normalize(input.getText().toString());if(path.isEmpty()||path.contains("..")){input.setError("Escribe una carpeta válida");return;}excluded.add(path);saveFolders();dialog.dismiss();folders();}));dialog.show();}
 private void saveFolders(){prefs.edit().putStringSet("excluded",new HashSet<>(excluded)).apply();renderRows();}
 private String clock(long ms){long sec=Math.max(0,ms)/1000;return sec>=3600?String.format(Locale.ROOT,"%d:%02d:%02d",sec/3600,sec/60%60,sec%60):String.format(Locale.ROOT,"%d:%02d",sec/60,sec%60);}
 private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
 @Override public void changed(){if(playerPage&&playback!=null){if(playback.current()==null){showLibrary();return;}title.setText(playback.current().title);playButton.setText(playback.playing()?"Ⅱ":"▶");modeButton.setText(new String[]{"Todos","Repetir uno","Aleatorio"}[playback.mode]);audioButton.setText(playback.audioOnly?"Ver vídeo":"Solo audio");audioLabel.setVisibility(playback.audioOnly?View.VISIBLE:View.GONE);surface.setVisibility(playback.audioOnly?View.INVISIBLE:View.VISIBLE);fitVideo();if(!playback.playing())controls.setVisibility(View.VISIBLE);}else renderRows();}
 @Override public void error(String message){toast(message);}
 @Override public void onBackPressed(){if(playerPage){setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);showLibrary();}else super.onBackPressed();}
 @Override protected void onStop(){super.onStop();stopped=true;if(playback!=null&&!playback.audioOnly)playback.pause();}
 @Override protected void onResume(){super.onResume();if(stopped){stopped=false;scan();}}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putBoolean("playerPage",playerPage);out.putBoolean("fullscreen",fullscreen);}
 @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);if(playerPage)showPlayer();else showLibrary();}
 @Override protected void onDestroy(){scanToken++;handler.removeCallbacksAndMessages(null);scanner.shutdownNow();if(playback!=null){playback.listener=null;playback.setSurface(null);}if(bound)unbindService(connection);super.onDestroy();}
}
