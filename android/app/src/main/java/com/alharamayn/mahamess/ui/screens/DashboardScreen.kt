package com.alharamayn.mahamess.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alharamayn.mahamess.AppViewModel
import com.alharamayn.mahamess.ui.ActionChip
import com.alharamayn.mahamess.ui.Badge
import com.alharamayn.mahamess.ui.BrownDark
import com.alharamayn.mahamess.ui.Danger
import com.alharamayn.mahamess.ui.Gold
import com.alharamayn.mahamess.ui.GoldLight
import com.alharamayn.mahamess.ui.MoneyText
import com.alharamayn.mahamess.ui.Screen
import com.alharamayn.mahamess.ui.SectionCard
import com.alharamayn.mahamess.ui.StatCard
import com.alharamayn.mahamess.ui.Success
import com.alharamayn.mahamess.util.Analytics
import com.alharamayn.mahamess.util.Dates
import com.alharamayn.mahamess.util.Money

@Composable
fun DashboardScreen(vm: AppViewModel, navigate: (Screen) -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val payments by vm.payments.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val withdrawals by vm.withdrawals.collectAsStateWithLifecycle()
    val workerTx by vm.workerTx.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()

    val today = Dates.today()
    val salesToday = Analytics.sales(invoices).filter { it.invoice.date == today }
    val sSYP = salesToday.filter { it.invoice.currency == Money.SYP }.sumOf { it.invoice.total }
    val sUSD = salesToday.filter { it.invoice.currency == Money.USD }.sumOf { it.invoice.total }
    val profitSYP = salesToday.filter { it.invoice.currency == Money.SYP }.sumOf { it.invoice.profit }
    val profitUSD = salesToday.filter { it.invoice.currency == Money.USD }.sumOf { it.invoice.profit }
    val recv = Analytics.customerBalances(invoices, payments)
    val pay = Analytics.supplierBalances(invoices, payments)
    val cashSYP = Analytics.cashFlow(Money.SYP, today, today, invoices, payments, expenses, withdrawals, workerTx)
    val cashUSD = Analytics.cashFlow(Money.USD, today, today, invoices, payments, expenses, withdrawals, workerTx)

    val low = items.filter { it.minStock > 0 && it.stock <= it.minStock }
    val expiring = items.filter { it.expiry != null }.sortedBy { it.expiry }
    val recent = Analytics.sales(invoices).sortedByDescending { it.invoice.createdAt }.take(5)
    var rateDialog by remember { mutableStateOf(false) }
    val isManager = session?.role == "manager"

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(BrownDark, Color(0xFF6D4C41))))
                    .padding(14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("أهلاً، ${session?.name ?: ""}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(settings.branch + " • " + Dates.format(today), color = Color(0xBBFFFFFF), fontSize = 10.sp)
                    }
                    Column(Modifier.clickable { rateDialog = true }, horizontalAlignment = Alignment.End) {
                        Text("سعر الدولار", color = Color(0xAAFFFFFF), fontSize = 9.sp)
                        Text("${Money.formatRate(settings.rate)} ل.س", color = GoldLight, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        Badge("تحديث ✎")
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("🧾", "مبيعات اليوم ل.س", Money.format(sSYP, Money.SYP), "${salesToday.count { it.invoice.currency == Money.SYP }} فاتورة", Modifier.weight(1f))
                StatCard("💵", "مبيعات اليوم $", Money.format(sUSD, Money.USD), "${salesToday.count { it.invoice.currency == Money.USD }} فاتورة", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("📥", "ذمم الزبائن", Money.format(recv.syp, Money.SYP), "+ " + Money.format(recv.usd, Money.USD), Modifier.weight(1f))
                StatCard("📤", "مستحقات الموردين", Money.format(pay.syp, Money.SYP), "+ " + Money.format(pay.usd, Money.USD), Modifier.weight(1f))
            }
        }
        if (isManager) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("📈", "ربح اليوم ل.س", Money.format(profitSYP, Money.SYP), "بعد التكلفة", Modifier.weight(1f))
                    StatCard("📈", "ربح اليوم $", Money.format(profitUSD, Money.USD), "بعد التكلفة", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("💰", "صندوق الليرة", Money.format(cashSYP.net, Money.SYP), "صافي الحركة اليوم", Modifier.weight(1f))
                    StatCard("💵", "صندوق الدولار", Money.format(cashUSD.net, Money.USD), "صافي الحركة اليوم", Modifier.weight(1f))
                }
            }
        }

        item {
            SectionCard(title = "اختصارات سريعة") {
                val actions = listOf(
                    Triple("🧾 فاتورة", "sale", Screen.Sale),
                    Triple("👥 زبائن", "customers", Screen.Customers),
                    Triple("🥜 أصناف", "items", Screen.Items),
                    Triple("🛒 شراء", "purchases", Screen.Purchases),
                    Triple("📦 مخزون", "inventory", Screen.Inventory),
                    Triple("💸 مصروف", "expenses", Screen.Expenses),
                    Triple("🎁 طلبات", "preorders", Screen.Preorders),
                    Triple("📊 تقارير", "reports", Screen.Reports),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (label, _, screen) ->
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { navigate(screen) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }

        if (low.isNotEmpty() || expiring.isNotEmpty()) {
            item {
                SectionCard(title = "تنبيهات المخزون", badge = "${low.size + expiring.count { (Dates.daysUntil(it.expiry) ?: 999) <= 30 }}") {
                    low.take(3).forEach { item ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (item.unit == "kg") "المتوفر ${item.stock / 1000.0} كغ" else "المتوفر ${item.stock} قطعة",
                                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Badge("مخزون منخفض", color = Danger, textColor = Danger)
                        }
                    }
                    expiring.filter { (Dates.daysUntil(it.expiry) ?: 999) <= 30 }.take(3).forEach { item ->
                        val d = Dates.daysUntil(item.expiry) ?: 0
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("ينتهي ${Dates.format(item.expiry)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Badge(if (d < 0) "منتهي" else "باقٍ $d يوم", color = if (d < 0) Danger else Gold, textColor = if (d < 0) Danger else BrownDark)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    ActionChip("عرض المخزون", { navigate(Screen.Inventory) })
                }
            }
        }

        item {
            SectionCard(title = "آخر الفواتير", badge = "${recent.size}") {
                if (recent.isEmpty()) {
                    Text("لا توجد فواتير بعد", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                recent.forEach { inv ->
                    Row(
                        Modifier.fillMaxWidth().clickable { navigate(Screen.InvoiceDetail(inv.invoice.id)) }
                            .padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("${inv.invoice.no} — ${inv.invoice.partyName}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${Dates.format(inv.invoice.date)} • ${inv.lines.size} بند", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            MoneyText(inv.invoice.total, inv.invoice.currency, 12, true)
                            Text(
                                if (inv.invoice.status == "cancelled") "ملغاة" else "مكتملة",
                                fontSize = 9.sp,
                                color = if (inv.invoice.status == "cancelled") Danger else Success,
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }

    if (rateDialog) {
        var rateText by remember { mutableStateOf(Money.formatRate(settings.rate).replace(",", "")) }
        AlertDialog(
            onDismissRequest = { rateDialog = false },
            title = { Text("تحديث سعر صرف الدولار", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("السعر الحالي: ${Money.formatRate(settings.rate)} ل.س", fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("سعر الدولار بالليرة") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "يُسجَّل مع كل فاتورة جديدة، وتعديله لا يغيّر الفواتير القديمة.",
                        fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val r = Money.rateFromInput(rateText)
                    if (r > 0) {
                        vm.updateSettings { it.copy(rate = r) }
                        vm.notify("تم تحديث سعر الصرف")
                    }
                    rateDialog = false
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { rateDialog = false }) { Text("إلغاء") } },
        )
    }
}
