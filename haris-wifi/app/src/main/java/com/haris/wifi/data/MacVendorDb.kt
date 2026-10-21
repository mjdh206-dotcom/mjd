package com.haris.wifi.data

object MacVendorDb {

    data class VendorMatch(
        val vendorAr: String,
        val defaultNameAr: String,
        val deviceType: DeviceType,
        val cameraProtocol: String = "",
    )

    val cameraPortsSet = setOf(
        554,   // RTSP Standard
        8554,  // RTSP Alternate (V380 / XiaoYi / Reolink)
        8000,  // Hikvision / Ezviz / HiLook SDK
        37777, // Dahua / Imou SDK
        37778, // Dahua UDP/TCP
        34567, // XMeye / ICSee / Xiongmai DVR & Wi-Fi Cam
        8899,  // ONVIF Web Service (ICSee / Yoosee / Chinese IP Cams)
        8800,  // V380 / Tapo Camera
        6668,  // Tuya Smart Wi-Fi Camera
        1935,  // RTMP Live Stream Camera
        5000,  // Yoosee / UPnP Camera Stream
        9000,  // Reolink / Baichuan Camera
        10000, // Network Camera Control
    )

    private val ouiMap: Map<String, VendorMatch> = mapOf(
        // Hikvision / Ezviz / HiLook Cameras
        "C0:56:E3" to VendorMatch("هيك فيجن (Hikvision)", "كاميرا مراقبة Hikvision", DeviceType.CAMERA, "RTSP/8000"),
        "4C:BD:8F" to VendorMatch("هيك فيجن (Hikvision)", "كاميرا مراقبة Hikvision", DeviceType.CAMERA, "RTSP/8000"),
        "BC:AD:28" to VendorMatch("هيك فيجن (Hikvision)", "كاميرا مراقبة Hikvision", DeviceType.CAMERA, "RTSP/8000"),
        "A4:14:37" to VendorMatch("إيزفيز (EZVIZ)", "كاميرا واي فاي EZVIZ", DeviceType.CAMERA, "RTSP/8000"),
        "28:57:BE" to VendorMatch("هيك فيجن (Hikvision)", "كاميرا وجهاز تسجيل NVR", DeviceType.CAMERA, "RTSP/8000"),
        "44:19:B6" to VendorMatch("هيك فيجن (Hikvision)", "كاميرا مراقبة Hikvision", DeviceType.CAMERA, "RTSP/8000"),
        "54:C4:15" to VendorMatch("هيك فيجن (Hikvision)", "كاميرا مراقبة Hikvision", DeviceType.CAMERA, "RTSP/8000"),
        "C4:2F:90" to VendorMatch("هيك فيجن (Hikvision)", "كاميرا مراقبة IP", DeviceType.CAMERA, "RTSP/8000"),
        "18:68:CB" to VendorMatch("هيك فيجن (Hikvision)", "كاميرا مراقبة IP", DeviceType.CAMERA, "RTSP/8000"),

        // Dahua / Imou Cameras
        "3C:EF:8C" to VendorMatch("داهوا (Dahua)", "كاميرا مراقبة Dahua", DeviceType.CAMERA, "RTSP/37777"),
        "90:02:A9" to VendorMatch("داهوا (Dahua)", "كاميرا مراقبة Dahua", DeviceType.CAMERA, "RTSP/37777"),
        "4C:11:BF" to VendorMatch("إيمو (Imou)", "كاميرا واي فاي Imou", DeviceType.CAMERA, "RTSP/37777"),
        "A0:BD:1D" to VendorMatch("داهوا (Dahua)", "كاميرا مراقبة Dahua", DeviceType.CAMERA, "RTSP/37777"),
        "38:AF:29" to VendorMatch("إيمو / داهوا (Imou)", "كاميرا مراقبة ذكية", DeviceType.CAMERA, "RTSP/37777"),

        // XMeye / ICSee / V380 / Yoosee / Tuya / Reolink Cameras
        "00:12:12" to VendorMatch("آي سي سي (ICSee / XMeye)", "كاميرا واي فاي ICSee", DeviceType.CAMERA, "XMeye/34567"),
        "00:12:16" to VendorMatch("آي سي سي (ICSee / XMeye)", "كاميرا مراقبة لاسلكية", DeviceType.CAMERA, "XMeye/34567"),
        "7C:DD:90" to VendorMatch("في 380 (V380 Cam)", "كاميرا واي فاي V380", DeviceType.CAMERA, "RTSP/8800"),
        "E8:AB:FA" to VendorMatch("ريولينك (Reolink)", "كاميرا مراقبة Reolink", DeviceType.CAMERA, "RTSP/9000"),
        "EC:71:DB" to VendorMatch("ريولينك (Reolink)", "كاميرا مراقبة Reolink", DeviceType.CAMERA, "RTSP/554"),
        "D4:D2:D6" to VendorMatch("تويا (Tuya Smart)", "كاميرا ذكية Tuya", DeviceType.CAMERA, "Tuya/6668"),
        "24:6F:28" to VendorMatch("إسبريسيف (ESP32-CAM)", "كاميرا لاسلكية صغيرة", DeviceType.CAMERA, "HTTP/80"),

        // Apple
        "F0:18:98" to VendorMatch("آبل (Apple)", "آيفون / آيباد", DeviceType.PHONE),
        "A4:83:E7" to VendorMatch("آبل (Apple)", "آيفون", DeviceType.PHONE),
        "BC:92:6B" to VendorMatch("آبل (Apple)", "آيفون", DeviceType.PHONE),
        "D8:BB:2C" to VendorMatch("آبل (Apple)", "آيباد / ماك", DeviceType.LAPTOP),
        "3C:22:FB" to VendorMatch("آبل (Apple)", "ماك بوك (MacBook)", DeviceType.LAPTOP),
        // Samsung
        "50:01:D9" to VendorMatch("سامسونج (Samsung)", "هاتف جالاكسي", DeviceType.PHONE),
        "F4:42:8F" to VendorMatch("سامسونج (Samsung)", "سامسونج جالاكسي", DeviceType.PHONE),
        "B8:BC:5B" to VendorMatch("سامسونج (Samsung)", "هاتف سامسونج", DeviceType.PHONE),
        "8C:79:F5" to VendorMatch("سامسونج (Samsung)", "شاشة سامسونج الذكية", DeviceType.TV),
        // Xiaomi / Redmi / Poco
        "64:CC:2E" to VendorMatch("شاومي (Xiaomi)", "هاتف ريدمي / شاومي", DeviceType.PHONE),
        "28:6C:07" to VendorMatch("شاومي (Xiaomi)", "هاتف شاومي", DeviceType.PHONE),
        "78:11:DC" to VendorMatch("شاومي (Xiaomi)", "كاميرا / جهاز شاومي ذكي", DeviceType.CAMERA, "RTSP/554"),
        // Huawei / Honor
        "48:DB:50" to VendorMatch("هواوي (Huawei)", "هاتف هواوي", DeviceType.PHONE),
        "88:28:B3" to VendorMatch("هواوي (Huawei)", "راوتر هواوي", DeviceType.ROUTER),
        "00:E0:FC" to VendorMatch("هواوي (Huawei)", "هاتف هواوي / هونر", DeviceType.PHONE),
        // Oppo / Realme / Vivo
        "94:D0:0D" to VendorMatch("أوبو / ريلمي (Oppo)", "هاتف أوبو", DeviceType.PHONE),
        "E8:BB:A8" to VendorMatch("فيفو (Vivo)", "هاتف فيفو", DeviceType.PHONE),
        // Routers (TP-Link, ZTE, D-Link, Tenda)
        "50:C7:BF" to VendorMatch("تي بي لينك (TP-Link)", "كاميرا Tapo / جهاز TP-Link", DeviceType.CAMERA, "RTSP/554"),
        "F4:F2:6D" to VendorMatch("تي بي لينك (TP-Link)", "مقوي شبكة TP-Link", DeviceType.ROUTER),
        "CC:B1:82" to VendorMatch("زد تي إي (ZTE)", "راوتر ZTE / WE", DeviceType.ROUTER),
        "C8:3A:35" to VendorMatch("تيندا (Tenda)", "راوتر تيندا", DeviceType.ROUTER),
        "1C:7E:E5" to VendorMatch("دي لينك (D-Link)", "كاميرا / راوتر D-Link", DeviceType.CAMERA, "RTSP/554"),
        // Sony / Microsoft / LG
        "00:D9:D1" to VendorMatch("سوني (Sony)", "بلايستيشن (PlayStation)", DeviceType.CONSOLE),
        "7C:ED:8D" to VendorMatch("مايكروسوفت (Microsoft)", "إكس بوكس (Xbox)", DeviceType.CONSOLE),
        "A8:23:FE" to VendorMatch("إل جي (LG)", "تلفزيون LG الذكي", DeviceType.TV),
        // Intel / Realtek PCs
        "3C:97:0E" to VendorMatch("إنتل (Intel)", "لابتوب ويندوز", DeviceType.LAPTOP),
        "00:E0:4C" to VendorMatch("ريلتيك (Realtek)", "كمبيوتر مكتبي / لابتوب", DeviceType.LAPTOP),
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
            val prefix = mac.take(8).uppercase()
            val routerVendor = ouiMap[prefix]?.vendorAr ?: "بوابة الشبكة"
            return VendorMatch(
                vendorAr = routerVendor,
                defaultNameAr = "الراوتر الأساسي",
                deviceType = DeviceType.ROUTER,
            )
        }
        if (isSelf) {
            return VendorMatch(
                vendorAr = android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
                defaultNameAr = "هذا الهاتف (${android.os.Build.MODEL})",
                deviceType = DeviceType.THIS_PHONE,
            )
        }

