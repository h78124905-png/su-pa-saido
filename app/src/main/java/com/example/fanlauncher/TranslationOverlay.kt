package com.example.fanlauncher
import android.content.Context
import android.graphics.*
import android.view.*
import android.widget.Button
import android.widget.FrameLayout
object TranslationOverlay{
 private var active:View?=null
 private fun showInternal(context:Context,wm:WindowManager,content:View){
  active?.let{runCatching{wm.removeView(it)}}
  val d=context.resources.displayMetrics.density
  val root=FrameLayout(context).apply{setBackgroundColor(Color.argb(210,45,45,45));addView(content,FrameLayout.LayoutParams(-1,-1))}
  root.addView(Button(context).apply{text="閉じる";setOnClickListener{clear(wm)}},FrameLayout.LayoutParams((96*d).toInt(),(52*d).toInt(),Gravity.TOP or Gravity.END).apply{topMargin=(12*d).toInt();rightMargin=(12*d).toInt()})
  val p=WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START}
  wm.addView(root,p);active=root
 }
 fun showLoading(context:Context,wm:WindowManager){val d=context.resources.displayMetrics.density;showInternal(context,wm,object:View(context){private val p=Paint(1).apply{color=Color.WHITE;textSize=24f*d;textAlign=Paint.Align.CENTER};override fun onDraw(c:Canvas){c.drawText("変換中…",width/2f,height/2f,p)}})}
 fun show(context:Context,wm:WindowManager,items:List<Pair<Rect,String>>){
  val d=context.resources.displayMetrics.density
  showInternal(context,wm,object:View(context){
   private val tp=Paint(1).apply{color=Color.WHITE;setShadowLayer(4f*d,0f,d,Color.BLACK)}
   private val bp=Paint(1).apply{color=Color.argb(125,0,0,0)}
   override fun onDraw(c:Canvas){for((r,t) in items){if(t.isBlank())continue;tp.textSize=(r.height()*.72f).coerceIn(12f*d,28f*d);val x=r.left.toFloat().coerceIn(8f*d,width-8f*d);val maxW=(width-r.left-12f*d).coerceAtLeast(80f*d);val lines=mutableListOf<String>();var line="";for(w in t.split(" ")){val test=if(line.isEmpty())w else "$line $w";if(tp.measureText(test)<=maxW)line=test else{if(line.isNotEmpty())lines+=line;line=w}};if(line.isNotEmpty())lines+=line;if(lines.isEmpty())lines+=t;var y=(r.top+tp.textSize).coerceAtMost(height-8f*d);for(s in lines.take(5)){val right=(x+tp.measureText(s)+16f*d).coerceAtMost(width-8f*d);val top=(y-tp.textSize-6f*d).coerceAtLeast(4f*d);c.drawRoundRect((x-8f*d).coerceAtLeast(4f*d),top,right,y+5f*d,8f*d,8f*d,bp);c.drawText(s,x,y,tp);y+=tp.textSize*1.12f}}}
  })
 }
 fun clear(wm:WindowManager){active?.let{runCatching{wm.removeView(it)}};active=null}
}