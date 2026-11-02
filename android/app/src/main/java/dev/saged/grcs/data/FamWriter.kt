package dev.saged.grcs.data

import org.json.JSONArray
import org.json.JSONObject

/** رفع التعديلات المعلّقة: يقرأ المستند البعيد، يدمج، ثم يكتب بشرط عدم تغيّره (يعيد المحاولة عند التعارض). */
class FamWriter(private val cache: FamCache) {

    /** يعيد true إن رُفع كل شيء. يرمي FsException عند الأخطاء غير المتوقعة (انقطاع/صلاحيات). */
    fun push(camp: String, token: String, userId: String): Boolean {
        val pend0 = cache.pending(camp); val tombs0 = cache.tombs(camp)
        if (pend0.isEmpty() && tombs0.isEmpty()) return true
        val cleared = cache.meta("cleared_$camp")?.toLongOrNull() ?: 0L

        val docOf = HashMap<String, String>()
        for ((id, j, _) in cache.docRows(camp)) {
            val arr = runCatching { JSONArray(j) }.getOrNull() ?: continue
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.optString("id")?.takeIf { it.isNotEmpty() }?.let { docOf[it] = id }
        }
        val local = LinkedHashMap<String, JSONObject>()
        val pendSnap = HashMap<String, Long>()
        for (p in pend0) { local[p.id] = JSONObject(p.json); pendSnap[p.id] = p.upd }
        val tombList = tombs0.map { CacheTomb(it.id, it.doc, it.at) }

        val dirty = LinkedHashSet<String>()
        for (lp in local.values) { dirty += Merge.personDocId(lp); docOf[lp.optString("id")]?.let { dirty += it } }
        for (t in tombList) (t.doc.ifEmpty { docOf[t.id] })?.let { if (it.isNotEmpty()) dirty += it }

        var allOk = true
        for (chunk in dirty.toList().chunked(100)) {
            var todo = chunk
            var attempt = 0
            while (todo.isNotEmpty() && attempt < 4) {
                attempt++
                val remote = Fs.famBatchGet(camp, todo, token)
                val now = Clock.now()
                val puts = ArrayList<Fs.Put>(); val built = HashMap<String, Merge.Result>()
                for (docId in todo) {
                    val r = remote[docId]
                    val rp = r?.let { runCatching { JSONArray(it.j) }.getOrNull() }?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } } ?: emptyList()
                    val rd = r?.d?.takeIf { it.isNotBlank() }?.let { runCatching { JSONObject(it) }.getOrNull() } ?: JSONObject()
                    val res = Merge.mergeDoc(docId, rp, rd, local, tombList, docOf, cleared, now)
                    if (r == null && res.persons.isEmpty()) {
                        for (lp in local.values) if (Merge.personDocId(lp) == docId) cache.donePend(camp, lp.optString("id"), pendSnap[lp.optString("id")] ?: -1)
                        for (t in tombList) if ((t.doc.ifEmpty { docOf[t.id] }) == docId) cache.doneTomb(camp, t.id, t.at)
                        continue
                    }
                    val fid = res.persons.firstOrNull { it.optString("familyId").isNotEmpty() }?.optString("familyId") ?: r?.fid.orEmpty()
                    puts += Fs.Put(docId, camp, fid, res.persons.size, JSONArray(res.persons).toString(), res.del.toString(), r?.f.orEmpty(), userId, r?.updateTime)
                    built[docId] = res
                }
                if (puts.isEmpty()) break
                val retry = ArrayList<String>()
                val done = HashMap<String, String>()
                try {
                    done += Fs.famCommit(puts, token)
                } catch (e: FsException) {
                    if (e.status !in listOf(400, 403, 404, 409)) throw e
                    for (p in puts) try { done += Fs.famCommit(listOf(p), token) } catch (e2: FsException) {
                        if (e2.status !in listOf(400, 403, 404, 409)) throw e2
                        if (e2.status == 403) allOk = false else retry += p.docId
                    }
                }
                for ((docId, ut) in done) {
                    val res = built[docId] ?: continue
                    val put = puts.first { it.docId == docId }
                    cache.put(camp, listOf(FamDoc(docId, "", put.j, put.d, put.f, put.fid, ut, ut)))
                    for (lp in local.values) if (Merge.personDocId(lp) == docId) cache.donePend(camp, lp.optString("id"), pendSnap[lp.optString("id")] ?: -1)
                    for (t in tombList) if (res.del.has(t.id) || (t.doc.ifEmpty { docOf[t.id] }) == docId) cache.doneTomb(camp, t.id, t.at)
                }
                todo = retry
            }
            if (todo.isNotEmpty()) allOk = false
        }
        return allOk
    }
}
