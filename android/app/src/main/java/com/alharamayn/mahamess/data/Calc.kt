package com.alharamayn.mahamess.data

/** بند مسودة فاتورة (قبل الحفظ) */
data class DraftLine(
    val itemId: Long,
    val name: String,
    val unit: String,          // kg | g | oz | piece
    val qtyMilli: Long,
    val unitPrice: Long,
    val discount: Long = 0,
    val costPerBase: Long = 0,
    val baseQty: Long = 0,
    val isBasket: Boolean = false,
)

data class Totals(
    val subtotal: Long,
    val discount: Long,
    val total: Long,
    val cost: Long,
    val profit: Long,
)

object Calc {

    fun unitFactor(unit: String, ozGrams: Int = 250): Long = when (unit) {
        "kg" -> 1000L
        "g" -> 1L
        "oz" -> ozGrams.toLong()
        else -> 1L
    }

    fun baseQty(unit: String, qtyMilli: Long, ozGrams: Int = 250): Long =
        Math.round(qtyMilli * unitFactor(unit, ozGrams) / 1000.0)

    fun lineGross(line: DraftLine): Long = Math.round(line.qtyMilli * line.unitPrice / 1000.0)

    fun lineTotal(line: DraftLine): Long = (lineGross(line) - line.discount).coerceAtLeast(0)

    fun lineCost(line: DraftLine): Long = Math.round(line.baseQty * line.costPerBase.toDouble())

    /** discountFixed بالوحدة الصغرى للعملة، والنسبة في discountPercent */
    fun totals(
        lines: List<DraftLine>,
        discountType: String,
        discountValue: Long,
    ): Totals {
        val subtotal = lines.sumOf { lineTotal(it) }
        val discount = when (discountType) {
            "percent" -> Math.round(subtotal * (discountValue.coerceIn(0, 100)) / 100.0)
            "fixed" -> discountValue
            else -> 0L
        }.coerceIn(0, subtotal)
        val total = subtotal - discount
        val cost = lines.sumOf { lineCost(it) }
        return Totals(subtotal, discount, total, cost, total - cost)
    }

    /** سعر الوحدة المختارة انطلاقاً من سعر الكيلو/القطعة */
    fun priceForUnit(pricePerBase: Long, unit: String, ozGrams: Int = 250, isWeight: Boolean): Long {
        if (!isWeight) return pricePerBase
        return Math.round(pricePerBase * unitFactor(unit, ozGrams) / 1000.0)
    }
}

/** أنواع الوحدات */
object Units {
    val weights = listOf("kg", "g", "oz")
    fun label(unit: String, ozGrams: Int = 250): String = when (unit) {
        "kg" -> "كيلو"
        "g" -> "غرام"
        "oz" -> "أوقية ($ozGrams غ)"
        "piece" -> "قطعة"
        else -> unit
    }

    fun short(unit: String): String = when (unit) {
        "kg" -> "كغ"
        "g" -> "غ"
        "oz" -> "أوقية"
        else -> "قطعة"
    }
}
