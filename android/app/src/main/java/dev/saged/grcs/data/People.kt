package dev.saged.grcs.data

import org.json.JSONObject

/** عرض الأفراد = مستندات الأسر المخزنة + التعديلات المعلّقة − المحذوفات. الكتابة محلية ثم تُرفع بـFamWriter. */
class People(private val cache: FamCache) {

    private class Snap(val ver: Long, val list: List<JSONObject>)
    private val memo = java.util.concurrent.ConcurrentHashMap<String, Snap>()

    /** نسخة سطحية لكل سجل: التحليل (فك JSON) يحدث مرة واحدة لكل تغيّر، والمستدعي حرّ في تعديل نسخته. */
    fun persons(camp: String): List<JSONObject> {
        val v = cache.version(camp)
        val snap = memo[camp]?.takeIf { it.ver == v } ?: Snap(v, build(camp)).also { memo[camp] = it }
        return snap.list.map { src -> JSONObject().also { o -> for (k in src.keys()) o.put(k, src.opt(k)) } }
    }

    /** للقراءة فقط (إحصاءات): نفس الكائنات المخزّنة بلا نسخ — ممنوع تعديلها. */
    internal fun readOnly(camp: String): List<JSONObject> {
        val v = cache.version(camp)
        return (memo[camp]?.takeIf { it.ver == v } ?: Snap(v, build(camp)).also { memo[camp] = it }).list
    }

    private fun build(camp: String): List<JSONObject> {
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

    /** استعادة سجل من نسخة احتياطية دون تغيير وقت تعديله: الأحدث يفوز عند الدمج. يعيد true إن أُضيف. */
    fun restore(camp: String, p: JSONObject, current: Map<String, JSONObject>): Boolean {
        val id = p.optString("id"); if (id.isEmpty()) return false
        val ex = current[id]
        if (ex != null && Merge.time(ex) >= Merge.time(p)) return false
        val o = Merge.copy(p).put("campId", camp)
        cache.putPend(camp, id, Merge.personDocId(o), o.toString(), Merge.time(o))
        return true
    }

    fun delete(camp: String, p: JSONObject) {
        cache.putTomb(camp, p.getString("id"), Merge.personDocId(p), Clock.now())
    }

    fun pendingCount(camp: String) = cache.pending(camp).size + cache.tombs(camp).size
}
