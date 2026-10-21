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
    var showGuideSteps by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        text = "قفل وحظر الجهاز من الراوتر (١٠٠٪)",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                    )
                    Text(
                        text = "تمت برمجة هذا التطبيق بواسطة Amjad omega",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberWarn,
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

        // ٢. بطاقة التحكم الأوتوماتيكي بالراوتر
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
                            text = "الجهاز المراد قطعه: ${targetDevice.displayName}",
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

            // أزرار المساعد الأوتوماتيكي
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
                                    var passFilled = false;
                                    for (var i = 0; i < inputs.length; i++) {
                                        var t = (inputs[i].type || '').toLowerCase();
                                        var n = (inputs[i].name || inputs[i].id || '').toLowerCase();
                                        if (t === 'text' || n.indexOf('user') !== -1 || n.indexOf('name') !== -1) {
                                            if (!inputs[i].value) {
                                                inputs[i].value = '$user';
                                                inputs[i].dispatchEvent(new Event('input', {bubbles:true}));
                                            }
                                        }
                                        if (t === 'password' || n.indexOf('pass') !== -1 || n.indexOf('pwd') !== -1) {
                                            inputs[i].value = '$pass';
                                            inputs[i].dispatchEvent(new Event('input', {bubbles:true}));
                                            passFilled = true;
                                        }
                                    }
                                    var btns = document.querySelectorAll('button, input[type="submit"], input[type="button"], a.button');
                                    for (var j = 0; j < btns.length; j++) {
                                        var txt = (btns[j].innerText || btns[j].value || '').toLowerCase();
                                        if (txt.indexOf('login') !== -1 || txt.indexOf('log in') !== -1 || txt.indexOf('دخول') !== -1) {
                                            btns[j].click();
                                            break;
                                        }
                                    }
                                })();
                            """.trimIndent()
                            webViewRef?.evaluateJavascript(js, null)
                            vm.showBanner("تمت تعبئة الدخول ($user / $pass) والضغط على دخول تلقائياً!")
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "🔑 دخول تلقائي للراوتر",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF032219),
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (targetDevice != null) {
                    Box(
                        modifier = Modifier
                            .weight(1.15f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CoralRed)
                            .clickable {
                                val mac = targetDevice.mac
                                val ip = targetDevice.ip
                                vm.copyToClipboard("MAC", mac)
                                val js = """
                                    (function() {
                                        var mac = '$mac';
                                        var ip = '$ip';
                                        // ١. البحث عن سطر الجهاز في جدول الأجهزة المتصلة وإيقاف مفتاح الإنترنت أمامه
                                        var rows = document.querySelectorAll('tr, div.device-item, li');
                                        for (var r = 0; r < rows.length; r++) {
                                            var html = (rows[r].innerText || '').toUpperCase();
                                            if (html.indexOf(mac.toUpperCase()) !== -1 || html.indexOf(ip) !== -1) {
                                                var toggles = rows[r].querySelectorAll('input[type="checkbox"], .switch, button, a');
                                                for (var k = 0; k < toggles.length; k++) {
                                                    toggles[k].click();
                                                }
                                            }
                                        }
                                        // ٢. لصق الماك في أي حقل حظر (MAC Filter / Blacklist)
                                        var active = document.activeElement;
                                        if (active && active.tagName === 'INPUT') {
                                            active.value = mac;
                                            active.dispatchEvent(new Event('input', {bubbles:true}));
                                        }
                                        var inputs = document.querySelectorAll('input[type="text"], input:not([type])');
                                        for (var i = 0; i < inputs.length; i++) {
                                            var id = (inputs[i].id || inputs[i].name || inputs[i].placeholder || '').toLowerCase();
                                            if (id.indexOf('mac') !== -1 || id.indexOf('filter') !== -1 || i === inputs.length - 1) {
                                                inputs[i].value = mac;
                                                inputs[i].dispatchEvent(new Event('input', {bubbles:true}));
                                            }
                                        }
                                        // ٣. الضغط على زر حفظ / حظر (Save / Apply / Block)
                                        var btns = document.querySelectorAll('button, input[type="button"], input[type="submit"]');
                                        for (var b = 0; b < btns.length; b++) {
                                            var t = (btns[b].innerText || btns[b].value || '').toLowerCase();
                                            if (t.indexOf('block') !== -1 || t.indexOf('save') !== -1 || t.indexOf('apply') !== -1 || t.indexOf('حظر') !== -1 || t.indexOf('حفظ') !== -1) {
                                                btns[b].click();
                                            }
                                        }
                                    })();
                                """.trimIndent()
                                webViewRef?.evaluateJavascript(js, null)
                                vm.showBanner("تم تنفيذ الحظر الأوتوماتيكي ولصق الماك ($mac)!")
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "⚡ حظر أوتوماتيكي للجهاز",
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
                    text = "💡 تعليمات الراوتر (${selectedGuide.nameAr})",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberWarn,
                )
                Text(
                    text = if (showGuideSteps) "إخفاء ▲" else "عرض الشرح ▼",
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
