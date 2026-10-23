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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.wasil.app.R
import com.wasil.app.WasilViewModel
import com.wasil.app.data.AppMode
import com.wasil.app.data.ConnectStatus
import com.wasil.app.ui.components.Hairline
import com.wasil.app.ui.components.InfoBanner
import com.wasil.app.ui.components.StatusPill
import com.wasil.app.ui.components.TopBar
import com.wasil.app.ui.components.statusColor
import com.wasil.app.ui.theme.Coral
import com.wasil.app.ui.theme.Gold
import com.wasil.app.ui.theme.Hairline as HairlineColor
import com.wasil.app.ui.theme.Mint
import com.wasil.app.ui.theme.Sky
import com.wasil.app.ui.theme.SurfaceOne
import com.wasil.app.ui.theme.SurfaceTwo
import com.wasil.app.ui.theme.TextMuted
import com.wasil.app.ui.theme.TextPrimary

@Composable
fun HomeScreen(
    vm: WasilViewModel,
    onOpenMessages: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenInfo: () -> Unit,
) {
    val config by vm.config.collectAsState()
    val number by vm.number.collectAsState()
    val status by vm.status.collectAsState()
    val lastSync by vm.lastSync.collectAsState()
    val messages by vm.messages.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = "واصل",
            icon = painterResource(R.drawable.ic_settings),
            onIconClick = onOpenSettings,
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
        ) {
            if (config.mode == AppMode.DEMO) {
                InfoBanner(
                    text = "أنت في وضع المعاينة: كل ما تراه رسائل تجريبية. للاستخدام الحقيقي اربط خادمك من الإعدادات.",
                    accent = Sky,
                    icon = painterResource(R.drawable.ic_bolt),
                )
                Spacer(Modifier.height(14.dp))
            }

            /* بطاقة الرقم */
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceOne)
                    .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                    .padding(22.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(SurfaceTwo),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painterResource(R.drawable.ic_phone),
                                contentDescription = null,
                                modifier = Modifier.size(21.dp),
                                colorFilter = ColorFilter.tint(Gold),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("رقمك الأمريكي", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.weight(1f))
                        val (statusText, color) = when {
                            config.mode == AppMode.DEMO -> "معاينة" to Sky
                            status == ConnectStatus.OK -> "متصل" to Mint
                            status == ConnectStatus.CONNECTING -> "مزامنة…" to Sky
                            status == ConnectStatus.ERROR -> "خطأ" to Coral
                            else -> "غير متصل" to TextMuted
                        }
                        StatusPill(statusText, color)
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(
                        if (number.isBlank()) "بانتظار الرقم من الخادم…" else number,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                        ),
                        color = if (number.isBlank()) TextMuted else Gold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        lastSync?.let { "آخر مزامنة: ${TimeFmt.relative(it)}" } ?: "لم تتم مزامنة بعد",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )

                    Spacer(Modifier.height(18.dp))
                    Hairline()
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ActionChip(
                            text = "نسخ الرقم",
                            icon = R.drawable.ic_copy,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (number.isNotBlank()) {
                                clipboard.setText(AnnotatedString(number))
                                Toast.makeText(context, "تم نسخ الرقم", Toast.LENGTH_SHORT).show()
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        ActionChip(
                            text = "تحديث الآن",
                            icon = R.drawable.ic_refresh,
                            modifier = Modifier.weight(1f),
                        ) { vm.refresh(showFeedback = true) }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("الوصول السريع", style = MaterialTheme.typography.titleSmall, color = TextMuted)
            Spacer(Modifier.height(10.dp))

            QuickRow(
                title = "الرسائل الواردة",
                subtitle = "${messages.size} رسالة محفوظة على هاتفك",
                icon = R.drawable.ic_chat,
                onClick = onOpenMessages,
            )
            Spacer(Modifier.height(10.dp))
            QuickRow(
                title = "معلومات مهمة قبل التفعيل",
                subtitle = "حقيقة أرقام الإنترنت وواتساب وفايبر",
                icon = R.drawable.ic_info,
                onClick = onOpenInfo,
            )
            Spacer(Modifier.height(10.dp))
            QuickRow(
                title = "الإعدادات",
                subtitle = "الخادم، الرمز السري، الإشعارات",
                icon = R.drawable.ic_settings,
                onClick = onOpenSettings,
            )

            Spacer(Modifier.height(22.dp))
            InfoBanner(
                text = "الخصوصية: الرسائل تُخزَّن على خادمك أنت ثم تُنقل عبر اتصال آمن إلى هاتفك. لا يمر أي شيء عبر مطوّر التطبيق.",
                accent = Mint,
                icon = painterResource(R.drawable.ic_shield),
            )
        }
    }
}

@Composable
private fun ActionChip(text: String, icon: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceTwo)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                colorFilter = ColorFilter.tint(Gold),
            )
            Spacer(Modifier.width(7.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        }
    }
}

@Composable
private fun QuickRow(title: String, subtitle: String, icon: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceOne)
            .border(1.dp, HairlineColor, RoundedCornerShape(18.dp))
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
                    colorFilter = ColorFilter.tint(Gold),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Image(
                painterResource(R.drawable.ic_chevron_left),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                colorFilter = ColorFilter.tint(TextMuted),
            )
        }
    }
}
