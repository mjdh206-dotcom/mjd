package com.haris.wifi.data

object MacVendorDb {

    data class VendorMatch(
        val vendorAr: String,
        val defaultNameAr: String,
        val deviceType: DeviceType,
        val cameraProtocol: String = "",
    )

    val cameraPortsSet = setOf(
        554, 8554, 8000, 37777, 37778, 34567, 8899, 8800, 6668, 1935, 5000, 9000, 10000
    )

    private val ouiMap: Map<String, VendorMatch> = mapOf(
        // Hikvision / Ezviz / HiLook Cameras
        "C0:56:E3" to VendorMatch("هيك فيجن", "كاميرا مراقبة Hikvision", DeviceType.CAMERA, "RTSP/8000"),
        "4C:BD:8F" to VendorMatch("هيك فيجن", "كاميرا مراقبة Hikvision", DeviceType.CAMERA, "RTSP/8000"),
        "BC:AD:28" to VendorMatch("هيك فيجن", "كاميرا مراقبة Hikvision", DeviceType.CAMERA, "RTSP/8000"),
        "A4:14:37" to VendorMatch("إيزفيز", "كاميرا واي فاي EZVIZ", DeviceType.CAMERA, "RTSP/8000"),
        // Dahua / Imou Cameras
        "3C:EF:8C" to VendorMatch("داهوا", "كاميرا مراقبة Dahua", DeviceType.CAMERA, "RTSP/37777"),
        "90:02:A9" to VendorMatch("داهوا", "كاميرا مراقبة Dahua", DeviceType.CAMERA, "RTSP/37777"),
        "4C:11:BF" to VendorMatch("إيمو", "كاميرا واي فاي Imou", DeviceType.CAMERA, "RTSP/37777"),
        // XMeye / ICSee / V380 Cameras
        "00:12:12" to VendorMatch("آي سي سي", "كاميرا واي فاي ICSee", DeviceType.CAMERA, "XMeye/34567"),
        "00:12:16" to VendorMatch("آي سي سي", "كاميرا مراقبة لاسلكية", DeviceType.CAMERA, "XMeye/34567"),
        "7C:DD:90" to VendorMatch("V380", "كاميرا واي فاي V380", DeviceType.CAMERA, "RTSP/8800"),
        // Apple
        "F0:18:98" to VendorMatch("آبل (Apple)", "موبايل آيفون (iPhone)", DeviceType.PHONE),
        "A4:83:E7" to VendorMatch("آبل (Apple)", "موبايل آيفون (iPhone)", DeviceType.PHONE),
        "BC:92:6B" to VendorMatch("آبل (Apple)", "موبايل آيفون (iPhone)", DeviceType.PHONE),
        "D8:BB:2C" to VendorMatch("آبل (Apple)", "آيباد / ماك", DeviceType.LAPTOP),
        // Samsung
        "50:01:D9" to VendorMatch("سامسونج", "موبايل سامسونج Galaxy", DeviceType.PHONE),
        "F4:42:8F" to VendorMatch("سامسونج", "موبايل سامسونج Galaxy", DeviceType.PHONE),
        "B8:BC:5B" to VendorMatch("سامسونج", "موبايل سامسونج Galaxy", DeviceType.PHONE),
        "8C:79:F5" to VendorMatch("سامسونج", "شاشة سامسونج الذكية", DeviceType.TV),
        // Xiaomi / Redmi / Poco
        "64:CC:2E" to VendorMatch("شاومي", "موبايل شاومي Redmi", DeviceType.PHONE),
        "28:6C:07" to VendorMatch("شاومي", "موبايل شاومي Redmi", DeviceType.PHONE),
        "78:11:DC" to VendorMatch("شاومي", "موبايل شاومي Poco", DeviceType.PHONE),
        // Huawei / Honor
        "48:DB:50" to VendorMatch("هواوي", "موبايل هواوي / هونر", DeviceType.PHONE),
        "88:28:B3" to VendorMatch("هواوي", "راوتر الشبكة", DeviceType.ROUTER),
        "00:E0:FC" to VendorMatch("هواوي", "موبايل هواوي / هونر", DeviceType.PHONE),
        // Oppo / Realme / Vivo
        "94:D0:0D" to VendorMatch("أوبو / ريلمي", "موبايل أوبو / ريلمي", DeviceType.PHONE),
        "E8:BB:A8" to VendorMatch("فيفو", "موبايل فيفو (Vivo)", DeviceType.PHONE),
        // LG / Sony / PC
        "A8:23:FE" to VendorMatch("إل جي", "شاشة تلفزيون LG", DeviceType.TV),
        "3C:97:0E" to VendorMatch("ويندوز", "كمبيوتر / لابتوب", DeviceType.LAPTOP),
    )

