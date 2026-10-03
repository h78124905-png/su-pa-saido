package com.example.fanlauncher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class EdgeOverlayView(context: Context) : View(context) {
    private var startX = 0f
    private var startY = 0f
    private var triggered = false

    private val density = resources.displayMetrics.density
    private val edgeWidth = (48 * density).toInt()
    private val threshold = 24 * density
    private val debugPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x80FF0000.toInt()
        style = Paint.Style.FILL
    }

    var onSwipeStart: ((Float, Float) -> Unit)? = null
    var onSwipeMove: ((Float, Float) -> Unit)? = null
    var onSwipeEnd: (() -> Unit)? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateExclusion(width)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        updateExclusion(w)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = (width - edgeWidth).coerceAtLeast(0)
        canvas.drawRect(left.toFloat(), 0f, width.toFloat(), height.toFloat(), debugPaint)
    }

    private fun updateExclusion(w: Int) {
        systemGestureExclusionRects = listOf(
            Rect((w - edgeWidth).coerceAtLeast(0), 0, w, height)
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.x < width - edgeWidth) return false
                startX = event.rawX
                startY = event.rawY
                triggered = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - startX
                val dy = abs(event.rawY - startY)
                if (!triggered && dx < -threshold && dy < 200 * density) {
                    triggered = true
                    onSwipeStart?.invoke(event.rawX, event.rawY)
                }
                if (triggered) onSwipeMove?.invoke(event.rawX, event.rawY)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (triggered) onSwipeEnd?.invoke()
                triggered = false
                return true
            }
        }
        return true
    }
}
