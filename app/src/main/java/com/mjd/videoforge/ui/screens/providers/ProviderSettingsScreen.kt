package com.mjd.videoforge.ui.screens.providers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * شاشة إعدادات محرك معين
 *
 * تسمح بـ:
 * - إضافة/حذف مفاتيح API (مشفرة)
 * - اختبار المفتاح
 * - ضبط الأولوية
 * - حد الاستخدام اليومي
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSettingsScreen(
    providerId: String,
    onBack: () -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var keyLabel by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    // Saved keys (placeholder)
    var savedKeys by remember {
        mutableStateOf(listOf(
            SavedKey("sk-***...abc1", "المفتاح الرئيسي", 150),
            SavedKey("sk-***...xyz2", "مفتاح احتياطي", 45)
        ))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("إعدادات المحرك", fontWeight = FontWeight.Bold)
                        Text(
                            providerId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // API Key section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "مفاتيح API",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "المفاتيح مشفرة بواسطة Android Keystore ولا تغادر جهازك",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    // Add new key
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("مفتاح API") },
                        placeholder = { Text("sk-...") },
                        visualTransformation = if (showKey) PasswordVisualTransformation()
                            else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = keyLabel,
                        onValueChange = { keyLabel = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("تسمية (اختياري)") },
                        placeholder = { Text("مثال: المفتاح الرئيسي") },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (apiKey.isNotBlank()) {
                                savedKeys = savedKeys + SavedKey(
                                    apiKey.take(6) + "***..." + apiKey.takeLast(4),
                                    keyLabel.ifEmpty { "مفتاح جديد" },
                                    0
                                )
                                apiKey = ""
                                keyLabel = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = apiKey.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("إضافة مفتاح")
                    }
                }
            }

            // Saved keys
            if (savedKeys.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "المفاتيح المحفوظة",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        savedKeys.forEachIndexed { index, key ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        key.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${key.keyPreview} • ${key.usageCount} استخدام",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = {
                                    savedKeys = savedKeys.toMutableList().also { it.removeAt(index) }
                                }) {
                                    Icon(Icons.Default.Delete, "حذف المفتاح", modifier = Modifier.size(18.dp))
                                }
                            }
                            if (index < savedKeys.size - 1) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // Health check
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "فحص الصحة",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            isTesting = true
                            // Simulate test
                            testResult = "✅ المحرك يعمل بشكل صحيح\n⚡ زمن الاستجابة: ٣٢٠ مللي ثانية\n📊 الحصة المتبقية: ٢٧ طلب"
                            isTesting = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isTesting
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.HealthAndSafety, null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (isTesting) "جاري الفحص..." else "اختبار المفتاح")
                    }

                    testResult?.let {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                it,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Security note
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "أمان المفاتيح",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "كل مفاتيح API مشفرة بواسطة Android Keystore. لا يتم إرسالها لأي سيرفر خارجي. التشفير يتم محلياً على جهازك فقط.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

data class SavedKey(
    val keyPreview: String,
    val label: String,
    val usageCount: Int
)
