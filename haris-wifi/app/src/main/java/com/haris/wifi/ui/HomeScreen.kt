package com.haris.wifi.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.haris.wifi.HarisViewModel
import com.haris.wifi.data.ConnectedDevice
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
import com.haris.wifi.ui.theme.EmeraldDim
import com.haris.wifi.ui.theme.Obsidian
import com.haris.wifi.ui.theme.TextSilver
import com.haris.wifi.ui.theme.TextWhite

@Composable
fun HomeScreen(vm: HarisViewModel) {
    val controllableDevices = vm.devices.filter { it.canDisconnect }
    val activeCount = controllableDevices.count { !it.isBlocked }
    val blockedCount = controllableDevices.count { it.isBlocked }
    var showLiveRouterBox by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ١. العنوان البسيط + توقيع المبرمج Amjad omega
            item {
                SimpleTopHeader(
                    ssid = vm.networkInfo.ssid,
                    gatewayIp = vm.networkInfo.gatewayIp,
                )
            }

            // ٢. بطاقة التحديث + ربط الراوتر بضغطة واحدة للفصل المؤكد ١٠٠٪
            item {
                SimpleControlCard(
                    vm = vm,
                    activeCount = activeCount,
                    blockedCount = blockedCount,
                    showLiveRouterBox = showLiveRouterBox,
                    onToggleLiveRouter = { showLiveRouterBox = !showLiveRouterBox },
                )
            }

            // ٣. محرك الراوتر الذكي (يعمل في الخلفية تلقائياً، أو يظهر عند الطلب لتأكيد الفصل من الراوتر)
            item {
                SmartRouterConnectorEngine(
                    vm = vm,
                    visible = showLiveRouterBox,
                )
            }

            // ٤. عنوان قائمة الأجهزة المتصلة بالراوتر
            item {
                Text(
                    text = "الأجهزة والموبايلات المتصلة بالراوتر (${controllableDevices.size}):",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                )
            }

            // ٥. قائمة الأجهزة المتصلة — كل جهاز أمامه كبسة واحدة: «فصل النت ✂️»
            if (controllableDevices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(CardSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (vm.isScanning) "جاري سحب الأجهزة المتصلة من الراوتر..."
                            else "لا توجد أجهزة أخرى متصلة حالياً — اضغط «تحديث الأجهزة المتصلة»",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextWhite,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                items(
                    items = controllableDevices,
                    key = { it.uniqueKey + "_" + it.ip },
                ) { device ->
                    SimplePhoneRowCard(
                        device = device,
                        onOneClickCut = { vm.toggleInstantCut(device) },
                        onRenameClick = { vm.openDeviceSheet(device) },
                    )
                }
            }

            // ٦. توقيع المبرمج الثابت في الأسفل
            item {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "تمت برمجة هذا التطبيق بواسطة Amjad omega",
                        style = MaterialTheme.typography.titleSmall,
                        color = AmberWarn,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SimpleTopHeader(ssid: String, gatewayIp: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Emerald, EmeraldDim)))
                    .border(1.dp, EmeraldBright.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("🛡️", fontSize = 24.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "حارس الشبكة — قاطع نت الراوتر",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "الشبكة: $ssid · الراوتر: $gatewayIp",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldBright,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF0A2E26), Color(0xFF13233F), Color(0xFF2B1B06))
                    )
                )
                .border(1.dp, AmberWarn.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                .padding(vertical = 9.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✨ تمت برمجة هذا التطبيق بواسطة Amjad omega ✨",
                style = MaterialTheme.typography.titleSmall,
                color = AmberWarn,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SimpleControlCard(
    vm: HarisViewModel,
    activeCount: Int,
    blockedCount: Int,
    showLiveRouterBox: Boolean,
    onToggleLiveRouter: () -> Unit,
) {
    var passInput by remember(vm.routerPassword) { mutableStateOf(vm.routerPassword) }
    var showPassEditor by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .border(1.dp, Emerald.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🟢 متصل بالراوتر: $activeCount",
                style = MaterialTheme.typography.titleSmall,
                color = EmeraldBright,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "🔴 مفصول عنه النت: $blockedCount",
                style = MaterialTheme.typography.titleSmall,
                color = CoralBright,
                fontWeight = FontWeight.Bold,
            )
        }

        // شريط صغير لضبط باسورد الراوتر (لأن فصل النت بدون روت يتم عبر الراوتر مباشرة)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DeepNavy)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🔑 كلمة سر الراوتر: ${vm.routerPassword}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSilver,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardElevated)
                        .clickable { showPassEditor = !showPassEditor }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (showPassEditor) "إغلاق" else "تعديل الباسورد",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberSky,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardElevated)
                        .clickable { onToggleLiveRouter() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (showLiveRouterBox) "إخفاء الراوتر ▲" else "شاشة الراوتر ▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberWarn,
                    )
                }
            }
        }

        AnimatedVisibility(visible = showPassEditor) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = passInput,
                    onValueChange = { passInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("باسورد الراوتر (الافتراضي admin)", color = TextSilver) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = DeepNavy,
                        unfocusedContainerColor = DeepNavy,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                    ),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Emerald)
                        .clickable {
                            vm.updateRouterPassword(passInput)
                            showPassEditor = false
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                ) {
                    Text("حفظ وربط", color = Color(0xFF032219), fontWeight = FontWeight.Bold)
                }
            }
        }

        if (vm.isScanning) {
            LinearProgressIndicator(
                progress = { vm.scanProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = EmeraldBright,
                trackColor = DeepNavy,
            )
            Text(
                text = vm.scanStatusText,
                style = MaterialTheme.typography.bodySmall,
                color = EmeraldBright,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (vm.isScanning) CardElevated else Emerald)
                .clickable(enabled = !vm.isScanning) { vm.startScan(deepCameraScan = true) }
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (vm.isScanning) "جاري سحب المتصلين من الراوتر... (${(vm.scanProgress * 100).toInt()}%)"
                else "🔄  إظهار وتحديث الأجهزة المتصلة بالراوتر",
                style = MaterialTheme.typography.titleMedium,
                color = if (vm.isScanning) TextSilver else Color(0xFF032219),
                fontWeight = FontWeight.ExtraBold,
            )
        }

        if (activeCount > 0 || blockedCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (activeCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CoralRed.copy(alpha = 0.2f))
                            .border(1.dp, CoralRed, RoundedCornerShape(12.dp))
                            .clickable { vm.cutAllStrangers() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "✂️ فصل النت عن الكل ($activeCount)",
                            style = MaterialTheme.typography.labelLarge,
                            color = CoralBright,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (blockedCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Emerald.copy(alpha = 0.2f))
                            .border(1.dp, Emerald, RoundedCornerShape(12.dp))
                            .clickable { vm.reconnectAllDevices() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "↺ إعادة النت للكل ($blockedCount)",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldBright,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimplePhoneRowCard(
    device: ConnectedDevice,
    onOneClickCut: () -> Unit,
    onRenameClick: () -> Unit,
) {
    val isCut = device.isBlocked

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isCut) Color(0xFF230B17) else CardSurface)
            .border(
                1.5.dp,
                if (isCut) CoralRed else Emerald.copy(alpha = 0.45f),
                RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onRenameClick() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isCut) CoralDim else CardElevated),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isCut) "⛔" else device.deviceType.emoji,
                    fontSize = 24.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCut) CoralBright else TextWhite,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (isCut) {
                        "🔴 تم فصل النت عنه من الراوتر · ${device.ip}"
                    } else {
                        "🟢 متصل بالراوتر · ${device.ip} · ${device.mac}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCut) CoralBright else TextSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "اضغط هنا لتغيير اسم الجهاز ✎",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberSky.copy(alpha = 0.85f),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (isCut) Emerald else CoralRed)
                .clickable { onOneClickCut() }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isCut) "↺ إعادة النت" else "✂️ فصل النت",
                style = MaterialTheme.typography.titleSmall,
                color = if (isCut) Color(0xFF032219) else TextWhite,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

