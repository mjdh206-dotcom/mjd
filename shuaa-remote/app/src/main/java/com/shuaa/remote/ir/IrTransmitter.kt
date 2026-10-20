package com.shuaa.remote.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Build
import com.shuaa.remote.data.IrSignal
import kotlin.math.abs

/**
 * إرسال الأشعة تحت الحمراء عبر مُرسِل الجهاز (ConsumerIrManager).
 * يعمل على أجهزة شاومي/ريدمي/هواوي/سامسونج القديمة وغيرها ممن لديها منفذ IR.
 */
class IrTransmitter(context: Context) {

    private val manager: ConsumerIrManager? =
        context.applicationContext.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    val hasEmitter: Boolean
        get() = try {
            manager?.hasIrEmitter() == true
        } catch (_: Exception) {
            false
        }

    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    /** ترددات الموجة الحاملة التي يدعمها الجهاز */
    private val supportedRanges: List<IntRange> by lazy {
        try {
            manager?.carrierFrequencies?.map { it.minFrequency..it.maxFrequency } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun carrierFor(requested: Int): Int {
        if (supportedRanges.isEmpty()) return requested
        if (supportedRanges.any { requested in it }) return requested
        // أقرب تردد مدعوم
        val candidates = listOf(36000, 38000, 40000, 56000)
        val flat = supportedRanges.flatMap { listOf(it.first, it.last) }
        return candidates.firstOrNull { c -> supportedRanges.any { c in it } }
            ?: flat.minByOrNull { abs(it - requested) }
            ?: requested
    }

    fun send(signal: IrSignal): Result<Unit> {
        val encoded = IrEncoder.encode(signal)
            ?: return Result.failure(IllegalArgumentException("بروتوكول غير مدعوم: ${signal.protocol}"))
        return sendEncoded(encoded)
    }

    fun sendEncoded(encoded: EncodedIr): Result<Unit> {
        val service = manager
            ?: return Result.failure(IllegalStateException("لا يوجد مُرسِل أشعة تحت الحمراء في هذا الجهاز"))
        if (!hasEmitter) {
            return Result.failure(IllegalStateException("مُرسِل الأشعة غير متاح"))
        }
        return try {
            service.transmit(carrierFor(encoded.carrierHz), encoded.tap)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** إرسال نبضة تكرار واحدة أثناء الاستمرار بالضغط */
    fun sendHold(encoded: EncodedIr): Result<Unit> {
        val service = manager ?: return Result.failure(IllegalStateException("لا يوجد مُرسِل أشعة"))
        return try {
            service.transmit(carrierFor(encoded.carrierHz), encoded.hold)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun describe(): String = if (hasEmitter) {
        "مُرسِل الأشعة جاهز · $deviceModel"
    } else {
        "هذا الجهاز لا يحتوي على مُرسِل أشعة تحت الحمراء"
    }
}
