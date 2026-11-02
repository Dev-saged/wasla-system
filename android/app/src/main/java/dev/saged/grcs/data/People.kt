package dev.saged.grcs.data

import org.json.JSONObject

/** عرض الأفراد = مستندات الأسر المخزنة + التعديلات المعلّقة − المحذوفات. الكتابة محلية ثم تُرفع بـFamWriter. */
class People(private val cache: FamCache) {

    fun persons(camp: String): List<JSONObject> {
        val cleared = cache.meta("cleared_$camp")?.toLongOrNull() ?: 0L
        val byId = LinkedHashMap<String, JSONObject>()
        for (p in Stats.persons(cache.docs(camp), cleared)) byId[p.getString("id")] = p
        for (lp in cache.pending(camp)) {
            val o = JSONObject(lp.json); val id = o.getString("id"); val ex = byId[id]
            if (ex == null || Merge.time(o) >= Merge.time(ex)) byId[id] = o
        }
        for (t in cache.tombs(camp)) byId[t.id]?.let { if (Merge.time(it) <= t.at) byId.remove(t.id) }
        return byId.values.toList()
    }

    /** يحفظ فرداً (جديد أو معدّل): يختم updatedAt ويعلّمه للرفع. */
    fun save(camp: String, p: JSONObject): JSONObject {
        val o = Merge.copy(p)
        val now = Clock.now()
        if (o.optString("id").isEmpty()) o.put("id", java.util.UUID.randomUUID().toString())
        if (o.optLong("createdAt", 0) == 0L) o.put("createdAt", now)
        o.put("campId", camp)
        o.put("updatedAt", now)
        o.remove("syncStatus")
        cache.putPend(camp, o.getString("id"), Merge.personDocId(o), o.toString(), now)
        return o
    }

    fun delete(camp: String, p: JSONObject) {
        cache.putTomb(camp, p.getString("id"), Merge.personDocId(p), Clock.now())
    }

    fun pendingCount(camp: String) = cache.pending(camp).size + cache.tombs(camp).size
}
