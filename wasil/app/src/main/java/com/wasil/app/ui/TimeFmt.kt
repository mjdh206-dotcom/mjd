package com.wasil.app.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** تنسيق عربي مبسّط للوقت: "قبل ٥ دقائق" + تاريخ مختصر عند الحاجة. */
object TimeFmt {

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

    fun relative(ts: Long, now: Long = System.currentTimeMillis()): String {
        if (ts <= 0L) return "—"
        val diff = now - ts
        return when {
            diff < 60_000L -> "الآن"
            diff < 3_600_000L -> "قبل ${diff / 60_000L} دقيقة"
            diff < 86_400_000L -> "قبل ${diff / 3_600_000L} ساعة"
            diff < 2L * 86_400_000L -> "أمس"
            diff < 7L * 86_400_000L -> "قبل ${diff / 86_400_000L} يوم"
            else -> dateFormat.format(Date(ts))
        }
    }

    fun clock(ts: Long): String =
        if (ts <= 0L) "—" else timeFormat.format(Date(ts))
}
