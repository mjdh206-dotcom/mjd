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
     * سحب الأجهزة المتصلة مباشرة من واجهات الراوتر (Tenda / TP-Link / Huawei / Netis / D-Link / ZTE)
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
     * تحليل أي صيغة يرد بها الراوتر (JSON أو مصفوفات TP-Link JS أو XML هواوي أو جداول HTML)
     * لاستخراج (الاسم الحقيقي + الـ IP + الـ MAC الحقيقي)
     */
    fun parseClientsFromResponse(raw: String, gatewayIp: String): List<RouterClientItem> {
        val results = mutableListOf<RouterClientItem>()
        if (raw.isBlank()) return results

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
                        if (ip.isNotBlank() && macRegex.matches(mac) && ip != gatewayIp) {
                            results.add(RouterClientItem(ip = ip, mac = mac, hostname = name.trim()))
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

        // ج) صيغة XML هواوي (<Host><IpAddress>...</IpAddress><MacAddress>...</MacAddress><HostName>...</HostName></Host>)
        runCatching {
            if (raw.contains("<HostName", ignoreCase = true) || raw.contains("<MacAddress", ignoreCase = true)) {
                val hostBlocks = raw.split(Regex("</Host>|</tr>", RegexOption.IGNORE_CASE))
                for (block in hostBlocks) {
                    val ip = ipRegex.find(block)?.value ?: continue
                    val mac = macRegex.find(block)?.value?.replace('-', ':')?.uppercase() ?: continue
                    val nameMatch = Regex("<(?:HostName|DeviceName)>(.*?)</(?:HostName|DeviceName)>", RegexOption.IGNORE_CASE).find(block)
                    val name = nameMatch?.groupValues?.getOrNull(1)?.trim() ?: ""
                    if (ip != gatewayIp) {
                        results.add(RouterClientItem(ip = ip, mac = mac, hostname = name))
                    }
                }
            }
        }

        // د) استخراج عام من أسطر الجدول (HTML <tr> أو نصوص مفصولة) تحتوي على IP و MAC في نفس السطر
        val lines = raw.split(Regex("</tr>|\\r?\\n|<br\\s*/?>", RegexOption.IGNORE_CASE))
        for (line in lines) {
            val ip = ipRegex.find(line)?.value ?: continue
            if (ip == gatewayIp || ip.endsWith(".255") || ip.endsWith(".0")) continue
            val mac = macRegex.find(line)?.value?.replace('-', ':')?.uppercase() ?: continue
            // استخراج أي اسم جهاز واضح في نفس السطر
            val cleanText = line
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
                        !token.equals("true", true) &&
                        !token.equals("false", true) &&
                        !token.startsWith("http", true)
                } ?: ""
            results.add(RouterClientItem(ip = ip, mac = mac, hostname = nameCandidate))
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
                // تفعيل الفلتر أولاً + ضبط القاعدة على Deny (Rule=0) + إضافة الماك
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
            val netisResp = httpPost(
                urlStr = "http://$gatewayIp/cgi-bin-igd/netcore_set.cgi",
                body = "mode_name=netcore_set&mac_filter_enable=$enable&mac_filter_rule=0&mac_addr=$macColon",
                basicAuth = basicAuth,
                cookie = cookieAuth,
            )
            if (netisResp != null) anySuccess = true
        }

        // ٤. D-Link / Realtek / ZTE / Boa Router (حظر الماك في WLAN Access Control)
        runCatching {
            val rBoa = httpPost(
                urlStr = "http://$gatewayIp/boafrm/formWlAc",
                body = if (block) {
                    "wlanAcEnabled=1&mac=$macColon&comment=Haris&addFilterMac=Add+%26+Apply"
                } else {
                    "wlanAcEnabled=0&apply=Apply+Changes"
                },
                basicAuth = basicAuth,
                cookie = cookieAuth,
            )
            val rDlink = httpPost(
                urlStr = "http://$gatewayIp/form2MacTbl.cgi",
                body = "mac=$macColon&enable=${if (block) 1 else 0}&submit.htm?macfilter.htm=Send",
                basicAuth = basicAuth,
                cookie = cookieAuth,
            )
            if (rBoa != null || rDlink != null) anySuccess = true
        }

        // ٥. Huawei HiLink Router API
        runCatching {
            val tokXml = httpGet("http://$gatewayIp/api/webserver/SesTokInfo", basicAuth, cookieAuth) ?: ""
            val sesInfo = Regex("<SesInfo>(.*?)</SesInfo>").find(tokXml)?.groupValues?.getOrNull(1) ?: ""
            val tokInfo = Regex("<TokInfo>(.*?)</TokInfo>").find(tokXml)?.groupValues?.getOrNull(1) ?: ""
            if (tokInfo.isNotBlank()) {
                val filterStatus = if (block) "2" else "0" // 2 = Deny / Blacklist
                val xmlPayload = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <request>
                        <WifiMacFilterStatus>$filterStatus</WifiMacFilterStatus>
                        <WifiMacFilterMac0>$macColon</WifiMacFilterMac0>
                    </request>
                """.trimIndent()
                val conn = (URL("http://$gatewayIp/api/wlan/mac-filter").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 900
                    readTimeout = 900
                    doOutput = true
                    if (sesInfo.isNotBlank()) setRequestProperty("Cookie", sesInfo)
                    setRequestProperty("__RequestVerificationToken", tokInfo)
                    setRequestProperty("Content-Type", "application/xml")
                }
                conn.outputStream.use { it.write(xmlPayload.toByteArray()) }
                if (conn.responseCode in 200..299) anySuccess = true
                conn.disconnect()
            }
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
                connectTimeout = 800
                readTimeout = 800
                instanceFollowRedirects = true
                setRequestProperty("Authorization", "Basic $basicAuth")
                setRequestProperty("Cookie", cookie)
                if (referer != null) setRequestProperty("Referer", referer)
            }
            val code = conn.responseCode
            val text = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
            conn.disconnect()
            text
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
                connectTimeout = 800
                readTimeout = 800
                doOutput = true
                setRequestProperty("Authorization", "Basic $basicAuth")
                setRequestProperty("Cookie", cookie)
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            val text = if (code in 200..399) {
                runCatching { conn.inputStream.bufferedReader().use { it.readText() } }.getOrDefault("")
            } else null
            conn.disconnect()
            text
        }.getOrNull()
    }
}
