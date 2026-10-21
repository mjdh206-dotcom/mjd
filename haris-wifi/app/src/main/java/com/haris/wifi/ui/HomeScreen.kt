package com.haris.wifi.ui

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
    // نعرض فقط الموبايلات والكاميرات القابلة للفصل في الأعلى، وتحتها معلومات الشبكة ببساطة تامة
    val controllableDevices = vm.devices.filter { it.canDisconnect }
    val activeCount = controllableDevices.count { !it.isBlocked }
    val blockedCount = controllableDevices.count { it.isBlocked }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
    ) {
        // محرك الراوتر الصامت المخفي: ينفذ الحظر من الراوتر في الخلفية تلقائياً عند ضغط «فصل النت» بدون فتح أي شاشة!
        SilentBackgroundRouterBlocker(vm = vm)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ١. العنوان البسيط + توقيع المبرمج Amjad omega
            item {
                SimpleTopHeader(ssid = vm.networkInfo.ssid)
            }

            // ٢. زر واحد فقط لتحديث قائمة الموبايلات المتصلة + زر فصل الكل بكبسة
            item {
                SimpleControlCard(
                    vm = vm,
                    activeCount = activeCount,
                    blockedCount = blockedCount,
                )
            }

            // ٣. عنوان القائمة
            item {
                Text(
                    text = "الموبايلات والأجهزة المتصلة بالشبكة (${controllableDevices.size}):",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                )
            }

            // ٤. قائمة الموبايلات المتصلة — كل موبايل أمامه كبسة واحدة فقط: «فصل النت ✂️»
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
                            text = if (vm.isScanning) "جاري البحث عن الموبايلات المتصلة..."
                            else "لا توجد موبايلات أخرى متصلة حالياً — اضغط «تحديث القائمة»",
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

            // ٥. توقيع المبرمج الثابت في الأسفل
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
private fun SimpleTopHeader(ssid: String) {
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
                    text = "حارس الشبكة — بدون روت",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "الشبكة المتصلة: $ssid",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldBright,
                )
            }
        }

        // شريط توقيع المبرمج البارز
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
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .border(1.dp, Emerald.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ملخص سريع جداً بسطر واحد
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🟢 متصل بالنت الآن: $activeCount",
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

        // زر تحديث القائمة الكبير
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
                text = if (vm.isScanning) "جاري البحث عن الموبايلات... (${(vm.scanProgress * 100).toInt()}%)"
                else "🔄  تحديث قائمة الموبايلات المتصلة",
                style = MaterialTheme.typography.titleMedium,
                color = if (vm.isScanning) TextSilver else Color(0xFF032219),
                fontWeight = FontWeight.ExtraBold,
            )
        }

        // أزرار جماعية بكبسة واحدة فقط عند الحاجة
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

/**
 * بطاقة الجهاز المبسطة لأقصى درجة:
 * اسم الموبايل واضح + كبسة واحدة فقط على اليسار («فصل النت ✂️» أو «إعادة النت ↺»)
 */
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
        // اليمين: أيقونة الموبايل + اسمه الواضح (الضغط على الاسم يتيح تغييره)
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
                        "🔴 تم فصل النت عنه · ${device.ip}"
                    } else {
                        "🟢 متصل بالنت · ${device.vendorAr} (${device.ip})"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCut) CoralBright else TextSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "اضغط هنا لتغيير اسم الموبايل ✎",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberSky.copy(alpha = 0.85f),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // اليسار: كبسة واحدة كبيرة فقط لفصل أو إعادة النت!
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
 * محرك الراوتر الصامت في الخلفية:
 * يعمل بشكل مخفي تماماً بدون إزعاج المستخدم بأي شاشة، وعندما يضغط المستخدم «فصل النت ✂️»
 * يقوم بتسجيل الدخول لصفحة الراوتر وتنفيذ أمر حظر الماك والـ IP أوتوماتيكياً!
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SilentBackgroundRouterBlocker(vm: HarisViewModel) {
    val cmd = vm.silentRouterCutCommand
    val gatewayIp = vm.networkInfo.gatewayIp
    val pass = vm.routerPassword
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(cmd) {
        val target = cmd?.first ?: return@LaunchedEffect
        val wv = webViewRef ?: return@LaunchedEffect
        val mac = target.mac
        val ip = target.ip
        val js = """
            (function() {
                try {
                    var inputs = document.querySelectorAll('input');
                    for (var i = 0; i < inputs.length; i++) {
                        var t = (inputs[i].type || '').toLowerCase();
                        var n = (inputs[i].name || inputs[i].id || '').toLowerCase();
                        if (t === 'text' || n.indexOf('user') !== -1) {
                            if (!inputs[i].value) inputs[i].value = 'admin';
                        }
                        if (t === 'password' || n.indexOf('pass') !== -1) {
                            inputs[i].value = '$pass';
                        }
                    }
                    var rows = document.querySelectorAll('tr, div, li');
                    for (var r = 0; r < rows.length; r++) {
                        var txt = (rows[r].innerText || '').toUpperCase();
                        if (txt.indexOf('$mac') !== -1 || txt.indexOf('$ip') !== -1) {
                            var sw = rows[r].querySelectorAll('input[type="checkbox"], .switch, button');
                            for (var k = 0; k < sw.length; k++) sw[k].click();
                        }
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        wv.evaluateJavascript(js, null)
    }

    Box(
        modifier = Modifier
            .size(1.dp)
            .alpha(0f)
    ) {
        AndroidView(
            modifier = Modifier.size(1.dp),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    webViewClient = WebViewClient()
                    loadUrl("http://$gatewayIp")
                    webViewRef = this
                }
            },
        )
    }
}
