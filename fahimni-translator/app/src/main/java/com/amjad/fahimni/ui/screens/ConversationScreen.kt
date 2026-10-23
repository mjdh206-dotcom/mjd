package com.amjad.fahimni.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amjad.fahimni.FahimniUiState
import com.amjad.fahimni.R
import com.amjad.fahimni.data.BuiltInSamples
import com.amjad.fahimni.data.ConversationMessage
import com.amjad.fahimni.data.DEVELOPER_ATTRIBUTION
import com.amjad.fahimni.data.DialectProfile
import com.amjad.fahimni.data.SpeakerSide
import com.amjad.fahimni.ui.components.LiveListeningRadarCard
import com.amjad.fahimni.ui.theme.BorderSubtle
import com.amjad.fahimni.ui.theme.CardDark
import com.amjad.fahimni.ui.theme.CardElevated
import com.amjad.fahimni.ui.theme.CoralAlert
import com.amjad.fahimni.ui.theme.CyanDeep
import com.amjad.fahimni.ui.theme.CyanRadar
import com.amjad.fahimni.ui.theme.EmeraldBright
import com.amjad.fahimni.ui.theme.EmeraldDeep
import com.amjad.fahimni.ui.theme.RoyalGold
import com.amjad.fahimni.ui.theme.TextSecondary
import com.amjad.fahimni.ui.theme.TextWhite

