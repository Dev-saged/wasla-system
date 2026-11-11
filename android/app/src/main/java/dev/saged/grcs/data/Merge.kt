package dev.saged.grcs.data

import org.json.JSONArray
import org.json.JSONObject

/** منطق الدمج المنقول حرفياً من الويب (index.html: famDocId/personDocId/mergeRequests). لا يُعدَّل دون مقارنة بالويب. */
object Merge {
    fun fnv1a(s: String): String {
        var h = 0x811c9dc5.toInt()
        for (ch in s) { h = h xor ch.code; h *= 0x01000193 }
        return Integer.toHexString(h).padStart(8, '0')
    }

    private val SAFE = Regex("^[A-Za-z0-9_-]{1,120}$")

    fun famDocId(fid: String): String = if (SAFE.matches(fid)) "f_$fid" else "fh_" + fnv1a(fid) + fnv1a("$fid#")

    fun personDocId(p: JSONObject): String {
        val fid = p.optString("familyId")
        return if (fid.isNotEmpty() && !p.isNull("familyId")) famDocId(fid) else "i_" + p.optString("id")
    }

    fun time(p: JSONObject): Long = p.optLong("updatedAt", 0).takeIf { it != 0L } ?: p.optLong("createdAt", 0)

    fun copy(p: JSONObject) = JSONObject(p.toString())

    /** mergePersons: يُبقي keep ويكمل ناقصه من drop (لا يستبدل قيمة موجودة). يعيد نسخة جديدة. */
    fun persons(keep0: JSONObject, drop: JSONObject): JSONObject {
        val keep = copy(keep0)
        fun has(o: JSONObject, k: String) = o.optString(k).let { it.isNotEmpty() && it != "null" }
        for (k in listOf("birthDate", "nationalId", "phone", "address", "maritalStatus", "shelterType", "livingSituation", "disabilityDetails", "diseaseDetails")) if (!has(keep, k) && has(drop, k)) keep.put(k, drop.opt(k))
        if (keep.optString("gender").let { it.isEmpty() || it == "unknown" } && has(drop, "gender")) keep.put("gender", drop.optString("gender"))
        for (k in listOf("hasChronicDisease", "hasDisability", "isPregnant", "isBreastfeeding", "urgentNeed")) keep.put(k, keep.optBoolean(k) || drop.optBoolean(k))
        fun arr(o: JSONObject) = o.optJSONArray("chronicConditions")?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
        keep.put("chronicConditions", org.json.JSONArray((arr(keep) + arr(drop)).distinct()))
        val dn = drop.optString("notes"); val kn = keep.optString("notes")
        if (dn.isNotEmpty() && !kn.contains(dn)) keep.put("notes", listOf(kn, dn).filter { it.isNotEmpty() }.joinToString(" — "))
        val merged = mergeRequests(keep, drop); for (k in listOf("requests", "aid")) merged.optJSONArray(k)?.let { keep.put(k, it) }
        val cf = JSONObject(); drop.optJSONObject("customFields")?.let { d -> d.keys().forEach { cf.put(it, d.opt(it)) } }; keep.optJSONObject("customFields")?.let { d -> d.keys().forEach { cf.put(it, d.opt(it)) } }
        keep.put("customFields", cf)
        Stats.ageOf(keep)?.let { keep.put("calculatedAge", it) }
        return keep
    }

    fun mergeRequests(a: JSONObject, b: JSONObject): JSONObject {
        for ((k, tk) in listOf("requests" to "updatedAt", "aid" to "at")) {
            val aa = a.optJSONArray(k); val bb = b.optJSONArray(k)
            if ((aa?.length() ?: 0) == 0 && (bb?.length() ?: 0) == 0) continue
            val map = LinkedHashMap<String, JSONObject>()
            for (arr in listOf(bb, aa)) arr?.let {
                for (i in 0 until it.length()) {
                    val r = it.optJSONObject(i) ?: continue
                    val id = r.optString("id"); val ex = map[id]
                    if (ex == null || r.optLong(tk) >= ex.optLong(tk)) map[id] = r
                }
            }
            a.put(k, JSONArray(map.values.toList()))
        }
        return a
    }

    class Result(val persons: List<JSONObject>, val del: JSONObject)

    /** دمج مستند أسرة: بعيد + معلّق محلي، مع شواهد الحذف وتاريخ الإفراغ (نفس خطوات الويب). */
    fun mergeDoc(
        docId: String, remotePersons: List<JSONObject>, remoteDel: JSONObject, local: Map<String, JSONObject>,
        tombs: List<CacheTomb>, docOf: Map<String, String>, cleared: Long, now: Long,
    ): Result {
        val del = JSONObject(remoteDel.toString())
        for (t in tombs) {
            val td = t.doc.ifEmpty { docOf[t.id] }
            if (td == docId || remotePersons.any { it.optString("id") == t.id }) del.put(t.id, maxOf(del.optLong(t.id, 0), t.at))
        }
        val merged = LinkedHashMap<String, JSONObject>()
        for (rp in remotePersons) {
            Clock.observe(rp.optLong("updatedAt", 0))
            if (rp.optString("id").isEmpty() || (cleared > 0 && time(rp) <= cleared)) continue
            val lp = local[rp.optString("id")]
            if (lp != null && personDocId(lp) != docId && time(lp) >= time(rp)) continue
            merged[rp.optString("id")] = rp
        }
        for (lp in local.values) {
            if (personDocId(lp) != docId) continue
            if (cleared > 0 && time(lp) <= cleared) continue
            val id = lp.optString("id"); val ex = merged[id]; val lc = copy(lp)
            merged[id] = if (ex == null || lp.optLong("updatedAt", 0) >= ex.optLong("updatedAt", 0)) mergeRequests(lc, ex ?: JSONObject())
            else mergeRequests(copy(ex), lc)
        }
        val keys = del.keys().asSequence().toList()
        for (id in keys) {
            val t = del.optLong(id)
            val p = merged[id]
            if (p != null) { if (time(p) <= t) merged.remove(id) else del.remove(id) }
            if (del.has(id) && t < now - 30L * 86_400_000) del.remove(id)
        }
        return Result(merged.values.toList(), del)
    }
}

class CacheTomb(val id: String, val doc: String, val at: Long)
