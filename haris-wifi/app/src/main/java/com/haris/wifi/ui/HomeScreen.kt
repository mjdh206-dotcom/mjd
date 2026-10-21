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
    val strangersActiveCount = allDevices.count { it.isStranger && !it.isBlocked }
    val visibleDevices = vm.filteredDevices()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ١. شريط العنوان العلوي
        item {
            TopHeaderBar(
                onOpenRouter = { vm.openRouterBlocker(null) },
                onToggleSettings = { showSettingsBar = !showSettingsBar },
            )
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

        // ٢. بطاقة حالة الشبكة وزر الفحص الكبير
        item {
            NetworkRadarCard(
                vm = vm,
                connectedCount = connectedCount,
                blockedCount = blockedCount,
                strangersCount = strangersActiveCount,
            )
        }

        // ٣. أزرار الفصل الجماعي السريع (فصل كل الغرباء / إعادة الجميع)
        if (strangersActiveCount > 0 || blockedCount > 0) {
            item {
                BatchActionsRow(
                    strangersActiveCount = strangersActiveCount,
                    blockedCount = blockedCount,
                    onCutAllStrangers = { vm.cutAllStrangers() },
                    onReconnectAll = { vm.reconnectAllDevices() },
                )
            }
        }

        // ٤. تبويبات التصفية والبحث السريع
        item {
            FilterAndSearchSection(
                selectedTab = vm.selectedTab,
                totalCount = allDevices.size,
                connectedCount = connectedCount,
                blockedCount = blockedCount,
                strangersCount = allDevices.count { it.isStranger },
                searchQuery = vm.searchQuery,
                onSelectTab = { vm.selectTab(it) },
                onSearchChange = { vm.updateSearch(it) },
            )
        }

        // ٥. قائمة الأجهزة المتصلة
        if (visibleDevices.isEmpty()) {
            item {
                EmptyDevicesBox(
                    selectedTab = vm.selectedTab,
                    onScanAgain = { vm.startScan() },
                )
            }
        } else {
            items(
                items = visibleDevices,
                key = { it.uniqueKey + "_" + it.ip },
            ) { device ->
                DeviceItemCard(
                    device = device,
                    onInstantCutToggle = { vm.toggleInstantCut(device) },
                    onOpenOptions = { vm.openDeviceSheet(device) },
                    onOpenRouterBlock = { vm.openRouterBlocker(device) },
                )
            }
        }

        item {
            Spacer(Modifier.height(30.dp))
        }
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
                    text = "كاشف المتصلين وفصل الاتصال بضغطة واحدة",
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
                    text = "فصل الغرباء تلقائياً عند الفحص",
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
                "وضع النظام: روت متاح (فصل مباشر + حظر الراوتر)"
            } else {
                "وضع النظام: بدون روت (إرباك الجلسة المباشر + حظر الراوتر الذكي بضغطة واحدة)"
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
                    listOf( Color(0xFF0E2238), CardSurface )
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

        // إحصائيات سريعة بثلاث بطاقات واضحة
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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

        // زر الفحص الكبير الواضح
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (vm.isScanning) CardElevated else Emerald
                )
                .clickable(enabled = !vm.isScanning) { vm.startScan() }
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (vm.isScanning) "جاري فحص الشبكة... (${(vm.scanProgress * 100).toInt()}%)"
                else "🔍  فحص الشبكة الآن واكتشاف المتصلين",
                style = MaterialTheme.typography.titleMedium,
                color = if (vm.isScanning) TextSilver else Color(0xFF032219),
                fontWeight = FontWeight.ExtraBold,
            )
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
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = accent,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TextWhite,
        )
    }
}

@Composable
private fun BatchActionsRow(
    strangersActiveCount: Int,
    blockedCount: Int,
    onCutAllStrangers: () -> Unit,
    onReconnectAll: () -> Unit,
) {
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
                    text = "✂️ فصل كل الغرباء بضغطة ($strangersActiveCount)",
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

@Composable
private fun FilterAndSearchSection(
    selectedTab: FilterTab,
    totalCount: Int,
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
                        .padding(horizontal = 14.dp, vertical = 8.dp),
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
                    "ابحث باسم الجهاز أو الـ IP أو الماركة...",
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
    onInstantCutToggle: () -> Unit,
    onOpenOptions: () -> Unit,
    onOpenRouterBlock: () -> Unit,
) {
    val borderColor = when {
        device.isBlocked -> CoralRed.copy(alpha = 0.75f)
        device.isGateway || device.isSelf -> CyberSky.copy(alpha = 0.45f)
        device.isStranger -> AmberWarn.copy(alpha = 0.45f)
        else -> Emerald.copy(alpha = 0.40f)
    }

    val containerBg = when {
        device.isBlocked -> Color(0xFF1F0D18)
        else -> CardSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(containerBg)
            .border(1.2.dp, borderColor, RoundedCornerShape(20.dp))
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

        // السطر الثاني: تفاصيل IP و MAC والسرعة
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DeepNavy)
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
                text = if (device.isBlocked) "0 KB/s" else "${device.pingMs}ms ⚡",
                style = MaterialTheme.typography.labelSmall,
                color = if (device.isBlocked) CoralBright else EmeraldBright,
            )
        }

        // إذا كان الجهاز مفصولاً نعرض سبب/حالة الفصل بوضوح
        if (device.isBlocked && device.blockMethodAr.isNotBlank()) {
            Text(
                text = "الحالة: ${device.blockMethodAr} — لا يمكنه استخدام الشبكة حالياً",
                style = MaterialTheme.typography.bodySmall,
                color = CoralBright,
            )
        }

        // السطر الثالث: أزرار التحكم المباشر السهلة جداً
        if (device.canDisconnect) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // زر الفصل / الإعادة الفوري بضغطة واحدة
                Box(
                    modifier = Modifier
                        .weight(1.6f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (device.isBlocked) Emerald else CoralRed
                        )
                        .clickable { onInstantCutToggle() }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (device.isBlocked) "↺  إعادة الاتصال الآن" else "✂️  فصل الاتصال فوراً",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (device.isBlocked) Color(0xFF032219) else TextWhite,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }

                // زر الحظر من الراوتر بضغطة واحدة
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardElevated)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .clickable { onOpenRouterBlock() }
                        .padding(horizontal = 11.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "حظر راوتر",
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberWarn,
                    )
                }

                // زر التسمية والخيارات
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardElevated)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .clickable { onOpenOptions() }
                        .padding(horizontal = 11.dp, vertical = 11.dp),
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
                FilterTab.BLOCKED -> "لا توجد أجهزة مفصولة حالياً"
                FilterTab.STRANGERS -> "ممتاز! لا توجد أجهزة غريبة على شبكتك"
                else -> "لم يتم العثور على أجهزة مطابقة"
            },
            style = MaterialTheme.typography.titleMedium,
            color = TextWhite,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(CardElevated)
                .clickable { onScanAgain() }
                .padding(horizontal = 16.dp, vertical = 9.dp),
        ) {
            Text("إعادة فحص الشبكة", color = EmeraldBright, style = MaterialTheme.typography.labelMedium)
        }
    }
}
