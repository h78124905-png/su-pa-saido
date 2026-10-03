package com.example.fanlauncher

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var statusLayout: LinearLayout
    private lateinit var serviceStatus: TextView
    private lateinit var crashStatus: TextView

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshStatus()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildSettingsHome()
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::statusLayout.isInitialized) refreshStatus()
    }

    private fun buildSettingsHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        root.addView(TextView(this).apply {
            text = "FanLauncher"
            textSize = 28f
            setTextColor(Color.BLACK)
        }, matchWrap())

        root.addView(TextView(this).apply {
            text = "右端から左へスワイプする扇形ランチャー"
            textSize = 14f
            setPadding(0, dp(4), 0, dp(20))
        }, matchWrap())

        statusLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(statusLayout, matchWrap())

        serviceStatus = TextView(this).apply {
            textSize = 16f
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        root.addView(serviceStatus, matchWrap())

        crashStatus = TextView(this).apply {
            textSize = 12f
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setTextColor(Color.rgb(180, 0, 0))
        }
        root.addView(crashStatus, matchWrap())

        root.addView(Button(this).apply {
            text = "サービスを開始 / 再起動"
            setOnClickListener { startOverlayService() }
        }, matchWrap())

        root.addView(Button(this).apply {
            text = "オーバーレイ権限を開く"
            setOnClickListener {
                startActivity(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ))
            }
        }, matchWrap())

        root.addView(Button(this).apply {
            text = "アクセシビリティ設定を開く"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }, matchWrap())

        root.addView(Button(this).apply {
            text = "通知権限を許可"
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= 33) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }, matchWrap())

        root.addView(TextView(this).apply {
            text = "現在は動作確認用の設定ホームです。赤いエッジ線が表示されれば、オーバーレイの表示まで到達しています。"
            textSize = 13f
            setPadding(0, dp(20), 0, 0)
        }, matchWrap())

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun refreshStatus() {
        if (!::statusLayout.isInitialized) return
        statusLayout.removeAllViews()

        val overlayGranted = Settings.canDrawOverlays(this)
        addStatus("オーバーレイ権限", if (overlayGranted) "許可済み" else "未許可", overlayGranted)

        val accessibilityEnabled = isAccessibilityEnabled()
        addStatus("アクセシビリティ", if (accessibilityEnabled) "有効" else "無効", accessibilityEnabled)

        val notificationGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        addStatus("通知権限", if (notificationGranted) "許可済み" else "未許可", notificationGranted)

        val running = OverlayService.isRunning
        val state = if (running) "稼働中" else "停止中"
        serviceStatus.text = "OverlayService: " + state
        serviceStatus.setTextColor(
            if (running) Color.rgb(0, 120, 70) else Color.rgb(180, 0, 0)
        )

        val crashFile = java.io.File(filesDir, "service_crash.log")
        if (crashFile.exists()) {
            crashStatus.text = "⚠ サービス起動エラー\n\n" + runCatching {
                crashFile.readText()
            }.getOrElse { error -> "エラーログの読み込みに失敗しました: " + error.stackTraceToString() }
            crashStatus.setBackgroundColor(Color.rgb(255, 235, 235))
        } else {
            crashStatus.text = ""
            crashStatus.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun addStatus(label: String, value: String, ok: Boolean) {
        statusLayout.addView(TextView(this).apply {
            text = "$label: $value"
            textSize = 16f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setTextColor(if (ok) Color.rgb(0, 120, 70) else Color.rgb(180, 0, 0))
        }, matchWrap())
    }

    private fun startOverlayService() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ))
            return
        }

        val serviceIntent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        refreshStatus()
    }

    private fun isAccessibilityEnabled(): Boolean {
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return manager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun matchWrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
}