@Composable
fun ConversationScreen(
    uiState: FahimniUiState,
    partialTranscript: String,
    audioLevel: Float,
    speakingId: String?,
    onOpenMyDialectPicker: () -> Unit,
    onOpenPartnerDialectPicker: () -> Unit,
    onSwapDialects: () -> Unit,
    onStartVoice: (SpeakerSide) -> Unit,
    onSelectEarLocale: (String, String) -> Unit,
    onCommitVoiceNow: () -> Unit,
    onCancelVoice: () -> Unit,
    onSendTypedText: (String, SpeakerSide) -> Unit,
    onSpeakMessage: (ConversationMessage) -> Unit,
    onOpenAllDialectsFor: (String) -> Unit,
    onCopyText: (String) -> Unit,
    onShareText: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    var showKeyboardBar by remember { mutableStateOf(false) }
    var typedInput by remember { mutableStateOf("") }
    var typedAsPartner by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
    ) {
        // شريط اختيار اللهجتين + مؤشر الكشف التلقائي
        DialectPairBar(
            myDialect = uiState.myDialect,
            partnerDialect = uiState.partnerDialect,
            lastDetectedPartner = uiState.lastDetectedPartner,
            onClickMine = onOpenMyDialectPicker,
            onClickPartner = onOpenPartnerDialectPicker,
            onSwap = onSwapDialects,
        )

        Spacer(Modifier.height(8.dp))

        // إشعار حالة حي عند اكتشاف اللهجة تلقائياً
        AnimatedVisibility(visible = !uiState.statusBanner.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0E293C),
                border = BorderStroke(1.dp, CyanRadar.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                Text(
                    text = uiState.statusBanner.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = RoyalGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        // منطقة المحادثة الرئيسية
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.messages.isEmpty()) {
                WelcomeEmptyHero(
                    onTrySample = { sample ->
                        onSendTypedText(sample.sampleText, sample.side)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(uiState.messages, key = { it.id }) { msg ->
                        MessageBubbleCard(
                            msg = msg,
                            isSpeakingThis = (speakingId == msg.id),
                            onSpeak = { onSpeakMessage(msg) },
                            onAllDialects = { onOpenAllDialectsFor(msg.originalText) },
                            onCopy = { onCopyText(msg.translatedText) },
                            onShare = {
                                onShareText("${msg.originalText}\n⬅️ (${msg.targetDialect.shortNameAr}):\n${msg.translatedText}")
                            },
                        )
                    }

                    item {
                        // شريط حقوق التطوير أسفل المحادثة
                        Text(
                            text = DEVELOPER_ATTRIBUTION,
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalGold.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        )
                    }
                }
            }

            if (uiState.isTranslating) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CardElevated,
                    border = BorderStroke(1.dp, RoyalGold),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.2.dp,
                            color = RoyalGold,
                        )
                        Text(
                            text = "جاري كشف اللهجة والترجمة الفورية عبر السحابة...",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextWhite,
                        )
                    }
                }
            }
        }

        // شريط كتابة نصية اختياري (مفيد للضوضاء أو لصق رسائل واتساب)
        AnimatedVisibility(visible = showKeyboardBar && uiState.activeListeningSide == null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CardDark,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "ترجمة نصية كـ:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                        RoleToggleChip(
                            label = "🎙️ كلامي أنا",
                            active = !typedAsPartner,
                            color = EmeraldBright,
                            onClick = { typedAsPartner = false }
                        )
                        RoleToggleChip(
                            label = "🎯 كلام الطرف المقابل (كشف لهجته)",
                            active = typedAsPartner,
                            color = CyanRadar,
                            onClick = { typedAsPartner = true }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = typedInput,
                            onValueChange = { typedInput = it },
                            placeholder = { Text("اكتب أو الصق أي كلام بأي لهجة...") },
                            maxLines = 2,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = {
                                if (typedInput.isNotBlank()) {
                                    onSendTypedText(
                                        typedInput,
                                        if (typedAsPartner) SpeakerSide.PARTNER else SpeakerSide.ME
                                    )
                                    typedInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (typedAsPartner) CyanRadar else EmeraldBright,
                                contentColor = Color(0xFF04141F),
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_send),
                                contentDescription = "إرسال",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }

        // منطقة التحكم الصوتي السفلية (إما رادار الاستماع الحي أو الزرّان العملاقان)
        if (uiState.activeListeningSide != null) {
            LiveListeningRadarCard(
                side = uiState.activeListeningSide,
                partialTranscript = partialTranscript,
                audioLevel = audioLevel,
                partnerEarLocale = uiState.partnerListenLocaleHint,
                onSelectEarLocale = onSelectEarLocale,
                onCommitNow = onCommitVoiceNow,
                onCancel = onCancelVoice,
            )
            Spacer(Modifier.height(8.dp))
        } else {
            DualVoiceDock(
                myDialect = uiState.myDialect,
                partnerDialect = uiState.partnerDialect,
                lastDetectedPartner = uiState.lastDetectedPartner,
                keyboardOpen = showKeyboardBar,
                onSpeakMine = { onStartVoice(SpeakerSide.ME) },
                onListenPartner = { onStartVoice(SpeakerSide.PARTNER) },
                onToggleKeyboard = { showKeyboardBar = !showKeyboardBar },
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun DialectPairBar(
    myDialect: DialectProfile,
    partnerDialect: DialectProfile,
    lastDetectedPartner: DialectProfile?,
    onClickMine: () -> Unit,
    onClickPartner: () -> Unit,
    onSwap: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // بطاقة لهجتي
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(CardElevated)
                .clickable(onClick = onClickMine)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                text = "لهجتي (أفهم بها)",
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldBright,
            )
            Text(
                text = "${myDialect.flag} ${myDialect.shortNameAr}",
                style = MaterialTheme.typography.titleSmall,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }

        // زر التبديل
        IconButton(
            onClick = onSwap,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A2946)),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_swap),
                contentDescription = "تبديل",
                tint = RoyalGold,
                modifier = Modifier.size(20.dp),
            )
        }

        // بطاقة الشخص المقابل (تلقائي ذكي)
        val partnerTitle = if (partnerDialect == DialectProfile.AUTO_DETECT) {
            if (lastDetectedPartner != null) {
                "🎯 تلقائي: ${lastDetectedPartner.flag} ${lastDetectedPartner.shortNameAr}"
            } else {
                "🎯 كشف تلقائي للهجة"
            }
        } else {
            "${partnerDialect.flag} ${partnerDialect.shortNameAr}"
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(CardElevated)
                .clickable(onClick = onClickPartner)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                text = "الشخص المقابل",
                style = MaterialTheme.typography.labelSmall,
                color = CyanRadar,
            )
            Text(
                text = partnerTitle,
                style = MaterialTheme.typography.titleSmall,
                color = if (lastDetectedPartner != null && partnerDialect == DialectProfile.AUTO_DETECT) {
                    RoyalGold
                } else {
                    TextWhite
                },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun WelcomeEmptyHero(
    onTrySample: (com.amjad.fahimni.data.SampleTestItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardDark,
            border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "🎙️ احكِ بأي لهجة عربية.. والكل يفهمك فوراً!",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "• اضغط «احكي بلهجتك» لتتكلم بأي لهجة عربية فينطقها التطبيق للشخص المقابل بلهجته أو بالعربية المبسطة.\n" +
                        "• اضغط «اسمع المقابل» ليكتشف التطبيق لهجة المتحدث تلقائياً وينطق لك معناها بلهجتك بالصوت فوراً!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Start,
                )

                Spacer(Modifier.height(12.dp))

                // بطاقة التوقيع الرسمية المطلوبة من المطور أمجد حبيب
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1D1A0E),
                    border = BorderStroke(1.dp, RoyalGold),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "✨ $DEVELOPER_ATTRIBUTION ✨",
                        style = MaterialTheme.typography.labelLarge,
                        color = RoyalGold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "⚡ جرّب الكشف التلقائي والنطق الصوتي بضغطة واحدة:",
                style = MaterialTheme.typography.labelLarge,
                color = CyanRadar,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BuiltInSamples.quickTests.forEach { sample ->
                    Surface(
                        onClick = { onTrySample(sample) },
                        shape = RoundedCornerShape(16.dp),
                        color = CardElevated,
                        border = BorderStroke(1.dp, BorderSubtle),
                    ) {
                        Column(
                            modifier = Modifier
                                .width(220.dp)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = sample.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (sample.side == SpeakerSide.PARTNER) CyanRadar else EmeraldBright,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "«${sample.sampleText}»",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageBubbleCard(
    msg: ConversationMessage,
    isSpeakingThis: Boolean,
    onSpeak: () -> Unit,
    onAllDialects: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    val isMe = (msg.side == SpeakerSide.ME)
    val accent = if (isMe) EmeraldBright else CyanRadar
    val containerColor = if (isMe) Color(0xFF0B241E) else Color(0xFF0C1E33)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = BorderStroke(
            width = if (isSpeakingThis) 1.8.dp else 1.dp,
            color = if (isSpeakingThis) RoyalGold else accent.copy(alpha = 0.45f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // رأس البطاقة: المتحدث + اللهجة المكتشفة + زمن الاستجابة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = if (isMe) "🎙️ كلامي" else "🎧 الطرف المقابل",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF07101E),
                        border = BorderStroke(0.8.dp, RoyalGold.copy(alpha = 0.6f)),
                    ) {
                        Text(
                            text = if (msg.isAutoDetected) {
                                "🎯 اكتُشفت: ${msg.detectedDialect.flag} ${msg.detectedDialect.shortNameAr} ${msg.confidence}%"
                            } else {
                                "${msg.detectedDialect.flag} ${msg.detectedDialect.shortNameAr} ⬅️ ${msg.targetDialect.flag} ${msg.targetDialect.shortNameAr}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                Text(
                    text = "⚡ ${msg.latencyMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }

            Spacer(Modifier.height(8.dp))

            // الكلام الأصلي
            Text(
                text = msg.originalText,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            Spacer(Modifier.height(6.dp))

            // الترجمة المفهومة (الخط البارز الكبير)
            Text(
                text = msg.translatedText,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                color = TextWhite,
                fontWeight = FontWeight.ExtraBold,
            )

            // سطر المعنى بالعربية المبسطة إذا كان مختلفاً ومفيداً
            if (msg.clearArabicSubtitle.isNotBlank() &&
                msg.clearArabicSubtitle != msg.translatedText &&
                msg.clearArabicSubtitle != msg.originalText
            ) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "المعنى المبسط: ${msg.clearArabicSubtitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldBright.copy(alpha = 0.9f),
                )
            }

            // شرح الكلمات الشعبية المكتشفة في الجملة!
            if (msg.slangNotes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    msg.slangNotes.forEach { note ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF081221),
                            border = BorderStroke(0.8.dp, BorderSubtle),
                        ) {
                            Text(
                                text = "💡 ${note.word} = ${note.meaning}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextWhite.copy(alpha = 0.92f),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // شريط أزرار الإجراءات (نطق صوتي + كل اللهجات + نسخ + مشاركة)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onSpeak,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSpeakingThis) CoralAlert else accent,
                        contentColor = Color(0xFF04141F),
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(
                        painter = painterResource(if (isSpeakingThis) R.drawable.ic_stop else R.drawable.ic_volume),
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isSpeakingThis) "إيقاف الصوت" else "اسمع بالصوت",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmallBubbleIconBtn(R.drawable.ic_globe, "بكل اللهجات", onAllDialects)
                    SmallBubbleIconBtn(R.drawable.ic_copy, "نسخ", onCopy)
                    SmallBubbleIconBtn(R.drawable.ic_share, "مشاركة", onShare)
                }
            }
        }
    }
}

@Composable
private fun SmallBubbleIconBtn(iconRes: Int, contentDesc: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xFF091426)),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDesc,
            tint = TextSecondary,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun RoleToggleChip(
    label: String,
    active: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (active) color.copy(alpha = 0.2f) else CardElevated,
        border = BorderStroke(1.dp, if (active) color else BorderSubtle),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) color else TextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun DualVoiceDock(
    myDialect: DialectProfile,
    partnerDialect: DialectProfile,
    lastDetectedPartner: DialectProfile?,
    keyboardOpen: Boolean,
    onSpeakMine: () -> Unit,
    onListenPartner: () -> Unit,
    onToggleKeyboard: () -> Unit,
) {
    val targetLabelForMe = when {
        partnerDialect != DialectProfile.AUTO_DETECT -> partnerDialect.shortNameAr
        lastDetectedPartner != null -> lastDetectedPartner.shortNameAr
        else -> "عربي مبسط"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // الزر الأول: أنا أتكلم بأي لهجة -> يفهمني الشخص المقابل
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.horizontalGradient(listOf(EmeraldBright, EmeraldDeep)))
                .clickable(onClick = onSpeakMine)
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x28000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column {
                    Text(
                        text = "احكي بلهجتك",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "ينطق له: $targetLabelForMe",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE6FFF5),
                        maxLines = 1,
                    )
                }
            }
        }

        // زر لوحة المفاتيح السريعة بالوسط
        IconButton(
            onClick = onToggleKeyboard,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (keyboardOpen) RoyalGold else CardElevated)
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_keyboard),
                contentDescription = "كتابة",
                tint = if (keyboardOpen) Color(0xFF1A1202) else RoyalGold,
                modifier = Modifier.size(22.dp),
            )
        }

        // الزر الثاني: اسمع الشخص المقابل -> كشف تلقائي للهجته ونطقها لي بالصوت!
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.horizontalGradient(listOf(CyanRadar, CyanDeep)))
                .clickable(onClick = onListenPartner)
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x28000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_radar),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column {
                    Text(
                        text = "اسمع المقابل",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "يكتشف لهجته ⬅️ ${myDialect.shortNameAr}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE0F7FF),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
