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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alharamayn.mahamess.AppViewModel
import com.alharamayn.mahamess.data.ClosingEntity
import com.alharamayn.mahamess.data.ExpenseEntity
import com.alharamayn.mahamess.data.PreorderEntity
import com.alharamayn.mahamess.data.WithdrawalEntity
import com.alharamayn.mahamess.ui.ActionChip
import com.alharamayn.mahamess.ui.AppTextField
import com.alharamayn.mahamess.ui.Badge
import com.alharamayn.mahamess.ui.BrownDark
import com.alharamayn.mahamess.ui.Danger
import com.alharamayn.mahamess.ui.Gold
import com.alharamayn.mahamess.ui.MoneyText
import com.alharamayn.mahamess.ui.SectionCard
import com.alharamayn.mahamess.ui.StatCard
import com.alharamayn.mahamess.ui.Success
import com.alharamayn.mahamess.util.Analytics
import com.alharamayn.mahamess.util.Dates
import com.alharamayn.mahamess.util.Money

@Composable
fun WithdrawalsScreen(vm: AppViewModel) {
    val withdrawals by vm.withdrawals.collectAsStateWithLifecycle()
    var date by remember { mutableStateOf(Dates.today()) }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(Money.SYP) }
    var note by remember { mutableStateOf("") }

    val syp = withdrawals.filter { it.currency == Money.SYP }.sumOf { it.amount }
    val usd = withdrawals.filter { it.currency == Money.USD }.sumOf { it.amount }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("🏦", "إجمالي المسحوبات", Money.format(syp, Money.SYP), "ليرة", Modifier.weight(1f))
                StatCard("💵", "بالدولار", Money.format(usd, Money.USD), "دولار", Modifier.weight(1f))
            }
        }
        item {
            SectionCard(title = "تسجيل مسحوب جديد") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("ليرة", { currency = Money.SYP }, filled = currency == Money.SYP)
                    ActionChip("دولار", { currency = Money.USD }, filled = currency == Money.USD)
                }
                Spacer(Modifier.height(6.dp))
                AppTextField(amount, { amount = it }, "المبلغ", numeric = true)
                Spacer(Modifier.height(6.dp))
                AppTextField(date, { date = it }, "التاريخ")
                Spacer(Modifier.height(6.dp))
                AppTextField(note, { note = it }, "الملاحظة")
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Gold)
                        .clickable {
                            val value = Money.parse(amount, currency)
                            if (value > 0) {
                                vm.addWithdrawal(WithdrawalEntity(date = date, amount = value, currency = currency, note = note))
                                amount = ""; note = ""
                            }
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("تسجيل المسحوب", fontWeight = FontWeight.Bold, color = BrownDark) }
            }
        }
        item { SectionCard(title = "سجل المسحوبات", badge = "${withdrawals.size}") { Text("", fontSize = 2.sp) } }
        withdrawals.forEach { w ->
            item(key = w.id) {
                SectionCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(Dates.format(w.date), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(w.note.ifBlank { "بدون ملاحظة" }, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        MoneyText(w.amount, w.currency, 13, true, Danger)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
fun ExpensesScreen(vm: AppViewModel) {
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    var date by remember { mutableStateOf(Dates.today()) }
    var category by remember { mutableStateOf("كهرباء") }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(Money.SYP) }
    var note by remember { mutableStateOf("") }
    val categories = listOf("إيجار", "كهرباء", "ماء", "أجور", "مواصلات", "أكياس وتغليف", "صيانة", "ضرائب", "أخرى")

    val syp = expenses.filter { it.currency == Money.SYP }.sumOf { it.amount }
    val usd = expenses.filter { it.currency == Money.USD }.sumOf { it.amount }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("💸", "المصاريف بالليرة", Money.format(syp, Money.SYP), "", Modifier.weight(1f))
                StatCard("💵", "المصاريف بالدولار", Money.format(usd, Money.USD), "", Modifier.weight(1f))
            }
        }
        item {
            SectionCard(title = "تسجيل مصروف") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.take(4).forEach { c ->
                        ActionChip(c, { category = c }, filled = category == c)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.drop(4).forEach { c ->
                        ActionChip(c, { category = c }, filled = category == c)
                    }
                }
                Spacer(Modifier.height(6.dp))
                AppTextField(amount, { amount = it }, "المبلغ", numeric = true)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("ليرة", { currency = Money.SYP }, filled = currency == Money.SYP)
                    ActionChip("دولار", { currency = Money.USD }, filled = currency == Money.USD)
                }
                Spacer(Modifier.height(6.dp))
                AppTextField(date, { date = it }, "التاريخ")
                Spacer(Modifier.height(6.dp))
                AppTextField(note, { note = it }, "ملاحظة")
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Gold)
                        .clickable {
                            val value = Money.parse(amount, currency)
                            if (value > 0) {
                                vm.addExpense(ExpenseEntity(date = date, category = category, amount = value, currency = currency, note = note))
                                amount = ""; note = ""
                            }
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("تسجيل المصروف", fontWeight = FontWeight.Bold, color = BrownDark) }
            }
        }
        item { SectionCard(title = "قائمة المصاريف", badge = "${expenses.size}") { Text("", fontSize = 2.sp) } }
        expenses.forEach { e ->
            item(key = e.id) {
                SectionCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(e.category, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${Dates.format(e.date)} ${e.note}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        MoneyText(e.amount, e.currency, 13, true, Danger)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
fun CashScreen(vm: AppViewModel) {
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val payments by vm.payments.collectAsStateWithLifecycle()
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val withdrawals by vm.withdrawals.collectAsStateWithLifecycle()
    val workerTx by vm.workerTx.collectAsStateWithLifecycle()
    val closings by vm.closings.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()

    var date by remember { mutableStateOf(Dates.today()) }
    var currency by remember { mutableStateOf(Money.SYP) }
    var opening by remember { mutableStateOf("") }
    var actual by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val flow = Analytics.cashFlow(currency, date, date, invoices, payments, expenses, withdrawals, workerTx)
    val openingMinor = Money.parse(opening, currency)
    val actualMinor = Money.parse(actual, currency)
    val system = openingMinor + flow.net
    val diff = if (actual.isBlank()) 0L else actualMinor - system
    val list = closings.filter { it.currency == currency }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("📥", "مقبوضات اليوم", Money.format(flow.cashIn + flow.payIn, currency), "", Modifier.weight(1f))
                StatCard("📤", "مدفوعات اليوم", Money.format(flow.out, currency), "", Modifier.weight(1f))
            }
        }
        item {
            SectionCard(title = "إقفال صندوق يومي") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("ليرة", { currency = Money.SYP }, filled = currency == Money.SYP)
                    ActionChip("دولار", { currency = Money.USD }, filled = currency == Money.USD)
                }
                Spacer(Modifier.height(6.dp))
                AppTextField(date, { date = it }, "التاريخ")
                Spacer(Modifier.height(6.dp))
                AppTextField(opening, { opening = it }, "الرصيد الافتتاحي", numeric = true)
                Spacer(Modifier.height(6.dp))
                AppTextField(actual, { actual = it }, "المبلغ الفعلي في الدرج", numeric = true)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
                        Text("المحسوب", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyText(system, currency, 12, true)
                    }
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
                        Text("الفعلي", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyText(actualMinor, currency, 12, true)
                    }
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
                        Text("الفرق", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyText(diff, currency, 12, true, if (diff < 0) Danger else Success)
                    }
                }
                Spacer(Modifier.height(6.dp))
                AppTextField(note, { note = it }, "ملاحظة")
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Gold)
                        .clickable {
                            if (actual.isNotBlank()) {
                                vm.addClosing(
                                    ClosingEntity(
                                        date = date, currency = currency, opening = openingMinor,
                                        systemCash = system, actualCash = actualMinor, diff = diff,
                                        note = note, createdBy = session?.name ?: "المدير",
                                    )
                                )
                                actual = ""; note = ""; opening = ""
                            }
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("حفظ الإقفال", fontWeight = FontWeight.Bold, color = BrownDark) }
            }
        }
        item {
            SectionCard(title = "تفصيل حركة الصندوق") {
                CashRow("مبيعات نقدية", flow.cashIn, currency)
                CashRow("دفعات زبائن", flow.payIn, currency)
                CashRow("مدفوعات موردين", -flow.purchases, currency)
                CashRow("مصاريف", -flow.expenses, currency)
                CashRow("مسحوبات المحل", -flow.withdrawals, currency)
                CashRow("سحوبات العمال", -flow.workerTx, currency)
                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الصافي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    MoneyText(flow.net, currency, 13, true, com.alharamayn.mahamess.ui.GoldDark)
                }
            }
        }
        item { SectionCard(title = "سجل الإقفالات", badge = "${list.size}") { Text("", fontSize = 2.sp) } }
        list.forEach { c ->
            item(key = c.id) {
                SectionCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(Dates.format(c.date), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("محسوب ${Money.format(c.systemCash, c.currency)} • فعلي ${Money.format(c.actualCash, c.currency)}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Badge(
                            "فرق ${Money.formatBare(c.diff, c.currency)}",
                            if (c.diff == 0L) Success else Danger,
                            if (c.diff == 0L) Success else Danger,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun CashRow(label: String, value: Long, currency: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyText(value, currency, 11, true, if (value < 0) Danger else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun PreordersScreen(vm: AppViewModel) {
    val preorders by vm.preorders.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<PreorderEntity?>(null) }

    val open = preorders.filter { it.status == "pending" || it.status == "ready" }
    val deposits = preorders.filter { it.status != "cancelled" }.sumOf { it.deposit }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("🎁", "طلبات مفتوحة", "${open.size}", "", Modifier.weight(1f))
                StatCard("💵", "إجمالي العربون", Money.format(deposits, Money.SYP), "", Modifier.weight(1f))
            }
        }
        item {
            SectionCard {
                Text("حجز سلال الضيافة للمناسبات مع تاريخ تسليم وعربون", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                ActionChip("＋ طلب مسبق", {
                    editing = PreorderEntity(
                        no = "PO-" + (preorders.size + 1).toString().padStart(3, '0'),
                        customerName = "", date = Dates.today(), deliveryDate = Dates.addDays(3),
                        total = 0, deposit = 0, currency = Money.SYP,
                    )
                }, filled = true)
            }
        }
        preorders.forEach { p ->
            item(key = p.id) {
                val days = Dates.daysUntil(p.deliveryDate)
                SectionCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${p.no} — ${p.customerName}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(5.dp))
                                Badge(
                                    when (p.status) {
                                        "ready" -> "جاهز"; "delivered" -> "تم التسليم"; "cancelled" -> "ملغى"; else -> "قيد الانتظار"
                                    },
                                    if (p.status == "delivered") Success else Gold,
                                    if (p.status == "delivered") Success else BrownDark,
                                )
                            }
                            Text("تسليم ${Dates.format(p.deliveryDate)}${if (days != null && p.status == "pending") " (${if (days < 0) "متأخر" else "بعد $days يوم"})" else ""}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("عربون ${Money.format(p.deposit, p.currency)} • متبقي ${Money.format(p.total - p.deposit, p.currency)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        MoneyText(p.total, p.currency, 12, true)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("pending" to "انتظار", "ready" to "جاهز", "delivered" to "تسليم", "cancelled" to "إلغاء").forEach { (status, label) ->
                            ActionChip(label, { vm.savePreorder(p.copy(status = status)) }, filled = p.status == status)
                        }
                        ActionChip("تعديل", { editing = p })
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    editing?.let { p ->
        var name by remember { mutableStateOf(p.customerName) }
        var phone by remember { mutableStateOf(p.phone) }
        var delivery by remember { mutableStateOf(p.deliveryDate) }
        var total by remember { mutableStateOf(if (p.total > 0) Money.formatBare(p.total, p.currency) else "") }
        var deposit by remember { mutableStateOf(if (p.deposit > 0) Money.formatBare(p.deposit, p.currency) else "") }
        var notes by remember { mutableStateOf(p.notes) }
        var currency by remember { mutableStateOf(p.currency) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (p.id == 0L) "طلب مسبق جديد" else "تعديل الطلب") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    AppTextField(name, { name = it }, "اسم الزبون")
                    Spacer(Modifier.height(6.dp))
                    AppTextField(phone, { phone = it }, "الهاتف")
                    Spacer(Modifier.height(6.dp))
                    AppTextField(delivery, { delivery = it }, "تاريخ التسليم")
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionChip("ليرة", { currency = Money.SYP }, filled = currency == Money.SYP)
                        ActionChip("دولار", { currency = Money.USD }, filled = currency == Money.USD)
                    }
                    Spacer(Modifier.height(6.dp))
                    AppTextField(total, { total = it }, "الإجمالي", numeric = true)
                    Spacer(Modifier.height(6.dp))
                    AppTextField(deposit, { deposit = it }, "العربون", numeric = true)
                    Spacer(Modifier.height(6.dp))
                    AppTextField(notes, { notes = it }, "تفاصيل الطلب")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        vm.savePreorder(
                            p.copy(
                                customerName = name, phone = phone, deliveryDate = delivery,
                                total = Money.parse(total, currency), deposit = Money.parse(deposit, currency),
                                currency = currency, notes = notes,
                            )
                        )
                        editing = null
                    }
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("إلغاء") } },
        )
    }
}
