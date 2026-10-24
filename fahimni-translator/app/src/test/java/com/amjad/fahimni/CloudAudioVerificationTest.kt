package com.amjad.fahimni

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import java.net.URLEncoder
import java.util.Base64
import java.util.concurrent.TimeUnit

class CloudAudioVerificationTest {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Test
    fun probeLiveCloudAudioEndpoints() {
        val report = StringBuilder()

        // 1) Fetch real spoken Dutch MP3 from Google TTS
        val dutchPhrase = "Goedemorgen, waar is het treinstation alstublieft?"
        val enc = URLEncoder.encode(dutchPhrase, "UTF-8")
        val ttsUrl = "https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=nl&q=$enc"
        val mp3Bytes = runCatching {
            val req = Request.Builder()
                .url(ttsUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(req).execute().use { resp ->
                report.appendLine("TTS_NL status=${resp.code} len=${resp.body?.contentLength()}")
                resp.body?.bytes()
            }
        }.getOrNull()

        report.appendLine("Downloaded MP3 bytes: ${mp3Bytes?.size ?: -1}")

        if (mp3Bytes != null && mp3Bytes.isNotEmpty()) {
            val b64 = Base64.getEncoder().encodeToString(mp3Bytes)

            // 2) Test Pollinations openai-audio with mp3
            for (modelName in listOf("openai-audio", "openai")) {
                runCatching {
                    val t0 = System.currentTimeMillis()
                    val payload = JSONObject().apply {
                        put("model", modelName)
                        put("messages", JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("type", "text")
                                        put("text", "Detect the spoken language ISO code (e.g. nl, en, tr, de, ar), transcribe it, and translate it to Arabic. Return JSON only: {\"iso\":\"...\",\"transcript\":\"...\",\"arabic\":\"...\"}")
                                    })
                                    put(JSONObject().apply {
                                        put("type", "input_audio")
                                        put("input_audio", JSONObject().apply {
                                            put("data", b64)
                                            put("format", "mp3")
                                        })
                                    })
                                })
                            })
                        })
                    }
                    val req = Request.Builder()
                        .url("https://text.pollinations.ai/openai")
                        .post(payload.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(req).execute().use { resp ->
                        val dt = System.currentTimeMillis() - t0
                        val body = resp.body?.string()?.take(500)
                        report.appendLine("POLLINATIONS [$modelName] status=${resp.code} time=${dt}ms body=$body")
                    }
                }.onFailure {
                    report.appendLine("POLLINATIONS [$modelName] err=${it.message}")
                }
            }

            // 3) Check Pollinations models list
            runCatching {
                val req = Request.Builder().url("https://text.pollinations.ai/models").get().build()
                client.newCall(req).execute().use { resp ->
                    report.appendLine("POLLINATIONS_MODELS status=${resp.code} body=${resp.body?.string()?.take(800)}")
                }
            }

            // 4) Check HuggingFace Spaces Gradio v4/v5 Whisper endpoints
            for (spaceHost in listOf(
                "https://hf-audio-whisper-large-v3-turbo.hf.space",
                "https://hf-audio-whisper-large-v3.hf.space",
                "https://openai-whisper.hf.space"
            )) {
                runCatching {
                    val t0 = System.currentTimeMillis()
                    val uploadBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            "files",
                            "speech.mp3",
                            mp3Bytes.toRequestBody("audio/mpeg".toMediaType())
                        )
                        .build()
                    val upReq = Request.Builder()
                        .url("$spaceHost/gradio_api/upload")
                        .post(uploadBody)
                        .build()
                    client.newCall(upReq).execute().use { resp ->
                        val dt = System.currentTimeMillis() - t0
                        report.appendLine("HF_UPLOAD [$spaceHost] status=${resp.code} time=${dt}ms body=${resp.body?.string()?.take(300)}")
                    }
                }.onFailure {
                    report.appendLine("HF_UPLOAD [$spaceHost] err=${it.message}")
                }
            }
        }

        // Write report so we can inspect it
        java.io.File("build/audio_probe_report.txt").apply {
            parentFile?.mkdirs()
            writeText(report.toString())
        }
        println(report.toString())
    }
}
