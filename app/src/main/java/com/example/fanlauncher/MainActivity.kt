package com.example.fanlauncher
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private lateinit var s: FanSettings
    private lateinit var picker1: FanSlotPickerView
    private lateinit var picker2: FanSlotPickerView
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); buildSettingsHome() }
    private fun buildSettingsHome() {
        s=FanSettings.load(this)
        val scroll=ScrollView(this)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(24),dp(24),dp(24),dp(40))}
        scroll.addView(root);setContentView(scroll)
        root.addView(title("FanLauncher"));root.addView(label("権限ステータス"))
        addPermissionRow(root,"オーバーレイ",Settings.canDrawOverlays(this)){startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))}
        addPermissionRow(root,"アクセシビリティ",isAccessibilityEnabled()){startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}
        if(Build.VERSION.SDK_INT>=33)addPermissionRow(root,"通知",checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),100)}
        root.addView(label("サービス"))
        val status=TextView(this).apply{setPadding(0,dp(8),0,dp(8))};root.addView(status)
        fun refresh(){val running=OverlayService.isRunning;status.text=if(running) "OverlayService: 稼働中" else "OverlayService: 停止中";status.setTextColor(if(running) Color.rgb(0,120,70) else Color.RED)}
        refresh()
        root.addView(button("サービスを開始 / 再起動"){stopService(Intent(this,OverlayService::class.java));ContextCompat.startForegroundService(this,Intent(this,OverlayService::class.java));status.postDelayed(::refresh,500)})
        root.addView(label("エッジ設定"))
        val ew=labelWithValue("エッジ幅","${s.edgeWidthDp}dp");root.addView(ew);root.addView(seekBar(16,96,s.edgeWidthDp){v->s=s.copy(edgeWidthDp=v);FanSettings.save(this,s);ew.text="エッジ幅: ${v}dp";OverlayService.instance?.applySettings()})
        val top=labelWithValue("上端","${(s.edgeTopRatio*100).toInt()}%");root.addView(top);root.addView(seekBar(0,100,(s.edgeTopRatio*100).toInt()){v->val x=v/100f;if(x<s.edgeBottomRatio){s=s.copy(edgeTopRatio=x);FanSettings.save(this,s);top.text="上端: ${v}%";OverlayService.instance?.applySettings()}})
        val bottom=labelWithValue("下端","${(s.edgeBottomRatio*100).toInt()}%");root.addView(bottom);root.addView(seekBar(0,100,(s.edgeBottomRatio*100).toInt()){v->val x=v/100f;if(x>s.edgeTopRatio){s=s.copy(edgeBottomRatio=x);FanSettings.save(this,s);bottom.text="下端: ${v}%";OverlayService.instance?.applySettings()}})
        val st=labelWithValue("スワイプ閾値","${s.swipeThresholdDp}dp");root.addView(st);root.addView(seekBar(8,64,s.swipeThresholdDp){v->s=s.copy(swipeThresholdDp=v);FanSettings.save(this,s);st.text="スワイプ閾値: ${v}dp";OverlayService.instance?.applySettings()})
        root.addView(label("反応するエッジ"))
        val eg=RadioGroup(this);val left=RadioButton(this).apply{text="左";id=1001};val right=RadioButton(this).apply{text="右";id=1002};eg.addView(left);eg.addView(right);(if(s.edge==EdgeSide.LEFT)left else right).isChecked=true
        eg.setOnCheckedChangeListener{_,id->s=s.copy(edge=if(id==1001)EdgeSide.LEFT else EdgeSide.RIGHT);FanSettings.save(this,s);OverlayService.instance?.applySettings()};root.addView(eg)
        root.addView(checkBox("エッジ判定位置を表示",s.showEdgeIndicator){s=s.copy(showEdgeIndicator=it);FanSettings.save(this,s);OverlayService.instance?.applySettings()})
        root.addView(label("扇設定"))
        val sweep=labelWithValue("扇の開き角","${s.fanSweepDeg}°");root.addView(sweep);root.addView(seekBar(90,180,s.fanSweepDeg){v->s=s.copy(fanSweepDeg=v);FanSettings.save(this,s);sweep.text="扇の開き角: ${v}°";picker1.updateGeometry(s);picker2.updateGeometry(s);OverlayService.instance?.applySettings()})
        val radius=labelWithValue("扇のサイズ","${s.fanRadiusDp}dp");root.addView(radius);root.addView(seekBar(160,420,s.fanRadiusDp){v->s=s.copy(fanRadiusDp=v);FanSettings.save(this,s);radius.text="扇のサイズ: ${v}dp";picker1.updateGeometry(s);picker2.updateGeometry(s);OverlayService.instance?.applySettings()})
        val tg=RadioGroup(this);val one=RadioButton(this).apply{text="1段";id=2001};val two=RadioButton(this).apply{text="2段";id=2002};tg.addView(one);tg.addView(two);(if(s.tiers==1)one else two).isChecked=true
        tg.setOnCheckedChangeListener{_,id->s=s.copy(tiers=if(id==2001)1 else 2);FanSettings.save(this,s);OverlayService.instance?.applySettings()};root.addView(tg)
        val c1=labelWithValue("1段目アプリ数",s.tier1Count.toString());root.addView(c1);root.addView(seekBar(1,16,s.tier1Count){v->s=s.copy(tier1Count=v);FanSettings.save(this,s);c1.text="1段目アプリ数: ${v}";OverlayService.instance?.applySettings()})
        val c2=labelWithValue("2段目アプリ数",s.tier2Count.toString());root.addView(c2);root.addView(seekBar(1,16,s.tier2Count){v->s=s.copy(tier2Count=v);FanSettings.save(this,s);c2.text="2段目アプリ数: ${v}";OverlayService.instance?.applySettings()})
        root.addView(label("アプリ配置"))
        picker1=FanSlotPickerView(this,s,1,s.tier1Count,s.tier1Apps){p->s=s.copy(tier1Apps=p);FanSettings.save(this,s);OverlayService.instance?.applySettings()}
        picker2=FanSlotPickerView(this,s,2,s.tier2Count,s.tier2Apps){p->s=s.copy(tier2Apps=p);FanSettings.save(this,s);OverlayService.instance?.applySettings()}
        root.addView(picker1,LinearLayout.LayoutParams(-1,dp(300)));root.addView(picker2,LinearLayout.LayoutParams(-1,dp(300)));picker2.visibility=if(s.tiers==2)View.VISIBLE else View.GONE
        root.addView(checkBox("スクショボタン表示",s.showScreenshot){s=s.copy(showScreenshot=it);FanSettings.save(this,s);OverlayService.instance?.applySettings()})
        root.addView(checkBox("翻訳ボタン表示",s.showTranslate){s=s.copy(showTranslate=it);FanSettings.save(this,s);OverlayService.instance?.applySettings()})
        root.addView(TextView(this).apply{text="設定は変更した瞬間に保存・反映されます。";setTextColor(Color.DKGRAY);setPadding(0,dp(12),0,0)})
    }
    private fun addPermissionRow(p:LinearLayout,n:String,g:Boolean,a:()->Unit){p.addView(TextView(this).apply{text=(if(g) "✅ " else "❌ ")+n;setPadding(0,dp(10),0,dp(10));setOnClickListener{a()}})}
    private fun title(t:String)=TextView(this).apply{text=t;textSize=28f;setTextColor(Color.BLACK);setPadding(0,0,0,dp(20))}
    private fun label(t:String)=TextView(this).apply{text=t;textSize=18f;setTextColor(Color.rgb(25,118,210));setPadding(0,dp(28),0,dp(8))}
    private fun labelWithValue(n:String,v:String)=TextView(this).apply{text="$n: $v";setPadding(0,dp(10),0,dp(4))}
    private fun button(t:String,a:()->Unit)=Button(this).apply{text=t;setOnClickListener{a()}}
    private fun checkBox(t:String,c:Boolean,a:(Boolean)->Unit)=CheckBox(this).apply{text=t;isChecked=c;setOnCheckedChangeListener{_,v->a(v)}}
    private fun seekBar(min:Int,max:Int,current:Int,a:(Int)->Unit)=SeekBar(this).apply{this.max=max-min;progress=(current-min).coerceIn(0,max-min);setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(sb:SeekBar?,p:Int,fromUser:Boolean){if(fromUser)a(p+min)};override fun onStartTrackingTouch(sb:SeekBar?)=Unit;override fun onStopTrackingTouch(sb:SeekBar?)=Unit})}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun isAccessibilityEnabled():Boolean{
        val enabled=Settings.Secure.getString(contentResolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)?:return false
        val target=ComponentName(this,ScreenshotAccessibilityService::class.java).flattenToString()
        return enabled.split(':').any{it.equals(target,true)}
    }
}
class FanSlotPickerView(context:android.content.Context,private var settings:FanSettings,private val tier:Int,private var slotCount:Int,private var selected:List<String>,private val onChanged:(List<String>)->Unit):View(context){
 private val pm=context.packageManager
 private val apps=pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0).map{it.activityInfo.applicationInfo}.distinctBy{it.packageName}.filter{it.packageName!=context.packageName}.sortedBy{pm.getApplicationLabel(it).toString()}
 private val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);private var vertexX=0f;private var vertexY=0f;private var selectedSlot=-1
 init{isClickable=true}
 fun updateGeometry(newSettings:FanSettings){settings=newSettings;invalidate()}
 fun setSlotCount(n:Int){slotCount=n.coerceIn(1,16);invalidate()}
 override fun onSizeChanged(w:Int,h:Int,ow:Int,oh:Int){vertexX=if(settings.edge==EdgeSide.RIGHT)w-dp(18)else dp(18).toFloat();vertexY=h*.52f}
 override fun onDraw(c:android.graphics.Canvas){val radius=settings.fanRadiusDp*resources.displayMetrics.density;val sweep=settings.fanSweepDeg.toFloat();val start=if(settings.edge==EdgeSide.RIGHT)180f else 270f;val inner=dp(38).toFloat();val per=sweep/slotCount;for(i in 0 until slotCount){val a=start+i*per;paint.color=selected.getOrNull(i)?.let{appColor(it)}?:0xFFE0E0E0.toInt();paint.alpha=if(selectedSlot==i)255 else 190;val path=android.graphics.Path().apply{moveTo(vertexX+inner*cos(Math.toRadians(a.toDouble())).toFloat(),vertexY+inner*sin(Math.toRadians(a.toDouble())).toFloat());arcTo(android.graphics.RectF(vertexX-inner,vertexY-inner,vertexX+inner,vertexY+inner),a,per);lineTo(vertexX+radius*cos(Math.toRadians((a+per).toDouble())).toFloat(),vertexY+radius*sin(Math.toRadians((a+per).toDouble())).toFloat());arcTo(android.graphics.RectF(vertexX-radius,vertexY-radius,vertexX+radius,vertexY+radius),a+per,-per);close()};c.drawPath(path,paint);selected.getOrNull(i)?.let{pkg->runCatching{val icon=pm.getApplicationIcon(pkg);val mid=Math.toRadians((a+per/2).toDouble());val ir=(inner+radius)/2f;val x=vertexX+ir*cos(mid).toFloat();val y=vertexY+ir*sin(mid).toFloat();val size=dp(38).toFloat();icon.setBounds((x-size/2).toInt(),(y-size/2).toInt(),(x+size/2).toInt(),(y+size/2).toInt());icon.draw(c)}}};paint.color=0xAAFFFFFF.toInt();c.drawCircle(vertexX,vertexY,dp(34).toFloat(),paint);paint.color=Color.DKGRAY;paint.textAlign=android.graphics.Paint.Align.CENTER;paint.textSize=dp(12).toFloat();c.drawText("タップして配置",vertexX,vertexY+dp(4).toFloat(),paint)}
 override fun onTouchEvent(e:android.view.MotionEvent):Boolean{if(e.action!=android.view.MotionEvent.ACTION_UP)return true;val dx=e.x-vertexX;val dy=e.y-vertexY;val dist=hypot(dx,dy);val radius=settings.fanRadiusDp*resources.displayMetrics.density;if(dist>radius)return true;var angle=Math.toDegrees(atan2(dy.toDouble(),dx.toDouble())).toFloat();if(angle<0)angle+=360f;val start=if(settings.edge==EdgeSide.RIGHT)180f else 270f;var n=angle-start;if(n<0)n+=360f;if(n !in 0f..settings.fanSweepDeg.toFloat())return true;selectedSlot=(n/(settings.fanSweepDeg.toFloat()/slotCount)).toInt().coerceIn(0,slotCount-1);showAppDialog(selectedSlot);return true}
 private fun showAppDialog(index:Int){val rows=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL};addChoice(rows,index,null,"未設定");for(info in apps)addChoice(rows,index,info.packageName,pm.getApplicationLabel(info).toString(),pm.getApplicationIcon(info));AlertDialog.Builder(context).setTitle("スロット "+(index+1)).setView(ScrollView(context).apply{addView(rows)}).setNegativeButton("閉じる",null).show()}
 private fun addChoice(parent:LinearLayout,index:Int,pkg:String?,label:String,icon:Drawable?=null){val row=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(8),dp(12),dp(8))};if(icon!=null){val iv=ImageView(context).apply{setImageDrawable(icon);layoutParams=LinearLayout.LayoutParams(dp(44),dp(44))};row.addView(iv)};row.addView(TextView(context).apply{text=label;textSize=16f;setPadding(dp(12),0,0,0);layoutParams=LinearLayout.LayoutParams(0,-2,1f)});row.setOnClickListener{val p=selected.toMutableList();while(p.size<=index)p.add("");p[index]=pkg?:"";selected=p;onChanged(p);invalidate();(parent.parent?.parent as?android.app.Dialog)?.dismiss()};parent.addView(row,LinearLayout.LayoutParams(-1,dp(60)))}
 private fun appColor(pkg:String):Int=0xFFE0E0E0.toInt()
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
