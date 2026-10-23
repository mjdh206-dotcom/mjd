package com.amjad.fahimni.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amjad.fahimni.R
import com.amjad.fahimni.data.SpeakerSide
import com.amjad.fahimni.ui.theme.BorderSubtle
import com.amjad.fahimni.ui.theme.CardElevated
import com.amjad.fahimni.ui.theme.CoralAlert
import com.amjad.fahimni.ui.theme.CyanRadar
import com.amjad.fahimni.ui.theme.EmeraldBright
import com.amjad.fahimni.ui.theme.RoyalGold
import com.amjad.fahimni.ui.theme.TextSecondary
import com.amjad.fahimni.ui.theme.TextWhite

@Composable
fun LiveListeningRadarCard(
    side: SpeakerSide,
    partialTranscript: String,
    audioLevel: Float,
    partnerEarLocale: String,
    onSelectEarLocale: (String, String) -> Unit,
    onCommitNow: () -> Unit,
    onCancel: () -> Unit,
) {
    val accent = if (side == SpeakerSide.ME) EmeraldBright else CyanRadar
    val infinite = rememberInfiniteTransition(label = "radar")
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse"
    )
    val smoothLevel by animateFloatAsState(
        targetValue = audioLevel.coerceIn(0.08f, 1f),
        animationSpec = tween(120),
        label = "lvl"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    listOf(CardElevated, Color(0xFF0A1324))
                )
            )
            .border(1.5.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(26.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (side == SpeakerSide.ME) {
                "🎙️ تكلم الآن بأي لهجة عربية..."
            } else {
                "🎯 رادار الاستماع التلقائي — دع الشخص المقابل يتكلم"
            },
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            textAlign = TextAlign.Center,
        )

        if (side == SpeakerSide.PARTNER) {
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EarChip("كل اللهجات العربية", "ar", partnerEarLocale == "ar", accent) {
                    onSelectEarLocale("ar", "جميع اللهجات العربية")
                }
                EarChip("English", "en-US", partnerEarLocale == "en-US", accent) {
                    onSelectEarLocale("en-US", "الإنجليزية")
                }
                EarChip("Türkçe", "tr-TR", partnerEarLocale == "tr-TR", accent) {
                    onSelectEarLocale("tr-TR", "التركية")
                }
                EarChip("NL", "nl-NL", partnerEarLocale == "nl-NL", accent) {
                    onSelectEarLocale("nl-NL", "الهولندية")
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // كرة الموجات الصوتية الحية
        Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(96.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxR = size.minDimension / 2f
                drawCircle(
                    color = accent.copy(alpha = 0.14f),
                    radius = (maxR * pulse * (0.72f + smoothLevel * 0.28f)).coerceAtMost(maxR),
                    center = center,
                )
                drawCircle(
                    color = accent.copy(alpha = 0.28f),
                    radius = (maxR * 0.68f * (0.85f + smoothLevel * 0.25f)).coerceAtMost(maxR * 0.88f),
                    center = center,
                )
            }
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(if (side == SpeakerSide.ME) R.drawable.ic_mic else R.drawable.ic_radar),
                    contentDescription = null,
                    tint = Color(0xFF04141F),
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // أشرطة تردد صوتي ديناميكية
        SoundEqualizerBars(level = smoothLevel, color = accent, phase = pulse)

        Spacer(Modifier.height(10.dp))

        Text(
            text = partialTranscript.ifBlank { "جاري الاستماع المباشر عبر السحابة..." },
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (partialTranscript.isNotBlank()) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (partialTranscript.isNotBlank()) TextWhite else TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF070E1B))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onCommitNow,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color(0xFF031914),
                ),
            ) {
                Text("ترجم وانطق الآن ✓", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAlert),
            ) {
                Text("إلغاء")
            }
        }
    }
}

@Composable
private fun EarChip(
    label: String,
    code: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) accent.copy(alpha = 0.22f) else Color(0xFF08101E),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) RoyalGold else BorderSubtle
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) RoyalGold else TextSecondary,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun SoundEqualizerBars(
    level: Float,
    color: Color,
    phase: Float,
) {
    val weights = listOf(0.4f, 0.75f, 1.0f, 0.65f, 0.9f, 0.55f, 0.8f)
    Canvas(
        modifier = Modifier
            .width(120.dp)
            .height(22.dp)
    ) {
        val barWidth = 8.dp.toPx()
        val gap = 8.dp.toPx()
        val totalW = weights.size * barWidth + (weights.size - 1) * gap
        var startX = (size.width - totalW) / 2f
        weights.forEachIndexed { idx, w ->
            val factor = ((w * level * phase) + (if (idx % 2 == 0) 0.18f else 0.1f)).coerceIn(0.18f, 1f)
            val h = size.height * factor
            drawRoundRect(
                color = color,
                topLeft = Offset(startX, (size.height - h) / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
            startX += barWidth + gap
        }
    }
}
