package com.haris.wifi.net

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * محرك الاتصال المباشر بالراوتر (بدون روت):
 * ١. يجلب القائمة الحقيقية للأجهزة المتصلة بالراوتر (الاسم الحقيقي + الـ MAC الحقيقي + الـ IP)
 * ٢. ينفذ الفصل الفعلي النهائي من الراوتر باستخدام عنوان الـ MAC الحقيقي للجهاز
 */
object RouterApiEngine {

    data class RouterClientItem(
        val ip: String,
        val mac: String,
        val hostname: String,
    )

    private val macRegex = Regex("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})")
    private val ipRegex = Regex("\\b(?:192\\.168|10\\.\\d{1,3}|172\\.(?:1[6-9]|2\\d|3[01]))\\.\\d{1,3}\\.\\d{1,3}\\b")

    /**
     * سحب الأجهزة المتصلة مباشرة من واجهات الراوتر (Tenda / TP-Link / Huawei / Netis / D-Link / ZTE / FiberHome)
     */
    suspend fun fetchClientsFromRouter(
        gatewayIp: String,
        user: String,
        pass: String,
    ): List<RouterClientItem> = withContext(Dispatchers.IO) {
        val found = mutableMapOf<String, RouterClientItem>()
        val authRaw = "$user:$pass"
        val basicAuth = Base64.encodeToString(authRaw.toByteArray(), Base64.NO_WRAP)
        val cookieAuth = "Authorization=Basic%20$basicAuth"

        // ١. محاولة تسجيل الدخول في راوترات Tenda أولاً إن وجدت
        runCatching {
            val passB64 = Base64.encodeToString(pass.toByteArray(), Base64.NO_WRAP)
            httpPost(
                urlStr = "http://$gatewayIp/login/Auth",
                body = "username=${URLEncoder.encode(user, "UTF-8")}&password=${URLEncoder.encode(passB64, "UTF-8")}",
                basicAuth = basicAuth,
                cookie = cookieAuth,
            )
        }

        // ٢. فحص أشهر روابط قائمة المتصلين في الراوترات المنزلية
        val endpoints = listOf(
            "http://$gatewayIp/goform/getOnlineList",
            "http://$gatewayIp/goform/getClientList",
            "http://$gatewayIp/cgi-bin-igd/netcore_get.cgi",
            "http://$gatewayIp/userRpm/AssignedIpAddrListRpm.htm",
            "http://$gatewayIp/userRpm/WlanStationRpm.htm",
            "http://$gatewayIp/api/wlan/host-list",
            "http://$gatewayIp/html/bbsp/common/GetLanUserDevInfo.asp",
            "http://$gatewayIp/status_deviceinfo.htm",
            "http://$gatewayIp/dhcptbl.asp",
            "http://$gatewayIp/wlstatbl.htm",
            "http://$gatewayIp/cgi-bin/luci",
            "http://$gatewayIp/",
        )

        for (ep in endpoints) {
            val body = httpGet(ep, basicAuth, cookieAuth) ?: continue
            parseClientsFromResponse(body, gatewayIp).forEach { item ->
                val existing = found[item.ip]
                if (existing == null || (existing.hostname.isBlank() && item.hostname.isNotBlank())) {
                    found[item.ip] = item
                }
            }
        }

        found.values.toList()
    }

