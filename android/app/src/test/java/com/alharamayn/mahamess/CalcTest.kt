package com.alharamayn.mahamess

import com.alharamayn.mahamess.data.Calc
import com.alharamayn.mahamess.data.DraftLine
import com.alharamayn.mahamess.util.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class CalcTest {

    @Test
    fun kgToGramsIsExact() {
        // 0.25 كغ = 250 غرام
        assertEquals(250L, Calc.baseQty("kg", 250))
        assertEquals(1000L, Calc.baseQty("kg", 1000))
    }

    @Test
    fun unitPriceConversion() {
        // سعر الكيلو 250,000 ل.س → الغرام 250 ل.س
        assertEquals(250L, Calc.priceForUnit(250_000, "g", 250, true))
        assertEquals(250_000L, Calc.priceForUnit(250_000, "kg", 250, true))
        // الأوقية 250 غرام → ربع سعر الكيلو
        assertEquals(62_500L, Calc.priceForUnit(250_000, "oz", 250, true))
    }

    @Test
    fun lineTotalUsesIntegers() {
        val line = DraftLine(itemId = 1, name = "فستق", unit = "kg", qtyMilli = 250, unitPrice = 250_000, baseQty = 250)
        assertEquals(62_500L, Calc.lineTotal(line))
    }

    @Test
    fun invoiceTotalsWithPercentDiscount() {
        val lines = listOf(
            DraftLine(1, "أ", "kg", 1000, 100_000, baseQty = 1000, costPerBase = 80),
            DraftLine(2, "ب", "kg", 500, 200_000, baseQty = 500, costPerBase = 80),
        )
        // 100,000 + 100,000 = 200,000 ، خصم 10% = 20,000
        val totals = Calc.totals(lines, "percent", 10)
        assertEquals(200_000L, totals.subtotal)
        assertEquals(20_000L, totals.discount)
        assertEquals(180_000L, totals.total)
        assertEquals(120_000L, totals.cost)
        assertEquals(60_000L, totals.profit)
    }

    @Test
    fun currencyConversionRoundTrips() {
        val rate = 1_250_000L // 12,500 ل.س للدولار
        val usd = Money.convert(1_250_000, Money.SYP, Money.USD, rate)
        assertEquals(100_00L, usd) // 100 دولار = 10000 سنت
        val syp = Money.convert(100_00, Money.USD, Money.SYP, rate)
        assertEquals(1_250_000L, syp)
    }

    @Test
    fun moneyParsingIsExact() {
        assertEquals(125_00L, Money.parse("125.00", Money.USD))
        assertEquals(125_000L, Money.parse("125,000", Money.SYP))
        assertEquals(0L, Money.parse("", Money.SYP))
    }

    @Test
    fun discountNeverExceedsSubtotal() {
        val lines = listOf(DraftLine(1, "أ", "piece", 1000, 10_000, baseQty = 1))
        val totals = Calc.totals(lines, "fixed", 50_000)
        assertEquals(10_000L, totals.subtotal)
        assertEquals(10_000L, totals.discount)
        assertEquals(0L, totals.total)
    }
}
