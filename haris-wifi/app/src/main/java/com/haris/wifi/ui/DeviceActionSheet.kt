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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.haris.wifi.ui.theme.BorderSubtle
import com.haris.wifi.ui.theme.CardElevated
import com.haris.wifi.ui.theme.CardSurface
import com.haris.wifi.ui.theme.Emerald
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

    val quickNames = listOf(
        "موبايل أبي",
        "موبايل أمي",
        "موبايل أخي",
        "موبايل أختي",
        "كاميرا المراقبة",
        "شاشة الصالة",
        "موبايل غريب",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Obsidian)
                .border(1.5.dp, BorderSubtle, RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "تسمية الموبايل",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.ExtraBold,
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

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text("اكتب اسم صاحب الموبايل (مثلاً: موبايل أحمد)", color = TextSilver)
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
                            .clickable { nameInput = preset }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(preset, style = MaterialTheme.typography.labelSmall, color = TextWhite)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Emerald)
                    .clickable {
                        vm.saveDeviceDetails(
                            device = device,
                            customName = nameInput,
                            deviceType = device.deviceType,
                            isTrusted = device.isTrusted,
                        )
                    }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✓  حفظ الاسم",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF032219),
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}
