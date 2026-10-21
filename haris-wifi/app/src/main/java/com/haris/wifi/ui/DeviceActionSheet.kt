package com.haris.wifi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haris.wifi.HarisViewModel
import com.haris.wifi.data.ConnectedDevice
import com.haris.wifi.data.DeviceType
import com.haris.wifi.data.DisconnectMode
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
import com.haris.wifi.ui.theme.Obsidian
import com.haris.wifi.ui.theme.TextSilver
import com.haris.wifi.ui.theme.TextWhite

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceActionSheet(
    device: ConnectedDevice,
    vm: HarisViewModel,
    onDismiss: () -> Unit,
) {
    var nameInput by remember(device.uniqueKey) { mutableStateOf(device.customName) }
    var chosenType by remember(device.uniqueKey) { mutableStateOf(device.deviceType) }
    var trustedState by remember(device.uniqueKey) { mutableStateOf(device.isTrusted) }

    val quickNames = listOf(
        "جوال أبي",
        "جوال أمي",
        "جوال أخي",
        "تلفزيون الصالة",
        "بلايستيشن",
        "لابتوب العمل",
        "جهاز غريب!",
    )

    val selectableTypes = listOf(
        DeviceType.PHONE,
        DeviceType.LAPTOP,
        DeviceType.TV,
        DeviceType.CONSOLE,
        DeviceType.CAMERA,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Obsidian)
                .border(
                    1.dp,
                    BorderSubtle,
                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .clickable(enabled = false) {}
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // شريط السحب العلوي
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${device.deviceType.emoji}  ${device.displayName}",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardElevated)
                        .clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("إغلاق ✕", color = TextSilver, style = MaterialTheme.typography.labelMedium)
                }
            }

            // بطاقة معلومات سريعة + نسخ بضغطة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DeepNavy)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { vm.copyToClipboard("IP", device.ip) }
                ) {
                    Text("عنوان IP (اضغط للنسخ)", style = MaterialTheme.typography.labelSmall, color = TextSilver)
                    Text(device.ip, style = MaterialTheme.typography.titleSmall, color = CyberSky)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { vm.copyToClipboard("MAC", device.mac) }
                ) {
                    Text("عنوان MAC (اضغط للنسخ)", style = MaterialTheme.typography.labelSmall, color = TextSilver)
                    Text(device.mac, style = MaterialTheme.typography.titleSmall, color = EmeraldBright)
                }
            }

            // قسم خيارات فصل الاتصال (إذا لم يكن الراوتر أو نفس الهاتف)
            if (device.canDisconnect) {
                Text(
                    text = "خيارات فصل الاتصال عن هذا الجهاز",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                )

                if (device.isBlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Emerald)
                            .clickable { vm.reconnectDevice(device) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "↺  إعادة الاتصال لهذا الجهاز الآن",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF032219),
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }

                DisconnectMode.entries.forEach { mode ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (mode == DisconnectMode.INSTANT_OPEN) CoralDim.copy(alpha = 0.6f)
                                else CardSurface
                            )
                            .border(
                                1.dp,
                                if (mode == DisconnectMode.INSTANT_OPEN) CoralRed.copy(alpha = 0.7f)
                                else BorderSubtle,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { vm.cutDevice(device, mode) }
                            .padding(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = mode.titleAr,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (mode == DisconnectMode.INSTANT_OPEN) CoralBright else TextWhite,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = mode.subtitleAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSilver,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "تنفيذ ✂️",
                                style = MaterialTheme.typography.labelMedium,
                                color = CoralBright,
                            )
                        }
                    }
                }
            }

            // قسم تسمية الجهاز وتصنيفه
            Text(
                text = "تسمية الجهاز (لتعرف صاحبه دائماً)",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite,
            )

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("اكتب اسماً واضحاً مثل: جوال أحمد", color = TextSilver)
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

            // اقتراحات أسماء جاهزة بضغطة واحدة
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                quickNames.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardElevated)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable {
                                nameInput = preset
                                trustedState = preset != "جهاز غريب!"
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(preset, style = MaterialTheme.typography.labelSmall, color = TextWhite)
                    }
                }
            }

            if (!device.isGateway && !device.isSelf) {
                // اختيار نوع الجهاز
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    selectableTypes.forEach { type ->
                        val active = chosenType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) Emerald else CardSurface)
                                .border(1.dp, if (active) EmeraldBright else BorderSubtle, RoundedCornerShape(12.dp))
                                .clickable { chosenType = type }
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = "${type.emoji} ${type.labelAr}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color(0xFF032219) else TextWhite,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }

                // مفتاح جهاز موثوق من العائلة
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardSurface)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "جهاز موثوق (من العائلة)",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextWhite,
                        )
                        Text(
                            text = "لا يُفصل عند الضغط على «فصل كل الغرباء»",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSilver,
                        )
                    }
                    Switch(
                        checked = trustedState,
                        onCheckedChange = { trustedState = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = TextWhite, checkedTrackColor = Emerald),
                    )
                }
            }

            // زر حفظ التسمية
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Emerald)
                    .clickable {
                        vm.saveDeviceDetails(
                            device = device,
                            customName = nameInput,
                            deviceType = chosenType,
                            isTrusted = trustedState,
                        )
                    }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✓  حفظ اسم وتصنيف الجهاز",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF032219),
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}
