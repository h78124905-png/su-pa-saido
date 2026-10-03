package com.example.fanlauncher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class EdgeOverlayView(
    context: Context,
    private val onSwipeStart: (Float, Float) -> Unit,
    private val onSwipeMove: (Float, Float) -> Unit,
    private val onSwipeEnd: () -> Unit
) : View(context) {

    private val density = resources.displayMetrics.density
    private val edgeWidthPx = (48 * density).toInt()
    private val showEdgeIndicator = true

    private var startX = 0f
    private var startY = 0f
    private var triggered = false

    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x60FF0000
    }

    init {
        setWillNotDraw(false)
        isClickable = false
        isFocusable = false
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (width == 0 || height == 0) return
        systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (showEdgeIndicator) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), indicatorPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                triggered = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!triggered) {
                    val dx = event.rawX - startX
                    val dy = abs(event.rawY - startY)
                    if (dx < -16 * density && dy < 200 * density) {
                        triggered = true
                        onSwipeStart(event.rawX, event.rawY)
                    }
                }

                if (triggered) {
                    onSwipeMove(event.rawX, event.rawY)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (triggered) {
                    onSwipeEnd()
                }
                triggered = false
                return true
            }
        }

        return false
    }
}
