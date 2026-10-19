package com.alharamayn.mahamess.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alharamayn.mahamess.AppViewModel
import com.alharamayn.mahamess.data.InvoiceWithLines
import com.alharamayn.mahamess.ui.ActionChip
import com.alharamayn.mahamess.ui.AppTextField
import com.alharamayn.mahamess.ui.Badge
import com.alharamayn.mahamess.ui.Brown
import com.alharamayn.mahamess.ui.BrownDark
import com.alharamayn.mahamess.ui.Danger
import com.alharamayn.mahamess.ui.Gold
import com.alharamayn.mahamess.ui.MoneyText
import com.alharamayn.mahamess.ui.Screen
import com.alharamayn.mahamess.ui.SectionCard
import com.alharamayn.mahamess.ui.Success
import com.alharamayn.mahamess.util.Analytics
import com.alharamayn.mahamess.util.Dates
import com.alharamayn.mahamess.util.Exporter
import com.alharamayn.mahamess.util.Money
import com.alharamayn.mahamess.util.ThermalPrinter
import kotlinx.coroutines.launch

@Composable
fun InvoicesScreen(vm: AppViewModel, navigate: (Screen) -> Unit) {
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val isManager = vm.isManager
    var search by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("all") }

    val all = Analytics.sales(invoices)
    val list = all.filter { inv ->
        (status == "all" || inv.invoice.status == status) &&
            (search.isBlank() || inv.invoice.no.contains(search) || inv.invoice.partyName.contains(search))
    }.sortedByDescending { it.invoice.createdAt }

    val syp = list.filter { it.invoice.status != "cancelled" && it.invoice.currency == Money.SYP }.sumOf { it.invoice.total }
    val usd = list.filter { it.invoice.status != "cancelled" && it.invoice.currency == Money.USD }.sumOf { it.invoice.total }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            SectionCard {
                AppTextField(search, { search = it }, "بحث برقم الفاتورة أو الزبون")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("الكل", { status = "all" }, filled = status == "all")
                    ActionChip("مكتملة", { status = "completed" }, filled = status == "completed")
                    ActionChip("ملغاة", { status = "cancelled" }, filled = status == "cancelled")
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { Badge("${list.size} فاتورة", Gold, BrownDark) }
                    Text("ل.س ${Money.formatBare(syp, Money.SYP)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("$ ${Money.formatBare(usd, Money.USD)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (list.isEmpty()) {
            item { SectionCard { Text("لا توجد فواتير", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        }

        list.forEach { inv ->
            item(key = inv.invoice.id) {
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth().clickable { navigate(Screen.InvoiceDetail(inv.invoice.id)) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(inv.invoice.no, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.width(6.dp))
                                Badge(
                                    if (inv.invoice.status == "cancelled") "ملغاة" else "مكتملة",
                                    if (inv.invoice.status == "cancelled") Danger else Success,
                                    if (inv.invoice.status == "cancelled") Danger else Success,
                                )
                            }
                            Text("${inv.invoice.partyName} • ${Dates.format(inv.invoice.date)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${inv.lines.size} بند • ${if (inv.invoice.currency == Money.SYP) "ليرة" else "دولار"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            MoneyText(inv.invoice.total, inv.invoice.currency, 14, true)
                            if (inv.invoice.remaining > 0) {
                                Text("متبقي ${Money.format(inv.invoice.remaining, inv.invoice.currency)}", fontSize = 9.sp, color = Danger)
                            }
                        }
                    }
                    if (isManager && inv.invoice.status != "cancelled") {
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            ActionChip("تعديل", { navigate(Screen.EditInvoice(inv.invoice.id)) })
                            Spacer(Modifier.width(6.dp))
                            ActionChip("عرض", { navigate(Screen.InvoiceDetail(inv.invoice.id)) })
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
fun InvoiceDetailScreen(vm: AppViewModel, id: Long, navigate: (Screen) -> Unit, onBack: () -> Unit) {
    val invoices by vm.invoices.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val customers by vm.customers.collectAsStateWithLifecycle()
    val invoice = invoices.find { it.invoice.id == id }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isManager = vm.isManager
    var showPrint by remember { mutableStateOf(false) }
    var showCancel by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showPrint = true else vm.notify("يجب السماح بالوصول إلى البلوتوث للطباعة")
    }

    if (invoice == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("الفاتورة غير موجودة") }
        return
    }
    val inv = invoice.invoice
    val customer = customers.find { it.id == inv.partyId }

    fun shareText(whatsapp: Boolean) {
        val text = buildInvoiceText(inv, invoice.lines, settings.shopName, settings.phone, settings.invoiceFooter)
        if (whatsapp) Exporter.whatsapp(context, customer?.phone ?: "", text)
        else Exporter.shareText(context, text)
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("← رجوع", onBack)
                    ActionChip("واتساب", { shareText(true) }, filled = true)
                    ActionChip("نص", { shareText(false) })
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("📄 PDF", {
                        val file = Exporter.invoicePdf(context, inv, invoice.lines, settings)
                        Exporter.shareFile(context, file, "application/pdf", "فاتورة ${inv.no}")
                    })
                    ActionChip("🖨 حرارية", {
                        if (ThermalPrinter.requiresPermission(context)) {
                            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else if (ThermalPrinter.bondedDevices(context).isEmpty()) {
                            vm.notify("لا توجد طابعة مقترنة — اقرن الطابعة من إعدادات البلوتوث أولاً")
                        } else showPrint = true
                    })
                    if (isManager && inv.status != "cancelled") {
                        ActionChip("تعديل", { navigate(Screen.EditInvoice(inv.id)) })
                        ActionChip("إلغاء", { showCancel = true })
                    }
                }
            }
        }

        item {
            // ورقة الفاتورة
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE0D7C8), RoundedCornerShape(14.dp))
                    .padding(16.dp),
            ) {
                Text(settings.shopName, color = Brown, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text(
                    "${settings.branch} — ${settings.address}",
                    color = Color(0xFF8D6E63), fontSize = 9.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
                if (settings.phone.isNotBlank()) {
                    Text(settings.phone, color = Color(0xFF8D6E63), fontSize = 9.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).background(Gold))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("فاتورة رقم: ${inv.no}", color = Brown, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("التاريخ: ${Dates.format(inv.date)}", color = Color(0xFF6D4C41), fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(inv.partyName, color = Brown, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(Money.name(inv.currency), color = Color(0xFF6D4C41), fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().background(Color(0xFFFFF3CD)).padding(vertical = 5.dp)) {
                    Text("الصنف", Modifier.weight(1.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Brown, textAlign = TextAlign.Right)
                    Text("الكمية", Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Brown, textAlign = TextAlign.Center)
                    Text("السعر", Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Brown, textAlign = TextAlign.Center)
                    Text("المجموع", Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Brown, textAlign = TextAlign.Left)
                }
                invoice.lines.forEach { line ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Text(line.name, Modifier.weight(1.6f), fontSize = 10.sp, color = Color(0xFF3E2723))
                        Text("${line.qtyMilli / 1000.0} ${com.alharamayn.mahamess.data.Units.short(line.unit)}", Modifier.weight(1f), fontSize = 10.sp, color = Color(0xFF3E2723), textAlign = TextAlign.Center)
                        Text(Money.formatBare(line.unitPrice, inv.currency), Modifier.weight(1f), fontSize = 10.sp, color = Color(0xFF3E2723), textAlign = TextAlign.Center)
                        val total = Math.round(line.qtyMilli * line.unitPrice / 1000.0) - line.discount
                        Text(Money.formatBare(total, inv.currency), Modifier.weight(1f), fontSize = 10.sp, color = Color(0xFF3E2723), textAlign = TextAlign.Left)
                    }
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFE8DFD0)))
                }
                Spacer(Modifier.height(8.dp))
                PaperRow("المجموع الفرعي", Money.format(inv.subtotal, inv.currency))
                if (inv.discount > 0) PaperRow("الخصم", "- " + Money.format(inv.discount, inv.currency), Danger)
                Box(Modifier.fillMaxWidth().height(1.dp).background(Gold))
                Spacer(Modifier.height(4.dp))
                PaperRow("الإجمالي", Money.format(inv.total, inv.currency), Brown, 14)
                PaperRow("المدفوع", Money.format(inv.paid, inv.currency))
                PaperRow("المتبقي", Money.format(inv.remaining, inv.currency), Brown)
                Spacer(Modifier.height(8.dp))
                Text(
                    settings.invoiceFooter,
                    color = Color(0xFF8D6E63), fontSize = 9.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
                if (inv.status == "cancelled") {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "ملغاة — ${inv.cancelReason ?: ""}",
                        color = Danger, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (isManager) {
            item {
                SectionCard(title = "تفاصيل للمدير") {
                    DetailRow("التكلفة", Money.format(inv.cost, inv.currency))
                    DetailRow("الربح", Money.format(inv.profit, inv.currency), Success)
                    DetailRow("سعر الصرف المسجل", Money.formatRate(inv.rate) + " ل.س")
                    DetailRow("أنشأها", inv.createdBy)
                    DetailRow("الحالة", if (inv.status == "cancelled") "ملغاة" else "مكتملة")
                    if (customer != null) DetailRow("هاتف الزبون", customer.phone)
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    if (showPrint) {
        val devices = ThermalPrinter.bondedDevices(context)
        AlertDialog(
            onDismissRequest = { showPrint = false },
            title = { Text("اختر الطابعة الحرارية") },
            text = {
                Column {
                    if (devices.isEmpty()) Text("لا توجد أجهزة مقترنة", fontSize = 12.sp)
                    devices.forEach { d ->
                        Text(
                            d.name ?: d.address,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().clickable {
                                showPrint = false
                                scope.launch {
                                    val bmp = ThermalPrinter.renderReceipt(inv, invoice.lines, settings)
                                    vm.notify(ThermalPrinter.print(context, d, bmp))
                                }
                            }.padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPrint = false }) { Text("إغلاق") } },
        )
    }

    if (showCancel) {
        AlertDialog(
            onDismissRequest = { showCancel = false },
            title = { Text("إلغاء الفاتورة") },
            text = {
                Column {
                    Text("سيتم إرجاع الكميات إلى المخزون وتبقى الفاتورة في السجل.", fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    AppTextField(cancelReason, { cancelReason = it }, "سبب الإلغاء")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.cancelInvoice(invoice, cancelReason) { showCancel = false }
                }) { Text("تأكيد الإلغاء") }
            },
            dismissButton = { TextButton(onClick = { showCancel = false }) { Text("رجوع") } },
        )
    }
}

@Composable
private fun PaperRow(label: String, value: String, color: Color = Color(0xFF3E2723), size: Int = 11) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = size.sp, color = color)
        Text(value, fontSize = size.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

fun buildInvoiceText(
    inv: com.alharamayn.mahamess.data.InvoiceEntity,
    lines: List<com.alharamayn.mahamess.data.InvoiceLineEntity>,
    shopName: String,
    phone: String,
    footer: String,
): String {
    val sb = StringBuilder()
    sb.appendLine("*$shopName*")
    sb.appendLine("فاتورة رقم: ${inv.no}")
    sb.appendLine("التاريخ: ${Dates.format(inv.date)}")
    sb.appendLine("الزبون: ${inv.partyName}")
    sb.appendLine()
    lines.forEach { l ->
        val total = Math.round(l.qtyMilli * l.unitPrice / 1000.0) - l.discount
        sb.appendLine("• ${l.name} — ${l.qtyMilli / 1000.0} ${com.alharamayn.mahamess.data.Units.short(l.unit)} × ${Money.format(l.unitPrice, inv.currency)} = ${Money.format(total, inv.currency)}")
    }
    sb.appendLine()
    sb.appendLine("المجموع: ${Money.format(inv.subtotal, inv.currency)}")
    if (inv.discount > 0) sb.appendLine("الخصم: ${Money.format(inv.discount, inv.currency)}")
    sb.appendLine("*الإجمالي: ${Money.format(inv.total, inv.currency)}*")
    sb.appendLine("المدفوع: ${Money.format(inv.paid, inv.currency)}")
    sb.appendLine("المتبقي: ${Money.format(inv.remaining, inv.currency)}")
    if (inv.currency == Money.USD) sb.appendLine("سعر الصرف: ${Money.formatRate(inv.rate)} ل.س")
    sb.appendLine()
    sb.appendLine(footer)
    if (phone.isNotBlank()) sb.appendLine("هاتف: $phone")
    return sb.toString()
}
