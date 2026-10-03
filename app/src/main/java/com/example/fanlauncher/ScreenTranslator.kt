package com.example.fanlauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.WindowManager
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ScreenTranslator {
    private val recognizer = TextRecognition.getClient(
        JapaneseTextRecognizerOptions.Builder().build()
    )

    private val translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.JAPANESE)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
    )

    suspend fun translateAndOverlay(
        context: Context,
        bitmap: Bitmap,
        wm: WindowManager,
        parent: android.view.ViewGroup
    ) {
        val text = recognize(bitmap) ?: return

        translator.downloadModelIfNeeded(
            DownloadConditions.Builder().build()
        ).await()

        val results = mutableListOf<Pair<Rect, String>>()
        for (block in text.textBlocks) {
            val rect = block.boundingBox ?: continue
            val translated = translator.translate(block.text).await()
            results += rect to translated
        }

        TranslationOverlay.show(context, wm, parent, results)
    }

    private suspend fun recognize(bitmap: Bitmap): Text? =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resume(null) }
        }
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
    }
