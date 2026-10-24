package com.amjad.fahimni.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amjad.fahimni.R
import com.amjad.fahimni.SimpleUiState
import com.amjad.fahimni.data.ChatTurn
import com.amjad.fahimni.data.DEVELOPER_ATTRIBUTION
import com.amjad.fahimni.data.LanguageCatalog
import com.amjad.fahimni.data.SpokenLanguage
import com.amjad.fahimni.data.TurnSide
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
fun SimpleTranslatorScreen(
    uiState: SimpleUiState,
    partialTranscript: String,
    audioLevel: Float,
    speakingId: String?,
    onPressDetectOther: () -> Unit,
    onPressSpeakArabic: () -> Unit,
    onFinishListeningNow: () -> Unit,
    onCancelListening: () -> Unit,
    onReplayTurn: (ChatTurn) -> Unit,
    onResetAutoDetect: () -> Unit,
    onManualSelectLanguage: (SpokenLanguage?) -> Unit,
    onToggleAutoSpeak: () -> Unit,
    onClearScreen: () -> Unit,
) {
    val listState = rememberLazyListState()
    var showLanguageSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.turns.size) {
        if (uiState.turns.isNotEmpty()) {
            listState.animateScrollToItem(uiState.turns.size - 1)
        }
    }

    if (showLanguageSheet) {
        SimpleLanguageSheet(
            selected = uiState.detectedPartnerLanguage,
            onSelect = {
                onManualSelectLanguage(it)
                showLanguageSheet = false
            },
            onDismiss = { showLanguageSheet = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        // 1) الرأس المبسط جداً + توقيع أمجد حبيب
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🎙️ فَهِّمني",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = DEVELOPER_ATTRIBUTION,
                    style = MaterialTheme.typography.labelMedium,
                    color = RoyalGold,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.turns.isNotEmpty()) {
                    IconButton(
                        onClick = onClearScreen,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CardElevated),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_trash),
                            contentDescription = "مسح",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                IconButton(
                    onClick = onToggleAutoSpeak,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (uiState.autoSpeak) EmeraldBright.copy(alpha = 0.2f) else CardElevated),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_volume),
                        contentDescription = "الصوت",
                        tint = if (uiState.autoSpeak) EmeraldBright else CoralAlert,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 2) شريط واضح يعرض لغة الشخص الذي أمامك
        val detectedLang = uiState.detectedPartnerLanguage
        Surface(
            onClick = { showLanguageSheet = true },
            shape = RoundedCornerShape(18.dp),
            color = CardDark,
            border = BorderStroke(
                width = 1.2.dp,
                color = if (detectedLang != null) RoyalGold else BorderSubtle,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "لغة الشخص يلي قدامك:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Text(
                        text = if (detectedLang != null) {
                            "${detectedLang.flag} ${detectedLang.nameAr}"
                        } else {
                            "🎯 كشف تلقائي بمجرد ما تسمعو"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (detectedLang != null) RoyalGold else CyanRadar,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                if (detectedLang != null) {
                    Surface(
                        onClick = onResetAutoDetect,
                        shape = RoundedCornerShape(50),
                        color = CardElevated,
                        border = BorderStroke(1.dp, CyanRadar.copy(alpha = 0.6f)),
                    ) {
                        Text(
                            text = "شخص جديد ↻",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanRadar,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                } else {
                    Text(
                        text = "تحديد ▾",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }
        }

        AnimatedVisibility(visible = !uiState.toastMessage.isNullOrBlank()) {
            Text(
                text = uiState.toastMessage.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = RoyalGold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
        }

        Spacer(Modifier.height(10.dp))

        // 3) منطقة عرض الترجمة (واضحة وكبيرة وبلا أي تعقيد)
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.turns.isEmpty()) {
                SimpleEmptyGuide()
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(uiState.turns, key = { it.id }) { turn ->
                        SimpleTurnCard(
                            turn = turn,
                            isSpeaking = (speakingId == turn.id),
                            onReplay = { onReplayTurn(turn) },
                        )
                    }
                }
            }

            if (uiState.isTranslating) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CardElevated,
                    border = BorderStroke(1.dp, RoyalGold),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.4.dp,
                            color = RoyalGold,
                        )
                        Text(
                            text = "جاري الترجمة والنطق فوراً...",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextWhite,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 4) الزرّان العملاقان فقط في الأسفل!
        val listeningSide = uiState.activeListeningSide
        if (listeningSide != null) {
            ActiveListeningBox(
                side = listeningSide,
                partialTranscript = partialTranscript,
                audioLevel = audioLevel,
                onFinishNow = onFinishListeningNow,
                onCancel = onCancelListening,
            )
        } else {
            val replyLangLabel = detectedLang?.let { "${it.flag} ${it.nameAr}" }
                ?: "🇬🇧 الإنجليزية (أو اسمعو أولاً لكشف لغتو)"

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // الزر الأول: اعرف شو لغتو ويترجم للعربي
                GiantActionButton(
                    title = "اعرف شو لغتو وترجم للعربي",
                    subtitle = "اكبس وخلي الشخص يلي قدامك يحكي",
                    iconRes = R.drawable.ic_radar,
                    gradient = listOf(CyanRadar, CyanDeep),
                    onClick = onPressDetectOther,
                )

                // الزر الثاني: احكي وبناءً على لغتو يترجملو
                GiantActionButton(
                    title = "احكي بالعربي — يترجملو للغتو",
                    subtitle = "بأي لهجة عربية ⬅️ ينطق له بـ: $replyLangLabel",
                    iconRes = R.drawable.ic_mic,
                    gradient = listOf(EmeraldBright, EmeraldDeep),
                    onClick = onPressSpeakArabic,
                )
            }
        }
    }
}

