package com.haris.wifi.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.haris.wifi.HarisViewModel
import com.haris.wifi.data.ConnectedDevice
import com.haris.wifi.data.MacVendorDb
import com.haris.wifi.ui.theme.AmberDim
import com.haris.wifi.ui.theme.AmberWarn
import com.haris.wifi.ui.theme.BorderSubtle
import com.haris.wifi.ui.theme.CardElevated
import com.haris.wifi.ui.theme.CardSurface
import com.haris.wifi.ui.theme.CoralBright
import com.haris.wifi.ui.theme.CoralDim
import com.haris.wifi.ui.theme.CoralRed
import com.haris.wifi.ui.theme.CyberSky
import com.haris.wifi.ui.theme.DeepNavy
import com.haris.wifi.ui.theme.Emerald
import com.haris.wifi.ui.theme.EmeraldBright
import com.haris.wifi.ui.theme.Obsidian
import com.haris.wifi.ui.theme.TextSilver
import com.haris.wifi.ui.theme.TextWhite

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RouterBlockerScreen(
    vm: HarisViewModel,
    targetDevice: ConnectedDevice?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val gatewayIp = vm.networkInfo.gatewayIp
    val routerUrl = "http://$gatewayIp"

    var selectedGuide by remember { mutableStateOf(vm.currentRouterGuide()) }
    var showGuideSteps by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ١. الشريط العلوي
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardElevated)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("→ رجوع", color = TextWhite, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "لوحة حظر الراوتر الذكية",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                    )
                    Text(
                        text = "بوابة الراوتر: $routerUrl",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberSky,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .clickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(routerUrl)))
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text("متصفح خارجي ↗", color = EmeraldBright, style = MaterialTheme.typography.labelSmall)
            }
        }

        // ٢. بطاقة الجهاز المراد حظره + أزرار التعبئة التلقائية بضغطة واحدة
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CardSurface)
                .border(1.dp, CoralRed.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (targetDevice != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "الجهاز المستهدف بالحظر: ${targetDevice.displayName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = CoralBright,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "MAC: ${targetDevice.mac}  ·  IP: ${targetDevice.ip}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextWhite,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(CoralDim)
                            .clickable { vm.copyToClipboard("MAC", targetDevice.mac) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text("نسخ الماك 📋", color = CoralBright, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // أزرار المساعد الذكي داخل صفحة الراوتر
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Emerald)
                        .clickable {
                            val user = selectedGuide.defaultUser
                            val pass = if (selectedGuide.defaultPass.contains(" ")) "admin" else selectedGuide.defaultPass
                            val js = """
                                (function() {
                                    var inputs = document.querySelectorAll('input');
                                    for (var i = 0; i < inputs.length; i++) {
                                        var t = (inputs[i].type || '').toLowerCase();
                                        var n = (inputs[i].name || inputs[i].id || '').toLowerCase();
                                        if (t === 'text' || n.indexOf('user') !== -1 || n.indexOf('name') !== -1) {
                                            if (!inputs[i].value) inputs[i].value = '$user';
                                        }
                                        if (t === 'password' || n.indexOf('pass') !== -1 || n.indexOf('pwd') !== -1) {
                                            inputs[i].value = '$pass';
                                        }
                                    }
                                })();
                            """.trimIndent()
                            webViewRef?.evaluateJavascript(js, null)
                            vm.showBanner("تمت تعبئة بيانات الدخول ($user / $pass) تلقائياً")
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "🔑 تعبئة الدخول تلقائياً",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF032219),
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (targetDevice != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CoralRed)
                            .clickable {
                                val mac = targetDevice.mac
                                vm.copyToClipboard("MAC", mac)
                                val js = """
                                    (function() {
                                        var active = document.activeElement;
                                        if (active && active.tagName === 'INPUT') {
                                            active.value = '$mac';
                                            return;
                                        }
                                        var inputs = document.querySelectorAll('input[type="text"], input:not([type])');
                                        for (var i = 0; i < inputs.length; i++) {
                                            var id = (inputs[i].id || inputs[i].name || '').toLowerCase();
                                            if (id.indexOf('mac') !== -1 || id.indexOf('filter') !== -1 || i === inputs.length - 1) {
                                                inputs[i].value = '$mac';
                                            }
                                        }
                                    })();
                                """.trimIndent()
                                webViewRef?.evaluateJavascript(js, null)
                                vm.showBanner("تم لصق عنوان الماك ($mac) في الحقل مباشرة!")
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "📌 لصق الماك في الصفحة",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // زر إظهار/إخفاء خطوات الحظر السريعة حسب نوع الراوتر
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showGuideSteps = !showGuideSteps }
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "💡 خطوات الحظر السريعة (${selectedGuide.nameAr})",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberWarn,
                )
                Text(
                    text = if (showGuideSteps) "إخفاء ▲" else "عرض الخطوات ▼",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSilver,
                )
            }

            AnimatedVisibility(visible = showGuideSteps) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeepNavy)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(MacVendorDb.routerGuides) { g ->
                            val active = g.id == selectedGuide.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) AmberDim else CardElevated)
                                    .border(
                                        1.dp,
                                        if (active) AmberWarn else BorderSubtle,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedGuide = g }
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = g.nameAr.substringBefore(" ("),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) AmberWarn else TextSilver,
                                )
                            }
                        }
                    }

                    Text(
                        text = "المسار: ${selectedGuide.menuPathAr}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberSky,
                    )
                    selectedGuide.quickStepsAr.forEach { step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextWhite,
                        )
                    }
                }
            }
        }

        // ٣. متصفح صفحة الراوتر المدمج
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                .background(Color.White),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        loadUrl(routerUrl)
                        webViewRef = this
                    }
                },
            )
        }
    }
}
