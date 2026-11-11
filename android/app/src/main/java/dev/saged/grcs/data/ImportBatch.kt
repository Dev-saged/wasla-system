package dev.saged.grcs.data

import dev.saged.grcs.auth.AuthState
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class BatchRow(val k: String, val n: String, val h: Int)
class BatchRec(val o: JSONObject) {
    val id: String get() = o.optString("id"); val campId: String get() = o.optString("campId"); val mode: String get() = o.optString("mode")
    val fileName: String get() = o.optString("fileName"); val at: Long get() = o.optLong("at"); val committedAt: Long get() = o.optLong("committedAt")
    val userName: String get() = o.optString("userName"); val undone: Boolean get() = o.optBoolean("undone")
    val added: Int get() = o.optJSONObject("counts")?.optInt("added") ?: 0; val updated: Int get() = o.optJSONObject("counts")?.optInt("updated") ?: 0
    val rows: List<BatchRow> get() = o.optJSONArray("rows").let { a -> if (a == null) emptyList() else (0 until a.length()).map { a.getJSONObject(it).let { r -> BatchRow(r.optString("k"), r.optString("n"), r.optInt("h")) } } }
}
class BatchDiff(val added: List<BatchRow>, val removed: List<BatchRow>, val changed: List<BatchRow>)
class UndoResult(val removed: Int, val restored: Int, val skipped: List<String>)

/** دفعات الاستيراد (ImportBatch): لقطة ما قبل الكتابة + ما أُنشئ ← تراجع كامل ومقارنة تاريخية. آخر 12 دفعة لكل مخيم. */
object ImportBatch {
    lateinit var dir: File
    val modeLabels = mapOf("flat" to "صف = فرد", "horizontal" to "شجرة عائلة", "family_summary" to "أسرة مختصرة", "native" to "ملف النظام")

    private fun campDir(camp: String) = File(dir, camp).apply { mkdirs() }

    /** rowSig. */
    fun sig(r: JSONObject): BatchRow {
        val k = Nid.norm(r.optString("nationalId")).ifEmpty { r.optString("familyId") + "|" + Cells.nameKey(r.optString("fullName")) }
        fun s(x: String) = r.optString(x).takeIf { it != "null" }.orEmpty()
        val h = listOf(Cells.nameKey(r.optString("fullName")), s("birthDate"), s("gender"), s("phone"), s("address"), s("maritalStatus"), if (r.optBoolean("hasChronicDisease")) "1" else "0", if (r.optBoolean("hasDisability")) "1" else "0").joinToString("|").hashCode()
        return BatchRow(k, r.optString("fullName"), h)
    }

    /** يسجّل دفعة بعد الكتابة: pre = نسخ السجلات قبل تحديثها، written = كل ما كُتب، rows = صفوف الملف كسجلات. */
    fun commit(st: AuthState, camp: String, mode: String, fileName: String, at: Long, pre: List<JSONObject>, written: List<JSONObject>, rows: List<JSONObject>): String? = runCatching {
        val preIds = pre.map { it.optString("id") }.toSet()
        val created = written.filter { it.optString("id") !in preIds }
        val id = java.util.UUID.randomUUID().toString()
        val rec = JSONObject().put("id", id).put("campId", camp).put("mode", mode).put("fileName", fileName).put("at", at).put("committedAt", System.currentTimeMillis())
            .put("userId", st.user.id).put("userName", st.user.fullName.ifEmpty { st.user.username })
            .put("created", JSONArray(created.map { JSONObject().put("id", it.optString("id")).put("nid", it.optString("nationalId")).put("fid", it.optString("familyId")) }))
            .put("pre", JSONArray(pre))
            .put("rows", JSONArray(rows.filter { it.optString("fullName").isNotEmpty() }.map { sig(it).let { s -> JSONObject().put("k", s.k).put("n", s.n).put("h", s.h) } }))
            .put("counts", JSONObject().put("added", created.size).put("updated", pre.size))
        File(campDir(camp), "$id.json").writeText(rec.toString())
        latest(camp).drop(12).forEach { File(campDir(camp), it.id + ".json").delete() }
        id
    }.getOrNull()

    fun latest(camp: String): List<BatchRec> = campDir(camp).listFiles { f -> f.name.endsWith(".json") }.orEmpty()
        .mapNotNull { runCatching { BatchRec(JSONObject(it.readText())) }.getOrNull() }.sortedByDescending { it.at }

    fun canUndo(rec: BatchRec, list: List<BatchRec>) = !rec.undone && list.firstOrNull()?.id == rec.id && System.currentTimeMillis() - rec.committedAt < 7 * 86400000L

    /** يحذف ما أضافته الدفعة ويعيد ما حدّثته؛ السجلات المعدّلة بعد الاستيراد تُترك. يرمي رسالة عربية عند المنع. */
    fun undo(st: AuthState, data: DataRepo, camp: String, id: String): UndoResult {
        val list = latest(camp)
        val rec = list.firstOrNull { it.id == id } ?: throw IllegalStateException("سجل الدفعة غير موجود")
        if (!canUndo(rec, list)) throw IllegalStateException("يمكن التراجع عن آخر استيراد فقط خلال 7 أيام")
        val cut = rec.committedAt + 2000; val skipped = ArrayList<String>()
        val cur = data.people.persons(camp).associateBy { it.optString("id") }
        val created = rec.o.optJSONArray("created") ?: JSONArray()
        var removed = 0
        for (i in 0 until created.length()) {
            val c = cur[created.getJSONObject(i).optString("id")] ?: continue
            if (c.optLong("updatedAt") > cut) { skipped += c.optString("fullName"); continue }
            data.people.delete(camp, c); data.afterSave(st, camp, c, true); removed++
        }
        val pre = rec.o.optJSONArray("pre") ?: JSONArray()
        var restored = 0
        for (i in 0 until pre.length()) {
            val p = pre.getJSONObject(i)
            val c = cur[p.optString("id")]
            if (c != null && c.optLong("updatedAt") > cut) { skipped += c.optString("fullName"); continue }
            data.afterSave(st, camp, data.people.save(camp, JSONObject(p.toString()).put("updatedBy", st.user.id)), false); restored++
        }
        rec.o.put("undone", true).put("undoneAt", System.currentTimeMillis())
        File(campDir(camp), "$id.json").writeText(rec.o.toString())
        data.pushNow(st, camp)
        runCatching { Audit.log(st, camp, "undo_import", camp, rec.fileName, JSONObject().put("batch", id).put("file", rec.fileName).put("removed", removed).put("restored", restored).put("skipped", skipped.size).toString()) }
        return UndoResult(removed, restored, skipped)
    }

    fun diff(a: BatchRec, b: BatchRec): BatchDiff {
        val ma = a.rows.associateBy { it.k }; val mb = b.rows.associateBy { it.k }
        return BatchDiff(mb.values.filter { it.k !in ma }, ma.values.filter { it.k !in mb }, mb.values.filter { r -> ma[r.k]?.let { it.h != r.h } == true })
    }
}
