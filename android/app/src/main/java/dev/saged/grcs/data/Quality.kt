package dev.saged.grcs.data

import org.json.JSONObject

class Issue(val key: String, val label: String, val people: List<JSONObject>, val hint: String = "", val action: String = "")

/** جودة البيانات بقواعد الويب: نسبة الاكتمال من كل القواعد، وخمسة أقسام عرض. */
object Quality {
    fun nid(p: JSONObject): String = p.optString("nationalId").filter { !it.isWhitespace() }

    fun nidStatus(raw: String): String {
        val s = raw.filter { !it.isWhitespace() && it != '-' && it != '.' }
        if (s.isEmpty()) return "empty"
        if (!s.all { it in '0'..'9' } || s.length < 8 || s.length > 9) return "format"
        val t = if (s.length == 8) "0$s" else s
        if (t.all { it == '0' }) return "invalid"
        var sum = 0
        for (i in 0 until 9) { var d = (t[i] - '0') * ((i % 2) + 1); if (d > 9) d -= 9; sum += d }
        return if (sum % 10 == 0) "valid" else "invalid"
    }

    fun dobWhy(p: JSONObject, head: JSONObject?): String {
        val b = p.optString("birthDate")
        if (b.isEmpty()) return ""
        if (!Regex("^\\d{4}-\\d{2}-\\d{2}").containsMatchIn(b)) return "تاريخ ميلاد غير صالح"
        if (b > java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(System.currentTimeMillis() + 86400000L))) return "تاريخ ميلاد مستقبلي"
        val a = Stats.ageOf(p) ?: return ""
        if (a > 105) return "عمر يتجاوز 105 سنة"
        if (p.optBoolean("isFamilyHead") && a < 16) return "رب أسرة أقل من 16 سنة"
        val r = p.optString("relation")
        if ((r == "wife" || r == "husband") && a < 14) return "زوج/ة أقل من 14 سنة"
        if (!p.optBoolean("isFamilyHead") && (r == "son" || r == "daughter") && head != null && head.optString("birthDate").isNotEmpty()) {
            val ha = Stats.ageOf(head)
            if (ha != null && ha - a < 12) return "فارق العمر عن رب الأسرة أقل من 12 سنة"
        }
        return ""
    }

    class Rule(val key: String, val label: String, val head: Boolean, val test: (JSONObject, Ctx) -> Boolean)
    class Ctx(val dup: Set<String>, val heads: Map<String, JSONObject>)

    private fun cf(p: JSONObject, vararg ks: String): String { val c = p.optJSONObject("customFields") ?: return ""; return ks.firstNotNullOfOrNull { c.optString(it).takeIf { v -> v.isNotEmpty() } }.orEmpty() }
    private fun phone2(p: JSONObject) = p.optString("phone2").ifEmpty { cf(p, "هاتف إضافي", "الجوال 2", "رقم التواصل 2") }
    private fun shelter(p: JSONObject) = p.optString("shelterType").ifEmpty { p.optString("livingSituation") }
    private val adult = listOf("wife", "husband")

    val rules: List<Rule> = listOf(
        Rule("nophone", "أسر بلا هاتف", true) { p, _ -> p.optString("phone").isEmpty() },
        Rule("nophone2", "أسر بلا هاتف إضافي", true) { p, _ -> phone2(p).isEmpty() },
        Rule("noaddress", "أسر بلا عنوان", true) { p, _ -> p.optString("address").isEmpty() },
        Rule("noshelter", "أسر بلا نوع مأوى", true) { p, _ -> shelter(p).isEmpty() },
        Rule("childnodob", "أطفال بلا تاريخ ميلاد", false) { p, _ -> !p.optBoolean("isFamilyHead") && p.optString("relation") in listOf("son", "daughter", "child", "orphan") && noDob(p) },
        Rule("adultnodob", "بالغون بلا تاريخ ميلاد", false) { p, _ -> (p.optBoolean("isFamilyHead") || p.optString("relation") in adult) && noDob(p) },
        Rule("noid", "أفراد بلا رقم هوية", false) { p, _ -> nid(p).isEmpty() },
        Rule("badid", "أرقام هوية غير صحيحة", false) { p, _ -> nidStatus(p.optString("nationalId")).let { it == "invalid" || it == "format" } },
        Rule("nogender", "أفراد بلا جنس محدد", false) { p, _ -> p.optString("gender").let { it.isEmpty() || it == "unknown" } },
        Rule("dupid", "هويات مكررة داخل المخيم", false) { p, c -> nid(p).let { it.isNotEmpty() && it in c.dup } },
        Rule("suspectdob", "تواريخ ميلاد مشبوهة", false) { p, c -> dobWhy(p, c.heads[p.optString("familyId")]).isNotEmpty() },
        Rule("stalenoid", "مواليد بلا هوية تجاوزوا السنتين", false) { p, _ -> nid(p).isEmpty() && p.optBoolean("noIdYet") && (Stats.ageOf(p) ?: 0) >= 2 },
    )

    private fun noDob(p: JSONObject) = p.isNull("birthDate") || p.optString("birthDate").let { it.isEmpty() || it == "null" }

    fun ctx(persons: List<JSONObject>) = Ctx(
        persons.map { nid(it) }.filter { it.isNotEmpty() }.groupingBy { it }.eachCount().filterValues { it > 1 }.keys,
        persons.filter { it.optBoolean("isFamilyHead") }.associateBy { it.optString("familyId") })

    fun match(key: String, p: JSONObject, c: Ctx): Boolean = rules.firstOrNull { it.key == key }?.let { (!it.head || p.optBoolean("isFamilyHead")) && it.test(p, c) } ?: false

    /** عدّ كل قاعدة ونسبة الاكتمال العامة */
    fun counts(persons: List<JSONObject>): Pair<Int, Map<String, Int>> {
        val c = ctx(persons); val m = LinkedHashMap<String, Int>(); var checks = 0; var fails = 0
        rules.forEach { m[it.key] = 0 }
        for (p in persons) for (r in rules) {
            if (r.head && !p.optBoolean("isFamilyHead")) continue
            checks++
            if (r.test(p, c)) { m[r.key] = m.getValue(r.key) + 1; fails++ }
        }
        return (if (checks == 0) 100 else Math.round((1 - fails.toDouble() / checks) * 100).toInt()) to m
    }

    fun report(persons: List<JSONObject>): Pair<Int, List<Issue>> {
        val pct = counts(persons).first
        val c = ctx(persons)
        fun of(k: String) = persons.filter { match(k, it, c) }
        val heads = c.heads
        return pct to listOf(
            Issue("dup", "هويات مكررة داخل المخيم", of("dupid"), "سجلات بنفس الهوية — الدمج يُبقي المختار ويكمل ناقصه من الباقي", "تصحيح"),
            Issue("noid", "أفراد بلا رقم هوية", of("noid"), "ارفع الكشف الوطني من «مطابقة كشف وطني» ليقترح النظام الهويات تلقائياً", "إكمال"),
            Issue("dob", "تواريخ ميلاد مشبوهة", of("suspectdob"), "مستقبلي، عمر مستحيل، أو فارق غير منطقي مع رب الأسرة", "تصحيح"),
            Issue("bad", "أرقام هوية غير صحيحة", of("badid"), "لا تجتاز خانة التحقق أو ليست 9 أرقام", "تصحيح"),
            Issue("stale", "مواليد بلا هوية تجاوزوا السنتين", of("stalenoid"), "حُدّد «لا يوجد رقم هوية بعد» لكن عمرهم تجاوز سنتين — يُرجَّح أن هويتهم صدرت", "إكمال الهوية"),
        )
    }
}
