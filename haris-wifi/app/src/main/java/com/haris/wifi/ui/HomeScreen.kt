package com.haris.wifi.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haris.wifi.HarisViewModel
import com.haris.wifi.data.ConnectedDevice
import com.haris.wifi.data.FilterTab
import com.haris.wifi.ui.theme.AmberDim
import com.haris.wifi.ui.theme.AmberWarn
import com.haris.wifi.ui.theme.BorderSubtle
import com.haris.wifi.ui.theme.CardElevated
import com.haris.wifi.ui.theme.CardSurface
import com.haris.wifi.ui.theme.CoralBright
import com.haris.wifi.ui.theme.CoralDim
import com.haris.wifi.ui.theme.CoralRed
import com.haris.wifi.ui.theme.CyberSky
import com.haris.wifi.ui.theme.DeepNavy
import com.haris.wifi.ui.theme.Emerald
import com.haris.wifi.ui.theme.EmeraldBright
import com.haris.wifi.ui.theme.EmeraldDim
import com.haris.wifi.ui.theme.Obsidian
import com.haris.wifi.ui.theme.TextDim
import com.haris.wifi.ui.theme.TextSilver
import com.haris.wifi.ui.theme.TextWhite

@Composable
fun HomeScreen(vm: HarisViewModel) {
    var showSettingsBar by remember { mutableStateOf(false) }

    val allDevices = vm.devices
    val connectedCount = allDevices.count { !it.isBlocked }
    val blockedCount = allDevices.count { it.isBlocked }
    val camerasCount = allDevices.count { it.isCamera }
    val activeCamerasCount = allDevices.count { it.isCamera && !it.isBlocked && it.canDisconnect }
    val strangersActiveCount = allDevices.count { it.isStranger && !it.isBlocked }
    val visibleDevices = vm.filteredDevices()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ١. شريط العنوان العلوي + توقيع المبرمج Amjad omega
        item {
            TopHeaderBar(
                onOpenRouter = { vm.openRouterBlocker(null) },
                onToggleSettings = { showSettingsBar = !showSettingsBar },
            )
        }

        // شارة حقوق البرمجة البارزة
        item {
            DeveloperSignatureBanner()
        }

        // شريط الإعدادات السريعة المنسدل
        item {
            AnimatedVisibility(visible = showSettingsBar) {
                QuickSettingsCard(
                    autoCutStrangers = vm.autoCutStrangers,
                    vibrateEnabled = vm.vibrateEnabled,
                    isRootAvailable = vm.networkInfo.isRootAvailable,
                    onToggleAutoCut = { vm.toggleAutoCutStrangers(it) },
                    onToggleVibrate = { vm.toggleVibrate(it) },
                )
            }
        }

        // ٢. بطاقة حالة الشبكة وزر الفحص الشامل + زر كشف الكاميرات
        item {
            NetworkRadarCard(
                vm = vm,
                connectedCount = connectedCount,
                camerasCount = camerasCount,
                blockedCount = blockedCount,
                strangersCount = strangersActiveCount,
            )
        }

        // ٣. أزرار الفصل الجماعي السريع (فصل الكاميرات / فصل الغرباء / إعادة الجميع)
        if (activeCamerasCount > 0 || strangersActiveCount > 0 || blockedCount > 0) {
            item {
                BatchActionsSection(
                    activeCamerasCount = activeCamerasCount,
                    strangersActiveCount = strangersActiveCount,
                    blockedCount = blockedCount,
                    onCutAllCameras = { vm.cutAllCameras() },
                    onCutAllStrangers = { vm.cutAllStrangers() },
                    onReconnectAll = { vm.reconnectAllDevices() },
                )
            }
        }

        // ٤. تبويبات التصفية والبحث السريع (بما فيها تبويب الكاميرات 📷)
        item {
            FilterAndSearchSection(
                selectedTab = vm.selectedTab,
                totalCount = allDevices.size,
                camerasCount = camerasCount,
                connectedCount = connectedCount,
                blockedCount = blockedCount,
                strangersCount = allDevices.count { it.isStranger },
                searchQuery = vm.searchQuery,
                onSelectTab = { vm.selectTab(it) },
                onSearchChange = { vm.updateSearch(it) },
            )
        }

        // ٥. قائمة الأجهزة والكاميرات المتصلة
        if (visibleDevices.isEmpty()) {
            item {
                EmptyDevicesBox(
                    selectedTab = vm.selectedTab,
                    onScanAgain = { vm.startScan(deepCameraScan = false) },
                    onDeepCamScan = { vm.startDeepCameraScan() },
                )
            }
        } else {
            items(
                items = visibleDevices,
                key = { it.uniqueKey + "_" + it.ip },
            ) { device ->
                val packetsSent = if (device.isBlocked) vm.getCutPacketsFor(device) else 0L
                DeviceItemCard(
                    device = device,
                    cutPacketsSent = packetsSent,
                    onInstantCutToggle = { vm.toggleInstantCut(device) },
                    onOpenOptions = { vm.openDeviceSheet(device) },
                    onOpenRouterBlock = { vm.openRouterBlocker(device) },
                )
            }
        }

        // تذييل حقوق المبرمج في أسفل الشاشة
        item {
            Spacer(Modifier.height(8.dp))
            FooterDeveloperCredit()
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun DeveloperSignatureBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF0A2E26), Color(0xFF13233F), Color(0xFF2B1B06))
                )
            )
            .border(1.dp, EmeraldBright.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(vertical = 9.dp, horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "✨ تمت برمجة هذا التطبيق بواسطة Amjad omega ✨",
            style = MaterialTheme.typography.titleSmall,
            color = AmberWarn,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FooterDeveloperCredit() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "تمت برمجة هذا التطبيق بواسطة Amjad omega",
            style = MaterialTheme.typography.titleSmall,
            color = EmeraldBright,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = "حارس الشبكة — كاشف المتصلين والكاميرات وقاطع الاتصال الفوري",
            style = MaterialTheme.typography.bodySmall,
            color = TextSilver,
        )
    }
}

