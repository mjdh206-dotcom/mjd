package com.amjad.fahimni

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.amjad.fahimni.data.DialectProfile
import com.amjad.fahimni.data.SpeakerSide
import com.amjad.fahimni.ui.components.DialectPickerSheet
import com.amjad.fahimni.ui.screens.AllDialectsScreen
import com.amjad.fahimni.ui.screens.ConversationScreen
import com.amjad.fahimni.ui.screens.FaceToFaceScreen
import com.amjad.fahimni.ui.screens.PhrasebookScreen
import com.amjad.fahimni.ui.screens.SettingsAboutScreen
import com.amjad.fahimni.ui.theme.BorderSubtle
import com.amjad.fahimni.ui.theme.CardDark
import com.amjad.fahimni.ui.theme.CardElevated
import com.amjad.fahimni.ui.theme.CoralAlert
import com.amjad.fahimni.ui.theme.CyanRadar
import com.amjad.fahimni.ui.theme.EmeraldBright
import com.amjad.fahimni.ui.theme.FahimniTheme
import com.amjad.fahimni.ui.theme.ObsidianBg
import com.amjad.fahimni.ui.theme.RoyalGold
import com.amjad.fahimni.ui.theme.TextSecondary
import com.amjad.fahimni.ui.theme.TextWhite

class MainActivity : ComponentActivity() {

    private val viewModel: FahimniViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingSharedText(intent)

        setContent {
            FahimniTheme {
                FahimniRootApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingSharedText(intent)
    }

    private fun handleIncomingSharedText(intent: Intent?) {
        if (intent == null) return
        val shared = when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> null
        }?.trim()

        if (!shared.isNullOrBlank()) {
            viewModel.selectTab(ActiveTab.CONVERSATION)
            viewModel.processSpokenOrTypedInput(shared, SpeakerSide.PARTNER)
        }
    }
}

