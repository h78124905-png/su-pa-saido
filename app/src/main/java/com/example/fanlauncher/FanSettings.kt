package com.example.fanlauncher
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
enum class EdgeSide { LEFT, RIGHT }
data class FanSettings(
 val edge:EdgeSide=EdgeSide.RIGHT,val edgeWidthDp:Int=48,val edgeTopRatio:Float=0f,val edgeBottomRatio:Float=1f,
 val swipeThresholdDp:Int=16,val tiers:Int=1,val tier1Count:Int=6,val tier2Count:Int=8,
 val tier1Apps:List<String> = emptyList(),val tier2Apps:List<String> = emptyList(),
 val fanSweepDeg:Int=90,val fanRadiusDp:Int=260,val actionRadiusDp:Int=70,val showScreenshot:Boolean=true,val showTranslate:Boolean=true,val showEdgeIndicator:Boolean=false){
 companion object{
  private const val PREF="fan_settings";private const val KEY="json"
  fun load(c:Context):FanSettings{val raw=c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY,null)?:return FanSettings();return try{val o=JSONObject(raw);FanSettings(
   edge=try{EdgeSide.valueOf(o.optString("edge","RIGHT"))}catch(_:Exception){EdgeSide.RIGHT},
   edgeWidthDp=o.optInt("ew",48).coerceIn(16,96),edgeTopRatio=o.optDouble("etr",0.0).toFloat().coerceIn(0f,1f),
   edgeBottomRatio=o.optDouble("ebr",1.0).toFloat().coerceIn(0f,1f),swipeThresholdDp=o.optInt("st",16).coerceIn(8,64),
   tiers=o.optInt("tiers",1).coerceIn(1,2),tier1Count=o.optInt("t1c",6).coerceIn(1,16),tier2Count=o.optInt("t2c",8).coerceIn(1,16),
   tier1Apps=o.optJSONArray("t1a").toList(),tier2Apps=o.optJSONArray("t2a").toList(),fanSweepDeg=o.optInt("sweep",90).coerceIn(90,180),fanRadiusDp=o.optInt("radius",260).coerceIn(160,420),actionRadiusDp=o.optInt("actionRadius",70).coerceIn(40,120),
   showScreenshot=o.optBoolean("ss",true),showTranslate=o.optBoolean("tr",true),showEdgeIndicator=o.optBoolean("sei",false))}catch(_:Exception){FanSettings()}}
  fun save(c:Context,s:FanSettings){val o=JSONObject().apply{put("edge",s.edge.name);put("ew",s.edgeWidthDp);put("etr",s.edgeTopRatio.toDouble());put("ebr",s.edgeBottomRatio.toDouble());put("st",s.swipeThresholdDp);put("tiers",s.tiers);put("t1c",s.tier1Count);put("t2c",s.tier2Count);put("t1a",JSONArray(s.tier1Apps));put("t2a",JSONArray(s.tier2Apps));put("sweep",s.fanSweepDeg);put("radius",s.fanRadiusDp);put("actionRadius",s.actionRadiusDp);put("ss",s.showScreenshot);put("tr",s.showTranslate);put("sei",s.showEdgeIndicator)};c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(KEY,o.toString()).apply()}
  private fun JSONArray?.toList():List<String> = if(this==null)emptyList()else(0 until length()).map{optString(it)}
 }
}