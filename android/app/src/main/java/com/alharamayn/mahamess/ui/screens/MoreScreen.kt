package com.alharamayn.mahamess.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alharamayn.mahamess.ui.Screen
import com.alharamayn.mahamess.ui.SectionCard

@Composable
fun MoreScreen(navigate: (Screen) -> Unit) {
    val groups = listOf(
        "الأصناف والمخزون" to listOf(
            Triple("🥜", "الأصناف", Screen.Items),
            Triple("📦", "المخزون والتحميص", Screen.Inventory),
        ),
        "الحسابات" to listOf(
            Triple("🏭", "الموردون", Screen.Suppliers),
            Triple("🛒", "المشتريات", Screen.Purchases),
            Triple("💸", "المصاريف", Screen.Expenses),
            Triple("💰", "الصندوق والإقفال", Screen.Cash),
            Triple("🏦", "مسحوبات المحل", Screen.Withdrawals),
            Triple("🎁", "الطلبات المسبقة", Screen.Preorders),
        ),
        "العمال والإدارة" to listOf(
            Triple("👷", "العمال والسحوبات", Screen.Workers),
            Triple("📊", "التقارير", Screen.Reports),
            Triple("🔐", "المستخدمون", Screen.Users),
            Triple("📜", "سجل العمليات", Screen.Audit),
            Triple("⚙️", "الإعدادات والنسخ", Screen.Settings),
        ),
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        groups.forEach { (title, items) ->
            SectionCard(title = title) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (icon, label, screen) ->
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { navigate(screen) }
                                        .padding(vertical = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(icon, fontSize = 20.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            }
                            if (row.size == 1) Box(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Text(
            "محامص الحرمين • الإصدار 1.0.0 • يعمل بدون إنترنت",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
    }
}
