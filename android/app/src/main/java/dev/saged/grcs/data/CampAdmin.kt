package dev.saged.grcs.data

import dev.saged.grcs.auth.AuthState
import org.json.JSONObject

/** معلومات المخيم كما في شاشة الويب screen-camp-info. */
class CampInfo(
    val id: String, val name: String, val region: String, val supervisor: String, val supervisorPhone: String,
    val deputy: String, val deputyPhone: String, val dataEntry: String, val dataEntryPhone: String, val notes: String,
    val createdAt: Long, val lastUpdated: Long,
)

/** قوالب مطابقة الاستيراد المشتركة (importTemplates/{بصمة}). */
object Templates {
    fun get(st: AuthState, fp: String): Pair<Map<String, String>, String>? = runCatching {
        val f = Fs.get("importTemplates/$fp", st.idToken) ?: return@runCatching null
        val j = JSONObject(Fs.stringField(f, "nmap").ifEmpty { return@runCatching null })
        j.keys().asSequence().associateWith { j.getString(it) } to Fs.stringField(f, "by")
    }.getOrNull()

    fun remember(st: AuthState, fp: String, headers: List<String>, map: Map<String, Int>) {
        runCatching {
            val j = JSONObject(); map.forEach { (k, i) -> headers.getOrNull(i)?.let { j.put(k, it) } }
            Fs.set("importTemplates/$fp", mapOf("fp" to fp, "mode" to "flat", "nmap" to j.toString(), "updatedAt" to System.currentTimeMillis(), "by" to st.user.fullName), st.idToken)
        }
    }
}

class CampDiag(val id: String, val name: String, val local: Int, val pending: Int, val tombs: Int, val since: String, val clearedAt: Long)
