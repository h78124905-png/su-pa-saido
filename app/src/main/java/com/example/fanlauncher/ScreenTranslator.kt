package com.example.fanlauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.WindowManager
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.languageid.LanguageIdentification
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.translate.TranslateLanguage
import com.google.mlkit.translate.Translation
import com.google.mlkit.translate.Translator
import com.google.mlkit.translate.TranslatorOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

object ScreenTranslator {
    private val recognizers = listOf(
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()),
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()),
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()),
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    )
    private val languageIdentifier = LanguageIdentification.getClient()

    suspend fun translateAndOverlay(context: Context, bitmap: Bitmap, wm: WindowManager) {
        val candidates = recognizeCandidates(bitmap)
        val detected = candidates.mapNotNull { text ->
            if (text.text.isBlank()) return@mapNotNull null
            runCatching {
                val possible = languageIdentifier.identifyPossibleLanguages(text.text).await()
                val best = possible.maxByOrNull { it.confidence } ?: return@runCatching null
                Triple(text, best.languageTag, best.confidence)
            }.getOrNull()
        }.maxByOrNull { it.third }

        if (detected == null || detected.third < 0.5f || detected.second == "und") {
            return
        }

        val sourceLanguage = TranslateLanguage.fromLanguageTag(detected.second) ?: return
        if (sourceLanguage == TranslateLanguage.JAPANESE) {
            TranslationOverlay.show(context, wm, detected.first.textBlocks.mapNotNull { b ->
                b.boundingBox?.let { it to b.text }
            })
            return
        }

        val translator: Translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(TranslateLanguage.JAPANESE)
                .build()
        )

        try {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            val results = mutableListOf<Pair<Rect, String>>()
            for (block in detected.first.textBlocks) {
                val rect = block.boundingBox ?: continue
                val translated = runCatching { translator.translate(block.text).await() }.getOrNull() ?: continue
                results += rect to translated
            }
            if (results.isNotEmpty()) {
                TranslationOverlay.show(context, wm, results)
            }
        } finally {
            translator.close()
        }
    }

    private suspend fun recognizeCandidates(bitmap: Bitmap): List<Text> = coroutineScope {
        recognizers.map { recognizer ->
            async {
                runCatching {
                    recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
                }.getOrNull()
            }
        }.awaitAll().filterNotNull()
    }
}