@Composable
private fun FahimniRootApp(viewModel: FahimniViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val partialTranscript by viewModel.speechRecognizer.partialTranscript.collectAsState()
    val audioLevel by viewModel.speechRecognizer.audioLevel.collectAsState()
    val speakingId by viewModel.voiceSpeaker.speakingId.collectAsState()

    var pendingVoiceSide by remember { mutableStateOf<SpeakerSide?>(null) }
    var popupSide by remember { mutableStateOf(SpeakerSide.ME) }
    var showMyPicker by remember { mutableStateOf(false) }
    var showPartnerPicker by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val side = pendingVoiceSide
        pendingVoiceSide = null
        if (granted && side != null) {
            viewModel.startVoiceCapture(side)
        } else if (!granted) {
            viewModel.showBanner("يرجى السماح بصلاحية المايكروفون لترجمة الصوت مباشرةً")
        }
    }

    val systemVoicePopupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val list = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val best = list?.firstOrNull()?.trim()
            if (!best.isNullOrBlank()) {
                viewModel.processSpokenOrTypedInput(best, popupSide)
            }
        }
    }

    DisposableEffect(viewModel) {
        viewModel.onLaunchSystemVoicePopup = { intent, side ->
            popupSide = side
            runCatching {
                systemVoicePopupLauncher.launch(intent)
            }.onFailure {
                viewModel.showBanner("جهازك لا يحتوي خدمة التعرف الصوتي، يمكنك الكتابة من زر لوحة المفاتيح")
            }
        }
        onDispose {
            viewModel.onLaunchSystemVoicePopup = null
        }
    }

    fun requestMicAndListen(side: SpeakerSide) {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            viewModel.startVoiceCapture(side)
        } else {
            pendingVoiceSide = side
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun copyToClipboard(text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Fahimni", text))
        viewModel.showBanner("📋 تم نسخ الترجمة إلى الحافظة")
    }

    fun shareTranslation(text: String) {
        runCatching {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "$text\n\n— عبر تطبيق فَهِّمني (تطوير أمجد حبيب)")
            }
            context.startActivity(Intent.createChooser(send, "مشاركة الترجمة"))
        }
    }

    if (showMyPicker) {
        DialectPickerSheet(
            title = "اختر لهجتك التي تحب أن تفهم بها",
            includeAutoDetect = false,
            selected = uiState.myDialect,
            onSelect = { viewModel.setMyDialect(it) },
            onDismiss = { showMyPicker = false },
        )
    }

    if (showPartnerPicker) {
        DialectPickerSheet(
            title = "لهجة أو لغة الشخص المقابل",
            includeAutoDetect = true,
            selected = uiState.partnerDialect,
            onSelect = { viewModel.setPartnerDialect(it) },
            onDismiss = { showPartnerPicker = false },
        )
    }

    // معالجة زر الرجوع بسلاسة
    BackHandler(
        enabled = uiState.activeListeningSide != null ||
            uiState.faceToFaceFlipped ||
            uiState.activeTab != ActiveTab.CONVERSATION
    ) {
        when {
            uiState.activeListeningSide != null -> viewModel.cancelActiveListening()
            uiState.faceToFaceFlipped -> viewModel.toggleFaceToFace()
            uiState.activeTab != ActiveTab.CONVERSATION -> viewModel.selectTab(ActiveTab.CONVERSATION)
        }
    }

    // في حال تفعيل وضع وجهاً لوجه 180 درجة نعرض الشاشة المقلوبة الكاملة
    if (uiState.faceToFaceFlipped) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg)
                .systemBarsPadding()
        ) {
            FaceToFaceScreen(
                uiState = uiState,
                partialTranscript = partialTranscript,
                onStartVoice = { side -> requestMicAndListen(side) },
                onCommitVoiceNow = { viewModel.commitActiveListeningNow() },
                onCancelVoice = { viewModel.cancelActiveListening() },
                onExitFlipMode = { viewModel.toggleFaceToFace() },
            )
        }
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .systemBarsPadding(),
        containerColor = ObsidianBg,
        topBar = {
            FahimniTopBar(
                isOnline = uiState.isOnline,
                handsFree = uiState.handsFreeLoop,
                onToggleHandsFree = { viewModel.setHandsFreeLoop(!uiState.handsFreeLoop) },
                onOpenFlipMode = { viewModel.toggleFaceToFace() },
            )
        },
        bottomBar = {
            FahimniBottomNav(
                activeTab = uiState.activeTab,
                onSelectTab = { viewModel.selectTab(it) },
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.activeTab) {
                ActiveTab.CONVERSATION -> ConversationScreen(
                    uiState = uiState,
                    partialTranscript = partialTranscript,
                    audioLevel = audioLevel,
                    speakingId = speakingId,
                    onOpenMyDialectPicker = { showMyPicker = true },
                    onOpenPartnerDialectPicker = { showPartnerPicker = true },
                    onSwapDialects = { viewModel.swapDialects() },
                    onStartVoice = { side -> requestMicAndListen(side) },
                    onSelectEarLocale = { loc, lbl -> viewModel.setPartnerListenEarLocale(loc, lbl) },
                    onCommitVoiceNow = { viewModel.commitActiveListeningNow() },
                    onCancelVoice = { viewModel.cancelActiveListening() },
                    onSendTypedText = { text, side -> viewModel.processSpokenOrTypedInput(text, side) },
                    onSpeakMessage = { msg -> viewModel.speakMessage(msg) },
                    onOpenAllDialectsFor = { txt -> viewModel.sendToAllDialectsStudio(txt) },
                    onCopyText = { txt -> copyToClipboard(txt) },
                    onShareText = { txt -> shareTranslation(txt) },
                )

                ActiveTab.ALL_DIALECTS -> AllDialectsScreen(
                    initialInput = uiState.comparisonInput,
                    items = uiState.comparisonResults,
                    speakingId = speakingId,
                    onCompare = { txt -> viewModel.refreshDialectComparison(txt) },
                    onSpeakItem = { txt, dialect, tag -> viewModel.speakArbitraryText(txt, dialect, tag) },
                    onCopy = { txt -> copyToClipboard(txt) },
                )

                ActiveTab.PHRASEBOOK -> {
                    val targetShort = when {
                        uiState.partnerDialect != DialectProfile.AUTO_DETECT -> uiState.partnerDialect.shortNameAr
                        uiState.lastDetectedPartner != null -> uiState.lastDetectedPartner!!.shortNameAr
                        else -> "العربية المبسطة"
                    }
                    PhrasebookScreen(
                        targetShortLabel = targetShort,
                        onSpeakPhraseToPartner = { phrase -> viewModel.speakQuickPhraseToPartner(phrase) },
                    )
                }

                ActiveTab.SETTINGS -> SettingsAboutScreen(
                    uiState = uiState,
                    onToggleAutoSpeak = { viewModel.setAutoSpeak(it) },
                    onToggleAutoSync = { viewModel.setAutoSyncToDetected(it) },
                    onToggleHandsFree = { viewModel.setHandsFreeLoop(it) },
                    onToggleDeepCloudAi = { viewModel.setDeepCloudAi(it) },
                    onSelectSpeed = { viewModel.setSpeechSpeed(it) },
                    onClearHistory = { viewModel.clearHistory() },
                )
            }
        }
    }
}

