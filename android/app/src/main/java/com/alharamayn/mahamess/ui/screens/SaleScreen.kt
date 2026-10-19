package com.alharamayn.mahamess.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alharamayn.mahamess.AppViewModel
import com.alharamayn.mahamess.data.Calc
import com.alharamayn.mahamess.data.CustomerEntity
import com.alharamayn.mahamess.data.DraftLine
import com.alharamayn.mahamess.data.InvoiceDraft
import com.alharamayn.mahamess.data.ItemEntity
import com.alharamayn.mahamess.ui.ActionChip
import com.alharamayn.mahamess.ui.AppTextField
import com.alharamayn.mahamess.ui.BrownDark
import com.alharamayn.mahamess.ui.Danger
import com.alharamayn.mahamess.ui.DropdownPicker
import com.alharamayn.mahamess.ui.Gold
import com.alharamayn.mahamess.ui.MoneyText
import com.alharamayn.mahamess.ui.SectionCard
import com.alharamayn.mahamess.ui.Success
import com.alharamayn.mahamess.util.Dates
import com.alharamayn.mahamess.util.Money

@Composable
fun SaleScreen(vm: AppViewModel, editId: Long?, onDone: () -> Unit) {
    val items by vm.items.collectAsStateWithLifecycle()
    val customers by vm.customers.collectAsStateWithLifecycle()
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val editing = editId?.let { id -> invoices.find { it.invoice.id == id } }

    var customerId by remember(editing) { mutableStateOf(editing?.invoice?.partyId ?: (customers.firstOrNull { it.type == "cash" }?.id ?: 0L)) }
    var currency by remember(editing) { mutableStateOf(editing?.invoice?.currency ?: settings.defaultCurrency) }
    var date by remember(editing) { mutableStateOf(editing?.invoice?.date ?: Dates.today()) }
    var search by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var barcodeMode by remember { mutableStateOf(false) }
    var discountType by remember(editing) { mutableStateOf(if ((editing?.invoice?.discount ?: 0L) > 0) "fixed" else "none") }
    var discountText by remember(editing) {
        mutableStateOf(
            editing?.let { if (it.invoice.discount > 0) Money.formatBare(it.invoice.discount, it.invoice.currency) else "" } ?: "",
        )
    }
    var payMethod by remember(editing) { mutableStateOf(editing?.invoice?.payMethod ?: "cash") }
    var paidText by remember { mutableStateOf("") }
    var paidCurrency by remember(editing) { mutableStateOf(editing?.invoice?.currency ?: settings.defaultCurrency) }
    var notes by remember(editing) { mutableStateOf(editing?.invoice?.notes ?: "") }
    var pickerItem by remember { mutableStateOf<ItemEntity?>(null) }
    var editingLineIndex by remember { mutableStateOf<Int?>(null) }

    val lines = remember(editing) {
        mutableStateListOf<DraftLine>().apply {
            editing?.lines?.forEach { l ->
                add(DraftLine(l.itemId, l.name, l.unit, l.qtyMilli, l.unitPrice, l.discount, l.costPerBase, l.baseQty, l.isBasket))
            }
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (text != null) {
                search = text
                val found = items.find { text.contains(it.name) || it.name.contains(text) }
                if (found != null) pickerItem = found else vm.notify("لم أجد صنفاً مطابقاً: $text")
            }
        }
    }

    val discountValue = when (discountType) {
        "percent" -> (discountText.toDoubleOrNull() ?: 0.0).toLong()
        "fixed" -> Money.parse(discountText, currency)
        else -> 0L
    }
    val totals = Calc.totals(lines, discountType, discountValue)
    val paidAmount = when (payMethod) {
        "cash" -> totals.total
        "debt" -> 0L
        else -> Money.parse(paidText, paidCurrency)
    }
    val paidInInvoice = Money.convert(paidAmount, paidCurrency, currency, settings.rate)
    val remaining = (totals.total - paidInInvoice).coerceAtLeast(0)
    val change = (paidInInvoice - totals.total).coerceAtLeast(0)

    LaunchedEffect(customers, editing) {
        if (editing == null && (customerId == 0L) && customers.isNotEmpty()) {
            customerId = customers.firstOrNull { it.type == "cash" }?.id ?: customers.first().id
        }
    }

    fun addItem(item: ItemEntity, qtyMilli: Long, unit: String, price: Long) {
        val isWeight = item.unit == "kg"
        val base = Calc.baseQty(unit, qtyMilli, settings.ozGrams)
        val costPerBase = if (item.isBasket) {
            vm.repo.parseComponents(item.componentsJson).sumOf { (compId, qty) ->
                val comp = items.find { it.id == compId }
                (comp?.cost ?: 0L) * qty
            }
        } else item.cost
        lines.add(
            DraftLine(
                itemId = item.id, name = item.name, unit = unit, qtyMilli = qtyMilli,
                unitPrice = price, costPerBase = costPerBase, baseQty = base, isBasket = item.isBasket,
            )
        )
        vm.notify("تمت إضافة ${item.name}")
        pickerItem = null
        search = ""
        barcode = ""
    }

    fun save() {
        if (lines.isEmpty()) { vm.notify("أضف صنفاً واحداً على الأقل"); return }
        val customer = customers.find { it.id == customerId }
        val draft = InvoiceDraft(
            type = "sale",
            date = date,
            partyId = customerId,
            partyName = customer?.name ?: "زبون نقدي",
            currency = currency,
            rate = settings.rate,
            lines = lines.toList(),
            discountType = discountType,
            discountValue = discountValue,
            payMethod = payMethod,
            paidCurrency = paidCurrency,
            paidAmount = paidAmount,
            notes = notes,
            createdBy = session?.name ?: "المدير",
        )
        if (editing != null) {
            vm.updateInvoice(editing, draft) { onDone() }
        } else {
            vm.createInvoice(draft) { onDone() }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            SectionCard(title = if (editing != null) "تعديل الفاتورة ${editing.invoice.no}" else "فاتورة مبيعات جديدة") {
                DropdownPicker(
                    label = "الزبون",
                    items = customers,
                    selected = customers.find { it.id == customerId },
                    itemLabel = { it.name },
                    onSelect = { customerId = it.id },
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionChip("ليرة سورية", { currency = Money.SYP; paidCurrency = Money.SYP }, filled = currency == Money.SYP)
                    ActionChip("دولار أمريكي", { currency = Money.USD; paidCurrency = Money.USD }, filled = currency == Money.USD)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "سعر الصرف المسجّل: ${Money.formatRate(settings.rate)} ل.س / $ — يُحفظ مع الفاتورة",
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(title = "البحث عن صنف") {
                AppTextField(search, { search = it }, "ابحث بالاسم...")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ActionChip(if (barcodeMode) "إخفاء الباركود" else "▮▯ باركود", { barcodeMode = !barcodeMode })
                    ActionChip("🎙 صوت", {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SY")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "قل اسم الصنف")
                        }
                        try { voiceLauncher.launch(intent) } catch (e: Exception) {
                            vm.notify("لا يوجد محرك تعرف صوتي على الجهاز")
                        }
                    })
                }
                if (barcodeMode) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("الباركود (قارئ أو إدخال يدوي)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = {
                                val item = items.find { it.barcode == barcode.trim() }
                                if (item != null) pickerItem = item else vm.notify("لا يوجد صنف بهذا الباركود")
                            },
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                        ),
                    )
                }
                Spacer(Modifier.height(8.dp))
                val results = items.filter {
                    search.isBlank() || it.name.contains(search.trim()) || (it.barcode.isNotBlank() && it.barcode.contains(search.trim()))
                }.take(8)
                results.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { pickerItem = item }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (item.unit == "kg") "متوفر ${item.stock / 1000.0} كغ" else "متوفر ${item.stock} قطعة",
                                fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val displayPrice = if (currency == Money.USD)
                            Money.convert(item.retail, Money.SYP, Money.USD, settings.rate)
                        else item.retail
                        Text(
                            Money.format(displayPrice, currency, false),
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrownDark,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        item {
            SectionCard(title = "بنود الفاتورة", badge = "${lines.size}") {
                if (lines.isEmpty()) {
                    Text("لم تُضف أصناف بعد", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                lines.forEachIndexed { index, line ->
                    Row(
                        Modifier.fillMaxWidth().clickable { editingLineIndex = index }.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(line.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "${(line.qtyMilli / 1000.0)} ${com.alharamayn.mahamess.data.Units.short(line.unit)} × ${Money.format(line.unitPrice, currency)}",
                                fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        MoneyText(Calc.lineTotal(line), currency, 12, true)
                        Spacer(Modifier.width(8.dp))
                        Text("✕", fontSize = 13.sp, color = Danger, modifier = Modifier.clickable { lines.removeAt(index) })
                    }
                }
            }
        }

        item {
            SectionCard(title = "الخصم والدفع") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("بدون خصم", { discountType = "none"; discountText = "" }, filled = discountType == "none")
                    ActionChip("خصم %", { discountType = "percent" }, filled = discountType == "percent")
                    ActionChip("خصم مبلغ", { discountType = "fixed" }, filled = discountType == "fixed")
                }
                if (discountType != "none") {
                    Spacer(Modifier.height(6.dp))
                    AppTextField(discountText, { discountText = it }, if (discountType == "percent") "نسبة الخصم %" else "مبلغ الخصم", numeric = true)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("نقدي", { payMethod = "cash" }, filled = payMethod == "cash")
                    ActionChip("دين", { payMethod = "debt" }, filled = payMethod == "debt")
                    ActionChip("دفعة جزئية", { payMethod = "partial" }, filled = payMethod == "partial")
                }
                if (payMethod == "partial") {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { AppTextField(paidText, { paidText = it }, "المبلغ المدفوع", numeric = true) }
                        ActionChip("ل.س", { paidCurrency = Money.SYP }, filled = paidCurrency == Money.SYP)
                        ActionChip("$", { paidCurrency = Money.USD }, filled = paidCurrency == Money.USD)
                    }
                }
                Spacer(Modifier.height(8.dp))
                AppTextField(notes, { notes = it }, "ملاحظة على الفاتورة")
            }
        }

        item {
            SectionCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("المجموع الفرعي", fontSize = 12.sp)
                    MoneyText(totals.subtotal, currency)
                }
                if (totals.discount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الخصم", fontSize = 12.sp, color = Danger)
                        MoneyText(-totals.discount, currency, color = Danger)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("الإجمالي", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    MoneyText(totals.total, currency, 19, true, Gold)
                }
                if (session?.role == "manager") {
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الربح (للمدير فقط)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyText(totals.profit, currency, 12, true, Success)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("المدفوع", fontSize = 12.sp)
                    MoneyText(paidInInvoice, currency)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("المتبقي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    MoneyText(remaining, currency, 13, true, Danger)
                }
                if (change > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الباقي للزبون", fontSize = 11.sp)
                        MoneyText(change, currency, 12)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Gold)
                        .clickable { save() }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (editing != null) "💾 حفظ التعديلات" else "💾 حفظ الفاتورة",
                        fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = BrownDark,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    pickerItem?.let { item ->
        AddItemDialog(
            item = item,
            currency = currency,
            ozGrams = settings.ozGrams,
            rate = settings.rate,
            lastPrice = run {
                val last = Analytics_lastPrice(invoices, item.id, customerId)
                last
            },
            onDismiss = { pickerItem = null },
            onAdd = { qty, unit, price -> addItem(item, qty, unit, price) },
        )
    }

    editingLineIndex?.let { idx ->
        val line = lines.getOrNull(idx) ?: return@let
        EditLineDialog(
            line = line,
            currency = currency,
            ozGrams = settings.ozGrams,
            onDismiss = { editingLineIndex = null },
            onSave = { updated -> lines[idx] = updated; editingLineIndex = null },
        )
    }
}

private fun Analytics_lastPrice(
    invoices: List<com.alharamayn.mahamess.data.InvoiceWithLines>,
    itemId: Long,
    customerId: Long,
) = invoices.firstOrNull { inv ->
    inv.invoice.type == "sale" && inv.invoice.partyId == customerId &&
        inv.lines.any { it.itemId == itemId }
}?.let { inv ->
    val l = inv.lines.first { it.itemId == itemId }
    Pair(l.unitPrice, inv.invoice.currency)
}

@Composable
private fun AddItemDialog(
    item: ItemEntity,
    currency: String,
    ozGrams: Int,
    rate: Long,
    lastPrice: Pair<Long, String>?,
    onDismiss: () -> Unit,
    onAdd: (Long, String, Long) -> Unit,
) {
    val isWeight = item.unit == "kg"
    var unit by remember { mutableStateOf(if (isWeight) "kg" else "piece") }
    var qtyText by remember { mutableStateOf("1") }
    var priceText by remember {
        mutableStateOf(
            Money.formatBare(Calc.priceForUnit(item.retail, if (isWeight) "kg" else "piece", ozGrams, isWeight), currency),
        )
    }

    fun refreshPrice() {
        // آخر سعر يُقترح فقط للوحدة الأساسية نفسها، وبقية الوحدات تُحسب من السعر المفرق
        val baseUnit = if (isWeight) "kg" else "piece"
        val p = if (lastPrice != null && unit == baseUnit) {
            Money.convert(lastPrice.first, lastPrice.second, currency, rate)
        } else {
            Calc.priceForUnit(item.retail, unit, ozGrams, isWeight)
        }
        priceText = Money.formatBare(p, currency)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    if (isWeight) "المخزون: ${item.stock / 1000.0} كغ" else "المخزون: ${item.stock} قطعة",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (lastPrice != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "💡 آخر سعر لهذا الزبون: ${Money.format(lastPrice.first, lastPrice.second)}",
                        fontSize = 10.sp, color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (isWeight) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("kg", "g", "oz").forEach { u ->
                            ActionChip(com.alharamayn.mahamess.data.Units.short(u), {
                                unit = u
                                refreshPrice()
                            }, filled = unit == u)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                AppTextField(qtyText, { qtyText = it }, "الكمية", numeric = true)
                Spacer(Modifier.height(6.dp))
                AppTextField(priceText, { priceText = it }, "سعر ${com.alharamayn.mahamess.data.Units.short(unit)}", numeric = true)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isWeight) {
                        ActionChip("250 غ", { unit = "g"; qtyText = "250"; refreshPrice() })
                        ActionChip("500 غ", { unit = "g"; qtyText = "500"; refreshPrice() })
                        ActionChip("1 كغ", { unit = "kg"; qtyText = "1"; refreshPrice() })
                    } else {
                        ActionChip("1 قطعة", { qtyText = "1" })
                        ActionChip("5 قطع", { qtyText = "5" })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val qty = Math.round((qtyText.toDoubleOrNull() ?: 0.0) * 1000)
                val price = Money.parse(priceText, currency)
                if (qty > 0 && price > 0) onAdd(qty, unit, price)
            }) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
private fun EditLineDialog(
    line: DraftLine,
    currency: String,
    ozGrams: Int,
    onDismiss: () -> Unit,
    onSave: (DraftLine) -> Unit,
) {
    var qty by remember { mutableStateOf((line.qtyMilli / 1000.0).toString()) }
    var price by remember { mutableStateOf(Money.formatBare(line.unitPrice, currency)) }
    var unit by remember { mutableStateOf(line.unit) }
    var discount by remember { mutableStateOf(if (line.discount > 0) Money.formatBare(line.discount, currency) else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(line.name, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                AppTextField(qty, { qty = it }, "الكمية", numeric = true)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("kg", "g", "oz").forEach { u ->
                        if (line.unit in listOf("kg", "g", "oz")) {
                            ActionChip(com.alharamayn.mahamess.data.Units.short(u), { unit = u }, filled = unit == u)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                AppTextField(price, { price = it }, "سعر الوحدة", numeric = true)
                Spacer(Modifier.height(6.dp))
                AppTextField(discount, { discount = it }, "خصم البند", numeric = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val q = Math.round((qty.toDoubleOrNull() ?: 0.0) * 1000)
                val p = Money.parse(price, currency)
                onSave(
                    line.copy(
                        qtyMilli = q, unitPrice = p, unit = unit,
                        discount = Money.parse(discount, currency),
                        baseQty = Calc.baseQty(unit, q, ozGrams),
                    )
                )
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}
