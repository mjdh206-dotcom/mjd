package com.shuaa.remote.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shuaa.remote.R
import com.shuaa.remote.ShuaaViewModel
import com.shuaa.remote.data.Buttons
import com.shuaa.remote.data.SavedDevice
import com.shuaa.remote.ui.components.BeamBackground
import com.shuaa.remote.ui.components.GlassCard
import com.shuaa.remote.ui.components.Pill
import com.shuaa.remote.ui.components.RemoteKey
import com.shuaa.remote.ui.theme.Coral
import com.shuaa.remote.ui.theme.Gold
import com.shuaa.remote.ui.theme.Hairline
import com.shuaa.remote.ui.theme.Mint
import com.shuaa.remote.ui.theme.Sky
import com.shuaa.remote.ui.theme.SurfaceOne
import com.shuaa.remote.ui.theme.SurfaceTwo
import com.shuaa.remote.ui.theme.TextMuted
import com.shuaa.remote.ui.theme.TextPrimary

@Composable
fun RemoteScreen(
    device: SavedDevice,
    vm: ShuaaViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onBrowseBrands: () -> Unit,
) {
    val view = LocalView.current
    var showDigits by remember { mutableStateOf(false) }
    var showExtra by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var renameValue by remember(device.id) { mutableStateOf(device.name) }
    var showRename by remember { mutableStateOf(false) }

    fun has(key: String) = device.codes.containsKey(key)

    fun tap(key: String) {
        vm.send(device, key, view)
    }

    @Composable
    fun interactive(
        key: String,
        label: String,
        modifier: Modifier = Modifier,
        icon: Int? = null,
        subLabel: String? = null,
        size: androidx.compose.ui.unit.Dp = 60.dp,
        shape: RoundedCornerShape = RoundedCornerShape(18.dp),
        accent: Color = Gold,
        fontSize: Int = 12,
    ) {
        val enabled = has(key)
        if (key in Buttons.repeatable) {
            RemoteKey(
                label = label,
                onPress = {},
                onPressStart = { if (enabled) vm.startHold(device, key, view) },
                onPressEnd = { vm.stopHold() },
                modifier = modifier,
                icon = icon,
                subLabel = subLabel,
                size = size,
                shape = shape,
                enabled = enabled,
                accent = accent,
                active = vm.activeKey == key,
                fontSize = fontSize,
            )
        } else {
            RemoteKey(
                label = label,
                onPress = { if (enabled) tap(key) },
                modifier = modifier,
                icon = icon,
                subLabel = subLabel,
                size = size,
                shape = shape,
                enabled = enabled,
                accent = accent,
                active = vm.activeKey == key,
                fontSize = fontSize,
            )
        }
    }

    BeamBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 14.dp, bottom = 26.dp),
        ) {
            /* ---------- الترويسة ---------- */
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceTwo)
                        .border(1.dp, Hairline, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = "رجوع",
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(TextPrimary),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            device.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            maxLines = 1,
                        )
                        if (device.isPrimary) {
                            Spacer(Modifier.width(8.dp))
                            Pill("افتراضي", Mint, filled = true)
                        }
                    }
                    Text(
                        "${device.brandAr} · ${device.codes.size} زر جاهز",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceTwo)
                        .border(1.dp, Hairline, CircleShape)
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⋯", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))

            /* ---------- صف الطاقة ---------- */
            GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Coral.copy(alpha = 0.38f), SurfaceOne)
                                )
                            )
                            .border(1.4.dp, Coral.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        interactive(
                            key = Buttons.POWER,
                            label = "الطاقة",
                            icon = R.drawable.ic_power,
                            size = 74.dp,
                            shape = RoundedCornerShape(37.dp),
                            accent = Coral,
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniKey(
                            label = "المصدر",
                            icon = R.drawable.ic_input,
                            enabled = has(Buttons.SOURCE),
                            active = vm.activeKey == Buttons.SOURCE,
                        ) { tap(Buttons.SOURCE) }
                        MiniKey(
                            label = "كتم الصوت",
                            icon = R.drawable.ic_mute,
                            enabled = has(Buttons.MUTE),
                            active = vm.activeKey == Buttons.MUTE,
                        ) { tap(Buttons.MUTE) }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            /* ---------- الأسهم + الصوت + القنوات ---------- */
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DpadPanel(
                    modifier = Modifier.weight(1f),
                    hasUp = has(Buttons.UP),
                    hasDown = has(Buttons.DOWN),
                    hasLeft = has(Buttons.LEFT),
                    hasRight = has(Buttons.RIGHT),
                    hasOk = has(Buttons.OK),
                    activeKey = vm.activeKey,
                    onUpStart = { vm.startHold(device, Buttons.UP, view) },
                    onDownStart = { vm.startHold(device, Buttons.DOWN, view) },
                    onLeftStart = { vm.startHold(device, Buttons.LEFT, view) },
                    onRightStart = { vm.startHold(device, Buttons.RIGHT, view) },
                    onStop = { vm.stopHold() },
                    onOk = { tap(Buttons.OK) },
                )

                Column(
                    modifier = Modifier.width(88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Rocker(
                        title = "الصوت",
                        upEnabled = has(Buttons.VOL_UP),
                        downEnabled = has(Buttons.VOL_DN),
                        upActive = vm.activeKey == Buttons.VOL_UP,
                        downActive = vm.activeKey == Buttons.VOL_DN,
                        onUpStart = { vm.startHold(device, Buttons.VOL_UP, view) },
                        onDownStart = { vm.startHold(device, Buttons.VOL_DN, view) },
                        onStop = { vm.stopHold() },
                    )
                    Rocker(
                        title = "القناة",
                        upEnabled = has(Buttons.CH_UP),
                        downEnabled = has(Buttons.CH_DN),
                        upActive = vm.activeKey == Buttons.CH_UP,
                        downActive = vm.activeKey == Buttons.CH_DN,
                        onUpStart = { vm.startHold(device, Buttons.CH_UP, view) },
                        onDownStart = { vm.startHold(device, Buttons.CH_DN, view) },
                        onStop = { vm.stopHold() },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            /* ---------- صف التنقل ---------- */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NavKey("رجوع", R.drawable.ic_chevron_left, has(Buttons.BACK), vm.activeKey == Buttons.BACK, Modifier.weight(1f)) { tap(Buttons.BACK) }
                NavKey("القائمة", R.drawable.ic_remote, has(Buttons.MENU), vm.activeKey == Buttons.MENU, Modifier.weight(1f)) { tap(Buttons.MENU) }
                NavKey("الرئيسية", R.drawable.ic_tv, has(Buttons.HOME), vm.activeKey == Buttons.HOME, Modifier.weight(1f)) { tap(Buttons.HOME) }
                NavKey("خروج", R.drawable.ic_close, has(Buttons.EXIT), vm.activeKey == Buttons.EXIT, Modifier.weight(1f)) { tap(Buttons.EXIT) }
            }

            Spacer(Modifier.height(16.dp))

            /* ---------- لوحة الأرقام ---------- */
            ToggleCard(
                title = "لوحة الأرقام",
                subtitle = "لإدخال القنوات مباشرة",
                expanded = showDigits,
                onToggle = { showDigits = !showDigits },
            )
            AnimatedVisibility(visible = showDigits) {
                Column(Modifier.padding(top = 10.dp)) {
                    val rows = listOf(
                        listOf("key1", "key2", "key3"),
                        listOf("key4", "key5", "key6"),
                        listOf("key7", "key8", "key9"),
                        listOf("dash", "key0", "source"),
                    )
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { key ->
                                val label = when (key) {
                                    "dash" -> "—"
                                    "source" -> "المصدر"
                                    else -> key.removePrefix("key")
                                }
                                RemoteKey(
                                    label = label,
                                    onPress = { tap(key) },
                                    modifier = Modifier.weight(1f),
                                    size = 54.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    enabled = has(key),
                                    accent = Sky,
                                    active = vm.activeKey == key,
                                    fontSize = if (key == "source") 11 else 18,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            /* ---------- أزرار إضافية ---------- */
            val extras = listOf(
                Buttons.INFO, Buttons.GUIDE, Buttons.LIST, Buttons.FAV,
                Buttons.PLAY, Buttons.PAUSE, Buttons.STOP, Buttons.REW,
                Buttons.FFW, Buttons.SUBTITLE, Buttons.ASPECT, Buttons.AUDIO,
                Buttons.SLEEP, Buttons.REC, Buttons.PREV, Buttons.NEXT,
            ).filter { has(it) }

            val colors = listOf(
                Buttons.RED to Coral,
                Buttons.GREEN to Mint,
                Buttons.YELLOW to Gold,
                Buttons.BLUE to Sky,
            ).filter { has(it.first) }

            if (extras.isNotEmpty() || colors.isNotEmpty()) {
                ToggleCard(
                    title = "أزرار إضافية",
                    subtitle = "${extras.size + colors.size} زر متاح لهذا الريموت",
                    expanded = showExtra,
                    onToggle = { showExtra = !showExtra },
                )
                AnimatedVisibility(visible = showExtra) {
                    Column(Modifier.padding(top = 10.dp)) {
                        if (colors.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                colors.forEach { (key, color) ->
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(color.copy(alpha = 0.22f))
                                            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                                            .clickable { tap(key) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            Buttons.label(key),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = color,
                                        )
                                    }
                                }
                            }
                        }
                        extras.chunked(4).forEach { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEach { key ->
                                    RemoteKey(
                                        label = Buttons.label(key),
                                        onPress = { tap(key) },
                                        modifier = Modifier.weight(1f),
                                        size = 62.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        accent = Sky,
                                        active = vm.activeKey == key,
                                        fontSize = 10,
                                    )
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            /* ---------- تلميح ---------- */
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceOne)
                    .border(1.dp, Hairline, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (vm.irAvailable && !vm.settings.demoMode) Mint else Coral)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (vm.irAvailable && !vm.settings.demoMode)
                        "وجّه الهاتف نحو الشاشة · المدى الفعّال حتى ٨ أمتار"
                    else
                        "وضع المعاينة — الإرسال يعمل على هاتف يحتوي مُرسِل أشعة تحت الحمراء",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }
    }

    /* ---------- قائمة الخيارات ---------- */
    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            containerColor = SurfaceOne,
            titleContentColor = TextPrimary,
            textContentColor = TextMuted,
            title = { Text("خيارات الريموت", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MenuRow("إعادة التسمية") {
                        showMenu = false
                        showRename = true
                    }
                    MenuRow("تعيين كافتراضي") {
                        vm.setPrimary(device.id)
                        showMenu = false
                    }
                    MenuRow("تبديل الماركة / إعادة المطابقة") {
                        showMenu = false
                        onBrowseBrands()
                    }
                    MenuRow("حذف الجهاز", danger = true) {
                        vm.delete(device.id)
                        showMenu = false
                        onDeleted()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMenu = false }) {
                    Text("إغلاق", color = Gold)
                }
            },
        )
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            containerColor = SurfaceOne,
            titleContentColor = TextPrimary,
            title = { Text("اسم الجهاز") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    singleLine = true,
                    placeholder = { Text("مثال: تلفزيون الصالون", color = TextMuted) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.rename(device.id, renameValue)
                    showRename = false
                }) { Text("حفظ", color = Gold) }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) {
                    Text("إلغاء", color = TextMuted)
                }
            },
        )
    }
}

@Composable
private fun MenuRow(text: String, danger: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (danger) Coral else TextPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
    )
}

@Composable
private fun MiniKey(
    label: String,
    icon: Int,
    enabled: Boolean,
    active: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) Gold.copy(alpha = 0.14f) else SurfaceTwo)
            .border(1.dp, if (active) Gold.copy(alpha = 0.6f) else Hairline, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            colorFilter = ColorFilter.tint(if (enabled) Gold.copy(alpha = 0.9f) else TextMuted.copy(alpha = 0.5f)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) TextPrimary else TextMuted.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun NavKey(
    label: String,
    icon: Int,
    enabled: Boolean,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) Gold.copy(alpha = 0.12f) else SurfaceOne)
            .border(1.dp, if (active) Gold.copy(alpha = 0.55f) else Hairline, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(19.dp),
            colorFilter = ColorFilter.tint(if (enabled) TextPrimary else TextMuted.copy(alpha = 0.45f)),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = if (enabled) TextPrimary else TextMuted.copy(alpha = 0.5f),
            maxLines = 1,
        )
    }
}

