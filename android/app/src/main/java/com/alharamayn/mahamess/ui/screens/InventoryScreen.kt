package com.alharamayn.mahamess.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
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
import com.alharamayn.mahamess.data.ItemEntity
import com.alharamayn.mahamess.ui.ActionChip
import com.alharamayn.mahamess.ui.AppTextField
import com.alharamayn.mahamess.ui.Badge
import com.alharamayn.mahamess.ui.Danger
import com.alharamayn.mahamess.ui.DropdownPicker
import com.alharamayn.mahamess.ui.Gold
import com.alharamayn.mahamess.ui.MoneyText
import com.alharamayn.mahamess.ui.SectionCard
import com.alharamayn.mahamess.ui.Success
import com.alharamayn.mahamess.util.Dates
import com.alharamayn.mahamess.util.Money

@Composable
fun InventoryScreen(vm: AppViewModel) {
    val items by vm.items.collectAsStateWithLifecycle()
    val moves by vm.stockMoves.collectAsStateWithLifecycle()
    val roasts by vm.roasts.collectAsStateWithLifecycle()
    val stocktakes by vm.stocktakes.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf("stock") }
    var stocktakeFor by remember { mutableStateOf<ItemEntity?>(null) }
    var showRoast by remember { mutableStateOf(false) }

    val low = items.filter { it.minStock > 0 && it.stock <= it.minStock }
    val expiring = items.filter { it.expiry != null && (Dates.daysUntil(it.expiry) ?: 999) <= 60 }
    val inventoryValue = items.sumOf { it.stock * (if (it.unit == "kg") it.cost / 1000.0 else it.cost.toDouble()).toLong() }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.alharamayn.mahamess.ui.StatCard("📦", "قيمة المخزون", Money.format(inventoryValue, Money.SYP), "بالتكلفة", Modifier.weight(1f))
                com.alharamayn.mahamess.ui.StatCard("⚠️", "تحت الحد الأدنى", "${low.size}", "صنف", Modifier.weight(1f))
            }
        }
        item {
            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("المخزون", { tab = "stock" }, filled = tab == "stock")
                    ActionChip("التنبيهات (${low.size + expiring.size})", { tab = "alerts" }, filled = tab == "alerts")
                    ActionChip("التحميص", { tab = "roast" }, filled = tab == "roast")
                    ActionChip("الحركات", { tab = "moves" }, filled = tab == "moves")
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("📋 جرد يدوي", { stocktakeFor = items.firstOrNull() }, filled = true)
                    ActionChip("🔥 تسجيل تحميص", { showRoast = true })
                }
            }
        }

        when (tab) {
            "stock" -> items.forEach { item ->
                this.item(key = item.id) {
                    val isWeight = item.unit == "kg"
                    val ratio = if (item.minStock > 0) (item.stock.toFloat() / (item.minStock * 3)).coerceIn(0f, 1f) else 1f
                    SectionCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (isWeight) "${item.stock / 1000.0} كغ" else "${item.stock} قطعة",
                                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                                    color = if (item.stock <= item.minStock) Danger else Gold,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Badge(
                                    if (item.stock <= item.minStock) "تحت الحد" else "جيد",
                                    if (item.stock <= item.minStock) Danger else Success,
                                    if (item.stock <= item.minStock) Danger else Success,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("تكلفة ${Money.format(item.cost, Money.SYP)}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (item.expiry != null) Text("صلاحية ${Dates.format(item.expiry)}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            "alerts" -> {
                item {
                    SectionCard(title = "مخزون منخفض") {
                        if (low.isEmpty()) Text("لا يوجد نقص", fontSize = 12.sp)
                        low.forEach { item ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.name, fontSize = 12.sp)
                                Text(if (item.unit == "kg") "${item.stock / 1000.0} كغ" else "${item.stock} قطعة", fontSize = 11.sp, color = Danger)
                            }
                        }
                    }
                }
                item {
                    SectionCard(title = "قرب انتهاء الصلاحية") {
                        if (expiring.isEmpty()) Text("لا يوجد", fontSize = 12.sp)
                        expiring.forEach { item ->
                            val d = Dates.daysUntil(item.expiry) ?: 0
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(Dates.format(item.expiry), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Badge(if (d < 0) "منتهي" else "باقٍ $d يوم", if (d < 0) Danger else Gold, if (d < 0) Danger else com.alharamayn.mahamess.ui.BrownDark)
                            }
                        }
                    }
                }
            }

            "roast" -> {
                item {
                    SectionCard(title = "سجل التحميص") {
                        Text("تسجيل كمية الخام والمحمّص لحساب الفاقد والتكلفة الفعلية", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        if (roasts.isEmpty()) Text("لا توجد عمليات تحميص", fontSize = 12.sp)
                        roasts.forEach { r ->
                            val raw = items.find { it.id == r.rawItemId }
                            val roasted = items.find { it.id == r.roastedItemId }
                            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Text("${raw?.name ?: ""} ← ${roasted?.name ?: ""}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "خام ${r.rawQtyBase / 1000.0} كغ • محمّص ${r.roastedQtyBase / 1000.0} كغ • فاقد ${r.loss / 1000.0} كغ (${String.format("%.1f", r.lossPct)}%)",
                                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text("التكلفة الفعلية ${Money.format(r.actualCostPerKg, Money.SYP)} /كغ", fontSize = 10.sp, color = Gold)
                            }
                        }
                    }
                }
            }

            "moves" -> {
                item {
                    SectionCard(title = "حركات المخزون", badge = "${moves.size}") {
                        if (moves.isEmpty()) Text("لا توجد حركات", fontSize = 12.sp)
                        moves.take(60).forEach { m ->
                            val item = items.find { it.id == m.itemId }
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(item?.name ?: "—", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("${Dates.formatMillis(m.at)} • ${m.note}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    "${if (m.qtyBase > 0) "+" else ""}${m.qtyBase}",
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    color = if (m.qtyBase > 0) Success else Danger,
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    stocktakeFor?.let { item ->
        StocktakeDialog(vm, item, items, onDismiss = { stocktakeFor = null })
    }

    if (showRoast) {
        RoastDialog(vm, items, onDismiss = { showRoast = false })
    }
}

@Composable
private fun StocktakeDialog(vm: AppViewModel, initial: ItemEntity, items: List<ItemEntity>, onDismiss: () -> Unit) {
    var item by remember { mutableStateOf(initial) }
    var counted by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val isWeight = item.unit == "kg"
    val countedBase = Math.round((counted.toDoubleOrNull() ?: 0.0) * if (isWeight) 1000 else 1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("جرد يدوي") },
        text = {
            Column {
                DropdownPicker("الصنف", items, item, { it.name }) { item = it }
                Spacer(Modifier.height(8.dp))
                Text(
                    "كمية النظام: " + if (isWeight) "${item.stock / 1000.0} كغ" else "${item.stock} قطعة",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                AppTextField(counted, { counted = it }, if (isWeight) "الكمية الفعلية (كغ)" else "الكمية الفعلية (قطعة)", numeric = true)
                if (counted.isNotBlank()) {
                    Text(
                        "الفرق: ${(countedBase - item.stock) / (if (isWeight) 1000.0 else 1.0)}",
                        fontSize = 11.sp, color = Danger,
                    )
                }
                Spacer(Modifier.height(6.dp))
                AppTextField(note, { note = it }, "ملاحظة")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (counted.isNotBlank()) {
                    vm.addStocktake(item, countedBase, note, Dates.today())
                    onDismiss()
                }
            }) { Text("حفظ الجرد") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
private fun RoastDialog(vm: AppViewModel, items: List<ItemEntity>, onDismiss: () -> Unit) {
    val coffee = items.filter { it.category == "coffee" }
    var raw by remember { mutableStateOf(coffee.firstOrNull { it.name.contains("خام") } ?: coffee.firstOrNull()) }
    var roasted by remember { mutableStateOf(coffee.firstOrNull { !it.name.contains("خام") } ?: coffee.firstOrNull()) }
    var rawQty by remember { mutableStateOf("") }
    var roastedQty by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val rawBase = Math.round((rawQty.toDoubleOrNull() ?: 0.0) * 1000)
    val roastedBase = Math.round((roastedQty.toDoubleOrNull() ?: 0.0) * 1000)
    val loss = rawBase - roastedBase
    val costPerKg = if (roastedBase > 0 && raw != null) Math.round(raw!!.cost * rawBase * 1.0 / roastedBase) else 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل عملية تحميص") },
        text = {
            Column {
                DropdownPicker("البن الخام", coffee, raw, { it.name }) { raw = it }
                Spacer(Modifier.height(6.dp))
                DropdownPicker("الصنف المحمّص", coffee, roasted, { it.name }) { roasted = it }
                Spacer(Modifier.height(6.dp))
                AppTextField(rawQty, { rawQty = it }, "كمية الخام (كغ)", numeric = true)
                Spacer(Modifier.height(6.dp))
                AppTextField(roastedQty, { roastedQty = it }, "كمية المحمّص (كغ)", numeric = true)
                Spacer(Modifier.height(6.dp))
                AppTextField(notes, { notes = it }, "ملاحظة")
                if (rawBase > 0 && roastedBase > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("الفاقد: ${loss / 1000.0} كغ (${String.format("%.1f", loss * 100.0 / rawBase)}%)", fontSize = 11.sp, color = Danger)
                    Text("التكلفة الفعلية: ${Money.format(costPerKg, Money.SYP)} /كغ", fontSize = 11.sp, color = Gold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (raw != null && roasted != null && rawBase > 0 && roastedBase > 0) {
                    vm.addRoast(raw!!.id, roasted!!.id, rawBase, roastedBase, Dates.today(), notes)
                    onDismiss()
                }
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}
