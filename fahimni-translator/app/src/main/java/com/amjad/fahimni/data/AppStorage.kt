package com.amjad.fahimni.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AppStorage(context: Context) {
    private val prefs = context.getSharedPreferences("fahimni_simple_v2", Context.MODE_PRIVATE)

    var detectedPartnerLanguage: SpokenLanguage?
        get() = LanguageCatalog.fromId(prefs.getString("partner_lang_id", null))
        set(value) = prefs.edit().putString("partner_lang_id", value?.id).apply()

    var autoSpeak: Boolean
        get() = prefs.getBoolean("auto_speak", true)
        set(value) = prefs.edit().putBoolean("auto_speak", value).apply()

    fun saveTurns(turns: List<ChatTurn>) {
        runCatching {
            val arr = JSONArray()
            turns.takeLast(40).forEach { t ->
                arr.put(JSONObject().apply {
                    put("id", t.id)
                    put("side", t.side.name)
                    put("orig", t.originalText)
                    put("trans", t.translatedText)
                    put("lang", t.partnerLanguage.id)
                    put("langName", t.partnerLanguage.nameAr)
                    put("langFlag", t.partnerLanguage.flag)
                    put("langIso", t.partnerLanguage.googleIso)
                    put("langLocale", t.partnerLanguage.speechLocale)
                    put("langTts", t.partnerLanguage.ttsLang)
                    put("isAr", t.partnerLanguage.isArabic)
                    put("ts", t.timestamp)
                })
            }
            prefs.edit().putString("turns_v2", arr.toString()).apply()
        }
    }

    fun loadTurns(): List<ChatTurn> {
        return runCatching {
            val raw = prefs.getString("turns_v2", null) ?: return emptyList()
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val langId = o.optString("lang", "en")
                    val lang = LanguageCatalog.fromId(langId) ?: SpokenLanguage(
                        id = langId,
                        nameAr = o.optString("langName", "الإنجليزية"),
                        flag = o.optString("langFlag", "🌐"),
                        googleIso = o.optString("langIso", "en"),
                        speechLocale = o.optString("langLocale", "en-US"),
                        ttsLang = o.optString("langTts", "en"),
                        isArabic = o.optBoolean("isAr", false),
                    )
                    add(
                        ChatTurn(
                            id = o.optString("id", "t_$i"),
                            side = runCatching { TurnSide.valueOf(o.optString("side")) }.getOrDefault(TurnSide.OTHER_PERSON),
                            originalText = o.optString("orig", ""),
                            translatedText = o.optString("trans", ""),
                            partnerLanguage = lang,
                            timestamp = o.optLong("ts", 0L),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
