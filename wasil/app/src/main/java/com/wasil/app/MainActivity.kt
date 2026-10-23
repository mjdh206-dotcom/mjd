package com.wasil.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wasil.app.data.AppMode
import com.wasil.app.ui.HomeScreen
import com.wasil.app.ui.InfoScreen
import com.wasil.app.ui.MessagesScreen
import com.wasil.app.ui.SetupScreen
import com.wasil.app.ui.SettingsScreen
import com.wasil.app.ui.theme.Gold
import com.wasil.app.ui.theme.Ink
import com.wasil.app.ui.theme.SurfaceTwo
import com.wasil.app.ui.theme.TextPrimary
import com.wasil.app.ui.theme.WasilTheme
import com.wasil.app.work.MessagePollWorker

/** شاشات التنقل */
private sealed interface Route {
    data object Setup : Route
    data object Home : Route
    data object Messages : Route
    data object Settings : Route
    data object Info : Route
}

class MainActivity : ComponentActivity() {

    private val vm: WasilViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        MessagePollWorker.schedule(this)

        setContent {
            WasilTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Ink)
                            .systemBarsPadding()
                    ) {
                        WasilApp(vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun WasilApp(vm: WasilViewModel) {
    val stack = remember {
        mutableStateListOf<Route>(if (vm.needsSetup) Route.Setup else Route.Home)
    }
    val current = stack.lastOrNull() ?: Route.Home
    val config by vm.config.collectAsState()
    val banner by vm.banner.collectAsState()

    // طلب إذن الإشعارات على أندرويد 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* القبول أو الرفض لا يوقفان التطبيق */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (config.mode == AppMode.LIVE) {
            vm.refresh(showFeedback = false)
            vm.restartPolling()
        }
    }

    fun navigate(route: Route) {
        if (stack.lastOrNull() != route) stack.add(route)
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    Box(Modifier.fillMaxSize()) {
        when (current) {
            Route.Setup -> SetupScreen(vm = vm) {
                vm.completeSetup()
                stack.clear()
                stack.add(Route.Home)
            }

            Route.Home -> HomeScreen(
                vm = vm,
                onOpenMessages = { navigate(Route.Messages) },
                onOpenSettings = { navigate(Route.Settings) },
                onOpenInfo = { navigate(Route.Info) },
            )

            Route.Messages -> MessagesScreen(vm = vm, onBack = { back() })
            Route.Settings -> SettingsScreen(
                vm = vm,
                onBack = { back() },
                onOpenInfo = { navigate(Route.Info) },
            )
            Route.Info -> InfoScreen(onBack = { back() })
        }

        AnimatedVisibility(
            visible = banner != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 22.dp),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            val message = banner ?: ""
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceTwo)
                    .border(1.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .clickable { vm.dismissBanner() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(message, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
        }
    }
}
