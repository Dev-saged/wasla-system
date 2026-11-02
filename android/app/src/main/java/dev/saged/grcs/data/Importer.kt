package dev.saged.grcs.data

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImportPreview(val toAdd: List<JSONObject>, val skippedExisting: Int, val skippedInvalid: Int, val mapped: List<String>, val headCount: Int)

/** استيراد نموذج مسطّح: صف لكل شخص. الاعتماد على عناوين الأعمدة؛ لا يعدّل ولا يحذف سجلاً موجوداً (يتخطى المكرر بالهوية). */
object Importer {
    private fun norm(s: String): String = s.replace(Regex("[\\u064B-\\u0652\\u0640]"), "").replace(Regex("[أإآ]"), "ا").replace('ة', 'ه').replace('ى', 'ي').replace(Regex("\\s+"), "")
    private fun digits(s: String): String = buildString { for (c in s) append(when (c) { in '٠'..'٩' -> '0' + (c - '٠'); in '۰'..'۹' -> '0' + (c - '۰'); else -> c }) }.trim()

    private val fields = listOf(
        "headNid" to listOf("هويهربالاسره", "هويهالوصي", "هويهالمعيل", "هويهالاب", "هويهربالاسرة"),
        "nid" to listOf("رقمالهويه", "الهويه", "هويه", "رقمالهوية", "الهوية"),
        "name" to listOf("الاسمالكامل", "اسمالفرد", "الاسم", "اسم"),
        "phone" to listOf("رقمالجوال", "الجوال", "جوال", "الهاتف", "هاتف", "موبايل"),
        "dob" to listOf("تاريخالميلاد", "الميلاد", "ميلاد"),
        "gender" to listOf("الجنس", "جنس", "النوع"),
        "marital" to listOf("الحالهالاجتماعيه", "الحالةالاجتماعية", "الحالهالاجتماعيه"),
        "relation" to listOf("صلهالقرابه", "الصله", "صله", "القرابه"),
        "address" to listOf("العنوان", "السكن"),
    )

    private fun detect(headers: List<String>): Map<String, Int> {
        val out = HashMap<String, Int>(); val used = HashSet<Int>()
        for ((f, keys) in fields) {
            val nk = keys.map { norm(it) }
            val idx = headers.indices.firstOrNull { it !in used && norm(headers[it]) in nk }
                ?: headers.indices.firstOrNull { it !in used && nk.any { k -> norm(headers[it]).contains(k) && !(f == "nid" && norm(headers[it]).contains("رب")) } && !(f == "name" && norm(headers[it]).contains("رب")) }
            if (idx != null) { out[f] = idx; used += idx }
        }
        return out
    }

    private fun date(raw: String): String {
        val s = digits(raw)
        if (s.isEmpty()) return ""
        Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})").find(s)?.let { return "%s-%02d-%02d".format(it.groupValues[1], it.groupValues[2].toInt(), it.groupValues[3].toInt()) }
        Regex("^(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})").find(s)?.let { return "%s-%02d-%02d".format(it.groupValues[3], it.groupValues[2].toInt(), it.groupValues[1].toInt()) }
        s.toDoubleOrNull()?.takeIf { it in 1.0..80000.0 }?.let { return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(((it - 25569) * 86400000).toLong() - java.util.TimeZone.getDefault().rawOffset.toLong())) }
        return ""
    }

    private fun gender(raw: String): String {
        val s = norm(raw).lowercase()
        return when { s.startsWith("ذكر") || s == "m" || s == "male" -> "male"; s.startsWith("انث") || s == "f" || s == "female" -> "female"; else -> "" }
    }

    private fun rel(raw: String): String {
        val s = norm(raw)
        return when { s.contains("زوجه") -> "wife"; s.contains("زوج") -> "husband"; s.contains("ابنه") || s.contains("بنت") -> "daughter"; s.contains("ابن") -> "son"; s.contains("يتيم") -> "orphan"; s.isEmpty() -> "other"; else -> "other" }
    }

    fun preview(rows: List<List<String>>, camp: String, existing: List<JSONObject>, userId: String): ImportPreview {
        if (rows.size < 2) return ImportPreview(emptyList(), 0, 0, emptyList(), 0)
        val h = rows[0]; val m = detect(h)
        val mapped = m.entries.sortedBy { it.value }.map { "${h[it.value]} ← ${it.key}" }
        val nameIdx = m["name"] ?: return ImportPreview(emptyList(), 0, rows.size - 1, mapped, 0)
        val have = existing.map { it.optString("nationalId") }.filter { it.isNotEmpty() }.toHashSet()
        val seen = HashSet<String>(); val out = ArrayList<JSONObject>(); var dupes = 0; var bad = 0
        val now = Clock.now()
        fun cell(r: List<String>, f: String) = m[f]?.let { r.getOrNull(it) }.orEmpty().trim()
        for (r in rows.drop(1)) {
            val name = r.getOrNull(nameIdx).orEmpty().trim()
            if (name.length < 2) { if (r.any { it.isNotEmpty() }) bad++; continue }
            val nid = digits(cell(r, "nid"))
            if (nid.isNotEmpty() && (nid in have || !seen.add(nid))) { dupes++; continue }
            val headNid = digits(cell(r, "headNid"))
            val isHead = headNid.isEmpty() || headNid == nid
            val fid = if (isHead) nid.ifEmpty { java.util.UUID.randomUUID().toString() } else headNid
            val dob = date(cell(r, "dob"))
            val g = gender(cell(r, "gender"))
            val p = JSONObject().put("id", java.util.UUID.randomUUID().toString()).put("campId", camp).put("fullName", name).put("nationalId", nid)
                .put("birthDate", if (dob.isEmpty()) JSONObject.NULL else dob).put("calculatedAge", dobAge(dob) ?: JSONObject.NULL).put("gender", g)
                .put("phone", digits(cell(r, "phone"))).put("address", cell(r, "address")).put("notes", "").put("isFamilyHead", isHead)
                .put("familyId", fid).put("relation", if (isHead) "head" else rel(cell(r, "relation")))
                .put("maritalStatus", if (isHead) cell(r, "marital") else "").put("isPregnant", false).put("isBreastfeeding", false)
                .put("hasChronicDisease", false).put("hasDisability", false).put("createdAt", now).put("createdBy", userId).put("updatedBy", userId)
            out += p
        }
        return ImportPreview(out, dupes, bad, mapped, out.count { it.optBoolean("isFamilyHead") })
    }

    private fun dobAge(d: String): Int? {
        val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").find(d) ?: return null
        val t = java.util.Calendar.getInstance()
        var a = t.get(java.util.Calendar.YEAR) - m.groupValues[1].toInt()
        val dm = t.get(java.util.Calendar.MONTH) + 1 - m.groupValues[2].toInt()
        if (dm < 0 || (dm == 0 && t.get(java.util.Calendar.DAY_OF_MONTH) < m.groupValues[3].toInt())) a--
        return a.coerceAtLeast(0)
    }
}
