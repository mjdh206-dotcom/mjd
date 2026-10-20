package com.shuaa.remote

import android.app.Application
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shuaa.remote.data.Brand
import com.shuaa.remote.data.Buttons
import com.shuaa.remote.data.CodeLibrary
import com.shuaa.remote.data.CodeSet
import com.shuaa.remote.data.DeviceStore
import com.shuaa.remote.data.IrSignal
import com.shuaa.remote.data.SavedDevice
import com.shuaa.remote.data.SettingsStore
import com.shuaa.remote.ir.IrEncoder
import com.shuaa.remote.ir.IrTransmitter
import com.shuaa.remote.ui.Feedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ShuaaViewModel(application: Application) : AndroidViewModel(application) {

    val settings = SettingsStore(application)
    val feedback = Feedback(application, settings)

    private val deviceStore = DeviceStore(application)
    private val transmitter = IrTransmitter(application)

    var brands by mutableStateOf<List<Brand>>(emptyList())
        private set

    var devices by mutableStateOf<List<SavedDevice>>(emptyList())
        private set

    /** رسالة قصيرة تظهر أسفل الشاشة */
    var banner by mutableStateOf<String?>(null)
        private set

    /** الزر الذي يُرسل الآن (لتوهّج الواجهة) */
    var activeKey by mutableStateOf<String?>(null)
        private set

    val irAvailable: Boolean get() = transmitter.hasEmitter
    val irDescription: String get() = transmitter.describe()

    private var holdJob: Job? = null
    private var bannerJob: Job? = null

    init {
        refreshDevices()
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = CodeLibrary.popularBrands(application)
            withContext(Dispatchers.Main) { brands = loaded }
        }
    }

    fun refreshDevices() {
        devices = deviceStore.devices()
    }

    /* ------------------------- الإرسال ------------------------- */

    /**
     * إرسال زر. يرجع true إذا تم الإرسال فعلياً (أو تمت محاكاته في وضع المعاينة).
     */
    fun send(device: SavedDevice, key: String, view: View? = null): Boolean {
        val signal = device.codes[key]
        if (signal == null) {
            showBanner("هذا الزر غير متوفر في أكواد «${device.brandAr}»")
            return false
        }
        feedback.tap(view)
        flash(key)
        return transmit(signal, key)
    }

    /** إرسال إشارة مباشرة (يُستخدم في المطابقة التلقائية) */
    fun sendSignal(signal: IrSignal?, key: String = Buttons.POWER): Boolean {
        if (signal == null) {
            showBanner("لا يوجد كود طاقة في هذه المجموعة")
            return false
        }
        flash(key)
        return transmit(signal, key)
    }

    private fun transmit(signal: IrSignal, key: String): Boolean {
        if (!irAvailable || settings.demoMode) {
            showBanner("وضع المعاينة: الإشارة جاهزة لكن الجهاز لا يُرسل أشعة")
            return true
        }
        val encoded = IrEncoder.encode(signal)
        if (encoded == null) {
            showBanner("بروتوكول غير مدعوم (${signal.protocol})")
            return false
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = transmitter.sendEncoded(encoded)
            result.exceptionOrNull()?.let { error ->
                withContext(Dispatchers.Main) {
                    showBanner(error.message ?: "تعذّر إرسال الإشارة")
                }
            }
        }
        return true
    }

    /** بدء الإرسال المتكرر عند الاستمرار بالضغط */
    fun startHold(device: SavedDevice, key: String, view: View? = null) {
        stopHold()
        val signal = device.codes[key] ?: return
        feedback.tap(view)
        flash(key)
        if (!irAvailable || settings.demoMode) {
            showBanner("وضع المعاينة: الإشارة جاهزة لكن الجهاز لا يُرسل أشعة")
            return
        }
        val encoded = IrEncoder.encode(signal) ?: return
        holdJob = viewModelScope.launch(Dispatchers.IO) {
            val first = transmitter.sendEncoded(encoded)
            first.exceptionOrNull()?.let { error ->
                withContext(Dispatchers.Main) { showBanner(error.message ?: "تعذّر إرسال الإشارة") }
                return@launch
            }
            delay(360)
            var count = 0
            while (isActive && count < 60) {
                if (transmitter.sendHold(encoded).isFailure) break
                delay(110)
                count++
            }
        }
    }

    fun stopHold() {
        holdJob?.cancel()
        holdJob = null
    }

    /* ------------------------- الأجهزة ------------------------- */

    fun saveDevice(brand: Brand, set: CodeSet, displayName: String? = null): SavedDevice {
        val device = SavedDevice(
            id = UUID.randomUUID().toString(),
            name = displayName?.trim().takeUnless { it.isNullOrEmpty() }
                ?: "تلفزيون ${brand.nameAr}",
            brandAr = brand.nameAr,
            brandEn = brand.nameEn,
            setName = set.name,
            codes = set.codes,
            createdAt = System.currentTimeMillis(),
            isPrimary = devices.isEmpty(),
        )
        val saved = deviceStore.add(device)
        refreshDevices()
        feedback.success()
        return saved
    }

    fun saveManual(brand: Brand, set: CodeSet) = saveDevice(brand, set)

    fun setPrimary(id: String) {
        deviceStore.setPrimary(id)
        refreshDevices()
    }

    fun rename(id: String, name: String) {
        if (name.isBlank()) return
        deviceStore.rename(id, name.trim())
        refreshDevices()
    }

    fun delete(id: String) {
        deviceStore.delete(id)
        refreshDevices()
    }

    fun deleteAll() {
        deviceStore.clear()
        refreshDevices()
    }

    fun primaryDevice(): SavedDevice? = devices.firstOrNull { it.isPrimary } ?: devices.firstOrNull()

    /* ------------------------- واجهة ------------------------- */

    fun showBanner(message: String) {
        banner = message
        bannerJob?.cancel()
        bannerJob = viewModelScope.launch {
            delay(3200)
            banner = null
        }
    }

    fun dismissBanner() {
        bannerJob?.cancel()
        banner = null
    }

    private fun flash(key: String) {
        activeKey = key
        viewModelScope.launch {
            delay(180)
            if (activeKey == key) activeKey = null
        }
    }

    override fun onCleared() {
        stopHold()
        super.onCleared()
    }
}
