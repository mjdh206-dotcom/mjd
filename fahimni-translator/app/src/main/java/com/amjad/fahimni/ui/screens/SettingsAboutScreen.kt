package com.amjad.fahimni.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amjad.fahimni.FahimniUiState
import com.amjad.fahimni.R
import com.amjad.fahimni.data.DEVELOPER_ATTRIBUTION
import com.amjad.fahimni.ui.theme.BorderSubtle
import com.amjad.fahimni.ui.theme.CardDark
import com.amjad.fahimni.ui.theme.CardElevated
import com.amjad.fahimni.ui.theme.CoralAlert
import com.amjad.fahimni.ui.theme.CyanRadar
import com.amjad.fahimni.ui.theme.EmeraldBright
import com.amjad.fahimni.ui.theme.RoyalGold
import com.amjad.fahimni.ui.theme.TextSecondary
import com.amjad.fahimni.ui.theme.TextWhite

@Composable
fun SettingsAboutScreen(
    uiState: FahimniUiState,
    onToggleAutoSpeak: (Boolean) -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onToggleHandsFree: (Boolean) -> Unit,
    onToggleDeepCloudAi: (Boolean) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onClearHistory: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // بطاقة المطور الملكية الذهبية (أمجد حبيب)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent,
            border = BorderStroke(1.5.dp, RoyalGold),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1F190B), Color(0xFF0E1627))
                    )
                ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "👑 فَهِّمني — مترجم الحكي واللهجات الذكي",
                    style = MaterialTheme.typography.titleLarge,
                    color = RoyalGold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF2B220C),
                    border = BorderStroke(1.dp, RoyalGold),
                ) {
                    Text(
                        text = DEVELOPER_ATTRIBUTION,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        color = TextWhite,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "الإصدار 1.0.0 • يعمل بالإنترنت لأقصى سرعة ودقة في كشف اللهجات وترجمة الصوت فوراً",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // إعدادات الصوت والذكاء التلقائي
        SettingToggleCard(
            title = "🔊 نطق الترجمة صوتياً بشكل تلقائي",
            subtitle = "فور انتهاء المتحدث من كلامه، ينطق التطبيق الترجمة بصوت واضح مباشرةً",
            checked = uiState.autoSpeak,
            onCheckedChange = onToggleAutoSpeak,
        )

        SettingToggleCard(
            title = "🎯 الربط التلقائي بلهجة الطرف المقابل المكتشفة",
            subtitle = "بمجرد أن يكتشف التطبيق لهجة الشخص الذي أمامك، يترجم ردودك القادمة إلى لهجته تلقائياً",
            checked = uiState.autoSyncToDetected,
            onCheckedChange = onToggleAutoSync,
        )

        SettingToggleCard(
            title = "🔁 وضع الحوار التلقائي المستمر بدون لمس",
            subtitle = "يتناوب المايكروفون تلقائياً بينك وبين الطرف المقابل بعد كل نطق صوتي",
            checked = uiState.handsFreeLoop,
            onCheckedChange = onToggleHandsFree,
        )

        SettingToggleCard(
            title = "☁️ المحرك السحابي المتقدم للهجات العميقة",
            subtitle = "يجمع بين الذكاء السحابي الفوري ومحرك Google Neural لفهم أدق التعابير الشعبية",
            checked = uiState.deepCloudAi,
            onCheckedChange = onToggleDeepCloudAi,
        )

        // سرعة النطق الصوتي
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardDark,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🎚️ سرعة النطق الصوتي",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SpeedChoiceChip("بطيء وواضح 0.8x", 0.82f, uiState.speechSpeed, Modifier.weight(1f), onSelectSpeed)
                    SpeedChoiceChip("طبيعي 1.0x", 1.0f, uiState.speechSpeed, Modifier.weight(1f), onSelectSpeed)
                    SpeedChoiceChip("سريع 1.2x", 1.2f, uiState.speechSpeed, Modifier.weight(1f), onSelectSpeed)
                }
            }
        }

        // مسح سجل المحادثة
        OutlinedButton(
            onClick = onClearHistory,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAlert),
            border = BorderStroke(1.dp, CoralAlert.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_trash),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("مسح سجل المحادثات المحفوظة (${uiState.messages.size})")
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardDark,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF04141F),
                    checkedTrackColor = EmeraldBright,
                ),
            )
        }
    }
}

@Composable
private fun SpeedChoiceChip(
    label: String,
    value: Float,
    current: Float,
    modifier: Modifier = Modifier,
    onSelect: (Float) -> Unit,
) {
    val active = kotlin.math.abs(current - value) < 0.05f
    Surface(
        onClick = { onSelect(value) },
        shape = RoundedCornerShape(14.dp),
        color = if (active) CyanRadar.copy(alpha = 0.2f) else CardElevated,
        border = BorderStroke(1.dp, if (active) CyanRadar else BorderSubtle),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) CyanRadar else TextSecondary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
        )
    }
}
