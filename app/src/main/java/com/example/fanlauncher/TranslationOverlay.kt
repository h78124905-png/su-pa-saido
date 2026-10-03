package com.example.fanlauncher

import android.content.Context
import android.graphics.*
import android.view.*
import android.widget.Button
import android.widget.FrameLayout

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

        val fullParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        val close = Button(context).apply {
            text = "閉じる"
            setOnClickListener {
                runCatching { wm.removeView(view) }
                runCatching { wm.removeView(this) }
            }
        }
        val closeSize = (96 * context.resources.displayMetrics.density).toInt()
        val closeParams = WindowManager.LayoutParams(
            closeSize,
            (52 * context.resources.displayMetrics.density).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            y = (12 * context.resources.displayMetrics.density).toInt()
            x = (12 * context.resources.displayMetrics.density).toInt()
        }

        wm.addView(view, fullParams)
        wm.addView(close, closeParams)
    }
}
