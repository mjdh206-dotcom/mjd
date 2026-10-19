package com.alharamayn.mahamess.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alharamayn.mahamess.AppViewModel
import com.alharamayn.mahamess.data.CustomerEntity
import com.alharamayn.mahamess.data.InvoiceWithLines
import com.alharamayn.mahamess.data.PaymentEntity
import com.alharamayn.mahamess.data.SupplierEntity
import com.alharamayn.mahamess.ui.ActionChip
import com.alharamayn.mahamess.ui.AppTextField
import com.alharamayn.mahamess.ui.Badge
import com.alharamayn.mahamess.ui.Danger
import com.alharamayn.mahamess.ui.MoneyText
import com.alharamayn.mahamess.ui.Screen
import com.alharamayn.mahamess.ui.SectionCard
import com.alharamayn.mahamess.ui.Success
import com.alharamayn.mahamess.util.Analytics
import com.alharamayn.mahamess.util.Dates
import com.alharamayn.mahamess.util.Exporter
import com.alharamayn.mahamess.util.Money

@Composable
fun CustomersScreen(vm: AppViewModel, navigate: (Screen) -> Unit) {
    val customers by vm.customers.collectAsStateWithLifecycle()
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val payments by vm.payments.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<CustomerEntity?>(null) }
    var paying by remember { mutableStateOf<CustomerEntity?>(null) }

    val list = customers.filter { search.isBlank() || it.name.contains(search) || it.phone.contains(search) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            SectionCard {
                AppTextField(search, { search = it }, "بحث عن زبون")
                Spacer(Modifier.height(8.dp))
                ActionChip("＋ زبون جديد", {
                    editing = CustomerEntity(name = "", type = "credit")
                }, filled = true)
            }
        }
        list.forEach { customer ->
            item(key = customer.id) {
                val balance = Analytics.partyBalance(customer.id, "customer", invoices, payments)
                val count = Analytics.sales(invoices).count { it.invoice.partyId == customer.id }
                SectionCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(customer.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                if (customer.type == "cash") {
                                    Spacer(Modifier.width(5.dp)); Badge("نقدي")
                                }
                            }
                            Text(
                                "${customer.phone.ifBlank { "بدون هاتف" }} • $count فاتورة",
                                fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(3.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row {
                                    Text("ل.س ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    MoneyText(balance.syp, Money.SYP, 11, true, if (balance.syp > 0) Danger else Success)
                                }
                                Row {
                                    Text("$ ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    MoneyText(balance.usd, Money.USD, 11, true, if (balance.usd > 0) Danger else Success)
                                }
                            }
                        }
                        if (customer.id != 1L) {
                            Column(horizontalAlignment = Alignment.End) {
                                ActionChip("دفعة", { paying = customer }, filled = true)
                                Spacer(Modifier.height(5.dp))
                                ActionChip("كشف حساب", { navigate(Screen.Statement(customer.id)) })
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionChip("تعديل", { editing = customer })
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    editing?.let { customer ->
        var name by remember { mutableStateOf(customer.name) }
        var phone by remember { mutableStateOf(customer.phone) }
        var type by remember { mutableStateOf(customer.type) }
        var notes by remember { mutableStateOf(customer.notes) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (customer.id == 0L) "زبون جديد" else "تعديل زبون") },
            text = {
                Column {
                    AppTextField(name, { name = it }, "الاسم")
                    Spacer(Modifier.height(6.dp))
                    AppTextField(phone, { phone = it }, "الهاتف")
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionChip("آجل", { type = "credit" }, filled = type == "credit")
                        ActionChip("نقدي", { type = "cash" }, filled = type == "cash")
                    }
                    Spacer(Modifier.height(6.dp))
                    AppTextField(notes, { notes = it }, "ملاحظات")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        vm.saveCustomer(customer.copy(name = name, phone = phone, type = type, notes = notes))
                        editing = null
                    }
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("إلغاء") } },
        )
    }

    paying?.let { customer ->
        PaymentDialog(
            title = "تسجيل دفعة — ${customer.name}",
            onDismiss = { paying = null },
            onSave = { currency, amount, date, note ->
                vm.addPayment(
                    PaymentEntity(
                        partyType = "customer", partyId = customer.id, date = date,
                        currency = currency, amount = amount, note = note,
                    )
                )
                paying = null
            },
        )
    }
}

@Composable
fun StatementScreen(vm: AppViewModel, customerId: Long, onBack: () -> Unit) {
    val customers by vm.customers.collectAsStateWithLifecycle()
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val payments by vm.payments.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var paying by remember { mutableStateOf(false) }

    val customer = customers.find { it.id == customerId }
    val balance = Analytics.partyBalance(customerId, "customer", invoices, payments)
    val invs = Analytics.sales(invoices).filter { it.invoice.partyId == customerId }.sortedBy { it.invoice.date }
    val pays = payments.filter { it.partyId == customerId && it.partyType == "customer" }.sortedBy { it.date }

    fun share() {
        val sb = StringBuilder()
        sb.appendLine("*${settings.shopName}*")
        sb.appendLine("كشف حساب: ${customer?.name}")
        sb.appendLine("التاريخ: ${Dates.format(Dates.today())}")
        sb.appendLine()
        invs.forEach { sb.appendLine("${Dates.format(it.invoice.date)} | ${it.invoice.no} | فاتورة | ${Money.format(it.invoice.total, it.invoice.currency)}") }
        pays.forEach { sb.appendLine("${Dates.format(it.date)} | دفعة | ${Money.format(it.amount, it.currency)}") }
        sb.appendLine()
        sb.appendLine("*الرصيد المستحق:*")
        sb.appendLine("ليرة سورية: ${Money.format(balance.syp, Money.SYP)}")
        sb.appendLine("دولار أمريكي: ${Money.format(balance.usd, Money.USD)}")
        if (settings.phone.isNotBlank()) sb.appendLine("للتواصل: ${settings.phone}")
        Exporter.whatsapp(context, customer?.phone ?: "", sb.toString())
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            SectionCard(title = "كشف حساب — ${customer?.name ?: ""}") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("← رجوع", onBack)
                    ActionChip("واتساب", { share() }, filled = true)
                    ActionChip("تسجيل دفعة", { paying = true })
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("الرصيد بالليرة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyText(balance.syp, Money.SYP, 16, true, if (balance.syp > 0) Danger else Success)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("الرصيد بالدولار", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyText(balance.usd, Money.USD, 16, true, if (balance.usd > 0) Danger else Success)
                    }
                }
            }
        }
        item {
            SectionCard(title = "الفواتير", badge = "${invs.size}") {
                if (invs.isEmpty()) Text("لا توجد فواتير", fontSize = 12.sp)
                invs.forEach { inv ->
                    InvoiceStatementRow(inv)
                }
            }
        }
        item {
            SectionCard(title = "الدفعات", badge = "${pays.size}") {
                if (pays.isEmpty()) Text("لا توجد دفعات", fontSize = 12.sp)
                pays.forEach { p ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("دفعة ${Dates.format(p.date)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            if (p.note.isNotBlank()) Text(p.note, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        MoneyText(p.amount, p.currency, 12, true, Success)
                    }
                }
            }
        }
        item {
            Text(
                "كل عملة تُحسب على حدة، ولا يتم خلط الليرة بالدولار في الرصيد.",
                fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    if (paying) {
        PaymentDialog(
            title = "دفعة — ${customer?.name ?: ""}",
            onDismiss = { paying = false },
            onSave = { currency, amount, date, note ->
                vm.addPayment(
                    PaymentEntity(
                        partyType = "customer", partyId = customerId, date = date,
                        currency = currency, amount = amount, note = note,
                    )
                )
                paying = false
            },
        )
    }
}

@Composable
private fun InvoiceStatementRow(inv: InvoiceWithLines) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("${inv.invoice.no} • ${Dates.format(inv.invoice.date)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                if (inv.invoice.status == "cancelled") "ملغاة" else "متبقي ${Money.format(inv.invoice.remaining, inv.invoice.currency)}",
                fontSize = 9.sp, color = if (inv.invoice.status == "cancelled") Danger else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MoneyText(inv.invoice.total, inv.invoice.currency, 12, true)
    }
}

