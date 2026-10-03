package com.example.fanlauncher
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.WindowManager
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.nl.translate.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
object ScreenTranslator{
 private val recognizers=listOf(TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()),TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()),TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()),TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS))
 private val languageIdentifier=LanguageIdentification.getClient()
 suspend fun translateAndOverlay(context:Context,bitmap:Bitmap,wm:WindowManager){
  TranslationOverlay.showLoading(context,wm)
  val detected=recognizeCandidates(bitmap).mapNotNull{(kind,t)->if(t.text.isBlank())null else runCatching{languageIdentifier.identifyPossibleLanguages(t.text).await().maxByOrNull{it.confidence}?.let{val tag=norm(it.languageTag);val bonus=if(kind==1&&tag=="zh")0.25f else if(kind==0&&tag=="ja")0.15f else if(kind==2&&tag=="ko")0.15f else 0f;Triple(t,tag,(it.confidence+bonus).coerceAtMost(1f))}}.getOrNull()}.maxByOrNull{it.third}
  if(detected==null||detected.third<0.5f||detected.second=="und"){TranslationOverlay.show(context,wm,emptyList());return}
  val source=when(detected.second){"zh"->TranslateLanguage.CHINESE;"ja"->TranslateLanguage.JAPANESE;"ko"->TranslateLanguage.KOREAN;else->TranslateLanguage.fromLanguageTag(detected.second)}?:run{TranslationOverlay.show(context,wm,emptyList());return}
  if(source==TranslateLanguage.JAPANESE){TranslationOverlay.show(context,wm,detected.first.textBlocks.mapNotNull{b->b.boundingBox?.let{it to b.text}});return}
  val translator=Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(TranslateLanguage.JAPANESE).build())
  try{translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await();val out=mutableListOf<Pair<Rect,String>>();for(b in detected.first.textBlocks){val r=b.boundingBox?:continue;val t=runCatching{translator.translate(b.text).await()}.getOrNull()?:continue;if(t.isNotBlank())out+=r to t};TranslationOverlay.show(context,wm,out)}catch(_:Throwable){TranslationOverlay.show(context,wm,emptyList())}finally{translator.close()}
 }
 private fun norm(t:String)=when{t.startsWith("zh-",true)||t.equals("zh",true)->"zh";t.startsWith("ja",true)->"ja";t.startsWith("ko",true)->"ko";else->t}
 private suspend fun recognizeCandidates(bitmap:Bitmap):List<Pair<Int,Text>> = coroutineScope{recognizers.mapIndexed{index,r->async{runCatching{index to r.process(InputImage.fromBitmap(bitmap,0)).await()}.getOrNull()}}.awaitAll().filterNotNull()}
}