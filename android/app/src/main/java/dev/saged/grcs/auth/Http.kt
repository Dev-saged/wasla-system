package dev.saged.grcs.auth

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class HttpResult(val code: Int, val json: JSONObject?, val array: org.json.JSONArray? = null)

object Http {
    fun getPublic(url: String, timeoutMs: Int = 15_000): HttpResult {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = timeoutMs
            c.readTimeout = timeoutMs
            c.setRequestProperty("Accept", "application/vnd.github+json")
            val code = c.responseCode
            val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return HttpResult(code, runCatching { JSONObject(text) }.getOrNull())
        } finally {
            c.disconnect()
        }
    }

    fun get(url: String, bearer: String, timeoutMs: Int = 30_000): HttpResult {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = timeoutMs
            c.readTimeout = timeoutMs
            c.setRequestProperty("Authorization", "Bearer $bearer")
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return HttpResult(code, runCatching { JSONObject(text) }.getOrNull())
        } finally {
            c.disconnect()
        }
    }

    /** POST JSON ويعيد الاستجابة حتى عند أكواد الخطأ؛ الاستثناء فقط عند انقطاع الشبكة. */
    fun postJson(url: String, body: JSONObject, bearer: String? = null, timeoutMs: Int = 20_000): HttpResult {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"
            c.connectTimeout = timeoutMs
            c.readTimeout = timeoutMs
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            if (bearer != null) c.setRequestProperty("Authorization", "Bearer $bearer")
            c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return HttpResult(code, runCatching { JSONObject(text) }.getOrNull(), runCatching { org.json.JSONArray(text) }.getOrNull())
        } finally {
            c.disconnect()
        }
    }
}