@Composable
fun SuppliersScreen(vm: AppViewModel) {
    val suppliers by vm.suppliers.collectAsStateWithLifecycle()
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val payments by vm.payments.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<SupplierEntity?>(null) }
    var paying by remember { mutableStateOf<SupplierEntity?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            SectionCard {
                Text("حساب كل مورد: المستحق عليه من فواتير الشراء والدفعات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                ActionChip("＋ مورد جديد", { editing = SupplierEntity(name = "", phone = "") }, filled = true)
            }
        }
        suppliers.forEach { supplier ->
            item(key = supplier.id) {
                val balance = Analytics.partyBalance(supplier.id, "supplier", invoices, payments)
                SectionCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(supplier.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(supplier.phone.ifBlank { "بدون هاتف" }, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(3.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MoneyText(balance.syp, Money.SYP, 11, true, if (balance.syp > 0) Danger else Success)
                                MoneyText(balance.usd, Money.USD, 11, true)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            ActionChip("دفعة", { paying = supplier }, filled = true)
                            Spacer(Modifier.height(5.dp))
                            ActionChip("تعديل", { editing = supplier })
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    editing?.let { supplier ->
        var name by remember { mutableStateOf(supplier.name) }
        var phone by remember { mutableStateOf(supplier.phone) }
        var notes by remember { mutableStateOf(supplier.notes) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (supplier.id == 0L) "مورد جديد" else "تعديل مورد") },
            text = {
                Column {
                    AppTextField(name, { name = it }, "الاسم")
                    Spacer(Modifier.height(6.dp))
                    AppTextField(phone, { phone = it }, "الهاتف")
                    Spacer(Modifier.height(6.dp))
                    AppTextField(notes, { notes = it }, "ملاحظات")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        vm.saveSupplier(supplier.copy(name = name, phone = phone, notes = notes))
                        editing = null
                    }
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("إلغاء") } },
        )
    }

    paying?.let { supplier ->
        PaymentDialog(
            title = "دفعة لمورد — ${supplier.name}",
            onDismiss = { paying = null },
            onSave = { currency, amount, date, note ->
                vm.addPayment(
                    PaymentEntity(
                        partyType = "supplier", partyId = supplier.id, date = date,
                        currency = currency, amount = amount, note = note,
                    )
                )
                paying = null
            },
        )
    }
}

@Composable
fun PaymentDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: (String, Long, String, String) -> Unit,
) {
    var currency by remember { mutableStateOf(Money.SYP) }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Dates.today()) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 14.sp) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("ليرة", { currency = Money.SYP }, filled = currency == Money.SYP)
                    ActionChip("دولار", { currency = Money.USD }, filled = currency == Money.USD)
                }
                Spacer(Modifier.height(6.dp))
                AppTextField(amount, { amount = it }, "المبلغ", numeric = true)
                Spacer(Modifier.height(6.dp))
                AppTextField(date, { date = it }, "التاريخ (2026-10-19)")
                Spacer(Modifier.height(6.dp))
                AppTextField(note, { note = it }, "ملاحظة")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = Money.parse(amount, currency)
                if (value > 0) onSave(currency, value, date, note)
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}
