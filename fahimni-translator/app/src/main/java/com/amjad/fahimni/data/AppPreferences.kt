package com.amjad.fahimni.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("fahimni_prefs_v1", Context.MODE_PRIVATE)

    var myDialect: DialectProfile
        get() = DialectProfile.fromId(prefs.getString("my_dialect", DialectProfile.AR_SY.id))
        set(value) = prefs.edit().putString("my_dialect", value.id).apply()

    var partnerDialect: DialectProfile
        get() {
            val saved = prefs.getString("partner_dialect", DialectProfile.AUTO_DETECT.id)
            return if (saved == DialectProfile.AUTO_DETECT.id) {
                DialectProfile.AUTO_DETECT
            } else {
                DialectProfile.fromId(saved)
            }
        }
        set(value) = prefs.edit().putString("partner_dialect", value.id).apply()

    var lastDetectedPartnerDialect: DialectProfile?
        get() {
            val id = prefs.getString("last_detected_partner", null) ?: return null
            return DialectProfile.fromId(id)
        }
        set(value) = prefs.edit().putString("last_detected_partner", value?.id).apply()

    var autoSyncTargetToDetected: Boolean
        get() = prefs.getBoolean("auto_sync_target", true)
        set(value) = prefs.edit().putBoolean("auto_sync_target", value).apply()

    var autoSpeak: Boolean
        get() = prefs.getBoolean("auto_speak", true)
        set(value) = prefs.edit().putBoolean("auto_speak", value).apply()

    var speechSpeed: Float
        get() = prefs.getFloat("speech_speed", 1.0f)
        set(value) = prefs.edit().putFloat("speech_speed", value).apply()

    var handsFreeLoop: Boolean
        get() = prefs.getBoolean("hands_free_loop", false)
        set(value) = prefs.edit().putBoolean("hands_free_loop", value).apply()

    var deepCloudAiEnabled: Boolean
        get() = prefs.getBoolean("deep_cloud_ai", true)
        set(value) = prefs.edit().putBoolean("deep_cloud_ai", value).apply()

    var partnerSpeechHintLocale: String
        get() = prefs.getString("partner_speech_hint_locale", "ar") ?: "ar"
        set(value) = prefs.edit().putString("partner_speech_hint_locale", value).apply()

    fun saveMessages(messages: List<ConversationMessage>) {
        runCatching {
            val arr = JSONArray()
            messages.takeLast(60).forEach { msg ->
                val notesArr = JSONArray()
                msg.slangNotes.forEach { n ->
                    notesArr.put(JSONObject().apply {
                        put("w", n.word)
                        put("m", n.meaning)
                    })
                }
                arr.put(JSONObject().apply {
                    put("id", msg.id)
                    put("side", msg.side.name)
                    put("orig", msg.originalText)
                    put("trans", msg.translatedText)
                    put("sub", msg.clearArabicSubtitle)
                    put("det", msg.detectedDialect.id)
                    put("tgt", msg.targetDialect.id)
                    put("conf", msg.confidence)
                    put("lat", msg.latencyMs)
                    put("ts", msg.timestamp)
                    put("auto", msg.isAutoDetected)
                    put("notes", notesArr)
                })
            }
            prefs.edit().putString("history_v1", arr.toString()).apply()
        }
    }

    fun loadMessages(): List<ConversationMessage> {
        return runCatching {
            val raw = prefs.getString("history_v1", null) ?: return emptyList()
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val notesJson = obj.optJSONArray("notes")
                    val notes = buildList {
                        if (notesJson != null) {
                            for (j in 0 until notesJson.length()) {
                                val n = notesJson.optJSONObject(j) ?: continue
                                val w = n.optString("w")
                                val m = n.optString("m")
                                if (w.isNotBlank() && m.isNotBlank()) add(SlangNote(w, m))
                            }
                        }
                    }
                    add(
                        ConversationMessage(
                            id = obj.optString("id", "msg_$i"),
                            side = runCatching { SpeakerSide.valueOf(obj.optString("side", "ME")) }.getOrDefault(SpeakerSide.ME),
                            originalText = obj.optString("orig", ""),
                            translatedText = obj.optString("trans", ""),
                            clearArabicSubtitle = obj.optString("sub", ""),
                            detectedDialect = DialectProfile.fromId(obj.optString("det")),
                            targetDialect = DialectProfile.fromId(obj.optString("tgt")),
                            confidence = obj.optInt("conf", 90),
                            slangNotes = notes,
                            latencyMs = obj.optLong("lat", 250L),
                            timestamp = obj.optLong("ts", System.currentTimeMillis()),
                            isAutoDetected = obj.optBoolean("auto", true),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
