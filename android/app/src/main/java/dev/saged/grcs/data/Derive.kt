package dev.saged.grcs.data

import org.json.JSONObject

/** المصنِّف الموحّد (Derive في الويب): مصدر واحد لكل بطاقة إحصاء وكل فلتر قائمة، فلا يختلف الرقم عن القائمة. */
object Derive {
    fun tag(p: JSONObject, t: String) = p.optJSONArray("tags")?.let { a -> (0 until a.length()).any { a.optString(it) == t } } == true
    fun isWidowed(p: JSONObject) = p.optString("maritalStatus").contains("أرمل")
    fun isDivorced(p: JSONObject) = p.optString("maritalStatus").contains("مطلق")
    fun isOrphan(p: JSONObject) = tag(p, "orphan")

    fun keys(p: JSONObject): List<String> {
        val ks = ArrayList<String>(8); val age = Stats.ageOf(p)
        when (p.optString("gender")) { "male" -> ks += "male"; "female" -> ks += "female" }
        if (age != null) {
            if (age < 1) ks += "infant"
            if (age < 2) ks += "under2"
            if (age in 1..18) ks += "child"
            if (age in 1..5) ks += "preschool"
            if (age in 6..17) ks += "school"
            if (age in 18..29) ks += "youth"
            if (age in 30..59) ks += "midAdult"
            if (age in 19..59) ks += "adult"
            if (age >= 60) ks += "elderly"
            if (age >= 18 && !p.optBoolean("isFamilyHead") && (Rel.isChild(p) || p.optString("relation") == "orphan")) ks += "adults18"
        }
        if (p.optBoolean("isPregnant")) ks += "pregnant"
        if (p.optBoolean("isBreastfeeding")) ks += "breastfeeding"
        if (p.optBoolean("hasChronicDisease")) ks += "chronic"
        if (p.optBoolean("hasDisability")) ks += "disability"
        if (isWidowed(p)) ks += "widowed"
        if (isDivorced(p)) ks += "divorced"
        if (isOrphan(p)) ks += "orphan"
        if (tag(p, "injured")) ks += "injured"
        if (tag(p, "martyr_family")) ks += "martyr"
        if (p.optString("nationalId").isEmpty()) ks += "noid"
        if (p.optBoolean("isFamilyHead") && p.optString("phone").isEmpty()) ks += "nophone"
        return ks
    }

    fun has(p: JSONObject, k: String) = k in keys(p)

    /** ageBucket في الويب: فئة عمرية واحدة لكل فرد (للتصدير). */
    fun ageBucket(age: Int?): String = when {
        age == null -> "unknown"; age < 1 -> "infant"; age <= 5 -> "preschool"; age <= 17 -> "school"; age <= 29 -> "youth"; age <= 59 -> "midAdult"; else -> "elderly"
    }

    /** أعداد كل مفتاح لقائمة أفراد. */
    fun counts(persons: List<JSONObject>): Map<String, Int> {
        val c = HashMap<String, Int>()
        for (p in persons) for (k in keys(p)) c[k] = (c[k] ?: 0) + 1
        return c
    }
}
