package com.haris.wifi.net

import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

/**
 * محرك كشف الأسماء الحقيقية للموبايلات والأجهزة المتصلة بدون روت:
 * ١. استعلام mDNS العكسي (Port 5353) — يكشف أسماء هواتف آيفون وأندرويد الحديثة
 * ٢. استعلام NetBIOS NBSTAT (Port 137) — يكشف أسماء الأجهزة على الشبكة المحلية
 * ٣. قراءة ملف وصف UPnP XML (<friendlyName>)
 * ٤. قراءة جدول المتصلين من صفحة الراوتر إن كانت متاحة
 */
object DeviceNameResolver {

    fun resolveBestName(ip: String, ssdpLocationUrl: String? = null): String {
        // ١. محاولة mDNS (يكشف أسماء الموبايلات مثل iPhone و Galaxy)
        queryMdnsPtr(ip)?.takeIf { it.isNotBlank() }?.let { return cleanHostName(it) }

        // ٢. محاولة NetBIOS NBSTAT على منفذ 137
        queryNetBiosName(ip)?.takeIf { it.isNotBlank() }?.let { return cleanHostName(it) }

        // ٣. محاولة قراءة اسم الجهاز من UPnP XML إن وجد
        if (!ssdpLocationUrl.isNullOrBlank()) {
            fetchUpnpFriendlyName(ssdpLocationUrl)?.takeIf { it.isNotBlank() }?.let { return it }
        }

        // ٤. محاولة DNS العكسي من الراوتر
        runCatching {
            val addr = InetAddress.getByName(ip)
            val canonical = addr.canonicalHostName ?: ""
            if (canonical.isNotBlank() && canonical != ip) {
                return cleanHostName(canonical)
            }
            val host = addr.hostName ?: ""
            if (host.isNotBlank() && host != ip) {
                return cleanHostName(host)
            }
        }

        return ""
    }

    private fun cleanHostName(raw: String): String {
        return raw
            .removeSuffix(".local.")
            .removeSuffix(".local")
            .removeSuffix(".home")
            .removeSuffix(".lan")
            .replace('_', '-')
            .trim()
    }

    /** إرسال استعلام DNS PTR عبر mDNS (منفذ 5353) للحصول على اسم الموبايل */
    private fun queryMdnsPtr(ip: String): String? {
        return runCatching {
            val parts = ip.split('.')
            if (parts.size != 4) return null
            val reversedLabels = listOf(parts[3], parts[2], parts[1], parts[0], "in-addr", "arpa")

            val out = ByteArrayOutputStream()
            // DNS Header (ID=0x1234, Flags=0x0000, QDCOUNT=1)
            out.write(byteArrayOf(0x12, 0x34, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
            for (label in reversedLabels) {
                val bytes = label.toByteArray(Charsets.US_ASCII)
                out.write(bytes.size)
                out.write(bytes)
            }
            out.write(0x00) // نهاية الاسم
            out.write(byteArrayOf(0x00, 0x0C)) // QTYPE = PTR (12)
            out.write(byteArrayOf(0x00, 0x01)) // QCLASS = IN (1)

            val queryBytes = out.toByteArray()
            DatagramSocket().use { socket ->
                socket.soTimeout = 200
                // نرسل الاستعلام مباشرة إلى الجهاز وإلى مجموعة mDNS
                val targetAddr = InetAddress.getByName(ip)
                socket.send(DatagramPacket(queryBytes, queryBytes.size, targetAddr, 5353))
                runCatching {
                    val mdnsGroup = InetAddress.getByName("224.0.0.251")
                    socket.send(DatagramPacket(queryBytes, queryBytes.size, mdnsGroup, 5353))
                }

                val buf = ByteArray(512)
                val resp = DatagramPacket(buf, buf.size)
                socket.receive(resp)
                parseDnsAnswerName(resp.data, resp.length)
            }
        }.getOrNull()
    }

    private fun parseDnsAnswerName(data: ByteArray, length: Int): String? {
        if (length < 24) return null
        val str = StringBuilder()
        var idx = length - 1
        // استخراج آخر سلسلة نصية مقروءة تنتهي بـ .local في رد mDNS
        val ascii = String(data, 12, length - 12, Charsets.ISO_8859_1)
        val localIdx = ascii.indexOf("local", ignoreCase = true)
        if (localIdx > 2) {
            val sub = ascii.substring(0, localIdx)
            val cleaned = sub.replace(Regex("[^A-Za-z0-9\\-_.]"), " ")
                .trim()
                .split(Regex("\\s+"))
                .lastOrNull { it.length >= 3 }
            if (!cleaned.isNullOrBlank()) {
                return cleaned
            }
        }
        return if (str.isNotEmpty()) str.toString() else null
    }

    /** إرسال حزمة NetBIOS Node Status (NBSTAT) على منفذ UDP 137 */
    private fun queryNetBiosName(ip: String): String? {
        return runCatching {
            val nbstatQuery = byteArrayOf(
                0x80.toByte(), 0x94.toByte(), 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x20, 0x43, 0x4B, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
                0x41, 0x41, 0x41, 0x00, 0x00, 0x21, 0x00, 0x01
            )
            DatagramSocket().use { socket ->
                socket.soTimeout = 190
                val addr = InetAddress.getByName(ip)
                socket.send(DatagramPacket(nbstatQuery, nbstatQuery.size, addr, 137))
                val buf = ByteArray(512)
                val resp = DatagramPacket(buf, buf.size)
                socket.receive(resp)
                if (resp.length > 57) {
                    val numNames = resp.data[56].toInt() and 0xFF
                    if (numNames > 0 && resp.length >= 57 + 15) {
                        val rawName = String(resp.data, 57, 15, Charsets.US_ASCII)
                            .replace(Regex("[^A-Za-z0-9\\-_ ]"), "")
                            .trim()
                        if (rawName.length >= 2) return rawName
                    }
                }
            }
            null
        }.getOrNull()
    }

    private fun fetchUpnpFriendlyName(urlStr: String): String? {
        return runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 250
                readTimeout = 250
            }
            val xml = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val match = Regex("<friendlyName>(.*?)</friendlyName>", RegexOption.IGNORE_CASE).find(xml)
            match?.groupValues?.getOrNull(1)?.trim()
        }.getOrNull()
    }
}
