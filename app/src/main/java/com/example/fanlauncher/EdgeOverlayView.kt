package com.example.fanlauncher
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
class EdgeOverlayView(context:Context,private val settings:FanSettings):View(context){
 var onSwipeStart:((Float,Float)->Unit)?=null;var onSwipeMove:((Float,Float)->Unit)?=null;var onSwipeEnd:(()->Unit)?=null
 private val density=resources.displayMetrics.density;private var startX=0f;private var startY=0f;private var triggered=false
 init{setWillNotDraw(false);isClickable=false;isFocusable=false}
 private fun topPx()=(height*settings.edgeTopRatio).toInt();private fun bottomPx()=(height*settings.edgeBottomRatio).toInt()
 override fun onLayout(c:Boolean,l:Int,t:Int,r:Int,b:Int){super.onLayout(c,l,t,r,b);if(width==0||height==0)return;systemGestureExclusionRects=listOf(Rect(0,topPx(),width,bottomPx()))}
 override fun onDraw(c:Canvas){if(!settings.showEdgeIndicator)return;val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=0x66FFFFFF};c.drawRect(0f,topPx().toFloat(),width.toFloat(),bottomPx().toFloat(),p)}
 override fun onTouchEvent(e:MotionEvent):Boolean{
    val top=topPx()
    val bottom=bottomPx()
    when(e.actionMasked){
        MotionEvent.ACTION_DOWN->{
            if(e.y<top||e.y>bottom)return false
            startX=e.rawX
            startY=e.rawY
            triggered=false
            return true
        }
        MotionEvent.ACTION_MOVE->{
            if(e.rawY<top||e.rawY>bottom){
                if(triggered)onSwipeEnd?.invoke()
                triggered=false
                return true
            }
            if(!triggered){
                val dx=e.rawX-startX
                val dy=abs(e.rawY-startY)
                val dir=if(settings.edge==EdgeSide.RIGHT)-1f else 1f
                if(dx*dir>settings.swipeThresholdDp*density&&dy<200*density){
                    triggered=true
                    onSwipeStart?.invoke(e.rawX,e.rawY)
                }
            }
            if(triggered)onSwipeMove?.invoke(e.rawX,e.rawY)
            return true
        }
        MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{
            if(triggered)onSwipeEnd?.invoke()
            triggered=false
            return true
        }
    }
    return false
}
}