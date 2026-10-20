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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shuaa.remote.R
import com.shuaa.remote.data.Brand
import com.shuaa.remote.ui.components.BeamBackground
import com.shuaa.remote.ui.components.GlassCard
import com.shuaa.remote.ui.components.Pill
import com.shuaa.remote.ui.theme.Gold
import com.shuaa.remote.ui.theme.Hairline
import com.shuaa.remote.ui.theme.Mint
import com.shuaa.remote.ui.theme.Sky
import com.shuaa.remote.ui.theme.SurfaceOne
import com.shuaa.remote.ui.theme.SurfaceTwo
import com.shuaa.remote.ui.theme.TextMuted
import com.shuaa.remote.ui.theme.TextPrimary

@Composable
fun BrandPickerScreen(
    brands: List<Brand>,
    fromScan: Boolean,
    onBack: () -> Unit,
    onChoose: (Brand) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, brands) {
        if (query.isBlank()) brands else brands.filter { brand ->
            normalize(brand.nameAr).contains(normalize(query)) ||
                brand.nameEn.lowercase().contains(query.lowercase().trim())
        }
    }

    BeamBackground {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                    Text("ماركة الشاشة", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(
                        "${brands.size} ماركة · ${brands.sumOf { it.sets.size }} مجموعة أكواد",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
                Pill("بدون إنترنت", Mint)
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                placeholder = { Text("ابحث: سامسونج، LG، توشيبا…", color = TextMuted) },
                leadingIcon = {
                    Image(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        colorFilter = ColorFilter.tint(TextMuted),
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold.copy(alpha = 0.7f),
                    unfocusedBorderColor = Hairline,
                    focusedContainerColor = SurfaceOne,
                    unfocusedContainerColor = SurfaceOne,
                    cursorColor = Gold,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
            )

            Spacer(Modifier.height(12.dp))

            if (fromScan) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    borderColor = Sky.copy(alpha = 0.35f),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_camera),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            colorFilter = ColorFilter.tint(Sky),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "تم التقاط الشاشة. اختر الماركة المكتوبة أسفل الشاشة أو على الريموت القديم.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            "لا توجد ماركة بهذا الاسم. جرّب الاسم بالإنجليزية أو اختر ماركة شائعة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                        )
                    }
                }
                if (query.isBlank()) {
                    item {
                        Text("الماركات الشائعة", style = MaterialTheme.typography.titleMedium, color = Gold)
                    }
                }
                items(filtered.chunked(2)) { rowBrands ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowBrands.forEach { brand ->
                            BrandCard(brand, Modifier.weight(1f)) { onChoose(brand) }
                        }
                        if (rowBrands.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandCard(brand: Brand, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(listOf(SurfaceTwo.copy(alpha = 0.9f), SurfaceOne))
            )
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(
            brand.nameAr,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            brand.nameEn,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            maxLines = 1,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill("${brand.sets.size} ريموت", Gold)
            Spacer(Modifier.width(6.dp))
            Pill("${brand.buttonCount} زر", Sky)
        }
    }
}

/** تبسيط الحروف العربية لتحسين البحث */
fun normalize(text: String): String = text
    .trim()
    .lowercase()
    .replace('أ', 'ا')
    .replace('إ', 'ا')
    .replace('آ', 'ا')
    .replace('ى', 'ي')
    .replace('ة', 'ه')
    .replace("ال", "")
