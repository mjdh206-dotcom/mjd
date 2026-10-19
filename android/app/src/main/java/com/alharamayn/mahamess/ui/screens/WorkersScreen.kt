package com.alharamayn.mahamess.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alharamayn.mahamess.AppViewModel
import com.alharamayn.mahamess.data.WorkerEntity
import com.alharamayn.mahamess.data.WorkerTxEntity
import com.alharamayn.mahamess.ui.ActionChip
import com.alharamayn.mahamess.ui.AppTextField
import com.alharamayn.mahamess.ui.Badge
import com.alharamayn.mahamess.ui.Danger
import com.alharamayn.mahamess.ui.MoneyText
import com.alharamayn.mahamess.ui.SectionCard
import com.alharamayn.mahamess.ui.StatCard
import com.alharamayn.mahamess.ui.Success
import com.alharamayn.mahamess.util.Dates
import com.alharamayn.mahamess.util.Exporter
import com.alharamayn.mahamess.util.Money

@Composable
fun WorkersScreen(vm: AppViewModel) {
    val workers by vm.workers.collectAsStateWithLifecycle()
    val workerTx by vm.workerTx.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editing by remember { mutableStateOf<WorkerEntity?>(null) }
    var drawFor by remember { mutableStateOf<WorkerEntity?>(null) }
    var statementFor by remember { mutableStateOf<WorkerEntity?>(null) }

    val totalAdvances = workerTx.filter { it.currency == Money.SYP }.sumOf { it.amount }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("👷", "عدد العمال", "${workers.size}", "عامل", Modifier.weight(1f))
                StatCard("💰", "إجمالي السحوبات", Money.format(totalAdvances, Money.SYP), "ل.س", Modifier.weight(1f))
            }
        }
        item {
            SectionCard {
                Text("السحوبات تشمل السلف والبضاعة المسحوبة من المحل", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                ActionChip("＋ عامل جديد", { editing = WorkerEntity(name = "", salary = 0) }, filled = true)
            }
        }
        workers.forEach { worker ->
            item(key = worker.id) {
                val txs = workerTx.filter { it.workerId == worker.id }
                val advances = txs.filter { it.currency == worker.currency }.sumOf { it.amount }
                val net = worker.salary - advances
                SectionCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(worker.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(worker.phone.ifBlank { "بدون هاتف" }, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("الراتب: ${Money.format(worker.salary, worker.currency)}", fontSize = 10.sp)
                                Text("السحوبات: ${Money.format(advances, worker.currency)}", fontSize = 10.sp, color = Danger)
                            }
                            Spacer(Modifier.height(2.dp))
                            Row {
                                Text("الصافي: ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                MoneyText(net, worker.currency, 12, true, Success)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            ActionChip("＋ سحب", { drawFor = worker }, filled = true)
                            Spacer(Modifier.height(5.dp))
                            ActionChip("كشف حساب", { statementFor = worker })
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    ActionChip("تعديل", { editing = worker })
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    editing?.let { worker ->
        var name by remember { mutableStateOf(worker.name) }
        var phone by remember { mutableStateOf(worker.phone) }
        var salary by remember { mutableStateOf(if (worker.salary > 0) Money.formatBare(worker.salary, worker.currency) else "") }
        var currency by remember { mutableStateOf(worker.currency) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (worker.id == 0L) "عامل جديد" else "تعديل عامل") },
            text = {
                Column {
                    AppTextField(name, { name = it }, "الاسم")
                    Spacer(Modifier.height(6.dp))
                    AppTextField(phone, { phone = it }, "الهاتف")
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionChip("ليرة", { currency = Money.SYP }, filled = currency == Money.SYP)
                        ActionChip("دولار", { currency = Money.USD }, filled = currency == Money.USD)
                    }
                    Spacer(Modifier.height(6.dp))
                    AppTextField(salary, { salary = it }, "الراتب الشهري", numeric = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        vm.saveWorker(worker.copy(name = name, phone = phone, salary = Money.parse(salary, currency), currency = currency))
                        editing = null
                    }
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("إلغاء") } },
        )
    }

    drawFor?.let { worker ->
        var type by remember { mutableStateOf("advance") }
        var amount by remember { mutableStateOf("") }
        var currency by remember { mutableStateOf(worker.currency) }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { drawFor = null },
            title = { Text("سحب عامل — ${worker.name}") },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionChip("سلفة نقدية", { type = "advance" }, filled = type == "advance")
                        ActionChip("بضاعة", { type = "goods" }, filled = type == "goods")
                    }
                    Spacer(Modifier.height(6.dp))
                    AppTextField(amount, { amount = it }, "المبلغ", numeric = true)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionChip("ليرة", { currency = Money.SYP }, filled = currency == Money.SYP)
                        ActionChip("دولار", { currency = Money.USD }, filled = currency == Money.USD)
                    }
                    Spacer(Modifier.height(6.dp))
                    AppTextField(note, { note = it }, "ملاحظة")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = Money.parse(amount, currency)
                    if (value > 0) {
                        vm.addWorkerTx(
                            WorkerTxEntity(
                                workerId = worker.id, date = Dates.today(), type = type,
                                amount = value, currency = currency, note = note,
                            )
                        )
                        drawFor = null
                    }
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { drawFor = null }) { Text("إلغاء") } },
        )
    }

    statementFor?.let { worker ->
        val txs = workerTx.filter { it.workerId == worker.id }.sortedByDescending { it.date }
        val advances = txs.filter { it.currency == worker.currency }.sumOf { it.amount }
        AlertDialog(
            onDismissRequest = { statementFor = null },
            title = { Text("كشف حساب — ${worker.name}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الراتب", fontSize = 11.sp); Text(Money.format(worker.salary, worker.currency), fontSize = 11.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("السحوبات", fontSize = 11.sp); Text(Money.format(advances, worker.currency), fontSize = 11.sp, color = Danger)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الصافي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(Money.format(worker.salary - advances, worker.currency), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Success)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (txs.isEmpty()) Text("لا توجد سحوبات", fontSize = 11.sp)
                    txs.forEach { t ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(if (t.type == "advance") "سلفة" else "بضاعة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("${Dates.format(t.date)} ${t.note}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            MoneyText(t.amount, t.currency, 11)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val sb = StringBuilder()
                    sb.appendLine("*${settings.shopName}*")
                    sb.appendLine("كشف حساب العامل: ${worker.name}")
                    sb.appendLine("الراتب: ${Money.format(worker.salary, worker.currency)}")
                    txs.forEach { t ->
                        sb.appendLine("${Dates.format(t.date)} | ${if (t.type == "advance") "سلفة" else "بضاعة"} | ${Money.format(t.amount, t.currency)}")
                    }
                    sb.appendLine("إجمالي السحوبات: ${Money.format(advances, worker.currency)}")
                    sb.appendLine("*صافي الراتب: ${Money.format(worker.salary - advances, worker.currency)}*")
                    Exporter.whatsapp(context, worker.phone, sb.toString())
                }) { Text("واتساب") }
            },
            dismissButton = { TextButton(onClick = { statementFor = null }) { Text("إغلاق") } },
        )
    }
}
