package dev.saged.grcs.auth

import android.util.Base64
import dev.saged.grcs.config.Backend
import org.json.JSONObject

/** بيانات المستخدم كما يُرجعها خادم الدخول، مع صلاحيات الرمز. */
class AuthUser(val json: JSONObject) {
    val id: String get() = json.optString("id")
    val username: String get() = json.optString("username")
    val fullName: String get() = json.optString("fullName").ifBlank { username }
    val role: String get() = json.optString("role")
    val campId: String get() = json.optString("campId")
}

class AuthState(val user: AuthUser, val idToken: String, val expiresAt: Long, val offline: Boolean)

class AuthException(val code: String, message: String) : Exception(message)

/** الدخول عبر خادم الدخول الحالي (نفس حسابات الويب والنسخة السابقة) دون ترحيل أي حساب. */
class AuthRepo(private val store: SessionStore) {

    fun normUsername(s: String): String {
        val noHidden = s.replace(Regex("[​-‏‪-‮⁦-⁩﻿ ]"), "")
        val sb = StringBuilder()
        for (ch in noHidden) sb.append(
            when (ch) {
                in '٠'..'٩' -> ('0' + (ch - '٠'))
                in '۰'..'۹' -> ('0' + (ch - '۰'))
                else -> ch
            },
        )
        return sb.toString().trim()
    }

    /** تسجيل دخول بكلمة المرور. يرمي AuthException برسالة عربية جاهزة للعرض. */
    fun login(usernameRaw: String, password: String): AuthState {
        val uname = normUsername(usernameRaw)
        if (uname.isEmpty() || password.isEmpty()) throw AuthException("empty", "أدخل اسم المستخدم وكلمة المرور")
        try {
            val s = post("/auth/salt", JSONObject().put("username", uname))
            when (s.optString("error")) {
                "not_found" -> throw AuthException("not_found", "اسم المستخدم غير موجود")
                "unset" -> throw AuthException("unset", "الحساب بلا كلمة مرور بعد — فعّله من النسخة السابقة أو راجع المسؤول")
            }
            var salt = s.optString("salt")
            var hash = Pbkdf2.hash(password, salt)
            var r = post("/auth/login", JSONObject().put("username", uname).put("hash", hash))
            if (r.optString("error") == "bad_password" && r.optString("salt").let { it.isNotEmpty() && it != salt }) {
                salt = r.optString("salt")
                hash = Pbkdf2.hash(password, salt)
                r = post("/auth/login", JSONObject().put("username", uname).put("hash", hash))
            }
            fail(r)
            val st = finish(r)
            store.save(Session(uname, hash, st.user.json.toString()))
            return st
        } catch (e: AuthException) {
            throw e
        } catch (e: Exception) {
            throw AuthException("net", "تعذّر الوصول للخادم — تحقق من الاتصال وأعد المحاولة")
        }
    }

    /** استعادة جلسة محفوظة: تجديد الرمز عبر البصمة، أو الدخول دون اتصال بالنسخة المخزنة. */
    fun restore(): AuthState? {
        val s = store.load() ?: return null
        return try {
            val r = post("/auth/login", JSONObject().put("username", s.username).put("hash", s.hash))
            when (r.optString("error")) {
                "inactive", "not_found", "bad_password", "unset" -> { store.clear(); null }
                "" -> finish(r).also { store.save(Session(s.username, s.hash, it.user.json.toString())) }
                else -> offline(s)
            }
        } catch (e: Exception) {
            offline(s)
        }
    }

    fun logout() = store.clear()

    private fun offline(s: Session) = AuthState(AuthUser(JSONObject(s.userJson)), "", 0L, true)

    private fun fail(r: JSONObject) {
        when (r.optString("error")) {
            "" -> return
            "not_found" -> throw AuthException("not_found", "اسم المستخدم غير موجود")
            "bad_password" -> throw AuthException("bad_password", "كلمة المرور غير صحيحة")
            "inactive" -> throw AuthException("inactive", "الحساب معطّل — راجع مسؤول النظام")
            "locked" -> throw AuthException("locked", "محاولات خاطئة كثيرة — انتظر 10 دقائق ثم أعد المحاولة")
            "unset" -> throw AuthException("unset", "الحساب بلا كلمة مرور بعد — فعّله من النسخة السابقة أو راجع المسؤول")
            else -> throw AuthException("server", "خطأ من خادم الدخول")
        }
    }

    private fun finish(r: JSONObject): AuthState {
        val token = r.optString("token")
        if (token.isEmpty()) throw AuthException("server", "استجابة غير صالحة من خادم الدخول")
        val res = Http.postJson(Backend.IDENTITY, JSONObject().put("token", token).put("returnSecureToken", true))
        val id = res.json?.optString("idToken").orEmpty()
        if (res.code !in 200..299 || id.isEmpty()) throw AuthException("token", "تعذّر إصدار رمز الدخول")
        val ttl = res.json?.optString("expiresIn")?.toLongOrNull() ?: 3600L
        return AuthState(AuthUser(r.getJSONObject("user")), id, System.currentTimeMillis() + ttl * 1000, false)
    }

    private fun post(path: String, body: JSONObject): JSONObject {
        val res = Http.postJson(Backend.WORKER + path, body)
        return res.json ?: throw AuthException("server", "استجابة غير صالحة من خادم الدخول")
    }

    /** مطالبات الرمز (الدور/المخيم) للاستخدام لاحقاً مع قواعد Firestore. */
    fun claims(idToken: String): JSONObject? = runCatching {
        val p = idToken.split(".")[1]
        JSONObject(String(Base64.decode(p, Base64.URL_SAFE or Base64.NO_PADDING), Charsets.UTF_8))
    }.getOrNull()
}
