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
}
