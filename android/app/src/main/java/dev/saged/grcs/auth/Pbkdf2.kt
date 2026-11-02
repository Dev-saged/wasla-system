package dev.saged.grcs.auth

import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** PBKDF2-HMAC-SHA256 يدوي ليعمل على API 23 (المكتبة الجاهزة تبدأ من 26). مطابق لنسخة الويب. */
object Pbkdf2 {
    private const val ITER = 50_000

    fun hash(password: String, saltB64: String): String {
        val salt = Base64.decode(saltB64, Base64.DEFAULT)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(password.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        mac.update(salt)
        mac.update(byteArrayOf(0, 0, 0, 1))
        var u = mac.doFinal()
        val t = u.copyOf()
        for (i in 1 until ITER) {
            u = mac.doFinal(u)
            for (j in t.indices) t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
        }
        return t.joinToString("") { "%02x".format(it) }
    }
}
