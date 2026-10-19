package com.alharamayn.mahamess.util

import com.alharamayn.mahamess.data.InvoiceWithLines
import com.alharamayn.mahamess.data.PaymentEntity
import com.alharamayn.mahamess.data.ExpenseEntity
import com.alharamayn.mahamess.data.WithdrawalEntity
import com.alharamayn.mahamess.data.WorkerTxEntity

data class PartyBalance(val syp: Long = 0, val usd: Long = 0)

data class CashFlow(
    val cashIn: Long = 0,
    val payIn: Long = 0,
    val purchases: Long = 0,
    val expenses: Long = 0,
    val withdrawals: Long = 0,
    val workerTx: Long = 0,
) {
    val out: Long get() = purchases + expenses + withdrawals + workerTx
    val net: Long get() = cashIn + payIn - out
}

data class CurrencyTotals(
    val sales: Long = 0,
    val count: Int = 0,
    val paid: Long = 0,
    val remaining: Long = 0,
    val profit: Long = 0,
    val purchases: Long = 0,
    val expenses: Long = 0,
    val withdrawals: Long = 0,
    val workerTx: Long = 0,
    val payments: Long = 0,
)

data class PeriodReport(
    val syp: CurrencyTotals = CurrencyTotals(),
    val usd: CurrencyTotals = CurrencyTotals(),
)

object Analytics {

    fun sales(invoices: List<InvoiceWithLines>) =
        invoices.filter { it.invoice.type == "sale" && it.invoice.status != "cancelled" }

    fun purchases(invoices: List<InvoiceWithLines>) =
        invoices.filter { it.invoice.type == "purchase" && it.invoice.status != "cancelled" }

    fun partyBalance(
        partyId: Long,
        type: String,
        invoices: List<InvoiceWithLines>,
        payments: List<PaymentEntity>,
    ): PartyBalance {
        val invType = if (type == "customer") "sale" else "purchase"
        var syp = 0L
        var usd = 0L
        invoices.filter { it.invoice.partyId == partyId && it.invoice.type == invType && it.invoice.status != "cancelled" }
            .forEach {
                if (it.invoice.currency == Money.SYP) syp += it.invoice.remaining else usd += it.invoice.remaining
            }
        payments.filter { it.partyId == partyId && it.partyType == type }.forEach {
            if (it.currency == Money.SYP) syp -= it.amount else usd -= it.amount
        }
        return PartyBalance(syp, usd)
    }

    fun customerBalances(invoices: List<InvoiceWithLines>, payments: List<PaymentEntity>): PartyBalance {
        var syp = 0L
        var usd = 0L
        sales(invoices).forEach { if (it.invoice.currency == Money.SYP) syp += it.invoice.remaining else usd += it.invoice.remaining }
        payments.filter { it.partyType == "customer" }.forEach { if (it.currency == Money.SYP) syp -= it.amount else usd -= it.amount }
        return PartyBalance(syp, usd)
    }

    fun supplierBalances(invoices: List<InvoiceWithLines>, payments: List<PaymentEntity>): PartyBalance {
        var syp = 0L
        var usd = 0L
        purchases(invoices).forEach { if (it.invoice.currency == Money.SYP) syp += it.invoice.remaining else usd += it.invoice.remaining }
        payments.filter { it.partyType == "supplier" }.forEach { if (it.currency == Money.SYP) syp -= it.amount else usd -= it.amount }
        return PartyBalance(syp, usd)
    }

