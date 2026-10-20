package com.shuaa.remote.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import com.shuaa.remote.ui.theme.Gold
import com.shuaa.remote.ui.theme.GoldDeep
import com.shuaa.remote.ui.theme.Hairline
import com.shuaa.remote.ui.theme.Night
import com.shuaa.remote.ui.theme.Sky
import com.shuaa.remote.ui.theme.SurfaceOne
import com.shuaa.remote.ui.theme.SurfaceTwo
import com.shuaa.remote.ui.theme.TextMuted
import com.shuaa.remote.ui.theme.TextPrimary

/** خلفية متدرّجة بتوهّج علوي */
@Composable
fun BeamBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Night, com.shuaa.remote.ui.theme.Ink)
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Gold.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.02f),
                    radius = size.width * 0.9f,
                ),
                radius = size.width * 0.9f,
                center = Offset(size.width * 0.82f, size.height * 0.02f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Sky.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.05f, size.height * 0.45f),
                    radius = size.width * 0.8f,
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width * 0.05f, size.height * 0.45f),
            )
        }
        content()
    }
}

/** بطاقة زجاجية بحدود رقيقة */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    borderColor: Color = Hairline,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(SurfaceTwo.copy(alpha = 0.92f), SurfaceOne.copy(alpha = 0.96f))
                )
            )
            .border(1.dp, borderColor.copy(alpha = 0.85f), shape)
    ) {
        content()
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
        action?.invoke()
    }
}

@Composable
fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (filled) color.copy(alpha = 0.18f) else Color.Transparent)
            .border(1.dp, color.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    enabled: Boolean = true,
    tonal: Boolean = false,
) {
    val bg = if (tonal) SurfaceTwo else Gold
    val fg = if (tonal) Gold else Color(0xFF1E1403)
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) Brush.horizontalGradient(listOf(bg, if (tonal) SurfaceTwo else Gold.copy(alpha = 0.86f))) else Brush.horizontalGradient(listOf(SurfaceTwo, SurfaceTwo)))
            .border(1.dp, if (tonal) Hairline else Color.Transparent, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(fg),
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) fg else TextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** زر ريموت: يتوهّج عند الضغط ويدعم الاستمرار */
@Composable
fun RemoteKey(
    label: String,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    onPressStart: (() -> Unit)? = null,
    onPressEnd: (() -> Unit)? = null,
    icon: Int? = null,
    subLabel: String? = null,
    size: Dp = 64.dp,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    enabled: Boolean = true,
    accent: Color = Gold,
    active: Boolean = false,
    fontSize: Int = 13,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, label = "keyScale")
    val glow by animateFloatAsState(if (pressed || active) 1f else 0f, label = "keyGlow")
    val effectiveAccent = if (enabled) accent else TextMuted

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        SurfaceTwo.copy(alpha = 1f),
                        SurfaceOne.copy(alpha = 1f),
                    )
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        effectiveAccent.copy(alpha = 0.35f * glow),
                        Color.Transparent,
                    )
                )
            )
            .border(
                width = if (glow > 0.4f) 1.2.dp else 1.dp,
                color = if (glow > 0.4f) effectiveAccent.copy(alpha = 0.9f) else Hairline,
                shape = shape,
            )
            .pointerInput(enabled, label) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onPressStart?.invoke() ?: onPress()
                        tryAwaitRelease()
                        pressed = false
                        onPressEnd?.invoke()
                    }
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (icon != null) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = label,
                    modifier = Modifier.size(size * 0.34f),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        if (glow > 0.4f) effectiveAccent else TextPrimary
                    ),
                )
                if (subLabel != null) Spacer(Modifier.height(4.dp))
            }
            if (subLabel != null || icon == null) {
                Text(
                    text = subLabel ?: label,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = fontSize.sp),
                    color = if (glow > 0.4f) effectiveAccent else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

/** شعار التطبيق */
@Composable
fun BrandMark(size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(Brush.linearGradient(listOf(Gold.copy(alpha = 0.92f), GoldDeep.copy(alpha = 0.75f))))
            .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(com.shuaa.remote.R.drawable.ic_tv),
            contentDescription = null,
            modifier = Modifier.size(size * 0.55f),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF1D1404)),
        )
    }
}

/** حلقة أشعّة متحركة حول أيقونة */
@Composable
fun PulseRings(modifier: Modifier = Modifier, color: Color = Gold) {
    val transition = rememberInfiniteTransition(label = "rings")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Restart),
        label = "ringProgress",
    )
    Canvas(modifier) {
        repeat(3) { index ->
            val p = ((progress + index / 3f) % 1f)
            val radius = size.minDimension * (0.24f + 0.34f * p)
            drawCircle(
                color = color.copy(alpha = (1f - p) * 0.35f),
                radius = radius,
                style = Stroke(width = 2.2f),
            )
        }
    }
}
