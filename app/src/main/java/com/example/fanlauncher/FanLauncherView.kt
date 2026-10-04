package com.example.fanlauncher
import android.content.Context
import android.graphics.*
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class FanLauncherView(context: Context, private val settings: FanSettings) : View(context) {
    var tier1Items=emptyList<FanItem>();var tier2Items=emptyList<FanItem>();var tier3Items=emptyList<FanItem>()
    var onItemSelected:((FanItem)->Unit)?=null;var onActionSelected:((FanAction)->Unit)?=null
    private val density=resources.displayMetrics.density
    private val sweepTotal=90f
    private val startAngle=if(settings.edge==EdgeSide.RIGHT)180f else 270f
    private val outerRx=settings.fanWidthDp*density;private val outerRy=settings.fanHeightDp*density
    private val actionRx=settings.actionRadiusDp*density;private val actionRy=settings.actionRadiusDp*density
    private val iconSize=58f*density
    private var vertexX=0f;private var vertexY=0f;private var selectedTier=0;private var selectedIndex=-1;private var selectedAction:FanAction?=null;private var lastSelectionKey:String?=null
    override fun onSizeChanged(w:Int,h:Int,ow:Int,oh:Int){vertexX=if(settings.edge==EdgeSide.RIGHT)w.toFloat() else 0f;vertexY=h.toFloat()}
    fun setVertexY(y:Float){vertexY=height.toFloat();invalidate()}
    private fun bounds():FloatArray{
        val action=max(actionRx/outerRx,actionRy/outerRy).coerceIn(.05f,.55f);val usable=1f-action
        val b1=action+usable*settings.tier1Ratio/100f;val b2=b1+usable*settings.tier2Ratio/100f
        return floatArrayOf(action,b1,b2,1f)
    }
    private fun point(rx:Float,ry:Float,a:Float):FloatArray{val r=Math.toRadians(a.toDouble());return floatArrayOf(vertexX+rx*cos(r).toFloat(),vertexY+ry*sin(r).toFloat())}
    override fun onDraw(c:Canvas){val p=Paint(Paint.ANTI_ALIAS_FLAG);val b=bounds();drawTier(c,p,tier1Items,b[0],b[1],1);drawTier(c,p,tier2Items,b[1],b[2],2);drawTier(c,p,tier3Items,b[2],b[3],3);drawCenterActions(c)}
    private fun drawTier(c:Canvas,p:Paint,items:List<FanItem>,innerF:Float,outerF:Float,tier:Int){
        if(items.isEmpty())return;val per=sweepTotal/items.size;val inRx=outerRx*innerF;val inRy=outerRy*innerF;val outRx=outerRx*outerF;val outRy=outerRy*outerF
        items.forEachIndexed{i,item->
            val a=startAngle+i*per;val e=a+per;val s=point(inRx,inRy,a);val ep=point(outRx,outRy,e);val path=Path()
            path.moveTo(s[0],s[1]);path.arcTo(RectF(vertexX-inRx,vertexY-inRy,vertexX+inRx,vertexY+inRy),a,per);path.lineTo(ep[0],ep[1]);path.arcTo(RectF(vertexX-outRx,vertexY-outRy,vertexX+outRx,vertexY+outRy),e,-per);path.close()
            p.style=Paint.Style.FILL;p.color=if(selectedTier==tier&&selectedIndex==i)Color.WHITE else item.color;p.alpha=if(selectedTier==tier&&selectedIndex==i)255 else 225;c.drawPath(path,p)
            item.icon?.let{icon->val m=point((inRx+outRx)/2f,(inRy+outRy)/2f,a+per/2f);val size=if(selectedTier==tier&&selectedIndex==i)iconSize*1.16f else iconSize;val h=size/2f;icon.setBounds((m[0]-h).toInt(),(m[1]-h).toInt(),(m[0]+h).toInt(),(m[1]+h).toInt());icon.draw(c)}
        }
    }
    private fun actionList():List<FanAction> = if(settings.showScreenshot)listOf(FanAction.Screenshot)else emptyList()
    private fun drawCenterActions(c:Canvas){
        val buttons=actionList();if(buttons.isEmpty())return;val size=min(96f*density,settings.actionRadiusDp*density*.78f);val fill=Paint(1);val icon=Paint(1).apply{color=Color.WHITE;style=Paint.Style.STROKE;strokeWidth=3f*density;strokeCap=Paint.Cap.ROUND}
        buttons.forEach{a->val p=point(actionRx*.55f,actionRy*.55f,startAngle+45f);fill.color=if(selectedAction==a)0xFFE10600.toInt()else 0xFF171717.toInt();fill.alpha=235;c.drawCircle(p[0],p[1],size/2f,fill);if(a==FanAction.Screenshot){c.drawRect(p[0]-size*.23f,p[1]-size*.16f,p[0]+size*.23f,p[1]+size*.16f,icon);c.drawCircle(p[0],p[1],size*.1f,icon)}}
    }
    override fun onTouchEvent(e:MotionEvent)=false
    fun updateSelection(rawX:Float,rawY:Float){
        val loc=IntArray(2);getLocationOnScreen(loc);val x=rawX-loc[0];val y=rawY-loc[1];val dx=x-vertexX;val dy=y-vertexY
        var angle=Math.toDegrees(atan2(dy.toDouble(),dx.toDouble())).toFloat();if(angle<0)angle+=360f;var n=angle-startAngle;while(n<0)n+=360f;while(n>=360)n-=360f
        selectedAction=null;selectedTier=0;selectedIndex=-1;val b=bounds();val radial=sqrt((dx/outerRx).pow(2)+(dy/outerRy).pow(2));val action=max(actionRx/outerRx,actionRy/outerRy);val actions=actionList()
        if(actions.isNotEmpty()&&radial<=action&&n in 0f..sweepTotal)selectedAction=actions[(n/(sweepTotal/actions.size)).toInt().coerceIn(0,actions.size-1)]
        else if(n in 0f..sweepTotal){selectedTier=when{radial>=b[0]&&radial<b[1]->1;radial>=b[1]&&radial<b[2]->2;radial>=b[2]&&radial<=1f->3;else->0};selectedIndex=when(selectedTier){1->indexFromAngle(n,tier1Items.size);2->indexFromAngle(n,tier2Items.size);3->indexFromAngle(n,tier3Items.size);else->-1}}
        val key=when{selectedAction!=null->"action:$selectedAction";selectedTier>0&&selectedIndex>=0->"tier:$selectedTier:$selectedIndex";else->null};if(key!=null&&key!=lastSelectionKey){performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);lastSelectionKey=key};invalidate()
    }
    fun commitSelection(){selectedAction?.let{onActionSelected?.invoke(it)}?:when(selectedTier){1->tier1Items.getOrNull(selectedIndex)?.let{onItemSelected?.invoke(it)};2->tier2Items.getOrNull(selectedIndex)?.let{onItemSelected?.invoke(it)};3->tier3Items.getOrNull(selectedIndex)?.let{onItemSelected?.invoke(it)}};selectedAction=null;selectedTier=0;selectedIndex=-1}
    private fun indexFromAngle(n:Float,count:Int)=if(count==0)-1 else(n/(sweepTotal/count)).toInt().coerceIn(0,count-1)
}
