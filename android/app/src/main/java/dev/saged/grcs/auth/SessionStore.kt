package dev.saged.grcs.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** تخزين الجلسة مشفّراً بمفتاح AES-GCM داخل Android Keystore؛ لا تُحفظ كلمة المرور بل البصمة فقط. */
class SessionStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("grcs_session", Context.MODE_PRIVATE)

    fun save(s: Session) {
        val raw = org.json.JSONObject().put("u", s.username).put("h", s.hash).put("user", s.userJson).toString()
        prefs.edit().putString(KEY, seal(raw)).apply()
    }

    fun load(): Session? = runCatching {
        val enc = prefs.getString(KEY, null) ?: return null
        val o = org.json.JSONObject(open(enc))
        Session(o.getString("u"), o.getString("h"), o.getString("user"))
    }.getOrNull()

    fun clear() = prefs.edit().remove(KEY).apply()

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        g.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build(),
        )
        return g.generateKey()
    }

    private fun seal(plain: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val out = c.iv + c.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun open(b64: String): String {
        val all = Base64.decode(b64, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
            .apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, all, 0, 12)) }
        return String(c.doFinal(all, 12, all.size - 12), Charsets.UTF_8)
    }

    private companion object {
        const val KEY = "s"
        const val ALIAS = "grcs_session_key"
    }
}

class Session(val username: String, val hash: String, val userJson: String)