@Composable
private fun ToggleCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceOne)
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        Image(
            painter = painterResource(if (expanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(TextMuted),
        )
    }
}

/** لوحة الأسهم الدائرية */
@Composable
private fun DpadPanel(
    modifier: Modifier = Modifier,
    hasUp: Boolean,
    hasDown: Boolean,
    hasLeft: Boolean,
    hasRight: Boolean,
    hasOk: Boolean,
    activeKey: String?,
    onUpStart: () -> Unit,
    onDownStart: () -> Unit,
    onLeftStart: () -> Unit,
    onRightStart: () -> Unit,
    onStop: () -> Unit,
    onOk: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.radialGradient(
                    listOf(SurfaceTwo.copy(alpha = 0.85f), SurfaceOne)
                )
            )
            .border(1.dp, Hairline, RoundedCornerShape(26.dp))
            .padding(12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            RemoteKey(
                label = "أعلى",
                onPress = {},
                onPressStart = { if (hasUp) onUpStart() },
                onPressEnd = onStop,
                icon = R.drawable.ic_chevron_up,
                size = 52.dp,
                shape = RoundedCornerShape(16.dp),
                enabled = hasUp,
                active = activeKey == Buttons.UP,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RemoteKey(
                    label = "يسار",
                    onPress = {},
                    onPressStart = { if (hasLeft) onLeftStart() },
                    onPressEnd = onStop,
                    icon = R.drawable.ic_chevron_left,
                    size = 52.dp,
                    shape = RoundedCornerShape(16.dp),
                    enabled = hasLeft,
                    active = activeKey == Buttons.LEFT,
                )
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Gold.copy(alpha = 0.26f), SurfaceTwo)
                            )
                        )
                        .border(1.2.dp, Gold.copy(alpha = 0.55f), CircleShape)
                        .clickable(enabled = hasOk, onClick = onOk),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "OK",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (hasOk) Gold else TextMuted,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                RemoteKey(
                    label = "يمين",
                    onPress = {},
                    onPressStart = { if (hasRight) onRightStart() },
                    onPressEnd = onStop,
                    icon = R.drawable.ic_chevron_left,
                    size = 52.dp,
                    shape = RoundedCornerShape(16.dp),
                    enabled = hasRight,
                    active = activeKey == Buttons.RIGHT,
                    modifier = Modifier.rotate180(),
                )
            }
            Spacer(Modifier.height(8.dp))
            RemoteKey(
                label = "أسفل",
                onPress = {},
                onPressStart = { if (hasDown) onDownStart() },
                onPressEnd = onStop,
                icon = R.drawable.ic_chevron_down,
                size = 52.dp,
                shape = RoundedCornerShape(16.dp),
                enabled = hasDown,
                active = activeKey == Buttons.DOWN,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "استمر بالضغط للتنقل السريع",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = TextMuted.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** زرّان فوق بعضهما (صوت/قناة) */
@Composable
private fun Rocker(
    title: String,
    upEnabled: Boolean,
    downEnabled: Boolean,
    upActive: Boolean,
    downActive: Boolean,
    onUpStart: () -> Unit,
    onDownStart: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceOne)
            .border(1.dp, Hairline, RoundedCornerShape(22.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RemoteKey(
            label = "$title +",
            onPress = {},
            onPressStart = { if (upEnabled) onUpStart() },
            onPressEnd = onStop,
            icon = R.drawable.ic_chevron_up,
            size = 50.dp,
            shape = RoundedCornerShape(15.dp),
            enabled = upEnabled,
            active = upActive,
            accent = Sky,
        )
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        RemoteKey(
            label = "$title −",
            onPress = {},
            onPressStart = { if (downEnabled) onDownStart() },
            onPressEnd = onStop,
            icon = R.drawable.ic_chevron_down,
            size = 50.dp,
            shape = RoundedCornerShape(15.dp),
            enabled = downEnabled,
            active = downActive,
            accent = Sky,
        )
    }
}

/** تدوير الأيقونة ١٨٠ درجة */
private fun Modifier.rotate180(): Modifier = this.rotate(180f)
