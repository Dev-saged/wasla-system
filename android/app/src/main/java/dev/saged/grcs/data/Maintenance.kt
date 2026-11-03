package dev.saged.grcs.data

import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.auth.Http
import dev.saged.grcs.config.Backend
import org.json.JSONArray
import org.json.JSONObject

/** صيانة الفهرس الوطني (cleanOrphanedNationalIndex): حذف مداخل nationalIds العائدة لمخيم لم يعد موجوداً. */
object Maintenance {
    /** يعيد عدد المداخل اليتيمة المحذوفة أو -1 عند الفشل. */
    fun cleanNationalIndex(st: AuthState, campIds: Set<String>): Int {
        if (st.user.role != "admin" || st.offline) return -1
        return try {
            val sq = JSONObject().put("from", JSONArray().put(JSONObject().put("collectionId", "nationalIds")))
                .put("select", JSONObject().put("fields", JSONArray().put(JSONObject().put("fieldPath", "campId")))).put("limit", 20000)
            val r = Http.postJson("${Backend.FIRESTORE}:runQuery", JSONObject().put("structuredQuery", sq), st.idToken, 60_000)
            if (r.code !in 200..299) return -1
            val arr = r.array ?: return 0
            Usage.read(maxOf(1, arr.length()))
            val orphans = (0 until arr.length()).mapNotNull { arr.getJSONObject(it).optJSONObject("document") }.filter { d ->
                val c = d.optJSONObject("fields")?.optJSONObject("campId")?.optString("stringValue").orEmpty()
                c.isNotEmpty() && c !in campIds
            }.map { "nationalIds/" + it.getString("name").substringAfterLast('/') }
            if (orphans.isNotEmpty()) Fs.deleteMany(orphans, st.idToken)
            orphans.size
        } catch (e: Exception) { -1 }
    }
}
