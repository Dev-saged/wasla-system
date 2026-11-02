package dev.saged.grcs.data

import org.json.JSONObject

/** فلاتر قائمة الأفراد كما في شرائح الويب (PERSON_FILTER_LABELS). */
object PFilter {
    val chips = listOf(
        "head" to "رب الأسرة", "all" to "الكل",
        "tier:critical" to "حرجة", "tier:high" to "عالية", "tier:medium" to "متوسطة", "tier:stable" to "مستقرة",
        "male" to "ذكور", "female" to "إناث",
        "infant" to "رضّع <1", "preschool" to "1-5", "school" to "6-18", "youth" to "شباب 18-30", "mid" to "بالغون 30-60", "elderly" to "كبار +60",
        "pregnant" to "حوامل", "orphan" to "أيتام", "breastfeeding" to "مرضعات", "under2" to "دون سنتين",
        "injured" to "جرحى", "martyr" to "أسر شهداء", "chronic" to "مرضى", "disability" to "إعاقة",
        "widowed" to "أرامل", "divorced" to "مطلّقات/ين", "nophone" to "بلا هاتف", "noid" to "بلا هوية",
    )

    val labels = chips.toMap() + mapOf("adults18" to "أبناء فوق 18", "child" to "أطفال 1-18")

    private fun tag(p: JSONObject, t: String) = p.optJSONArray("tags")?.let { a -> (0 until a.length()).any { a.optString(it) == t } } == true
    private fun ms(p: JSONObject) = p.optString("maritalStatus")

    private fun test(k: String, p: JSONObject): Boolean {
        val a = Stats.ageOf(p)
        return when (k) {
            "head" -> p.optBoolean("isFamilyHead")
            "male" -> p.optString("gender") == "male"
            "female" -> p.optString("gender") == "female"
            "infant" -> a != null && a < 1
            "under2" -> a != null && a < 2
            "preschool" -> a != null && a in 1..5
            "school" -> a != null && a in 6..18
            "child" -> a != null && a in 1..18
            "youth" -> a != null && a in 18..30
            "mid" -> a != null && a in 30..60
            "elderly" -> a != null && a >= 60
            "adults18" -> a != null && a >= 18 && !p.optBoolean("isFamilyHead") && p.optString("relation") in listOf("child", "son", "daughter", "orphan")
            "pregnant" -> p.optBoolean("isPregnant")
            "breastfeeding" -> p.optBoolean("isBreastfeeding")
            "orphan" -> p.optString("relation") == "orphan" || tag(p, "orphan")
            "injured" -> tag(p, "injured")
            "martyr" -> tag(p, "martyr_family")
            "chronic" -> p.optBoolean("hasChronicDisease")
            "disability" -> p.optBoolean("hasDisability")
            "widowed" -> ms(p).contains("أرمل")
            "divorced" -> ms(p).contains("مطلق")
            "nophone" -> p.optString("phone").isEmpty()
            "noid" -> p.optString("nationalId").isEmpty()
            else -> true
        }
    }

    /** يطبّق الفلتر؛ فلاتر الهشاشة تخص أرباب الأسر بحسب مستوى الأسرة. */
    fun apply(key: String, persons: List<JSONObject>): List<JSONObject> {
        if (key == "all") return persons
        if (key.startsWith("tier:")) {
            val t = key.removePrefix("tier:")
            val ids = Vuln.families(persons).filter { it.result.tier == t }.map { it.head.optString("id") }.toHashSet()
            return persons.filter { it.optString("id") in ids }
        }
        return persons.filter { test(key, it) }
    }

    /** مفتاح الفلتر لكل بطاقة إحصاء في الرئيسية. */
    val kpiKey = mapOf(
        "أسرة" to "head", "فرد" to "all", "ذكور" to "male", "إناث" to "female", "أطفال" to "child", "رضّع" to "infant", "كبار سن" to "elderly",
        "حوامل" to "pregnant", "أمراض مزمنة" to "chronic", "ذوو إعاقة" to "disability", "أرامل" to "widowed", "مطلقون" to "divorced",
        "بلا هوية" to "noid", "أسر بلا جوال" to "nophone",
    )
}
