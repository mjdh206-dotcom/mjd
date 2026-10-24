package com.amjad.fahimni

import android.Manifest
import android.app.Activity
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.amjad.fahimni.data.TurnSide
import com.amjad.fahimni.ui.SimpleTranslatorScreen
import com.amjad.fahimni.ui.theme.FahimniTheme
import com.amjad.fahimni.ui.theme.ObsidianBg

class MainActivity : ComponentActivity() {

    private val viewModel: FahimniViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingText(intent)

        setContent {
            FahimniTheme {
                FahimniSimpleApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingText(intent)
    }

    private fun handleIncomingText(intent: Intent?) {
        if (intent == null) return
        val shared = when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> null
        }?.trim()

        if (!shared.isNullOrBlank()) {
            viewModel.handleOtherPersonSpeech(shared)
        }
    }
}

@Composable
private fun FahimniSimpleApp(viewModel: FahimniViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val partialTranscript by viewModel.speechRecognizer.partialTranscript.collectAsState()
    val audioLevel by viewModel.speechRecognizer.audioLevel.collectAsState()
    val speakingId by viewModel.voiceSpeaker.speakingId.collectAsState()

    var pendingSide by remember { mutableStateOf<TurnSide?>(null) }
    var popupSide by remember { mutableStateOf(TurnSide.OTHER_PERSON) }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val side = pendingSide
        pendingSide = null
        if (granted && side != null) {
            if (side == TurnSide.OTHER_PERSON) {
                viewModel.startDetectOtherPerson()
            } else {
                viewModel.startSpeakMyArabic()
            }
        } else if (!granted) {
            viewModel.showToast("يرجى السماح بصلاحية المايكروفون")
        }
    }

    val popupVoiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val best = res.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!best.isNullOrBlank()) {
                if (popupSide == TurnSide.OTHER_PERSON) {
                    viewModel.handleOtherPersonSpeech(best)
                } else {
                    viewModel.handleMyArabicSpeech(best)
                }
            }
        }
    }

    DisposableEffect(viewModel) {
        viewModel.onLaunchSystemVoicePopup = { intent, side ->
            popupSide = side
            runCatching { popupVoiceLauncher.launch(intent) }
        }
        onDispose {
            viewModel.onLaunchSystemVoicePopup = null
        }
    }

    BackHandler(enabled = uiState.activeListeningSide != null) {
        viewModel.cancelListening()
    }

    fun launchWithMicPermission(side: TurnSide) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            if (side == TurnSide.OTHER_PERSON) {
                viewModel.startDetectOtherPerson()
            } else {
                viewModel.startSpeakMyArabic()
            }
        } else {
            pendingSide = side
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .systemBarsPadding()
    ) {
        SimpleTranslatorScreen(
            uiState = uiState,
            partialTranscript = partialTranscript,
            audioLevel = audioLevel,
            speakingId = speakingId,
            onPressDetectOther = { launchWithMicPermission(TurnSide.OTHER_PERSON) },
            onPressSpeakArabic = { launchWithMicPermission(TurnSide.ME_ARABIC) },
            onFinishListeningNow = { viewModel.finishListeningNow() },
            onCancelListening = { viewModel.cancelListening() },
            onReplayTurn = { viewModel.replayTurnAudio(it) },
            onResetAutoDetect = { viewModel.resetToAutoDetect() },
            onManualSelectLanguage = { viewModel.setPartnerLanguageManually(it) },
            onToggleAutoSpeak = { viewModel.toggleAutoSpeak() },
            onClearScreen = { viewModel.clearScreen() },
        )
    }
}