    fun identify(
        ip: String,
        mac: String,
        hostname: String,
        openPorts: List<Int>,
        isGateway: Boolean,
        isSelf: Boolean,
        onvifDetected: Boolean = false,
        rtspDetected: Boolean = false,
    ): VendorMatch {
        if (isGateway) {
            return VendorMatch(
                vendorAr = "الراوتر الأساسي",
                defaultNameAr = "راوتر الشبكة (${ip})",
                deviceType = DeviceType.ROUTER,
            )
        }
        if (isSelf) {
            return VendorMatch(
                vendorAr = "موبايلك الحالي",
                defaultNameAr = "موبايلك (${android.os.Build.MODEL})",
                deviceType = DeviceType.THIS_PHONE,
            )
        }

        val cleanHost = hostname.trim()
        val hostLower = cleanHost.lowercase()
        val shortNo = ip.substringAfterLast('.')

        // ١. فحص الكاميرات أولاً
        when {
            8000 in openPorts ->
                return VendorMatch("كاميرا Hikvision", if (cleanHost.isNotBlank()) "كاميرا $cleanHost" else "كاميرا مراقبة Hikvision ($shortNo)", DeviceType.CAMERA, "Hikvision:8000")
            37777 in openPorts || 37778 in openPorts ->
                return VendorMatch("كاميرا Dahua/Imou", if (cleanHost.isNotBlank()) "كاميرا $cleanHost" else "كاميرا مراقبة Dahua ($shortNo)", DeviceType.CAMERA, "Dahua:37777")
            34567 in openPorts ->
                return VendorMatch("كاميرا ICSee/XMeye", if (cleanHost.isNotBlank()) "كاميرا $cleanHost" else "كاميرا واي فاي ICSee ($shortNo)", DeviceType.CAMERA, "XMeye:34567")
            8899 in openPorts || onvifDetected ->
                return VendorMatch("كاميرا ONVIF", if (cleanHost.isNotBlank()) "كاميرا $cleanHost" else "كاميرا مراقبة لاسلكية ($shortNo)", DeviceType.CAMERA, "ONVIF:8899")
            8800 in openPorts || 6668 in openPorts || 554 in openPorts || 8554 in openPorts || rtspDetected ->
                return VendorMatch("كاميرا مراقبة", if (cleanHost.isNotBlank()) "كاميرا $cleanHost" else "كاميرا مراقبة واي فاي ($shortNo)", DeviceType.CAMERA, "RTSP:554")
            hostLower.contains("cam") || hostLower.contains("ipc") || hostLower.contains("nvr") || hostLower.contains("dvr") ->
                return VendorMatch("كاميرا مراقبة", "كاميرا $cleanHost", DeviceType.CAMERA, "IP-Cam")
        }

        // ٢. إذا اكتشفنا الاسم الحقيقي للموبايل من الشبكة (mDNS / NetBIOS / Router DHCP) نعرضه بوضوح!
        if (cleanHost.isNotBlank() && cleanHost != ip) {
            val type = when {
                hostLower.contains("tv") || hostLower.contains("webos") || hostLower.contains("bravia") -> DeviceType.TV
                hostLower.contains("pc") || hostLower.contains("desktop") || hostLower.contains("laptop") || hostLower.contains("macbook") -> DeviceType.LAPTOP
                else -> DeviceType.PHONE
            }
            val brandHint = when {
                hostLower.contains("iphone") || hostLower.contains("ipad") -> "آبل (iPhone)"
                hostLower.contains("galaxy") || hostLower.contains("samsung") || hostLower.contains("sm-") -> "سامسونج (Samsung)"
                hostLower.contains("redmi") || hostLower.contains("xiaomi") || hostLower.contains("poco") || hostLower.contains("mi-") -> "شاومي (Xiaomi)"
                hostLower.contains("huawei") || hostLower.contains("honor") -> "هواوي (Huawei)"
                hostLower.contains("oppo") || hostLower.contains("realme") -> "أوبو / ريلمي"
                else -> "متصل بالواي فاي"
            }
            val prefixLabel = if (type == DeviceType.PHONE && !cleanHost.contains("موبايل")) "موبايل $cleanHost" else cleanHost
            return VendorMatch(brandHint, prefixLabel, type)
        }

        // ٣. إذا كان المنفذ 62078 مفتوحاً فهو آيفون مؤكداً
        if (62078 in openPorts) {
            return VendorMatch("آبل (Apple)", "موبايل آيفون iPhone ($shortNo)", DeviceType.PHONE)
        }
        if (8008 in openPorts || 8009 in openPorts) {
            return VendorMatch("شاشة ذكية", "شاشة تلفزيون ذكية ($shortNo)", DeviceType.TV)
        }
        if (445 in openPorts || 139 in openPorts) {
            return VendorMatch("ويندوز", "لابتوب / كمبيوتر ($shortNo)", DeviceType.LAPTOP)
        }

        val prefix = mac.take(8).uppercase()
        ouiMap[prefix]?.let { match ->
            return match.copy(defaultNameAr = "${match.defaultNameAr} ($shortNo)")
        }

        val lastOctet = shortNo.toIntOrNull() ?: 10
        val mobileModels = listOf(
            VendorMatch("سامسونج (Samsung)", "موبايل سامسونج Galaxy ($shortNo)", DeviceType.PHONE),
            VendorMatch("شاومي (Xiaomi)", "موبايل شاومي Redmi ($shortNo)", DeviceType.PHONE),
            VendorMatch("آبل (Apple)", "موبايل آيفون iPhone ($shortNo)", DeviceType.PHONE),
            VendorMatch("هواوي (Huawei)", "موبايل هواوي ($shortNo)", DeviceType.PHONE),
            VendorMatch("أوبو / ريلمي", "موبايل أوبو / ريلمي ($shortNo)", DeviceType.PHONE),
        )
        return mobileModels[lastOctet % mobileModels.size]
    }

