package com.alharamayn.mahamess.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "mahamess_settings")

data class AppSettings(
    val shopName: String = "محامص الحرمين",
    val branch: String = "الفرع الرئيسي",
    val phone: String = "",
    val address: String = "",
    val ozGrams: Int = 250,
    val defaultCurrency: String = "SYP",
    val rate: Long = 1250000,          // 12500.00 ل.س للدولار
    val theme: String = "light",
    val pinEnabled: Boolean = true,
    val managerPin: String = "1234",
    val invoiceFooter: String = "شكراً لتعاملكم مع محامص الحرمين",
    val autoBackup: Boolean = true,
    val lastBackup: Long = 0L,
)

class SettingsStore(private val context: Context) {

    private object K {
        val shopName = stringPreferencesKey("shop_name")
        val branch = stringPreferencesKey("branch")
        val phone = stringPreferencesKey("phone")
        val address = stringPreferencesKey("address")
        val ozGrams = longPreferencesKey("oz_grams")
        val defaultCurrency = stringPreferencesKey("default_currency")
        val rate = longPreferencesKey("rate")
        val theme = stringPreferencesKey("theme")
        val pinEnabled = booleanPreferencesKey("pin_enabled")
        val managerPin = stringPreferencesKey("manager_pin")
        val invoiceFooter = stringPreferencesKey("invoice_footer")
        val autoBackup = booleanPreferencesKey("auto_backup")
        val lastBackup = longPreferencesKey("last_backup")
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            shopName = p[K.shopName] ?: "محامص الحرمين",
            branch = p[K.branch] ?: "الفرع الرئيسي",
            phone = p[K.phone] ?: "",
            address = p[K.address] ?: "",
            ozGrams = (p[K.ozGrams] ?: 250L).toInt(),
            defaultCurrency = p[K.defaultCurrency] ?: "SYP",
            rate = p[K.rate] ?: 1250000L,
            theme = p[K.theme] ?: "light",
            pinEnabled = p[K.pinEnabled] ?: true,
            managerPin = p[K.managerPin] ?: "1234",
            invoiceFooter = p[K.invoiceFooter] ?: "شكراً لتعاملكم مع محامص الحرمين",
            autoBackup = p[K.autoBackup] ?: true,
            lastBackup = p[K.lastBackup] ?: 0L,
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val current = AppSettings(
                shopName = p[K.shopName] ?: "محامص الحرمين",
                branch = p[K.branch] ?: "الفرع الرئيسي",
                phone = p[K.phone] ?: "",
                address = p[K.address] ?: "",
                ozGrams = (p[K.ozGrams] ?: 250L).toInt(),
                defaultCurrency = p[K.defaultCurrency] ?: "SYP",
                rate = p[K.rate] ?: 1250000L,
                theme = p[K.theme] ?: "light",
                pinEnabled = p[K.pinEnabled] ?: true,
                managerPin = p[K.managerPin] ?: "1234",
                invoiceFooter = p[K.invoiceFooter] ?: "شكراً لتعاملكم مع محامص الحرمين",
                autoBackup = p[K.autoBackup] ?: true,
                lastBackup = p[K.lastBackup] ?: 0L,
            )
            val next = transform(current)
            p[K.shopName] = next.shopName
            p[K.branch] = next.branch
            p[K.phone] = next.phone
            p[K.address] = next.address
            p[K.ozGrams] = next.ozGrams.toLong()
            p[K.defaultCurrency] = next.defaultCurrency
            p[K.rate] = next.rate
            p[K.theme] = next.theme
            p[K.pinEnabled] = next.pinEnabled
            p[K.managerPin] = next.managerPin
            p[K.invoiceFooter] = next.invoiceFooter
            p[K.autoBackup] = next.autoBackup
            p[K.lastBackup] = next.lastBackup
        }
    }
}