@Composable
private fun TopHeaderBar(
    onOpenRouter: () -> Unit,
    onToggleSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(listOf(Emerald, EmeraldDim))
                    )
                    .border(1.dp, EmeraldBright.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("🛡️", fontSize = 22.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "حارس الشبكة",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                )
                Text(
                    text = "كاشف المتصلين والكاميرات وقاطع الاتصال",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSilver,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .clickable { onOpenRouter() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "📡 الراوتر",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberSky,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .clickable { onToggleSettings() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "⚙️",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextWhite,
                )
            }
        }
    }
}

@Composable
private fun QuickSettingsCard(
    autoCutStrangers: Boolean,
    vibrateEnabled: Boolean,
    isRootAvailable: Boolean,
    onToggleAutoCut: (Boolean) -> Unit,
    onToggleVibrate: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "فصل الغرباء والكاميرات تلقائياً عند الفحص",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextWhite,
                )
                Text(
                    text = "أي جهاز غير محفوظ كـ«موثوق» يتم فصله تلقائياً",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSilver,
                )
            }
            Switch(
                checked = autoCutStrangers,
                onCheckedChange = onToggleAutoCut,
                colors = SwitchDefaults.colors(checkedThumbColor = TextWhite, checkedTrackColor = CoralRed),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "الاهتزاز عند الضغط",
                style = MaterialTheme.typography.titleSmall,
                color = TextWhite,
            )
            Switch(
                checked = vibrateEnabled,
                onCheckedChange = onToggleVibrate,
                colors = SwitchDefaults.colors(checkedThumbColor = TextWhite, checkedTrackColor = Emerald),
            )
        }

        Text(
            text = if (isRootAvailable) {
                "وضع القوة: روت مفعّل (قطع ARP Spoof كامل + إغراق RTSP/TCP + حظر الراوتر)"
            } else {
                "وضع القوة: هجوم مزدوج نشط (12 خط إغراق RTSP/TCP/UDP + حظر الراوتر الذكي)"
            },
            style = MaterialTheme.typography.labelSmall,
            color = EmeraldBright,
        )
    }
}