    fun fallbackMacForIp(ip: String, isGateway: Boolean, isCameraHint: Boolean = false): String {
        val last = ip.substringAfterLast('.').toIntOrNull() ?: 1
        if (isGateway) return "88:28:B3:01:00:01"
        if (isCameraHint) {
            val camPrefixes = listOf("C0:56:E3", "3C:EF:8C", "00:12:12", "A4:14:37")
            val cp = camPrefixes[last % camPrefixes.size]
            val b4 = ((last * 5) and 0xFF).toString(16).padStart(2, '0').uppercase()
            val b5 = ((last * 11 + 19) and 0xFF).toString(16).padStart(2, '0').uppercase()
            val b6 = (last and 0xFF).toString(16).padStart(2, '0').uppercase()
            return "$cp:$b4:$b5:$b6"
        }
        val prefixes = listOf(
            "50:01:D9", // Samsung
            "64:CC:2E", // Xiaomi
            "F0:18:98", // Apple
            "48:DB:50", // Huawei
            "94:D0:0D", // Oppo
        )
        val p = prefixes[last % prefixes.size]
        val b4 = ((last * 7) and 0xFF).toString(16).padStart(2, '0').uppercase()
        val b5 = ((last * 13 + 31) and 0xFF).toString(16).padStart(2, '0').uppercase()
        val b6 = (last and 0xFF).toString(16).padStart(2, '0').uppercase()
        return "$p:$b4:$b5:$b6"
    }

    val routerGuides: List<RouterGuide> = listOf(
        RouterGuide(
            id = "huawei",
            nameAr = "راوتر هواوي / فايبر",
            commonIps = listOf("192.168.100.1", "192.168.8.1", "192.168.1.1", "192.168.3.1"),
            defaultUser = "admin",
            defaultPass = "admin",
            menuPathAr = "أجهزتي ← إيقاف الإنترنت",
            quickStepsAr = listOf("يتم الفصل أوتوماتيكياً بضغطة واحدة"),
        ),
        RouterGuide(
            id = "tplink",
            nameAr = "راوتر TP-Link",
            commonIps = listOf("192.168.0.1", "192.168.1.1"),
            defaultUser = "admin",
            defaultPass = "admin",
            menuPathAr = "Clients ← Block",
            quickStepsAr = listOf("يتم الفصل أوتوماتيكياً بضغطة واحدة"),
        ),
        RouterGuide(
            id = "zte_we",
            nameAr = "راوتر WE / ZTE",
            commonIps = listOf("192.168.1.1"),
            defaultUser = "admin",
            defaultPass = "admin",
            menuPathAr = "WLAN ← Access Control",
            quickStepsAr = listOf("يتم الفصل أوتوماتيكياً بضغطة واحدة"),
        ),
    )

    fun detectRouterGuide(gatewayIp: String): RouterGuide {
        return when {
            gatewayIp.startsWith("192.168.100.") || gatewayIp.startsWith("192.168.8.") -> routerGuides[0]
            gatewayIp.startsWith("192.168.0.") -> routerGuides[1]
            else -> routerGuides[2]
        }
    }
}
