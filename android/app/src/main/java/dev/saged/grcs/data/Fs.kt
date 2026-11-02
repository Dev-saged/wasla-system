package dev.saged.grcs.data

import dev.saged.grcs.auth.Http
import dev.saged.grcs.config.Backend
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class FsException(val status: Int, val reason: String = "") : Exception("firestore $status $reason")

class FamDoc(val id: String, val name: String, val j: String, val d: String, val f: String, val fid: String, val u: String, val updateTime: String = "")

/** قراءة Firestore عبر REST برمز الدخول؛ القواعد على الخادم هي التي تفرض الصلاحيات. */
object Fs {
    private fun str(f: JSONObject, k: String) = f.optJSONObject(k)?.optString("stringValue").orEmpty()

    fun get(path: String, token: String): JSONObject? {
        val r = Http.get("${Backend.FIRESTORE}/$path", token)
        if (r.code == 404) return null
        if (r.code !in 200..299) throw FsException(r.code)
        return r.json?.optJSONObject("fields")
    }

    fun intField(f: JSONObject?, k: String): Long =
        f?.optJSONObject(k)?.let { it.optString("integerValue").toLongOrNull() ?: it.optDouble("doubleValue", 0.0).toLong() } ?: 0L

    fun stringField(f: JSONObject?, k: String) = if (f == null) "" else str(f, k)

    /** قائمة المخيمات: (المعرّف، الاسم). */
    fun listCamps(token: String): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        var page = ""
        do {
            val url = "${Backend.FIRESTORE}/camps?pageSize=100" + if (page.isEmpty()) "" else "&pageToken=${URLEncoder.encode(page, "UTF-8")}"
            val r = Http.get(url, token)
            if (r.code !in 200..299) throw FsException(r.code)
            val docs = r.json?.optJSONArray("documents") ?: JSONArray()
            for (i in 0 until docs.length()) {
                val d = docs.getJSONObject(i)
                val id = d.getString("name").substringAfterLast('/')
                out += id to str(d.optJSONObject("fields") ?: JSONObject(), "name").ifBlank { id }
            }
            page = r.json?.optString("nextPageToken").orEmpty()
        } while (page.isNotEmpty())
        return out
    }

    /** صفحة من مستندات الأسر مرتبة بوقت الخادم u ثم الاسم (نفس استعلام الويب). */
    fun famPage(campId: String, sinceIso: String?, after: FamDoc?, token: String, limit: Int = 300): List<FamDoc> {
        val sq = JSONObject()
            .put("from", JSONArray().put(JSONObject().put("collectionId", "fam")))
            .put("orderBy", JSONArray()
                .put(JSONObject().put("field", JSONObject().put("fieldPath", "u")).put("direction", "ASCENDING"))
                .put(JSONObject().put("field", JSONObject().put("fieldPath", "__name__")).put("direction", "ASCENDING")))
            .put("limit", limit)
        if (sinceIso != null) sq.put("where", JSONObject().put("fieldFilter", JSONObject()
            .put("field", JSONObject().put("fieldPath", "u")).put("op", "GREATER_THAN_OR_EQUAL")
            .put("value", JSONObject().put("timestampValue", sinceIso))))
        if (after != null) sq.put("startAt", JSONObject().put("before", false).put("values", JSONArray()
            .put(JSONObject().put("timestampValue", after.u)).put(JSONObject().put("referenceValue", after.name))))
        val t0 = System.currentTimeMillis()
        val r = Http.postJson("${Backend.FIRESTORE}/camps/${URLEncoder.encode(campId, "UTF-8")}:runQuery",
            JSONObject().put("structuredQuery", sq), token, 40_000)
        if (r.code !in 200..299) throw FsException(r.code)
        val arr = r.array ?: return emptyList()
        arr.optJSONObject(0)?.optString("readTime")?.takeIf { it.isNotEmpty() }?.let { Clock.sample(it, t0, System.currentTimeMillis()) }
        val out = ArrayList<FamDoc>()
        for (i in 0 until arr.length()) {
            val doc = arr.getJSONObject(i).optJSONObject("document") ?: continue
            val f = doc.optJSONObject("fields") ?: JSONObject()
            val name = doc.getString("name")
            out += FamDoc(name.substringAfterLast('/'), name, str(f, "j"), str(f, "d"), str(f, "f"), str(f, "fid"),
                f.optJSONObject("u")?.optString("timestampValue").orEmpty())
        }
        return out
    }

    private fun docName(campId: String, id: String) = "projects/${Backend.PROJECT_ID}/databases/(default)/documents/camps/$campId/fam/$id"

    /** قراءة مستندات أسر محددة؛ المفقود غير موجود في الخريطة. */
    fun famBatchGet(campId: String, ids: List<String>, token: String): Map<String, FamDoc> {
        if (ids.isEmpty()) return emptyMap()
        val r = Http.postJson("${Backend.FIRESTORE}:batchGet",
            JSONObject().put("documents", JSONArray(ids.map { docName(campId, it) })), token, 40_000)
        if (r.code !in 200..299) throw FsException(r.code)
        val out = HashMap<String, FamDoc>()
        val arr = r.array ?: return out
        for (i in 0 until arr.length()) {
            val doc = arr.getJSONObject(i).optJSONObject("found") ?: continue
            val f = doc.optJSONObject("fields") ?: JSONObject()
            val name = doc.getString("name")
            val id = name.substringAfterLast('/')
            out[id] = FamDoc(id, name, str(f, "j"), str(f, "d"), str(f, "f"), str(f, "fid"),
                f.optJSONObject("u")?.optString("timestampValue").orEmpty(), doc.optString("updateTime"))
        }
        return out
    }

    class Put(val docId: String, val campId: String, val fid: String, val n: Int, val j: String, val d: String, val f: String, val by: String, val prevUpdateTime: String?)

    /** كتابة مستندات أسر بشرط عدم تغيّر المستند منذ قراءته (يمنع الكتابة فوق تعديل زميل). يعيد وقت التحديث لكل مستند ناجح. */
    fun famCommit(puts: List<Put>, token: String): Map<String, String> {
        val writes = JSONArray()
        for (p in puts) {
            fun sv(x: String) = JSONObject().put("stringValue", x)
            val fields = JSONObject().put("campId", sv(p.campId)).put("fid", sv(p.fid))
                .put("n", JSONObject().put("integerValue", p.n.toString()))
                .put("j", sv(p.j)).put("d", sv(p.d)).put("f", sv(p.f)).put("by", sv(p.by))
            writes.put(JSONObject()
                .put("update", JSONObject().put("name", docName(p.campId, p.docId)).put("fields", fields))
                .put("updateTransforms", JSONArray().put(JSONObject().put("fieldPath", "u").put("setToServerValue", "REQUEST_TIME")))
                .put("currentDocument", if (p.prevUpdateTime != null) JSONObject().put("updateTime", p.prevUpdateTime) else JSONObject().put("exists", false)))
        }
        val r = Http.postJson("${Backend.FIRESTORE}:commit", JSONObject().put("writes", writes), token, 60_000)
        if (r.code !in 200..299) throw FsException(r.code, r.json?.optJSONObject("error")?.optString("status").orEmpty())
        val res = r.json?.optJSONArray("writeResults") ?: JSONArray()
        val out = HashMap<String, String>()
        puts.forEachIndexed { i, p -> out[p.docId] = res.optJSONObject(i)?.optString("updateTime").orEmpty() }
        return out
    }
}
