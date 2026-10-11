package com.mjd.videoforge.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * أدوات مساعدة عامة
 */

object NetworkUtils {
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isWifi(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}

object FileUtils {
    fun getCacheDir(context: Context, subDir: String): File {
        val dir = File(context.cacheDir, subDir)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getExportDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getTempDir(context: Context): File {
        val dir = File(context.cacheDir, "temp")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun clearCache(context: Context) {
        context.cacheDir.deleteRecursively()
    }

    fun getCacheSize(context: Context): Long {
        return context.cacheDir.walkTopDown().sumOf { it.length() }
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}

object DateUtils {
    fun formatDate(date: Date, pattern: String = "yyyy-MM-dd"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }

    fun formatTime(date: Date): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }

    fun formatArabicDate(date: Date): String {
        return SimpleDateFormat("d MMMM yyyy", Locale("ar")).format(date)
    }
}

object ArabicTextUtils {
    /**
     * إضافة تشكيل تلقائي للنص العربي لتحسين نطق التعليق الصوتي
     */
    fun addTashkeel(text: String): String {
        // In real implementation: use an Arabic NLP model to add diacritics
        return text
    }

    /**
     * تطبيع النص العربي (إزالة التشكيل الزائد، توحيد الألف، إلخ)
     */
    fun normalize(text: String): String {
        return text
            .replace("إ", "ا")
            .replace("أ", "ا")
            .replace("آ", "ا")
            .replace("ى", "ي")
            .replace("ة", "ه")
            .trim()
    }

    /**
     * تحويل الأرقام إلى حروف عربية
     */
    fun arabicNumbers(number: Int): String {
        return when (number) {
            1 -> "واحد"; 2 -> "اثنان"; 3 -> "ثلاثة"; 4 -> "أربعة"; 5 -> "خمسة"
            6 -> "ستة"; 7 -> "سبعة"; 8 -> "ثمانية"; 9 -> "تسعة"; 10 -> "عشرة"
            else -> number.toString()
        }
    }
}

/**
 * Exponential Backoff strategy for network retries
 */
object RetryPolicy {
    fun getDelay(attempt: Int, baseDelayMs: Long = 1000, maxDelayMs: Long = 30000): Long {
        val delay = baseDelayMs * (1L shl attempt.coerceAtMost(5))
        return delay.coerceAtMost(maxDelayMs)
    }
}
