package com.example.fanlauncher

import android.content.Context
import android.graphics.*
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

object TranslationOverlay {
    fun show(
        context: Context,
        wm: WindowManager,
        parent: ViewGroup,
        items: List<Pair<Rect, String>>
    ) {
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
                super.onDraw(canvas)
                items.forEach { (rect, text) ->
                    canvas.drawRect(rect, backgroundPaint)
                    canvas.drawText(
                        text,
                        rect.left.toFloat(),
                        rect.centerY().toFloat(),
                        textPaint
                    )
                }
            }
        }

        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        parent.addView(view)
        view.postDelayed({ parent.removeView(view) }, 4000)
    }
}
