package dev.saged.grcs.data

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImportPreview(val toAdd: List<JSONObject>, val skippedExisting: Int, val skippedInvalid: Int, val mapped: List<String>, val headCount: Int, val excluded: List<List<String>> = emptyList())

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

    val fieldLabels = listOf("name" to "الاسم", "nid" to "رقم الهوية", "headNid" to "هوية رب الأسرة", "phone" to "الجوال", "dob" to "تاريخ الميلاد",
        "gender" to "الجنس", "marital" to "الحالة الاجتماعية", "relation" to "صلة القرابة", "address" to "العنوان")

    fun isFlat(h: List<String>) = childGroups(h).isEmpty()
    fun auto(h: List<String>) = detect(h)

    /** بصمة الرؤوس: FNV-1a على الرؤوس المطبّعة، لقالب المطابقة المحفوظ. */
    fun fingerprint(h: List<String>): String {
        var x = 0x811c9dc5.toInt()
        for (c in h.joinToString("|") { norm(it) }) { x = x xor c.code; x *= 0x01000193 }
        return "flat_" + (x.toLong() and 0xffffffffL).toString(16).padStart(8, '0')
    }

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

    /** parseDateCell بمحرك التواريخ الموحّد. */
    internal fun date(raw: String): String = Cells.date(raw).orEmpty()

    private fun gender(raw: String): String {
        val s = norm(raw).lowercase()
        return when { s.startsWith("ذكر") || s == "m" || s == "male" -> "male"; s.startsWith("انث") || s == "f" || s == "female" -> "female"; else -> "" }
    }

    private fun rel(raw: String): String {
        val s = norm(raw)
        return when { s.contains("زوجه") -> "wife"; s.contains("زوج") -> "husband"; s.contains("ابنه") || s.contains("بنت") -> "daughter"; s.contains("ابن") -> "son"; s.contains("يتيم") -> "orphan"; s.isEmpty() -> "other"; else -> "other" }
    }

    fun preview(rows: List<List<String>>, camp: String, existing: List<JSONObject>, userId: String, map: Map<String, Int>? = null): ImportPreview {
        if (rows.size < 2) return ImportPreview(emptyList(), 0, 0, emptyList(), 0)
        val h = rows[0]
        if (childGroups(h).isNotEmpty()) return horizontal(rows, camp, existing, userId)
        val m = map ?: detect(h)
        val mapped = m.entries.sortedBy { it.value }.map { "${h[it.value]} ← ${it.key}" }
        val nameIdx = m["name"] ?: return ImportPreview(emptyList(), 0, rows.size - 1, mapped, 0)
        val have = existing.map { it.optString("nationalId") }.filter { it.isNotEmpty() }.toHashSet()
        val seen = HashSet<String>(); val out = ArrayList<JSONObject>(); var dupes = 0; var bad = 0
        val ex = ArrayList<List<String>>()
        val now = Clock.now()
        fun cell(r: List<String>, f: String) = m[f]?.let { r.getOrNull(it) }.orEmpty().trim()
        for (r in rows.drop(1)) {
            val name = r.getOrNull(nameIdx).orEmpty().trim()
            if (name.length < 2) { if (r.any { it.isNotEmpty() }) { bad++; ex += r + "اسم غير صالح" }; continue }
            val nid = digits(cell(r, "nid"))
            if (nid.isNotEmpty() && (nid in have || !seen.add(nid))) { dupes++; ex += r + "هوية مكررة"; continue }
            val headNid = digits(cell(r, "headNid"))
            val isHead = headNid.isEmpty() || headNid == nid
            val fid = if (isHead) nid.ifEmpty { java.util.UUID.randomUUID().toString() } else headNid
            val dob = date(cell(r, "dob"))
            val g = gender(cell(r, "gender")).ifEmpty { GenderInfer.infer(name) }
            val p = JSONObject().put("id", java.util.UUID.randomUUID().toString()).put("campId", camp).put("fullName", name).put("nationalId", nid)
                .put("birthDate", if (dob.isEmpty()) JSONObject.NULL else dob).put("calculatedAge", dobAge(dob) ?: JSONObject.NULL).put("gender", g)
                .put("phone", digits(cell(r, "phone"))).put("address", cell(r, "address")).put("notes", "").put("isFamilyHead", isHead)
                .put("familyId", fid).put("relation", if (isHead) "head" else rel(cell(r, "relation")))
                .put("maritalStatus", if (isHead) cell(r, "marital") else "").put("isPregnant", false).put("isBreastfeeding", false)
                .put("hasChronicDisease", false).put("hasDisability", false).put("createdAt", now).put("createdBy", userId).put("updatedBy", userId)
            out += p
        }
        return ImportPreview(out, dupes, bad, mapped, out.count { it.optBoolean("isFamilyHead") }, ex)
    }

    class Kid(val name: Int, val nid: Int?, val dob: Int?)

    /** مجموعات أعمدة الأبناء المرقّمة (اسم/هوية/ميلاد) كما detectRepeatingChildGroups. */
    internal fun childGroups(h: List<String>): List<Kid> {
        val g = sortedMapOf<Int, Array<Int?>>()
        h.forEachIndexed { i, raw ->
            val n = Regex("(\\d+)").find(digits(raw))?.value?.toIntOrNull() ?: return@forEachIndexed
            val t = norm(digits(raw).replace(n.toString(), " "))
            if (!Regex("طفل|ابن|child").containsMatchIn(t)) return@forEachIndexed
            val a = g.getOrPut(n) { arrayOfNulls(3) }
            when {
                Regex("هويه|id").containsMatchIn(t) && !t.contains("ميلاد") -> a[1] = i
                Regex("ميلاد|dob|birth").containsMatchIn(t) -> a[2] = i
                Regex("اسم|name").containsMatchIn(t) -> a[0] = i
            }
        }
        return g.values.filter { it[0] != null }.map { Kid(it[0]!!, it[1], it[2]) }
    }

    /** نموذج «شجرة العائلة»: صف = أسرة كاملة (رب الأسرة + الزوجة + الأبناء)؛ إضافة فقط وتخطّي الأسر الموجودة. */
    private fun horizontal(rows: List<List<String>>, camp: String, existing: List<JSONObject>, userId: String): ImportPreview {
        val h = rows[0]; val kids = childGroups(h)
        val used = kids.flatMap { listOfNotNull(it.name, it.nid, it.dob) }.toHashSet()
        val c = HashMap<String, Int>()
        h.forEachIndexed { i, raw ->
            if (i in used) return@forEachIndexed
            val t = norm(raw); if (t.isEmpty()) return@forEachIndexed
            fun put(k: String) { if (k !in c) c[k] = i }
            if (t.contains("زوجه")) { when { t.contains("اسم") -> put("wName"); Regex("هويه|بطاقه").containsMatchIn(t) -> put("wNid"); t.contains("ميلاد") -> put("wDob") }; return@forEachIndexed }
            when {
                Regex("الاسمالرباعي|اسمربالاسره|اسمالمستفيد").containsMatchIn(t) -> put("hName")
                Regex("رقمالهويه|هويهالمستفيد").containsMatchIn(t) -> put("hNid")
                t.contains("تاريخالميلاد") -> put("hDob")
                Regex("الجوال1|رقمالتواصل|هاتف|جوال|موبايل").containsMatchIn(t) && !Regex("2|بديل").containsMatchIn(t) -> put("phone")
                t.contains("الجنس") -> put("gender")
                t.contains("الحالهالاجتماعيه") -> put("marital")
                t.contains("الحالهالصحيه") -> put("health")
                Regex("الحي|المنطقه").containsMatchIn(t) -> put("address")
            }
        }
        val mapped = c.entries.sortedBy { it.value }.map { "${h[it.value]} ← ${it.key}" } + kids.map { "أبناء ← ${h[it.name]}" }
        val hn = c["hName"] ?: return ImportPreview(emptyList(), 0, rows.size - 1, mapped, 0)
        val have = existing.map { it.optString("nationalId") }.filter { it.isNotEmpty() }.toHashSet()
        val seen = HashSet<String>(); val out = ArrayList<JSONObject>(); var dupes = 0; var bad = 0; var heads = 0
        val now = Clock.now()
        fun cell(r: List<String>, i: Int?) = i?.let { r.getOrNull(it) }.orEmpty().trim()
        fun person(name: String, nid: String, dobRaw: String, fid: String, relation: String, head: Boolean, gender: String): JSONObject {
            val dob = date(dobRaw)
            return JSONObject().put("id", java.util.UUID.randomUUID().toString()).put("campId", camp).put("fullName", name).put("nationalId", nid)
                .put("birthDate", if (dob.isEmpty()) JSONObject.NULL else dob).put("calculatedAge", dobAge(dob) ?: JSONObject.NULL).put("gender", gender)
                .put("phone", "").put("address", "").put("notes", "").put("isFamilyHead", head).put("familyId", fid).put("relation", relation)
                .put("maritalStatus", "").put("isPregnant", false).put("isBreastfeeding", false).put("hasChronicDisease", false).put("hasDisability", false)
                .put("createdAt", now).put("createdBy", userId).put("updatedBy", userId)
        }
        for (r in rows.drop(1)) {
            val name = cell(r, hn)
            if (name.length < 2) { if (r.any { it.isNotEmpty() }) bad++; continue }
            val nid = digits(cell(r, c["hNid"]))
            if (nid.isNotEmpty() && (nid in have || !seen.add(nid))) { dupes++; continue }
            val fid = nid.ifEmpty { java.util.UUID.randomUUID().toString() }
            val head = person(name, nid, cell(r, c["hDob"]), fid, "head", true, gender(cell(r, c["gender"])).ifEmpty { GenderInfer.infer(name) })
                .put("phone", digits(cell(r, c["phone"]))).put("address", cell(r, c["address"])).put("maritalStatus", cell(r, c["marital"]))
            val hs = cell(r, c["health"])
            if (hs.isNotEmpty() && norm(hs) != "سليم") {
                if (Regex("اعاقه|إعاقة|طرف صناعي|كرسي متحرك|كفيف|اصم|شلل").containsMatchIn(hs)) head.put("hasDisability", true).put("disabilityDetails", hs)
                else head.put("hasChronicDisease", true).put("diseaseDetails", hs)
            }
            out += head; heads++
            val wn = cell(r, c["wName"])
            if (wn.length >= 2) {
                val wnid = digits(cell(r, c["wNid"]))
                if (wnid.isEmpty() || (wnid !in have && seen.add(wnid))) out += person(wn, wnid, cell(r, c["wDob"]), fid, "wife", false, "female")
            }
            for (k in kids) {
                val kn = cell(r, k.name); if (kn.length < 2) continue
                val knid = digits(cell(r, k.nid))
                if (knid.isNotEmpty() && (knid in have || !seen.add(knid))) continue
                out += person(kn, knid, cell(r, k.dob), fid, "child", false, GenderInfer.infer(kn))
            }
        }
        return ImportPreview(out, dupes, bad, mapped, heads)
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
