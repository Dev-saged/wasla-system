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

    fun report(persons: List<JSONObject>): Pair<Int, List<Issue>> {
        val dupIds = persons.map { nid(it) }.filter { it.isNotEmpty() }.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        val heads = persons.filter { it.optBoolean("isFamilyHead") }.associateBy { it.optString("familyId") }
        fun head(p: JSONObject) = heads[p.optString("familyId")]
        val noId = persons.filter { nid(it).isEmpty() }
        val dup = persons.filter { nid(it) in dupIds }
        val bad = persons.filter { nidStatus(it.optString("nationalId")).let { s -> s == "invalid" || s == "format" } }
        val dob = persons.filter { dobWhy(it, head(it)).isNotEmpty() }
        val stale = noId.filter { it.optBoolean("noIdYet") && (Stats.ageOf(it) ?: 0) >= 2 }
        val noGender = persons.filter { it.optString("gender").let { g -> g.isEmpty() || g == "unknown" } }
        val childNoDob = persons.filter { !it.optBoolean("isFamilyHead") && it.optString("relation") in listOf("son", "daughter", "orphan") && it.optString("birthDate").isEmpty() }
        val adultNoDob = persons.filter { (it.optBoolean("isFamilyHead") || it.optString("relation") in listOf("wife", "husband")) && it.optString("birthDate").isEmpty() }
        val h = persons.filter { it.optBoolean("isFamilyHead") }
        val headFails = h.count { it.optString("phone").isEmpty() } + h.count { it.optString("phone2").isEmpty() } + h.count { it.optString("address").isEmpty() } + h.count { it.optString("shelter").isEmpty() }
        val checks = h.size * 4 + persons.size * 8
        val fails = headFails + noId.size + bad.size + noGender.size + dup.size + dob.size + stale.size + childNoDob.size + adultNoDob.size
        val pct = if (checks == 0) 100 else Math.round((1 - fails.toDouble() / checks) * 100).toInt().coerceIn(0, 100)
        return pct to listOf(
            Issue("dup", "هويات مكررة داخل المخيم", dup, "سجلات بنفس الهوية — راجعها وصحّح أو احذف المكرر", "تصحيح"),
            Issue("noid", "أفراد بلا رقم هوية", noId, "ارفع الكشف الوطني من «مطابقة كشف وطني» ليقترح النظام الهويات تلقائياً", "إكمال"),
            Issue("dob", "تواريخ ميلاد مشبوهة", dob, "مستقبلي، عمر مستحيل، أو فارق غير منطقي مع رب الأسرة", "تصحيح"),
            Issue("bad", "أرقام هوية غير صحيحة", bad, "لا تجتاز خانة التحقق أو ليست 9 أرقام", "تصحيح"),
            Issue("stale", "مواليد بلا هوية تجاوزوا السنتين", stale, "حُدّد «لا يوجد رقم هوية بعد» لكن عمرهم تجاوز سنتين — يُرجَّح أن هويتهم صدرت", "إكمال الهوية"),
        )
    }
}
