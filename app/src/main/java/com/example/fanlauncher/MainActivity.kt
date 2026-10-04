package com.example.fanlauncher

import android.app.AlertDialog as AndroidAlertDialog
import android.graphics.*
import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import android.view.*
import android.widget.*
import kotlin.math.*
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FanLauncherSettings() }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun FanLauncherSettings() {
        val context = LocalContext.current
        var settings by remember { mutableStateOf(FanSettings.load(context)) }
        var serviceRunning by remember { mutableStateOf(OverlayService.isRunning) }
        var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
        var accessibilityGranted by remember { mutableStateOf(isAccessibilityEnabled()) }

        fun save(next: FanSettings) {
            settings = next
            FanSettings.save(context, next)
            OverlayService.instance?.applySettings()
        }

        LaunchedEffect(Unit) {
            while (true) {
                serviceRunning = OverlayService.isRunning
                overlayGranted = Settings.canDrawOverlays(context)
                accessibilityGranted = isAccessibilityEnabled()
                delay(800)
            }
        }

        MaterialTheme(
            colorScheme = expressiveDarkColors(),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(14.dp),
                medium = RoundedCornerShape(22.dp),
                large = RoundedCornerShape(30.dp),
                extraLarge = RoundedCornerShape(38.dp)
            ),
            typography = Typography(
                headlineMedium = TextStyle(fontWeight = FontWeight.Bold),
                titleLarge = TextStyle(fontWeight = FontWeight.SemiBold),
                bodyLarge = TextStyle(fontSize = 16.sp)
            )
        ) {
            Scaffold(
                topBar = {
                    LargeTopAppBar(
                        title = {
                            Column {
                                Text("FanLauncher", fontWeight = FontWeight.Bold)
                                Text("自分だけの扇ランチャー", style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        navigationIcon = {
                            Icon(Icons.Default.Widgets, contentDescription = null,
                                modifier = Modifier.padding(start = 16.dp))
                        }
                    )
                }
            ) { padding ->
                Column(
                    Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ExpressiveSectionCard("状態") {
                        PermissionRow("アプリ情報", true, "このアプリの設定・権限・バッテリーなどを開きます") {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                        }
                        PermissionRow("アクセシビリティ", accessibilityGranted, "スクリーンショット機能の設定を開きます") {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    }

                    ExpressiveSectionCard("エッジ") {
                        ValueSlider("反応幅", settings.edgeWidthDp, 16, 96, "dp") { save(settings.copy(edgeWidthDp = it)) }
                        ValueSlider("上端", (settings.edgeTopRatio * 100).toInt(), 0, 99, "%") {
                            val x = it / 100f
                            if (x < settings.edgeBottomRatio) save(settings.copy(edgeTopRatio = x))
                        }
                        ValueSlider("下端", (settings.edgeBottomRatio * 100).toInt(), 1, 100, "%") {
                            val x = it / 100f
                            if (x > settings.edgeTopRatio) save(settings.copy(edgeBottomRatio = x))
                        }
                        ValueSlider("スワイプ閾値", settings.swipeThresholdDp, 8, 64, "dp") { save(settings.copy(swipeThresholdDp = it)) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = settings.edge == EdgeSide.LEFT, onClick = { save(settings.copy(edge = EdgeSide.LEFT)) }, label = { Text("左端") })
                            FilterChip(selected = settings.edge == EdgeSide.RIGHT, onClick = { save(settings.copy(edge = EdgeSide.RIGHT)) }, label = { Text("右端") })
                        }
                        Text("白い判定領域は常に表示されます。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    ExpressiveSectionCard("扇のデザイン") {
                        ValueSlider("扇の幅", settings.fanWidthDp, 180, 700, "dp") { save(settings.copy(fanWidthDp = it)) }
                        ValueSlider("扇の高さ", settings.fanHeightDp, 180, 700, "dp") { save(settings.copy(fanHeightDp = it)) }
                        ValueSlider("中央アクション領域", settings.actionRadiusDp, 48, 240, "dp") { save(settings.copy(actionRadiusDp = it)) }
                        Text("幅と高さを別々に調整できます。扇は画面下の左下・右下を中心に開きます。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    ExpressiveSectionCard("アプリ配置") {
                        Text("1〜3段目のスロットをタップしてアプリを設定できます。比率は合計100%になるよう自動調整されます。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ValueSlider("1段目のスロット", settings.tier1Count, 1, 16, "個") { save(settings.copy(tier1Count = it)) }
                        ValueSlider("2段目のスロット", settings.tier2Count, 1, 16, "個") { save(settings.copy(tier2Count = it)) }
                        ValueSlider("3段目のスロット", settings.tier3Count, 1, 16, "個") { save(settings.copy(tier3Count = it)) }
                        ValueSlider("1段目の比率", settings.tier1Ratio, 10, 80, "%") { save(settings.withTierRatio(1, it)) }
                        ValueSlider("2段目の比率", settings.tier2Ratio, 10, 80, "%") { save(settings.withTierRatio(2, it)) }
                        ValueSlider("3段目の比率", settings.tier3Ratio, 10, 80, "%") { save(settings.withTierRatio(3, it)) }
                        AndroidView(
                            factory = { FanSlotPickerView(it, settings) { save(it) } },
                            update = { it.updateSettings(settings) },
                            modifier = Modifier.fillMaxWidth().height(620.dp)
                        )
                    }

                    ExpressiveSectionCard("中央アクション") {
                        SettingSwitch("スクリーンショット", settings.showScreenshot) {
                            save(settings.copy(showScreenshot = it))
                        }
                    }

                    ExpressiveSectionCard("使い方") {
                        Text("エッジの灰色エリアから内側へスワイプすると扇が開きます。扇の外側へ指を動かしても、開始位置が有効なら操作は継続します。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        }
    }

    @Composable
    private fun expressiveDarkColors() = darkColorScheme(
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8),
        tertiary = Color(0xFFEFB8C8),
        onTertiary = Color(0xFF492532),
        tertiaryContainer = Color(0xFF633B48),
        onTertiaryContainer = Color(0xFFFFD8E4),
        background = Color.Black,
        onBackground = Color(0xFFE6E1E5),
        surface = Color.Black,
        onSurface = Color(0xFFE6E1E5),
        surfaceVariant = Color(0xFF171717),
        onSurfaceVariant = Color(0xFFC9C4D0),
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF47444C)
    )

    @Composable
    private fun ExpressiveSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
        ElevatedCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                content()
            }
        }
    }

    @Composable
    private fun PermissionRow(title: String, granted: Boolean, description: String, onClick: () -> Unit) {
        ListItem(
            headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(description) },
            leadingContent = { Text(if (granted) "✓" else "!", color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) },
            trailingContent = { TextButton(onClick = onClick) { Text(if (granted) "確認" else "設定") } }
        )
    }

    @Composable
    private fun SettingSwitch(title: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = checked, onCheckedChange = onChanged)
        }
    }

    @Composable
    private fun ValueSlider(title: String, value: Int, min: Int, max: Int, unit: String, onValue: (Int) -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Text("$value $unit", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onValue(it.toInt()) },
                valueRange = min.toFloat()..max.toFloat(),
                steps = ((max - min) / 4 - 1).coerceAtLeast(0)
            )
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val target = ComponentName(this, ScreenshotAccessibilityService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(target, true) }
    }
}

