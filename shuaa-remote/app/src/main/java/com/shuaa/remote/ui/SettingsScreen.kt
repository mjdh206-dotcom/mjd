package com.shuaa.remote.ui

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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shuaa.remote.R
import com.shuaa.remote.ShuaaViewModel
import com.shuaa.remote.ui.components.BeamBackground
import com.shuaa.remote.ui.components.BrandMark
import com.shuaa.remote.ui.components.GlassCard
import com.shuaa.remote.ui.components.Pill
import com.shuaa.remote.ui.theme.Coral
import com.shuaa.remote.ui.theme.Gold
import com.shuaa.remote.ui.theme.Hairline
import com.shuaa.remote.ui.theme.Mint
import com.shuaa.remote.ui.theme.Sky
import com.shuaa.remote.ui.theme.SurfaceOne
import com.shuaa.remote.ui.theme.TextMuted
import com.shuaa.remote.ui.theme.TextPrimary

@Composable
fun SettingsScreen(vm: ShuaaViewModel, onBack: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    val settings = vm.settings

    BeamBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceOne)
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
                Text("الإعدادات", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }

            Spacer(Modifier.height(18.dp))

            GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    BrandMark(52.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("شُعاع", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text(
                            "ريموت الشاشات القديمة — يعمل دون إنترنت",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                    Pill("v1.0", Gold)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("الإحساس والاستجابة", style = MaterialTheme.typography.titleMedium, color = Gold)
            Spacer(Modifier.height(10.dp))

            SettingSwitch(
                title = "الاهتزاز عند الضغط",
                subtitle = "إحساس ملموس كالريموت الحقيقي",
                checked = settings.hapticsEnabled,
                onChange = { settings.updateHaptics(it) },
            )
            Spacer(Modifier.height(10.dp))
            SettingSwitch(
                title = "صوت الضغط",
                subtitle = "نقرة خفيفة مع كل زر",
                checked = settings.soundEnabled,
                onChange = { settings.updateSound(it) },
            )

            Spacer(Modifier.height(20.dp))
            Text("التوافق", style = MaterialTheme.typography.titleMedium, color = Gold)
            Spacer(Modifier.height(10.dp))

            GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (vm.irAvailable) Mint else Coral)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (vm.irAvailable) "مُرسِل الأشعة تحت الحمراء متاح" else "لا يوجد مُرسِل أشعة في هذا الهاتف",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(vm.irDescription, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "البروتوكولات المدعومة: NEC · Samsung · Sony SIRC · RC5 · RC6 · RCA · Panasonic",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            SettingSwitch(
                title = "وضع المعاينة",
                subtitle = "تجربة الواجهة والأزرار بدون إرسال فعلي",
                checked = settings.demoMode,
                onChange = { settings.updateDemoMode(it) },
            )

            Spacer(Modifier.height(20.dp))
            Text("البيانات", style = MaterialTheme.typography.titleMedium, color = Gold)
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceOne)
                    .border(1.dp, Hairline, RoundedCornerShape(18.dp))
                    .clickable { confirmClear = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("حذف كل الأجهزة", style = MaterialTheme.typography.titleSmall, color = Coral)
                    Text(
                        "${vm.devices.size} جهاز محفوظ على هذا الهاتف",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
                Image(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    colorFilter = ColorFilter.tint(Coral),
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("عن التطبيق", style = MaterialTheme.typography.titleMedium, color = Gold)
            Spacer(Modifier.height(10.dp))

            GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "شُعاع تطبيق عربي مجاني يحوّل هاتفك إلى ريموت للشاشات والتلفزيونات القديمة والحديثة. كل الأكواد مدمجة داخل التطبيق، ولا يُرسل أي شيء إلى الإنترنت.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                    Text(
                        "• الأكواد: Flipper-IRDB — رخصة CC BY-SA 4.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = Sky,
                    )
                    Text(
                        "• الخط: Tajawal — رخصة OFL",
                        style = MaterialTheme.typography.labelSmall,
                        color = Sky,
                    )
                    Text(
                        "• التطبيق لا يجمع أي بيانات ولا يحتاج حساباً.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "صُنع بعناية للمستخدم العربي",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = SurfaceOne,
            titleContentColor = TextPrimary,
            textContentColor = TextMuted,
            title = { Text("حذف كل الأجهزة؟") },
            text = { Text("سيتم حذف الريموتات المحفوظة من هذا الهاتف. لا يمكن التراجع.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteAll()
                    confirmClear = false
                    vm.showBanner("تم حذف كل الأجهزة")
                }) { Text("حذف", color = Coral) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("إلغاء", color = TextMuted) }
            },
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceOne)
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF241704),
                checkedTrackColor = Gold,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceOne,
                uncheckedBorderColor = Hairline,
            ),
        )
    }
}
