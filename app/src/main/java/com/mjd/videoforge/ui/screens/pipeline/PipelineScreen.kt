package com.mjd.videoforge.ui.screens.pipeline

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mjd.videoforge.pipeline.PipelineStep

/**
 * شاشة مسار الإنتاج
 *
 * تعرض تقدم كل خطوة من خطوات الإنتاج:
 * - السكربت
 * - الصور
 * - الصوت
 * - الموسيقى
 * - المونتاج
 * - التصدير
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipelineScreen(
    projectId: String,
    onProductionComplete: () -> Unit,
    onBack: () -> Unit
) {
    // Simulated progress
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }

    val steps = listOf(
        PipelineStepData(PipelineStep.GENERATING_SCRIPT, "توليد السكربت", Icons.Default.Description),
        PipelineStepData(PipelineStep.BUILDING_STORYBOARD, "بناء لوحة القصة", Icons.Default.ViewCarousel),
        PipelineStepData(PipelineStep.GENERATING_IMAGES, "توليد الصور", Icons.Default.Image),
        PipelineStepData(PipelineStep.GENERATING_AUDIO, "التعليق الصوتي", Icons.Default.RecordVoiceOver),
        PipelineStepData(PipelineStep.GENERATING_MUSIC, "الموسيقى", Icons.Default.MusicNote),
        PipelineStepData(PipelineStep.MONTAGE, "المونتاج", Icons.Default.MovieCreation),
        PipelineStepData(PipelineStep.EXPORTING, "التصدير", Icons.Default.FileDownload),
    )

    // Auto-advance for demo
    LaunchedEffect(Unit) {
        for (step in steps.indices) {
            currentStepIndex = step
            for (p in 0..100 step 5) {
                progress = p / 100f
                kotlinx.coroutines.delay(50)
            }
        }
        onProductionComplete()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("مسار الإنتاج", fontWeight = FontWeight.Bold)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall progress
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Circular progress
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { ((currentStepIndex.toFloat() + progress) / steps.size) },
                            modifier = Modifier.size(100.dp),
                            strokeWidth = 8.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            "${((currentStepIndex.toFloat() + progress) / steps.size * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "جاري الإنتاج...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        steps[currentStepIndex].label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Steps list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                steps.forEachIndexed { index, step ->
                    val status = when {
                        index < currentStepIndex -> StepStatus.COMPLETED
                        index == currentStepIndex -> StepStatus.IN_PROGRESS
                        else -> StepStatus.PENDING
                    }

                    StepRow(
                        step = step,
                        status = status,
                        stepProgress = if (index == currentStepIndex) progress else 0f
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Provider info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "التبديل التلقائي نشط",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "ينتقل بين المحركات تلقائياً عند نفاد الحصة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepRow(
    step: PipelineStepData,
    status: StepStatus,
    stepProgress: Float
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = when (status) {
            StepStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            StepStatus.IN_PROGRESS -> MaterialTheme.colorScheme.surfaceVariant
            StepStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status icon
            when (status) {
                StepStatus.COMPLETED -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                StepStatus.IN_PROGRESS -> {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { stepProgress },
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Icon(
                            step.icon,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                StepStatus.PENDING -> {
                    Icon(
                        step.icon,
                        null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            // Label
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    step.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = when (status) {
                        StepStatus.IN_PROGRESS -> FontWeight.Bold
                        else -> FontWeight.Normal
                    },
                    color = when (status) {
                        StepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                if (status == StepStatus.IN_PROGRESS) {
                    LinearProgressIndicator(
                        progress = { stepProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                }
            }

            // Status text
            Text(
                when (status) {
                    StepStatus.COMPLETED -> "✓"
                    StepStatus.IN_PROGRESS -> "${(stepProgress * 100).toInt()}%"
                    StepStatus.PENDING -> "—"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when (status) {
                    StepStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    StepStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                    StepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                }
            )
        }
    }
}

data class PipelineStepData(
    val step: PipelineStep,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

enum class StepStatus { PENDING, IN_PROGRESS, COMPLETED }
