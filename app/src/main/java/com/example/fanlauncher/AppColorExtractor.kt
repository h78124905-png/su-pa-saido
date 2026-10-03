package com.example.fanlauncher

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppColorExtractor {
    suspend fun extract(context: Context, packageName: String): Int =
        withContext(Dispatchers.IO) {
            try {
                val icon = context.packageManager.getApplicationIcon(packageName)
                if (icon is AdaptiveIconDrawable) {
                    val background = icon.background
                    if (background is ColorDrawable) return@withContext background.color
                }
                val bitmap = icon.toBitmap()
                if (bitmap.width <= 0 || bitmap.height <= 0) {
                    return@withContext 0xFF607D8B.toInt()
                }
                val palette = Palette.from(bitmap).generate()
                palette.getVibrantColor(
                    palette.getMutedColor(
                        palette.getDominantColor(0xFF607D8B.toInt())
                    )
                )
            } catch (_: Exception) {
                0xFF607D8B.toInt()
            }
        }
}
