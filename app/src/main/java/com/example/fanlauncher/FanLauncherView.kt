package com.example.fanlauncher
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
class FanLauncherView(context:Context,private val settings:FanSettings):View(context){
 var tier1Items:List<FanItem>=emptyList();var tier2Items:List<FanItem>=emptyList();var onItemSelected:((FanItem)->Unit)?=null;var onDismissed:(()->Unit)?=null
 private val sweepTotal=90f;private val startAngle=if(settings.edge==EdgeSide.RIGHT)180f else 270f;private var vertexX=0f;private var vertexY=0f
 private val tier1Radius=420f;private val tier2Radius=780f;private val iconSize=90f;private var selectedTier=0;private var selectedIndex=-1
 override fun onSizeChanged(w:Int,h:Int,ow:Int,oh:Int){vertexX=if(settings.edge==EdgeSide.RIGHT)w.toFloat() else 0f;vertexY=h/2f}
 fun setVertexY(y:Float){vertexY=y.coerceIn(height*.1f,height*.6f);invalidate()}
 override fun onDraw(c:Canvas){val p=Paint(Paint.ANTI_ALIAS_FLAG);drawTier(c,p,tier1Items,tier1Radius,1);if(settings.tiers==2&&tier2Items.isNotEmpty())drawTier(c,p,tier2Items,tier2Radius,2)}
 private fun drawTier(c:Canvas,p:Paint,items:List<FanItem>,radius:Float,tier:Int){if(items.isEmpty())return;val per=sweepTotal/items.size;items.forEachIndexed{i,item->val start=startAngle+i*per;val sel=selectedTier==tier&&selectedIndex==i;p.color=item.color;p.alpha=if(sel)255 else 170;c.drawArc(RectF(vertexX-radius,vertexY-radius,vertexX+radius,vertexY+radius),start,per-2f,true,p);item.icon?.let{icon->val mid=Math.toRadians((start+per/2).toDouble());val ir=radius*.72f;val ix=vertexX+ir*cos(mid).toFloat();val iy=vertexY+ir*sin(mid).toFloat();val size=if(sel)iconSize*1.2f else iconSize;val h=size/2;icon.setBounds((ix-h).toInt(),(iy-h).toInt(),(ix+h).toInt(),(iy+h).toInt());icon.draw(c)}}}
 override fun onTouchEvent(e:MotionEvent)=false
 fun updateSelection(rawX:Float,rawY:Float){val loc=IntArray(2);getLocationOnScreen(loc);val x=rawX-loc[0];val y=rawY-loc[1];val dx=x-vertexX;val dy=y-vertexY;val dist=hypot(dx,dy);var angle=Math.toDegrees(atan2(dy.toDouble(),dx.toDouble())).toFloat();if(angle<0)angle+=360f;val n=angle-startAngle;if(n<0||n>sweepTotal){selectedTier=0;selectedIndex=-1;invalidate();return};if(dist<tier1Radius*.95f){selectedTier=1;selectedIndex=indexFromAngle(n,tier1Items.size)}else if(settings.tiers==2&&tier2Items.isNotEmpty()&&dist<tier2Radius*1.05f){selectedTier=2;selectedIndex=indexFromAngle(n,tier2Items.size)}else{selectedTier=0;selectedIndex=-1};invalidate()}
 fun commitSelection(){val item=when(selectedTier){1->tier1Items.getOrNull(selectedIndex);2->tier2Items.getOrNull(selectedIndex);else->null};item?.let{onItemSelected?.invoke(it)};selectedTier=0;selectedIndex=-1}
 private fun indexFromAngle(n:Float,count:Int):Int=if(count==0)-1 else(n/(sweepTotal/count)).toInt().coerceIn(0,count-1)
}