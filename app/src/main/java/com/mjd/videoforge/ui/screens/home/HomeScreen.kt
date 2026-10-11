package com.mjd.videoforge.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * الشاشة الرئيسية
 *
 * تعرض:
 * - حقل إدخال الفكرة (أو زر التسجيل الصوتي)
 * - أزرار التنقل الرئيسية
 * - القوالب الجاهزة
 * - المشاريع الأخيرة
 * - اقتراحات ذكية
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToQuestions: (String) -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBrand: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    var ideaText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مصنع الفيديو",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToQueue) {
                        Icon(Icons.Default.PlaylistPlay, contentDescription = "قائمة الانتظار")
                        Badge { Text("3") }
                    }
                    IconButton(onClick = onNavigateToProviders) {
                        Icon(Icons.Default.Memory, contentDescription = "المحركات")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات")
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
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Welcome section
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "حوّل فكرتك إلى فيديو ✨",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "اكتب فكرة أو موضوع، وسيقوم التطبيق بإنتاج فيديو جاهز",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Idea input card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Text input
                    OutlinedTextField(
                        value = ideaText,
                        onValueChange = { ideaText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = {
                            Text(
                                "اكتب فكرة الفيديو هنا...\nمثال: أغرب ٥ حيوانات في أعماق المحيط",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )

                    // Voice input button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { /* TODO: voice input */ },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("أو اضغط للتحدث")
                        }

                        // Generate button
                        Button(
                            onClick = {
                                if (ideaText.isNotBlank()) {
                                    val projectId = java.util.UUID.randomUUID().toString()
                                    onNavigateToQuestions(projectId)
                                }
                            },
                            enabled = ideaText.isNotBlank(),
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "ابدأ الإنتاج",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Quick templates
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قوالب جاهزة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { /* Navigate to all templates */ }) {
                        Text("عرض الكل")
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(templates) { template ->
                        TemplateCard(
                            template = template,
                            onClick = {
                                ideaText = template.prompt
                            }
                        )
                    }
                }
            }

            // Quick navigation cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "أدوات سريعة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Default.Palette,
                        label = "هوية القناة",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToBrand
                    )
                    QuickActionCard(
                        icon = Icons.Default.Analytics,
                        label = "التحليلات",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAnalytics
                    )
                    QuickActionCard(
                        icon = Icons.Default.Queue,
                        label = "قائمة الانتظار",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToQueue,
                        badge = "3"
                    )
                }
            }

            // Recent projects
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المشاريع الأخيرة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { /* Navigate to all projects */ }) {
                        Text("عرض الكل")
                    }
                }

                // Placeholder for recent projects
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            "لا توجد مشاريع سابقة",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            "ابدأ بكتابة فكرة لتوليد أول فيديو!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun TemplateCard(
    template: TemplateData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(template.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    template.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box {
                    Icon(
                        icon,
                        contentDescription = label,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    badge?.let {
                        Badge(
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) { Text(it) }
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Template data
data class TemplateData(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Brush,
    val prompt: String
)

val templates = listOf(
    TemplateData(
        name = "حقائق علمية",
        description = "معلومات مذهلة عن الكون والطبيعة",
        icon = Icons.Default.Science,
        color = Brush.linearGradient(
            listOf(
                androidx.compose.ui.graphics.Color(0xFF1B6B93),
                androidx.compose.ui.graphics.Color(0xFF2196F3)
            )
        ),
        prompt = "أغرب ٥ حقائق عن المحيط العميق"
    ),
    TemplateData(
        name = "تاريخ",
        description = "قصص وأحداث تاريخية مشوقة",
        icon = Icons.Default.History,
        color = Brush.linearGradient(
            listOf(
                androidx.compose.ui.graphics.Color(0xFF8B4513),
                androidx.compose.ui.graphics.Color(0xFFD2691E)
            )
        ),
        prompt = "قصة الحضارة السومرية وأول كتابة في التاريخ"
    ),
    TemplateData(
        name = "تحفيز",
        description = "كلمات ملهمة وقصص نجاح",
        icon = Icons.Default.Favorite,
        color = Brush.linearGradient(
            listOf(
                androidx.compose.ui.graphics.Color(0xFFE91E63),
                androidx.compose.ui.graphics.Color(0xFFFC9D4A)
            )
        ),
        prompt = "٥ دروس من حياة أديب عربي غيّر العالم"
    ),
    TemplateData(
        name = "تقنية",
        description = "آخر الأخبار والاكتشافات التقنية",
        icon = Icons.Default.Computer,
        color = Brush.linearGradient(
            listOf(
                androidx.compose.ui.graphics.Color(0xFF4CAF50),
                androidx.compose.ui.graphics.Color(0xFF00BCD4)
            )
        ),
        prompt = "كيف سيغير الذكاء الاصطناعي حياتنا في ٢٠٣٠"
    ),
    TemplateData(
        name = "صحة",
        description = "نصائح طبية ومعلومات صحية",
        icon = Icons.Default.HealthAndSafety,
        color = Brush.linearGradient(
            listOf(
                androidx.compose.ui.graphics.Color(0xFF4CAF50),
                androidx.compose.ui.graphics.Color(0xFF8BC34A)
            )
        ),
        prompt = "٧ عادات يومية تطيل عمرك حسب العلم"
    )
)
