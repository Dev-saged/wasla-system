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
    /** خيارات حقل «المخيم» تُملأ من الشاشة. */
    @Volatile var camps: List<Pair<String, String>> = emptyList()
    fun optionsOf(f: QFieldDef) = if (f.k == "camp") camps else f.options
    val chronic = listOf("any" to "أي مرض مزمن", "heart" to "قلب", "diabetes" to "سكري", "hypertension" to "ضغط الدم", "kidney" to "كلى", "cancer" to "سرطان", "respiratory" to "ربو / جهاز تنفسي",
        "liver" to "كبد", "thyroid" to "غدة درقية", "mental" to "أمراض نفسية", "blood" to "أمراض دم", "neuro" to "أمراض عصبية", "bone" to "مفاصل / عظام", "other" to "أخرى")

    val fields = listOf<QFieldDef>(
        QFieldDef("camp", "المخيم", false),
        QFieldDef("gender", "الجنس", false, listOf("male" to "ذكر", "female" to "أنثى", "unknown" to "غير محدد")),
        QFieldDef("age", "العمر", true),
        QFieldDef("relation", "صلة القرابة", false, listOf("head" to "رب أسرة", "wife" to "زوجة", "child" to "ابن/ابنة", "other" to "فرد آخر")),
        QFieldDef("marital", "الحالة الاجتماعية", false, listOf("widowed" to "أرمل/ة", "divorced" to "مطلق/ة", "married" to "متزوج/ة", "single" to "أعزب/عزباء")),
        QFieldDef("chronic", "مرض مزمن", false, chronic),
        QFieldDef("flag", "حالة خاصة", false, listOf("disability" to "ذو إعاقة", "pregnant" to "حامل", "breastfeeding" to "مرضعة", "urgent" to "احتياج طارئ",
            "nophone" to "بلا هاتف", "noid" to "بلا هوية", "badid" to "هوية غير صحيحة", "nodob" to "بلا تاريخ ميلاد")),
        QFieldDef("tier", "مستوى هشاشة الأسرة", false, Vuln.tiers),
        QFieldDef("score", "مؤشر الهشاشة", true),
        QFieldDef("shelter", "نوع مأوى الأسرة", false, listOf("tent_ok" to "خيمة سليمة", "tent_damaged" to "خيمة تالفة", "shared" to "مأوى مشترك", "building_damaged" to "مبنى متضرر", "none" to "بلا مأوى")),
        QFieldDef("famsize", "حجم الأسرة", true),
    )

    private fun c(k: String, v: String = "", min: String = "", max: String = "", not: Boolean = false) = Cond(k, v, min, max, not)

    val presets = listOf(
        Preset("إناث فوق 60 + مرض مزمن", listOf(listOf(c("gender", "female"), c("age", min = "60"), c("chronic", "any")))),
        Preset("أسر حرجة بلا مأوى سليم", listOf(listOf(c("relation", "head"), c("tier", "critical"), c("shelter", "tent_ok", not = true)))),
        Preset("رضّع دون سنتين", listOf(listOf(c("age", min = "0", max = "1")))),
        Preset("ذوو إعاقة أو حوامل", listOf(listOf(c("flag", "disability")), listOf(c("flag", "pregnant")))),
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
            "camp" -> p.optString("campId") == c.v
            "chronic" -> if (c.v == "any") p.optBoolean("hasChronicDisease") else p.optJSONArray("chronicConditions")?.let { a -> (0 until a.length()).any { a.optString(it) == c.v } } == true
            "gender" -> p.optString("gender").ifEmpty { "unknown" } == c.v
            "relation" -> when (c.v) { "head" -> p.optBoolean("isFamilyHead"); "child" -> p.optString("relation") in listOf("child", "son", "daughter")
                "wife" -> p.optString("relation") in listOf("wife", "husband"); else -> !p.optBoolean("isFamilyHead") && p.optString("relation") !in listOf("child", "son", "daughter", "wife", "husband", "head") }
            "marital" -> when (c.v) { "widowed" -> ms.contains("أرمل"); "divorced" -> ms.contains("مطلق"); "married" -> ms.contains("متزوج"); else -> Regex("اعزب|أعزب|عزباء").containsMatchIn(ms) }
            "flag" -> when (c.v) { "disability" -> p.optBoolean("hasDisability"); "pregnant" -> p.optBoolean("isPregnant"); "breastfeeding" -> p.optBoolean("isBreastfeeding")
                "chronic" -> p.optBoolean("hasChronicDisease"); "urgent" -> p.optBoolean("urgentNeed"); "nophone" -> p.optString("phone").isEmpty()
                "noid" -> Nid.norm(p.optString("nationalId")).isEmpty(); "badid" -> Nid.status(p.optString("nationalId")) in listOf("invalid", "format"); else -> p.optString("birthDate").let { it.isEmpty() || it == "null" } }
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

    val exportHeader = listOf("الاسم الكامل", "رقم الهوية", "العمر", "الجنس", "صلة القرابة", "الهاتف", "اسم ولي الأمر", "هوية ولي الأمر", "جوال ولي الأمر", "مؤشر الهشاشة", "مستوى الهشاشة", "نوع المأوى", "المخيم", "مسؤول المخيم", "هاتف المسؤول")
    private val shelterLabels = mapOf("tent_ok" to "خيمة سليمة", "tent_damaged" to "خيمة تالفة", "shared" to "مأوى مشترك", "building_damaged" to "مبنى متضرر", "none" to "بلا مأوى")

    /** صفوف qbExport: all = كل الأفراد (لحساب ولي الأمر والهشاشة)، camps = id ← (الاسم، المسؤول، هاتفه). */
    fun exportRows(result: List<JSONObject>, all: List<JSONObject>, camps: Map<String, Triple<String, String, String>>): List<List<String>> {
        val x = Ctx(all)
        return result.map { p ->
            val h = x.head(p); val v = x.vuln(p); val c = camps[p.optString("campId")]
            val g = if (h.optString("id") == p.optString("id") || !h.optBoolean("isFamilyHead")) listOf("", "", "") else listOf(h.optString("fullName"), h.optString("nationalId"), h.optString("phone"))
            listOf(p.optString("fullName"), p.optString("nationalId"), Stats.ageOf(p)?.toString().orEmpty(), when (p.optString("gender")) { "male" -> "ذكر"; "female" -> "أنثى"; else -> "—" }, Rel.label(p), p.optString("phone")) + g +
                listOf(v.score.toString(), Vuln.tiers.firstOrNull { it.first == v.tier }?.second ?: v.tier, shelterLabels[Vuln.effectiveShelter(h)].orEmpty(), c?.first ?: p.optString("campId"), c?.second.orEmpty(), c?.third.orEmpty())
        }
    }

    fun describe(c: Cond): String {
        val f = fields.firstOrNull { it.k == c.k } ?: return c.k
        if (f.range) return f.label + ": " + listOf(c.min.takeIf { it.isNotEmpty() }?.let { "من $it" }, c.max.takeIf { it.isNotEmpty() }?.let { "إلى $it" }).filterNotNull().joinToString(" ")
        return f.label + (if (c.not) " ليس " else ": ") + (optionsOf(f).firstOrNull { it.first == c.v }?.second ?: c.v)
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