@Composable
private fun NetworkRadarCard(
    vm: HarisViewModel,
    connectedCount: Int,
    camerasCount: Int,
    blockedCount: Int,
    strangersCount: Int,
) {
    val net = vm.networkInfo
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0E2238), CardSurface)
                )
            )
            .border(1.dp, Emerald.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(EmeraldBright)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = net.ssid,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "الراوتر: ${net.gatewayIp}  ·  هاتفك: ${net.localIp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSilver,
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldDim.copy(alpha = 0.7f))
                    .border(1.dp, Emerald.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "${net.signalPercent}% 📶",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldBright,
                )
            }
        }

        // ٤ إحصائيات واضحة تشمل الكاميرات
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatBadgeBox(
                modifier = Modifier.weight(1f),
                title = "متصل الآن",
                value = connectedCount.toString(),
                accent = EmeraldBright,
                bg = EmeraldDim.copy(alpha = 0.45f),
            )
            StatBadgeBox(
                modifier = Modifier.weight(1f),
                title = "📷 كاميرات",
                value = camerasCount.toString(),
                accent = CyberSky,
                bg = Color(0xFF0C2D48),
            )
            StatBadgeBox(
                modifier = Modifier.weight(1f),
                title = "مفصول ⛔",
                value = blockedCount.toString(),
                accent = CoralBright,
                bg = CoralDim.copy(alpha = 0.45f),
            )
            StatBadgeBox(
                modifier = Modifier.weight(1f),
                title = "غرباء ⚠️",
                value = strangersCount.toString(),
                accent = AmberWarn,
                bg = AmberDim.copy(alpha = 0.45f),
            )
        }

        if (vm.isScanning) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { vm.scanProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(99.dp)),
                    color = EmeraldBright,
                    trackColor = DeepNavy,
                )
                Text(
                    text = vm.scanStatusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldBright,
                )
            }
        } else {
            Text(
                text = vm.scanStatusText,
                style = MaterialTheme.typography.bodySmall,
                color = TextSilver,
            )
        }

        // زران للفحص: فحص شامل + فحص عميق للكاميرات
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (vm.isScanning) CardElevated else Emerald)
                    .clickable(enabled = !vm.isScanning) { vm.startScan(deepCameraScan = false) }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (vm.isScanning) "جاري الفحص (${(vm.scanProgress * 100).toInt()}%)"
                    else "🔍 فحص الشبكة الآن",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (vm.isScanning) TextSilver else Color(0xFF032219),
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (vm.isScanning) CardElevated else Color(0xFF0C2D48))
                    .border(1.dp, CyberSky.copy(alpha = 0.6f), RoundedCornerShape(15.dp))
                    .clickable(enabled = !vm.isScanning) { vm.startDeepCameraScan() }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "📷 كشف الكاميرات",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyberSky,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun StatBadgeBox(
    modifier: Modifier,
    title: String,
    value: String,
    accent: Color,
    bg: Color,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(vertical = 9.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = accent,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TextWhite,
            maxLines = 1,
        )
    }
}

@Composable
private fun BatchActionsSection(
    activeCamerasCount: Int,
    strangersActiveCount: Int,
    blockedCount: Int,
    onCutAllCameras: () -> Unit,
    onCutAllStrangers: () -> Unit,
    onReconnectAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (activeCamerasCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF3B0A26))
                    .border(1.2.dp, CoralBright, RoundedCornerShape(14.dp))
                    .clickable { onCutAllCameras() }
                    .padding(vertical = 11.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "📷✂️  فصل وإيقاف بث كل الكاميرات بضغطة واحدة ($activeCamerasCount)",
                    style = MaterialTheme.typography.labelLarge,
                    color = CoralBright,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (strangersActiveCount > 0) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CoralRed.copy(alpha = 0.18f))
                        .border(1.dp, CoralRed.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                        .clickable { onCutAllStrangers() }
                        .padding(vertical = 11.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "✂️ فصل كل الغرباء ($strangersActiveCount)",
                        style = MaterialTheme.typography.labelLarge,
                        color = CoralBright,
                    )
                }
            }

            if (blockedCount > 0) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Emerald.copy(alpha = 0.16f))
                        .border(1.dp, Emerald.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        .clickable { onReconnectAll() }
                        .padding(vertical = 11.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "↺ إعادة الاتصال للجميع ($blockedCount)",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldBright,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterAndSearchSection(
    selectedTab: FilterTab,
    totalCount: Int,
    camerasCount: Int,
    connectedCount: Int,
    blockedCount: Int,
    strangersCount: Int,
    searchQuery: String,
    onSelectTab: (FilterTab) -> Unit,
    onSearchChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(FilterTab.entries) { tab ->
                val count = when (tab) {
                    FilterTab.ALL -> totalCount
                    FilterTab.CAMERAS -> camerasCount
                    FilterTab.CONNECTED -> connectedCount
                    FilterTab.BLOCKED -> blockedCount
                    FilterTab.STRANGERS -> strangersCount
                }
                val active = selectedTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) Emerald else CardSurface)
                        .border(
                            1.dp,
                            if (active) EmeraldBright else BorderSubtle,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 13.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "${tab.labelAr} ($count)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (active) Color(0xFF032219) else TextWhite,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(
                    "ابحث باسم الجهاز أو الكاميرا أو الـ IP أو الماركة...",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim,
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald,
                unfocusedBorderColor = BorderSubtle,
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
            ),
        )
    }
}

