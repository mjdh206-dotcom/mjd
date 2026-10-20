package com.shuaa.remote.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * إشارة ريموت واحدة: البروتوكول + العنوان + الأمر.
 * تُخزَّن في مكتبة الأكواد بصيغة نصية مختصرة: "NEC:40000000:12000000"
 */
data class IrSignal(
    val protocol: String,
    val address: Long,
    val command: Long,
) {
    fun serialize(): String = "$protocol:${address.toString(16).uppercase()}:${command.toString(16).uppercase()}"

    companion object {
        fun parse(raw: String): IrSignal? {
            val parts = raw.split(":")
            if (parts.size != 3) return null
            return try {
                IrSignal(parts[0], parts[1].toLong(16), parts[2].toLong(16))
            } catch (_: Exception) {
                null
            }
        }
    }
}

/** مجموعة أكواد لريموت معيّن (ماركة + موديل) */
data class CodeSet(
    val name: String,
    val codes: Map<String, IrSignal>,
)

data class Brand(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val sets: List<CodeSet>,
) {
    val buttonCount: Int get() = sets.maxOfOrNull { it.codes.size } ?: 0
    val supportedIcons: String get() = if (buttonCount >= 20) "كامل" else "أساسي"
}

/** جهاز محفوظ عند المستخدم */
data class SavedDevice(
    val id: String,
    val name: String,
    val brandAr: String,
    val brandEn: String,
    val setName: String,
    val codes: Map<String, IrSignal>,
    val createdAt: Long,
    val isPrimary: Boolean = false,
) {
    fun toJson(): JSONObject {
        val codesJson = JSONObject()
        codes.forEach { (k, v) -> codesJson.put(k, v.serialize()) }
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("brandAr", brandAr)
            put("brandEn", brandEn)
            put("setName", setName)
            put("createdAt", createdAt)
            put("isPrimary", isPrimary)
            put("codes", codesJson)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): SavedDevice? = try {
            val codesObj = obj.optJSONObject("codes") ?: JSONObject()
            val codes = mutableMapOf<String, IrSignal>()
            val keys = codesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                IrSignal.parse(codesObj.optString(key))?.let { codes[key] = it }
            }
            SavedDevice(
                id = obj.getString("id"),
                name = obj.optString("name", "تلفزيوني"),
                brandAr = obj.optString("brandAr", ""),
                brandEn = obj.optString("brandEn", ""),
                setName = obj.optString("setName", ""),
                codes = codes,
                createdAt = obj.optLong("createdAt", 0L),
                isPrimary = obj.optBoolean("isPrimary", false),
            )
        } catch (_: Exception) {
            null
        }
    }
}

/** أزرار الريموت المعروفة وترتيبها على الشاشة */
object Buttons {
    const val POWER = "power"
    const val SOURCE = "source"
    const val MUTE = "mute"
    const val VOL_UP = "vol_up"
    const val VOL_DN = "vol_dn"
    const val CH_UP = "ch_up"
    const val CH_DN = "ch_dn"
    const val UP = "up"
    const val DOWN = "down"
    const val LEFT = "left"
    const val RIGHT = "right"
    const val OK = "ok"
    const val BACK = "back"
    const val EXIT = "exit"
    const val MENU = "menu"
    const val HOME = "home"
    const val INFO = "info"
    const val GUIDE = "guide"
    const val LIST = "list"
    const val PLAY = "play"
    const val PAUSE = "pause"
    const val STOP = "stop"
    const val REW = "rew"
    const val FFW = "ffw"
    const val REC = "rec"
    const val PREV = "prev"
    const val NEXT = "next"
    const val RED = "red"
    const val GREEN = "green"
    const val YELLOW = "yellow"
    const val BLUE = "blue"
    const val SUBTITLE = "subtitle"
    const val ASPECT = "aspect"
    const val AUDIO = "audio"
    const val SLEEP = "sleep"
    const val FAV = "fav"
    const val PIC = "pic"
    const val DASH = "dash"

    val digits = listOf("key0", "key1", "key2", "key3", "key4", "key5", "key6", "key7", "key8", "key9")

    /** الأزرار التي تدعم التكرار عند الاستمرار بالضغط */
    val repeatable = setOf(VOL_UP, VOL_DN, CH_UP, CH_DN, UP, DOWN, LEFT, RIGHT)

    fun label(key: String): String = when (key) {
        POWER -> "الطاقة"
        SOURCE -> "المصدر"
        MUTE -> "كتم الصوت"
        VOL_UP -> "رفع الصوت"
        VOL_DN -> "خفض الصوت"
        CH_UP -> "القناة التالية"
        CH_DN -> "القناة السابقة"
        UP -> "أعلى"
        DOWN -> "أسفل"
        LEFT -> "يسار"
        RIGHT -> "يمين"
        OK -> "موافق"
        BACK -> "رجوع"
        EXIT -> "خروج"
        MENU -> "القائمة"
        HOME -> "الرئيسية"
        INFO -> "معلومات"
        GUIDE -> "دليل البرامج"
        LIST -> "قائمة القنوات"
        PLAY -> "تشغيل"
        PAUSE -> "إيقاف مؤقت"
        STOP -> "إيقاف"
        REW -> "إرجاع"
        FFW -> "تقديم"
        REC -> "تسجيل"
        PREV -> "السابق"
        NEXT -> "التالي"
        RED -> "أحمر"
        GREEN -> "أخضر"
        YELLOW -> "أصفر"
        BLUE -> "أزرق"
        SUBTITLE -> "الترجمة"
        ASPECT -> "نسبة الصورة"
        AUDIO -> "الصوت"
        SLEEP -> "مؤقت النوم"
        FAV -> "المفضلة"
        PIC -> "الصورة"
        DASH -> "شرطة"
        else -> when {
            key.startsWith("key") -> key.removePrefix("key")
            else -> key
        }
    }
}
