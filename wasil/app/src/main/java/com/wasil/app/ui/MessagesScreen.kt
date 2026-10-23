package com.wasil.app.ui

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.wasil.app.R
import com.wasil.app.WasilViewModel
import com.wasil.app.data.SmsMessage
import com.wasil.app.ui.components.EmptyState
import com.wasil.app.ui.components.TopBar
import com.wasil.app.ui.theme.Gold
import com.wasil.app.ui.theme.Hairline
import com.wasil.app.ui.theme.SurfaceOne
import com.wasil.app.ui.theme.SurfaceTwo
import com.wasil.app.ui.theme.TextMuted
import com.wasil.app.ui.theme.TextPrimary

@Composable
fun MessagesScreen(vm: WasilViewModel, onBack: () -> Unit) {
    val messages by vm.messages.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(Unit) { vm.refresh(showFeedback = false) }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = "الرسائل الواردة",
            onBack = onBack,
            icon = painterResource(R.drawable.ic_refresh),
            onIconClick = { vm.refresh(showFeedback = true) },
        )

        if (messages.isEmpty()) {
            EmptyState(
                title = "لا توجد رسائل بعد",
                subtitle = "عند وصول رسالة SMS إلى رقمك الأمريكي ستظهر هنا فور المزامنة، مع تمييز كود التفعيل وزر نسخ سريع.",
                icon = painterResource(R.drawable.ic_chat),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp, end = 18.dp, top = 4.dp, bottom = 28.dp
                ),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageCard(
                        message = message,
                        onCopy = { text ->
                            clipboard.setText(AnnotatedString(text))
                            Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageCard(message: SmsMessage, onCopy: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceOne)
            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceTwo),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painterResource(R.drawable.ic_chat),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        colorFilter = ColorFilter.tint(Gold),
                    )
                }
                Spacer(Modifier.width(11.dp))
                Text(
                    message.from,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    TimeFmt.relative(message.ts),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                message.body,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )

            if (!message.code.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Gold.copy(alpha = 0.12f))
                        .clickable { onCopy(message.code) }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "كود التفعيل",
                        style = MaterialTheme.typography.labelMedium,
                        color = Gold,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        message.code,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = Gold,
                    )
                    Spacer(Modifier.width(12.dp))
                    Image(
                        painterResource(R.drawable.ic_copy),
                        contentDescription = "نسخ الكود",
                        modifier = Modifier.size(19.dp),
                        colorFilter = ColorFilter.tint(Gold),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "واردة في ${TimeFmt.clock(message.ts)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
    }
}