class FanSlotPickerView(
    context: android.content.Context,
    private var settings: FanSettings,
    private val onChanged: (FanSettings) -> Unit
) : View(context) {
    private val pm = context.packageManager
    private val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        .map { it.activityInfo.applicationInfo }.distinctBy { it.packageName }
        .filter { it.packageName != context.packageName }
        .sortedBy { pm.getApplicationLabel(it).toString() }
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private var vertexX=0f; private var vertexY=0f; private var rx=0f; private var ry=0f
    private var selectedTier=1; private var selectedSlot=-1; private var currentDialog: AndroidAlertDialog?=null
    init { isClickable=true; setBackgroundColor(AndroidColor.BLACK) }
    fun updateSettings(newSettings: FanSettings){settings=newSettings;invalidate()}
    override fun onSizeChanged(w:Int,h:Int,oldW:Int,oldH:Int){
        vertexX=if(settings.edge==EdgeSide.RIGHT)w*.96f else w*.04f;vertexY=h*.90f
        rx=min(w*.88f,dp(settings.fanWidthDp));ry=min(h*.78f,dp(settings.fanHeightDp))
    }
    private fun pAt(xr:Float,yr:Float,a:Float):FloatArray{val r=Math.toRadians(a.toDouble());return floatArrayOf(vertexX+xr*cos(r).toFloat(),vertexY+yr*sin(r).toFloat())}
    private fun bounds():FloatArray{val a=.18f;val b1=a+(1f-a)*settings.tier1Ratio/100f;val b2=b1+(1f-a)*settings.tier2Ratio/100f;return floatArrayOf(a,b1,b2,1f)}
    override fun onDraw(c:android.graphics.Canvas){
        val start=if(settings.edge==EdgeSide.RIGHT)180f else 270f;val b=bounds()
        drawTier(c,start,settings.tier1Count,settings.tier1Apps,b[0],b[1],0xFF64B5F6.toInt(),1)
        drawTier(c,start,settings.tier2Count,settings.tier2Apps,b[1],b[2],0xFFFFB74D.toInt(),2)
        drawTier(c,start,settings.tier3Count,settings.tier3Apps,b[2],b[3],0xFF81C784.toInt(),3)
        paint.style=android.graphics.Paint.Style.FILL;paint.color=AndroidColor.WHITE;paint.textAlign=android.graphics.Paint.Align.CENTER;paint.textSize=dp(14).toFloat()
        c.drawText("タップしてアプリを設定",width/2f,dp(24).toFloat(),paint)
    }
    private fun drawTier(c:android.graphics.Canvas,start:Float,count:Int,selected:List<String>,inner:Float,outer:Float,color:Int,tier:Int){
        if(count<=0)return;val per=90f/count;val inRx=rx*inner;val inRy=ry*inner;val outRx=rx*outer;val outRy=ry*outer
        for(i in 0 until count){
            val a=start+i*per;val next=a+per;val s=pAt(inRx,inRy,a);val e=pAt(outRx,outRy,next);val path=android.graphics.Path()
            path.moveTo(s[0],s[1]);path.arcTo(android.graphics.RectF(vertexX-inRx,vertexY-inRy,vertexX+inRx,vertexY+inRy),a,per);path.lineTo(e[0],e[1]);path.arcTo(android.graphics.RectF(vertexX-outRx,vertexY-outRy,vertexX+outRx,vertexY+outRy),next,-per);path.close()
            paint.style=android.graphics.Paint.Style.FILL;paint.color=if(selected.getOrNull(i).isNullOrBlank())0xFF303030.toInt() else color;paint.alpha=if(selectedTier==tier&&selectedSlot==i)255 else 220;c.drawPath(path,paint)
            selected.getOrNull(i)?.takeIf{it.isNotBlank()}?.let{pkg->runCatching{val icon=pm.getApplicationIcon(pkg);val m=pAt((inRx+outRx)/2f,(inRy+outRy)/2f,a+per/2f);val size=dp(44);icon.setBounds((m[0]-size/2).toInt(),(m[1]-size/2).toInt(),(m[0]+size/2).toInt(),(m[1]+size/2).toInt());icon.draw(c)}}
        }
    }
    override fun onTouchEvent(e:android.view.MotionEvent):Boolean{
        if(e.action!=android.view.MotionEvent.ACTION_UP)return true;val start=if(settings.edge==EdgeSide.RIGHT)180f else 270f
        val dx=e.x-vertexX;val dy=e.y-vertexY;val radial=sqrt((dx/rx).pow(2)+(dy/ry).pow(2))
        var angle=Math.toDegrees(atan2(dy.toDouble(),dx.toDouble())).toFloat();if(angle<0)angle+=360f;var n=angle-start;while(n<0)n+=360f;while(n>=360)n-=360f
        if(n !in 0f..90f)return true;val b=bounds();val tier=when{radial>=b[0]&&radial<b[1]->1;radial>=b[1]&&radial<b[2]->2;radial>=b[2]&&radial<=1f->3;else->0};if(tier==0)return true
        val count=when(tier){1->settings.tier1Count;2->settings.tier2Count;else->settings.tier3Count};selectedTier=tier;selectedSlot=(n/(90f/count)).toInt().coerceIn(0,count-1);showAppDialog(tier,selectedSlot);invalidate();return true
    }
    private fun showAppDialog(tier:Int,index:Int){
        currentDialog?.dismiss();val rows=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL};addChoice(rows,tier,index,null,"未設定");addChoice(rows,tier,index,"__CLOSE_FAN__","戻る（扇を閉じる）")
        for(info in apps)addChoice(rows,tier,index,info.packageName,pm.getApplicationLabel(info).toString(),pm.getApplicationIcon(info))
        currentDialog=AndroidAlertDialog.Builder(context).setTitle(tier.toString()+"段目・"+(index+1)+"番").setView(ScrollView(context).apply{addView(rows)}).setNegativeButton("閉じる",null).create()
        currentDialog?.setOnDismissListener{currentDialog=null};currentDialog?.show()
    }
    private fun addChoice(parent:LinearLayout,tier:Int,index:Int,pkg:String?,label:String,icon:Drawable?=null){
        val row=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(10),dp(16),dp(10))}
        if(pkg=="__CLOSE_FAN__")row.addView(TextView(context).apply{text="▦";textSize=28f;gravity=Gravity.CENTER;layoutParams=LinearLayout.LayoutParams(dp(52),dp(52))}) else if(icon!=null)row.addView(ImageView(context).apply{setImageDrawable(icon);layoutParams=LinearLayout.LayoutParams(dp(52),dp(52))})
        row.addView(TextView(context).apply{text=label;textSize=17f;setPadding(dp(14),0,0,0);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
        row.setOnClickListener{val p=(when(tier){1->settings.tier1Apps;2->settings.tier2Apps;else->settings.tier3Apps}).toMutableList();while(p.size<=index)p.add("");p[index]=pkg?:"";settings=when(tier){1->settings.copy(tier1Apps=p);2->settings.copy(tier2Apps=p);else->settings.copy(tier3Apps=p)};onChanged(settings);currentDialog?.dismiss();currentDialog=null;invalidate()}
        parent.addView(row,LinearLayout.LayoutParams(-1,dp(76)))
    }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