@Composable
private fun FahimniTopBar(
    isOnline: Boolean,
    handsFree: Boolean,
    onToggleHandsFree: () -> Unit,
    onOpenFlipMode: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = CardElevated,
                border = BorderStroke(1.2.dp, RoyalGold),
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🎙️", style = MaterialTheme.typography.titleLarge)
                }
            }
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "فَهِّمني",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextWhite,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isOnline) EmeraldBright.copy(alpha = 0.16f) else CoralAlert.copy(alpha = 0.2f),
                        border = BorderStroke(0.8.dp, if (isOnline) EmeraldBright else CoralAlert),
                    ) {
                        Text(
                            text = if (isOnline) "⚡ متصل سريع" else "غير متصل",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOnline) EmeraldBright else CoralAlert,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    text = "بواسطة أمجد حبيب • كشف اللهجة والترجمة الصوتية",
                    style = MaterialTheme.typography.labelSmall,
                    color = RoyalGold,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(
                onClick = onToggleHandsFree,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (handsFree) EmeraldBright else CardElevated),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_loop),
                    contentDescription = "حوار تلقائي",
                    tint = if (handsFree) Color(0xFF04141F) else CyanRadar,
                    modifier = Modifier.size(20.dp),
                )
            }

            IconButton(
                onClick = onOpenFlipMode,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardElevated),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_flip),
                    contentDescription = "وضع وجهاً لوجه",
                    tint = RoyalGold,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun FahimniBottomNav(
    activeTab: ActiveTab,
    onSelectTab: (ActiveTab) -> Unit,
) {
    NavigationBar(
        containerColor = CardDark,
        tonalElevation = 0.dp,
    ) {
        val navColors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color(0xFF04141F),
            selectedTextColor = RoyalGold,
            indicatorColor = RoyalGold,
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextSecondary,
        )

        NavigationBarItem(
            selected = activeTab == ActiveTab.CONVERSATION,
            onClick = { onSelectTab(ActiveTab.CONVERSATION) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            label = { Text("المحادثة", style = MaterialTheme.typography.labelSmall) },
            colors = navColors,
        )

        NavigationBarItem(
            selected = activeTab == ActiveTab.ALL_DIALECTS,
            onClick = { onSelectTab(ActiveTab.ALL_DIALECTS) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_globe),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            label = { Text("كل اللهجات", style = MaterialTheme.typography.labelSmall) },
            colors = navColors,
        )

        NavigationBarItem(
            selected = activeTab == ActiveTab.PHRASEBOOK,
            onClick = { onSelectTab(ActiveTab.PHRASEBOOK) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_book),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            label = { Text("عبارات سريعة", style = MaterialTheme.typography.labelSmall) },
            colors = navColors,
        )

        NavigationBarItem(
            selected = activeTab == ActiveTab.SETTINGS,
            onClick = { onSelectTab(ActiveTab.SETTINGS) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
            label = { Text("الإعدادات", style = MaterialTheme.typography.labelSmall) },
            colors = navColors,
        )
    }
}