/**
 * محرك الربط المباشر مع الراوتر:
 * ١. يدخل إلى صفحة الراوتر (192.168.1.1) تلقائياً بباسورد الراوتر المحفوظ
 * ٢. يقرأ الأسماء وعناوين الـ MAC الحقيقية ١٠٠٪ للأجهزة المتصلة بالراوتر ويرسلها للتطبيق
 * ٣. عند الضغط على «✂️ فصل النت» يقوم بالبحث عن الماك والـ IP داخل الراوتر (حتى داخل الـ iframes)
 *    ويضغط زر الحظر / إيقاف الإنترنت / الإضافة لقائمة الحظر تلقائياً!
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SmartRouterConnectorEngine(
    vm: HarisViewModel,
    visible: Boolean,
) {
    val cmd = vm.silentRouterCutCommand
    val gatewayIp = vm.networkInfo.gatewayIp
    val pass = vm.routerPassword
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // عند تغيير باسورد الراوتر نعيد تسجيل الدخول تلقائياً
    LaunchedEffect(pass, gatewayIp) {
        webViewRef?.loadUrl("http://$gatewayIp")
    }

    // عند ضغط «فصل النت ✂️» على أي جهاز، ننفذ الحظر التلقائي داخل جلسة الراوتر الحية
    LaunchedEffect(cmd) {
        val target = cmd?.first ?: return@LaunchedEffect
        val wv = webViewRef ?: return@LaunchedEffect
        val mac = target.mac
        val ip = target.ip
        val jsBlock = buildRouterAutoBlockJs(mac = mac, ip = ip, pass = pass)
        wv.evaluateJavascript(jsBlock, null)
    }

    val boxModifier = if (visible) {
        Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, AmberWarn, RoundedCornerShape(16.dp))
    } else {
        Modifier
            .size(1.dp)
            .alpha(0f)
    }

    Box(modifier = boxModifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onPageDump(content: String) {
                                vm.mergeRouterDomText(content)
                            }
                        },
                        "HarisRouterBridge"
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val autoLoginAndExtractJs = """
                                (function() {
                                    try {
                                        var docs = [document];
                                        var iframes = document.querySelectorAll('iframe, frame');
                                        for (var f = 0; f < iframes.length; f++) {
                                            try { if (iframes[f].contentDocument) docs.push(iframes[f].contentDocument); } catch(e) {}
                                        }
                                        var fullDump = '';
                                        for (var d = 0; d < docs.length; d++) {
                                            var doc = docs[d];
                                            fullDump += '\n' + (doc.body ? doc.body.innerHTML : '');
                                            var passInput = doc.querySelector('input[type="password"]');
                                            if (passInput && !passInput.value) {
                                                var userInputs = doc.querySelectorAll('input[type="text"], input:not([type])');
                                                for (var u = 0; u < userInputs.length; u++) {
                                                    if (!userInputs[u].value) {
                                                        userInputs[u].value = 'admin';
                                                        userInputs[u].dispatchEvent(new Event('input', {bubbles:true}));
                                                    }
                                                }
                                                passInput.value = '${vm.routerPassword}';
                                                passInput.dispatchEvent(new Event('input', {bubbles:true}));
                                                var btns = doc.querySelectorAll('button, input[type="submit"], input[type="button"], a');
                                                for (var b = 0; b < btns.length; b++) {
                                                    var t = (btns[b].innerText || btns[b].value || '').toLowerCase();
                                                    if (t.indexOf('login') !== -1 || t.indexOf('log in') !== -1 || t.indexOf('دخول') !== -1) {
                                                        btns[b].click();
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        if (window.HarisRouterBridge && fullDump.length > 0) {
                                            window.HarisRouterBridge.onPageDump(fullDump);
                                        }
                                    } catch(e) {}
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(autoLoginAndExtractJs, null)
                        }
                    }
                    loadUrl("http://$gatewayIp")
                    webViewRef = this
                }
            },
        )
    }
}

private fun buildRouterAutoBlockJs(mac: String, ip: String, pass: String): String {
    val macDash = mac.replace(':', '-')
    return """
        (function() {
            try {
                var docs = [document];
                var iframes = document.querySelectorAll('iframe, frame');
                for (var f = 0; f < iframes.length; f++) {
                    try { if (iframes[f].contentDocument) docs.push(iframes[f].contentDocument); } catch(e) {}
                }
                for (var d = 0; d < docs.length; d++) {
                    var doc = docs[d];
                    // ١. البحث عن سطر الجهاز المتصل (عبر MAC أو IP) وإيقاف مفتاح الإنترنت أو ضغط Block/Kick
                    var rows = doc.querySelectorAll('tr, div, li');
                    for (var r = 0; r < rows.length; r++) {
                        var txt = (rows[r].innerText || '').toUpperCase();
                        if (txt.indexOf('$mac') !== -1 || txt.indexOf('$macDash') !== -1 || txt.indexOf('$ip') !== -1) {
                            var controls = rows[r].querySelectorAll('input[type="checkbox"], .switch, button, a, input[type="button"]');
                            for (var c = 0; c < controls.length; c++) {
                                var ct = (controls[c].innerText || controls[c].value || controls[c].title || '').toLowerCase();
                                if (controls[c].type === 'checkbox' || ct.indexOf('block') !== -1 || ct.indexOf('kick') !== -1 || ct.indexOf('disable') !== -1 || ct.indexOf('حظر') !== -1 || ct.indexOf('فصل') !== -1 || ct.indexOf('إيقاف') !== -1) {
                                    controls[c].click();
                                }
                            }
                        }
                    }
                    // ٢. لصق الماك في أي حقل حظر MAC Filter وحفظه
                    var inputs = doc.querySelectorAll('input[type="text"], input:not([type])');
                    for (var i = 0; i < inputs.length; i++) {
                        var id = (inputs[i].id || inputs[i].name || inputs[i].placeholder || '').toLowerCase();
                        if (id.indexOf('mac') !== -1 || id.indexOf('filter') !== -1) {
                            inputs[i].value = '$mac';
                            inputs[i].dispatchEvent(new Event('input', {bubbles:true}));
                        }
                    }
                    var saveBtns = doc.querySelectorAll('button, input[type="submit"], input[type="button"]');
                    for (var s = 0; s < saveBtns.length; s++) {
                        var st = (saveBtns[s].innerText || saveBtns[s].value || '').toLowerCase();
                        if (st.indexOf('save') !== -1 || st.indexOf('apply') !== -1 || st.indexOf('add') !== -1 || st.indexOf('block') !== -1 || st.indexOf('حفظ') !== -1 || st.indexOf('تطبيق') !== -1) {
                            saveBtns[s].click();
                        }
                    }
                }
            } catch(e) {}
        })();
    """.trimIndent()
}