@Composable
private fun DeviceItemCard(
    device: ConnectedDevice,
    cutPacketsSent: Long,
    onInstantCutToggle: () -> Unit,
    onOpenOptions: () -> Unit,
    onOpenRouterBlock: () -> Unit,
) {
    val borderColor = when {
        device.isBlocked -> CoralRed.copy(alpha = 0.85f)
        device.isCamera -> CyberSky.copy(alpha = 0.75f)
        device.isGateway || device.isSelf -> CyberSky.copy(alpha = 0.45f)
        device.isStranger -> AmberWarn.copy(alpha = 0.45f)
        else -> Emerald.copy(alpha = 0.40f)
    }

    val containerBg = when {
        device.isBlocked -> Color(0xFF210B16)
        device.isCamera -> Color(0xFF0D2036)
        else -> CardSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(containerBg)
            .border(1.3.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onOpenOptions() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // السطر الأول: الأيقونة + الاسم + شارة الحالة
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (device.isBlocked) CoralDim else CardElevated
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (device.isBlocked) "⛔" else device.deviceType.emoji,
                        fontSize = 22.sp,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = device.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (device.isBlocked) CoralBright else TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${device.vendorAr} · ${device.deviceType.labelAr}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSilver,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            StatusPillBadge(device = device)
        }

        // السطر الثاني: تفاصيل IP و MAC والسرعة والمنافذ
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DeepNavy)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "IP: ${device.ip}",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberSky,
                )
                Text(
                    text = "MAC: ${device.mac}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSilver,
                )
                Text(
                    text = if (device.isBlocked) "مقطوع ⛔" else "${device.pingMs}ms ⚡",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (device.isBlocked) CoralBright else EmeraldBright,
                )
            }

            if (device.isCamera || device.openPorts.isNotEmpty()) {
                val portsText = if (device.openPorts.isNotEmpty()) {
                    device.openPorts.joinToString(", ")
                } else "554, 8000"
                Text(
                    text = if (device.isCamera) {
                        "📷 كاميرا مكتشفة (${device.cameraProtocol.ifBlank { "RTSP/ONVIF" }}) · المنافذ: $portsText"
                    } else {
                        "المنافذ النشطة: $portsText"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (device.isCamera) CyberSky else TextSilver,
                )
            }
        }

        // إذا كان الجهاز مفصولاً نعرض عداد حزم القطع الحي + زر القفل النهائي من الراوتر
        if (device.isBlocked) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CoralDim.copy(alpha = 0.55f))
                    .border(1.dp, CoralRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "🔥 ${device.blockMethodAr.ifBlank { "هجوم القطع نشط الآن" }}",
                    style = MaterialTheme.typography.labelMedium,
                    color = CoralBright,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "حزم القطع وإغراق الجلسة المرسلة: $cutPacketsSent حزمة  ·  للقفل النهائي ١٠٠٪ من الراوتر اضغط «قفل راوتر» أدناه",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextWhite,
                )
            }
        }

        // السطر الثالث: أزرار التحكم المباشر
        if (device.canDisconnect) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (device.isBlocked) Emerald else CoralRed
                        )
                        .clickable { onInstantCutToggle() }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when {
                            device.isBlocked -> "↺  إعادة الاتصال الآن"
                            device.isCamera -> "✂️  قطع بث الكاميرا فوراً"
                            else -> "✂️  فصل الاتصال فوراً"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (device.isBlocked) Color(0xFF032219) else TextWhite,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(AmberDim)
                        .border(1.dp, AmberWarn.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        .clickable { onOpenRouterBlock() }
                        .padding(horizontal = 11.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "🔒 قفل راوتر",
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberWarn,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardElevated)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .clickable { onOpenOptions() }
                        .padding(horizontal = 10.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "تسمية",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextWhite,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPillBadge(device: ConnectedDevice) {
    val (label, fg, bg) = when {
        device.isBlocked -> Triple("مفصول ⛔", CoralBright, CoralDim)
        device.isCamera -> Triple("كاميرا 📷", CyberSky, Color(0xFF0C2D48))
        device.isGateway -> Triple("الراوتر 📡", CyberSky, Color(0xFF0C2D48))
        device.isSelf -> Triple("هاتفك 🛡️", EmeraldBright, EmeraldDim)
        device.isTrusted -> Triple("موثوق ✓", EmeraldBright, EmeraldDim)
        else -> Triple("غريب ⚠️", AmberWarn, AmberDim)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EmptyDevicesBox(
    selectedTab: FilterTab,
    onScanAgain: () -> Unit,
    onDeepCamScan: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = when (selectedTab) {
                FilterTab.CAMERAS -> "لم تظهر كاميرات في الفحص السريع — اضغط «فحص عميق للكاميرات»"
                FilterTab.BLOCKED -> "لا توجد أجهزة مفصولة حالياً"
                FilterTab.STRANGERS -> "ممتاز! لا توجد أجهزة غريبة على شبكتك"
                else -> "لم يتم العثور على أجهزة مطابقة"
            },
            style = MaterialTheme.typography.titleSmall,
            color = TextWhite,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Emerald)
                    .clickable { onDeepCamScan() }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text("📷 فحص عميق للكاميرات", color = Color(0xFF032219), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardElevated)
                    .clickable { onScanAgain() }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text("إعادة فحص الشبكة", color = EmeraldBright, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
