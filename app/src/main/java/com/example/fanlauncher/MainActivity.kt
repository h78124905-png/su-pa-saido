package com.example.fanlauncher
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
        val tg=RadioGroup(this);val one=RadioButton(this).apply{text="1段";id=2001};val two=RadioButton(this).apply{text="2段";id=2002};tg.addView(one);tg.addView(two);(if(s.tiers==1)one else two).isChecked=true
        tg.setOnCheckedChangeListener{_,id->s=s.copy(tiers=if(id==2001)1 else 2);FanSettings.save(this,s);OverlayService.instance?.applySettings()};root.addView(tg)
        val c1=labelWithValue("1段目アプリ数",s.tier1Count.toString());root.addView(c1);root.addView(seekBar(1,8,s.tier1Count){v->s=s.copy(tier1Count=v);FanSettings.save(this,s);c1.text="1段目アプリ数: ${v}";OverlayService.instance?.applySettings()})
        val c2=labelWithValue("2段目アプリ数",s.tier2Count.toString());root.addView(c2);root.addView(seekBar(1,8,s.tier2Count){v->s=s.copy(tier2Count=v);FanSettings.save(this,s);c2.text="2段目アプリ数: ${v}";OverlayService.instance?.applySettings()})
        root.addView(label("アプリ配置"));root.addView(FanSlotPickerView(this,s.tier1Count,s.tier1Apps){p->s=s.copy(tier1Apps=p);FanSettings.save(this,s);OverlayService.instance?.applySettings()})
        if(s.tiers==2)root.addView(FanSlotPickerView(this,s.tier2Count,s.tier2Apps){p->s=s.copy(tier2Apps=p);FanSettings.save(this,s);OverlayService.instance?.applySettings()})
        root.addView(checkBox("スクショボタン表示",s.showScreenshot){s=s.copy(showScreenshot=it);FanSettings.save(this,s);OverlayService.instance?.applySettings()})
        root.addView(checkBox("翻訳ボタン表示",s.showTranslate){s=s.copy(showTranslate=it);FanSettings.save(this,s);OverlayService.instance?.applySettings()})
        root.addView(button("設定を反映（サービス再起動）"){stopService(Intent(this,OverlayService::class.java));ContextCompat.startForegroundService(this,Intent(this,OverlayService::class.java));Toast.makeText(this,"設定を反映しました",Toast.LENGTH_SHORT).show()})
    }
    private fun addPermissionRow(p:LinearLayout,n:String,g:Boolean,a:()->Unit){p.addView(TextView(this).apply{text=(if(g) "✅ " else "❌ ")+n;setPadding(0,dp(10),0,dp(10));setOnClickListener{a()}})}
    private fun title(t:String)=TextView(this).apply{text=t;textSize=28f;setTextColor(Color.BLACK);setPadding(0,0,0,dp(20))}
    private fun label(t:String)=TextView(this).apply{text=t;textSize=18f;setTextColor(Color.rgb(25,118,210));setPadding(0,dp(28),0,dp(8))}
    private fun labelWithValue(n:String,v:String)=TextView(this).apply{text="$n: $v";setPadding(0,dp(10),0,dp(4))}
    private fun button(t:String,a:()->Unit)=Button(this).apply{text=t;setOnClickListener{a()}}
    private fun checkBox(t:String,c:Boolean,a:(Boolean)->Unit)=CheckBox(this).apply{text=t;isChecked=c;setOnCheckedChangeListener{_,v->a(v)}}
    private fun seekBar(min:Int,max:Int,current:Int,a:(Int)->Unit)=SeekBar(this).apply{this.max=max-min;progress=(current-min).coerceIn(0,max-min);setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(sb:SeekBar?,p:Int,fromUser:Boolean){if(fromUser)a(p+min)};override fun onStartTrackingTouch(sb:SeekBar?)=Unit;override fun onStopTrackingTouch(sb:SeekBar?)=Unit})}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun isAccessibilityEnabled():Boolean{return ScreenshotAccessibilityService.instance!=null}
}
class FanSlotPickerView(context:android.content.Context,var slotCount:Int,var selected:List<String>,val onChanged:(List<String>)->Unit):LinearLayout(context){private val apps=context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0).map{it.activityInfo.applicationInfo}.distinctBy{it.packageName}.filter{it.packageName!=context.packageName}.sortedBy{context.packageManager.getApplicationLabel(it).toString()};init{orientation=HORIZONTAL;setPadding(0,8,0,8);rebuild()};fun setCount(n:Int){slotCount=n;rebuild()};private fun rebuild(){removeAllViews();for(i in 0 until slotCount){val b=Button(context);b.text=selected.getOrNull(i)?.let{context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(it,0)).toString()}?:"＋ アプリ";b.setOnClickListener{val names=apps.map{context.packageManager.getApplicationLabel(it).toString()};AlertDialog.Builder(context).setTitle("スロット "+(i+1)).setItems(names.toTypedArray()){_,which->val p=selected.toMutableList();while(p.size<=i)p.add("");p[i]=apps[which].packageName;selected=p;onChanged(p.filter{it.isNotEmpty()});rebuild()}.setNegativeButton("キャンセル",null).show()};addView(b,LinearLayout.LayoutParams(0,dp(72),1f))}};private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()}
