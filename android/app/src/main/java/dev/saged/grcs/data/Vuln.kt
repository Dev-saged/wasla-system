package dev.saged.grcs.data

import org.json.JSONObject

class VulnResult(val score: Int, val tier: String, val factors: List<Pair<String, Int>>)
class VulnFamily(val head: JSONObject, val members: Int, val result: VulnResult)

/** مؤشر الهشاشة: نفس قواعد وأوزان الويب؛ تُستبدل الأوزان بما تضبطه الإدارة في config/vulnWeights عند توفره. */
object Vuln {
    private val rules = listOf(
        "disability" to "إعاقة (لكل فرد)", "femaleHead" to "أرملة أو مطلّقة ربة أسرة", "shelter" to "وضع سكن سيئ", "chronic" to "مرض مزمن",
        "infant" to "رضيع دون سنتين", "maternal" to "حامل أو مرضعة", "bigFamily" to "أسرة أكبر من 7 أفراد", "urgent" to "حاجة عاجلة", "elderly" to "مسن فوق 60",
    )
    private val defaults = mapOf("disability" to 15, "femaleHead" to 15, "shelter" to 15, "chronic" to 10, "infant" to 10, "maternal" to 10, "bigFamily" to 10, "urgent" to 10, "elderly" to 5)
    private val badShelter = setOf("tent_damaged", "shared", "building_damaged", "none")
    val tiers = listOf("critical" to "حرجة", "high" to "عالية", "medium" to "متوسطة", "stable" to "مستقرة")

    var weights: Map<String, Int> = defaults
    var cuts = Triple(60, 40, 20)

    /** يقرأ الأوزان المعتمدة من الخادم إن وُجدت. */
    fun load(token: String) {
        val f = runCatching { Fs.get("config/vulnWeights", token) }.getOrNull() ?: return
        fun num(v: JSONObject?): Int? = v?.let { it.optString("integerValue").toIntOrNull() ?: it.optDouble("doubleValue", Double.NaN).takeIf { d -> !d.isNaN() }?.toInt() }
        f.optJSONObject("weights")?.optJSONObject("mapValue")?.optJSONObject("fields")?.let { w ->
            weights = defaults.mapValues { (k, d) -> num(w.optJSONObject(k))?.takeIf { it in 0..50 } ?: d }
        }
        f.optJSONObject("cuts")?.optJSONObject("mapValue")?.optJSONObject("fields")?.let { c ->
            val cr = num(c.optJSONObject("critical")); val hi = num(c.optJSONObject("high")); val me = num(c.optJSONObject("medium"))
            if (cr != null && hi != null && me != null) cuts = Triple(cr, hi, me)
        }
    }

    fun compute(members: List<JSONObject>): VulnResult {
        val head = members.firstOrNull { it.optBoolean("isFamilyHead") } ?: members.firstOrNull { it.optString("relation") == "head" } ?: members.first()
        val ages = members.map { Stats.ageOf(it) }
        val ms = head.optString("maritalStatus")
        val fate = head.optJSONObject("customFields")?.optString("حالة الزوج/ة").orEmpty()
        val n = mapOf(
            "disability" to members.count { it.optBoolean("hasDisability") },
            "femaleHead" to if (head.optString("gender") == "female" && (ms.contains("أرمل") || ms.contains("مطلق") || Regex("شهيد|متوف").containsMatchIn(fate))) 1 else 0,
            "shelter" to if (head.optString("shelterType") in badShelter) 1 else 0,
            "chronic" to if (members.any { it.optBoolean("hasChronicDisease") }) 1 else 0,
            "infant" to if (ages.any { it != null && it < 2 }) 1 else 0,
            "maternal" to if (members.any { it.optBoolean("isPregnant") || it.optBoolean("isBreastfeeding") }) 1 else 0,
            "bigFamily" to if (members.size > 7) 1 else 0,
            "urgent" to if (members.any { it.optBoolean("urgentNeed") }) 1 else 0,
            "elderly" to if (ages.any { it != null && it >= 60 }) 1 else 0,
        )
        val factors = rules.mapNotNull { (k, l) -> val c = n[k] ?: 0; val w = weights[k] ?: 0; if (c > 0 && w > 0) l to w * c else null }
        val score = factors.sumOf { it.second }.coerceAtMost(100)
        val tier = when { score >= cuts.first -> "critical"; score >= cuts.second -> "high"; score >= cuts.third -> "medium"; else -> "stable" }
        return VulnResult(score, tier, factors)
    }

    fun families(persons: List<JSONObject>): List<VulnFamily> =
        persons.groupBy { it.optString("familyId").ifEmpty { "i_" + it.optString("id") } }.values.map { m ->
            val r = compute(m)
            VulnFamily(m.firstOrNull { it.optBoolean("isFamilyHead") } ?: m.first(), m.size, r)
        }.sortedByDescending { it.result.score }
}
