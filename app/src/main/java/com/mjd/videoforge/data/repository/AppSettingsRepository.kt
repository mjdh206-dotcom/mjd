package com.mjd.videoforge.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مستودع الإعدادات باستخدام DataStore
 * يخزن الإعدادات البسيطة (غير العلائقية)
 */
@Singleton
class AppSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

    // Keys
    private val DARK_MODE = booleanPreferencesKey("dark_mode")
    private val AUTO_SAVE = booleanPreferencesKey("auto_save")
    private val DATA_SAVER = booleanPreferencesKey("data_saver")
    private val NOTIFICATIONS = booleanPreferencesKey("notifications")
    private val QUALITY_WARNING = booleanPreferencesKey("quality_warning")
    private val DEFAULT_QUALITY = intPreferencesKey("default_quality")
    private val PREFERRED_MODE = stringPreferencesKey("preferred_mode")
    private val FREE_FIRST = booleanPreferencesKey("free_first")

    // Getters
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE] ?: false }
    val autoSave: Flow<Boolean> = context.dataStore.data.map { it[AUTO_SAVE] ?: true }
    val dataSaver: Flow<Boolean> = context.dataStore.data.map { it[DATA_SAVER] ?: false }
    val notifications: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS] ?: true }
    val qualityWarning: Flow<Boolean> = context.dataStore.data.map { it[QUALITY_WARNING] ?: true }
    val defaultQuality: Flow<Int> = context.dataStore.data.map { it[DEFAULT_QUALITY] ?: 1080 }
    val freeFirst: Flow<Boolean> = context.dataStore.data.map { it[FREE_FIRST] ?: true }

    // Setters
    suspend fun setDarkMode(value: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = value }
    }

    suspend fun setAutoSave(value: Boolean) {
        context.dataStore.edit { it[AUTO_SAVE] = value }
    }

    suspend fun setDataSaver(value: Boolean) {
        context.dataStore.edit { it[DATA_SAVER] = value }
    }

    suspend fun setNotifications(value: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS] = value }
    }

    suspend fun setQualityWarning(value: Boolean) {
        context.dataStore.edit { it[QUALITY_WARNING] = value }
    }

    suspend fun setDefaultQuality(value: Int) {
        context.dataStore.edit { it[DEFAULT_QUALITY] = value }
    }

    suspend fun setFreeFirst(value: Boolean) {
        context.dataStore.edit { it[FREE_FIRST] = value }
    }
}
