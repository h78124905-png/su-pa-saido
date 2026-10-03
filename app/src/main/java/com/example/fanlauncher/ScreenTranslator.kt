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
 private val ja=TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
 private val zh=TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
 private val ko=TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
 private val latin=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
 private val id=LanguageIdentification.getClient()
 suspend fun translateAndOverlay(context:Context,bitmap:Bitmap,wm:WindowManager){
  TranslationOverlay.showLoading(context,wm)
  val candidates=recognize(bitmap)
  val detected=candidates.mapNotNull{(kind,t)->if(t.text.isBlank())null else runCatching{id.identifyPossibleLanguages(t.text).await().maxByOrNull{it.confidence}?.let{Triple(t,norm(it.languageTag),(it.confidence+(if(kind==1&&norm(it.languageTag)=="zh")0.25f else 0f)).coerceAtMost(1f))}}.getOrNull()}.maxByOrNull{it.third}
  if(detected==null||detected.third<0.5f||detected.second=="und"){TranslationOverlay.show(context,wm,emptyList());return}
  val source=when(detected.second){"zh"->TranslateLanguage.CHINESE;"ja"->TranslateLanguage.JAPANESE;"ko"->TranslateLanguage.KOREAN;else->TranslateLanguage.fromLanguageTag(detected.second)}?:run{TranslationOverlay.show(context,wm,emptyList());return}
  if(source==TranslateLanguage.JAPANESE){TranslationOverlay.show(context,wm,detected.first.textBlocks.mapNotNull{b->b.boundingBox?.let{it to b.text}});return}
  val tr=Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(TranslateLanguage.JAPANESE).build())
  try{
   tr.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
   val out=coroutineScope{detected.first.textBlocks.mapNotNull{b->val r=b.boundingBox?:return@mapNotNull null;async{runCatching{tr.translate(b.text).await()}.getOrNull()?.takeIf{it.isNotBlank()}?.let{r to it}}}.awaitAll().filterNotNull()}
   TranslationOverlay.show(context,wm,out)
  }catch(_:Throwable){TranslationOverlay.show(context,wm,emptyList())}finally{tr.close()}
 }
 private fun norm(t:String)=when{t.startsWith("zh",true)->"zh";t.startsWith("ja",true)->"ja";t.startsWith("ko",true)->"ko";else->t}
 private suspend fun recognize(bitmap:Bitmap):List<Pair<Int,Text>>=coroutineScope{
  val input=InputImage.fromBitmap(bitmap,0)
  val z=runCatching{zh.process(input).await()}.getOrNull()
  if(z?.text?.any{it in '\u4e00'..'\u9fff'}==true)return@coroutineScope listOf(1 to z)
  val a=async{runCatching{0 to ja.process(input).await()}.getOrNull()}
  val b=async{runCatching{2 to ko.process(input).await()}.getOrNull()}
  val d=async{runCatching{3 to latin.process(input).await()}.getOrNull()}
  listOfNotNull(a.await(),b.await(),d.await())
 }
}