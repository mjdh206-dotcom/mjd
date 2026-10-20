package com.shuaa.remote.data

import android.content.Context
import org.json.JSONObject

/**
 * مكتبة الأكواد المدمجة: ١٠٤ ماركة و١٩٢ مجموعة أكواد تعمل دون إنترنت.
 * المصدر: Flipper-IRDB (رخصة CC BY-SA 4.0) - انظر شاشة "عن التطبيق".
 */
object CodeLibrary {

    @Volatile
    private var cache: List<Brand>? = null

    @Volatile
    private var meta: Pair<String, Int> = "" to 0

    fun brands(context: Context): List<Brand> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val parsed = mutableListOf<Brand>()
            try {
                val json = context.assets.open("codes.json").bufferedReader().use { it.readText() }
                val root = JSONObject(json)
                meta = (root.optString("source", "") to root.optInt("version", 1))
                val arr = root.getJSONArray("brands")
                for (i in 0 until arr.length()) {
                    val b = arr.getJSONObject(i)
                    val setsArr = b.getJSONArray("sets")
                    val sets = mutableListOf<CodeSet>()
                    for (j in 0 until setsArr.length()) {
                        val s = setsArr.getJSONObject(j)
                        val codesObj = s.getJSONObject("codes")
                        val codes = mutableMapOf<String, IrSignal>()
                        val keys = codesObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            IrSignal.parse(codesObj.optString(k))?.let { codes[k] = it }
                        }
                        if (codes.isNotEmpty()) {
                            sets.add(CodeSet(s.optString("name", "ريموت ${j + 1}"), codes))
                        }
                    }
                    if (sets.isNotEmpty()) {
                        parsed.add(
                            Brand(
                                id = b.optString("id", b.optString("en")),
                                nameAr = b.optString("ar", b.optString("en")),
                                nameEn = b.optString("en", ""),
                                sets = sets,
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // في حال فشل قراءة الملف نُبقي قائمة فارغة بدل الانهيار
            }
            parsed.sortBy { it.nameAr }
            cache = parsed
            return parsed
        }
    }

    fun brand(context: Context, id: String): Brand? = brands(context).firstOrNull { it.id == id }

    fun findBrandByEnglishName(context: Context, name: String): Brand? =
        brands(context).firstOrNull { it.nameEn.equals(name, ignoreCase = true) }

    fun sourceInfo(): String = meta.first

    fun totalSets(context: Context): Int = brands(context).sumOf { it.sets.size }

    /** الماركات الأكثر انتشاراً في المنطقة العربية */
    val popular = listOf("Samsung", "LG", "Sony", "Toshiba", "Sharp", "Philips", "Panasonic", "TCL", "Hisense", "Daewoo", "Hitachi", "JVC")

    fun popularBrands(context: Context): List<Brand> {
        val all = brands(context)
        val ordered = popular.mapNotNull { en -> all.firstOrNull { it.nameEn.equals(en, ignoreCase = true) } }
        val rest = all.filter { b -> ordered.none { it.id == b.id } }
        return ordered + rest
    }
}
