package com.amjad.fahimni.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amjad.fahimni.FahimniUiState
import com.amjad.fahimni.R
import com.amjad.fahimni.data.SpeakerSide
import com.amjad.fahimni.ui.theme.CardDark
import com.amjad.fahimni.ui.theme.CardElevated
import com.amjad.fahimni.ui.theme.CoralAlert
import com.amjad.fahimni.ui.theme.CyanRadar
import com.amjad.fahimni.ui.theme.EmeraldBright
import com.amjad.fahimni.ui.theme.RoyalGold
import com.amjad.fahimni.ui.theme.TextSecondary
import com.amjad.fahimni.ui.theme.TextWhite

/**
 * وضع المحادثة وجهاً لوجه على الطاولة (شاشة مقسومة 180 درجة):
 * النصف العلوي مقلوب للشخص الجالس أمامك، والنصف السفلي مواجه لك!
 */
@Composable
fun FaceToFaceScreen(
    uiState: FahimniUiState,
    partialTranscript: String,
    onStartVoice: (SpeakerSide) -> Unit,
    onCommitVoiceNow: () -> Unit,
    onCancelVoice: () -> Unit,
    onExitFlipMode: () -> Unit,
) {
    val lastFromMeToPartner = uiState.messages.lastOrNull { it.side == SpeakerSide.ME }
    val lastFromPartnerToMe = uiState.messages.lastOrNull { it.side == SpeakerSide.PARTNER }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // النصف العلوي (مقلوب 180 درجة للشخص الجالس أمامك!)
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0A1D31),
            border = BorderStroke(1.5.dp, CyanRadar.copy(alpha = 0.6f)),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .rotate(180f),
        ) {
            HalfSpeakerPane(
                title = uiState.lastDetectedPartner?.let {
                    "🎯 لهجتك المكتشفة: ${it.flag} ${it.shortNameAr}"
                } ?: "🎯 تحدث بأي لهجة (كشف تلقائي)",
                bigMessage = if (uiState.activeListeningSide == SpeakerSide.PARTNER) {
                    partialTranscript.ifBlank { "جاري الاستماع إليك الآن..." }
                } else {
                    lastFromMeToPartner?.translatedText
                        ?: "مرحباً! اضغط الزر بالأسفل وتحدث بلهجتك بحرية"
                },
                subMessage = lastFromMeToPartner?.originalText.orEmpty(),
                isListening = (uiState.activeListeningSide == SpeakerSide.PARTNER),
                accent = CyanRadar,
                buttonLabel = "تحدث الآن (كشف تلقائي)",
                onMicClick = { onStartVoice(SpeakerSide.PARTNER) },
                onCommitNow = onCommitVoiceNow,
                onCancel = onCancelVoice,
            )
        }

        // شريط فاصل وسط الطاولة مع زر العودة للوضع العادي
        Surface(
            shape = RoundedCornerShape(50),
            color = CardElevated,
            border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "🔄 وضع الطاولة وجهاً لوجه (180°)",
                    style = MaterialTheme.typography.labelLarge,
                    color = RoyalGold,
                )
                OutlinedButton(
                    onClick = onExitFlipMode,
                    shape = RoundedCornerShape(50),
                ) {
                    Text("إنهاء القلب", color = TextWhite)
                }
            }
        }

        // النصف السفلي (مواجه لك أنت!)
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = CardDark,
            border = BorderStroke(1.5.dp, EmeraldBright.copy(alpha = 0.6f)),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            HalfSpeakerPane(
                title = "🎙️ لهجتي: ${uiState.myDialect.flag} ${uiState.myDialect.shortNameAr}",
                bigMessage = if (uiState.activeListeningSide == SpeakerSide.ME) {
                    partialTranscript.ifBlank { "تكلم الآن بأي لهجة عربية..." }
                } else {
                    lastFromPartnerToMe?.translatedText
                        ?: "هنا يظهر ويُنطق كلام الشخص المقابل مترجماً للهجتك"
                },
                subMessage = lastFromPartnerToMe?.let {
                    "قال (${it.detectedDialect.shortNameAr}): ${it.originalText}"
                }.orEmpty(),
                isListening = (uiState.activeListeningSide == SpeakerSide.ME),
                accent = EmeraldBright,
                buttonLabel = "احكي بلهجتك الآن",
                onMicClick = { onStartVoice(SpeakerSide.ME) },
                onCommitNow = onCommitVoiceNow,
                onCancel = onCancelVoice,
            )
        }
    }
}

@Composable
private fun HalfSpeakerPane(
    title: String,
    bigMessage: String,
    subMessage: String,
    isListening: Boolean,
    accent: Color,
    buttonLabel: String,
    onMicClick: () -> Unit,
    onCommitNow: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = bigMessage,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                    color = TextWhite,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                if (subMessage.isNotBlank() && !isListening) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = subMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }

        if (isListening) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onCommitNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalGold,
                        contentColor = Color(0xFF1A1202),
                    ),
                ) {
                    Text("ترجم وانطق الآن ✓", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onCancel) {
                    Text("إلغاء", color = CoralAlert)
                }
            }
        } else {
            Button(
                onClick = onMicClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color(0xFF04141F),
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(buttonLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
