package com.wasil.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wasil.app.R
import com.wasil.app.WasilViewModel
import com.wasil.app.data.AppConfig
import com.wasil.app.data.AppMode
import com.wasil.app.ui.components.GhostButton
import com.wasil.app.ui.components.LabeledField
import com.wasil.app.ui.components.PrimaryButton
import com.wasil.app.ui.components.SettingRow
import com.wasil.app.ui.components.TopBar
import com.wasil.app.ui.theme.Coral
import com.wasil.app.ui.theme.Gold
import com.wasil.app.ui.theme.Hairline
import com.wasil.app.ui.theme.Mint
import com.wasil.app.ui.theme.SurfaceOne
import com.wasil.app.ui.theme.SurfaceTwo
import com.wasil.app.ui.theme.TextMuted
import com.wasil.app.ui.theme.TextPrimary

@Composable
fun SettingsScreen(
    vm: WasilViewModel,
    onBack: () -> Unit,
    onOpenInfo: () -> Unit,
) {
    val config by vm.config.collectAsState()
    var url by remember { mutableStateOf(config.serverUrl) }
    var token by remember { mutableStateOf(config.token) }
    var notify by remember { mutableStateOf(config.notificationsEnabled) }
    var testLine by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "الإعدادات", onBack = onBack)

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 30.dp),
        ) {
            Text("طريقة التشغيل", style = MaterialTheme.typography.titleSmall, color = TextMuted)
            Spacer(Modifier.height(8.dp))

            ModeOption(
                selected = config.mode == AppMode.DEMO,
                title = "وضع المعاينة",
                subtitle = "رسائل تجريبية بدون خادم",
                icon = R.drawable.ic_bolt,
            ) {
                vm.updateConfig(config.copy(mode = AppMode.DEMO))
            }
            Spacer(Modifier.height(10.dp))
            ModeOption(
                selected = config.mode == AppMode.LIVE,
                title = "الوضع الحقيقي",
                subtitle = "خادمك ورقمك الأمريكي",
                icon = R.drawable.ic_globe,
            ) {
                vm.updateConfig(config.copy(mode = AppMode.LIVE, serverUrl = url, token = token))
            }

            Spacer(Modifier.height(22.dp))
            Text("إعداد الخادم", style = MaterialTheme.typography.titleSmall, color = TextMuted)
            Spacer(Modifier.height(10.dp))

            LabeledField(
                label = "رابط الخادم",
                value = url,
                onValueChange = { url = it; testLine = null },
                placeholder = "https://my-server.onrender.com",
                icon = painterResource(R.drawable.ic_link),
                keyboardType = KeyboardType.Uri,
            )
            Spacer(Modifier.height(14.dp))
            LabeledField(
                label = "الرمز السري (API_TOKEN)",
                value = token,
                onValueChange = { token = it; testLine = null },
                placeholder = "اتركه فارغاً إذا لم تفعّل الرمز في الخادم",
                icon = painterResource(R.drawable.ic_lock),
                secret = true,
            )

            Spacer(Modifier.height(16.dp))
            PrimaryButton(text = "حفظ الإعداد") {
                vm.updateConfig(
                    config.copy(
                        mode = if (url.isBlank()) AppMode.DEMO else config.mode,
                        serverUrl = url,
                        token = token,
                        notificationsEnabled = notify,
                    )
                )
                testLine = "تم الحفظ والمزامنة" to true
            }
            Spacer(Modifier.height(10.dp))
            GhostButton(text = "اختبار الاتصال الآن", leadingPainter = painterResource(R.drawable.ic_send)) {
                if (url.isBlank()) {
                    testLine = "أدخل رابط الخادم أولاً" to false
                } else {
                    vm.testConnection(url, token) { message, ok -> testLine = message to ok }
                }
            }
            testLine?.let { (message, ok) ->
                Spacer(Modifier.height(10.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium, color = if (ok) Mint else Coral)
            }

            Spacer(Modifier.height(24.dp))
            Text("الإشعارات والبيانات", style = MaterialTheme.typography.titleSmall, color = TextMuted)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceOne)
                    .padding(horizontal = 14.dp),
            ) {
                SettingRow(
                    title = "إشعار الرسائل الجديدة",
                    subtitle = "إشعار محلي عند وصول رسالة أو كود تفعيل",
                    icon = painterResource(R.drawable.ic_bell),
                    trailing = {
                        Switch(
                            checked = notify,
                            onCheckedChange = {
                                notify = it
                                vm.updateConfig(
                                    vm.config.value.copy(notificationsEnabled = it)
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF231703),
                                checkedTrackColor = Gold,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceTwo,
                            ),
                        )
                    },
                )
            }

            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceOne)
                    .padding(horizontal = 14.dp),
            ) {
                SettingRow(
                    title = "مسح الرسائل المحفوظة",
                    subtitle = "حذف النسخة المحلية فقط (تبقى على خادمك)",
                    icon = painterResource(R.drawable.ic_delete),
                    onClick = { vm.clearCachedMessages() },
                )
            }

            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceOne)
                    .padding(horizontal = 14.dp),
            ) {
                SettingRow(
                    title = "معلومات مهمة قبل التفعيل",
                    subtitle = "حقيقة أرقام الإنترنت وواتساب وفايبر",
                    icon = painterResource(R.drawable.ic_info),
                    onClick = onOpenInfo,
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(
                "المزامنة التلقائية كل 15 ثانية أثناء فتح التطبيق، وكل 15 دقيقة في الخلفية عبر WorkManager. " +
                    "الإصدار 1.0.0 — لا يجمع التطبيق أي بيانات.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun ModeOption(selected: Boolean, title: String, subtitle: String, icon: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceOne)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Gold else Hairline, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(SurfaceTwo),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(if (selected) Gold else TextMuted),
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, if (selected) Gold else Hairline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Box(Modifier.size(11.dp).clip(CircleShape).background(Gold))
            }
        }
    }
}
