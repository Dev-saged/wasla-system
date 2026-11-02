package dev.saged.grcs.data

import android.util.Base64
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.auth.Http
import dev.saged.grcs.config.Backend
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class TrashItem(val tid: String, val camp: String, val kind: String, val label: String, val persons: Int, val users: Int, val at: Long, val byName: String, val parts: Int)

/** سلة المحذوفات: نفس مستندات الويب `trash/{tid}` (gzip + base64 مقسّم 800KB)، فما يُحذف هنا يُستعاد من الويب والعكس. */
class Trash(private val repo: DataRepo) {
    companion object { const val PART = 800_000; const val TTL = 30 * 86_400_000L
        private fun doc(path: String) = "projects/${Backend.PROJECT_ID}/databases/(default)/documents/$path" }

    private fun paths(tid: String, parts: Int) = (0 until maxOf(parts, 1)).map { if (it == 0) "trash/$tid" else "trash/${tid}__p$it" }

    fun canUse(st: AuthState) = st.user.role == "admin" || st.user.role == "delegate"

    /** حفظ نسخة قبل الحذف؛ فشلها لا يمنع الحذف. */
    fun put(st: AuthState, camp: String, kind: String, label: String, persons: List<JSONObject>): Boolean {
        if (!canUse(st) || persons.isEmpty()) return false
        return runCatching {
            val payload = JSONObject().put("persons", JSONArray(persons)).put("families", JSONArray()).put("users", JSONArray())
            val bo = ByteArrayOutputStream(); GZIPOutputStream(bo).use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val b64 = Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP)
            val chunks = b64.chunked(PART).ifEmpty { listOf("") }
            val id = java.util.UUID.randomUUID().toString(); val at = System.currentTimeMillis()
            val docs = chunks.mapIndexed { i, data ->
                paths(id, chunks.size)[i] to mapOf<String, Any?>("tid" to id, "campId" to camp.ifEmpty { "HQ" }, "kind" to kind, "label" to label.take(160),
                    "persons" to persons.size, "users" to 0, "at" to at, "by" to st.user.id, "byName" to st.user.fullName, "expiresAt" to at + TTL,
                    "parts" to chunks.size, "part" to i, "data" to data)
            }
            Fs.setMany(docs, st.idToken); true
        }.getOrDefault(false)
    }

    private fun intOf(f: JSONObject, k: String) = f.optJSONObject(k)?.optString("integerValue")?.toLongOrNull() ?: 0L
    private fun strOf(f: JSONObject, k: String) = f.optJSONObject(k)?.optString("stringValue").orEmpty()

    /** العناصر (الجزء الأول فقط ودون حقل data). الأدمن يرى الكل والمندوب مخيمه. */
    fun list(st: AuthState): List<TrashItem> {
        val sq = JSONObject().put("from", JSONArray().put(JSONObject().put("collectionId", "trash"))).put("limit", 300)
            .put("select", JSONObject().put("fields", JSONArray(listOf("tid", "campId", "kind", "label", "persons", "users", "at", "byName", "expiresAt", "parts", "part").map { JSONObject().put("fieldPath", it) })))
        if (st.user.role != "admin") sq.put("where", JSONObject().put("fieldFilter", JSONObject().put("field", JSONObject().put("fieldPath", "campId")).put("op", "EQUAL").put("value", Fs.enc(st.user.campId))))
        val r = Http.postJson("${Backend.FIRESTORE}:runQuery", JSONObject().put("structuredQuery", sq), st.idToken, 30_000)
        if (r.code !in 200..299) throw FsException(r.code)
        val arr = r.array ?: return emptyList()
        Usage.read(maxOf(1, arr.length()))
        val now = System.currentTimeMillis(); val out = ArrayList<TrashItem>(); val expired = ArrayList<TrashItem>()
        for (i in 0 until arr.length()) {
            val f = arr.getJSONObject(i).optJSONObject("document")?.optJSONObject("fields") ?: continue
            if (intOf(f, "part") != 0L) continue
            val it = TrashItem(strOf(f, "tid"), strOf(f, "campId"), strOf(f, "kind"), strOf(f, "label"), intOf(f, "persons").toInt(), intOf(f, "users").toInt(), intOf(f, "at"), strOf(f, "byName"), intOf(f, "parts").toInt())
            if (intOf(f, "expiresAt") < now) expired += it else out += it
        }
        if (expired.isNotEmpty()) runCatching { purge(st, expired) }
        return out.sortedByDescending { it.at }
    }

    fun purge(st: AuthState, items: List<TrashItem>) {
        val w = JSONArray()
        items.flatMap { paths(it.tid, it.parts) }.forEach { w.put(JSONObject().put("delete", doc(it))) }
        if (w.length() == 0) return
        val r = Http.postJson("${Backend.FIRESTORE}:commit", JSONObject().put("writes", w), st.idToken, 30_000)
        Usage.write(w.length())
        if (r.code !in 200..299) throw FsException(r.code)
    }

    private fun load(st: AuthState, it: TrashItem): JSONObject {
        val ps = paths(it.tid, it.parts)
        val r = Http.postJson("${Backend.FIRESTORE}:batchGet", JSONObject().put("documents", JSONArray(ps.map { p -> doc(p) })), st.idToken, 40_000)
        if (r.code !in 200..299) throw FsException(r.code)
        val got = HashMap<String, String>()
        val arr = r.array ?: JSONArray()
        Usage.read(maxOf(1, arr.length()))
        for (i in 0 until arr.length()) {
            val d = arr.getJSONObject(i).optJSONObject("found") ?: continue
            got[d.getString("name")] = strOf(d.optJSONObject("fields") ?: JSONObject(), "data")
        }
        val b64 = ps.joinToString("") { p -> got[doc(p)] ?: throw IllegalStateException("جزء مفقود") }
        val bytes = GZIPInputStream(Base64.decode(b64, Base64.DEFAULT).inputStream()).use { s -> s.readBytes() }
        return JSONObject(String(bytes, Charsets.UTF_8))
    }

    /** استعادة الأفراد بختم جديد ليتغلبوا على الحذف السابق؛ ثم حذف نسخة السلة. يعيد عدد المستعادين. */
    fun restore(st: AuthState, it: TrashItem): Int {
        val payload = load(st, it)
        val ps = payload.optJSONArray("persons") ?: JSONArray()
        val camp = it.camp
        var n = 0
        for (i in 0 until ps.length()) {
            val p = ps.getJSONObject(i)
            val saved = repo.people.save(camp, p); n++
            if (saved.optBoolean("isFamilyHead")) runCatching { repo.afterSave(st, camp, saved, false) }
        }
        repo.pushNow(st, camp)
        runCatching { purge(st, listOf(it)) }
        return n
    }
}
