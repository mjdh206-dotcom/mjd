package com.alharamayn.mahamess.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.alharamayn.mahamess.data.ItemEntity
import com.alharamayn.mahamess.ui.ActionChip
import com.alharamayn.mahamess.ui.AppTextField
import com.alharamayn.mahamess.ui.Badge
import com.alharamayn.mahamess.ui.Danger
import com.alharamayn.mahamess.ui.DropdownPicker
import com.alharamayn.mahamess.ui.MoneyText
import com.alharamayn.mahamess.ui.SectionCard
import com.alharamayn.mahamess.ui.Success
import com.alharamayn.mahamess.util.Dates
import com.alharamayn.mahamess.util.Money

private val CATS = listOf(
    "nuts" to "🥜 مكسرات",
    "hospitality" to "🍽️ ضيافة",
    "candy" to "🍬 كاندي",
    "coffee" to "☕ قهوة",
)

@Composable
fun ItemsScreen(vm: AppViewModel) {
    val items by vm.items.collectAsStateWithLifecycle()
    val isManager = vm.isManager
    var category by remember { mutableStateOf("all") }
    var search by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<ItemEntity?>(null) }
    var deleting by remember { mutableStateOf<ItemEntity?>(null) }

    val list = items.filter {
        (category == "all" || it.category == category) &&
            (search.isBlank() || it.name.contains(search) || it.barcode.contains(search))
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            SectionCard {
                AppTextField(search, { search = it }, "بحث بالاسم أو الباركود")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("الكل", { category = "all" }, filled = category == "all")
                    CATS.forEach { (id, label) ->
                        ActionChip(label, { category = id }, filled = category == id)
                    }
                }
                Spacer(Modifier.height(8.dp))
                ActionChip("＋ صنف جديد", { editing = ItemEntity(name = "", category = "nuts", unit = "kg", retail = 0, wholesale = 0, cost = 0, stock = 0, minStock = 0) }, filled = true)
            }
        }

        if (list.isEmpty()) {
            item { SectionCard { Text("لا توجد أصناف", fontSize = 12.sp) } }
        }

        list.forEach { item ->
            this.item(key = item.id) {
                SectionCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                if (item.isBasket) { Spacer(Modifier.width(5.dp)); Badge("سلة") }
                            }
                            Text(
                                CATS.firstOrNull { it.first == item.category }?.second ?: "",
                                fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(3.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MoneyText(item.retail, Money.SYP, 11, true)
                                Text("/ ${if (item.unit == "kg") "كغ" else "قطعة"}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val low = item.minStock > 0 && item.stock <= item.minStock
                            Badge(
                                if (item.unit == "kg") "${item.stock / 1000.0} كغ" else "${item.stock} قطعة",
                                if (low) Danger else Success,
                                if (low) Danger else Success,
                            )
                            if (item.expiry != null) {
                                Text("صلاحية ${Dates.format(item.expiry)}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionChip("تعديل", { editing = item })
                        if (isManager) ActionChip("حذف", { deleting = item })
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    editing?.let { item ->
        ItemEditorDialog(
            initial = item,
            allItems = items.filter { !it.isBasket },
            onDismiss = { editing = null },
            onSave = { vm.saveItem(it) { editing = null } },
            parseComponents = { vm.repo.parseComponents(it) },
            buildComponents = { vm.repo.componentsJson(it) },
        )
    }

    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("حذف الصنف") },
            text = { Text("هل تريد حذف «${item.name}»؟") },
            confirmButton = { TextButton(onClick = { vm.deleteItem(item); deleting = null }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("إلغاء") } },
        )
    }
}

@Composable
private fun ItemEditorDialog(
    initial: ItemEntity,
    allItems: List<ItemEntity>,
    onDismiss: () -> Unit,
    onSave: (ItemEntity) -> Unit,
    parseComponents: (String) -> List<Pair<Long, Long>>,
    buildComponents: (List<Pair<Long, Long>>) -> String,
) {
    var name by remember { mutableStateOf(initial.name) }
    var category by remember { mutableStateOf(initial.category) }
    var unit by remember { mutableStateOf(initial.unit) }
    var retail by remember { mutableStateOf(if (initial.retail > 0) Money.formatBare(initial.retail, Money.SYP) else "") }
    var wholesale by remember { mutableStateOf(if (initial.wholesale > 0) Money.formatBare(initial.wholesale, Money.SYP) else "") }
    var cost by remember { mutableStateOf(if (initial.cost > 0) Money.formatBare(initial.cost, Money.SYP) else "") }
    var stock by remember {
        mutableStateOf(
            if (initial.unit == "kg") (initial.stock / 1000.0).toString() else initial.stock.toString(),
        )
    }
    var minStock by remember {
        mutableStateOf(
            if (initial.unit == "kg") (initial.minStock / 1000.0).toString() else initial.minStock.toString(),
        )
    }
    var barcode by remember { mutableStateOf(initial.barcode) }
    var expiry by remember { mutableStateOf(initial.expiry ?: "") }
    var isBasket by remember { mutableStateOf(initial.isBasket) }
    val components = remember { mutableStateListOf<Pair<Long, Long>>().apply { addAll(parseComponents(initial.componentsJson)) } }
    var compPicker by remember { mutableStateOf<ItemEntity?>(null) }
    var compQty by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "صنف جديد" else "تعديل صنف", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AppTextField(name, { name = it }, "اسم الصنف")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    CATS.forEach { (id, label) ->
                        ActionChip(label.substringAfter(" "), { category = id }, filled = category == id)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ActionChip("كيلو", { unit = "kg" }, filled = unit == "kg")
                    ActionChip("قطعة", { unit = "piece" }, filled = unit == "piece")
                }
                Spacer(Modifier.height(6.dp))
                AppTextField(retail, { retail = it }, "سعر المفرق للـ${if (unit == "kg") "كغ" else "قطعة"}", numeric = true)
                Spacer(Modifier.height(6.dp))
                AppTextField(wholesale, { wholesale = it }, "سعر الجملة", numeric = true)
                Spacer(Modifier.height(6.dp))
                AppTextField(cost, { cost = it }, "سعر التكلفة", numeric = true)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.weight(1f)) { AppTextField(stock, { stock = it }, "المخزون", numeric = true) }
                    Box(Modifier.weight(1f)) { AppTextField(minStock, { minStock = it }, "حد التنبيه", numeric = true) }
                }
                Spacer(Modifier.height(6.dp))
                AppTextField(barcode, { barcode = it }, "الباركود")
                Spacer(Modifier.height(6.dp))
                AppTextField(expiry, { expiry = it }, "تاريخ الصلاحية (2026-12-31)")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("سلة ضيافة (تجميع أصناف بسعر واحد)", fontSize = 11.sp)
                    androidx.compose.material3.Switch(checked = isBasket, onCheckedChange = { isBasket = it })
                }
                if (isBasket) {
                    Spacer(Modifier.height(6.dp))
                    components.forEachIndexed { index, pair ->
                        val compItem = allItems.find { it.id == pair.first }
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(compItem?.name ?: "صنف", Modifier.weight(1f), fontSize = 11.sp)
                            Text("${pair.second / 1000.0} كغ", fontSize = 10.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("✕", fontSize = 12.sp, color = Danger, modifier = Modifier.clickable { components.removeAt(index) })
                        }
                    }
                    DropdownPicker(
                        label = "إضافة مكوّن",
                        items = allItems,
                        selected = compPicker,
                        itemLabel = { it.name },
                        onSelect = { compPicker = it },
                    )
                    if (compPicker != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { AppTextField(compQty, { compQty = it }, "الكمية (كغ)", numeric = true) }
                            Spacer(Modifier.width(6.dp))
                            ActionChip("إضافة", {
                                val qty = Math.round((compQty.toDoubleOrNull() ?: 0.0) * 1000)
                                if (qty > 0 && compPicker != null) {
                                    components.add(compPicker!!.id to qty)
                                    compPicker = null
                                    compQty = "1"
                                }
                            }, filled = true)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) return@TextButton
                val factor = if (unit == "kg") 1000.0 else 1.0
                onSave(
                    initial.copy(
                        name = name.trim(),
                        category = category,
                        unit = unit,
                        retail = Money.parse(retail, Money.SYP),
                        wholesale = Money.parse(wholesale, Money.SYP),
                        cost = Money.parse(cost, Money.SYP),
                        stock = Math.round((stock.toDoubleOrNull() ?: 0.0) * factor),
                        minStock = Math.round((minStock.toDoubleOrNull() ?: 0.0) * factor),
                        barcode = barcode.trim(),
                        expiry = expiry.ifBlank { null },
                        isBasket = isBasket,
                        componentsJson = if (isBasket) buildComponents(components.toList()) else "",
                    )
                )
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}
