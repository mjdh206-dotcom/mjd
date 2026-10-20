package com.shuaa.remote

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.shuaa.remote.ui.BrandPickerScreen
import com.shuaa.remote.ui.HomeScreen
import com.shuaa.remote.ui.MatchScreen
import com.shuaa.remote.ui.RemoteScreen
import com.shuaa.remote.ui.ScanScreen
import com.shuaa.remote.ui.SettingsScreen
import com.shuaa.remote.ui.theme.Gold
import com.shuaa.remote.ui.theme.Ink
import com.shuaa.remote.ui.theme.ShuaaTheme
import com.shuaa.remote.ui.theme.SurfaceTwo
import com.shuaa.remote.ui.theme.TextPrimary

/** شاشات التنقل */
private sealed interface Route {
    data object Home : Route
    data object Scan : Route
    data class Brands(val fromScan: Boolean = false) : Route
    data class Match(val brandId: String) : Route
    data class Remote(val deviceId: String) : Route
    data object Settings : Route
}

class MainActivity : ComponentActivity() {

    private val vm: ShuaaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val openRemote = intent?.getBooleanExtra(EXTRA_OPEN_REMOTE, false) == true

        setContent {
            ShuaaTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Ink)
                            .systemBarsPadding()
                    ) {
                        ShuaaApp(vm = vm, openRemote = openRemote)
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_REMOTE = "open_remote"
    }
}

@Composable
private fun ShuaaApp(vm: ShuaaViewModel, openRemote: Boolean) {
    val stack = remember { mutableStateListOf<Route>(Route.Home) }
    val current = stack.lastOrNull() ?: Route.Home

    fun navigate(route: Route) {
        if (stack.lastOrNull() != route) stack.add(route)
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    // فتح الريموت الافتراضي عند الدخول من بلاطة الإعدادات السريعة
    LaunchedEffect(openRemote) {
        if (openRemote) {
            vm.primaryDevice()?.let { navigate(Route.Remote(it.id)) }
        }
    }

    BackHandler(enabled = stack.size > 1) { back() }

    Box(Modifier.fillMaxSize()) {
        when (current) {
            is Route.Home -> HomeScreen(
                vm = vm,
                onScan = { navigate(Route.Scan) },
                onBrowseBrands = { navigate(Route.Brands()) },
                onSelectBrand = { id -> navigate(Route.Match(id)) },
                onOpenDevice = { id -> navigate(Route.Remote(id)) },
                onOpenSettings = { navigate(Route.Settings) },
            )

            is Route.Scan -> ScanScreen(
                onClose = { back() },
                onContinue = { navigate(Route.Brands(fromScan = true)) },
                onManual = { navigate(Route.Brands()) },
            )

            is Route.Brands -> BrandPickerScreen(
                brands = vm.brands,
                fromScan = current.fromScan,
                onBack = { back() },
                onChoose = { brand -> navigate(Route.Match(brand.id)) },
            )

            is Route.Match -> {
                val brand = vm.brands.firstOrNull { it.id == current.brandId }
                if (brand == null) {
                    LaunchedEffect(current.brandId) { back() }
                } else {
                    MatchScreen(
                        brand = brand,
                        vm = vm,
                        onBack = { back() },
                        onSaved = { device ->
                            vm.refreshDevices()
                            navigate(Route.Remote(device.id))
                        },
                    )
                }
            }

            is Route.Remote -> {
                val device = vm.devices.firstOrNull { it.id == current.deviceId }
                if (device == null) {
                    LaunchedEffect(current.deviceId) { back() }
                } else {
                    RemoteScreen(
                        device = device,
                        vm = vm,
                        onBack = { back() },
                        onDeleted = {
                            vm.refreshDevices()
                            back()
                        },
                        onBrowseBrands = { navigate(Route.Brands()) },
                    )
                }
            }

            is Route.Settings -> SettingsScreen(vm = vm, onBack = { back() })
        }

        // شريط الرسائل السريعة
        AnimatedVisibility(
            visible = vm.banner != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 22.dp),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            val message = vm.banner ?: ""
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceTwo)
                    .border(1.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Gold.copy(alpha = 0.10f), Color.Transparent)
                        )
                    )
                    .clickable { vm.dismissBanner() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                )
            }
        }
    }
}
