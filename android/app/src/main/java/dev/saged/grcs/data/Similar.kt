package dev.saged.grcs.data

import org.json.JSONArray
import org.json.JSONObject

class SimPair(val a: JSONObject, val b: JSONObject, val score: Double)

/** كشف السجلات المتشابهة داخل الأسرة الواحدة (منطق dupScore في الويب)؛ لا دمج تلقائي — القرار للمستخدم. */
object Similar {
    private fun tokens(s: String) = GenderInfer.key(s).split(' ').filter { it.isNotEmpty() }

    fun jw(a: String, b: String) = jaroWinkler(a, b)

    private fun jaroWinkler(a: String, b: String): Double {
        if (a == b) return 1.0
        val la = a.length; val lb = b.length
        if (la == 0 || lb == 0) return 0.0
        val win = maxOf(0, maxOf(la, lb) / 2 - 1)
        val ma = BooleanArray(la); val mb = BooleanArray(lb); var m = 0
        for (i in 0 until la) for (j in maxOf(0, i - win) until minOf(lb, i + win + 1)) { if (mb[j] || a[i] != b[j]) continue; ma[i] = true; mb[j] = true; m++; break }
        if (m == 0) return 0.0
        var t = 0; var k = 0
        for (i in 0 until la) { if (!ma[i]) continue; while (!mb[k]) k++; if (a[i] != b[k]) t++; k++ }
        val jaro = (m.toDouble() / la + m.toDouble() / lb + (m - t / 2.0) / m) / 3
        var p = 0; while (p < 4 && p < la && p < lb && a[p] == b[p]) p++
        return jaro + p * 0.1 * (1 - jaro)
    }

    private fun lev(a: String, b: String): Int {
        if (a == b) return 0
        val prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var diag = prev[0]; prev[0] = i
            for (j in 1..b.length) { val tmp = prev[j]; prev[j] = minOf(prev[j] + 1, prev[j - 1] + 1, diag + if (a[i - 1] == b[j - 1]) 0 else 1); diag = tmp }
        }
        return prev[b.length]
    }

    private fun day(s: String): Long? = Regex("^(\\d{4})-(\\d{2})-(\\d{2})").find(s)?.let { (it.groupValues[1].toLong() * 372 + it.groupValues[2].toLong() * 31 + it.groupValues[3].toLong()) }

    fun score(a: JSONObject, b: JSONObject): Double {
        val ta = tokens(a.optString("fullName")); val tb = tokens(b.optString("fullName"))
        if (ta.isEmpty() || tb.isEmpty()) return 0.0
        val na = a.optString("nationalId"); val nb = b.optString("nationalId")
        if (na.isNotEmpty() && nb.isNotEmpty() && na != nb) return 0.0
        val da = day(a.optString("birthDate")); val db = day(b.optString("birthDate"))
        if (da != null && db != null && Math.abs(da - db) > 45) return 0.0
        val ga = a.optString("gender"); val gb = b.optString("gender")
        if (ga.isNotEmpty() && gb.isNotEmpty() && ga != "unknown" && gb != "unknown" && ga != gb) return 0.0
        val f1 = ta[0]; val f2 = tb[0]
        val first = when { f1 == f2 -> 1.0; minOf(f1.length, f2.length) >= 4 && lev(f1, f2) == 1 -> 0.9; else -> return 0.0 }
        val ra = ta.drop(1); val rb = tb.drop(1); val n = minOf(ra.size, rb.size)
        var rest = 1.0
        if (n > 0) { rest = jaroWinkler(ra.take(n).joinToString(" "), rb.take(n).joinToString(" ")); if (rest < 0.88) return 0.0 }
        var s = first * 0.7 + rest * 0.3
        if (da != null && db != null && a.optString("birthDate") == b.optString("birthDate")) s = minOf(1.0, s + 0.05)
        return s
    }

    private fun pk(a: JSONObject, b: JSONObject) = listOf(a.optString("id"), b.optString("id")).sorted().joinToString("|")

    fun ignored(cache: FamCache): Set<String> = runCatching { JSONArray(cache.meta("sim_ignore") ?: "[]").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } }.getOrDefault(emptySet())
    fun ignore(cache: FamCache, a: JSONObject, b: JSONObject) { cache.setMeta("sim_ignore", JSONArray((ignored(cache) + pk(a, b)).toList()).toString()) }

    fun find(persons: List<JSONObject>, ignore: Set<String>): List<SimPair> {
        val out = ArrayList<SimPair>()
        persons.groupBy { it.optString("familyId") }.filterKeys { it.isNotEmpty() }.values.forEach { ms ->
            for (i in ms.indices) for (j in i + 1 until ms.size) {
                val a = ms[i]; val b = ms[j]
                if (a.optString("nationalId").isNotEmpty() && b.optString("nationalId").isNotEmpty()) continue
                if (pk(a, b) in ignore) continue
                val s = score(a, b); if (s >= 0.9) out += SimPair(a, b, s)
            }
        }
        return out.sortedByDescending { it.score }
    }

    /** أيّهما يُحتفظ به افتراضياً: الأكمل (رب أسرة، أو له هوية، أو له ميلاد). */
    fun keepFirst(p: SimPair) = !(p.b.optBoolean("isFamilyHead") || (p.b.optString("nationalId").isNotEmpty() && p.a.optString("nationalId").isEmpty()) ||
        (p.b.optString("birthDate").let { it.isNotEmpty() && it != "null" } && p.a.optString("birthDate").let { it.isEmpty() || it == "null" }))
}
