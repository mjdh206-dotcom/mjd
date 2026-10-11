package com.mjd.videoforge.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var darkMode by remember { mutableStateOf(false) }
    var autoSave by remember { mutableStateOf(true) }
    var dataSaver by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(true) }
    var qualityWarning by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General settings
            SettingsSection("عام") {
                SettingsToggle("الوضع الليلي", Icons.Default.DarkMode, darkMode) { darkMode = it }
                SettingsToggle("حفظ تلقائي للمسودات", Icons.Default.Save, autoSave) { autoSave = it }
                SettingsToggle("توفير البيانات", Icons.Default.DataSaverOn, dataSaver) { dataSaver = it }
                SettingsToggle("الإشعارات", Icons.Default.Notifications, notifications) { notifications = it }
                SettingsToggle("تحذير الجودة", Icons.Default.Warning, qualityWarning) { qualityWarning = it }
            }

            // Export settings
            SettingsSection("التصدير") {
                SettingsItem("جودة التصدير الافتراضية", "١٠٨٠p", Icons.Default.HighQuality)
                SettingsItem("صيغة الملف", "MP4", Icons.Default.InsertDriveFile)
                SettingsItem("حرق الترجمة", "نعم", Icons.Default.Subtitles)
            }

            // Backup
            SettingsSection("النسخ الاحتياطي") {
                SettingsItem("النسخ على Google Drive", "غير مُفعّل", Icons.Default.CloudUpload)
                SettingsItem("استعادة نسخة سابقة", "—", Icons.Default.CloudDownload)
            }

            // Tools
            SettingsSection("أدوات") {
                SettingsItem("تصدير سجل الأخطاء", "—", Icons.Default.BugReport)
                SettingsItem("مسح البيانات المؤقتة", "٢٣٤ ميجابايت", Icons.Default.CleaningServices)
            }

            // About
            SettingsSection("حول") {
                SettingsItem("الإصدار", "1.0.0", Icons.Default.Info)
                SettingsItem("المطور", "VideoForge", Icons.Default.DeveloperMode)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsToggle(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
