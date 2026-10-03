package com.example.fanlauncher
import android.accessibilityservice.AccessibilityService
import android.app.*;import android.content.Intent;import android.content.pm.ServiceInfo;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.Toast;import androidx.core.app.NotificationCompat;import kotlinx.coroutines.*
class OverlayService:Service(){
 private lateinit var wm:WindowManager;private var edgeView:EdgeOverlayView?=null;private var fanView:FanLauncherView?=null;private val scope=CoroutineScope(Dispatchers.Main.immediate+SupervisorJob())
 companion object{@Volatile var isRunning=false;private set;var instance:OverlayService?=null;private set}
 override fun onCreate(){super.onCreate();instance=this;try{if(Build.VERSION.SDK_INT>=34)startForeground(1,buildNotification(),ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)else startForeground(1,buildNotification());wm=getSystemService(WINDOW_SERVICE)as WindowManager;setupOverlay();isRunning=true}catch(t:Throwable){isRunning=false;runCatching{java.io.File(filesDir,"service_crash.log").writeText(t.stackTraceToString())};stopSelf()}}
 fun applySettings(){if(!isRunning)return;runOnMainThread{runCatching{edgeView?.let{wm.removeView(it)};edgeView=null;hideFan();setupOverlay()}}}
 private fun runOnMainThread(block:()->Unit){if(Looper.myLooper()==Looper.getMainLooper())block()else Handler(Looper.getMainLooper()).post(block)}
 private fun setupOverlay(){val s=FanSettings.load(this);val d=resources.displayMetrics.density;val screenH=resources.displayMetrics.heightPixels;val top=(screenH*s.edgeTopRatio).toInt().coerceIn(0,screenH);val bottom=(screenH*s.edgeBottomRatio).toInt().coerceIn(top,screenH);val p=WindowManager.LayoutParams((s.edgeWidthDp*d).toInt(),(bottom-top).coerceAtLeast(1),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply{gravity=(if(s.edge==EdgeSide.RIGHT)Gravity.END else Gravity.START) or Gravity.TOP;y=top};val e=EdgeOverlayView(this,s);e.onSwipeStart={x,y->val full=wmParams(e);full.height=screenH;full.y=0;e.setFullHeightMode(true);wm.updateViewLayout(e,full);showFan(x,y)};e.onSwipeMove={x,y->fanView?.updateSelection(x,y)};e.onSwipeEnd={fanView?.commitSelection();hideFan();restoreEdgeWindow(e,s)};wm.addView(e,p);edgeView=e}
 private fun showFan(x:Float,y:Float){if(fanView!=null)return;val s=FanSettings.load(this);val p=WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START};val f=FanLauncherView(this,s);f.onItemSelected={executeAction(it)};f.onActionSelected={executeAction(it)};wm.addView(f,p);fanView=f;scope.launch{f.tier1Items=buildItems(s.tier1Apps,s.tier1Count);f.tier2Items=if(s.tiers==2)buildItems(s.tier2Apps,s.tier2Count)else emptyList();f.setVertexY(y);f.updateSelection(x,y)}}
 private suspend fun buildItems(pkgs:List<String>,maxApps:Int):List<FanItem> = withContext(Dispatchers.Default){
  val pm=packageManager
  (0 until maxApps).map{index->
   val pkg=pkgs.getOrNull(index).orEmpty()
   if(pkg.isBlank()) FanItem("",null,0xFFE0E0E0.toInt(),FanAction.Empty)
   else if(pkg=="__CLOSE_FAN__") FanItem("アプリ一覧",pm.getDefaultActivityIcon(),0xFF616161.toInt(),FanAction.CloseFan)
   else runCatching{val i=pm.getApplicationInfo(pkg,0);FanItem(pm.getApplicationLabel(i).toString(),pm.getApplicationIcon(pkg),AppColorExtractor.extract(this@OverlayService,pkg),FanAction.LaunchApp(pkg))}.getOrElse{FanItem("",null,0xFFE0E0E0.toInt(),FanAction.Empty)}
  }
}
 private fun wmParams(v:View):WindowManager.LayoutParams = v.layoutParams as WindowManager.LayoutParams
 private fun restoreEdgeWindow(v:View,s:FanSettings){runCatching{(v as? EdgeOverlayView)?.setFullHeightMode(false);val d=resources.displayMetrics.density;val h=resources.displayMetrics.heightPixels;val top=(h*s.edgeTopRatio).toInt().coerceIn(0,h);val bottom=(h*s.edgeBottomRatio).toInt().coerceIn(top,h);val p=wmParams(v);p.height=(bottom-top).coerceAtLeast(1);p.y=top;wm.updateViewLayout(v,p)}}
 private fun hideFan(){fanView?.let{runCatching{wm.removeView(it)}};fanView=null}
 private fun executeAction(item:FanItem){executeAction(item.action)}
 private fun executeAction(a:FanAction){
     hideFan()
     when(a){
       is FanAction.LaunchApp->packageManager.getLaunchIntentForPackage(a.packageName)?.also{it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(it)}
       FanAction.Screenshot->{
         val service=ScreenshotAccessibilityService.instance
         if(service==null){Toast.makeText(this,"アクセシビリティを有効にしてください",Toast.LENGTH_SHORT).show()}
         else if(!service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)){
             Toast.makeText(this,"スクリーンショットを開始できませんでした",Toast.LENGTH_SHORT).show()
         }
       }
       FanAction.CloseFan->Unit
       FanAction.Empty->Unit
       FanAction.Translate->{
         val service=ScreenshotAccessibilityService.instance
         if(service==null){Toast.makeText(this,"アクセシビリティを有効にしてください",Toast.LENGTH_SHORT).show()}
         else scope.launch{
             delay(120)
             service.capture{b->if(b!=null)scope.launch{ScreenTranslator.translateAndOverlay(this@OverlayService,b,wm)}else Toast.makeText(this@OverlayService,"画面取得に失敗しました",Toast.LENGTH_SHORT).show()}
         }
       }
     }
 }
 private fun buildNotification():Notification{val c=NotificationChannel("fan","FanLauncher",NotificationManager.IMPORTANCE_LOW);(getSystemService(NOTIFICATION_SERVICE)as NotificationManager).createNotificationChannel(c);return NotificationCompat.Builder(this,"fan").setContentTitle("FanLauncher 稼働中").setContentText("エッジからスワイプでランチャーを開きます").setSmallIcon(android.R.drawable.ic_menu_compass).setOngoing(true).build()}
 override fun onStartCommand(i:Intent?,flags:Int,startId:Int)=START_STICKY
 override fun onDestroy(){isRunning=false;instance=null;edgeView?.let{runCatching{wm.removeView(it)}};hideFan();scope.cancel();super.onDestroy()}
 override fun onBind(i:Intent?):IBinder?=null
}
