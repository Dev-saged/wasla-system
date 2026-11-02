package dev.saged.grcs.data

import org.json.JSONArray
import org.json.JSONObject

/** شرط: k الحقل، v القيمة (أو min/max للمدى)، not للنفي. مجموعة = شروط AND، والمجموعات OR (كمنشئ استعلامات الويب). */
class Cond(val k: String, val v: String = "", val min: String = "", val max: String = "", val not: Boolean = false) {
    fun toJson() = JSONObject().put("k", k).put("v", v).put("min", min).put("max", max).put("not", not)
    companion object { fun of(o: JSONObject) = Cond(o.getString("k"), o.optString("v"), o.optString("min"), o.optString("max"), o.optBoolean("not")) }
}

class QFieldDef(val k: String, val label: String, val range: Boolean, val options: List<Pair<String, String>> = emptyList())
class Preset(val label: String, val groups: List<List<Cond>>)
class Saved(val name: String, val groups: List<List<Cond>>)
class QResult(val persons: List<JSONObject>, val families: Int)

object Query {
    val fields = listOf<QFieldDef>(
        QFieldDef("gender", "الجنس", false, listOf("male" to "ذكر", "female" to "أنثى", "unknown" to "غير محدد")),
        QFieldDef("age", "العمر", true),
        QFieldDef("relation", "صلة القرابة", false, listOf("head" to "رب أسرة", "wife" to "زوجة", "child" to "ابن/ابنة", "other" to "فرد آخر")),
        QFieldDef("marital", "الحالة الاجتماعية", false, listOf("widowed" to "أرمل/ة", "divorced" to "مطلق/ة", "married" to "متزوج/ة", "single" to "أعزب/عزباء")),
        QFieldDef("flag", "حالة خاصة", false, listOf("disability" to "ذو إعاقة", "pregnant" to "حامل", "breastfeeding" to "مرضعة", "chronic" to "مرض مزمن", "urgent" to "احتياج طارئ",
            "nophone" to "بلا هاتف", "noid" to "بلا هوية", "nobirth" to "بلا تاريخ ميلاد")),
        QFieldDef("tier", "مستوى هشاشة الأسرة", false, Vuln.tiers),
        QFieldDef("score", "مؤشر الهشاشة", true),
        QFieldDef("shelter", "نوع مأوى الأسرة", false, listOf("tent_ok" to "خيمة سليمة", "tent_damaged" to "خيمة تالفة", "shared" to "مأوى مشترك", "building_damaged" to "مبنى متضرر", "none" to "بلا مأوى")),
        QFieldDef("famsize", "حجم الأسرة", true),
    )

    private fun c(k: String, v: String = "", min: String = "", max: String = "", not: Boolean = false) = Cond(k, v, min, max, not)

    val presets = listOf(
        Preset("إناث فوق 60 + مرض مزمن", listOf(listOf(c("gender", "female"), c("age", min = "60"), c("flag", "chronic")))),
        Preset("أسر حرجة بلا مأوى سليم", listOf(listOf(c("relation", "head"), c("tier", "critical"), c("shelter", "tent_ok", not = true)))),
        Preset("رضّع دون سنتين", listOf(listOf(c("age", min = "0", max = "1")))),
        Preset("ذوو إعاقة أو حوامل", listOf(listOf(c("flag", "disability")), listOf(c("flag", "pregnant")))),
    )

    /** فئات الشاشة الشاملة: كل فئة استعلام جاهز. */
    val categories = listOf(
        Preset("أطفال دون سنتين", listOf(listOf(c("age", min = "0", max = "1")))),
        Preset("أطفال 2 – 5", listOf(listOf(c("age", min = "2", max = "5")))),
        Preset("أطفال 6 – 12", listOf(listOf(c("age", min = "6", max = "12")))),
        Preset("مراهقون 13 – 17", listOf(listOf(c("age", min = "13", max = "17")))),
        Preset("بالغون 18 – 59", listOf(listOf(c("age", min = "18", max = "59")))),
        Preset("كبار السن 60+", listOf(listOf(c("age", min = "60")))),
        Preset("حوامل", listOf(listOf(c("flag", "pregnant")))),
        Preset("مرضعات", listOf(listOf(c("flag", "breastfeeding")))),
        Preset("ذوو إعاقة", listOf(listOf(c("flag", "disability")))),
        Preset("أمراض مزمنة", listOf(listOf(c("flag", "chronic")))),
        Preset("أرامل", listOf(listOf(c("marital", "widowed")))),
        Preset("مطلقات ومطلقون", listOf(listOf(c("marital", "divorced")))),
        Preset("احتياج طارئ", listOf(listOf(c("flag", "urgent")))),
        Preset("بلا هوية", listOf(listOf(c("flag", "noid")))),
        Preset("بلا هاتف", listOf(listOf(c("flag", "nophone")))),
    )

