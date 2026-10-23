package com.wasil.app.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wasil.app.R
import com.wasil.app.WasilViewModel
import com.wasil.app.data.AppConfig
import com.wasil.app.data.AppMode
import com.wasil.app.ui.components.GhostButton
import com.wasil.app.ui.components.InfoBanner
import com.wasil.app.ui.components.LabeledField
import com.wasil.app.ui.components.PrimaryButton
import com.wasil.app.ui.theme.Coral
import com.wasil.app.ui.theme.Gold
import com.wasil.app.ui.theme.Hairline
import com.wasil.app.ui.theme.Mint
import com.wasil.app.ui.theme.SurfaceOne
import com.wasil.app.ui.theme.SurfaceTwo
import com.wasil.app.ui.theme.TextMuted
import com.wasil.app.ui.theme.TextPrimary

@Composable
fun SetupScreen(vm: WasilViewModel, onDone: () -> Unit) {
    val initial = vm.config.value
    var mode by remember { mutableStateOf(initial.mode) }
    var url by remember { mutableStateOf(initial.serverUrl) }
    var token by remember { mutableStateOf(initial.token) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Spacer(Modifier.height(36.dp))
        Box(
            Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(SurfaceTwo)
                .border(1.dp, Gold.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(R.drawable.ic_chat),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                colorFilter = ColorFilter.tint(Gold),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("واصل", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            "استقبل رسائل التفعيل على رقم أمريكي تملكه أنت، عبر خادمك الخاص ومزوّد خدمة مرخّص.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted,
        )
        Spacer(Modifier.height(20.dp))

        InfoBanner(
            text = "لا يوجد تطبيق يولّد رقم هاتف حقيقي بنفسه. الأرقام الحقيقية تُمنح من شركات اتصالات مرخّصة (مثل Twilio)، وهذا التطبيق صندوق وارد لرقمك أنت.",
            icon = painterResource(R.drawable.ic_shield),
        )

        Spacer(Modifier.height(24.dp))
        Text("اختر طريقة التشغيل", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(12.dp))

        ModeCard(
            selected = mode == AppMode.DEMO,
            title = "وضع المعاينة",
            subtitle = "جرّب الواجهة كاملة برسائل تجريبية، بدون خادم ولا إنترنت.",
            icon = R.drawable.ic_bolt,
        ) { mode = AppMode.DEMO }

        Spacer(Modifier.height(10.dp))

        ModeCard(
            selected = mode == AppMode.LIVE,
            title = "الوضع الحقيقي",
            subtitle = "اربط خادمك الذي يستقبل رسائل رقم Twilio الأمريكي الخاص بك.",
            icon = R.drawable.ic_globe,
        ) { mode = AppMode.LIVE }

        if (mode == AppMode.LIVE) {
            Spacer(Modifier.height(22.dp))
            LabeledField(
                label = "رابط الخادم",
                value = url,
                onValueChange = { url = it; testResult = null },
                placeholder = "https://my-server.onrender.com",
                icon = painterResource(R.drawable.ic_link),
                keyboardType = KeyboardType.Uri,
            )
            Spacer(Modifier.height(14.dp))
            LabeledField(
                label = "الرمز السري للواجهة (API_TOKEN)",
                value = token,
                onValueChange = { token = it; testResult = null },
                placeholder = "الرمز نفسه الذي عيّنته في خادمك",
                icon = painterResource(R.drawable.ic_lock),
                secret = true,
            )
            Spacer(Modifier.height(14.dp))
            GhostButton(
                text = if (testing) "جارٍ الاختبار…" else "اختبار الاتصال",
                leadingPainter = painterResource(R.drawable.ic_send),
            ) {
                if (url.isBlank()) {
                    testResult = "أدخل رابط الخادم أولاً" to false
                    return@GhostButton
                }
                testing = true
                vm.testConnection(url, token) { message, ok ->
                    testing = false
                    testResult = message to ok
                }
            }
            testResult?.let { (message, ok) ->
                Spacer(Modifier.height(10.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (ok) Mint else Coral,
                )
            }
        }

        Spacer(Modifier.height(26.dp))
        PrimaryButton(
            text = if (mode == AppMode.LIVE) "احفظ الإعداد وابدأ" else "ابدأ بالوضع التجريبي",
            enabled = mode == AppMode.DEMO || url.isNotBlank(),
            loading = false,
        ) {
            vm.updateConfig(
                AppConfig(
                    mode = mode,
                    serverUrl = url,
                    token = token,
                    notificationsEnabled = initial.notificationsEnabled,
                )
            )
            onDone()
        }

        Spacer(Modifier.height(16.dp))
        InfoBanner(
            text = "تنبيه صريح: واتساب وفايبر قد يرفضان أو يحظران أرقام الإنترنت (VoIP) مثل أرقام Twilio، وقد يخالف ذلك شروط استخدامها. التفاصيل وخيارات أكثر في شاشة «معلومات مهمة».",
            accent = Coral,
            icon = painterResource(R.drawable.ic_warning),
        )
    }
}

@Composable
private fun ModeCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    icon: Int,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceOne)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Gold else Hairline,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable { onClick() }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(SurfaceTwo),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(if (selected) Gold else TextMuted),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, if (selected) Gold else Hairline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(
                        Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(Gold)
                    )
                }
            }
        }
    }
}
