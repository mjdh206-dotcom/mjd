package com.mjd.videoforge.ui.screens.providers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * شاشة إدارة المحركات
 *
 * تعرض:
 * - حالة كل محرك (نشط / انتهت حصته / خطأ)
 * - الاستخدام اليومي
 * - ترتيب الأولوية (سحب وإفلات)
 * - زر فحص الصحة
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersScreen(
    onProviderSettings: (String) -> Unit,
    onBack: () -> Unit
) {
    var isHealthCheckRunning by remember { mutableStateOf(false) }

    // Placeholder data
    val providers = remember {
        listOf(
            ProviderItem("gemini", "Google Gemini", "🤖", ProviderItemStatus.ACTIVE, 23, 50, true, "نص"),
            ProviderItem("deepseek", "DeepSeek", "🔍", ProviderItemStatus.ACTIVE, 15, -1, true, "نص"),
            ProviderItem("groq", "Groq", "⚡", ProviderItemStatus.EXHAUSTED, 50, 50, true, "نص"),
            ProviderItem("claude", "Claude", "🧠", ProviderItemStatus.ACTIVE, 5, 100, false, "نص"),
            ProviderItem("gpt", "OpenAI GPT", "🔮", ProviderItemStatus.ACTIVE, 8, -1, false, "نص + صور"),
            ProviderItem("mistral", "Mistral", "🌀", ProviderItemStatus.ACTIVE, 10, -1, true, "نص"),
            ProviderItem("grok", "Grok", "🚀", ProviderItemStatus.ACTIVE, 3, 20, false, "نص"),
            ProviderItem("elevenlabs", "ElevenLabs", "🎙️", ProviderItemStatus.ACTIVE, 12000, 50000, false, "صوت"),
            ProviderItem("edge_tts", "Edge TTS", "🆓", ProviderItemStatus.ACTIVE, -1, -1, true, "صوت"),
            ProviderItem("stability", "Stable Diffusion", "🎨", ProviderItemStatus.ACTIVE, 30, 100, false, "صور"),
            ProviderItem("runway", "Runway", "🎬", ProviderItemStatus.COOLDOWN, 5, 10, false, "فيديو"),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("محركات الذكاء الاصطناعي", fontWeight = FontWeight.Bold)
                        Text(
                            "${providers.count { it.status == ProviderItemStatus.ACTIVE }} نشط من ${providers.size}",
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
        },
        bottomBar = {
            Surface(
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { isHealthCheckRunning = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isHealthCheckRunning
                    ) {
                        if (isHealthCheckRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("جاري الفحص...")
                        } else {
                            Icon(Icons.Default.HealthAndSafety, null)
                            Spacer(Modifier.width(8.dp))
                            Text("فحص الصحة")
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                // Mode selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "وضع التبديل",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = true,
                                onClick = { },
                                label = { Text("تلقائي") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = false,
                                onClick = { },
                                label = { Text("أعلى جودة") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = false,
                                onClick = { },
                                label = { Text("أقل تكلفة") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = true, onCheckedChange = null)
                            Text("المجاني أولاً", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            items(providers) { provider ->
                ProviderCard(
                    provider = provider,
                    onClick = { onProviderSettings(provider.id) }
                )
            }
        }
    }
}

@Composable
fun ProviderCard(
    provider: ProviderItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Text(provider.emoji, style = MaterialTheme.typography.headlineSmall)

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        provider.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (provider.isFree) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "مجاني",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                Text(
                    provider.tasks,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Usage bar
                if (provider.dailyLimit > 0) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { provider.dailyUsage.toFloat() / provider.dailyLimit },
                        modifier = Modifier.fillMaxWidth(),
                        color = when (provider.status) {
                            ProviderItemStatus.EXHAUSTED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Text(
                        "${provider.dailyUsage}/${if (provider.dailyLimit == -1) "∞" else provider.dailyLimit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status indicator
            Surface(
                color = when (provider.status) {
                    ProviderItemStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                    ProviderItemStatus.EXHAUSTED -> MaterialTheme.colorScheme.errorContainer
                    ProviderItemStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                    ProviderItemStatus.COOLDOWN -> MaterialTheme.colorScheme.tertiaryContainer
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    when (provider.status) {
                        ProviderItemStatus.ACTIVE -> "نشط"
                        ProviderItemStatus.EXHAUSTED -> "انتهت"
                        ProviderItemStatus.ERROR -> "خطأ"
                        ProviderItemStatus.COOLDOWN -> "تبريد"
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (provider.status) {
                        ProviderItemStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        }
    }
}

data class ProviderItem(
    val id: String,
    val name: String,
    val emoji: String,
    val status: ProviderItemStatus,
    val dailyUsage: Int,
    val dailyLimit: Int,
    val isFree: Boolean,
    val tasks: String
)

enum class ProviderItemStatus { ACTIVE, EXHAUSTED, ERROR, COOLDOWN }
