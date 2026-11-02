package dev.saged.grcs.auth

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class HttpResult(val code: Int, val json: JSONObject?)

object Http {
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
            return HttpResult(code, runCatching { JSONObject(text) }.getOrNull())
        } finally {
            c.disconnect()
        }
    }
}
