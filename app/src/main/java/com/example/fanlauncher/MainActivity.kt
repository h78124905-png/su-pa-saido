package com.example.fanlauncher
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import android.provider.Settings
import android.widget.*
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private lateinit var s: FanSettings
    private lateinit var picker: FanSlotPickerView
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); buildSettingsHome() }
    private fun buildSettingsHome() {
        s = FanSettings.load(this)

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(40))
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(title("FanLauncher"))
        root.addView(label("権限ステータス"))
        addPermissionRow(root, "オーバーレイ", Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName)))
        }
        addPermissionRow(root, "アクセシビリティ", isAccessibilityEnabled()) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        if (Build.VERSION.SDK_INT >= 33) {
            addPermissionRow(root, "通知", checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        root.addView(label("サービス"))
        val status = TextView(this).apply { setPadding(0, dp(8), 0, dp(8)) }
        root.addView(status)
        fun refresh() {
            val running = OverlayService.isRunning
            status.text = if (running) "OverlayService: 稼働中" else "OverlayService: 停止中"
            status.setTextColor(if (running) Color.rgb(0, 120, 70) else Color.RED)
        }
        refresh()
        root.addView(button("サービスを開始 / 再起動") {
            stopService(Intent(this, OverlayService::class.java))
            ContextCompat.startForegroundService(this, Intent(this, OverlayService::class.java))
            status.postDelayed(::refresh, 500)
        })

        root.addView(label("エッジ設定"))
        val ew = labelWithValue("エッジ幅", s.edgeWidthDp.toString() + "dp")
        root.addView(ew)
        root.addView(seekBar(16, 96, s.edgeWidthDp) { v ->
            s = s.copy(edgeWidthDp = v); FanSettings.save(this, s)
            ew.text = "エッジ幅: " + v + "dp"; OverlayService.instance?.applySettings()
        })
        val top = labelWithValue("上端", (s.edgeTopRatio * 100).toInt().toString() + "%")
        root.addView(top)
        root.addView(seekBar(0, 100, (s.edgeTopRatio * 100).toInt()) { v ->
            val x = v / 100f
            if (x < s.edgeBottomRatio) {
                s = s.copy(edgeTopRatio = x); FanSettings.save(this, s)
                top.text = "上端: " + v + "%"; OverlayService.instance?.applySettings()
            }
        })
        val bottom = labelWithValue("下端", (s.edgeBottomRatio * 100).toInt().toString() + "%")
        root.addView(bottom)
        root.addView(seekBar(0, 100, (s.edgeBottomRatio * 100).toInt()) { v ->
            val x = v / 100f
            if (x > s.edgeTopRatio) {
                s = s.copy(edgeBottomRatio = x); FanSettings.save(this, s)
                bottom.text = "下端: " + v + "%"; OverlayService.instance?.applySettings()
            }
        })
        val st = labelWithValue("スワイプ閾値", s.swipeThresholdDp.toString() + "dp")
        root.addView(st)
        root.addView(seekBar(8, 64, s.swipeThresholdDp) { v ->
            s = s.copy(swipeThresholdDp = v); FanSettings.save(this, s)
            st.text = "スワイプ閾値: " + v + "dp"; OverlayService.instance?.applySettings()
        })

        root.addView(label("反応するエッジ"))
        val eg = RadioGroup(this)
        val left = RadioButton(this).apply { text = "左"; id = 1001 }
        val right = RadioButton(this).apply { text = "右"; id = 1002 }
        eg.addView(left); eg.addView(right)
        (if (s.edge == EdgeSide.LEFT) left else right).isChecked = true
        eg.setOnCheckedChangeListener { _, id ->
            s = s.copy(edge = if (id == 1001) EdgeSide.LEFT else EdgeSide.RIGHT)
            FanSettings.save(this, s); picker.updateSettings(s); OverlayService.instance?.applySettings()
        }
        root.addView(eg)
        root.addView(checkBox("エッジ判定位置を表示", s.showEdgeIndicator) {
            s = s.copy(showEdgeIndicator = it); FanSettings.save(this, s); OverlayService.instance?.applySettings()
        })

        root.addView(label("扇設定"))
        val sweep = labelWithValue("扇の開き角", s.fanSweepDeg.toString() + "°")
        root.addView(sweep)
        root.addView(seekBar(90, 180, s.fanSweepDeg) { v ->
            s = s.copy(fanSweepDeg = v); FanSettings.save(this, s)
            sweep.text = "扇の開き角: " + v + "°"; picker.updateSettings(s); OverlayService.instance?.applySettings()
        })
        val radius = labelWithValue("扇のサイズ", s.fanRadiusDp.toString() + "dp")
        root.addView(radius)
        root.addView(seekBar(160, 420, s.fanRadiusDp) { v ->
            s = s.copy(fanRadiusDp = v); FanSettings.save(this, s)
            radius.text = "扇のサイズ: " + v + "dp"
            // 設定画面のプレビューは固定サイズ。実際の扇だけを変更する。
            OverlayService.instance?.applySettings()
        })

        root.addView(label("段数"))
        root.addView(TextView(this).apply {
            text = "1段目は常に使用。2段目だけオン / オフできます。"
            setTextColor(Color.DKGRAY)
        })
        val tier2 = Switch(this).apply {
            text = "2段目を使用"
            isChecked = s.tiers == 2
        }
        root.addView(tier2)
        tier2.setOnCheckedChangeListener { _, checked ->
            s = s.copy(tiers = if (checked) 2 else 1)
            FanSettings.save(this, s); picker.updateSettings(s); OverlayService.instance?.applySettings()
        }

        val c1 = labelWithValue("1段目アプリ数", s.tier1Count.toString())
        root.addView(c1)
        root.addView(seekBar(1, 16, s.tier1Count) { v ->
            s = s.copy(tier1Count = v); FanSettings.save(this, s)
            c1.text = "1段目アプリ数: " + v; picker.updateSettings(s); OverlayService.instance?.applySettings()
        })
        val c2 = labelWithValue("2段目アプリ数", s.tier2Count.toString())
        root.addView(c2)
        root.addView(seekBar(1, 16, s.tier2Count) { v ->
            s = s.copy(tier2Count = v); FanSettings.save(this, s)
            c2.text = "2段目アプリ数: " + v; picker.updateSettings(s); OverlayService.instance?.applySettings()
        })

        root.addView(divider())
        root.addView(label("アプリ配置"))
        root.addView(TextView(this).apply {
            text = "下のランチャーUIをそのままプレビュー表示します。\n内側＝1段目 / 外側＝2段目。スロットをタップしてアプリを選択します。"
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, dp(8))
        })

        picker = FanSlotPickerView(this, s) { updated ->
            s = updated
            FanSettings.save(this, s)
            OverlayService.instance?.applySettings()
        }
        root.addView(picker, LinearLayout.LayoutParams(-1, dp(340)))

        root.addView(checkBox("設定画面にランチャーUIを表示", true) {
            picker.visibility = if (it) View.VISIBLE else View.GONE
        })

        root.addView(label("中央ボタン"))
        root.addView(checkBox("スクショボタン表示", s.showScreenshot) {
            s = s.copy(showScreenshot = it); FanSettings.save(this, s); picker.updateSettings(s); OverlayService.instance?.applySettings()
        })
        root.addView(checkBox("翻訳ボタン表示", s.showTranslate) {
            s = s.copy(showTranslate = it); FanSettings.save(this, s); picker.updateSettings(s); OverlayService.instance?.applySettings()
        })
        root.addView(TextView(this).apply {
            text = "設定は変更した瞬間に保存・反映されます。プレビューの大きさは実際の扇サイズとは独立しています。"
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(12), 0, 0)
        })
    }

    private fun addPermissionRow(p:LinearLayout,n:String,g:Boolean,a:()->Unit){p.addView(TextView(this).apply{text=(if(g) "✅ " else "❌ ")+n;setPadding(0,dp(10),0,dp(10));setOnClickListener{a()}})}
    private fun title(t:String)=TextView(this).apply{text=t;textSize=28f;setTextColor(Color.BLACK);setPadding(0,0,0,dp(20))}
    private fun label(t:String)=TextView(this).apply{text=t;textSize=18f;setTextColor(Color.rgb(25,118,210));setPadding(0,dp(28),0,dp(8))}
    private fun labelWithValue(n:String,v:String)=TextView(this).apply{text="$n: $v";setPadding(0,dp(10),0,dp(4))}
    private fun divider()=View(this).apply{setBackgroundColor(0xFFBDBDBD.toInt());layoutParams=LinearLayout.LayoutParams(-1,dp(1)).apply{topMargin=dp(18);bottomMargin=dp(4)}}
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
class FanSlotPickerView(
    context: android.content.Context,
    private var settings: FanSettings,
    private val onChanged: (FanSettings) -> Unit
) : View(context) {

    private val pm = context.packageManager
    private val apps = pm.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
    ).map { it.activityInfo.applicationInfo }
        .distinctBy { it.packageName }
        .filter { it.packageName != context.packageName }
        .sortedBy { pm.getApplicationLabel(it).toString() }

    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private var vertexX = 0f
    private var vertexY = 0f
    private var previewRadius = 0f
    private var selectedTier = 1
    private var selectedSlot = -1
    private var currentDialog: AlertDialog? = null

    init {
        isClickable = true
        setBackgroundColor(0xFFF7F7F7.toInt())
    }

    fun updateSettings(newSettings: FanSettings) {
        settings = newSettings
        if (settings.tiers == 1) selectedTier = 1
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        // 実際のfanRadiusDpとは無関係。2段目まで必ず画面内に収まる固定プレビュー。
        previewRadius = minOf(w * 0.20f, h * 0.31f).coerceAtLeast(dp(60).toFloat())
        vertexX = if (settings.edge == EdgeSide.RIGHT) w - dp(24).toFloat() else dp(24).toFloat()
        vertexY = h * 0.52f
    }

    override fun onDraw(c: android.graphics.Canvas) {
        super.onDraw(c)

        val sweep = settings.fanSweepDeg.toFloat()
        val inwardCenter = if (settings.edge == EdgeSide.RIGHT) 180f else 0f
        val start = inwardCenter - sweep / 2f

        drawTier(c, start, sweep, settings.tier1Count, settings.tier1Apps,
            previewRadius * 0.52f, previewRadius, 1, 0xFF90CAF9.toInt())

        if (settings.tiers == 2) {
            drawTier(c, start, sweep, settings.tier2Count, settings.tier2Apps,
                previewRadius, previewRadius * 1.55f, 2, 0xFFFFCC80.toInt())
        }

        drawCenterActions(c)

        paint.style = android.graphics.Paint.Style.FILL
        paint.color = Color.DKGRAY
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.textSize = dp(12).toFloat()
        c.drawText(if (settings.edge == EdgeSide.RIGHT) "右端から開く" else "左端から開く",
            vertexX, dp(22).toFloat(), paint)

        paint.textSize = dp(11).toFloat()
        paint.color = 0xFF555555.toInt()
        c.drawText("青：1段目", width / 2f, height - dp(30).toFloat(), paint)
        if (settings.tiers == 2) c.drawText("橙：2段目", width / 2f, height - dp(12).toFloat(), paint)
    }

    private fun drawTier(
        c: android.graphics.Canvas,
        start: Float,
        sweep: Float,
        count: Int,
        selected: List<String>,
        inner: Float,
        outer: Float,
        tier: Int,
        baseColor: Int
    ) {
        if (count <= 0) return
        val per = sweep / count

        for (i in 0 until count) {
            val a = start + i * per
            val next = a + per
            val path = android.graphics.Path().apply {
                moveTo(vertexX + inner * cos(Math.toRadians(a.toDouble())).toFloat(),
                    vertexY + inner * sin(Math.toRadians(a.toDouble())).toFloat())
                arcTo(android.graphics.RectF(vertexX - inner, vertexY - inner,
                    vertexX + inner, vertexY + inner), a, per)
                lineTo(vertexX + outer * cos(Math.toRadians(next.toDouble())).toFloat(),
                    vertexY + outer * sin(Math.toRadians(next.toDouble())).toFloat())
                arcTo(android.graphics.RectF(vertexX - outer, vertexY - outer,
                    vertexX + outer, vertexY + outer), next, -per)
                close()
            }

            paint.color = if (selected.getOrNull(i).isNullOrBlank()) 0xFFE0E0E0.toInt() else baseColor
            paint.alpha = if (selectedTier == tier && selectedSlot == i) 255 else 205
            paint.style = android.graphics.Paint.Style.FILL
            c.drawPath(path, paint)
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = dp(2).toFloat()
            paint.color = 0xFF757575.toInt()
            paint.alpha = 220
            c.drawPath(path, paint)
            paint.style = android.graphics.Paint.Style.FILL

            selected.getOrNull(i)?.takeIf { it.isNotBlank() }?.let { pkg ->
                runCatching {
                    val icon = pm.getApplicationIcon(pkg)
                    val mid = Math.toRadians((a + per / 2f).toDouble())
                    val r = (inner + outer) / 2f
                    val x = vertexX + r * cos(mid).toFloat()
                    val y = vertexY + r * sin(mid).toFloat()
                    val size = dp(30).toFloat()
                    icon.setBounds((x - size / 2).toInt(), (y - size / 2).toInt(),
                        (x + size / 2).toInt(), (y + size / 2).toInt())
                    icon.draw(c)
                }
            }
        }
    }

    private fun drawCenterActions(c: android.graphics.Canvas) {
        val actions = mutableListOf<String>()
        if (settings.showScreenshot) actions += "写真"
        if (settings.showTranslate) actions += "翻訳"
        if (actions.isEmpty()) return

        val centerX = vertexX + (if (settings.edge == EdgeSide.RIGHT) -1 else 1) * minOf(dp(30).toFloat(), previewRadius * .28f)
        val centerY = vertexY

        if (actions.size == 2) {
            paint.color = 0xFF455A64.toInt(); paint.alpha = 230
            c.drawCircle(centerX, centerY - dp(24), dp(20).toFloat(), paint)
            paint.color = 0xFF00897B.toInt()
            c.drawCircle(centerX, centerY + dp(24), dp(20).toFloat(), paint)
            paint.color = Color.WHITE; paint.textSize = dp(9).toFloat()
            c.drawText("写真", centerX, centerY - dp(21), paint)
            c.drawText("翻訳", centerX, centerY + dp(27), paint)
        } else {
            paint.color = if (actions[0] == "写真") 0xFF455A64.toInt() else 0xFF00897B.toInt()
            paint.alpha = 230
            c.drawCircle(centerX, centerY, dp(22).toFloat(), paint)
            paint.color = Color.WHITE; paint.textSize = dp(9).toFloat()
            c.drawText(actions[0], centerX, centerY + dp(3), paint)
        }
    }

    override fun onTouchEvent(e: android.view.MotionEvent): Boolean {
        if (e.action != android.view.MotionEvent.ACTION_UP) return true

        val dx = e.x - vertexX
        val dy = e.y - vertexY
        val distance = hypot(dx, dy)
        val sweep = settings.fanSweepDeg.toFloat()
        val inwardCenter = if (settings.edge == EdgeSide.RIGHT) 180f else 0f
        val start = inwardCenter - sweep / 2f

        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (angle < 0f) angle += 360f
        var n = angle - start
        while (n < 0f) n += 360f
        while (n >= 360f) n -= 360f
        if (n !in 0f..sweep) return true

        val tier2 = settings.tiers == 2 && distance >= previewRadius && distance <= previewRadius * 1.55f
        val tier = if (tier2) 2 else if (distance >= previewRadius * .52f && distance <= previewRadius) 1 else return true
        val count = if (tier == 1) settings.tier1Count else settings.tier2Count
        val slot = (n / (sweep / count)).toInt().coerceIn(0, count - 1)

        selectedTier = tier
        selectedSlot = slot
        showAppDialog(tier, slot)
        return true
    }

    private fun showAppDialog(tier: Int, index: Int) {
        currentDialog?.dismiss()
        val rows = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        addChoice(rows, tier, index, null, "未設定")
        addChoice(rows, tier, index, "__APP_LIST__", "アプリ一覧へ戻る")
        for (info in apps) {
            addChoice(rows, tier, index, info.packageName,
                pm.getApplicationLabel(info).toString(), pm.getApplicationIcon(info))
        }

        currentDialog = AlertDialog.Builder(context)
            .setTitle(tier.toString() + "段目・" + (index + 1) + "番")
            .setView(ScrollView(context).apply { addView(rows) })
            .setNegativeButton("閉じる", null)
            .create()
        currentDialog?.setOnDismissListener { currentDialog = null }
        currentDialog?.show()
    }

    private fun addChoice(parent: LinearLayout, tier: Int, index: Int, pkg: String?, label: String, icon: Drawable? = null) {
        val row = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        if (pkg == "__APP_LIST__") { row.addView(TextView(context).apply { text = "▦"; textSize = 24f; setTextColor(Color.DKGRAY); gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)) })
        } else if (icon != null) {
            row.addView(ImageView(context).apply {
                setImageDrawable(icon)
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            })
        }
        row.addView(TextView(context).apply {
            text = label
            textSize = 16f
            setPadding(dp(12), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })
        row.setOnClickListener {
            val source = if (tier == 1) settings.tier1Apps else settings.tier2Apps
            val p = source.toMutableList()
            while (p.size <= index) p.add("")
            p[index] = pkg ?: ""
            settings = if (tier == 1) settings.copy(tier1Apps = p) else settings.copy(tier2Apps = p)
            onChanged(settings)
            invalidate()
            currentDialog?.let { dialog ->
                currentDialog = null
                dialog.dismiss()
            }
        }
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(60)))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
