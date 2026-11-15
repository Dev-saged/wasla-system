package dev.saged.grcs.data

import org.json.JSONObject

/** فلاتر قائمة الأفراد كما في شرائح الويب (PERSON_FILTER_LABELS). */
object PFilter {
    val chips = listOf(
        "head" to "رب الأسرة", "all" to "الكل",
        "tier:critical" to "حرجة", "tier:high" to "عالية", "tier:medium" to "متوسطة", "tier:stable" to "مستقرة",
        "male" to "ذكور", "female" to "إناث",
        "infant" to "رضع <1", "preschool" to "1-5", "school" to "6-18", "youth" to "شباب 18-30", "midAdult" to "بالغون 30-60", "elderly" to "كبار +60",
        "pregnant" to "حوامل", "orphan" to "أيتام", "breastfeeding" to "مرضعات", "under2" to "دون سنتين",
        "injured" to "جرحى", "martyr" to "أسر شهداء", "chronic" to "مرضى", "disability" to "إعاقة",
        "widowed" to "أرامل", "divorced" to "مطلّقات/ين", "nophone" to "بلا هاتف", "noid" to "بلا هوية",
    )

    /** PERSON_FILTER_LABELS في الويب: عنوان الفلتر في الكشوف والترويسات. */
    val labels = mapOf("all" to "كل الأفراد", "head" to "أرباب الأسر", "male" to "الذكور", "female" to "الإناث", "child" to "أطفال 1-18",
        "infant" to "رضّع أقل من سنة", "preschool" to "أطفال 1-5 سنوات", "school" to "أطفال 6-18 (سن مدرسي)", "youth" to "شباب 18-30",
        "mid" to "بالغون 30-60", "midAdult" to "بالغون 30-60", "elderly" to "كبار السن +60", "pregnant" to "حوامل", "chronic" to "أمراض مزمنة", "disability" to "ذوو إعاقة",
        "widowed" to "أرامل", "divorced" to "مطلّقات/ين", "nophone" to "أسر بلا هاتف", "noid" to "بلا رقم هوية",
        "orphan" to "أيتام", "breastfeeding" to "مرضعات", "injured" to "جرحى ومصابون", "martyr" to "أسر شهداء", "under2" to "أطفال دون سنتين", "adults18" to "أبناء فوق 18")

    /** أسماء قديمة للمفاتيح تبقى مقبولة. */
    private fun canon(k: String) = when (k) { "mid" -> "midAdult"; "headOfFamily" -> "head"; else -> k }

    private fun test(k0: String, p: JSONObject): Boolean = when (val k = canon(k0)) {
        "head" -> p.optBoolean("isFamilyHead")
        else -> Derive.has(p, k)
    }

    /** يطبّق الفلتر؛ فلاتر الهشاشة تخص أرباب الأسر بحسب مستوى الأسرة. */
    fun apply(key: String, persons: List<JSONObject>): List<JSONObject> {
        if (key == "all") return persons
        if (key.startsWith("tier:")) {
            val t = key.removePrefix("tier:")
            val ids = Vuln.families(persons).filter { it.result.tier == t }.map { it.head.optString("id") }.toHashSet()
            return persons.filter { it.optString("id") in ids }
        }
        if (key.startsWith("q:")) {
            val q = key.removePrefix("q:"); val c = Quality.ctx(persons)
            return persons.filter { Quality.match(q, it, c) }
        }
        return persons.filter { test(key, it) }
    }

    fun label(key: String): String = labels[key] ?: when {
        key.startsWith("q:") -> Quality.rules.firstOrNull { it.key == key.removePrefix("q:") }?.label
        key.startsWith("tier:") -> Vuln.tiers.firstOrNull { it.first == key.removePrefix("tier:") }?.second?.let { "هشاشة: $it" }
        else -> null
    } ?: key

    /** مفتاح الفلتر لكل بطاقة إحصاء في الرئيسية. */
    val kpiKey = mapOf(
        "أسرة" to "head", "فرد" to "all", "ذكور" to "male", "إناث" to "female", "أطفال" to "child", "رضّع" to "infant", "كبار سن" to "elderly",
        "حوامل" to "pregnant", "أمراض مزمنة" to "chronic", "ذوو إعاقة" to "disability", "أرامل" to "widowed", "مطلقون" to "divorced",
        "بلا هوية" to "noid", "أسر بلا جوال" to "nophone",
    )
}
