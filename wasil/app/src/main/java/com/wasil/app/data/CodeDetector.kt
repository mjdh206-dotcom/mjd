package com.wasil.app.data

/**
 * يستخرج كود التفعيل من نون الرسالة: يفضّل الأرقام القريبة من كلمات
 * (رمز، كود، تحقق، code، pin، otp، verification…)، وإلا فأول مجموعة أرقام.
 */
object CodeDetector {

    private val KEYWORDS = listOf(
        "رمز", "كود", "الكود", "تفعيل", "تحقق", "تأكيد", "الرقم السري",
        "code", "pin", "otp", "verif", "confirm", "whatsapp", "viber", "security",
    )

    private val DIGITS = Regex("\\d{4,8}")

    fun findCode(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val lower = body.lowercase()
        val groups = DIGITS.findAll(body).toList()
        if (groups.isEmpty()) return null

        // أفضل مجموعة قريبة من إحدى الكلمات الدالة
        val keywordIndex = KEYWORDS
            .map { lower.indexOf(it) }
            .filter { it >= 0 }
            .minOrNull() ?: -1

        if (keywordIndex >= 0) {
            val near = groups.firstOrNull { mr ->
                val start = mr.range.first
                start >= keywordIndex && start - keywordIndex <= 40
            }
            if (near != null) return near.value
        }
        return groups.first().value
    }
}