    fun cashFlow(
        currency: String,
        from: String,
        to: String,
        invoices: List<InvoiceWithLines>,
        payments: List<PaymentEntity>,
        expenses: List<ExpenseEntity>,
        withdrawals: List<WithdrawalEntity>,
        workerTx: List<WorkerTxEntity>,
    ): CashFlow {
        fun inRange(d: String) = d >= from && d <= to
        var cashIn = 0L
        sales(invoices).filter { inRange(it.invoice.date) }.forEach { inv ->
            if (inv.invoice.paidCurrency == currency) cashIn += inv.invoice.paid
            else if (inv.invoice.paidForeign > 0 && inv.invoice.paidCurrency == currency) cashIn += inv.invoice.paidForeign
        }
        val payIn = payments.filter { it.partyType == "customer" && it.currency == currency && inRange(it.date) }
            .sumOf { it.amount }
        val purch = purchases(invoices).filter { it.invoice.currency == currency && inRange(it.invoice.date) }
            .sumOf { it.invoice.paid }
        val exp = expenses.filter { it.currency == currency && inRange(it.date) }.sumOf { it.amount }
        val wd = withdrawals.filter { it.currency == currency && inRange(it.date) }.sumOf { it.amount }
        val wtx = workerTx.filter { it.currency == currency && inRange(it.date) }.sumOf { it.amount }
        return CashFlow(cashIn, payIn, purch, exp, wd, wtx)
    }

    fun report(
        from: String,
        to: String,
        invoices: List<InvoiceWithLines>,
        payments: List<PaymentEntity>,
        expenses: List<ExpenseEntity>,
        withdrawals: List<WithdrawalEntity>,
        workerTx: List<WorkerTxEntity>,
    ): PeriodReport {
        fun inRange(d: String) = d >= from && d <= to
        fun forCurrency(c: String): CurrencyTotals {
            val s = sales(invoices).filter { it.invoice.currency == c && inRange(it.invoice.date) }
            val p = purchases(invoices).filter { it.invoice.currency == c && inRange(it.invoice.date) }
            return CurrencyTotals(
                sales = s.sumOf { it.invoice.total },
                count = s.size,
                paid = s.sumOf { it.invoice.paid },
                remaining = s.sumOf { it.invoice.remaining },
                profit = s.sumOf { it.invoice.profit },
                purchases = p.sumOf { it.invoice.total },
                expenses = expenses.filter { it.currency == c && inRange(it.date) }.sumOf { it.amount },
                withdrawals = withdrawals.filter { it.currency == c && inRange(it.date) }.sumOf { it.amount },
                workerTx = workerTx.filter { it.currency == c && inRange(it.date) }.sumOf { it.amount },
                payments = payments.filter { it.partyType == "customer" && it.currency == c && inRange(it.date) }.sumOf { it.amount },
            )
        }
        return PeriodReport(forCurrency(Money.SYP), forCurrency(Money.USD))
    }

    data class TopItem(
        val itemId: Long,
        val name: String,
        val qtyBase: Long,
        val revenue: Long,
        val profit: Long,
        val currency: String,
    )

    fun topItems(
        from: String,
        to: String,
        invoices: List<InvoiceWithLines>,
    ): List<TopItem> {
        val map = LinkedHashMap<Long, TopItem>()
        sales(invoices).filter { it.invoice.date >= from && it.invoice.date <= to }.forEach { inv ->
            inv.lines.forEach { line ->
                val rev = (Math.round(line.qtyMilli * line.unitPrice / 1000.0) - line.discount).coerceAtLeast(0)
                val cost = Math.round(line.baseQty * line.costPerBase.toDouble())
                val prev = map[line.itemId]
                map[line.itemId] = if (prev == null) {
                    TopItem(line.itemId, line.name, line.baseQty, rev, rev - cost, inv.invoice.currency)
                } else {
                    prev.copy(
                        qtyBase = prev.qtyBase + line.baseQty,
                        revenue = prev.revenue + rev,
                        profit = prev.profit + rev - cost,
                    )
                }
            }
        }
        return map.values.sortedByDescending { it.revenue }
    }

    /** إجمالي العملتين بعملة عرض واحدة، باستخدام سعر الصرف الحالي */
    fun combined(syp: Long, usd: Long, display: String, rate: Long): Long = when (display) {
        Money.SYP -> syp + Money.convert(usd, Money.USD, Money.SYP, rate)
        Money.USD -> Money.convert(syp, Money.SYP, Money.USD, rate) + usd
        else -> syp
    }
}
