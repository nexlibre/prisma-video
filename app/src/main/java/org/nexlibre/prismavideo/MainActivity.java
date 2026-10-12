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
 private LinearLayout root, rows, controls, playerHeader;
 private FrameLayout stage;
 private SurfaceView surface;
 private TextView title, time, totalTime, feedback, audioLabel;
 private IconButton playButton, modeButton, audioButton, fullscreenButton, optionsButton;
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
 private final Runnable hideControls=()->{if(playerPage&&playback!=null&&playback.playing()&&controls!=null)setControlsVisible(false);};
 private final Runnable tick=new Runnable(){public void run(){if(playerPage&&playback!=null){if(!dragging){timeline.setMax(Math.max(1,playback.duration()));timeline.setProgress(playback.position());}time.setText(clock(playback.position()));totalTime.setText(clock(playback.duration()));}handler.postDelayed(this,500);}};
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
  private IconButton icon(String glyph,String label,int color,boolean ring,Runnable action){return new IconButton(this,glyph,label,color,ring,()->{action.run();scheduleHide();});}
 private void addIcon(LinearLayout parent,IconButton icon,int size){parent.addView(icon,new LinearLayout.LayoutParams(dp(size),dp(size)));}
 private void spacer(LinearLayout parent){parent.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));}
 private void setControlsVisible(boolean visible){if(controls==null)return;controls.setVisibility(visible?View.VISIBLE:View.GONE);if(playerHeader!=null)playerHeader.setVisibility(visible?View.VISIBLE:View.GONE);}
 private void toggleFullscreen(){fullscreen=!fullscreen;getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);setRequestedOrientation(fullscreen?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE:android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);if(fullscreenButton!=null)fullscreenButton.setGlyph(fullscreen?"exit_fullscreen":"fullscreen",fullscreen?"Salir de pantalla completa":"Pantalla completa");}
 private void playerOptions(){handler.removeCallbacks(hideControls);PopupMenu menu=new PopupMenu(this,optionsButton);menu.getMenu().add("Volumen y amplificación").setOnMenuItemClickListener(i->{volumeDialog();return true;});menu.getMenu().add("Segundos del doble toque").setOnMenuItemClickListener(i->{seekSettings();return true;});menu.getMenu().add("Ajustes").setOnMenuItemClickListener(i->{settings();return true;});menu.setOnDismissListener(m->scheduleHide());menu.show();}
 private void base(){palette();root=column();root.setBackgroundColor(bg);root.setPadding(dp(20),dp(16),dp(20),dp(12));root.setFitsSystemWindows(true);setContentView(root);}
 private void showLibrary(){
  playerPage=false;fullscreen=false;WindowManager.LayoutParams windowParams=getWindow().getAttributes();windowParams.screenBrightness=-1;getWindow().setAttributes(windowParams);getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  if(playback!=null)playback.setSurface(null);base();
  LinearLayout top=row();TextView logo=text("Prisma Video",26,fg);logo.setTypeface(null,Typeface.BOLD);top.addView(logo,new LinearLayout.LayoutParams(0,dp(56),1));addIcon(top,icon("settings","Ajustes",accent,false,this::settings),48);root.addView(top);
  TextView subtitle=text("Tus vídeos. Sin distracciones.",14,muted);root.addView(subtitle);
  search=new EditText(this);search.setSingleLine(true);search.setTextColor(fg);search.setHintTextColor(muted);search.setHint("Buscar vídeos");search.setTextSize(16);search.setPadding(dp(16),0,dp(16),0);search.setBackground(shape(panel,16));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(52));sp.setMargins(0,dp(24),0,dp(12));root.addView(search,sp);
  search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){renderRows();}public void afterTextChanged(android.text.Editable e){}});
  LinearLayout tools=row();spacer(tools);addIcon(tools,icon("refresh","Actualizar biblioteca",accent,false,this::permission),48);addIcon(tools,icon("folder","Abrir archivo de vídeo",accent,false,this::pickFile),48);root.addView(tools);
  ListView list=new ListView(this);list.setDivider(null);list.setAdapter(adapter);list.setOnItemClickListener((p,v,pos,id)->startVideo(new ArrayList<>(visible),pos));root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
  rows=column();root.addView(rows);renderRows();scan();
 }
 private final BaseAdapter adapter=new BaseAdapter(){
  public int getCount(){return visible.size();}public Object getItem(int pos){return visible.get(pos);}public long getItemId(int pos){return pos;}
  public View getView(int pos,View old,android.view.ViewGroup parent){VideoItem video=visible.get(pos);LinearLayout r=row();r.setPadding(0,dp(13),0,dp(13));TextView icon=text("▶",22,accent);icon.setGravity(Gravity.CENTER);icon.setBackground(shape(panel,14));r.addView(icon,new LinearLayout.LayoutParams(dp(52),dp(52)));LinearLayout details=column();details.setPadding(dp(14),0,0,0);TextView name=text(video.title,16,fg);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);details.addView(name);details.addView(text(clock(video.duration)+"  ·  "+video.folder,12,muted));r.addView(details,new LinearLayout.LayoutParams(0,-2,1));return r;}
 };
 private void renderRows(){if(playerPage||search==null||rows==null)return;String q=search.getText().toString().toLowerCase(Locale.ROOT);visible.clear();for(VideoItem v:videos)if(!blocked(v.folder)&&v.title.toLowerCase(Locale.ROOT).contains(q))visible.add(v);adapter.notifyDataSetChanged();rows.removeAllViews();
  if(visible.isEmpty())rows.addView(text("No hay vídeos visibles. Revisa los permisos y las carpetas excluidas.",14,muted));
  if(playback!=null&&playback.current()!=null){LinearLayout resume=row();resume.setBackground(shape(panel,14));TextView name=text(playback.current().title,14,fg);name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setPadding(dp(14),0,dp(10),0);resume.addView(name,new LinearLayout.LayoutParams(0,dp(52),1));addIcon(resume,icon("play","Continuar reproducción",accent,false,this::showPlayer),48);resume.setOnClickListener(v->showPlayer());rows.addView(resume);}
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
 private void showPlayer(){
  if(playback==null||playback.current()==null)return;
  playerPage=true;base();root.setPadding(0,0,0,0);root.setFitsSystemWindows(false);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
  FrameLayout playerFrame=new FrameLayout(this);root.addView(playerFrame,new LinearLayout.LayoutParams(-1,-1));
  stage=new FrameLayout(this);stage.setBackgroundColor(Color.BLACK);playerFrame.addView(stage,new FrameLayout.LayoutParams(-1,-1));
  surface=new SurfaceView(this);stage.addView(surface,new FrameLayout.LayoutParams(-1,-1));
  final SurfaceView attachedSurface=surface;
  surface.getHolder().addCallback(new SurfaceHolder.Callback(){
   public void surfaceCreated(SurfaceHolder h){if(surface!=attachedSurface||!playerPage)return;playback.setSurface(h.getSurface());stage.post(()->fitVideo());}
   public void surfaceChanged(SurfaceHolder h,int f,int w,int height){if(surface==attachedSurface&&playerPage)fitVideo();}
   public void surfaceDestroyed(SurfaceHolder h){if(playback!=null)playback.clearSurface(h.getSurface());}
  });
  stage.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(surface==attachedSurface&&playerPage&&(r-l!=or-ol||b-t!=ob-ot))fitVideo();});
  audioLabel=text("Solo audio",22,Color.WHITE);audioLabel.setGravity(Gravity.CENTER);stage.addView(audioLabel,new FrameLayout.LayoutParams(-1,-1));
  feedback=text("",18,Color.WHITE);feedback.setGravity(Gravity.CENTER);feedback.setBackground(shape(0x99000000,20));stage.addView(feedback,new FrameLayout.LayoutParams(-2,dp(52),Gravity.CENTER));feedback.setVisibility(View.GONE);
  View touch=new View(this);stage.addView(touch,new FrameLayout.LayoutParams(-1,-1));gestures(touch);
  playerHeader=row();playerHeader.setPadding(dp(12),dp(8),dp(12),dp(18));
  playerHeader.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xA6000000,0x00000000}));
  addIcon(playerHeader,icon("back","Volver a la biblioteca",Color.WHITE,false,()->{setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);showLibrary();}),48);
  title=text(playback.current().title,17,Color.WHITE);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setGravity(Gravity.CENTER_VERTICAL);title.setPadding(dp(12),0,dp(12),0);playerHeader.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
  playerHeader.addView(new View(this),new LinearLayout.LayoutParams(dp(48),dp(48)));
  playerFrame.addView(playerHeader,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
  controls=column();controls.setPadding(dp(12),dp(22),dp(12),dp(10));
  controls.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00000000,0x99000000,0xC4000000}));playerFrame.addView(controls,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
  LinearLayout times=row();time=text(clock(playback.position()),12,Color.WHITE);totalTime=text(clock(playback.duration()),12,Color.WHITE);times.addView(time);spacer(times);times.addView(totalTime);controls.addView(times);
  timeline=new SeekBar(this);timeline.setPadding(dp(2),0,dp(2),0);
  GradientDrawable empty=new GradientDrawable();empty.setColor(0x55FFFFFF);empty.setCornerRadius(dp(2));
  GradientDrawable progress=new GradientDrawable();progress.setColor(accent);progress.setCornerRadius(dp(2));
  android.graphics.drawable.ClipDrawable clip=new android.graphics.drawable.ClipDrawable(progress,Gravity.LEFT,android.graphics.drawable.ClipDrawable.HORIZONTAL);
  android.graphics.drawable.LayerDrawable track=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{empty,clip});track.setId(0,android.R.id.background);track.setId(1,android.R.id.progress);timeline.setProgressDrawable(track);timeline.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));timeline.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0x55FFFFFF));timeline.setMinHeight(dp(3));timeline.setMaxHeight(dp(3));
  GradientDrawable thumb=new GradientDrawable();thumb.setShape(GradientDrawable.OVAL);thumb.setColor(accent);thumb.setSize(dp(12),dp(12));timeline.setThumb(thumb);timeline.setThumbOffset(dp(6));controls.addView(timeline,new LinearLayout.LayoutParams(-1,dp(32)));
  timeline.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){if(user)time.setText(clock(p));}public void onStartTrackingTouch(SeekBar s){dragging=true;handler.removeCallbacks(hideControls);}public void onStopTrackingTouch(SeekBar s){playback.seek(s.getProgress());dragging=false;scheduleHide();}});
  LinearLayout toolbar=row();int target=getResources().getConfiguration().screenWidthDp<360?40:44;
  modeButton=icon("repeat","Repetir todos",Color.WHITE,false,()->playback.setMode((playback.mode+1)%3));addIcon(toolbar,modeButton,target);
  audioButton=icon("headphones","Reproducir solo audio",Color.WHITE,false,()->playback.setAudioOnly(!playback.audioOnly));addIcon(toolbar,audioButton,target);
  spacer(toolbar);
  addIcon(toolbar,icon("previous","Vídeo anterior",Color.WHITE,false,playback::previous),target);
  playButton=icon("play","Reproducir",Color.WHITE,true,playback::toggle);addIcon(toolbar,playButton,target==40?52:58);
  addIcon(toolbar,icon("next","Vídeo siguiente",Color.WHITE,false,playback::next),target);
  spacer(toolbar);
  fullscreenButton=icon(fullscreen?"exit_fullscreen":"fullscreen","Pantalla completa",Color.WHITE,false,this::toggleFullscreen);addIcon(toolbar,fullscreenButton,target);
  optionsButton=icon("more","Más opciones de reproducción",Color.WHITE,false,this::playerOptions);addIcon(toolbar,optionsButton,target);
  controls.addView(toolbar);
  playerFrame.setOnApplyWindowInsetsListener((v,insets)->{
   int left=0,right=0,top=0,bottom=0;
   if(Build.VERSION.SDK_INT>=28&&insets.getDisplayCutout()!=null){android.view.DisplayCutout cutout=insets.getDisplayCutout();left=cutout.getSafeInsetLeft();right=cutout.getSafeInsetRight();top=cutout.getSafeInsetTop();bottom=cutout.getSafeInsetBottom();}
   playerHeader.setPadding(left+dp(12),top+dp(8),right+dp(12),dp(18));controls.setPadding(left+dp(12),dp(22),right+dp(12),bottom+dp(10));return insets;
  });playerFrame.requestApplyInsets();changed();scheduleHide();
 }
 private void fitVideo(){if(surface==null||stage==null||playback==null||!playback.prepared)return;int w=playback.player.getVideoWidth(),h=playback.player.getVideoHeight();if(w<=0||h<=0)return;int sw=stage.getWidth(),sh=stage.getHeight();if(sw<=0||sh<=0)return;double ratio=Math.min((double)sw/w,(double)sh/h);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams((int)(w*ratio),(int)(h*ratio),Gravity.CENTER);if(surface.getLayoutParams().width!=p.width||surface.getLayoutParams().height!=p.height)surface.setLayoutParams(p);}
 private void scheduleHide(){handler.removeCallbacks(hideControls);handler.postDelayed(hideControls,4000);}
 private int step(){return prefs.getInt("seekSeconds",5);}
 private void hud(String value){feedback.setText("  "+value+"  ");feedback.setVisibility(View.VISIBLE);handler.removeCallbacks(hideFeedback);handler.postDelayed(hideFeedback,1200);}
 private void gestures(View touch){
  GestureDetector detector=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
   public boolean onDown(MotionEvent e){return true;}
   public boolean onSingleTapConfirmed(MotionEvent e){setControlsVisible(controls.getVisibility()!=View.VISIBLE);scheduleHide();return true;}
   public boolean onDoubleTap(MotionEvent e){float fraction=e.getX()/Math.max(1,stage.getWidth());if(fraction>.30f&&fraction<.70f)return true;long delta=(fraction<=.30f?-1:1)*step()*1000L;playback.seek(playback.position()+delta);hud((delta<0?"−":"+")+step()+" s");return true;}
  });
  touch.setOnTouchListener(new View.OnTouchListener(){float x,y,brightness;int startPosition,startVolume,direction;boolean moving;
   public boolean onTouch(View v,MotionEvent e){
    if(e.getAction()==MotionEvent.ACTION_DOWN){x=e.getX();y=e.getY();startPosition=playback.position();startVolume=playback.volume;brightness=getWindow().getAttributes().screenBrightness;if(brightness<0)try{brightness=android.provider.Settings.System.getInt(getContentResolver(),android.provider.Settings.System.SCREEN_BRIGHTNESS)/255f;}catch(Exception ex){brightness=.5f;}moving=false;direction=0;handler.removeCallbacks(hideControls);}
    if(e.getAction()==MotionEvent.ACTION_MOVE){float dx=e.getX()-x,dy=y-e.getY();if(!moving&&Math.max(Math.abs(dx),Math.abs(dy))>dp(24)){moving=true;direction=Math.abs(dx)>Math.abs(dy)?1:2;MotionEvent cancel=MotionEvent.obtain(e.getDownTime(),e.getEventTime(),MotionEvent.ACTION_CANCEL,e.getX(),e.getY(),0);detector.onTouchEvent(cancel);cancel.recycle();}
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
 private void seekSettings(){EditText input=new EditText(this);input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);input.setText(String.valueOf(step()));input.setSelectAllOnFocus(true);AlertDialog d=new AlertDialog.Builder(this).setTitle("Segundos por salto").setMessage("Elige de 1 a 120 segundos para el doble toque en los extremos.").setView(input).setPositiveButton("Guardar",null).setNegativeButton("Cancelar",null).create();d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{int value=Integer.parseInt(input.getText().toString());if(value<1||value>120)throw new NumberFormatException();prefs.edit().putInt("seekSeconds",value).apply();d.dismiss();if(playerPage)showPlayer();}catch(NumberFormatException e){input.setError("Introduce un número de 1 a 120");}}));d.show();}
 private void folders(){TreeSet<String> paths=new TreeSet<>(excluded);for(VideoItem v:videos)if(!v.folder.isEmpty())paths.add(v.folder);String[] names=paths.toArray(new String[0]);boolean[] checked=new boolean[names.length];for(int i=0;i<names.length;i++)checked[i]=excluded.contains(names[i]);new AlertDialog.Builder(this,night?AlertDialog.THEME_DEVICE_DEFAULT_DARK:AlertDialog.THEME_DEVICE_DEFAULT_LIGHT).setTitle("Excluir carpetas y subcarpetas").setMultiChoiceItems(names,checked,(d,i,c)->{if(c)excluded.add(names[i]);else excluded.remove(names[i]);saveFolders();}).setPositiveButton("Listo",(d,w)->renderRows()).setNeutralButton("Añadir ruta",(d,w)->manualFolder()).setNegativeButton("Cerrar",null).show();}
 private void manualFolder(){EditText input=new EditText(this);input.setSingleLine();input.setHint("Movies/Privados");input.setPadding(dp(24),dp(16),dp(24),dp(16));AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Ruta de carpeta").setMessage("Escribe la ruta relativa al almacenamiento, por ejemplo Movies/Privados. Incluye sus subcarpetas.").setView(input).setPositiveButton("Excluir",null).setNegativeButton("Cancelar",null).create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String path=normalize(input.getText().toString());if(path.isEmpty()||path.contains("..")){input.setError("Escribe una carpeta válida");return;}excluded.add(path);saveFolders();dialog.dismiss();folders();}));dialog.show();}
 private void saveFolders(){prefs.edit().putStringSet("excluded",new HashSet<>(excluded)).apply();renderRows();}
 private String clock(long ms){long sec=Math.max(0,ms)/1000;return sec>=3600?String.format(Locale.ROOT,"%d:%02d:%02d",sec/3600,sec/60%60,sec%60):String.format(Locale.ROOT,"%d:%02d",sec/60,sec%60);}
 private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
 @Override public void changed(){if(playerPage&&playback!=null){if(playback.current()==null){showLibrary();return;}title.setText(playback.current().title);playButton.setGlyph(playback.playing()?"pause":"play",playback.playing()?"Pausar":"Reproducir");modeButton.setGlyph(new String[]{"repeat","repeat_one","shuffle"}[playback.mode],new String[]{"Repetir todos","Repetir un vídeo","Reproducción aleatoria"}[playback.mode]);audioButton.setGlyph(playback.audioOnly?"video":"headphones",playback.audioOnly?"Ver vídeo":"Reproducir solo audio");audioLabel.setVisibility(playback.audioOnly?View.VISIBLE:View.GONE);surface.setVisibility(playback.audioOnly?View.INVISIBLE:View.VISIBLE);fitVideo();if(!playback.playing())setControlsVisible(true);}else renderRows();}
 @Override public void error(String message){toast(message);}
 @Override public void onBackPressed(){if(playerPage){setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);showLibrary();}else super.onBackPressed();}
 @Override protected void onStop(){super.onStop();stopped=true;if(playback!=null&&!playback.audioOnly)playback.pause();}
 @Override protected void onResume(){super.onResume();if(stopped){stopped=false;scan();}}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putBoolean("playerPage",playerPage);out.putBoolean("fullscreen",fullscreen);}
 @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);if(playerPage)showPlayer();else showLibrary();}
 @Override protected void onDestroy(){scanToken++;handler.removeCallbacksAndMessages(null);scanner.shutdownNow();if(playback!=null){playback.listener=null;playback.setSurface(null);}if(bound)unbindService(connection);super.onDestroy();}
}
