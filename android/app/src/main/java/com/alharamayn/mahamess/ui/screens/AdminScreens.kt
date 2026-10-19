package com.alharamayn.mahamess.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.alharamayn.mahamess.data.UserEntity
import com.alharamayn.mahamess.ui.ActionChip
import com.alharamayn.mahamess.ui.AppTextField
import com.alharamayn.mahamess.ui.Badge
import com.alharamayn.mahamess.ui.Danger
import com.alharamayn.mahamess.ui.SectionCard
import com.alharamayn.mahamess.ui.Success
import com.alharamayn.mahamess.util.Dates
import com.alharamayn.mahamess.util.Exporter
import com.alharamayn.mahamess.util.Money

@Composable
fun UsersScreen(vm: AppViewModel) {
    val users by vm.users.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<UserEntity?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            SectionCard {
                Text("المدير: كل الصلاحيات + الأرباح + التقارير", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("العامل: فواتير فقط بدون أرباح أو تقارير", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                ActionChip("＋ مستخدم", { editing = UserEntity(name = "", role = "worker", pin = "") }, filled = true)
            }
        }
        users.forEach { user ->
            item(key = user.id) {
                SectionCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(user.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(5.dp))
                                Badge(
                                    if (user.role == "manager") "مدير" else "عامل",
                                    if (user.role == "manager") com.alharamayn.mahamess.ui.Gold else MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Text(
                                if (user.role == "manager") "كل الصلاحيات" else "فواتير فقط",
                                fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text("PIN: ${"•".repeat(user.pin.length)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ActionChip("تعديل", { editing = user })
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    editing?.let { user ->
        var name by remember { mutableStateOf(user.name) }
        var role by remember { mutableStateOf(user.role) }
        var pin by remember { mutableStateOf(user.pin) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (user.id == 0L) "مستخدم جديد" else "تعديل مستخدم") },
            text = {
                Column {
                    AppTextField(name, { name = it }, "الاسم")
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionChip("مدير", { role = "manager" }, filled = role == "manager")
                        ActionChip("عامل", { role = "worker" }, filled = role == "worker")
                    }
                    Spacer(Modifier.height(6.dp))
                    AppTextField(pin, { pin = it.filter { ch -> ch.isDigit() } }, "الرقم السري (4 أرقام)")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank() && pin.length >= 3) {
                        vm.saveUser(user.copy(name = name, role = role, pin = pin))
                        editing = null
                    }
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("إلغاء") } },
        )
    }
}

@Composable
fun AuditScreen(vm: AppViewModel) {
    val audit by vm.audit.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            SectionCard {
                Text("كل إنشاء أو تعديل أو إلغاء يُسجَّل تلقائياً مع المستخدم والتاريخ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        audit.forEach { entry ->
            item(key = entry.id) {
                SectionCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Badge(
                                    entry.action,
                                    when (entry.action) {
                                        "حذف", "إلغاء" -> Danger
                                        "تعديل" -> com.alharamayn.mahamess.ui.Gold
                                        else -> Success
                                    },
                                    MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("${entry.entity} ${entry.entityId}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            if (entry.details.isNotBlank()) {
                                Text(entry.details, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(Dates.formatMillis(entry.at), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("بواسطة ${entry.user}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dark = settings.theme == "dark"

    var shopName by remember(settings.shopName) { mutableStateOf(settings.shopName) }
    var branch by remember(settings.branch) { mutableStateOf(settings.branch) }
    var phone by remember(settings.phone) { mutableStateOf(settings.phone) }
    var address by remember(settings.address) { mutableStateOf(settings.address) }
    var footer by remember(settings.invoiceFooter) { mutableStateOf(settings.invoiceFooter) }
    var ozGrams by remember(settings.ozGrams) { mutableStateOf(settings.ozGrams.toString()) }
    var rate by remember(settings.rate) { mutableStateOf(Money.formatRate(settings.rate).replace(",", "")) }
    var managerPin by remember(settings.managerPin) { mutableStateOf(settings.managerPin) }
    var showDemoConfirm by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (text != null) vm.importBackup(text) { }
            } catch (e: Exception) {
                vm.notify("تعذر قراءة الملف")
            }
        }
    }

    fun saveAll() {
        vm.updateSettings {
            it.copy(
                shopName = shopName.ifBlank { "محامص الحرمين" },
                branch = branch,
                phone = phone,
                address = address,
                invoiceFooter = footer,
                ozGrams = ozGrams.toIntOrNull() ?: 250,
                rate = Money.rateFromInput(rate).takeIf { r -> r > 0 } ?: it.rate,
                managerPin = managerPin.ifBlank { "1234" },
            )
        }
        vm.notify("تم حفظ الإعدادات")
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        item {
            SectionCard(title = "بيانات المحل", badge = "تظهر على كل شاشة وفاتورة") {
                AppTextField(shopName, { shopName = it }, "اسم المحل")
                Spacer(Modifier.height(6.dp))
                AppTextField(branch, { branch = it }, "الفرع")
                Spacer(Modifier.height(6.dp))
                AppTextField(phone, { phone = it }, "الهاتف")
                Spacer(Modifier.height(6.dp))
                AppTextField(address, { address = it }, "العنوان")
                Spacer(Modifier.height(6.dp))
                AppTextField(footer, { footer = it }, "عبارة أسفل الفاتورة")
                Spacer(Modifier.height(6.dp))
                AppTextField(ozGrams, { ozGrams = it.filter { c -> c.isDigit() } }, "وزن الأوقية بالغرام")
            }
        }

        item {
            SectionCard(title = "العملة وسعر الصرف") {
                AppTextField(rate, { rate = it }, "سعر صرف الدولار بالليرة")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("العملة الافتراضية: ليرة", {
                        vm.updateSettings { it.copy(defaultCurrency = Money.SYP) }
                    }, filled = settings.defaultCurrency == Money.SYP)
                    ActionChip("دولار", {
                        vm.updateSettings { it.copy(defaultCurrency = Money.USD) }
                    }, filled = settings.defaultCurrency == Money.USD)
                }
            }
        }

        item {
            SectionCard(title = "المظهر والأمان") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("الوضع الداكن", fontSize = 12.sp)
                    Switch(
                        checked = dark,
                        onCheckedChange = { vm.updateSettings { s -> s.copy(theme = if (it) "dark" else "light") } },
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("الرقم السري عند فتح التطبيق", fontSize = 12.sp)
                    Switch(
                        checked = settings.pinEnabled,
                        onCheckedChange = { vm.updateSettings { s -> s.copy(pinEnabled = it) } },
                    )
                }
                Spacer(Modifier.height(6.dp))
                AppTextField(managerPin, { managerPin = it.filter { c -> c.isDigit() } }, "الرقم السري للمدير")
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionChip("💾 حفظ الإعدادات", { saveAll() }, filled = true)
            }
        }

        item {
            SectionCard(title = "النسخ الاحتياطي") {
                Text(
                    "نسخة JSON كاملة من كل البيانات. النسخ التلقائي يتم يومياً داخل مساحة التطبيق.",
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "آخر نسخة: " + if (settings.lastBackup > 0) Dates.formatMillis(settings.lastBackup) else "لا يوجد",
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("📤 تصدير نسخة", {
                        vm.exportBackup { json ->
                            val file = Exporter.writeText(context, "نسخة-محامص-الحرمين-${Dates.today()}.json", json)
                            Exporter.shareFile(context, file, "application/json", "نسخة احتياطية — محامص الحرمين")
                        }
                    })
                    ActionChip("📥 استعادة", { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) })
                }
                Spacer(Modifier.height(6.dp))
                Text("في أندرويد يمكن إرسال النسخة إلى Google Drive عبر تطبيق Drive من قائمة المشاركة.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (vm.isManager) {
            item {
                SectionCard(title = "بيانات تجريبية") {
                    Text("تحميل بيانات مثال (أصناف، فواتير، مصاريف) لتجربة التطبيق", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    ActionChip("تحميل بيانات تجريبية", { showDemoConfirm = true })
                }
            }
        }

        item {
            SectionCard(title = "عن التطبيق") {
                Text("محامص الحرمين — إدارة الفواتير والحسابات", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("الإصدار 1.0.0 • يعمل بدون إنترنت • قاعدة بيانات Room محلية", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("الأمان: الرقم السري، صلاحيات المدير/العامل، سجل عمليات كامل", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    if (showDemoConfirm) {
        AlertDialog(
            onDismissRequest = { showDemoConfirm = false },
            title = { Text("تحميل بيانات تجريبية") },
            text = { Text("سيتم إضافة أصناف وفواتير ومصاريف تجريبية إلى البيانات الحالية. هل تريد المتابعة؟") },
            confirmButton = {
                TextButton(onClick = { vm.loadDemo(); showDemoConfirm = false }) { Text("تحميل") }
            },
            dismissButton = { TextButton(onClick = { showDemoConfirm = false }) { Text("إلغاء") } },
        )
    }
}
