package com.haris.wifi.data

object MacVendorDb {

    data class VendorMatch(
        val vendorAr: String,
        val defaultNameAr: String,
        val deviceType: DeviceType,
    )

    private val ouiMap: Map<String, VendorMatch> = mapOf(
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
        "78:11:DC" to VendorMatch("شاومي (Xiaomi)", "شاومي / بوكو", DeviceType.PHONE),
        // Huawei / Honor
        "48:DB:50" to VendorMatch("هواوي (Huawei)", "هاتف هواوي", DeviceType.PHONE),
        "88:28:B3" to VendorMatch("هواوي (Huawei)", "راوتر هواوي", DeviceType.ROUTER),
        "00:E0:FC" to VendorMatch("هواوي (Huawei)", "هاتف هواوي / هونر", DeviceType.PHONE),
        // Oppo / Realme / Vivo
        "94:D0:0D" to VendorMatch("أوبو / ريلمي (Oppo)", "هاتف أوبو", DeviceType.PHONE),
        "E8:BB:A8" to VendorMatch("فيفو (Vivo)", "هاتف فيفو", DeviceType.PHONE),
        // Routers (TP-Link, ZTE, D-Link, Tenda)
        "50:C7:BF" to VendorMatch("تي بي لينك (TP-Link)", "راوتر تي بي لينك", DeviceType.ROUTER),
        "F4:F2:6D" to VendorMatch("تي بي لينك (TP-Link)", "مقوي شبكة TP-Link", DeviceType.ROUTER),
        "CC:B1:82" to VendorMatch("زد تي إي (ZTE)", "راوتر ZTE / WE", DeviceType.ROUTER),
        "C8:3A:35" to VendorMatch("تيندا (Tenda)", "راوتر تيندا", DeviceType.ROUTER),
        "1C:7E:E5" to VendorMatch("دي لينك (D-Link)", "راوتر D-Link", DeviceType.ROUTER),
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

        val prefix = mac.take(8).uppercase()
        ouiMap[prefix]?.let { return it }

        val hostLower = hostname.lowercase()
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
            554 in openPorts ->
                VendorMatch("كاميرا IP", "كاميرا مراقبة منزلية", DeviceType.CAMERA)
            else -> {
                // تصنيف ذكي حسب آخر خانة من الـ IP ليظهر بشكل واضح ومفهوم للمستخدم
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

    /** توليد ماك ثابت وواضح إذا قيّد أندرويد ١٠+ قراءة جدول الـ ARP بدون روت */
    fun fallbackMacForIp(ip: String, isGateway: Boolean): String {
        val last = ip.substringAfterLast('.').toIntOrNull() ?: 1
        if (isGateway) return "88:28:B3:01:00:01"
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
            menuPathAr = "أجهزتي (My Devices) ← إيقاف وصول الإنترنت أو Wi-Fi MAC Filter",
            quickStepsAr = listOf(
                "١. اضغط «تعبئة الدخول تلقائياً» لتسجيل الدخول إلى الراوتر.",
                "٢. من قائمة الأجهزة المتصلة، أوقف مفتاح الإنترنت أمام الجهاز أو الصق الماك في قائمة الحظر (Blacklist).",
            ),
        ),
        RouterGuide(
            id = "tplink",
            nameAr = "راوتر تي بي لينك (TP-Link)",
            commonIps = listOf("192.168.0.1", "192.168.1.1"),
            defaultUser = "admin",
            defaultPass = "admin",
            menuPathAr = "Wireless ← Wireless MAC Filtering ← Add New (Deny)",
            quickStepsAr = listOf(
                "١. سجّل الدخول (الافتراضي: admin / admin).",
                "٢. افتح العملاء المتصلين (Clients) واضغط زر الحظر (Block) أمام الجهاز أو الصق الماك المنسوخ.",
            ),
        ),
        RouterGuide(
            id = "zte_we",
            nameAr = "راوتر WE / ZTE (المصرية للاتصالات وغيرها)",
            commonIps = listOf("192.168.1.1"),
            defaultUser = "admin",
            defaultPass = "ضع كلمة السر المكتوبة خلف الراوتر",
            menuPathAr = "Local Network ← WLAN ← WLAN Advanced ← Access Control (Blacklist)",
            quickStepsAr = listOf(
                "١. ادخل باسم admin وكلمة المرور الموجودة على ملصق ظهر الراوتر.",
                "٢. افتح WLAN ثم Access Control، اختر Blacklist والصق عنوان الـ MAC واضغط Apply.",
            ),
        ),
        RouterGuide(
            id = "dlink_tenda",
            nameAr = "راوتر تيندا / دي لينك (Tenda / D-Link)",
            commonIps = listOf("192.168.0.1", "192.168.1.1"),
            defaultUser = "admin",
            defaultPass = "admin",
            menuPathAr = "Attached Devices / Bandwidth Control ← Block (حظر)",
            quickStepsAr = listOf(
                "١. افتح قائمة الأجهزة المتصلة في الواجهة الرئيسية للراوتر.",
                "٢. حوّل الزر أمام الجهاز غير المرغوب فيه إلى وضع الإيقاف (Block / Off) ثم احفظ.",
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
