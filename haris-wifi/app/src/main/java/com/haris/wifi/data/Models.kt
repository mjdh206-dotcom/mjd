package com.haris.wifi.data

enum class DeviceType(
    val id: String,
    val labelAr: String,
    val emoji: String,
) {
    ROUTER("router", "الراوتر الأساسي", "📡"),
    THIS_PHONE("self", "جهازك الحالي", "🛡️"),
    CAMERA("camera", "كاميرا مراقبة", "📷"),
    PHONE("phone", "هاتف ذكي", "📱"),
    LAPTOP("laptop", "كمبيوتر / لابتوب", "💻"),
    TV("tv", "تلفزيون ذكي", "📺"),
    CONSOLE("console", "جهاز ألعاب", "🎮"),
    UNKNOWN("unknown", "جهاز غير معروف", "❓");

    companion object {
        fun fromId(id: String?): DeviceType =
            entries.firstOrNull { it.id == id } ?: UNKNOWN
    }
}

enum class DisconnectMode(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val durationMinutes: Int?,
) {
    INSTANT_OPEN(
        id = "instant",
        titleAr = "قطع الاتصال بقوة قصوى (هجوم مزدوج + ARP/RTSP)",
        subtitleAr = "إغراق منافذ الجهاز/الكاميرا بـ 16 خط متوازٍ + قطع الجلسات فوراً",
        durationMinutes = null,
    ),
    PAUSE_15M(
        id = "pause_15m",
        titleAr = "فصل مؤقت (١٥ دقيقة)",
        subtitleAr = "إيقاف الإنترنت عن الجهاز لمدة ربع ساعة ثم إعادته تلقائياً",
        durationMinutes = 15,
    ),
    PAUSE_1H(
        id = "pause_1h",
        titleAr = "فصل مؤقت (ساعة واحدة)",
        subtitleAr = "إيقاف الإنترنت لمدة ٦٠ دقيقة (مثالي لوقت الدراسة أو النوم)",
        durationMinutes = 60,
    ),
    ROUTER_PERMANENT(
        id = "router_block",
        titleAr = "حظر فعلي نهائي من الراوتر (مضمون ١٠٠٪ بدون روت)",
        subtitleAr = "فتح الراوتر وتعبئة الدخول وحظر عنوان MAC الخاص بالجهاز تلقائياً",
        durationMinutes = null,
    ),
}

enum class FilterTab(val labelAr: String) {
    ALL("الكل"),
    CAMERAS("📷 الكاميرات"),
    CONNECTED("متصل الآن"),
    BLOCKED("مفصول ⛔"),
    STRANGERS("غرباء ⚠️"),
}

data class NetworkInfoModel(
    val ssid: String = "شبكة الواي فاي المنزلية",
    val bssid: String = "AA:BB:CC:11:22:33",
    val gatewayIp: String = "192.168.1.1",
    val localIp: String = "192.168.1.105",
    val subnetPrefix: String = "192.168.1",
    val linkSpeedMbps: Int = 300,
    val rssiDbm: Int = -52,
    val signalPercent: Int = 92,
    val routerBrandAr: String = "راوتر منزلي",
    val isRealWifi: Boolean = true,
    val isRootAvailable: Boolean = false,
)

data class ConnectedDevice(
    val ip: String,
    val mac: String,
    val hostname: String = "",
    val vendorAr: String = "جهاز شبكة",
    val defaultNameAr: String = "جهاز متصل",
    val customName: String = "",
    val deviceType: DeviceType = DeviceType.PHONE,
    val isOnline: Boolean = true,
    val isBlocked: Boolean = false,
    val blockedUntilMillis: Long? = null,
    val blockMethodAr: String = "",
    val pingMs: Int = 12,
    val openPorts: List<Int> = emptyList(),
    val isTrusted: Boolean = false,
    val isGateway: Boolean = false,
    val isSelf: Boolean = false,
    val signalQuality: Int = 85,
    val cameraProtocol: String = "",
) {
    val displayName: String
        get() = customName.trim().ifEmpty { defaultNameAr }

    val uniqueKey: String
        get() = if (mac.isNotBlank() && mac != "02:00:00:00:00:00") mac.uppercase() else ip

    val canDisconnect: Boolean
        get() = !isGateway && !isSelf

    val isCamera: Boolean
        get() = deviceType == DeviceType.CAMERA || cameraProtocol.isNotBlank()

    val isStranger: Boolean
        get() = !isGateway && !isSelf && !isTrusted
}

data class SavedDeviceMeta(
    val key: String,
    val customName: String = "",
    val deviceTypeId: String = "",
    val isTrusted: Boolean = false,
    val isBlocked: Boolean = false,
    val blockedUntilMillis: Long? = null,
    val blockMethodAr: String = "",
    val lastIp: String = "",
    val lastMac: String = "",
    val vendorAr: String = "",
    val defaultNameAr: String = "",
)

data class RouterGuide(
    val id: String,
    val nameAr: String,
    val commonIps: List<String>,
    val defaultUser: String,
    val defaultPass: String,
    val menuPathAr: String,
    val quickStepsAr: List<String>,
)
