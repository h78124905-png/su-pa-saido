# ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# Palette
-keep class androidx.palette.** { *; }

# AccessibilityService
-keep class com.example.fanlauncher.ScreenshotAccessibilityService { *; }

# App data classes
-keep class com.example.fanlauncher.FanSettings { *; }
-keep class com.example.fanlauncher.FanItem { *; }
