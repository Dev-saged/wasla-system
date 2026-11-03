package dev.saged.grcs.data

import android.util.Base64
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.auth.Pbkdf2
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/** حسابات الطاقم (مندوب، مدخل بيانات، إدارة) للأدمن فقط: عرض وتعطيل وإعادة تعيين كلمة المرور. */
object Staff {
    val roles = listOf("delegate" to "مندوب", "dataentry" to "مدخل بيانات", "admin" to "إدارة")

    fun list(st: AuthState): List<JSONObject> =
        roles.flatMap { (r, _) -> runCatching { Fs.queryEq("users", listOf("role" to r), st.idToken, 500) }.getOrDefault(emptyList()) }
            .sortedWith(compareBy({ it.optString("role") }, { it.optString("fullName") }))

    /** حسابات المستفيدين لمخيم واحد (تُنشأ تلقائياً لأرباب الأسر). */
    fun beneficiaries(st: AuthState, camp: String): List<JSONObject> =
        runCatching { Fs.queryEq("users", listOf("role" to "beneficiary", "campId" to camp), st.idToken, 1000) }.getOrDefault(emptyList()).sortedBy { it.optString("fullName") }

    fun setActive(st: AuthState, u: JSONObject, active: Boolean): Boolean {
        if (st.user.role != "admin" || u.getString("id") == st.user.id) return false
        return runCatching { Fs.patch("users/${u.getString("id")}", mapOf("isActive" to active), st.idToken); Fs.pulse("u", null, st.idToken) }.isSuccess
    }

    fun resetPassword(st: AuthState, u: JSONObject, pw: String): Boolean {
        if (st.user.role != "admin" || pw.length < 6) return false
        val salt = Base64.encodeToString(ByteArray(16).also { SecureRandom().nextBytes(it) }, Base64.NO_WRAP)
        val hash = Pbkdf2.hash(pw, salt)
        val ver = MessageDigest.getInstance("SHA-256").digest("grcs:v1:$hash".toByteArray()).joinToString("") { "%02x".format(it) }
        val id = u.getString("id"); val now = Clock.now()
        return runCatching {
            Fs.patch("users/$id", mapOf("salt" to salt, "verifier" to ver, "pwUpdatedAt" to now), st.idToken)
            Fs.set("creds/$id", mapOf("uid" to id, "pw" to pw, "username" to u.optString("username"), "role" to u.optString("role"),
                "campId" to u.optString("campId"), "fullName" to u.optString("fullName"), "updatedAt" to now, "by" to st.user.id), st.idToken)
            Fs.pulse("u", null, st.idToken)
        }.isSuccess
    }

    /** إنشاء حساب طاقم (كما createNewUser): يعيد null عند النجاح أو رسالة الخطأ. */
    fun create(st: AuthState, role: String, fullName: String, username: String, pw: String, phone: String, campId: String, campName: String): String? {
        if (st.user.role != "admin") return "للإدارة فقط"
        val un = username.trim().lowercase()
        if (fullName.isBlank() || un.isEmpty() || pw.length < 6) return "أكمل الاسم واسم المستخدم وكلمة مرور من 6 أحرف"
        if (role != "admin" && campId.isEmpty()) return "اختر المخيم"
        return try {
            if (Fs.queryEq("users", listOf("username" to un), st.idToken, 1).isNotEmpty()) return "اسم المستخدم مستخدم بالفعل"
            val id = java.util.UUID.randomUUID().toString()
            val salt = Base64.encodeToString(ByteArray(16).also { SecureRandom().nextBytes(it) }, Base64.NO_WRAP)
            val hash = Pbkdf2.hash(pw, salt)
            val ver = MessageDigest.getInstance("SHA-256").digest("grcs:v1:$hash".toByteArray()).joinToString("") { "%02x".format(it) }
            val now = Clock.now()
            val camp = if (role == "admin") "HQ" else campId
            val cname = if (role == "admin") "المركز الرئيسي" else campName
            Fs.set("users/$id", mapOf("id" to id, "username" to un, "salt" to salt, "verifier" to ver, "role" to role, "campId" to camp, "campName" to cname,
                "fullName" to fullName.trim(), "phone" to phone.trim(), "createdAt" to now, "isActive" to true, "pwUpdatedAt" to now), st.idToken)
            Fs.set("creds/$id", mapOf("uid" to id, "pw" to pw, "username" to un, "role" to role, "campId" to camp, "fullName" to fullName.trim(),
                "updatedAt" to now, "by" to st.user.id), st.idToken)
            runCatching { Fs.pulse("u", null, st.idToken) }
            null
        } catch (e: Exception) { "تعذّر الإنشاء — تحقق من الاتصال" }
    }
    /** تعديل حساب (openEditUserModal): يعيد null عند النجاح أو رسالة الخطأ. */
    fun update(st: AuthState, u: JSONObject, fullName: String, username: String, phone: String, role: String, campId: String, campName: String): String? {
        if (st.user.role != "admin") return "للإدارة فقط"
        val un = username.trim().lowercase()
        if (fullName.isBlank() || un.isEmpty()) return "الاسم واسم المستخدم مطلوبان"
        if (role != "admin" && campId.isEmpty()) return "اختر المخيم"
        val id = u.getString("id")
        return try {
            if (un != u.optString("username") && Fs.queryEq("users", listOf("username" to un), st.idToken, 2).any { it.optString("id") != id }) return "اسم المستخدم مستخدم بالفعل"
            val camp = if (role == "admin") "HQ" else campId
            Fs.patch("users/$id", mapOf("fullName" to fullName.trim(), "username" to un, "phone" to phone.trim(), "role" to role, "campId" to camp,
                "campName" to if (role == "admin") "المركز الرئيسي" else campName), st.idToken)
            Audit.log(st, camp, "update_user", id, fullName.trim())
            runCatching { Fs.pulse("u", null, st.idToken) }
            null
        } catch (e: Exception) { "تعذّر الحفظ" }
    }

    /** حذف حساب (deleteUserAccount): طاقم المخيم يحذف معه حسابات مستفيديه. يعيد عدد المستفيدين المحذوفين أو -1 عند الفشل. */
    fun delete(st: AuthState, u: JSONObject): Int {
        val id = u.getString("id")
        if (st.user.role != "admin" || id == st.user.id) return -1
        return try {
            val staff = u.optString("role") in listOf("delegate", "dataentry") && u.optString("campId").isNotEmpty()
            val bens = if (staff) beneficiaries(st, u.optString("campId")) else emptyList()
            val paths = (listOf(id) + bens.map { it.getString("id") }).flatMap { listOf("users/$it", "creds/$it") }
            Fs.deleteMany(paths, st.idToken)
            Audit.log(st, u.optString("campId"), "update_user", id, u.optString("fullName"), "deleted")
            runCatching { Fs.pulse("u", null, st.idToken) }
            bens.size
        } catch (e: Exception) { -1 }
    }

    /** كلمة المرور المحفوظة للعرض عند الأدمن (creds/{id}) أو null إن لم تُفعَّل. */
    fun password(st: AuthState, id: String): String? = runCatching { Fs.get("creds/$id", st.idToken)?.let { Fs.stringField(it, "pw") }?.takeIf { it.isNotEmpty() } }.getOrNull()
}