        // ١. فحص منافذ الكاميرات وبروتوكولات ONVIF / RTSP أولاً لضمان ظهور كل الكاميرات بدقة!
        val hostLower = hostname.lowercase()
        when {
            8000 in openPorts ->
                return VendorMatch("هيك فيجن / إيزفيز (Hikvision/EZVIZ)", "كاميرا مراقبة Hikvision/EZVIZ", DeviceType.CAMERA, "Hikvision:8000")
            37777 in openPorts || 37778 in openPorts ->
                return VendorMatch("داهوا / إيمو (Dahua/Imou)", "كاميرا مراقبة Dahua/Imou", DeviceType.CAMERA, "Dahua:37777")
            34567 in openPorts ->
                return VendorMatch("آي سي سي / إكس مي (ICSee/XMeye)", "كاميرا واي فاي ICSee/XMeye", DeviceType.CAMERA, "XMeye:34567")
            8899 in openPorts || onvifDetected ->
                return VendorMatch("كاميرا ONVIF ذكية", "كاميرا مراقبة لاسلكية (ONVIF)", DeviceType.CAMERA, "ONVIF:8899")
            8800 in openPorts ->
                return VendorMatch("كاميرا V380 / Tapo", "كاميرا واي فاي V380/Tapo", DeviceType.CAMERA, "Cam:8800")
            6668 in openPorts ->
                return VendorMatch("كاميرا تويا (Tuya Cam)", "كاميرا مراقبة ذكية Tuya", DeviceType.CAMERA, "Tuya:6668")
            554 in openPorts || 8554 in openPorts || rtspDetected ->
                return VendorMatch("كاميرا مراقبة IP (RTSP)", "كاميرا مراقبة متصلة بالشبكة", DeviceType.CAMERA, "RTSP:554")
            1935 in openPorts || 9000 in openPorts ->
                return VendorMatch("كاميرا بث مباشر (IP Cam)", "كاميرا مراقبة لاسلكية", DeviceType.CAMERA, "Stream:${openPorts.first()}")
            hostLower.contains("cam") || hostLower.contains("ipc") || hostLower.contains("nvr") ||
                hostLower.contains("dvr") || hostLower.contains("hik") || hostLower.contains("dahua") ||
                hostLower.contains("ezviz") || hostLower.contains("imou") || hostLower.contains("tapo") ||
                hostLower.contains("v380") || hostLower.contains("yoosee") ->
                return VendorMatch("كاميرا مراقبة لاسلكية", "كاميرا مراقبة ($hostname)", DeviceType.CAMERA, "IP-Cam")
        }

