package com.example.fanlauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import androidx.core.graphics.drawable.toBitmap

object AppColorExtractor {
    fun extract(context: Context, packageName: String): Int {
        return try {
            val icon = context.packageManager.getApplicationIcon(packageName)
            if (icon is AdaptiveIconDrawable && icon.background is ColorDrawable) {
                return (icon.background as ColorDrawable).color
            }
            val bitmap = icon.toBitmap(48, 48, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            var r=0L; var g=0L; var b=0L; var count=0L
            for (p in pixels) {
                if (Color.alpha(p) < 48) continue
                r+=Color.red(p); g+=Color.green(p); b+=Color.blue(p); count++
            }
            if (count==0L) 0xFF607D8B.toInt()
            else Color.rgb((r/count).toInt(),(g/count).toInt(),(b/count).toInt())
        } catch (_: Exception) { 0xFF607D8B.toInt() }
    }
}
