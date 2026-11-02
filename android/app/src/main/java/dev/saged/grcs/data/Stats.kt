package dev.saged.grcs.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class CampStats(
    val total: Int, val families: Int, val male: Int, val female: Int, val infant: Int, val child: Int,
    val elderly: Int, val pregnant: Int, val chronic: Int, val disability: Int, val widowed: Int,
    val divorced: Int, val noId: Int, val familiesNoPhone: Int,
) {
    operator fun plus(o: CampStats) = CampStats(total + o.total, families + o.families, male + o.male, female + o.female,
        infant + o.infant, child + o.child, elderly + o.elderly, pregnant + o.pregnant, chronic + o.chronic,
        disability + o.disability, widowed + o.widowed, divorced + o.divorced, noId + o.noId, familiesNoPhone + o.familiesNoPhone)

    companion object { val ZERO = CampStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0) }
}

/** مطابق لـcomputeStats في الويب: الأفراد من مستندات الأسر بعد تطبيق شواهد الحذف وتاريخ الإفراغ. */
object Stats {
    private fun time(p: JSONObject) = p.optLong("updatedAt", 0).takeIf { it != 0L } ?: p.optLong("createdAt", 0)

    fun ageOf(p: JSONObject) = age(p)

    private fun age(p: JSONObject): Int? {
        val bd = p.optString("birthDate")
        val m = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})""").find(bd)
        if (m != null) {
            val (y, mo, d) = m.destructured
            val t = Calendar.getInstance()
            var a = t.get(Calendar.YEAR) - y.toInt()
            val dm = t.get(Calendar.MONTH) + 1 - mo.toInt()
            if (dm < 0 || (dm == 0 && t.get(Calendar.DAY_OF_MONTH) < d.toInt())) a--
            return a.coerceAtLeast(0)
        }
        return if (p.has("calculatedAge") && p.opt("calculatedAge") is Number) p.getInt("calculatedAge") else null
    }

    fun persons(docs: List<Pair<String, String>>, clearedAt: Long): List<JSONObject> {
        val out = LinkedHashMap<String, JSONObject>()
        for ((j, d) in docs) {
            val arr = runCatching { JSONArray(j) }.getOrNull() ?: continue
            val del = runCatching { JSONObject(d.ifBlank { "{}" }) }.getOrDefault(JSONObject())
            for (i in 0 until arr.length()) {
                val p = arr.optJSONObject(i) ?: continue
                if (p.optString("id").isEmpty()) continue
                if (clearedAt > 0 && time(p) <= clearedAt) continue
                if (del.has(p.getString("id")) && time(p) <= del.optLong(p.getString("id"))) continue
                val id = p.getString("id"); val ex = out[id]
                if (ex == null || time(p) >= time(ex)) out[id] = p
            }
        }
        return out.values.toList()
    }

    fun compute(persons: List<JSONObject>): CampStats {
        val fams = HashSet<String>(); val famPhone = HashMap<String, Boolean>()
        var male = 0; var female = 0; var infant = 0; var child = 0; var elderly = 0; var pregnant = 0
        var chronic = 0; var dis = 0; var widowed = 0; var divorced = 0; var noId = 0
        for (p in persons) {
            val fid = p.optString("familyId")
            if (fid.isNotEmpty()) { fams += fid; famPhone[fid] = (famPhone[fid] == true) || p.optString("phone").isNotEmpty() }
            when (p.optString("gender")) { "male" -> male++; "female" -> female++ }
            age(p)?.let { a -> if (a < 1) infant++; if (a in 1..18) child++; if (a >= 60) elderly++ }
            if (p.optBoolean("isPregnant")) pregnant++
            if (p.optBoolean("hasChronicDisease")) chronic++
            if (p.optBoolean("hasDisability")) dis++
            val ms = p.optString("maritalStatus")
            if (ms.contains("أرمل")) widowed++
            if (ms.contains("مطلق")) divorced++
            if (p.optString("nationalId").isEmpty()) noId++
        }
        return CampStats(persons.size, fams.size, male, female, infant, child, elderly, pregnant, chronic, dis, widowed,
            divorced, noId, famPhone.values.count { !it })
    }
}
