package com.wasil.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wasil.app.data.ApiClient
import com.wasil.app.data.AppConfig
import com.wasil.app.data.AppMode
import com.wasil.app.data.ConfigStore
import com.wasil.app.data.DemoData
import com.wasil.app.data.SmsMessage
import com.wasil.app.notify.MessageNotifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WasilViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ConfigStore(app)
    private val api = ApiClient()
    private val notifier = MessageNotifier(app)

    private val _config = MutableStateFlow(store.loadConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    private val _number = MutableStateFlow(store.loadNumber())
    val number: StateFlow<String> = _number.asStateFlow()

    private val _messages = MutableStateFlow(store.loadMessages())
    val messages: StateFlow<List<SmsMessage>> = _messages.asStateFlow()

    private val _status = MutableStateFlow(com.wasil.app.data.ConnectStatus.IDLE)
    val status: StateFlow<com.wasil.app.data.ConnectStatus> = _status.asStateFlow()

    private val _lastSync = MutableStateFlow<Long?>(null)
    val lastSync: StateFlow<Long?> = _lastSync.asStateFlow()

    private val _banner = MutableStateFlow<String?>(null)
    val banner: StateFlow<String?> = _banner.asStateFlow()

    private var pollJob: Job? = null

    val needsSetup: Boolean get() = !store.isLaunchedBefore()

    fun dismissBanner() { _banner.value = null }

    fun completeSetup() = store.setLaunchedBefore()

    fun updateConfig(newConfig: AppConfig) {
        store.saveConfig(newConfig)
        _config.value = newConfig
        when (newConfig.mode) {
            AppMode.DEMO -> enterDemoMode()
            AppMode.LIVE -> {
                _status.value = com.wasil.app.data.ConnectStatus.IDLE
                refresh(showFeedback = false)
                restartPolling()
            }
        }
    }

    private fun enterDemoMode() {
        stopPolling()
        _number.value = DemoData.DEMO_NUMBER
        _messages.value = DemoData.messages
        _status.value = com.wasil.app.data.ConnectStatus.OK
        _lastSync.value = System.currentTimeMillis()
        store.saveNumber(DemoData.DEMO_NUMBER)
        store.saveMessages(DemoData.messages)
    }

    /** اختبار الاتصال من شاشة الإعداد قبل الحفظ */
    fun testConnection(url: String, token: String, onResult: (String, Boolean) -> Unit) {
        viewModelScope.launch {
            when (val r = api.health(url)) {
                is ApiClient.Result.Failure -> onResult(r.message, false)
                is ApiClient.Result.Success -> onResult("الاتصال بالخادم ناجح", true)
            }
        }
    }

    fun refresh(showFeedback: Boolean = false) {
        val cfg = _config.value
        if (cfg.mode == AppMode.DEMO) {
            enterDemoMode()
            return
        }
        viewModelScope.launch {
            _status.value = com.wasil.app.data.ConnectStatus.CONNECTING
            // الرقم
            if (_number.value.isBlank()) {
                when (val r = api.fetchNumber(cfg.serverUrl, cfg.token)) {
                    is ApiClient.Result.Success -> {
                        _number.value = r.value
                        store.saveNumber(r.value)
                    }
                    is ApiClient.Result.Failure -> {
                        _status.value = com.wasil.app.data.ConnectStatus.ERROR
                        _banner.value = r.message
                        return@launch
                    }
                }
            }
            // الرسائل
            when (val r = api.fetchMessages(cfg.serverUrl, cfg.token)) {
                is ApiClient.Result.Failure -> {
                    _status.value = com.wasil.app.data.ConnectStatus.ERROR
                    if (showFeedback) _banner.value = r.message
                }
                is ApiClient.Result.Success -> {
                    val incoming = r.value
                    val known = store.loadNotifiedIds()
                    val fresh = incoming.filter { it.id !in known }
                    if (cfg.notificationsEnabled && fresh.isNotEmpty()) {
                        fresh.forEach { notifier.show(it) }
                        store.saveNotifiedIds(known + fresh.map { it.id })
                    } else if (fresh.isNotEmpty()) {
                        store.saveNotifiedIds(known + fresh.map { it.id })
                    }
                    _messages.value = incoming
                    store.saveMessages(incoming)
                    _status.value = com.wasil.app.data.ConnectStatus.OK
                    _lastSync.value = System.currentTimeMillis()
                }
            }
        }
    }

    fun restartPolling() {
        stopPolling()
        if (_config.value.mode != AppMode.LIVE) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                refresh(showFeedback = false)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun clearCachedMessages() {
        store.clearMessages()
        store.saveNotifiedIds(emptySet())
        _messages.value = emptyList()
        _banner.value = "تم مسح الرسائل المحفوظة على هذا الهاتف"
    }

    companion object {
        const val POLL_INTERVAL_MS = 15_000L
    }
}
