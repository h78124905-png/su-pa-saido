package com.example.fanlauncher

import android.graphics.drawable.Drawable

data class FanItem(
    val label: String,
    val icon: Drawable?,
    val color: Int,
    val action: FanAction
)

sealed class FanAction {
    data class LaunchApp(val packageName: String) : FanAction()
    data object Screenshot : FanAction()
    data object Translate : FanAction()
}
