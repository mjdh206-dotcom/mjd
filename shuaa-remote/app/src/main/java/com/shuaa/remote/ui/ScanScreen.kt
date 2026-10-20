package com.shuaa.remote.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.shuaa.remote.ir.ScanResult
import com.shuaa.remote.ir.ScreenAnalyzer
import com.shuaa.remote.ui.components.GlassCard
import com.shuaa.remote.ui.components.Pill
import com.shuaa.remote.ui.components.PrimaryButton
import com.shuaa.remote.ui.theme.Coral
import com.shuaa.remote.ui.theme.Gold
import com.shuaa.remote.ui.theme.Hairline
import com.shuaa.remote.ui.theme.Mint
import com.shuaa.remote.ui.theme.Sky
import com.shuaa.remote.ui.theme.SurfaceOne
import com.shuaa.remote.ui.theme.TextMuted
import com.shuaa.remote.ui.theme.TextPrimary
import java.util.concurrent.Executors

@Composable
fun ScanScreen(
    onClose: () -> Unit,
    onContinue: () -> Unit,
    onManual: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var scan by remember { mutableStateOf(ScanResult()) }
    var cameraError by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(hasPermission) {
        if (!hasPermission) return@DisposableEffect onDispose { }
        var executor: java.util.concurrent.ExecutorService? = null
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                executor = Executors.newSingleThreadExecutor()
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                val analyzer = ScreenAnalyzer { result ->
                    previewView.post { scan = result }
                }
                analysis.setAnalyzer(executor!!, analyzer)
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            } catch (_: Exception) {
                cameraError = true
            }
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            try {
                if (future.isDone) future.get().unbindAll()
            } catch (_: Exception) {
            }
            executor?.shutdown()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        if (hasPermission && !cameraError) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0A1120), Color.Black))),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text(
                        if (cameraError) "تعذّر تشغيل الكاميرا" else "نحتاج إذن الكاميرا لرصد الشاشة",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "لا تُحفظ أي صورة — التحليل يتم داخل الهاتف فقط.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    PrimaryButton(
                        text = if (cameraError) "اختيار الماركة يدوياً" else "السماح بالكاميرا",
                        onClick = { if (cameraError) onManual() else launcher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // تعتيم علوي وسفلي لقراءة النصوص
        Box(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)))
                .align(Alignment.TopCenter)
        )

        // إطار المسح
        ScanOverlay(
            detected = scan.detected,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 26.dp),
        )

        // الترويسة
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                    .clickableNoRipple(onClose),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(com.shuaa.remote.R.drawable.ic_close),
                    contentDescription = "إغلاق",
                    modifier = Modifier.size(18.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "مسح الشاشة",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (scan.detected) "تم رصد شاشة ${scan.aspect ?: ""}" else "وجّه الهاتف نحو الشاشة",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
            Pill(
                text = if (scan.detected) "رُصدت" else "جارٍ المسح",
                color = if (scan.detected) Mint else Sky,
                filled = true,
            )
        }

        // اللوحة السفلية
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            GlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (scan.detected) Mint.copy(alpha = 0.16f) else Sky.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(
                                    if (scan.detected) com.shuaa.remote.R.drawable.ic_check else com.shuaa.remote.R.drawable.ic_scan
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(if (scan.detected) Mint else Sky),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                scan.hint,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                            Text(
                                "الإضاءة ${scan.brightness} · التباين ${scan.contrast}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(
                        text = "متابعة واختيار الماركة",
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        icon = androidx.compose.ui.res.painterResource(com.shuaa.remote.R.drawable.ic_chevron_left),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "لا تعرف الماركة؟ اختر «غير معروفة» وسنجرّب الأكواد الشائعة تلقائياً.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanOverlay(detected: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scanline")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "line",
    )
    val accent = if (detected) Mint else Gold
    Canvas(modifier.height(300.dp)) {
        val corner = 34f
        val stroke = 3.5f
        val w = size.width
        val h = size.height
        // زوايا الإطار
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, corner); lineTo(0f, 0f); lineTo(corner, 0f)
            moveTo(w - corner, 0f); lineTo(w, 0f); lineTo(w, corner)
            moveTo(w, h - corner); lineTo(w, h); lineTo(w - corner, h)
            moveTo(corner, h); lineTo(0f, h); lineTo(0f, h - corner)
        }
        drawPath(path, color = accent, style = Stroke(width = stroke, cap = StrokeCap.Round))

        // خط المسح
        val y = h * (0.08f + 0.84f * progress)
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    accent.copy(alpha = 0.15f),
                    accent.copy(alpha = 0.8f),
                    accent.copy(alpha = 0.15f),
                    Color.Transparent,
                )
            ),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 2.2f,
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, accent.copy(alpha = 0.08f)),
                startY = y - 90f,
                endY = y,
            ),
            topLeft = Offset(0f, y - 90f),
            size = Size(w, 90f),
        )
    }
}

/** ضغطة بدون موجة Material */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.pointerInput(Unit) {
        detectTapGestures(onTap = { onClick() })
    }
