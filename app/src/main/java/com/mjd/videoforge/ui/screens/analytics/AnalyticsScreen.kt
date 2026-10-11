package com.mjd.videoforge.ui.screens.analytics

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
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التحليلات والاستهلاك", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
            // Summary cards
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("الفيديوهات", "١٢", Icons.Default.Videocam, Modifier.weight(1f))
                StatCard("التكلفة", "$٤.٢٣", Icons.Default.AttachMoney, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("الوقت الكلي", "٣.٥ ساعة", Icons.Default.Timer, Modifier.weight(1f))
                StatCard("المحركات", "٧ نشطة", Icons.Default.Memory, Modifier.weight(1f))
            }

            // Usage by provider
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("استهلاك المحركات اليوم", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    ProviderUsageRow("Gemini", 23, 50, MaterialTheme.colorScheme.primary)
                    ProviderUsageRow("DeepSeek", 15, 50, MaterialTheme.colorScheme.secondary)
                    ProviderUsageRow("Claude", 5, 100, MaterialTheme.colorScheme.tertiary)
                    ProviderUsageRow("Groq", 50, 50, MaterialTheme.colorScheme.error)
                    ProviderUsageRow("Stability", 8, 20, MaterialTheme.colorScheme.primary)
                }
            }

            // Cost breakdown
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("تفصيل التكلفة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    CostRow("توليد النص (سكربت)", "$0.15")
                    CostRow("توليد الصور", "$1.80")
                    CostRow("التعليق الصوتي", "$1.20")
                    CostRow("توليد الفيديو", "$0.80")
                    CostRow("الموسيقى", "$0.28")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    CostRow("المجموع", "$4.23", bold = true)
                }
            }

            // Tips
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Lightbulb, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("نصيحة لتوفير التكلفة", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "تفعيل وضع «المجاني أولاً» يقلل التكلفة بنسبة ٨٠٪ باستخدام DeepSeek و Groq بدلاً من GPT و Claude",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ProviderUsageRow(name: String, used: Int, total: Int, color: androidx.compose.ui.graphics.Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.bodyMedium)
            Text("$used/$total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { used.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
            color = color
        )
    }
}

@Composable
fun CostRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = if (bold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium)
        Text(value, style = if (bold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium)
    }
}
