package com.mjd.videoforge.ui.screens.home

import androidx.compose.animation.*
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
import androidx.compose.ui.unit.sp
import com.mjd.videoforge.data.model.*

/**
 * شاشة الأسئلة الذكية
 *
 * تسأل المستخدم عن:
 * - مدة الفيديو
 * - نسبة العرض
 * - اللهجة
 * - التعليق الصوتي
 * - أسلوب الصور
 * - الموسيقى
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartQuestionsScreen(
    projectId: String,
    onQuestionsComplete: () -> Unit,
    onBack: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var duration by remember { mutableStateOf(VideoDuration.SEC_30) }
    var ratio by remember { mutableStateOf(AspectRatio.VERTICAL) }
    var dialect by remember { mutableStateOf(Dialect.FUSHA) }
    var voiceProvider by remember { mutableStateOf("edge_tts") }
    var imageStyle by remember { mutableStateOf(ImageStyle.CINEMATIC) }
    var includeMusic by remember { mutableStateOf(true) }

    val steps = listOf("المدة", "النسبة", "اللهجة", "الصوت", "الأسلوب", "الموسيقى")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أسئلة ذكية (${currentStep + 1}/${steps.size})") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) currentStep-- else onBack()
                    }) {
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
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("السابق")
                        }
                    }

                    if (currentStep < steps.size - 1) {
                        Button(
                            onClick = { currentStep++ },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("التالي", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onQuestionsComplete,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("ابدأ التوليد", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
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
            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentStep + 1).toFloat() / steps.size },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Step title
            Text(
                text = steps[currentStep],
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Step content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn() + slideInHorizontally { it / 4 } togetherWith
                            fadeOut() + slideOutHorizontally { -it / 4 }
                }
            ) { step ->
                when (step) {
                    0 -> DurationSelector(selected = duration) { duration = it }
                    1 -> RatioSelector(selected = ratio) { ratio = it }
                    2 -> DialectSelector(selected = dialect) { dialect = it }
                    3 -> VoiceSelector(selected = voiceProvider) { voiceProvider = it }
                    4 -> StyleSelector(selected = imageStyle) { imageStyle = it }
                    5 -> MusicSelector(include = includeMusic) { includeMusic = it }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun DurationSelector(selected: VideoDuration, onSelect: (VideoDuration) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "كم مدة الفيديو؟",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        VideoDuration.entries.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            option.labelAr,
                            fontWeight = if (selected == option) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "(${option.seconds} ثانية)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                leadingIcon = if (selected == option) {
                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                } else null,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun RatioSelector(selected: AspectRatio, onSelect: (AspectRatio) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "ما نسبة العرض؟",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AspectRatio.entries.forEach { option ->
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp),
                    onClick = { onSelect(option) },
                    shape = RoundedCornerShape(16.dp),
                    border = if (selected == option) {
                        androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected == option)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Visual ratio indicator
                        val ratioBox = when (option) {
                            AspectRatio.VERTICAL -> Modifier.size(40.dp, 70.dp)
                            AspectRatio.HORIZONTAL -> Modifier.size(70.dp, 40.dp)
                        }
                        Surface(
                            modifier = ratioBox,
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected == option)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ) {}
                        Spacer(Modifier.height(12.dp))
                        Text(
                            option.labelAr,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selected == option) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DialectSelector(selected: Dialect, onSelect: (Dialect) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "بأي لهجة؟",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Dialect.entries.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option.labelAr) },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun VoiceSelector(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        "edge_tts" to "Edge TTS (مجاني)",
        "elevenlabs" to "ElevenLabs (أعلى جودة)",
        "gpt" to "OpenAI TTS",
        "google" to "Google TTS",
        "azure" to "Azure Speech",
        "" to "بدون تعليق صوتي"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "أي محرك للتعليق الصوتي؟",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        options.forEach { (id, label) ->
            FilterChip(
                selected = selected == id,
                onClick = { onSelect(id) },
                label = { Text(label) },
                shape = RoundedCornerShape(12.dp),
                leadingIcon = if (id == "edge_tts") {
                    { Text("🆓", fontSize = 12.sp) }
                } else null
            )
        }
    }
}

@Composable
fun StyleSelector(selected: ImageStyle, onSelect: (ImageStyle) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "أسلوب الصور؟",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ImageStyle.entries.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option.labelAr) },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun MusicSelector(include: Boolean, onToggle: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "هل تريد موسيقى خلفية؟",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                onClick = { onToggle(true) },
                shape = RoundedCornerShape(16.dp),
                border = if (include)
                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (include)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("نعم", fontWeight = FontWeight.Bold)
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                onClick = { onToggle(false) },
                shape = RoundedCornerShape(16.dp),
                border = if (!include)
                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (!include)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.MusicOff, null, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("لا", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