    /**
     * تحليل أي صيغة يرد بها الراوتر (JSON أو مصفوفات TP-Link JS أو XML هواوي أو جداول HTML متعددة الأسطر)
     * لاستخراج (الاسم الحقيقي + الـ IP + الـ MAC الحقيقي)
     */
    fun parseClientsFromResponse(raw: String, gatewayIp: String): List<RouterClientItem> {
        val results = mutableListOf<RouterClientItem>()
        if (raw.isBlank()) return results
        val subnetPrefix = gatewayIp.substringBeforeLast('.', "192.168.1")

        // أ) صيغة JSON (مثل Tenda / Netis / الراوترات الحديثة)
        runCatching {
            val trimmed = raw.trim()
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                val arr = when {
                    trimmed.startsWith("[") -> JSONArray(trimmed)
                    else -> {
                        val obj = JSONObject(trimmed)
                        obj.optJSONArray("onlineList")
                            ?: obj.optJSONArray("clientList")
                            ?: obj.optJSONArray("host_list")
                            ?: obj.optJSONArray("list")
                            ?: obj.optJSONArray("data")
                    }
                }
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val ip = o.optString("ip", o.optString("ipaddr", o.optString("IPAddress", "")))
                        val mac = o.optString("mac", o.optString("macaddr", o.optString("MACAddress", "")))
                            .replace('-', ':').uppercase()
                        val name = o.optString(
                            "devName",
                            o.optString(
                                "hostname",
                                o.optString("hostName", o.optString("Name", o.optString("HostName", "")))
                            )
                        )
                        if (ip.isNotBlank() && ip != gatewayIp) {
                            val validMac = if (macRegex.matches(mac)) mac else ""
                            results.add(RouterClientItem(ip = ip, mac = validMac, hostname = name.trim()))
                        }
                    }
                }
            }
        }

        // ب) صيغة مصفوفة TP-Link DHCPClientList:
        // مثال: "Galaxy-A54", "50-01-D9-AA-BB-CC", "192.168.0.102", "01:22:10"
        runCatching {
            val tplinkPattern = Regex(
                """["']([^"']{1,40})["']\s*,\s*["'](([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2})["']\s*,\s*["'](192\.168\.\d{1,3}\.\d{1,3}|10\.\d{1,3}\.\d{1,3}\.\d{1,3})["']"""
            )
            tplinkPattern.findAll(raw).forEach { m ->
                val name = m.groupValues[1].trim()
                val mac = m.groupValues[2].replace('-', ':').uppercase()
                val ip = m.groupValues[4].trim()
                if (ip != gatewayIp) {
                    results.add(RouterClientItem(ip = ip, mac = mac, hostname = name))
                }
            }
        }

        // ج) استخراج من كتل HTML كاملة (<tr>...</tr> أو <li>...</li> أو <Host>...</Host>)
        // مهم جداً: لا نقسم بالأسطر الجديدة قبل فحص </tr> لأن خلايا الجدول <td> تكون في أسطر متتالية!
        val blocks = raw.split(Regex("</tr>|</li>|</Host>", RegexOption.IGNORE_CASE))
        for (block in blocks) {
            val ip = ipRegex.find(block)?.value ?: continue
            if (!ip.startsWith("$subnetPrefix.")) continue
            val lastOctet = ip.substringAfterLast('.').toIntOrNull() ?: continue
            if (ip == gatewayIp || lastOctet <= 1 || lastOctet >= 255) continue

            val mac = macRegex.find(block)?.value?.replace('-', ':')?.uppercase() ?: ""
            val cleanText = block
                .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("<[^>]+>"), " ")
                .replace(ip, " ")
                .replace(mac, " ")
                .replace(mac.replace(':', '-'), " ")
                .replace(Regex("&[a-zA-Z0-9#]+;"), " ")

            val nameCandidate = cleanText
                .split(Regex("[\\s,;|\"']+"))
                .firstOrNull { token ->
                    token.length in 3..28 &&
                        token.any { it.isLetter() } &&
                        !token.equals("dynamic", true) &&
                        !token.equals("static", true) &&
                        !token.equals("active", true) &&
                        !token.equals("online", true) &&
                        !token.equals("connected", true) &&
                        !token.equals("wlan0", true) &&
                        !token.equals("ssid", true) &&
                        !token.equals("dhcp", true) &&
                        !token.equals("lease", true) &&
                        !token.equals("client", true) &&
                        !token.equals("true", true) &&
                        !token.equals("false", true) &&
                        !token.startsWith("http", true)
                } ?: ""

            if (mac.isNotBlank() || nameCandidate.isNotBlank()) {
                results.add(RouterClientItem(ip = ip, mac = mac, hostname = nameCandidate))
            }
        }

        // د) فحص إضافي سطراً بسطر للنصوص العادية
        val lines = raw.split(Regex("\\r?\\n|<br\\s*/?>", RegexOption.IGNORE_CASE))
        for (line in lines) {
            val ip = ipRegex.find(line)?.value ?: continue
            if (!ip.startsWith("$subnetPrefix.")) continue
            val lastOctet = ip.substringAfterLast('.').toIntOrNull() ?: continue
            if (ip == gatewayIp || lastOctet <= 1 || lastOctet >= 255) continue
            val mac = macRegex.find(line)?.value?.replace('-', ':')?.uppercase() ?: continue
            results.add(RouterClientItem(ip = ip, mac = mac, hostname = ""))
        }

        return results.distinctBy { it.ip }
    }

    /**
     * تنفيذ الحظر الفعلي من الراوتر (باستخدام الـ MAC الحقيقي):
     * يرسل أوامر الحظر المباشرة لراوترات TP-Link و Tenda و Huawei و Netis و D-Link و ZTE
     */
    suspend fun executeRouterBlockOrUnblock(
        gatewayIp: String,
        user: String,
        pass: String,
        targetIp: String,
        targetMac: String,
        block: Boolean,
    ): Boolean = withContext(Dispatchers.IO) {
        val macColon = targetMac.replace('-', ':').uppercase()
        val macDash = targetMac.replace(':', '-').uppercase()
        val authRaw = "$user:$pass"
        val basicAuth = Base64.encodeToString(authRaw.toByteArray(), Base64.NO_WRAP)
        val cookieAuth = "Authorization=Basic%20$basicAuth"
        var anySuccess = false

        // ١. Tenda Router (Parental Control + MAC Filter Blacklist)
        runCatching {
            val passB64 = Base64.encodeToString(pass.toByteArray(), Base64.NO_WRAP)
            httpPost("http://$gatewayIp/login/Auth", "username=$user&password=$passB64", basicAuth, cookieAuth)
            val enableVal = if (block) "1" else "0"
            val r1 = httpPost(
                urlStr = "http://$gatewayIp/goform/saveParentControlInfo",
                body = "mac=${URLEncoder.encode(macColon, "UTF-8")}&enable=$enableVal&time=0:0-24:0&day=1,1,1,1,1,1,1",
                basicAuth = basicAuth,
                cookie = cookieAuth,
            )
            val r2 = httpPost(
                urlStr = "http://$gatewayIp/goform/addBlackRule",
                body = "mac=${URLEncoder.encode(macColon, "UTF-8")}&devName=BlockedByHaris",
                basicAuth = basicAuth,
                cookie = cookieAuth,
            )
            if (r1 != null || r2 != null) anySuccess = true
        }

        // ٢. TP-Link Router (تفعيل Wireless MAC Filter على وضع Deny وإضافة الماك الحقيقي)
        runCatching {
            val referer = "http://$gatewayIp/userRpm/WlanMacFilterRpm.htm"
            if (block) {
                httpGet("http://$gatewayIp/userRpm/WlanMacFilterRpm.htm?Rule=0&ChangeRule=Change", basicAuth, cookieAuth, referer)
                httpGet("http://$gatewayIp/userRpm/WlanMacFilterRpm.htm?EnFilter=1", basicAuth, cookieAuth, referer)
                val addResp = httpGet(
                    "http://$gatewayIp/userRpm/WlanMacFilterRpm.htm?Mac=$macDash&Desc=HarisCut&entryEnabled=1&Changed=1&SelIndex=0&Page=1&Save=Save",
                    basicAuth,
                    cookieAuth,
                    referer,
                )
                if (addResp != null) anySuccess = true
            } else {
                httpGet("http://$gatewayIp/userRpm/WlanMacFilterRpm.htm?DisFilter=1", basicAuth, cookieAuth, referer)
            }
        }

        // ٣. Netis Router (حظر فوري عبر CGI)
        runCatching {
            val enable = if (block) "1" else "0"
            val r = httpPost(
                urlStr = "http://$gatewayIp/cgi-bin-igd/netcore_set.cgi",
                body = "mode_name=netcore_set&mac_filter_enable=$enable&mac_addr=$macColon&ip_addr=$targetIp",
                basicAuth = basicAuth,
                cookie = cookieAuth,
            )
            if (r != null) anySuccess = true
        }

        // ٤. D-Link / Realtek / ZTE / Boa CGI (حظر MAC)
        runCatching {
            val r = httpPost(
                urlStr = "http://$gatewayIp/boaform/admin/formRteMacFilter",
                body = "mac=$macColon&action=${if (block) "add" else "delete"}&submit-url=%2Fmacfilter.asp",
                basicAuth = basicAuth,
                cookie = cookieAuth,
            )
            if (r != null) anySuccess = true
        }

        // ٥. Huawei HiLink Router (حظر الماك عبر XML API)
        runCatching {
            val xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <request>
                    <Ssids><Ssid><Index>0</Index><WifiMacFilterStatus>${if (block) "2" else "0"}</WifiMacFilterStatus><WifiMacFilterMac0>$macColon</WifiMacFilterMac0></Ssid></Ssids>
                </request>
            """.trimIndent()
            val r = httpPostXml("http://$gatewayIp/api/wlan/multi-macfilter-settings", xml, basicAuth, cookieAuth)
            if (r != null) anySuccess = true
        }

        anySuccess
    }

    private fun httpGet(
        urlStr: String,
        basicAuth: String,
        cookie: String,
        referer: String? = null,
    ): String? {
        return runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 900
                readTimeout = 900
                instanceFollowRedirects = true
                setRequestProperty("Authorization", "Basic $basicAuth")
                setRequestProperty("Cookie", cookie)
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                if (referer != null) setRequestProperty("Referer", referer)
            }
            val code = conn.responseCode
            if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        }.getOrNull()
    }

    private fun httpPost(
        urlStr: String,
        body: String,
        basicAuth: String,
        cookie: String,
    ): String? {
        return runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 900
                readTimeout = 900
                instanceFollowRedirects = true
                setRequestProperty("Authorization", "Basic $basicAuth")
                setRequestProperty("Cookie", cookie)
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                setRequestProperty("Referer", urlStr)
            }
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            if (code in 200..399) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        }.getOrNull()
    }

    private fun httpPostXml(
        urlStr: String,
        xmlBody: String,
        basicAuth: String,
        cookie: String,
    ): String? {
        return runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 900
                readTimeout = 900
                setRequestProperty("Authorization", "Basic $basicAuth")
                setRequestProperty("Cookie", cookie)
                setRequestProperty("Content-Type", "application/xml; charset=UTF-8")
            }
            conn.outputStream.use { it.write(xmlBody.toByteArray()) }
            val code = conn.responseCode
            if (code in 200..399) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        }.getOrNull()
    }
}
