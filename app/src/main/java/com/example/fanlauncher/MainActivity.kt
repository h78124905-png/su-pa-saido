package com.example.fanlauncher

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LocalTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
        var notificationGranted by remember {
            mutableStateOf(Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        }
        var showGuide by remember { mutableStateOf(false) }

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
                notificationGranted = Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                val prefs = context.getSharedPreferences("guide", MODE_PRIVATE)
                if (!prefs.getBoolean("shown", false)) showGuide = true
                delay(800)
            }
        }

        MaterialTheme(
            colorScheme = expressiveLightColors(),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(14.dp),
                medium = RoundedCornerShape(22.dp),
                large = RoundedCornerShape(30.dp),
                extraLarge = RoundedCornerShape(38.dp)
            ),
            typography = Typography(
                headlineMedium = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
                titleLarge = LocalTextStyle.current.toTextStyle().copy(fontWeight = FontWeight.SemiBold),
                bodyLarge = LocalTextStyle.current.toTextStyle().copy(fontSize = 16.sp)
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
                    ExpressiveSectionCard("状態", Icons.Default.Verified) {
                        PermissionRow("オーバーレイ", overlayGranted, "他のアプリの上に扇を表示します") {
                            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                        }
                        PermissionRow("アクセシビリティ", accessibilityGranted, "スクショと画面翻訳に使用します") {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                        if (Build.VERSION.SDK_INT >= 33) {
                            PermissionRow("通知", notificationGranted, "バックグラウンド動作の通知を表示します") {
                                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AssistChip(
                                onClick = {},
                                label = { Text(if (serviceRunning) "稼働中" else "停止中") },
                                leadingIcon = { Icon(if (serviceRunning) Icons.Default.CheckCircle else Icons.Default.PauseCircle, null) }
                            )
                            Button(onClick = {
                                context.stopService(Intent(context, OverlayService::class.java))
                                ContextCompat.startForegroundService(context, Intent(context, OverlayService::class.java))
                            }) { Text("再起動") }
                        }
                    }

                    ExpressiveSectionCard("エッジ", Icons.Default.Swipe) {
                        ValueSlider("反応幅", settings.edgeWidthDp, 16, 96, "dp") {
                            save(settings.copy(edgeWidthDp = it))
                        }
                        ValueSlider("上端", (settings.edgeTopRatio * 100).toInt(), 0, 99, "%") {
                            val x = it / 100f
                            if (x < settings.edgeBottomRatio) save(settings.copy(edgeTopRatio = x))
                        }
                        ValueSlider("下端", (settings.edgeBottomRatio * 100).toInt(), 1, 100, "%") {
                            val x = it / 100f
                            if (x > settings.edgeTopRatio) save(settings.copy(edgeBottomRatio = x))
                        }
                        ValueSlider("スワイプ閾値", settings.swipeThresholdDp, 8, 64, "dp") {
                            save(settings.copy(swipeThresholdDp = it))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = settings.edge == EdgeSide.LEFT, onClick = { save(settings.copy(edge = EdgeSide.LEFT)) }, label = { Text("左端") })
                            FilterChip(selected = settings.edge == EdgeSide.RIGHT, onClick = { save(settings.copy(edge = EdgeSide.RIGHT)) }, label = { Text("右端") })
                        }
                        SettingSwitch("エッジ判定位置を表示", settings.showEdgeIndicator) {
                            save(settings.copy(showEdgeIndicator = it))
                        }
                    }

                    ExpressiveSectionCard("扇のデザイン", Icons.Default.PieChart) {
                        ValueSlider("開き角", settings.fanSweepDeg, 90, 180, "°") {
                            save(settings.copy(fanSweepDeg = it))
                        }
                        ValueSlider("扇のサイズ", settings.fanRadiusDp, 160, 420, "dp") {
                            save(settings.copy(fanRadiusDp = it))
                        }
                        ValueSlider("中央アクション領域", settings.actionRadiusDp, 40, 220, "dp") {
                            save(settings.copy(actionRadiusDp = it))
                        }
                    }

                    ExpressiveSectionCard("アプリ配置", Icons.Default.Apps) {
                        Text("スロットをタップするとアプリを選べます。空きスロットもそのまま残ります。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("2段目", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Switch(checked = settings.tiers == 2, onCheckedChange = {
                                save(settings.copy(tiers = if (it) 2 else 1))
                            })
                        }
                        ValueSlider("1段目のスロット", settings.tier1Count, 1, 16, "個") {
                            save(settings.copy(tier1Count = it))
                        }
                        if (settings.tiers == 2) {
                            ValueSlider("2段目のスロット", settings.tier2Count, 1, 16, "個") {
                                save(settings.copy(tier2Count = it))
                            }
                        }
                        AndroidView(
                            factory = { FanSlotPickerView(it, settings) { save(it) } },
                            update = { it.updateSettings(settings) },
                            modifier = Modifier.fillMaxWidth().height(360.dp)
                        )
                    }

                    ExpressiveSectionCard("中央アクション", Icons.Default.TouchApp) {
                        SettingSwitch("スクリーンショット", settings.showScreenshot) {
                            save(settings.copy(showScreenshot = it))
                        }
                        SettingSwitch("画面翻訳", settings.showTranslate) {
                            save(settings.copy(showTranslate = it))
                        }
                    }

                    ExpressiveSectionCard("使い方", Icons.Default.Info) {
                        Text("エッジの灰色エリアから内側へスワイプすると扇が開きます。扇の外側へ指を動かしても、開始位置が有効なら操作は継続します。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        if (showGuide) {
            AlertDialog(
                onDismissRequest = {
                    context.getSharedPreferences("guide", MODE_PRIVATE).edit().putBoolean("shown", true).apply()
                    showGuide = false
                },
                icon = { Icon(Icons.Default.AutoAwesome, null) },
                title = { Text("FanLauncherをはじめよう") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("最初に3つの権限を設定すると、扇ランチャーの機能を使えます。")
                        Text("① オーバーレイ：扇を他のアプリの上に表示")
                        Text("② アクセシビリティ：スクショと画面翻訳")
                        Text("③ 通知：バックグラウンド動作を安定させるため")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        context.getSharedPreferences("guide", MODE_PRIVATE).edit().putBoolean("shown", true).apply()
                        showGuide = false
                    }) { Text("閉じる") }
                },
                dismissButton = {
                    TextButton(onClick = { showGuide = false }) { Text("あとで") }
                }
            )
        }
    }

    @Composable
    private fun expressiveLightColors() = if (Build.VERSION.SDK_INT >= 31) {
        dynamicLightColorScheme(this)
    } else {
        lightColorScheme(
            primary = Color(0xFF6750A4),
            secondary = Color(0xFF625B71),
            tertiary = Color(0xFF7D5260)
        )
    }

    @Composable
    private fun ExpressiveSectionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
        ElevatedCard(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
                    }
                    Text(title, style = MaterialTheme.typography.titleLarge)
                }
                content()
            }
        }
    }

    @Composable
    private fun PermissionRow(title: String, granted: Boolean, description: String, onClick: () -> Unit) {
        ListItem(
            headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(description) },
            leadingContent = {
                Icon(if (granted) Icons.Default.CheckCircle else Icons.Default.ErrorOutline, null,
                    tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            },
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
        addChoice(rows, tier, index, "__CLOSE_FAN__", "戻る（扇を閉じる）")
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
        if (pkg == "__CLOSE_FAN__") { row.addView(TextView(context).apply { text = "▦"; textSize = 24f; setTextColor(Color.DKGRAY); gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)) })
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
