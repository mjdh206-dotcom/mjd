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

    var scanStatusText by mutableStateOf("جاهز لفحص المتصلين")
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

    var routerPassword by mutableStateOf(store.routerPassword)
        private set

    /** الجهاز الذي يتم حظره صامتاً في الخلفية عبر محرك الراوتر المخفي */
    var silentRouterCutCommand by mutableStateOf<Pair<ConnectedDevice, Long>?>(null)
        private set

    var liveCutTick by mutableLongStateOf(0L)
        private set

    init {
        startScan(deepCameraScan = false)
        startExpirationAndLiveTicker()
    }

    fun updateRouterPassword(newPass: String) {
        routerPassword = newPass.trim().ifEmpty { "admin" }
        store.routerPassword = routerPassword
        showBanner("تم حفظ كلمة سر الراوتر للفصل الأوتوماتيكي")
    }

    fun getCutPacketsFor(device: ConnectedDevice): Long {
        @Suppress("UNUSED_EXPRESSION")
        liveCutTick
        return cutter.getCutPacketsCount(device.uniqueKey)
    }

    /** بدء فحص الشبكة واكتشاف جميع الموبايلات والكاميرات */
    fun startScan(deepCameraScan: Boolean = false) {
        if (isScanning) return
        viewModelScope.launch {
            isScanning = true
            scanProgress = 0.04f
            scanStatusText = "جاري البحث عن الموبايلات والكاميرات المتصلة..."
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

            found.forEach { dev ->
                if (dev.isBlocked) {
                    cutter.startCut(
                        device = dev,
                        gatewayIp = info.gatewayIp,
                        localIp = info.localIp,
                        routerPassword = routerPassword,
                        isRootAvailable = info.isRootAvailable,
                    )
                }
            }

            devices = scanner.sortDevices(found)
            isScanning = false
            scanProgress = 1f

            val controllableCount = devices.count { it.canDisconnect }
            val blockedCount = devices.count { it.isBlocked }
            scanStatusText = if (blockedCount > 0) {
                "الأجهزة المكتشفة: $controllableCount · تم فصل النت عن: $blockedCount"
            } else {
                "تم العثور على $controllableCount أجهزة متصلة بالشبكة"
            }
        }
    }

    fun startDeepCameraScan() {
        startScan(deepCameraScan = true)
    }

    /**
     * كبسة واحدة فقط لفصل أو إعادة الإنترنت بدون روت:
     * تشغّل إغراق الجلسات + UPnP + الحظر الصامت من الراوتر في الخلفية معاً!
     */
    fun toggleInstantCut(device: ConnectedDevice) {
        if (!device.canDisconnect) {
            showBanner("هذا جهاز الراوتر أو موبايلك الشخصي")
            return
        }
        hapticTap()
        if (device.isBlocked) {
            reconnectDevice(device)
        } else {
            cutDevice(device, DisconnectMode.INSTANT_OPEN)
        }
    }

    fun cutDevice(device: ConnectedDevice, mode: DisconnectMode) {
        if (!device.canDisconnect) return
        hapticTap()

        val untilMillis = mode.durationMinutes?.let {
            System.currentTimeMillis() + it * 60_000L
        }

        val updated = device.copy(
            isBlocked = true,
            blockedUntilMillis = untilMillis,
            blockMethodAr = "تم قطع الإنترنت عنه ⛔",
        )

        updateDeviceInList(updated)
        persistDeviceState(updated)

        // ١. تشغيل محرك القطع الرباعي (16 خط إغراق + UPnP + Router API + ARP)
        cutter.startCut(
            device = updated,
            gatewayIp = networkInfo.gatewayIp,
            localIp = networkInfo.localIp,
            routerPassword = routerPassword,
            isRootAvailable = networkInfo.isRootAvailable,
        )

        // ٢. إرسال أمر فوري لمحرك الراوتر الصامت في الخلفية لحظر الماك والـ IP بدون فتح أي شاشة!
        silentRouterCutCommand = updated to System.currentTimeMillis()

        selectedDeviceForSheet = null
        showBanner("تم فصل النت عن «${updated.displayName}» ✂️")
    }

    fun reconnectDevice(device: ConnectedDevice) {
        hapticTap()
        val updated = device.copy(
            isBlocked = false,
            blockedUntilMillis = null,
            blockMethodAr = "",
        )
        updateDeviceInList(updated)
        persistDeviceState(updated)
        cutter.stopCut(
            device = updated,
            gatewayIp = networkInfo.gatewayIp,
            routerPassword = routerPassword,
            isRootAvailable = networkInfo.isRootAvailable,
        )
        selectedDeviceForSheet = null
        showBanner("تمت إعادة النت لـ «${updated.displayName}» ✓")
    }

    fun cutAllCameras() {
        val activeCameras = devices.filter { it.isCamera && !it.isBlocked && it.canDisconnect }
        if (activeCameras.isEmpty()) return
        hapticTap()
        val updatedList = devices.map { dev ->
            if (dev.isCamera && !dev.isBlocked && dev.canDisconnect) {
                val cut = dev.copy(isBlocked = true, blockMethodAr = "تم قطع الإنترنت عنه ⛔")
                persistDeviceState(cut)
                cutter.startCut(cut, networkInfo.gatewayIp, networkInfo.localIp, routerPassword, networkInfo.isRootAvailable)
                silentRouterCutCommand = cut to System.currentTimeMillis()
                cut
            } else dev
        }
        devices = scanner.sortDevices(updatedList)
        showBanner("تم فصل النت عن جميع الكاميرات (${activeCameras.size}) ✂️")
    }

    /** فصل النت عن الجميع بكبسة واحدة */
    fun cutAllStrangers() {
        val targets = devices.filter { it.canDisconnect && !it.isBlocked }
        if (targets.isEmpty()) {
            showBanner("جميع الأجهزة مفصولة بالفعل")
            return
        }
        hapticTap()
        val updatedList = devices.map { dev ->
            if (dev.canDisconnect && !dev.isBlocked) {
                val cut = dev.copy(
                    isBlocked = true,
                    blockedUntilMillis = null,
                    blockMethodAr = "تم قطع الإنترنت عنه ⛔",
                )
                persistDeviceState(cut)
                cutter.startCut(
                    device = cut,
                    gatewayIp = networkInfo.gatewayIp,
                    localIp = networkInfo.localIp,
                    routerPassword = routerPassword,
                    isRootAvailable = networkInfo.isRootAvailable,
                )
                silentRouterCutCommand = cut to System.currentTimeMillis()
                cut
            } else {
                dev
            }
        }
        devices = scanner.sortDevices(updatedList)
        showBanner("تم فصل النت عن جميع المتصلين (${targets.size}) بكبسة واحدة ✂️")
    }

    fun reconnectAllDevices() {
        val blocked = devices.filter { it.isBlocked }
        if (blocked.isEmpty()) return
        hapticTap()
        cutter.stopAll(
            gatewayIp = networkInfo.gatewayIp,
            routerPassword = routerPassword,
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
        showBanner("تمت إعادة النت لجميع الأجهزة (${blocked.size}) ✓")
    }

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
        showBanner("تم تغيير الاسم إلى «${updated.displayName}»")
    }

    fun openRouterBlocker(device: ConnectedDevice?) {
        hapticTap()
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
        return devices
    }

    fun showBanner(msg: String) {
        bannerMessage = msg
        viewModelScope.launch {
            delay(3000L)
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
