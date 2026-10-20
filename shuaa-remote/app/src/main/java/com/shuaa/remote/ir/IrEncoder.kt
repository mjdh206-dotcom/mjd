package com.shuaa.remote.ir

import com.shuaa.remote.data.IrSignal
import kotlin.math.max

/**
 * النمط الجاهز للإرسال.
 *
 * tap  : نمط الضغطة الواحدة (فترات بالميكروثانية، تبدأ بشرارة IR)
 * hold : نمط التكرار عند الاستمرار بالضغط
 */
data class EncodedIr(
    val carrierHz: Int,
    val tap: IntArray,
    val hold: IntArray,
)

/**
 * مُرمِّز بروتوكولات الأشعة تحت الحمراء المستخدمة في الشاشات القديمة والحديثة.
 *
 * المدعوم: NEC · NECext · Samsung32 · SIRC/SIRC15/SIRC20 · RC5 · RC5X · RC6 · RCA · Kaseikyo.
 * الأزمنة مأخوذة من المواصفات القياسية ومن مشروع Flipper Zero (GPLv3) كمرجع.
 */
object IrEncoder {

    private const val NEC_HDR_MARK = 9000
    private const val NEC_HDR_SPACE = 4500
    private const val NEC_BIT_MARK = 560
    private const val NEC_ONE_SPACE = 1690
    private const val NEC_ZERO_SPACE = 560
    private const val NEC_REPEAT_SPACE = 2250

    private const val SAM_HDR_MARK = 4500
    private const val SAM_HDR_SPACE = 4500
    private const val SAM_BIT_MARK = 550
    private const val SAM_ONE_SPACE = 1650
    private const val SAM_ZERO_SPACE = 550

    private const val SIRC_HDR_MARK = 2400
    private const val SIRC_HDR_SPACE = 600
    private const val SIRC_ONE_MARK = 1200
    private const val SIRC_ZERO_MARK = 600
    private const val SIRC_SPACE = 600

    private const val RCA_HDR_MARK = 4000
    private const val RCA_HDR_SPACE = 4000
    private const val RCA_BIT_MARK = 500
    private const val RCA_ONE_SPACE = 2000
    private const val RCA_ZERO_SPACE = 1000

    private const val KASEIKYO_UNIT = 432

