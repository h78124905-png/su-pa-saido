package com.example.fanlauncher

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class FanLauncherView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var items: List<FanItem> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sweepTotal = 100f
    private val startAngle = 180f - sweepTotal / 2f
    private val iconSize = 96f
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    private var selectedIndex = -1

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = (w * 0.85f).coerceAtLeast(600f)
        if (centerX == 0f) centerX = w.toFloat()
        if (centerY == 0f) centerY = h / 2f
    }

    fun setCenter(x: Float, y: Float) {
        centerX = x
        centerY = y
        invalidate()
    }

    fun updateSelection(rawX: Float, rawY: Float) {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        val localX = rawX - loc[0]
        val localY = rawY - loc[1]
        selectedIndex = computeSector(localX, localY)
        invalidate()
    }

    fun consumeSelection(): FanItem? {
        val result = items.getOrNull(selectedIndex)
        selectedIndex = -1
        return result
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (items.isEmpty()) return

        val sweepPer = sweepTotal / items.size
        val rect = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        items.forEachIndexed { i, item ->
            val start = startAngle + i * sweepPer
            paint.color = item.color
            paint.alpha = if (i == selectedIndex) 255 else 180
            canvas.drawArc(rect, start, sweepPer - 2f, true, paint)

            item.icon?.let { icon ->
                val mid = Math.toRadians((start + sweepPer / 2f).toDouble())
                val iconR = radius * 0.72f
                val ix = centerX + iconR * cos(mid).toFloat()
                val iy = centerY + iconR * sin(mid).toFloat()
                drawIcon(canvas, icon, ix, iy, i == selectedIndex)
            }
        }
    }

    private fun drawIcon(canvas: Canvas, icon: Drawable, cx: Float, cy: Float, selected: Boolean) {
        val size = if (selected) iconSize * 1.2f else iconSize
        val half = size / 2f
        icon.setBounds(
            (cx - half).toInt(), (cy - half).toInt(),
            (cx + half).toInt(), (cy + half).toInt()
        )
        icon.draw(canvas)
    }

    private fun computeSector(x: Float, y: Float): Int {
        if (items.isEmpty()) return -1
        val dx = x - centerX
        val dy = y - centerY
        if (hypot(dx, dy) < radius * 0.25f) return -1

        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (angle < 0f) angle += 360f

        val normalized = angle - startAngle
        if (normalized < 0f || normalized > sweepTotal) return -1

        val sweepPer = sweepTotal / items.size
        return (normalized / sweepPer).toInt().coerceIn(0, items.lastIndex)
    }
}
