package com.shuaa.remote.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shuaa.remote.R
import com.shuaa.remote.ShuaaViewModel
import com.shuaa.remote.data.Brand
import com.shuaa.remote.data.Buttons
import com.shuaa.remote.data.SavedDevice
import com.shuaa.remote.ui.components.BeamBackground
import com.shuaa.remote.ui.components.GlassCard
import com.shuaa.remote.ui.components.Pill
import com.shuaa.remote.ui.components.PrimaryButton
import com.shuaa.remote.ui.theme.Coral
import com.shuaa.remote.ui.theme.Gold
import com.shuaa.remote.ui.theme.Hairline
import com.shuaa.remote.ui.theme.Mint
import com.shuaa.remote.ui.theme.Sky
import com.shuaa.remote.ui.theme.SurfaceOne
import com.shuaa.remote.ui.theme.SurfaceTwo
import com.shuaa.remote.ui.theme.TextMuted
import com.shuaa.remote.ui.theme.TextPrimary
import kotlinx.coroutines.delay

/**
 * المطابقة التلقائية: نرسل كود الطاقة لكل مجموعة أكواد للماركة،
 * والمستخدم يجيب بنعم/لا، حتى نصل إلى الريموت الصحيح.
 */
@Composable
fun MatchScreen(
    brand: Brand,
    vm: ShuaaViewModel,
    onBack: () -> Unit,
    onSaved: (SavedDevice) -> Unit,
) {
    val sets = remember(brand) { brand.sets.filter { it.codes.containsKey(Buttons.POWER) } }
    var index by remember(brand) { mutableIntStateOf(0) }
    var tries by remember(brand) { mutableIntStateOf(0) }
    var sentPulse by remember { mutableStateOf(false) }
    val current = sets.getOrNull(index)
    val exhausted = sets.isEmpty() || index >= sets.size

    fun sendCurrent() {
        current ?: return
        vm.sendSignal(current.codes[Buttons.POWER], Buttons.POWER)
        tries += 1
        sentPulse = true
    }

    LaunchedEffect(sentPulse) {
        if (sentPulse) {
            delay(700)
            sentPulse = false
        }
    }

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
                    Text("مطابقة تلقائية", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(
                        "نجرّب الأكواد حتى يعمل التلفزيون",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
                Pill(if (exhausted) "انتهت" else "${index + 1}/${sets.size}", Gold, filled = true)
            }

            Spacer(Modifier.height(18.dp))

            GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Gold.copy(alpha = 0.24f), Sky.copy(alpha = 0.10f))
                                )
                            )
                            .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            brand.nameAr.take(1),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Gold,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(brand.nameAr, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text(
                            "${brand.nameEn} · ${brand.sets.size} مجموعة أكواد",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            /* خطوات */
            GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StepRow(1, "وجّه الهاتف نحو التلفزيون من مسافة ٢–٣ أمتار")
                    StepRow(2, "اضغط زر الإرسال بالأسفل وراقب الشاشة")
                    StepRow(3, "أجب: هل أضاءت الشاشة؟ وسنحفظ الريموت مباشرة")
                }
            }

            Spacer(Modifier.height(20.dp))

            if (exhausted) {
                GlassCard(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    borderColor = Coral.copy(alpha = 0.35f),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "جرّبنا كل الأكواد المتوفرة لهذه الماركة",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "قد تكون الشاشة من ماركة أخرى أو من موديل قديم جداً. جرّب ماركة قريبة مثل Samsung أو LG أو «عام».",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton(
                            text = "إعادة المحاولة من البداية",
                            onClick = {
                                index = 0
                                tries = 0
                            },
                            modifier = Modifier.fillMaxWidth(),
                            tonal = true,
                        )
                        Spacer(Modifier.height(8.dp))
                        PrimaryButton(
                            text = "اختيار ماركة أخرى",
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "الريموت ${index + 1} من ${sets.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        current?.name ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))

                    SendPowerButton(pulse = sentPulse) { sendCurrent() }

                    Spacer(Modifier.height(18.dp))
                    Text(
                        if (tries == 0) "اضغط لإرسال كود التشغيل" else "أرسلنا الكود ${tries} مرة",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )

                    Spacer(Modifier.height(20.dp))

                    PrimaryButton(
                        text = "أضاءت الشاشة — احفظ الريموت",
                        onClick = {
                            current?.let { set ->
                                vm.feedback.success()
                                onSaved(vm.saveDevice(brand, set))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = painterResource(R.drawable.ic_check),
                    )
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton(
                        text = "لم تعمل — جرّب الكود التالي",
                        onClick = {
                            index += 1
                            tries = 0
                        },
                        modifier = Modifier.fillMaxWidth(),
                        tonal = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "نصيحة: وجّه الهاتف إلى مستقبل الأشعة في التلفزيون، وأبعد أي مصباح فلورسنت قوي.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (vm.irAvailable && !vm.settings.demoMode) {
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceOne)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_power),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        colorFilter = ColorFilter.tint(Mint),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        vm.irDescription,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepRow(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Gold.copy(alpha = 0.14f))
                .border(1.dp, Gold.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.labelSmall,
                color = Gold,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
    }
}

@Composable
private fun SendPowerButton(pulse: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "powerPulse")
    val halo by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "halo",
    )
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size((150 * halo).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Gold.copy(alpha = 0.20f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(124.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (pulse) Gold.copy(alpha = 0.55f) else Gold.copy(alpha = 0.22f),
                            SurfaceOne,
                        )
                    )
                )
                .border(1.5.dp, Gold.copy(alpha = 0.55f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.ic_power),
                    contentDescription = "إرسال",
                    modifier = Modifier.size(38.dp),
                    colorFilter = ColorFilter.tint(Gold),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "إرسال",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
