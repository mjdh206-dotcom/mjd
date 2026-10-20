package com.shuaa.remote.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import com.shuaa.remote.data.SavedDevice
import com.shuaa.remote.ui.components.BeamBackground
import com.shuaa.remote.ui.components.BrandMark
import com.shuaa.remote.ui.components.GlassCard
import com.shuaa.remote.ui.components.Pill
import com.shuaa.remote.ui.components.PrimaryButton
import com.shuaa.remote.ui.components.PulseRings
import com.shuaa.remote.ui.components.SectionHeader
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
fun HomeScreen(
    vm: ShuaaViewModel,
    onScan: () -> Unit,
    onBrowseBrands: () -> Unit,
    onSelectBrand: (String) -> Unit,
    onOpenDevice: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val brands = vm.brands
    val devices = vm.devices
    val irReady = vm.irAvailable && !vm.settings.demoMode

    BeamBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 28.dp),
        ) {
            /* ---------- الترويسة ---------- */
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark(46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "شُعاع",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        "ريموت الشاشات القديمة",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SurfaceTwo)
                        .border(1.dp, Hairline, CircleShape)
                        .clickable(onClick = onOpenSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = "الإعدادات",
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(TextPrimary),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            /* ---------- حالة مُرسِل الأشعة ---------- */
            GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (irReady) Mint else Coral)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (irReady) "مُرسِل الأشعة جاهز" else if (vm.settings.demoMode) "وضع المعاينة مُفعّل" else "لا يوجد مُرسِل أشعة",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Pill(
                        text = if (irReady) "IR" else "معاينة",
                        color = if (irReady) Mint else Coral,
                        filled = true,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            /* ---------- البطاقة الرئيسية ---------- */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1B2A44), Color(0xFF111A2B), Color(0xFF0C1320))
                        )
                    )
                    .border(1.dp, Gold.copy(alpha = 0.22f), RoundedCornerShape(28.dp)),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Gold.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(size.width * 0.18f, size.height * 0.1f),
                            radius = size.width * 0.62f,
                        ),
                        radius = size.width * 0.62f,
                        center = Offset(size.width * 0.18f, size.height * 0.1f),
                    )
                }
                Column(Modifier.padding(22.dp)) {
                    Pill("مطابقة تلقائية", Gold, filled = true)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "وجّه الموبايل نحو الشاشة",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "نرصد الشاشة، ثم نختار الريموت المناسب تلقائياً من مكتبة ١٠٤ ماركات — كل ذلك دون إنترنت.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            PulseRings(Modifier.size(64.dp))
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Gold),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_scan),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    colorFilter = ColorFilter.tint(Color(0xFF1E1403)),
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        PrimaryButton(
                            text = "امسح الشاشة الآن",
                            onClick = onScan,
                            icon = painterResource(R.drawable.ic_camera),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            /* ---------- أجهزتي ---------- */
            SectionHeader(
                title = "أجهزتي",
                subtitle = if (devices.isEmpty()) "لم تُضِف شاشة بعد" else "${devices.size} جهاز محفوظ",
                action = {
                    Text(
                        "كل الماركات",
                        style = MaterialTheme.typography.labelMedium,
                        color = Gold,
                        modifier = Modifier.clickable(onClick = onBrowseBrands),
                    )
                },
            )
            Spacer(Modifier.height(12.dp))

            if (devices.isEmpty()) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Gold.copy(alpha = 0.12f))
                                .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_remote),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                colorFilter = ColorFilter.tint(Gold),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "أضف أول شاشة",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                            )
                            Text(
                                "بعد المطابقة يظهر الريموت هنا بضغطة واحدة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                    }
                }
            } else {
                devices.forEach { device ->
                    DeviceCard(
                        device = device,
                        onClick = { onOpenDevice(device.id) },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            /* ---------- الماركات ---------- */
            SectionHeader(
                title = "الماركات الشائعة",
                subtitle = "اختر ماركة الشاشة مباشرة",
            )
            Spacer(Modifier.height(12.dp))
            BrandTiles(
                brands = brands.take(6),
                onSelect = { brand -> onSelectBrand(brand.id) },
            )

            Spacer(Modifier.height(22.dp))

            Text(
                text = "مكتبة ${brands.size} ماركة · ${brands.sumOf { it.sets.size }} مجموعة أكواد · تعمل دون إنترنت",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "شُعاع — النسخة 1.0",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun DeviceCard(device: SavedDevice, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Sky.copy(alpha = 0.18f), Gold.copy(alpha = 0.10f))
                        )
                    )
                    .border(1.dp, Hairline, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_tv),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(Sky),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Text(
                    "${device.brandAr} · ${device.codes.size} زر",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
            if (device.isPrimary) {
                Pill("الافتراضي", Mint, filled = true)
                Spacer(Modifier.width(8.dp))
            }
            Image(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                colorFilter = ColorFilter.tint(TextMuted),
            )
        }
    }
}

/** شبكة ماركات بسيطة (٣ أعمدة) */
@Composable
fun BrandTiles(
    brands: List<Brand>,
    onSelect: (Brand) -> Unit,
    columns: Int = 3,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        brands.chunked(columns).forEach { rowBrands ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowBrands.forEach { brand ->
                    BrandTile(brand, Modifier.weight(1f)) { onSelect(brand) }
                }
                repeat(columns - rowBrands.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun BrandTile(brand: Brand, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceOne)
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            brand.nameAr,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            brand.nameEn,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Pill("${brand.sets.size} ريموت", Gold.copy(alpha = 0.85f))
    }
}
