package com.example.fanlauncher
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class EdgeOverlayView(context:Context,private val settings:FanSettings):View(context){
    var onSwipeStart:((Float,Float)->Unit)?=null;var onSwipeMove:((Float,Float)->Unit)?=null;var onSwipeEnd:(()->Unit)?=null
    private val density=resources.displayMetrics.density;private var fullHeightMode=false;private var startX=0f;private var startY=0f;private var tracking=false;private var triggered=false
    init{setWillNotDraw(false);isClickable=false;isFocusable=false}
    fun setFullHeightMode(enabled:Boolean){fullHeightMode=enabled;requestLayout();invalidate()}
    private fun topPx()=if(fullHeightMode)(height*settings.edgeTopRatio).toInt().coerceIn(0,height)else 0
    private fun bottomPx()=if(fullHeightMode)(height*settings.edgeBottomRatio).toInt().coerceIn(topPx(),height)else height
    override fun onLayout(c:Boolean,l:Int,t:Int,r:Int,b:Int){super.onLayout(c,l,t,r,b);if(width==0||height==0)return;systemGestureExclusionRects=listOf(Rect(0,topPx(),width,bottomPx()))}
    override fun onDraw(c:Canvas){
        val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE}
        val top=topPx().toFloat();val bottom=bottomPx().toFloat();val rad=14f*density;val path=Path()
        if(settings.edge==EdgeSide.RIGHT){
            path.moveTo(0f,top);path.lineTo(width-rad,top);path.quadTo(width.toFloat(),top,width.toFloat(),top+rad);path.lineTo(width.toFloat(),bottom-rad);path.quadTo(width.toFloat(),bottom,width.toFloat()-rad,bottom);path.lineTo(0f,bottom)
        }else{
            path.moveTo(width.toFloat(),top);path.lineTo(rad,top);path.quadTo(0f,top,0f,top+rad);path.lineTo(0f,bottom-rad);path.quadTo(0f,bottom,rad,bottom);path.lineTo(width.toFloat(),bottom)
        }
        path.close();c.drawPath(path,p)
    }
    override fun onTouchEvent(e:MotionEvent):Boolean{when(e.actionMasked){
        MotionEvent.ACTION_DOWN->{val y=e.y;if(y<topPx()||y>=bottomPx()){tracking=false;triggered=false;return false};startX=e.rawX;startY=e.rawY;tracking=true;triggered=false;return true}
        MotionEvent.ACTION_MOVE->{if(!tracking)return false;if(!triggered){val dx=e.rawX-startX;val dy=abs(e.rawY-startY);val dir=if(settings.edge==EdgeSide.RIGHT)-1f else 1f;if(dx*dir>settings.swipeThresholdDp*density&&dy<200f*density){triggered=true;onSwipeStart?.invoke(e.rawX,e.rawY)}};if(triggered)onSwipeMove?.invoke(e.rawX,e.rawY);return true}
        MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{if(!tracking)return false;if(triggered)onSwipeEnd?.invoke();tracking=false;triggered=false;return true}}
        return false}
}
