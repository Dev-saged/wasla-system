package dev.saged.grcs.data

import org.json.JSONObject

/** صلات القرابة كما في الويب: relation ∈ head/wife/husband/child/orphan/other، والنوع الدقيق في memberRelation. */
object Rel {
    /** خيارات صلة فرد الأسرة في النموذج (CHILD_RELATIONS). */
    val child = listOf("son" to "ابن", "daughter" to "ابنة", "nephew" to "ابن أخ/عم", "niece" to "ابنة أخ/عم", "orphan" to "يتيم في رعاية الأسرة")

    /** childGenderFromRel. */
    fun genderOf(rel: String) = when (rel) { "son", "nephew" -> "male"; "daughter", "niece" -> "female"; else -> "" }

    /** سجلات قديمة كتبت الصلة الدقيقة في relation نفسها تُعامل كأبناء. */
    fun isChild(p: JSONObject) = p.optString("relation").let { it == "child" || it == "son" || it == "daughter" || it == "nephew" || it == "niece" }

    /** relationLabel. */
    fun label(p: JSONObject): String {
        val g = p.optString("gender"); val r = p.optString("relation")
        if (r == "head") return "رب الأسرة"
        if (r == "wife") return "الزوجة"
        if (r == "husband") return "الزوج"
        if (r == "orphan") return if (g == "female") "يتيمة في رعاية الأسرة" else "يتيم في رعاية الأسرة"
        when (p.optString("memberRelation").ifEmpty { r }) { "son" -> return "ابن"; "daughter" -> return "ابنة"; "nephew" -> return "ابن أخ/عم"; "niece" -> return "ابنة أخ/عم" }
        if (r == "child") return if (g == "female") "ابنة" else if (g == "male") "ابن" else "ابن/ابنة"
        return p.optString("relationDetail").ifEmpty { "فرد من الأسرة" }
    }
}

/** رقم الهوية: التطبيع وخانة التحقق كما في الويب (normNid / isValidPalId / nidStatus). */
object Nid {
    fun digits(s: String) = buildString { for (c in s) append(when (c) { in '٠'..'٩' -> '0' + (c - '٠'); in '۰'..'۹' -> '0' + (c - '۰'); else -> c }) }
    fun norm(v: String): String {
        val s = digits(v).replace(Regex("[\\s\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF\\u00A0\\-.]"), "")
        return if (s.isNotEmpty() && s.all { it in '0'..'9' }) s else v.trim()
    }
    fun status(v: String): String = Quality.nidStatus(norm(v))
}

/** نتيجة حفظ نموذج الفرد: السجل الأساسي، سجلات تُحفظ معه (الزوج/ة والأبناء)، وسجلات تُحذف. */
class FormResult(val main: JSONObject, val others: List<JSONObject>, val removed: List<JSONObject>, val isNew: Boolean, val skippedNids: Int)
