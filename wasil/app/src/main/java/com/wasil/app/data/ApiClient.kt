package com.wasil.app.data

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * عميل شبكة خفيف بدون مكتبات خارجية (HttpURLConnection).
 * يتوقع خادماً يطبّق العقد الموثّق في backend/README.md:
 *   GET /api/health            → { "ok": true }
 *   GET /api/number  (Bearer)  → { "number": "+1…", "country": "US" }
 *   GET /api/messages (Bearer) → { "messages": [ {id,from,body,ts} … ] }
 */
class ApiClient {

    sealed class Result<out T> {
        data class Success<T>(val value: T) : Result<T>()
        data class Failure(val message: String) : Result<Nothing>()
    }

    fun health(baseUrl: String): Result<Boolean> = when (val r = request(baseUrl, "/api/health", null)) {
        is Result.Failure -> r
        is Result.Success -> Result.Success(r.value.optBoolean("ok", true))
    }

    fun fetchNumber(baseUrl: String, token: String): Result<String> =
        when (val r = request(baseUrl, "/api/number", token)) {
            is Result.Failure -> r
            is Result.Success -> {
                val number = r.value.optString("number").trim()
                if (number.isBlank()) Result.Failure("الخادم لم يُرجع رقماً")
                else Result.Success(number)
            }
        }

    fun fetchMessages(baseUrl: String, token: String): Result<List<SmsMessage>> =
        when (val r = request(baseUrl, "/api/messages", token)) {
            is Result.Failure -> r
            is Result.Success -> {
                val arr = r.value.optJSONArray("messages")
                val list = ArrayList<SmsMessage>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        list.add(
                            SmsMessage(
                                id = o.optString("id").ifBlank { "msg-$i-${o.optLong("ts", 0L)}" },
                                from = o.optString("from", "غير معروف"),
                                body = o.optString("body", ""),
                                ts = o.optLong("ts", parseIsoFallback(o.optString("receivedAt"))),
                            )
                        )
                    }
                }
                Result.Success(list.sortedByDescending { it.ts })
            }
        }

    /* ---------------- internals ---------------- */

    private fun fail(message: String): Result<Nothing> = Result.Failure(message)

    private fun request(baseUrl: String, path: String, token: String?): Result<JSONObject> {
        val base = baseUrl.trim().trimEnd('/')
        if (base.isEmpty() || !(base.startsWith("http://") || base.startsWith("https://"))) {
            return fail("رابط الخادم غير صحيح؛ ابدأه بـ https://")
        }
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(base + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 12_000
                setRequestProperty("Accept", "application/json")
                if (!token.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer ${token.trim()}")
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            when {
                code in 200..299 -> Result.Success(JSONObject(raw))
                code == 401 || code == 403 -> fail("الرمز السري مرفوض (401/403). تأكد من API_TOKEN.")
                code == 404 -> fail("المسار غير موجود على الخادم (404).")
                else -> fail("استجابة الخادم: HTTP $code")
            }
        } catch (e: IOException) {
            fail("تعذّر الوصول للخادم: ${e.message ?: "لا يوجد اتصال"}")
        } catch (e: Exception) {
            fail("خطأ: ${e.message ?: "استجابة غير صالحة"}")
        } finally {
            conn?.disconnect()
        }
    }

    /** احتياطي لو أرسل الخادم تاريخ ISO بدل رقم ts */
    private fun parseIsoFallback(iso: String): Long {
        if (iso.isBlank()) return 0L
        return runCatching {
            val fixed = iso.replace("Z", "+00:00")
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
                .parse(fixed)?.time ?: 0L
        }.getOrDefault(0L)
    }
}
