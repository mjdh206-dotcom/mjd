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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.alharamayn.mahamess.data.Calc
import com.alharamayn.mahamess.data.DraftLine
import com.alharamayn.mahamess.data.InvoiceDraft
import com.alharamayn.mahamess.data.ItemEntity
import com.alharamayn.mahamess.ui.ActionChip
import com.alharamayn.mahamess.ui.AppTextField
import com.alharamayn.mahamess.ui.Badge
import com.alharamayn.mahamess.ui.Danger
import com.alharamayn.mahamess.ui.DropdownPicker
import com.alharamayn.mahamess.ui.Gold
import com.alharamayn.mahamess.ui.MoneyText
import com.alharamayn.mahamess.ui.Screen
import com.alharamayn.mahamess.ui.SectionCard
import com.alharamayn.mahamess.ui.Success
import com.alharamayn.mahamess.util.Analytics
import com.alharamayn.mahamess.util.Dates
import com.alharamayn.mahamess.util.Money

@Composable
fun PurchasesScreen(vm: AppViewModel, navigate: (Screen) -> Unit) {
    val items by vm.items.collectAsStateWithLifecycle()
    val suppliers by vm.suppliers.collectAsStateWithLifecycle()
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()

    var editorOpen by remember { mutableStateOf(false) }
    var supplierId by remember { mutableStateOf(0L) }
    var currency by remember { mutableStateOf(Money.SYP) }
    var date by remember { mutableStateOf(Dates.today()) }
    var payMethod by remember { mutableStateOf("cash") }
    var paidText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val lines = remember { mutableStateListOf<DraftLine>() }
    var picking by remember { mutableStateOf(false) }

    val purchases = Analytics.purchases(invoices).sortedByDescending { it.invoice.createdAt }
    val totals = Calc.totals(lines, "none", 0)
    val paid = when (payMethod) {
        "cash" -> totals.total
        "debt" -> 0L
        else -> Money.parse(paidText, currency)
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            SectionCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("فاتورة الشراء تزيد المخزون وتحدّث متوسط التكلفة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    ActionChip(if (editorOpen) "إغلاق" else "＋ فاتورة شراء", { editorOpen = !editorOpen }, filled = !editorOpen)
                }
            }
        }

        if (editorOpen) {
            item {
                SectionCard(title = "فاتورة شراء جديدة") {
                    DropdownPicker("المورد", suppliers, suppliers.find { it.id == supplierId }, { it.name }, onSelect = { supplierId = it.id })
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionChip("ليرة", { currency = Money.SYP }, filled = currency == Money.SYP)
                        ActionChip("دولار", { currency = Money.USD }, filled = currency == Money.USD)
                    }
                    Spacer(Modifier.height(6.dp))
                    AppTextField(date, { date = it }, "التاريخ")
                    Spacer(Modifier.height(6.dp))
                    ActionChip("＋ إضافة صنف", { picking = !picking })
                    if (picking) {
                        Spacer(Modifier.height(6.dp))
                        var search by remember { mutableStateOf("") }
                        AppTextField(search, { search = it }, "ابحث عن صنف")
                        items.filter { search.isBlank() || it.name.contains(search) }.take(6).forEach { item ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        val unit = if (item.unit == "kg") "kg" else "piece"
                                        lines.add(
                                            DraftLine(
                                                itemId = item.id, name = item.name, unit = unit,
                                                qtyMilli = 1000, unitPrice = item.cost,
                                                costPerBase = item.cost,
                                                baseQty = Calc.baseQty(unit, 1000, settings.ozGrams),
                                            )
                                        )
                                        picking = false
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(item.name, fontSize = 11.sp)
                                Text("تكلفة ${Money.format(item.cost, Money.SYP)}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(3.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    lines.forEachIndexed { index, line ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(line.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("✕", fontSize = 12.sp, color = Danger, modifier = Modifier.clickable { lines.removeAt(index) })
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = (line.qtyMilli / 1000.0).toString(),
                                    onValueChange = { v ->
                                        lines[index] = line.copy(
                                            qtyMilli = Math.round((v.toDoubleOrNull() ?: 0.0) * 1000),
                                            baseQty = Calc.baseQty(line.unit, Math.round((v.toDoubleOrNull() ?: 0.0) * 1000), settings.ozGrams),
                                        )
                                    },
                                    label = { Text("الكمية", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = Money.formatBare(line.unitPrice, currency),
                                    onValueChange = { v -> lines[index] = line.copy(unitPrice = Money.parse(v, currency)) },
                                    label = { Text("سعر الشراء", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                )
                            }
                        }
                    }
                    if (lines.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ActionChip("نقدي", { payMethod = "cash" }, filled = payMethod == "cash")
                            ActionChip("دين", { payMethod = "debt" }, filled = payMethod == "debt")
                            ActionChip("جزئي", { payMethod = "partial" }, filled = payMethod == "partial")
                        }
                        if (payMethod == "partial") {
                            Spacer(Modifier.height(6.dp))
                            AppTextField(paidText, { paidText = it }, "المبلغ المدفوع", numeric = true)
                        }
                        Spacer(Modifier.height(6.dp))
                        AppTextField(notes, { notes = it }, "ملاحظة")
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الإجمالي", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            MoneyText(totals.total, currency, 15, true, Gold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Gold)
                                .clickable {
                                    if (supplierId == 0L && suppliers.isNotEmpty()) supplierId = suppliers.first().id
                                    val supplier = suppliers.find { it.id == supplierId }
                                    vm.createInvoice(
                                        InvoiceDraft(
                                            type = "purchase", date = date, partyId = supplierId,
                                            partyName = supplier?.name ?: "",
                                            currency = currency, rate = settings.rate, lines = lines.toList(),
                                            payMethod = payMethod, paidCurrency = currency,
                                            paidAmount = paid, notes = notes,
                                            createdBy = session?.name ?: "المدير",
                                        )
                                    ) {
                                        lines.clear()
                                        editorOpen = false
                                        payMethod = "cash"
                                        paidText = ""
                                        notes = ""
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("💾 حفظ فاتورة الشراء", fontWeight = FontWeight.Bold, color = com.alharamayn.mahamess.ui.BrownDark) }
                    }
                }
            }
        }

        item { SectionCard(title = "فواتير المشتريات", badge = "${purchases.size}") { Text("اضغط على الفاتورة للتفاصيل", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

        purchases.forEach { inv ->
            item(key = inv.invoice.id) {
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth().clickable { navigate(Screen.InvoiceDetail(inv.invoice.id)) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${inv.invoice.no} — ${inv.invoice.partyName}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(Dates.format(inv.invoice.date), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            MoneyText(inv.invoice.total, inv.invoice.currency, 12, true)
                            Text(
                                "متبقي ${Money.format(inv.invoice.remaining, inv.invoice.currency)}",
                                fontSize = 9.sp, color = if (inv.invoice.remaining > 0) Danger else Success,
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}