    fun encode(signal: IrSignal): EncodedIr? = try {
        when (signal.protocol.uppercase()) {
            "NEC" -> nec(signal, extended = false)
            "NECEXT" -> nec(signal, extended = true)
            "SAMSUNG32" -> samsung(signal)
            "SIRC" -> sirc(signal, 5)
            "SIRC15" -> sirc(signal, 8)
            "SIRC20" -> sirc(signal, 13)
            "RC5" -> rc5(signal, extended = false)
            "RC5X" -> rc5(signal, extended = true)
            "RC6" -> rc6(signal)
            "RCA" -> rca(signal)
            "KASEIKYO" -> kaseikyo(signal)
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    /* ------------------------- أدوات مساعدة ------------------------- */

    private fun bits(value: Long, count: Int): IntArray =
        IntArray(count) { ((value ushr it) and 1L).toInt() }

    private fun pdwm(
        data: IntArray,
        headerMark: Int,
        headerSpace: Int,
        mark: Int,
        oneSpace: Int,
        zeroSpace: Int,
        stopMark: Int,
    ): IntArray {
        val out = ArrayList<Int>(data.size * 2 + 3)
        out.add(headerMark)
        out.add(headerSpace)
        for (b in data) {
            out.add(mark)
            out.add(if (b == 1) oneSpace else zeroSpace)
        }
        out.removeAt(out.size - 1)
        out.add(stopMark)
        return out.toIntArray()
    }

    private fun pwmMark(
        data: IntArray,
        headerMark: Int,
        headerSpace: Int,
        oneMark: Int,
        zeroMark: Int,
        space: Int,
    ): IntArray {
        val out = ArrayList<Int>(data.size * 2 + 3)
        out.add(headerMark)
        out.add(headerSpace)
        for (b in data) {
            out.add(if (b == 1) oneMark else zeroMark)
            out.add(space)
        }
        out.removeAt(out.size - 1)
        return out.toIntArray()
    }

    private fun manchester(levels: IntArray, durations: IntArray): IntArray {
        if (levels.isEmpty()) return IntArray(0)
        val mergedLevels = ArrayList<Int>()
        val mergedDurations = ArrayList<Int>()
        for (i in levels.indices) {
            if (mergedLevels.isNotEmpty() && mergedLevels.last() == levels[i]) {
                val lastIndex = mergedDurations.size - 1
                mergedDurations[lastIndex] = mergedDurations[lastIndex] + durations[i]
            } else {
                mergedLevels.add(levels[i])
                mergedDurations.add(durations[i])
            }
        }
        while (mergedLevels.isNotEmpty() && mergedLevels.first() == 0) {
            mergedLevels.removeAt(0)
            mergedDurations.removeAt(0)
        }
        if (mergedLevels.isNotEmpty() && mergedLevels.last() == 0) {
            mergedLevels.removeAt(mergedLevels.size - 1)
            mergedDurations.removeAt(mergedDurations.size - 1)
        }
        return mergedDurations.toIntArray()
    }

    private fun appendGap(pattern: IntArray, gapUs: Int): IntArray =
        pattern.copyOf(pattern.size + 1).also { it[pattern.size] = gapUs }

    private fun frameDuration(pattern: IntArray): Int = pattern.sum()

    /* --------------------------- NEC --------------------------- */

    private fun nec(signal: IrSignal, extended: Boolean): EncodedIr {
        val payload: Long
        if (extended) {
            payload = (signal.address and 0xFFFFL) or ((signal.command and 0xFFFFL) shl 16)
        } else {
            val address = (signal.address and 0xFFL).toInt()
            val command = (signal.command and 0xFFL).toInt()
            payload = address.toLong() or
                ((address.inv() and 0xFF).toLong() shl 8) or
                (command.toLong() shl 16) or
                ((command.inv() and 0xFF).toLong() shl 24)
        }
        val frame = pdwm(
            data = bits(payload, 32),
            headerMark = NEC_HDR_MARK,
            headerSpace = NEC_HDR_SPACE,
            mark = NEC_BIT_MARK,
            oneSpace = NEC_ONE_SPACE,
            zeroSpace = NEC_ZERO_SPACE,
            stopMark = NEC_BIT_MARK,
        )
        // إطار التكرار القصير: ٩ms + ٢.٢٥ms + ٥٦٠µs ثم صمت
        val repeat = intArrayOf(NEC_HDR_MARK, NEC_REPEAT_SPACE, NEC_BIT_MARK)
        return EncodedIr(38000, frame, appendGap(repeat, 96000))
    }

    /* ------------------------ Samsung32 ------------------------ */

    private fun samsung(signal: IrSignal): EncodedIr {
        val address = (signal.address and 0xFFL).toInt()
        val command = (signal.command and 0xFFL).toInt()
        val payload = address.toLong() or
            (address.toLong() shl 8) or
            (command.toLong() shl 16) or
            ((command.inv() and 0xFF).toLong() shl 24)
        val frame = pdwm(
            data = bits(payload, 32),
            headerMark = SAM_HDR_MARK,
            headerSpace = SAM_HDR_SPACE,
            mark = SAM_BIT_MARK,
            oneSpace = SAM_ONE_SPACE,
            zeroSpace = SAM_ZERO_SPACE,
            stopMark = SAM_BIT_MARK,
        )
        return EncodedIr(38000, frame, appendGap(frame, 46000))
    }

    /* ------------------------- Sony SIRC ------------------------- */

    private fun sirc(signal: IrSignal, addressBits: Int): EncodedIr {
        val command = (signal.command and 0x7FL).toInt()
        val addressMask = (1L shl addressBits) - 1L
        val address = (signal.address and addressMask).toInt()
        val data = bits(command.toLong(), 7) + bits(address.toLong(), addressBits)
        val frame = pwmMark(
            data = data,
            headerMark = SIRC_HDR_MARK,
            headerSpace = SIRC_HDR_SPACE,
            oneMark = SIRC_ONE_MARK,
            zeroMark = SIRC_ZERO_MARK,
            space = SIRC_SPACE,
        )
        val gap = max(8000, 45000 - frameDuration(frame))
        // أجهزة سوني تحتاج تكرار الأمر ٣ مرات على الأقل
        val tap = frame + intArrayOf(gap) + frame + intArrayOf(gap) + frame
        return EncodedIr(40000, tap, appendGap(frame, gap))
    }

    /* ------------------------ RC5 / RC5X ------------------------ */

    private fun rc5(signal: IrSignal, extended: Boolean): EncodedIr {
        val half = 888
        val address = (signal.address and 0x1FL).toInt()
        val command = (signal.command and if (extended) 0x7FL else 0x3FL).toInt()

        val logical = ArrayList<Int>(14)
        logical.add(1) // بت البدء الأول
        if (extended) {
            logical.add(((command shr 6) and 1) xor 1) // بت البدء الثاني = معكوس البت السابع
        } else {
            logical.add(1)
        }
        logical.add(nextToggle()) // بت التبديل
        for (i in 4 downTo 0) logical.add((address shr i) and 1)
        for (i in 5 downTo 0) logical.add((command shr i) and 1)

        val levels = IntArray(logical.size * 2)
        val durations = IntArray(logical.size * 2) { half }
        logical.forEachIndexed { index, bit ->
            // RC5: المنطقي ١ = صمت ثم شرارة، والمنطقي ٠ = شرارة ثم صمت
            levels[index * 2] = if (bit == 1) 0 else 1
            levels[index * 2 + 1] = if (bit == 1) 1 else 0
        }
        val frame = manchester(levels, durations)
        return EncodedIr(36000, frame, appendGap(frame, 114000))
    }

    /* --------------------------- RC6 --------------------------- */

    private fun rc6(signal: IrSignal): EncodedIr {
        val half = 444
        val address = (signal.address and 0xFFL).toInt()
        val command = (signal.command and 0xFFL).toInt()

        val sequence = ArrayList<Int>(21)
        sequence.add(1) // بت البدء
        sequence.add(0); sequence.add(0); sequence.add(0) // النمط
        sequence.add(nextToggle()) // بت التبديل
        for (i in 7 downTo 0) sequence.add((address shr i) and 1)
        for (i in 7 downTo 0) sequence.add((command shr i) and 1)

        val levels = ArrayList<Int>(42)
        val durations = ArrayList<Int>(42)
        sequence.forEachIndexed { index, bit ->
            val d = if (index == 4) half * 2 else half // بت التبديل مضاعف العرض
            // RC6: المنطقي ١ = شرارة ثم صمت
            levels.add(if (bit == 1) 1 else 0)
            durations.add(d)
            levels.add(if (bit == 1) 0 else 1)
            durations.add(d)
        }
        val frame = manchester(levels.toIntArray(), durations.toIntArray())
        return EncodedIr(36000, frame, appendGap(frame, 114000))
    }

    /* --------------------------- RCA --------------------------- */

    private fun rca(signal: IrSignal): EncodedIr {
        val address = (signal.address and 0x0FL).toInt()
        val command = (signal.command and 0xFFL).toInt()
        val payload = address.toLong() or
            (command.toLong() shl 4) or
            ((address.inv() and 0x0F).toLong() shl 12) or
            ((command.inv() and 0xFF).toLong() shl 16)
        val frame = pdwm(
            data = bits(payload, 24),
            headerMark = RCA_HDR_MARK,
            headerSpace = RCA_HDR_SPACE,
            mark = RCA_BIT_MARK,
            oneSpace = RCA_ONE_SPACE,
            zeroSpace = RCA_ZERO_SPACE,
            stopMark = RCA_BIT_MARK,
        )
        return EncodedIr(38000, frame, appendGap(frame, 8000))
    }

    /* -------------------- Panasonic (Kaseikyo) -------------------- */

    private fun kaseikyo(signal: IrSignal): EncodedIr {
        val address = signal.address
        val command = (signal.command and 0xFFFFL).toInt()
        val id = ((address shr 24) and 0x3L).toInt()
        val vendor = ((address shr 8) and 0xFFFFL).toInt()
        val genre1 = ((address shr 4) and 0xFL).toInt()
        val genre2 = (address and 0xFL).toInt()

        val data = IntArray(6)
        data[0] = vendor and 0xFF
        data[1] = (vendor shr 8) and 0xFF
        var parity = data[0] xor data[1]
        parity = (parity and 0xF) xor (parity shr 4)
        data[2] = (parity and 0xF) or (genre1 shl 4)
        data[3] = (genre2 and 0xF) or ((command and 0xF) shl 4)
        data[4] = (id shl 6) or ((command shr 4) and 0xFF)
        data[5] = data[2] xor data[3] xor data[4]

        val dataBits = ArrayList<Int>(48)
        for (byte in data) for (i in 0..7) dataBits.add((byte shr i) and 1)

        val out = ArrayList<Int>(100)
        out.add(8 * KASEIKYO_UNIT)
        out.add(4 * KASEIKYO_UNIT)
        for (b in dataBits) {
            out.add(KASEIKYO_UNIT)
            out.add(if (b == 1) KASEIKYO_UNIT * 3 else KASEIKYO_UNIT)
        }
        out.removeAt(out.size - 1)
        out.add(KASEIKYO_UNIT)
        val frame = out.toIntArray()
        // باناسونيك يكرر الإطار نفسه بعد صمت قصير
        val tap = frame + intArrayOf(40000) + frame
        return EncodedIr(38000, tap, appendGap(frame, 74000))
    }

    /* --------------------- بت التبديل --------------------- */

    private var toggleState = false

    @Synchronized
    private fun nextToggle(): Int {
        toggleState = !toggleState
        return if (toggleState) 1 else 0
    }
}
