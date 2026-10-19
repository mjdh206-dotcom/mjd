package com.alharamayn.mahamess.pro

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewAssetLoader
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
    private var pendingThermal: String? = null

    // تقديم حزمة الويب عبر أصل https آمن بدل file:// — يضمن عمل وحدات ES والتخزين المحلي.
    private val assetLoader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

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

                // تقديم ملفات الحزمة من داخل التطبيق عبر أصل https آمن.
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val url = request?.url ?: return null
                    if (url.host == "appassets.androidplatform.net") {
                        return assetLoader.shouldInterceptRequest(url)
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
            loadUrl("https://appassets.androidplatform.net/assets/www/index.html")
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
        if (host == "localhost" || host == "appassets.androidplatform.net") return false
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_BT_PERM) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) choosePrinter()
            else toastMsg("يلزم السماح بالوصول للبلوتوث للطباعة")
        }
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
                printer.loadDataWithBaseURL("https://appassets.androidplatform.net/assets/www/", html, "text/html", "UTF-8", null)
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

        /** طباعة إيصال حراري على طابعة بلوتوث (ESC/POS). */
        @JavascriptInterface
        fun printThermal(dataUrl: String) {
            pendingThermal = dataUrl
            runOnUiThread {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQ_BT_PERM)
                } else {
                    choosePrinter()
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

    private fun toastMsg(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    /** اختيار طابعة من الأجهزة المقترنة ثم بدء الإرسال. */
    private fun choosePrinter() {
        val data = pendingThermal ?: return
        val adapter = try { BluetoothAdapter.getDefaultAdapter() } catch (_: Exception) { null }
        if (adapter == null) { toastMsg("الجهاز لا يدعم البلوتوث"); pendingThermal = null; return }
        if (!adapter.isEnabled) { toastMsg("فعّل البلوتوث أولاً"); pendingThermal = null; return }
        val bonded = try { adapter.bondedDevices.toList() } catch (_: SecurityException) { emptyList() }
        if (bonded.isEmpty()) { toastMsg("لا توجد طابعة مقترنة — اقترن بها من إعدادات البلوتوث"); pendingThermal = null; return }

        val saved = getSharedPreferences("printer", MODE_PRIVATE).getString("address", null)
        val known = bonded.firstOrNull { it.address == saved }
        if (known != null) { sendThermal(known, data); return }

        val names = bonded.map { it.name ?: it.address }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("اختر طابعة الإيصالات")
            .setItems(names) { _, which ->
                val device = bonded[which]
                getSharedPreferences("printer", MODE_PRIVATE).edit().putString("address", device.address).apply()
                sendThermal(device, data)
            }
            .setOnCancelListener { pendingThermal = null }
            .show()
    }

    /** تحويل صورة الإيصال إلى أوامر ESC/POS وإرسالها على خيط منفصل. */
    private fun sendThermal(device: BluetoothDevice, dataUrl: String) {
        pendingThermal = null
        Thread {
            var socket: BluetoothSocket? = null
            try {
                val base64 = dataUrl.substringAfter(",", dataUrl)
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw IllegalStateException("صورة غير صالحة")
                val command = escPosRaster(bitmap)
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                socket.outputStream.use { out ->
                    out.write(byteArrayOf(0x1B, 0x40))      // init
                    out.write(command)
                    out.write(byteArrayOf(0x0A, 0x0A, 0x0A)) // feed
                    out.write(byteArrayOf(0x1D, 0x56, 0x00)) // cut
                    out.flush()
                }
                runOnUiThread { toastMsg("تم إرسال الإيصال إلى ${device.name ?: "الطابعة"}") }
            } catch (e: Exception) {
                runOnUiThread { toastMsg("تعذر الاتصال بالطابعة — تأكد من تشغيلها واقترانها") }
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }.start()
    }

    /** ESC/POS GS v 0: تحويل الصورة إلى أبيض/أسود بعرض 576 أو 384 نقطة. */
    private fun escPosRaster(source: Bitmap): ByteArray {
        val targetWidth = if (source.width > 420) 576 else 384
        val ratio = targetWidth.toFloat() / source.width
        val width = targetWidth
        val height = (source.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(source, width, height, true)
        val bytesPerRow = (width + 7) / 8
        val out = java.io.ByteArrayOutputStream()
        var y = 0
        while (y < height) {
            val band = minOf(255, height - y)
            out.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, (bytesPerRow and 0xFF).toByte(), ((bytesPerRow shr 8) and 0xFF).toByte(), (band and 0xFF).toByte(), ((band shr 8) and 0xFF).toByte()))
            for (row in 0 until band) {
                var current = 0
                var bit = 0
                for (x in 0 until width) {
                    val pixel = scaled.getPixel(x, y + row)
                    val gray = (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114)
                    val alpha = Color.alpha(pixel)
                    val black = alpha > 40 && gray < 150
                    if (black) current = current or (0x80 shr bit)
                    bit++
                    if (bit == 8) { out.write(current); current = 0; bit = 0 }
                }
                if (bit > 0) out.write(current)
            }
            y += band
        }
        scaled.recycle()
        return out.toByteArray()
    }

    companion object {
        private const val REQ_SPEECH = 4701
        private const val REQ_FILE = 4702
        private const val REQ_BT_PERM = 4703
        private val SPP_UUID: java.util.UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