@Composable
private fun SimpleEmptyGuide() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = CardDark,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "بكبستين بس بدون أي تعقيد 👇",
                    style = MaterialTheme.typography.titleLarge,
                    color = RoyalGold,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "1️⃣ اكبس الزر الأزرق لتسمع الشخص يلي قدامك: بيعرف شو لغتو تلقائياً وبيترجملك للعربي بالصوت.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextWhite,
                    textAlign = TextAlign.Start,
                )

                Text(
                    text = "2️⃣ اكبس الزر الأخضر واحكي بالعربي بأي لهجة: بيترجم كلامك وبينطقلو إياه بلغتو فوراً!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextWhite,
                    textAlign = TextAlign.Start,
                )

                HorizontalDivider(color = BorderSubtle)

                Text(
                    text = "✨ $DEVELOPER_ATTRIBUTION ✨",
                    style = MaterialTheme.typography.labelLarge,
                    color = RoyalGold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SimpleTurnCard(
    turn: ChatTurn,
    isSpeaking: Boolean,
    onReplay: () -> Unit,
) {
    val isOther = (turn.side == TurnSide.OTHER_PERSON)
    val accent = if (isOther) CyanRadar else EmeraldBright
    val bg = if (isOther) Color(0xFF0B1F35) else Color(0xFF0A241D)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = bg,
        border = BorderStroke(
            width = if (isSpeaking) 2.dp else 1.dp,
            color = if (isSpeaking) RoyalGold else accent.copy(alpha = 0.5f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isOther) {
                        "🎧 هو حكى (${turn.partnerLanguage.flag} ${turn.partnerLanguage.nameAr})"
                    } else {
                        "🎙️ أنت حكيت ⬅️ نُطقت له بـ (${turn.partnerLanguage.flag} ${turn.partnerLanguage.nameAr})"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = turn.originalText,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = turn.translatedText,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                color = TextWhite,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onReplay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSpeaking) CoralAlert else accent,
                    contentColor = Color(0xFF04141F),
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(
                    painter = painterResource(if (isSpeaking) R.drawable.ic_stop else R.drawable.ic_volume),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isSpeaking) "إيقاف الصوت" else "اسمع مرة ثانية",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun GiantActionButton(
    title: String,
    subtitle: String,
    iconRes: Int,
    gradient: List<Color>,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(gradient))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x2B000000)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xEBFFFFFF),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ActiveListeningBox(
    side: TurnSide,
    partialTranscript: String,
    audioLevel: Float,
    onFinishNow: () -> Unit,
    onCancel: () -> Unit,
) {
    val isOther = (side == TurnSide.OTHER_PERSON)
    val accent = if (isOther) CyanRadar else EmeraldBright
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "p"
    )
    val smoothLevel by animateFloatAsState(
        targetValue = audioLevel.coerceIn(0.1f, 1f),
        animationSpec = tween(100),
        label = "lvl"
    )

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = CardElevated,
        border = BorderStroke(2.dp, accent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(56.dp)) {
                        val r = (size.minDimension / 2f) * pulse * (0.75f + smoothLevel * 0.25f)
                        drawCircle(
                            color = accent.copy(alpha = 0.25f),
                            radius = r.coerceAtMost(size.minDimension / 2f),
                            center = Offset(size.width / 2f, size.height / 2f),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(if (isOther) R.drawable.ic_radar else R.drawable.ic_mic),
                            contentDescription = null,
                            tint = Color(0xFF04141F),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isOther) {
                            "🎯 عم اسمع الشخص المقابل..."
                        } else {
                            "🎙️ احكي بالعربي بأي لهجة..."
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = accent,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = partialTranscript.ifBlank { "تكلم الآن بشكل طبيعي..." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextWhite,
                        maxLines = 2,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onFinishNow,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color(0xFF04141F),
                    ),
                ) {
                    Text(
                        text = "✓ خلص حكي — ترجم الآن",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("إلغاء", color = CoralAlert, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleLanguageSheet(
    selected: SpokenLanguage?,
    onSelect: (SpokenLanguage?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }

    val filtered = remember(search) {
        LanguageCatalog.allLanguages.filter {
            search.isBlank() || it.nameAr.contains(search, ignoreCase = true) || it.id.contains(search, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardDark,
        contentColor = TextWhite,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "لغة الشخص المقابل",
                style = MaterialTheme.typography.headlineSmall,
                color = RoyalGold,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("ابحث عن لغة (مثلاً: الهولندية، التركية، الإنجليزية...)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(400.dp),
            ) {
                if (search.isBlank()) {
                    item {
                        Surface(
                            onClick = { onSelect(null) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected == null) CyanRadar.copy(alpha = 0.2f) else CardElevated,
                            border = BorderStroke(1.2.dp, if (selected == null) CyanRadar else BorderSubtle),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "🎯 كشف تلقائي ذكي عند سماع صوته",
                                style = MaterialTheme.typography.titleMedium,
                                color = CyanRadar,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                    }
                }

                items(filtered, key = { it.id }) { lang ->
                    val isSel = (selected?.id == lang.id)
                    Surface(
                        onClick = { onSelect(lang) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSel) RoyalGold.copy(alpha = 0.18f) else CardElevated,
                        border = BorderStroke(1.dp, if (isSel) RoyalGold else BorderSubtle),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(lang.flag, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                text = lang.nameAr,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSel) RoyalGold else TextWhite,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
