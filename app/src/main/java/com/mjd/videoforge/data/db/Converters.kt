package com.mjd.videoforge.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mjd.videoforge.data.model.EncryptedApiKey
import com.mjd.videoforge.data.model.Scene
import java.util.Date

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String = gson.toJson(list ?: emptyList())

    @TypeConverter
    fun fromEncryptedKeyList(value: String?): List<EncryptedApiKey> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<EncryptedApiKey>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun toEncryptedKeyList(list: List<EncryptedApiKey>?): String = gson.toJson(list ?: emptyList())

    @TypeConverter
    fun fromSceneList(value: String?): List<Scene> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<Scene>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun toSceneList(list: List<Scene>?): String = gson.toJson(list ?: emptyList())
}
