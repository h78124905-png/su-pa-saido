package com.example.fanlauncher

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import android.graphics.Path
import android.accessibilityservice.GestureDescription
import android.os.Handler
import android.os.Looper
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

}
