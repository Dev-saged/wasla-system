package dev.saged.grcs.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** ساعة منطقية مطابقة للويب: توقيت الخادم تقريباً، متصاعدة دائماً، لتفادي انحراف ساعة الجهاز في الدمج. */
object Clock {
    private var prefs: android.content.SharedPreferences? = null
    private var off = 0L
    private var last = 0L
    private var n = 0

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("grcs_clock", Context.MODE_PRIVATE)
        off = prefs!!.getLong("off", 0L); last = prefs!!.getLong("last", 0L); n = if (off != 0L) 1 else 0
    }

    private fun save() { prefs?.edit()?.putLong("off", off)?.putLong("last", last)?.apply() }
    private fun server() = System.currentTimeMillis() + off

    fun parseIso(iso: String): Long? = runCatching {
        val m = Regex("""^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d+))?Z$""").find(iso) ?: return null
        val ms = (m.groupValues[2] + "000").take(3)
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(m.groupValues[1] + "." + ms + "Z")?.time
    }.getOrNull()

    @Synchronized
    fun sample(iso: String, t0: Long, t1: Long) {
        val s = parseIso(iso) ?: return
        if (t1 < t0 || t1 - t0 > 8000) return
        val est = s - (t0 + t1) / 2
        off = if (n > 0 && Math.abs(est - off) < 120_000) (off * 0.8 + est * 0.2).toLong() else est
        n++; save()
    }

    @Synchronized
    fun now(): Long {
        val t = server()
        last = if (t > last) t else last + 1
        save(); return last
    }

    @Synchronized
    fun observe(ts: Long) {
        if (ts <= last || ts > server() + 5 * 60_000) return
        last = ts; save()
    }
}