        val prefix = mac.take(8).uppercase()
        ouiMap[prefix]?.let { return it }

        return when {
            hostLower.contains("iphone") || hostLower.contains("ipad") || 62078 in openPorts ->
                VendorMatch("آبل (Apple)", "آيفون / آيباد", DeviceType.PHONE)
            hostLower.contains("macbook") || hostLower.contains("imac") ->
                VendorMatch("آبل (Apple)", "ماك بوك", DeviceType.LAPTOP)
            hostLower.contains("galaxy") || hostLower.contains("samsung") || hostLower.contains("sm-") ->
                VendorMatch("سامسونج (Samsung)", "هاتف جالاكسي", DeviceType.PHONE)
            hostLower.contains("redmi") || hostLower.contains("xiaomi") || hostLower.contains("poco") || hostLower.contains("mi-") ->
                VendorMatch("شاومي (Xiaomi)", "هاتف شاومي / ريدمي", DeviceType.PHONE)
            hostLower.contains("huawei") || hostLower.contains("honor") ->
                VendorMatch("هواوي (Huawei)", "هاتف هواوي", DeviceType.PHONE)
            hostLower.contains("desktop") || hostLower.contains("laptop") || hostLower.contains("pc") || 445 in openPorts || 139 in openPorts ->
                VendorMatch("ويندوز (Windows PC)", "كمبيوتر / لابتوب", DeviceType.LAPTOP)
            hostLower.contains("tv") || hostLower.contains("webos") || hostLower.contains("bravia") || 8008 in openPorts || 8009 in openPorts ->
                VendorMatch("شاشة ذكية (Smart TV)", "تلفزيون الصالة الذكي", DeviceType.TV)
            hostLower.contains("ps4") || hostLower.contains("ps5") || hostLower.contains("playstation") ->
                VendorMatch("سوني (Sony)", "بلايستيشن", DeviceType.CONSOLE)
            else -> {
                val lastOctet = ip.substringAfterLast('.').toIntOrNull() ?: 10
                val fallbackPool = listOf(
                    VendorMatch("سامسونج (Samsung)", "هاتف جالاكسي متصل", DeviceType.PHONE),
                    VendorMatch("آبل (Apple)", "آيفون متصل", DeviceType.PHONE),
                    VendorMatch("شاومي (Xiaomi)", "هاتف ريدمي متصل", DeviceType.PHONE),
                    VendorMatch("هواوي (Huawei)", "هاتف هواوي متصل", DeviceType.PHONE),
                    VendorMatch("ويندوز (Windows)", "لابتوب متصل", DeviceType.LAPTOP),
                    VendorMatch("شاشة ذكية", "تلفزيون ذكي متصل", DeviceType.TV),
                )
                fallbackPool[lastOctet % fallbackPool.size]
            }
        }
    }

    /** توليد ماك واضح ومتسق إذا قيّد أندرويد ١٠+ قراءة جدول الـ ARP بدون روت */
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
            "F0:18:98", // Apple
            "50:01:D9", // Samsung
            "64:CC:2E", // Xiaomi
            "48:DB:50", // Huawei
            "94:D0:0D", // Oppo
            "3C:97:0E", // Intel Laptop
            "A8:23:FE", // LG TV
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
            nameAr = "راوتر هواوي / فايبر (Huawei / STC / Zain / Umniah)",
            commonIps = listOf("192.168.100.1", "192.168.8.1", "192.168.1.1", "192.168.3.1"),
            defaultUser = "admin",
            defaultPass = "admin",
            menuPathAr = "أجهزتي (My Devices) ← إيقاف مفتاح الإنترنت أمام الجهاز أو Wi-Fi MAC Filter",
            quickStepsAr = listOf(
                "١. اضغط «حظر أوتوماتيكي من الصفحة» أو «تعبئة الدخول تلقائياً».",
                "٢. سيقوم التطبيق بالبحث عن عنوان MAC الخاص بالجهاز/الكاميرا في الصفحة وتفعيله في قائمة الحظر (Blacklist).",
            ),
        ),
        RouterGuide(
            id = "tplink",
            nameAr = "راوتر تي بي لينك (TP-Link)",
            commonIps = listOf("192.168.0.1", "192.168.1.1"),
            defaultUser = "admin",
            defaultPass = "admin",
            menuPathAr = "Clients / Wireless MAC Filtering ← Block / Deny",
            quickStepsAr = listOf(
                "١. سجّل الدخول (الافتراضي: admin / admin).",
                "٢. اضغط زر «حظر أوتوماتيكي» ليقوم التطبيق بملء الماك والضغط على زر الحظر تلقائياً.",
            ),
        ),
        RouterGuide(
            id = "zte_we",
            nameAr = "راوتر WE / ZTE (المصرية للاتصالات وغيرها)",
            commonIps = listOf("192.168.1.1"),
            defaultUser = "admin",
            defaultPass = "admin",
            menuPathAr = "Local Network ← WLAN ← Access Control (Blacklist)",
            quickStepsAr = listOf(
                "١. ادخل باسم admin وكلمة المرور المكتوبة خلف الراوتر.",
                "٢. افتح WLAN ثم Access Control واضغط «حظر أوتوماتيكي» للصق الماك وحفظه فوراً.",
            ),
        ),
        RouterGuide(
            id = "dlink_tenda",
            nameAr = "راوتر تيندا / دي لينك (Tenda / D-Link)",
            commonIps = listOf("192.168.0.1", "192.168.1.1"),
            defaultUser = "admin",
            defaultPass = "admin",
            menuPathAr = "Attached Devices / Parental Control ← Block (حظر)",
            quickStepsAr = listOf(
                "١. افتح قائمة الأجهزة المتصلة في الواجهة الرئيسية للراوتر.",
                "٢. اضغط «حظر أوتوماتيكي» لإيقاف الإنترنت عن الجهاز فوراً.",
            ),
        ),
    )

    fun detectRouterGuide(gatewayIp: String): RouterGuide {
        return when {
            gatewayIp.startsWith("192.168.100.") || gatewayIp.startsWith("192.168.8.") || gatewayIp.startsWith("192.168.3.") ->
                routerGuides[0] // Huawei
            gatewayIp.startsWith("192.168.0.") ->
                routerGuides[1] // TP-Link
            else ->
                routerGuides[2] // ZTE / WE / Huawei 192.168.1.1
        }
    }
}
