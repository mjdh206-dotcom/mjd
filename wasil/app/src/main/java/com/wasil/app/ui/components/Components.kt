package com.wasil.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wasil.app.ui.theme.Coral
import com.wasil.app.ui.theme.Gold
import com.wasil.app.ui.theme.Hairline
import com.wasil.app.ui.theme.Mint
import com.wasil.app.ui.theme.Sky
import com.wasil.app.ui.theme.SurfaceOne
import com.wasil.app.ui.theme.SurfaceTwo
import com.wasil.app.ui.theme.TextMuted
import com.wasil.app.ui.theme.TextPrimary

/* ---------- شريط علوي بسيط ---------- */
@Composable
fun TopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    icon: Painter? = null,
    onIconClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceTwo)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResourceBack(),
                    contentDescription = "رجوع",
                    modifier = Modifier.size(20.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(TextPrimary),
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (icon != null && onIconClick != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceTwo)
                    .clickable { onIconClick() },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Gold),
                )
            }
        }
    }
}

@Composable
private fun painterResourceBack(): Painter =
    androidx.compose.ui.res.painterResource(com.wasil.app.R.drawable.ic_chevron_left)

/* ---------- بطاقة عامة ---------- */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val base = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(SurfaceOne)
        .then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )
        .padding(18.dp)
    Box(base) { content() }
}

/* ---------- أزرار ---------- */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingPainter: Painter? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) Gold else Gold.copy(alpha = 0.35f))
            .clickable(enabled = enabled && !loading) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color(0xFF231703),
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingPainter != null) {
                    androidx.compose.foundation.Image(
                        painter = leadingPainter,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF231703)),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(text, color = Color(0xFF231703), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    modifier: Modifier = Modifier,
    leadingPainter: Painter? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceTwo)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingPainter != null) {
                androidx.compose.foundation.Image(
                    painter = leadingPainter,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Gold),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = Gold, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/* ---------- شارة الحالة ---------- */
@Composable
fun StatusPill(text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(7.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

/* ---------- عنوان قسم ---------- */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = TextMuted,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

/* ---------- لافتة تنبيه معلومات ---------- */
@Composable
fun InfoBanner(
    text: String,
    accent: Color = Sky,
    icon: Painter,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(14.dp),
    ) {
        androidx.compose.foundation.Image(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(accent),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
    }
}

/* ---------- حالة فارغة ---------- */
@Composable
fun EmptyState(title: String, subtitle: String, icon: Painter) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(SurfaceTwo),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(38.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(TextMuted),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center,
        )
    }
}

/* ---------- صف إعداد بعنوان ووصف ---------- */
@Composable
fun SettingRow(
    title: String,
    subtitle: String,
    icon: Painter,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceTwo),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Gold),
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

/* ---------- شريط فاصل ---------- */
@Composable
fun Hairline() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Hairline)
    )
}

@Composable
fun gradientBrush(): Brush = Brush.horizontalGradient(
    listOf(Gold.copy(alpha = 0.12f), Color.Transparent)
)

@Composable
fun statusColor(status: com.wasil.app.data.ConnectStatus): Color = when (status) {
    com.wasil.app.data.ConnectStatus.OK -> Mint
    com.wasil.app.data.ConnectStatus.CONNECTING -> Sky
    com.wasil.app.data.ConnectStatus.ERROR -> Coral
    com.wasil.app.data.ConnectStatus.IDLE -> TextMuted
}
