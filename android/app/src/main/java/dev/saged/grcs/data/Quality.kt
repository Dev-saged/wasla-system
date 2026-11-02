package dev.saged.grcs.data

import org.json.JSONObject

class Issue(val key: String, val label: String, val people: List<JSONObject>)

/** جودة البيانات: فحوص أساسية مطابقة لروح مركز الجودة في الويب (هوية ناقصة/مكررة، تاريخ ميلاد مفقود/مشبوه، جوال رب الأسرة). */
object Quality {
    fun report(persons: List<JSONObject>): Pair<Int, List<Issue>> {
        val dup = persons.filter { it.optString("nationalId").isNotEmpty() }.groupBy { it.optString("nationalId") }.filter { it.value.size > 1 }.values.flatten()
        val issues = listOf(
            Issue("noid", "بلا رقم هوية", persons.filter { it.optString("nationalId").isEmpty() }),
            Issue("dup", "هوية مكررة", dup),
            Issue("nodob", "بلا تاريخ ميلاد", persons.filter { it.optString("birthDate").isEmpty() || it.isNull("birthDate") }),
            Issue("dob", "تاريخ ميلاد مشبوه", persons.filter { suspect(it) }),
            Issue("nophone", "رب أسرة بلا جوال", persons.filter { it.optBoolean("isFamilyHead") && it.optString("phone").isEmpty() }),
            Issue("nogender", "بلا جنس", persons.filter { it.optString("gender").isEmpty() }),
        )
        val checks = persons.size * 5 + persons.count { it.optBoolean("isFamilyHead") }
        val fails = issues.sumOf { it.people.size }
        return (if (checks == 0) 100 else ((1 - fails.toDouble() / checks) * 100).toInt().coerceIn(0, 100)) to issues
    }

    private fun suspect(p: JSONObject): Boolean {
        if (p.optString("birthDate").isEmpty()) return false
        val a = Stats.ageOf(p)
        val d = Regex("^\\d{4}-\\d{2}-\\d{2}").containsMatchIn(p.optString("birthDate"))
        if (!d || a == null) return true
        return a > 105 || (p.optBoolean("isFamilyHead") && a < 16) || p.optString("birthDate") > java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    }
}
