package com.haris.wifi

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haris.wifi.data.ConnectedDevice
import com.haris.wifi.data.DeviceStore
import com.haris.wifi.data.DeviceType
import com.haris.wifi.data.DisconnectMode
import com.haris.wifi.data.FilterTab
import com.haris.wifi.data.MacVendorDb
import com.haris.wifi.data.NetworkInfoModel
import com.haris.wifi.data.RouterGuide
import com.haris.wifi.data.SavedDeviceMeta
import com.haris.wifi.net.ConnectionCutter
import com.haris.wifi.net.LanScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HarisViewModel(app: Application) : AndroidViewModel(app) {

    private val store = DeviceStore(app)
    private val scanner = LanScanner(app)
    private val cutter = ConnectionCutter(viewModelScope)

    var networkInfo by mutableStateOf(NetworkInfoModel())
        private set

    var devices by mutableStateOf<List<ConnectedDevice>>(emptyList())
        private set

    var isScanning by mutableStateOf(false)
        private set

    var scanProgress by mutableFloatStateOf(0f)
        private set

    var scanStatusText by mutableStateOf("جاهز لفحص الشبكة والكاميرات")
        private set

    var selectedTab by mutableStateOf(FilterTab.ALL)
        private set

    var searchQuery by mutableStateOf("")
        private set

    var selectedDeviceForSheet by mutableStateOf<ConnectedDevice?>(null)
        private set

    var routerBlockerDevice by mutableStateOf<ConnectedDevice?>(null)
        private set

    var showRouterScreen by mutableStateOf(false)
        private set

    var bannerMessage by mutableStateOf<String?>(null)
        private set

    var vibrateEnabled by mutableStateOf(store.vibrateEnabled)
        private set

    var autoCutStrangers by mutableStateOf(store.autoCutStrangers)
        private set

    var liveCutTick by mutableLongStateOf(0L)
        private set

    init {
        startScan(deepCameraScan = false)
        startExpirationAndLiveTicker()
    }

    fun getCutPacketsFor(device: ConnectedDevice): Long {
        // قراءة liveCutTick لتحديث الواجهة لحظياً أثناء الهجوم
        @Suppress("UNUSED_EXPRESSION")
        liveCutTick
        return cutter.getCutPacketsCount(device.uniqueKey)
    }

    /** بدء فحص الشبكة واكتشاف جميع المتصلين والكاميرات */
    fun startScan(deepCameraScan: Boolean = false) {
        if (isScanning) return
        viewModelScope.launch {
            isScanning = true
            scanProgress = 0.02f
            scanStatusText = if (deepCameraScan) {
                "بدء الفحص العميق لكاميرات المراقبة (ONVIF / RTSP / Hikvision / Dahua)..."
            } else {
                "جاري قراءة بيانات الراوتر والواي فاي وكشف الكاميرات..."
            }
            hapticTap()

            val hasRoot = cutter.checkRootAvailable()
            val info = scanner.readNetworkInfo(isRootAvailable = hasRoot)
            networkInfo = info

            val savedMeta = store.loadAllMeta()
            val found = scanner.scanSubnet(
                netInfo = info,
                savedMeta = savedMeta,
                deepCameraScan = deepCameraScan,
                onProgress = { progress, text ->
                    scanProgress = progress
                    scanStatusText = text
                }
            )

            val processed = if (autoCutStrangers) {
                found.map { dev ->
                    if (dev.isStranger && !dev.isBlocked) {
                        val cutDev = dev.copy(
                            isBlocked = true,
                            blockMethodAr = "فصل تلقائي للغرباء",
                        )
                        persistDeviceState(cutDev)
                        cutter.startCut(cutDev, info.gatewayIp, info.isRootAvailable)
                        cutDev
                    } else {
                        if (dev.isBlocked) {
                            cutter.startCut(dev, info.gatewayIp, info.isRootAvailable)
                        }
                        cutDevOrOriginal(dev)
                    }
                }
            } else {
                found.onEach { dev ->
                    if (dev.isBlocked) {
                        cutter.startCut(dev, info.gatewayIp, info.isRootAvailable)
                    }
                }
            }

            devices = scanner.sortDevices(processed)
            isScanning = false
            scanProgress = 1f

            val connectedCount = devices.count { !it.isBlocked }
            val blockedCount = devices.count { it.isBlocked }
            val camerasCount = devices.count { it.isCamera }

            if (deepCameraScan && camerasCount > 0) {
                selectedTab = FilterTab.CAMERAS
                showBanner("تم اكتشاف $camerasCount كاميرات مراقبة على الشبكة 📷")
            }

            scanStatusText = "متصل: $connectedCount · كاميرات: $camerasCount · مفصول: $blockedCount"
        }
    }

    private fun cutDevOrOriginal(dev: ConnectedDevice): ConnectedDevice = dev

    /** فحص مخصص للكاميرات بضغطة واحدة */
    fun startDeepCameraScan() {
        startScan(deepCameraScan = true)
    }

    /** زر الفصل السريع بضغطة واحدة من البطاقة مباشرة */
    fun toggleInstantCut(device: ConnectedDevice) {
        if (!device.canDisconnect) {
            showBanner("لا يمكن فصل الراوتر الأساسي أو هاتفك الحالي")
            return
        }
        hapticTap()
        if (device.isBlocked) {
            reconnectDevice(device)
        } else {
            cutDevice(device, DisconnectMode.INSTANT_OPEN)
        }
    }

    /** فصل الجهاز بالوضع المحدد */
    fun cutDevice(device: ConnectedDevice, mode: DisconnectMode) {
        if (!device.canDisconnect) return
        hapticTap()

        val untilMillis = mode.durationMinutes?.let {
            System.currentTimeMillis() + it * 60_000L
        }
        val methodLabel = when (mode) {
            DisconnectMode.INSTANT_OPEN -> when {
                networkInfo.isRootAvailable -> "قطع كامل بالروت (ARP + iptables)"
                device.isCamera -> "إغراق بث الكاميرا نشط (12 خط RTSP/ONVIF)"
                else -> "هجوم قطع الجلسات نشط (8 خطوط TCP/UDP)"
            }
            DisconnectMode.PAUSE_15M -> "مفصول مؤقتاً (١٥ دقيقة)"
            DisconnectMode.PAUSE_1H -> "مفصول مؤقتاً (ساعة واحدة)"
            DisconnectMode.ROUTER_PERMANENT -> "قطع مزدوج + حظر من الراوتر"
        }

        val updated = device.copy(
            isBlocked = true,
            blockedUntilMillis = untilMillis,
            blockMethodAr = methodLabel,
        )

        updateDeviceInList(updated)
        persistDeviceState(updated)
        cutter.startCut(updated, networkInfo.gatewayIp, networkInfo.isRootAvailable)

        if (mode == DisconnectMode.ROUTER_PERMANENT) {
            copyToClipboard("MAC", updated.mac)
            selectedDeviceForSheet = null
            routerBlockerDevice = updated
            showRouterScreen = true
            showBanner("تم نسخ الماك (${updated.mac}) وفتح الحظر الأوتوماتيكي للراوتر")
        } else {
            selectedDeviceForSheet = null
            val extraHint = if (!networkInfo.isRootAvailable && !device.isCamera) {
                " · للحظر النهائي ١٠٠٪ اضغط «حظر راوتر»"
            } else ""
            showBanner("بدأ قطع الاتصال عن «${updated.displayName}» ✂️$extraHint")
        }
    }

    /** إعادة الاتصال لجهاز مفصول */
    fun reconnectDevice(device: ConnectedDevice) {
        hapticTap()
        val updated = device.copy(
            isBlocked = false,
            blockedUntilMillis = null,
            blockMethodAr = "",
        )
        updateDeviceInList(updated)
        persistDeviceState(updated)
        cutter.stopCut(updated, networkInfo.gatewayIp, networkInfo.isRootAvailable)
        selectedDeviceForSheet = null
        showBanner("تمت إعادة الاتصال لـ «${updated.displayName}» ✓")
    }

    /** فصل كل الكاميرات المتصلة بضغطة واحدة */
    fun cutAllCameras() {
        val activeCameras = devices.filter { it.isCamera && !it.isBlocked && it.canDisconnect }
        if (activeCameras.isEmpty()) {
            showBanner("لا توجد كاميرات متصلة غير مفصولة حالياً")
            return
        }
        hapticTap()
        val updatedList = devices.map { dev ->
            if (dev.isCamera && !dev.isBlocked && dev.canDisconnect) {
                val cut = dev.copy(
                    isBlocked = true,
                    blockedUntilMillis = null,
                    blockMethodAr = "إغراق وإيقاف بث الكاميرا (12 خط RTSP/ONVIF) ⛔",
                )
                persistDeviceState(cut)
                cutter.startCut(cut, networkInfo.gatewayIp, networkInfo.isRootAvailable)
                cut
            } else {
                dev
            }
        }
        devices = scanner.sortDevices(updatedList)
        showBanner("تم قطع البث والاتصال عن ${activeCameras.size} كاميرات مراقبة 📷✂️")
    }

    /** فصل كل الأجهزة الغريبة (غير الموثوقة) بضغطة واحدة */
    fun cutAllStrangers() {
        val strangers = devices.filter { it.isStranger && !it.isBlocked }
        if (strangers.isEmpty()) {
            showBanner("لا توجد أجهزة غريبة متصلة حالياً")
            return
        }
        hapticTap()
        val updatedList = devices.map { dev ->
            if (dev.isStranger && !dev.isBlocked) {
                val cut = dev.copy(
                    isBlocked = true,
                    blockedUntilMillis = null,
                    blockMethodAr = "فصل جماعي للغرباء ⛔",
                )
                persistDeviceState(cut)
                cutter.startCut(cut, networkInfo.gatewayIp, networkInfo.isRootAvailable)
                cut
            } else {
                dev
            }
        }
        devices = scanner.sortDevices(updatedList)
        showBanner("تم فصل الاتصال عن ${strangers.size} أجهزة غريبة بضغطة واحدة ✂️")
    }

    /** إعادة الاتصال لجميع الأجهزة المفصولة */
    fun reconnectAllDevices() {
        val blocked = devices.filter { it.isBlocked }
        if (blocked.isEmpty()) return
        hapticTap()
        cutter.stopAll(
            gatewayIp = networkInfo.gatewayIp,
            isRootAvailable = networkInfo.isRootAvailable,
            devices = blocked,
        )
        val updatedList = devices.map { dev ->
            if (dev.isBlocked) {
                val restored = dev.copy(
                    isBlocked = false,
                    blockedUntilMillis = null,
                    blockMethodAr = "",
                )
                persistDeviceState(restored)
                restored
            } else {
                dev
            }
        }
        devices = scanner.sortDevices(updatedList)
        showBanner("تمت إعادة الاتصال لجميع الأجهزة (${blocked.size}) ✓")
    }

    /** تسمية الجهاز وتحديد نوعه وثقته */
    fun saveDeviceDetails(
        device: ConnectedDevice,
        customName: String,
        deviceType: DeviceType,
        isTrusted: Boolean,
    ) {
        hapticTap()
        val updated = device.copy(
            customName = customName.trim(),
            deviceType = deviceType,
            isTrusted = if (device.isGateway || device.isSelf) true else isTrusted,
        )
        updateDeviceInList(updated)
        persistDeviceState(updated)
        selectedDeviceForSheet = null
        showBanner("تم حفظ بيانات «${updated.displayName}»")
    }

    fun openRouterBlocker(device: ConnectedDevice?) {
        hapticTap()
        if (device != null) {
            copyToClipboard("MAC", device.mac)
            if (!device.isBlocked && device.canDisconnect) {
                cutDevice(device, DisconnectMode.ROUTER_PERMANENT)
                return
            }
        }
        selectedDeviceForSheet = null
        routerBlockerDevice = device
        showRouterScreen = true
    }

    fun closeRouterBlocker() {
        showRouterScreen = false
    }

    fun openDeviceSheet(device: ConnectedDevice) {
        hapticTap()
        selectedDeviceForSheet = device
    }

    fun closeDeviceSheet() {
        selectedDeviceForSheet = null
    }

    fun selectTab(tab: FilterTab) {
        selectedTab = tab
    }

    fun updateSearch(query: String) {
        searchQuery = query
    }

    fun toggleVibrate(enabled: Boolean) {
        vibrateEnabled = enabled
        store.vibrateEnabled = enabled
    }

    fun toggleAutoCutStrangers(enabled: Boolean) {
        autoCutStrangers = enabled
        store.autoCutStrangers = enabled
        if (enabled) {
            cutAllStrangers()
        }
    }

    fun copyToClipboard(label: String, text: String) {
        runCatching {
            val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText(label, text))
        }
        showBanner("تم نسخ $label: $text")
    }

    fun currentRouterGuide(): RouterGuide =
        MacVendorDb.detectRouterGuide(networkInfo.gatewayIp)

    fun filteredDevices(): List<ConnectedDevice> {
        val byTab = when (selectedTab) {
            FilterTab.ALL -> devices
            FilterTab.CAMERAS -> devices.filter { it.isCamera }
            FilterTab.CONNECTED -> devices.filter { !it.isBlocked }
            FilterTab.BLOCKED -> devices.filter { it.isBlocked }
            FilterTab.STRANGERS -> devices.filter { it.isStranger }
        }
        val q = searchQuery.trim()
        if (q.isEmpty()) return byTab
        return byTab.filter {
            it.displayName.contains(q, ignoreCase = true) ||
                it.vendorAr.contains(q, ignoreCase = true) ||
                it.ip.contains(q, ignoreCase = true) ||
                it.mac.contains(q, ignoreCase = true)
        }
    }

    fun showBanner(msg: String) {
        bannerMessage = msg
        viewModelScope.launch {
            delay(3600L)
            if (bannerMessage == msg) {
                bannerMessage = null
            }
        }
    }

    fun dismissBanner() {
        bannerMessage = null
    }

    private fun updateDeviceInList(updated: ConnectedDevice) {
        val next = devices.map {
            if (it.uniqueKey == updated.uniqueKey || it.ip == updated.ip) updated else it
        }
        devices = scanner.sortDevices(next)
    }

    private fun persistDeviceState(device: ConnectedDevice) {
        store.saveMeta(
            SavedDeviceMeta(
                key = device.uniqueKey,
                customName = device.customName,
                deviceTypeId = device.deviceType.id,
                isTrusted = device.isTrusted,
                isBlocked = device.isBlocked,
                blockedUntilMillis = device.blockedUntilMillis,
                blockMethodAr = device.blockMethodAr,
                lastIp = device.ip,
                lastMac = device.mac,
                vendorAr = device.vendorAr,
                defaultNameAr = device.defaultNameAr,
            )
        )
    }

    private fun startExpirationAndLiveTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (devices.any { it.isBlocked }) {
                    liveCutTick++
                }
                val now = System.currentTimeMillis()
                val expired = devices.filter { dev ->
                    val until = dev.blockedUntilMillis
                    dev.isBlocked && until != null && now >= until
                }
                if (expired.isNotEmpty()) {
                    expired.forEach { reconnectDevice(it) }
                }
            }
        }
    }

    private fun hapticTap() {
        if (!vibrateEnabled) return
        runCatching {
            val app = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(18L)
                }
            }
        }
    }
}
