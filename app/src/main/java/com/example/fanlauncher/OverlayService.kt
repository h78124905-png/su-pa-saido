package com.example.fanlauncher

import android.app.*
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class OverlayService : Service() {
    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private lateinit var wm: WindowManager
    private lateinit var edgeRoot: FrameLayout
    private lateinit var fanRoot: FrameLayout
    private lateinit var edge: EdgeOverlayView
    private var fanView: FanLauncherView? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1, notification)
            }
            wm = getSystemService(WINDOW_SERVICE) as WindowManager
            setupOverlay()
            isRunning = true
        } catch (e: Exception) {
            isRunning = false
            runCatching {
                java.io.File(filesDir, "service_crash.log").writeText(e.stackTraceToString())
            }
            stopSelf()
        }
    }

    private fun setupOverlay() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val density = resources.displayMetrics.density
        val edgeWidthDp = 48
        val edgeWidth = (edgeWidthDp * density).toInt()

        val edgeParams = WindowManager.LayoutParams(
            edgeWidth * 2,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        edgeRoot = FrameLayout(this)
        edge = EdgeOverlayView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        edgeRoot.addView(edge)
        wm.addView(edgeRoot, edgeParams)

        edge.onSwipeStart = { x, y -> showFan(x, y, type) }
        edge.onSwipeMove = { x, y -> fanView?.updateSelection(x, y) }
        edge.onSwipeEnd = { finishFan() }
    }

    private fun showFan(x: Float, y: Float, type: Int) {
        if (fanView != null) return

        val fan = FanLauncherView(this)
        fan.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        fan.setCenter(resources.displayMetrics.widthPixels.toFloat(), y)

        fanRoot = FrameLayout(this)
        fanRoot.addView(fan)

        val fanParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        wm.addView(fanRoot, fanParams)
        fanView = fan
        scope.launch { fan.items = buildItems() }
    }

    private fun finishFan() {
        val fan = fanView ?: return
        val item = fan.consumeSelection()
        fanView = null
        if (::fanRoot.isInitialized) {
            runCatching { wm.removeView(fanRoot) }
        }
        item?.let(::executeAction)
    }

    private suspend fun buildItems(): List<FanItem> = withContext(Dispatchers.Default) {
        val pm = packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { pm.getApplicationLabel(it.activityInfo.applicationInfo).toString() }

        val appItems = apps.map { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            val info = resolveInfo.activityInfo.applicationInfo
            FanItem(
                label = pm.getApplicationLabel(info).toString(),
                icon = pm.getApplicationIcon(info),
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
            FanAction.Screenshot -> ScreenshotAccessibilityService.instance?.capture { bitmap ->
                if (bitmap != null) saveScreenshot(bitmap)
                else Toast.makeText(this, "スクリーンショットに失敗しました", Toast.LENGTH_SHORT).show()
            }
            FanAction.Translate -> {
                ScreenshotAccessibilityService.instance?.capture { bitmap ->
                    if (bitmap != null) {
                        scope.launch {
                            ScreenTranslator.translateAndOverlay(this@OverlayService, bitmap, wm, edgeRoot)
                        }
                    }
                }
            }
        }
    }

    private fun saveScreenshot(bitmap: Bitmap) {
        scope.launch(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "fanlauncher_" + System.currentTimeMillis() + ".png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FanLauncher")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri: Uri? = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            )

            if (uri == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OverlayService, "スクリーンショットの保存に失敗しました", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val success = runCatching {
                contentResolver.openOutputStream(uri)?.use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                } ?: false
            }.getOrDefault(false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                    null,
                    null
                )
            }

            if (!success) contentResolver.delete(uri, null, null)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@OverlayService,
                    if (success) "スクリーンショットを保存しました" else "スクリーンショットの保存に失敗しました",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel("fan", "FanLauncher", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        return NotificationCompat.Builder(this, "fan")
            .setContentTitle("FanLauncher 稼働中")
            .setContentText("右端から左へスワイプでランチャーを開きます")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        isRunning = false
        scope.cancel()
        if (::fanRoot.isInitialized) runCatching { wm.removeView(fanRoot) }
        if (::edgeRoot.isInitialized) runCatching { wm.removeView(edgeRoot) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
