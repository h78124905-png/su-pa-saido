package com.example.fanlauncher
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class FanLauncherView(context: Context, private val settings: FanSettings) : View(context) {
    var tier1Items: List<FanItem> = emptyList()
    var tier2Items: List<FanItem> = emptyList()
    var onItemSelected: ((FanItem) -> Unit)? = null
    var onActionSelected: ((FanAction) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val sweepTotal = settings.fanSweepDeg.toFloat()
    private val inwardCenter = if (settings.edge == EdgeSide.RIGHT) 180f else 0f
    private val startAngle = inwardCenter - sweepTotal / 2f
    private val tier1Radius = settings.fanRadiusDp * density
    private val tier2Radius = tier1Radius * 1.62f
    private val actionRadius = settings.actionRadiusDp * density
    private val iconSize = 46f * density

    private var vertexX = 0f
    private var vertexY = 0f
    private var selectedTier = 0
    private var selectedIndex = -1
    private var selectedAction: FanAction? = null

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        vertexX = if (settings.edge == EdgeSide.RIGHT) w.toFloat() else 0f
        vertexY = h / 2f
    }

    fun setVertexY(y: Float) {
        vertexY = y.coerceIn(0f, height.toFloat())
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        drawTier(c, p, tier1Items, tier1Radius, 1, actionRadius)
        if (settings.tiers == 2 && tier2Items.isNotEmpty()) {
            drawTier(c, p, tier2Items, tier2Radius, 2, tier1Radius)
        }
        drawCenterActions(c)
    }

    private fun drawTier(
        c: Canvas,
        p: Paint,
        items: List<FanItem>,
        radius: Float,
        tier: Int,
        inner: Float
    ) {
        if (items.isEmpty()) return
        val per = sweepTotal / items.size
        items.forEachIndexed { i, item ->
            val start = startAngle + i * per
            val selected = selectedTier == tier && selectedIndex == i
            p.color = if (selected) 0xFFEFEFEF.toInt() else 0xFF171717.toInt()
            p.alpha = if (selected) 245 else 215
            val path = Path().apply {
                moveTo(
                    vertexX + inner * cos(Math.toRadians(start.toDouble())).toFloat(),
                    vertexY + inner * sin(Math.toRadians(start.toDouble())).toFloat()
                )
                arcTo(RectF(vertexX - inner, vertexY - inner, vertexX + inner, vertexY + inner), start, per)
                lineTo(
                    vertexX + radius * cos(Math.toRadians((start + per).toDouble())).toFloat(),
                    vertexY + radius * sin(Math.toRadians((start + per).toDouble())).toFloat()
                )
                arcTo(RectF(vertexX - radius, vertexY - radius, vertexX + radius, vertexY + radius), start + per, -per)
                close()
            }
            c.drawPath(path, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.5f * density
            p.color = if (selected) 0xFFE10600.toInt() else 0xFF707070.toInt()
            p.alpha = if (selected) 255 else 180
            c.drawPath(path, p)
            p.style = Paint.Style.FILL

            item.icon?.let { icon ->
                val mid = Math.toRadians((start + per / 2f).toDouble())
                val ir = (inner + radius) / 2f
                val ix = vertexX + ir * cos(mid).toFloat()
                val iy = vertexY + ir * sin(mid).toFloat()
                val size = if (selected) iconSize * 1.16f else iconSize
                val h = size / 2f
                icon.setBounds((ix - h).toInt(), (iy - h).toInt(), (ix + h).toInt(), (iy + h).toInt())
                icon.draw(c)
            }
        }
    }

    private fun actionList(): List<FanAction> {
        val result = mutableListOf<FanAction>()
        if (settings.showScreenshot) result += FanAction.Screenshot
        if (settings.showTranslate) result += FanAction.Translate
        return result
    }

    private fun actionPoint(index: Int, count: Int, radius: Float): Pair<Float, Float> {
        val per = sweepTotal / count
        val angle = Math.toRadians((startAngle + per * (index + .5f)).toDouble())
        val r = radius * .55f
        return Pair(
            vertexX + r * cos(angle).toFloat(),
            vertexY + r * sin(angle).toFloat()
        )
    }

    private fun drawCenterActions(c: Canvas) {
        val buttons = actionList()
        if (buttons.isEmpty()) return

        val buttonSize = min(84f * density, actionRadius * .72f)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val icon = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f * density
            strokeCap = Paint.Cap.ROUND
        }

        buttons.forEachIndexed { index, action ->
            val point = actionPoint(index, buttons.size, actionRadius)
            val cx = point.first
            val cy = point.second
            fill.color = if (selectedAction == action) 0xFFE10600.toInt() else 0xFF171717.toInt()
            fill.alpha = if (selectedAction == action) 255 else 235
            c.drawCircle(cx, cy, buttonSize / 2f, fill)

            if (action == FanAction.Screenshot) {
                c.drawRect(
                    cx - buttonSize * .23f, cy - buttonSize * .16f,
                    cx + buttonSize * .23f, cy + buttonSize * .16f, icon
                )
                c.drawCircle(cx, cy, buttonSize * .10f, icon)
            } else {
                icon.style = Paint.Style.FILL
                icon.textSize = buttonSize * .43f
                icon.textAlign = Paint.Align.CENTER
                c.drawText("文", cx, cy + icon.textSize * .34f, icon)
                icon.style = Paint.Style.STROKE
            }
        }
    }

    override fun onTouchEvent(e: MotionEvent) = false

    fun updateSelection(rawX: Float, rawY: Float) {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        val x = rawX - loc[0]
        val y = rawY - loc[1]
        val dx = x - vertexX
        val dy = y - vertexY
        val dist = hypot(dx, dy)

        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (angle < 0f) angle += 360f

        var n = angle - startAngle
        while (n < 0f) n += 360f
        while (n >= 360f) n -= 360f

        selectedAction = null
        selectedTier = 0
        selectedIndex = -1

        val actions = actionList()
        if (actions.isNotEmpty() && dist <= actionRadius) {
            val per = sweepTotal / actions.size
            if (n in 0f..sweepTotal) {
                val idx = (n / per).toInt().coerceIn(0, actions.size - 1)
                selectedAction = actions[idx]
            }
        } else if (n in 0f..sweepTotal) {
            if (dist >= actionRadius && dist < tier1Radius) {
                selectedTier = 1
                selectedIndex = indexFromAngle(n, tier1Items.size)
            } else if (settings.tiers == 2 && dist >= tier1Radius && dist < tier2Radius) {
                selectedTier = 2
                selectedIndex = indexFromAngle(n, tier2Items.size)
            }
        }
        invalidate()
    }

    fun commitSelection() {
        selectedAction?.let { onActionSelected?.invoke(it) }
            ?: when (selectedTier) {
                1 -> tier1Items.getOrNull(selectedIndex)?.let { onItemSelected?.invoke(it) }
                2 -> tier2Items.getOrNull(selectedIndex)?.let { onItemSelected?.invoke(it) }
                else -> Unit
            }
        selectedAction = null
        selectedTier = 0
        selectedIndex = -1
    }

    private fun indexFromAngle(n: Float, count: Int): Int =
        if (count == 0) -1 else (n / (sweepTotal / count)).toInt().coerceIn(0, count - 1)
}
