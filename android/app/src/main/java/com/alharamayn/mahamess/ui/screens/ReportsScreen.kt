package com.alharamayn.mahamess.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alharamayn.mahamess.AppViewModel
import com.alharamayn.mahamess.ui.ActionChip
import com.alharamayn.mahamess.ui.AppTextField
import com.alharamayn.mahamess.ui.BrownDark
import com.alharamayn.mahamess.ui.Gold
import com.alharamayn.mahamess.ui.MoneyText
import com.alharamayn.mahamess.ui.SectionCard
import com.alharamayn.mahamess.ui.StatCard
import com.alharamayn.mahamess.ui.Success
import com.alharamayn.mahamess.util.Analytics
import com.alharamayn.mahamess.util.Dates
import com.alharamayn.mahamess.util.Exporter
import com.alharamayn.mahamess.util.Money

@Composable
fun ReportsScreen(vm: AppViewModel) {
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val payments by vm.payments.collectAsStateWithLifecycle()
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val withdrawals by vm.withdrawals.collectAsStateWithLifecycle()
    val workerTx by vm.workerTx.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var period by remember { mutableStateOf("month") }
    var from by remember { mutableStateOf(Dates.startOfMonth()) }
    var to by remember { mutableStateOf(Dates.today()) }
    var display by remember { mutableStateOf(Money.SYP) }
    val isManager = session?.role == "manager"

    val range = when (period) {
        "today" -> Dates.today() to Dates.today()
        "week" -> Dates.addDays(-6) to Dates.today()
        "month" -> Dates.startOfMonth() to Dates.today()
        else -> from to to
    }
    val report = Analytics.report(range.first, range.second, invoices, payments, expenses, withdrawals, workerTx)
    val top = Analytics.topItems(range.first, range.second, invoices)

    fun label(minorSyp: Long, minorUsd: Long): String =
        Money.format(Analytics.combined(minorSyp, minorUsd, display, settings.rate), display)

    val netProfitSyp = report.syp.profit - report.syp.expenses - report.syp.withdrawals - report.syp.workerTx
    val netProfitUsd = report.usd.profit - report.usd.expenses - report.usd.withdrawals - report.usd.workerTx

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("اليوم", { period = "today" }, filled = period == "today")
                    ActionChip("7 أيام", { period = "week" }, filled = period == "week")
                    ActionChip("هذا الشهر", { period = "month" }, filled = period == "month")
                    ActionChip("مخصص", { period = "custom" }, filled = period == "custom")
                }
                if (period == "custom") {
                    Spacer(Modifier.height(6.dp))
                    AppTextField(from, { from = it }, "من تاريخ")
                    Spacer(Modifier.height(6.dp))
                    AppTextField(to, { to = it }, "إلى تاريخ")
                }
                Spacer(Modifier.height(8.dp))
                Text("العرض بعملة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("ليرة", { display = Money.SYP }, filled = display == Money.SYP)
                    ActionChip("دولار", { display = Money.USD }, filled = display == Money.USD)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("🧾", "المبيعات", label(report.syp.sales, report.usd.sales), "${report.syp.count + report.usd.count} فاتورة", Modifier.weight(1f))
                StatCard("🛒", "المشتريات", label(report.syp.purchases, report.usd.purchases), "", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("💸", "المصاريف", label(report.syp.expenses, report.usd.expenses), "", Modifier.weight(1f))
                StatCard("🏦", "المسحوبات", label(report.syp.withdrawals, report.usd.withdrawals), "", Modifier.weight(1f))
            }
        }
        if (isManager) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("📈", "الربح الإجمالي", label(report.syp.profit, report.usd.profit), "بعد التكلفة", Modifier.weight(1f))
                    StatCard("✅", "صافي الربح", label(netProfitSyp, netProfitUsd), "بعد المصاريف والمسحوبات", Modifier.weight(1f))
                }
            }
        }

        item {
            SectionCard(title = "ملخص بالعملتين") {
                HeaderRow()
                SummaryRow("المبيعات", report.syp.sales, report.usd.sales)
                SummaryRow("المشتريات", report.syp.purchases, report.usd.purchases)
                SummaryRow("المصاريف", report.syp.expenses, report.usd.expenses)
                SummaryRow("المسحوبات", report.syp.withdrawals, report.usd.withdrawals)
                SummaryRow("سحوبات العمال", report.syp.workerTx, report.usd.workerTx)
                if (isManager) SummaryRow("الأرباح", report.syp.profit, report.usd.profit, Success)
                SummaryRow("صافي الربح", netProfitSyp, netProfitUsd, com.alharamayn.mahamess.ui.GoldDark)
                Spacer(Modifier.height(6.dp))
                Text(
                    "المجموع المقدّر يستخدم سعر الصرف الحالي (${Money.formatRate(settings.rate)} ل.س) وقد يختلف عن أسعار الفواتير المسجلة.",
                    fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(title = "الأصناف الأكثر مبيعاً", badge = "${top.size}") {
                if (top.isEmpty()) Text("لا توجد مبيعات في الفترة", fontSize = 12.sp)
                top.take(15).forEach { t ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(t.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (t.qtyBase >= 1000) "${t.qtyBase / 1000.0} كغ" else "${t.qtyBase} قطعة",
                                fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            MoneyText(t.revenue, t.currency, 11, true)
                            if (isManager) MoneyText(t.profit, t.currency, 9, false, Success)
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "تصدير التقرير") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("📊 Excel (CSV)", {
                        val rows = mutableListOf<List<String>>()
                        rows.add(listOf("محامص الحرمين — تقرير", "", ""))
                        rows.add(listOf("من", range.first, "إلى", range.second))
                        rows.add(listOf("سعر الصرف", Money.formatRate(settings.rate), ""))
                        rows.add(listOf("البند", "ليرة سورية", "دولار أمريكي"))
                        rows.add(listOf("المبيعات", Money.formatBare(report.syp.sales, Money.SYP), Money.formatBare(report.usd.sales, Money.USD)))
                        rows.add(listOf("عدد الفواتير", "${report.syp.count}", "${report.usd.count}"))
                        rows.add(listOf("المشتريات", Money.formatBare(report.syp.purchases, Money.SYP), Money.formatBare(report.usd.purchases, Money.USD)))
                        rows.add(listOf("المصاريف", Money.formatBare(report.syp.expenses, Money.SYP), Money.formatBare(report.usd.expenses, Money.USD)))
                        rows.add(listOf("المسحوبات", Money.formatBare(report.syp.withdrawals, Money.SYP), Money.formatBare(report.usd.withdrawals, Money.USD)))
                        rows.add(listOf("سحوبات العمال", Money.formatBare(report.syp.workerTx, Money.SYP), Money.formatBare(report.usd.workerTx, Money.USD)))
                        if (isManager) {
                            rows.add(listOf("الربح الإجمالي", Money.formatBare(report.syp.profit, Money.SYP), Money.formatBare(report.usd.profit, Money.USD)))
                            rows.add(listOf("صافي الربح", Money.formatBare(netProfitSyp, Money.SYP), Money.formatBare(netProfitUsd, Money.USD)))
                        }
                        rows.add(listOf("", "", ""))
                        rows.add(listOf("الأصناف الأكثر مبيعاً", "", ""))
                        rows.add(listOf("الصنف", "الكمية", "الإيراد", "الربح"))
                        top.forEach { t -> rows.add(listOf(t.name, "${t.qtyBase}", Money.formatBare(t.revenue, t.currency), Money.formatBare(t.profit, t.currency))) }
                        val file = Exporter.writeText(context, "تقرير-${range.first}_${range.second}.csv", Exporter.csv(rows))
                        Exporter.shareFile(context, file, "text/csv", "تقرير محامص الحرمين")
                    }, filled = true)
                    ActionChip("🖨 PDF", {
                        val rows = mutableListOf<Triple<String, String, String>>()
                        rows.add(Triple("المبيعات", Money.formatBare(report.syp.sales, Money.SYP), Money.formatBare(report.usd.sales, Money.USD)))
                        rows.add(Triple("المشتريات", Money.formatBare(report.syp.purchases, Money.SYP), Money.formatBare(report.usd.purchases, Money.USD)))
                        rows.add(Triple("المصاريف", Money.formatBare(report.syp.expenses, Money.SYP), Money.formatBare(report.usd.expenses, Money.USD)))
                        rows.add(Triple("المسحوبات", Money.formatBare(report.syp.withdrawals, Money.SYP), Money.formatBare(report.usd.withdrawals, Money.USD)))
                        rows.add(Triple("سحوبات العمال", Money.formatBare(report.syp.workerTx, Money.SYP), Money.formatBare(report.usd.workerTx, Money.USD)))
                        if (isManager) {
                            rows.add(Triple("الربح الإجمالي", Money.formatBare(report.syp.profit, Money.SYP), Money.formatBare(report.usd.profit, Money.USD)))
                            rows.add(Triple("صافي الربح", Money.formatBare(netProfitSyp, Money.SYP), Money.formatBare(netProfitUsd, Money.USD)))
                        }
                        val file = Exporter.reportPdf(context, settings, range.first, range.second, rows)
                        Exporter.shareFile(context, file, "application/pdf", "تقرير محامص الحرمين")
                    })
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun HeaderRow() {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("البند", Modifier.weight(1.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("ليرة", Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("دولار", Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SummaryRow(label: String, syp: Long, usd: Long, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, Modifier.weight(1.4f), fontSize = 11.sp, color = color)
        Text(Money.formatBare(syp, Money.SYP), Modifier.weight(1f), fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
        Text(Money.formatBare(usd, Money.USD), Modifier.weight(1f), fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
    }
}
