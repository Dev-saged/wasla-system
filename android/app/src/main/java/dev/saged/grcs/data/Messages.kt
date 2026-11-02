package dev.saged.grcs.data

import dev.saged.grcs.auth.AuthState
import org.json.JSONObject

/** رسائل المخيم والرسائل العامة (اتصال مباشر؛ الإرسال الفاشل يبقى نصه في الشاشة لإعادة المحاولة). */
class Messages {
    fun load(st: AuthState, thread: String): List<JSONObject> {
        val own = if (thread == "global") emptyList() else Fs.recentMessages("camps/$thread", "messages", st.idToken)
        val glob = if (st.user.role == "beneficiary") emptyList() else Fs.recentMessages("globalMessages/global", "messages", st.idToken, 50)
        return (own + glob).distinctBy { it.getString("id") }.sortedBy { it.getLong("timestamp") }
    }

    fun loadBen(st: AuthState): List<JSONObject> =
        Fs.recentMessages("camps/${st.user.campId}", "benMessages", st.idToken).filter { it.optString("threadId") == "ben_" + st.user.id }.sortedBy { it.getLong("timestamp") }

    /** يعيد true عند النجاح. */
    fun send(st: AuthState, thread: String, text: String): Boolean = try {
        val u = st.user
        val id = java.util.UUID.randomUUID().toString()
        val ben = u.role == "beneficiary"
        val camp = when { ben -> u.campId; thread == "global" -> "ALL"; u.role == "admin" -> thread; else -> u.campId }
        val tid = if (ben) "ben_" + u.id else thread
        val f = hashMapOf<String, Any?>("id" to id, "threadId" to tid, "senderId" to u.id, "senderName" to u.fullName, "senderRole" to u.role,
            "campId" to camp, "text" to text, "timestamp" to Clock.now())
        if (ben) f["beneficiaryId"] = u.id
        val path = when { ben -> "camps/${u.campId}/benMessages/$id"; thread == "global" -> "globalMessages/global/messages/$id"; else -> "camps/$camp/messages/$id" }
        Fs.setWithServerTime(path, f, "st", st.idToken)
        runCatching { if (thread == "global" && !ben) Fs.pulse("g", null, st.idToken) else Fs.pulse("m", camp, st.idToken) }
        true
    } catch (e: Exception) { false }
}