    private class Ctx(val persons: List<JSONObject>) {
        val byFam = persons.groupBy { it.optString("familyId").ifEmpty { "i_" + it.optString("id") } }
        val tier = HashMap<String, VulnResult>()
        fun key(p: JSONObject) = p.optString("familyId").ifEmpty { "i_" + p.optString("id") }
        fun vuln(p: JSONObject) = tier.getOrPut(key(p)) { Vuln.compute(byFam[key(p)] ?: listOf(p)) }
        fun head(p: JSONObject) = (byFam[key(p)] ?: listOf(p)).let { m -> m.firstOrNull { it.optBoolean("isFamilyHead") } ?: m.first() }
    }

    private fun test(c: Cond, p: JSONObject, x: Ctx): Boolean {
        val f = fields.firstOrNull { it.k == c.k } ?: return true
        if (f.range) {
            val v = when (c.k) { "age" -> Stats.ageOf(p); "score" -> x.vuln(p).score; else -> x.byFam[x.key(p)]?.size ?: 1 } ?: return false
            c.min.toIntOrNull()?.let { if (v < it) return false }
            c.max.toIntOrNull()?.let { if (v > it) return false }
            return true
        }
        val ms = p.optString("maritalStatus")
        val hit = when (c.k) {
            "gender" -> p.optString("gender").ifEmpty { "unknown" } == c.v
            "relation" -> when (c.v) { "head" -> p.optBoolean("isFamilyHead"); "child" -> p.optString("relation") in listOf("child", "son", "daughter")
                "wife" -> p.optString("relation") in listOf("wife", "husband"); else -> !p.optBoolean("isFamilyHead") && p.optString("relation") !in listOf("child", "son", "daughter", "wife", "husband", "head") }
            "marital" -> when (c.v) { "widowed" -> ms.contains("أرمل"); "divorced" -> ms.contains("مطلق"); "married" -> ms.contains("متزوج"); else -> Regex("اعزب|أعزب|عزباء").containsMatchIn(ms) }
            "flag" -> when (c.v) { "disability" -> p.optBoolean("hasDisability"); "pregnant" -> p.optBoolean("isPregnant"); "breastfeeding" -> p.optBoolean("isBreastfeeding")
                "chronic" -> p.optBoolean("hasChronicDisease"); "urgent" -> p.optBoolean("urgentNeed"); "nophone" -> p.optString("phone").isEmpty()
                "noid" -> p.optString("nationalId").isEmpty(); else -> p.optString("birthDate").let { it.isEmpty() || it == "null" } }
            "tier" -> x.vuln(p).tier == c.v
            "shelter" -> Vuln.effectiveShelter(x.head(p)) == c.v
            else -> true
        }
        return if (c.not) !hit else hit
    }

    fun run(persons: List<JSONObject>, groups: List<List<Cond>>): QResult {
        val gs = groups.filter { it.isNotEmpty() }
        if (gs.isEmpty()) return QResult(emptyList(), 0)
        val x = Ctx(persons)
        val r = persons.filter { p -> gs.any { g -> g.all { test(it, p, x) } } }
        return QResult(r, r.map { x.key(it) }.toSet().size)
    }

    fun describe(c: Cond): String {
        val f = fields.firstOrNull { it.k == c.k } ?: return c.k
        if (f.range) return f.label + ": " + listOf(c.min.takeIf { it.isNotEmpty() }?.let { "من $it" }, c.max.takeIf { it.isNotEmpty() }?.let { "إلى $it" }).filterNotNull().joinToString(" ")
        return f.label + (if (c.not) " ليس " else ": ") + (f.options.firstOrNull { it.first == c.v }?.second ?: c.v)
    }

    fun loadSaved(cache: FamCache): List<Saved> = runCatching {
        val a = JSONArray(cache.meta("queries") ?: "[]")
        (0 until a.length()).map { i -> val o = a.getJSONObject(i)
            Saved(o.getString("name"), o.getJSONArray("groups").let { g -> (0 until g.length()).map { gi -> g.getJSONArray(gi).let { cs -> (0 until cs.length()).map { Cond.of(cs.getJSONObject(it)) } } } }) }
    }.getOrDefault(emptyList())

    fun saveAll(cache: FamCache, list: List<Saved>) {
        cache.setMeta("queries", JSONArray(list.map { s -> JSONObject().put("name", s.name).put("groups", JSONArray(s.groups.map { g -> JSONArray(g.map { it.toJson() }) })) }).toString())
    }
}
