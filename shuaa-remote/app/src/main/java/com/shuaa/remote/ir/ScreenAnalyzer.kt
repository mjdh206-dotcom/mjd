package com.shuaa.remote.ir

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlin.math.abs

/** نتيجة تحليل صورة الكاميرا للبحث عن شاشة */
data class ScanResult(
    val detected: Boolean = false,
    val brightness: Int = 0,
    val contrast: Int = 0,
    val hint: String = "وجّه الكاميرا نحو الشاشة…",
    val aspect: String? = null,
)

/**
 * تحليل بسيط وخفيض الكلفة لصورة الكاميرا:
 * نبحث عن مساحة مضيئة مستطيلة في وسط الإطار (الشاشة) ونقدّر نسبة أبعادها.
 */
class ScreenAnalyzer(
    private val onResult: (ScanResult) -> Unit,
) : ImageAnalysis.Analyzer {

    private val cols = 40
    private val rows = 30
    private var frameCount = 0

    override fun analyze(image: ImageProxy) {
        try {
            frameCount++
            if (frameCount % 3 != 0) return // تخفيف الحمل: نحلّل إطاراً من كل ثلاثة
            val result = analyzeFrame(image)
            onResult(result)
        } catch (_: Exception) {
        } finally {
            image.close()
        }
    }

    private fun analyzeFrame(image: ImageProxy): ScanResult {
        val width = image.width
        val height = image.height
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val limit = buffer.limit()

        val grid = IntArray(cols * rows)
        var sum = 0L
        for (ry in 0 until rows) {
            val y = (((ry + 0.5f) * height) / rows).toInt().coerceIn(0, height - 1)
            for (rx in 0 until cols) {
                val x = (((rx + 0.5f) * width) / cols).toInt().coerceIn(0, width - 1)
                val index = y * rowStride + x * pixelStride
                val value = if (index in 0 until limit) buffer.get(index).toInt() and 0xFF else 0
                grid[ry * cols + rx] = value
                sum += value
            }
        }

        val count = cols * rows
        val mean = (sum / count).toInt()
        var variance = 0.0
        for (v in grid) variance += (v - mean) * (v - mean)
        val contrast = kotlin.math.sqrt(variance / count).toInt()

        // متوسط الوسط مقابل الأطراف
        var centerSum = 0L
        var centerCount = 0
        var borderSum = 0L
        var borderCount = 0
        val c0 = cols / 4
        val c1 = cols - c0
        val r0 = rows / 5
        val r1 = rows - r0
        for (ry in 0 until rows) {
            for (rx in 0 until cols) {
                val v = grid[ry * cols + rx].toLong()
                if (rx in c0 until c1 && ry in r0 until r1) {
                    centerSum += v; centerCount++
                } else {
                    borderSum += v; borderCount++
                }
            }
        }
        val centerMean = if (centerCount > 0) (centerSum / centerCount).toInt() else 0
        val borderMean = if (borderCount > 0) (borderSum / borderCount).toInt() else 0

        // مربّع الإحاطة للمناطق المضيئة
        val threshold = (centerMean + borderMean) / 2 + 18
        var minX = cols; var maxX = -1; var minY = rows; var maxY = -1
        for (ry in r0 until r1) {
            for (rx in c0 until c1) {
                if (grid[ry * cols + rx] >= threshold) {
                    if (rx < minX) minX = rx
                    if (rx > maxX) maxX = rx
                    if (ry < minY) minY = ry
                    if (ry > maxY) maxY = ry
                }
            }
        }

        val hasBox = maxX >= 0 && maxY >= 0
        val boxW = if (hasBox) (maxX - minX + 1).toFloat() else 0f
        val boxH = if (hasBox) (maxY - minY + 1).toFloat() else 0f
        val coverage = if (hasBox) (boxW * boxH) / (cols * rows).toFloat() else 0f

        val detected = centerMean > 50 && abs(centerMean - borderMean) > 8 && hasBox && boxW > cols * 0.18f

        val aspect = if (detected && boxH > 0f) {
            val ratio = boxW / boxH
            when {
                abs(ratio - 16f / 9f) < 0.35f -> "16:9"
                abs(ratio - 4f / 3f) < 0.35f -> "4:3"
                abs(ratio - 21f / 9f) < 0.5f -> "21:9"
                else -> null
            }
        } else {
            null
        }

        val hint = when {
            mean < 22 -> "المشهد مظلم — أضِف إضاءة أو قرّب الهاتف"
            !detected && centerMean < 40 -> "وجّه الكاميرا نحو الشاشة مباشرة"
            !detected -> "ثبّت الهاتف قليلاً حتى تكتمل حدود الشاشة"
            else -> "تم رصد الشاشة — تابع لاختيار الماركة"
        }

        return ScanResult(detected, centerMean, contrast, hint, aspect)
    }
}
