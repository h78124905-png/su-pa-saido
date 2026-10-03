package com.example.fanlauncher

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent

class ScreenshotAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile
        var instance: ScreenshotAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    fun capture(callback: (Bitmap?) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            callback(null)
            return
        }

        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            mainExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    val buffer = screenshot.hardwareBuffer
                    val bitmap = Bitmap.wrapHardwareBuffer(
                        buffer,
                        screenshot.colorSpace
                    )?.copy(Bitmap.Config.ARGB_8888, false)
                    buffer.close()
                    callback(bitmap)
                }

                override fun onFailure(errorCode: Int) {
                    callback(null)
                }
            }
        )
    }
}
