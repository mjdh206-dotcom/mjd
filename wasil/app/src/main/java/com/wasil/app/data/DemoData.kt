package com.wasil.app.data

/** بيانات وضع المعاينة: رسائل تجريبية لاستعراض الواجهة بدون خادم. */
object DemoData {

    const val DEMO_NUMBER = "+1 (415) 555-0134"

    val messages: List<SmsMessage>
        get() {
            val now = System.currentTimeMillis()
            return listOf(
                SmsMessage(
                    id = "demo-1",
                    from = "WhatsApp",
                    body = "رمز التفعيل الخاص بك في واتساب هو: 348-291. لا تشاركه مع أحد.",
                    ts = now - 3 * 60_000L,
                ),
                SmsMessage(
                    id = "demo-2",
                    from = "Viber",
                    body = "Viber verification code: 705412. This code expires in 60 seconds.",
                    ts = now - 42 * 60_000L,
                ),
                SmsMessage(
                    id = "demo-3",
                    from = "+12025550188",
                    body = "Your security code is 9930. Do not reply to this message.",
                    ts = now - 3 * 3_600_000L,
                ),
                SmsMessage(
                    id = "demo-4",
                    from = "Google",
                    body = "G-482917 هو رمز التحقق من Google.",
                    ts = now - 26 * 3_600_000L,
                ),
            )
        }
}
