package com.alharamayn.mahamess.pro

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.util.Base64
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.view.WindowCompat
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * محامص الحرمين — تطبيق أندرويد احترافي.
 *
 * يعرض نسخة الويب الكاملة (React) من داخل assets/www، وبذلك يحصل على:
 *  • العمل دون إنترنت بشكل كامل (كل الملفات محلية، والتخزين في localStorage).
 *  • تجربة عرض كاملة بلا شريط عنوان.
 *  • جسور أصلية: حفظ الملفات/الصور، الطباعة، الإدخال الصوتي، زر الرجوع.
 */
class MainActivity : Activity() {

    private lateinit var web: WebView
    private var pendingSpeech = false
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        web = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                loadsImagesAutomatically = true
                useWideViewPort = true
                loadWithOverviewMode = true
                mediaPlaybackRequiresUserGesture = false
                textZoom = 100
                builtInZoomControls = false
                displayZoomControls = false
                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            }
            setBackgroundColor(0xFFF5F0E6.toInt())
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    return openExternally(url)
                }

                // ملفات محلية: تضمن أن أي مسار مطلق يعمل داخل التطبيق ولا يكسر الحزمة.
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val url = request?.url ?: return null
                    if (url.scheme == "https" && url.host == "localhost") {
                        val path = url.path?.removePrefix("/") ?: return null
                        val stream = runCatching { assets.open("www/$path") }.getOrNull() ?: return null
                        return WebResourceResponse(mimeOf(path), "UTF-8", stream)
                    }
                    return null
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(m: android.webkit.ConsoleMessage?): Boolean {
                    android.util.Log.d("MahamessWeb", m?.message() ?: "")
                    return true
                }

                // يسمح باختيار ملف النسخة الاحتياطية من ذاكرة الجهاز (استعادة).
                override fun onShowFileChooser(
                    view: WebView?,
                    callback: ValueCallback<Array<Uri>>?,
                    params: FileChooserParams?
                ): Boolean {
                    fileCallback?.onReceiveValue(null)
                    fileCallback = callback
                    return try {
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                        startActivityForResult(Intent.createChooser(intent, "اختر ملف النسخة الاحتياطية"), REQ_FILE)
                        true
                    } catch (_: Exception) {
                        fileCallback = null
                        false
                    }
                }
            }
            addJavascriptInterface(Bridge(), "AndroidApp")
            loadUrl("file:///android_asset/www/index.html")
        }

        val lp = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        setContentView(web, lp)
    }

    private fun mimeOf(path: String): String = when (path.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
        "html" -> "text/html"
        "js", "mjs" -> "application/javascript"
        "css" -> "text/css"
        "svg" -> "image/svg+xml"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "json", "webmanifest" -> "application/json"
        "woff" -> "font/woff"
        "woff2" -> "font/woff2"
        "ttf" -> "font/ttf"
        "mp3" -> "audio/mpeg"
        else -> "application/octet-stream"
    }

    /** يفتح الروابط الخارجية (واتساب ونحوه) في التطبيقات المناسبة. */
    private fun openExternally(url: String): Boolean {
        if (url.startsWith("file://") || url.startsWith("about:") || url.startsWith("blob:")) return false
        val host = Uri.parse(url).host ?: return false
        if (host == "localhost") return false
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            true
        } catch (_: Exception) {
            Toast.makeText(this, "لا يوجد تطبيق لفتح هذا الرابط", Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            web.evaluateJavascript("window.__onBack && window.__onBack()") { result ->
                val handled = result?.trim() == "true"
                if (!handled) {
                    if (web.canGoBack()) web.goBack() else finish()
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_FILE) {
            val cb = fileCallback
            fileCallback = null
            cb?.onReceiveValue(
                if (resultCode == RESULT_OK && data?.data != null) arrayOf(data.data!!) else null
            )
            return
        }
        if (requestCode == REQ_SPEECH) {
            pendingSpeech = false
            if (resultCode == RESULT_OK) {
                val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
                if (text.isNotBlank()) {
                    val safe = text.replace("\\", "\\\\").replace("'", "\\'")
                    web.evaluateJavascript("window.__onSpeechResult && window.__onSpeechResult('$safe')", null)
                }
            }
        }
    }

    inner class Bridge {

        /** حفظ ملف (واردة كـ base64) في مجلد «التنزيلات». */
        @JavascriptInterface
        fun saveBase64(filename: String, base64: String, mime: String): Boolean {
            return try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val safeName = filename.replace(Regex("[\\\\/:*?\"<>|]"), "-")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mime.ifBlank { "application/octet-stream" })
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    }
                } else {
                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!dir.exists()) dir.mkdirs()
                    FileOutputStream(File(dir, safeName)).use { it.write(bytes) }
                }
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "تم الحفظ في التنزيلات: $safeName", Toast.LENGTH_LONG).show()
                }
                true
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MainActivity, "تعذر حفظ الملف", Toast.LENGTH_SHORT).show() }
                false
            }
        }

        /** طباعة صفحة HTML عبر نظام الطباعة (يدعم الطابعات وخدمة Save as PDF). */
        @JavascriptInterface
        fun printHtml(html: String) {
            runOnUiThread {
                val printer = WebView(this@MainActivity)
                printer.settings.javaScriptEnabled = true
                printer.loadDataWithBaseURL("file:///android_asset/www/", html, "text/html", "UTF-8", null)
                printer.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        val manager = getSystemService(PRINT_SERVICE) as PrintManager
                        val adapter = view.createPrintDocumentAdapter("محامص-الحرمين")
                        manager.print("محامص الحرمين", adapter, PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A5)
                            .build())
                    }
                }
            }
        }

        /** طباعة صورة (إيصال حراري) عبر نظام الطباعة. */
        @JavascriptInterface
        fun printImageBase64(dataUrl: String) {
            val html = """
                <html dir="rtl"><head><meta charset="utf-8">
                <style>@page{margin:4mm} body{margin:0;text-align:center} img{width:72mm}</style>
                </head><body><img src="$dataUrl" onload="window.__ready=true"></body></html>
            """.trimIndent()
            printHtml(html)
        }

        /** إدخال صوتي أصلي (يعمل حتى حين لا يدعم WebView واجهة SpeechRecognition). */
        @JavascriptInterface
        fun startSpeech() {
            if (pendingSpeech) return
            pendingSpeech = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SY")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدّث باسم الصنف…")
            }
            runOnUiThread {
                try {
                    startActivityForResult(intent, REQ_SPEECH)
                } catch (_: Exception) {
                    pendingSpeech = false
                    Toast.makeText(this@MainActivity, "خدمة التعرف على الصوت غير متاحة على الجهاز", Toast.LENGTH_SHORT).show()
                }
            }
        }

        @JavascriptInterface
        fun toast(message: String) {
            runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show() }
        }

        @JavascriptInterface
        fun appVersion(): String = "2.0.0"
    }

    companion object {
        private const val REQ_SPEECH = 4701
        private const val REQ_FILE = 4702
    }
}
