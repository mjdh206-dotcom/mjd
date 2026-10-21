package com.haris.wifi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.haris.wifi.ui.DeviceActionSheet
import com.haris.wifi.ui.HomeScreen
import com.haris.wifi.ui.RouterBlockerScreen
import com.haris.wifi.ui.theme.CardElevated
import com.haris.wifi.ui.theme.EmeraldBright
import com.haris.wifi.ui.theme.HarisTheme
import com.haris.wifi.ui.theme.Obsidian
import com.haris.wifi.ui.theme.TextWhite

class MainActivity : ComponentActivity() {

    private val vm: HarisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            HarisTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Obsidian)
                            .systemBarsPadding()
                    ) {
                        HarisAppContent(vm = vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun HarisAppContent(vm: HarisViewModel) {
    BackHandler(enabled = vm.showRouterScreen || vm.selectedDeviceForSheet != null) {
        when {
            vm.selectedDeviceForSheet != null -> vm.closeDeviceSheet()
            vm.showRouterScreen -> vm.closeRouterBlocker()
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (vm.showRouterScreen) {
            RouterBlockerScreen(
                vm = vm,
                targetDevice = vm.routerBlockerDevice,
                onBack = { vm.closeRouterBlocker() },
            )
        } else {
            HomeScreen(vm = vm)
        }

        val sheetDevice = vm.selectedDeviceForSheet
        if (sheetDevice != null) {
            DeviceActionSheet(
                device = sheetDevice,
                vm = vm,
                onDismiss = { vm.closeDeviceSheet() },
            )
        }

        // شريط الإشعارات السريعة في الأسفل
        AnimatedVisibility(
            visible = vm.bannerMessage != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            val msg = vm.bannerMessage ?: ""
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardElevated)
                    .border(1.dp, EmeraldBright.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .clickable { vm.dismissBanner() }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite,
                )
            }
        }
    }
}
