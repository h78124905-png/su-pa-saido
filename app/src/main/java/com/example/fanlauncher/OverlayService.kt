package com.example.fanlauncher

import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var root: FrameLayout
    private lateinit var edge: EdgeOverlayView
    private var fanView: FanLauncherView? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        setupOverlay()
    }

    private fun setupOverlay() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        root = FrameLayout(this)
        edge = EdgeOverlayView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(edge)
        wm.addView(root, params)

        edge.onSwipeStart = { x, y -> showFan(x, y) }
        edge.onSwipeMove = { x, y -> fanView?.updateSelection(x, y) }
        edge.onSwipeEnd = { finishFan() }
    }

    private fun showFan(x: Float, y: Float) {
        if (fanView != null) return

        val fan = FanLauncherView(this)
        fan.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        fan.setCenter(resources.displayMetrics.widthPixels.toFloat(), y)
        root.addView(fan)
        fanView = fan

        scope.launch {
            fan.items = buildItems()
        }
    }

    private fun finishFan() {
        val fan = fanView ?: return
        val item = fan.consumeSelection()
        fanView = null
        root.removeView(fan)
        item?.let(::executeAction)
    }

    private suspend fun buildItems(): List<FanItem> = withContext(Dispatchers.Default) {
        val pm = packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != packageName }

        val appItems = apps.map { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            val info = resolveInfo.activityInfo.applicationInfo
            val label = pm.getApplicationLabel(info).toString()
            val icon = pm.getApplicationIcon(info)
            FanItem(
                label = label,
                icon = icon,
                color = AppColorExtractor.extract(this@OverlayService, pkg),
                action = FanAction.LaunchApp(pkg)
            )
        }.take(8)

        appItems + listOf(
            FanItem("Screenshot", null, 0xFF455A64.toInt(), FanAction.Screenshot),
            FanItem("Translate", null, 0xFF00897B.toInt(), FanAction.Translate)
        )
    }

    private fun executeAction(item: FanItem) {
        when (val action = item.action) {
            is FanAction.LaunchApp -> {
                packageManager.getLaunchIntentForPackage(action.packageName)?.also {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(it)
                }
            }
            FanAction.Screenshot -> ScreenshotAccessibilityService.instance?.capture { _ -> }
            FanAction.Translate -> {
                ScreenshotAccessibilityService.instance?.capture { bitmap ->
                    if (bitmap != null) {
                        scope.launch {
                            ScreenTranslator.translateAndOverlay(
                                this@OverlayService, bitmap, wm, root
                            )
                        }
                    }
                }
            }
        }
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(
            "fan", "FanLauncher", NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)

        return NotificationCompat.Builder(this, "fan")
            .setContentTitle("FanLauncher 稼働中")
            .setContentText("右端から左へスワイプでランチャーを開きます")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        if (::root.isInitialized) runCatching { wm.removeView(root) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
