package com.alharamayn.mahamess.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * كل المبالغ أعداد صحيحة (Long) بالوحدة الصغرى للعملة:
 * الليرة السورية = 1 ، الدولار = سنت (×100)
 * سعر الصرف يُخزَّن ×100 أيضاً.
 */
object Money {

    const val SYP = "SYP"
    const val USD = "USD"

    fun decimals(currency: String): Int = if (currency == USD) 2 else 0

    fun factor(currency: String): Long = if (currency == USD) 100L else 1L

    fun symbol(currency: String): String = if (currency == USD) "$" else "ل.س"

    fun name(currency: String): String =
        if (currency == USD) "دولار أمريكي" else "ليرة سورية"

    fun format(minor: Long, currency: String, withSymbol: Boolean = true): String {
        val dec = decimals(currency)
        val value = minor.toDouble() / factor(currency)
        val symbols = DecimalFormatSymbols(Locale.US)
        val pattern = if (dec == 0) "#,##0" else "#,##0.00"
        val s = DecimalFormat(pattern, symbols).format(value)
        return if (withSymbol) "$s ${symbol(currency)}" else s
    }

    fun formatBare(minor: Long, currency: String): String = format(minor, currency, false)

    fun parse(text: String, currency: String): Long {
        val cleaned = text.replace("[^0-9.]".toRegex(), "")
        if (cleaned.isEmpty() || cleaned == ".") return 0
        val v = cleaned.toDoubleOrNull() ?: 0.0
        return Math.round(v * factor(currency))
    }

    fun parsePlain(text: String): Double {
        val cleaned = text.replace("[^0-9.]".toRegex(), "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    /** تحويل بين عملتين بسعر صرف مخزّن ×100 */
    fun convert(minor: Long, from: String, to: String, rate: Long): Long {
        if (from == to) return minor
        val r = rate / 100.0
        if (r <= 0.0) return 0
        return when {
            from == SYP && to == USD -> Math.round((minor / r) * 100.0)
            from == USD && to == SYP -> Math.round((minor / 100.0) * r)
            else -> minor
        }
    }

    fun rateValue(rate: Long): Double = rate / 100.0

    fun formatRate(rate: Long): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        return DecimalFormat("#,##0.##", symbols).format(rate / 100.0)
    }

    fun rateFromInput(text: String): Long = Math.round(parsePlain(text) * 100)
}

object Dates {
    private val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val display = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    private val stamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)

    fun today(): String = iso.format(Date())

    fun nowStamp(): String = stamp.format(Date())

    fun format(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "—"
        return try {
            display.format(iso.format(iso.parse(isoDate) ?: Date()))
        } catch (e: Exception) {
            isoDate
        }
    }

    fun formatMillis(millis: Long): String = stamp.format(Date(millis))

    fun daysUntil(isoDate: String?): Int? {
        if (isoDate.isNullOrBlank()) return null
        return try {
            val target = iso.parse(isoDate) ?: return null
            val today = iso.parse(today()) ?: Date()
            ((target.time - today.time) / 86_400_000L).toInt()
        } catch (e: Exception) {
            null
        }
    }

    fun startOfMonth(): String {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, 1)
        return iso.format(c.time)
    }

    fun addDays(days: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_MONTH, days)
        return iso.format(c.time)
    }
}
