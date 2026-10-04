package com.example.fanlauncher
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class EdgeSide { LEFT, RIGHT }

data class FanSettings(
    val edge: EdgeSide = EdgeSide.RIGHT,
    val edgeWidthDp: Int = 48,
    val edgeTopRatio: Float = 0f,
    val edgeBottomRatio: Float = 1f,
    val swipeThresholdDp: Int = 16,
    val tier1Count: Int = 6,
    val tier2Count: Int = 6,
    val tier3Count: Int = 6,
    val tier1Apps: List<String> = emptyList(),
    val tier2Apps: List<String> = emptyList(),
    val tier3Apps: List<String> = emptyList(),
    val fanWidthDp: Int = 360,
    val fanHeightDp: Int = 320,
    val actionRadiusDp: Int = 104,
    val tier1Ratio: Int = 34,
    val tier2Ratio: Int = 33,
    val tier3Ratio: Int = 33,
    val showScreenshot: Boolean = true
) {
    companion object {
        private const val PREF = "fan_settings"
        private const val KEY = "json"

        fun load(c: Context): FanSettings {
            val raw = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, null) ?: return FanSettings()
            return try {
                val o = JSONObject(raw)
                val n = normalizeRatios(o.optInt("r1",34), o.optInt("r2",33), o.optInt("r3",33))
                FanSettings(
                    edge = runCatching { EdgeSide.valueOf(o.optString("edge","RIGHT")) }.getOrDefault(EdgeSide.RIGHT),
                    edgeWidthDp = o.optInt("ew",48).coerceIn(16,96),
                    edgeTopRatio = o.optDouble("etr",0.0).toFloat().coerceIn(0f,1f),
                    edgeBottomRatio = o.optDouble("ebr",1.0).toFloat().coerceIn(0f,1f),
                    swipeThresholdDp = o.optInt("st",16).coerceIn(8,64),
                    tier1Count = o.optInt("t1c",6).coerceIn(1,16),
                    tier2Count = o.optInt("t2c",6).coerceIn(1,16),
                    tier3Count = o.optInt("t3c",6).coerceIn(1,16),
                    tier1Apps = o.optJSONArray("t1a").toList(),
                    tier2Apps = o.optJSONArray("t2a").toList(),
                    tier3Apps = o.optJSONArray("t3a").toList(),
                    fanWidthDp = o.optInt("width",o.optInt("radius",360)).coerceIn(180,700),
                    fanHeightDp = o.optInt("height",o.optInt("radius",320)).coerceIn(180,700),
                    actionRadiusDp = o.optInt("actionRadius",104).coerceIn(48,240),
                    tier1Ratio=n[0],tier2Ratio=n[1],tier3Ratio=n[2],
                    showScreenshot = o.optBoolean("ss",true)
                )
            } catch (_: Exception) { FanSettings() }
        }

        fun save(c: Context,s: FanSettings) {
            val o=JSONObject().apply{
                put("edge",s.edge.name);put("ew",s.edgeWidthDp);put("etr",s.edgeTopRatio.toDouble());put("ebr",s.edgeBottomRatio.toDouble())
                put("st",s.swipeThresholdDp);put("t1c",s.tier1Count);put("t2c",s.tier2Count);put("t3c",s.tier3Count)
                put("t1a",JSONArray(s.tier1Apps));put("t2a",JSONArray(s.tier2Apps));put("t3a",JSONArray(s.tier3Apps))
                put("width",s.fanWidthDp);put("height",s.fanHeightDp);put("actionRadius",s.actionRadiusDp)
                put("r1",s.tier1Ratio);put("r2",s.tier2Ratio);put("r3",s.tier3Ratio);put("ss",s.showScreenshot)
            }
            c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(KEY,o.toString()).apply()
        }

        fun normalizeRatios(a0:Int,b0:Int,c0:Int):IntArray{
            var a=a0.coerceIn(10,80);var b=b0.coerceIn(10,80);var c=c0.coerceIn(10,80)
            val total=a+b+c
            if(total!=100){
                a=kotlin.math.round(a*100f/total).toInt().coerceAtLeast(10)
                b=kotlin.math.round(b*100f/total).toInt().coerceAtLeast(10)
                c=100-a-b
                if(c<10){c=10;b=(100-a-c).coerceAtLeast(10)}
                if(a+b+c!=100){a=34;b=33;c=33}
            }
            return intArrayOf(a,b,c)
        }
    }

    fun withTierRatio(index:Int,value:Int):FanSettings{
        val v=value.coerceIn(10,80)
        val old=when(index){1->intArrayOf(tier2Ratio,tier3Ratio);2->intArrayOf(tier1Ratio,tier3Ratio);else->intArrayOf(tier1Ratio,tier2Ratio)}
        val remain=100-v
        val sum=(old[0]+old[1]).coerceAtLeast(1)
        val a=(remain*old[0]/sum).coerceAtLeast(10)
        val b=remain-a
        return when(index){
            1->copy(tier1Ratio=v,tier2Ratio=a,tier3Ratio=b).let{normalize(it)}
            2->copy(tier1Ratio=a,tier2Ratio=v,tier3Ratio=b).let{normalize(it)}
            else->copy(tier1Ratio=a,tier2Ratio=b,tier3Ratio=v).let{normalize(it)}
        }
    }

    private fun normalize(s:FanSettings):FanSettings{
        val n=Companion.normalizeRatios(s.tier1Ratio,s.tier2Ratio,s.tier3Ratio)
        return s.copy(tier1Ratio=n[0],tier2Ratio=n[1],tier3Ratio=n[2])
    }
    private fun JSONArray?.toList():List<String> = if(this==null)emptyList() else (0 until length()).map{optString(it)}
}

