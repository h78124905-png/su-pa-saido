package com.example.fanlauncher
import android.content.Context
import android.graphics.*
import android.view.*
import android.widget.Button
import android.widget.FrameLayout
object TranslationOverlay {
 private var activeRoot:View?=null
 private fun showInternal(context:Context,wm:WindowManager,content:View){
  activeRoot?.let{runCatching{wm.removeView(it)}}
  val d=context.resources.displayMetrics.density
  val root=FrameLayout(context).apply{setBackgroundColor(Color.argb(210,45,45,45));addView(content,FrameLayout.LayoutParams(-1,-1))}
  root.addView(Button(context).apply{text="閉じる";setOnClickListener{clear(wm)}},FrameLayout.LayoutParams((96*d).toInt(),(52*d).toInt(),Gravity.TOP or Gravity.END).apply{topMargin=(12*d).toInt();rightMargin=(12*d).toInt()})
  val p=WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START}
  wm.addView(root,p);activeRoot=root
 }
 fun showLoading(context:Context,wm:WindowManager){
  val d=context.resources.displayMetrics.density
  showInternal(context,wm,object:View(context){
   private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE;textSize=26f*d;textAlign=Paint.Align.CENTER}
   override fun onDraw(c:Canvas){c.drawText("変換中…",width/2f,height/2f,paint)}
  })
 }
 fun show(context:Context,wm:WindowManager,items:List<Pair<Rect,String>>){
  val d=context.resources.displayMetrics.density
  showInternal(context,wm,object:View(context){
   private val tp=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE;textSize=30f*d;setShadowLayer(5f*d,0f,1f*d,Color.BLACK)}
   private val bp=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.argb(125,0,0,0)}
   override fun onDraw(c:Canvas){items.forEach{(r,t)->if(t.isNotBlank()){val x=r.left.toFloat();val y=(r.top+tp.textSize).coerceAtMost(height-8f);val l=x-10f*d;val top=y-tp.textSize-8f*d;val right=(x+tp.measureText(t)+18f*d).coerceAtMost(width-8f*d);c.drawRoundRect(l,top,right,y+8f*d,10f*d,10f*d,bp);c.drawText(t,x,y,tp)}}}
  })
 }
 fun clear(wm:WindowManager){activeRoot?.let{runCatching{wm.removeView(it)}};activeRoot=null}
}