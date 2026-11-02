package dev.saged.grcs.data

import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.auth.Http
import dev.saged.grcs.config.Backend
import org.json.JSONArray
import org.json.JSONObject

class AuditEntry(val action: String, val userName: String, val targetName: String, val at: Long, val detail: String)

/** سجل النشاط في camps/{camp}/activityLogs بنفس حقول الويب؛ فشل الكتابة لا يمنع العملية. */
object Audit {
    val labels = mapOf(
        "login" to "تسجيل دخول", "import_excel" to "استيراد", "edit_person" to "تعديل فرد", "add_person" to "إضافة فرد", "delete_person" to "حذف فرد",
        "export" to "تصدير", "create_user" to "إنشاء حساب", "update_user" to "تعديل حساب", "camp_info" to "تحديث معلومات المخيم", "restore_backup" to "استعادة نسخة احتياطية",
        "aid_give" to "تسليم مساعدة", "aid_undo" to "تراجع عن مساعدة", "restore_trash" to "استعادة من السلة", "purge_trash" to "حذف نهائي من السلة", "transfer_family" to "نقل أسرة", "family_transfer" to "نقل أسرة", "clear_camp" to "إفراغ بيانات المخيم", "delete_camp" to "حذف مخيم",
    )

    fun log(st: AuthState, camp: String, action: String, targetId: String, targetName: String, detail: String? = null) {
        if (camp.isEmpty() || camp == "HQ" || st.user.role == "beneficiary") return
        runCatching {
            val id = java.util.UUID.randomUUID().toString()
            Fs.set("camps/$camp/activityLogs/$id", mapOf("id" to id, "action" to action, "userId" to st.user.id, "userName" to st.user.fullName, "campId" to camp,
                "targetId" to targetId, "targetName" to targetName, "oldValue" to null, "newValue" to detail, "timestamp" to System.currentTimeMillis(), "device" to "grcs-android"), st.idToken)
        }
    }

    fun list(st: AuthState, camp: String, limit: Int = 100): List<AuditEntry> {
        val sq = JSONObject().put("from", JSONArray().put(JSONObject().put("collectionId", "activityLogs")))
            .put("orderBy", JSONArray().put(JSONObject().put("field", JSONObject().put("fieldPath", "timestamp")).put("direction", "DESCENDING"))).put("limit", limit)
        val r = Http.postJson("${Backend.FIRESTORE}/camps/$camp:runQuery", JSONObject().put("structuredQuery", sq), st.idToken, 30_000)
        if (r.code !in 200..299) throw FsException(r.code)
        val arr = r.array ?: return emptyList()
        Usage.read(maxOf(1, arr.length()))
        fun s(f: JSONObject, k: String) = f.optJSONObject(k)?.optString("stringValue").orEmpty()
        return (0 until arr.length()).mapNotNull { arr.getJSONObject(it).optJSONObject("document")?.optJSONObject("fields") }.map { f ->
            AuditEntry(s(f, "action"), s(f, "userName"), s(f, "targetName"), f.optJSONObject("timestamp")?.optString("integerValue")?.toLongOrNull() ?: 0L, s(f, "newValue"))
        }
    }
}
