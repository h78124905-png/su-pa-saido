package com.example.fanlauncher

import android.content.Context
import android.graphics.*
import android.view.View
import android.view.WindowManager

object TranslationOverlay {
    fun show(context: Context, wm: WindowManager, items: List<Pair<Rect, String>>) {
        val view = object : View(context) {
            private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 36f
                setShadowLayer(8f, 0f, 0f, Color.BLACK)
            }
            private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(180, 0, 0, 0)
            }
            override fun onDraw(canvas: Canvas) {
                items.forEach { (rect, text) ->
                    canvas.drawRect(rect, backgroundPaint)
                    canvas.drawText(text, rect.left.toFloat(), rect.centerY().toFloat(), textPaint)
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = android.view.Gravity.TOP or android.view.Gravity.START }
        wm.addView(view, params)
        view.postDelayed({ runCatching { wm.removeView(view) } }, 4000L)
    }
}
