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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alharamayn.mahamess.AppViewModel
import com.alharamayn.mahamess.data.AppSettings
import com.alharamayn.mahamess.ui.Brown
import com.alharamayn.mahamess.ui.BrownDark
import com.alharamayn.mahamess.ui.Gold
import com.alharamayn.mahamess.ui.GoldLight

@Composable
fun LockScreen(vm: AppViewModel, settings: AppSettings) {
    var pin by remember { mutableStateOf("") }
    val message by vm.message.collectAsStateWithLifecycle()
    val users by vm.users.collectAsStateWithLifecycle()
    var selectedUserId by remember { mutableStateOf<Long?>(null) }

    fun submit(value: String) {
        vm.login(value)
        pin = ""
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2B1A13), BrownDark, Color(0xFF5D4037)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(18.dp)
                .widthIn(max = 380.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(66.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Gold, GoldLight))),
                contentAlignment = Alignment.Center,
            ) { Text("مح", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = BrownDark) }
            Spacer(Modifier.height(10.dp))
            Text(settings.shopName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Brown)
            Text("إدارة الفواتير والحسابات", fontSize = 11.sp, color = Color(0xFF8D6E63))
            Spacer(Modifier.height(16.dp))
            Text("أدخل الرقم السري للدخول", fontSize = 12.sp, color = Color(0xFF6D4C41))
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (0 until 4).forEach { i ->
                    Box(
                        Modifier.size(13.dp).clip(CircleShape)
                            .background(if (pin.length > i) Brown else Color(0xFFE0D7C8)),
                    )
                }
            }
            if (message != null) {
                Spacer(Modifier.height(8.dp))
                Text(message ?: "", fontSize = 11.sp, color = Color(0xFFC62828), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(14.dp))

            val keys = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))
            keys.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (key == "⌫") Color(0xFFFFF3E0) else Color(0xFFFAF6EC))
                                .clickable(enabled = key.isNotEmpty()) {
                                    when (key) {
                                        "⌫" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        else -> {
                                            if (pin.length < 4) {
                                                pin += key
                                                if (pin.length == 4) submit(pin)
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(key, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Brown)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                users.filter { it.active }.forEach { user ->
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (selectedUserId == user.id) Brown else Color(0xFFF1EAD9))
                            .clickable { selectedUserId = user.id }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            "${user.name}",
                            fontSize = 10.sp,
                            color = if (selectedUserId == user.id) Color.White else Brown,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "يعمل بدون إنترنت • بيانات محفوظة على الجهاز\nتجريبي: المدير 1234 — العامل 1111",
                fontSize = 9.sp,
                color = Color(0xFF9E8C7A),
                textAlign = TextAlign.Center,
            )
        }
    }
}
