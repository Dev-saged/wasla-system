package dev.saged.grcs.data

import dev.saged.grcs.auth.AuthState
import org.json.JSONObject

/** رسائل المخيم والرسائل العامة (اتصال مباشر؛ الإرسال الفاشل يبقى نصه في الشاشة لإعادة المحاولة). */
class Messages {
    /** عدد غير المقروء لكل موضوع، يُملأ عند benThreads (من الرسائل المحمّلة أصلاً دون قراءات إضافية). */
    val unread = HashMap<String, Int>()

    /** آخر وقت قراءة لكل موضوع (محلي للجهاز كما في الويب). */
    object Seen {
        private var prefs: android.content.SharedPreferences? = null
        fun init(ctx: android.content.Context) { prefs = ctx.getSharedPreferences("grcs_seen", android.content.Context.MODE_PRIVATE) }
        fun get(user: String, thread: String) = prefs?.getLong("$user|$thread", 0L) ?: 0L
        fun mark(user: String, thread: String, ts: Long) { if (ts > get(user, thread)) prefs?.edit()?.putLong("$user|$thread", ts)?.apply() }
    }

    /** مواضيع المستفيدين في مخيم: (معرّف الموضوع، اسم المستفيد، آخر رسالة). */
    fun benThreads(st: AuthState, camp: String): List<Triple<String, String, String>> {
        val all = Fs.recentMessages("camps/$camp", "benMessages", st.idToken, 200)
        return all.groupBy { it.optString("threadId") }.filterKeys { it.startsWith("ben_") }.map { (tid, ms) ->
            val seen = Seen.get(st.user.id, tid)
            unread[tid] = ms.count { it.optString("senderId") != st.user.id && it.optLong("timestamp") > seen }
            val name = ms.firstOrNull { it.optString("senderId") == tid.removePrefix("ben_") }?.optString("senderName") ?: ms.first().optString("senderName")
            Triple(tid, name, ms.maxByOrNull { it.getLong("timestamp") }!!.optString("text"))
        }.sortedBy { it.second }
    }

    fun load(st: AuthState, thread: String): List<JSONObject> {
        runCatching { flush(st) }
        if (thread.startsWith("bt:")) {
            val (camp, tid) = thread.removePrefix("bt:").split(":", limit = 2)
            return Fs.recentMessages("camps/$camp", "benMessages", st.idToken, 200).filter { it.optString("threadId") == tid }.sortedBy { it.getLong("timestamp") }
        }
        val own = if (thread == "global") emptyList() else Fs.recentMessages("camps/$thread", "messages", st.idToken)
        val glob = if (st.user.role == "beneficiary") emptyList() else Fs.recentMessages("globalMessages/global", "messages", st.idToken, 50)
        return (own + glob).distinctBy { it.getString("id") }.sortedBy { it.getLong("timestamp") }
    }

    fun loadBen(st: AuthState): List<JSONObject> {
        runCatching { flush(st) }
        return loadBenRaw(st)
    }

    private fun loadBenRaw(st: AuthState): List<JSONObject> =
        Fs.recentMessages("camps/${st.user.campId}", "benMessages", st.idToken).filter { it.optString("threadId") == "ben_" + st.user.id }.sortedBy { it.getLong("timestamp") }

    /** صندوق صادر دائم: الرسالة الفاشلة بسبب الشبكة تُحفظ وتُعاد تلقائياً بنفس المعرّف (كتابة idempotent). */
    object Outbox {
        private var prefs: android.content.SharedPreferences? = null
        fun init(ctx: android.content.Context) { prefs = ctx.getSharedPreferences("grcs_outbox", android.content.Context.MODE_PRIVATE) }
        @Synchronized fun all(): org.json.JSONArray = runCatching { org.json.JSONArray(prefs?.getString("q", "[]")) }.getOrDefault(org.json.JSONArray())
        @Synchronized fun save(a: org.json.JSONArray) { prefs?.edit()?.putString("q", a.toString())?.apply() }
        @Synchronized fun add(user: String, thread: String, text: String, id: String) {
            save(all().put(JSONObject().put("u", user).put("th", thread).put("t", text).put("id", id)))
        }
        fun count(user: String) = all().let { a -> (0 until a.length()).count { a.getJSONObject(it).optString("u") == user } }
    }

    /** يعيد إرسال المعلّق لهذا المستخدم؛ يعيد عدد ما أُرسل. */
    fun flush(st: AuthState): Int {
        val q = Outbox.all(); val keep = org.json.JSONArray(); var sent = 0
        for (i in 0 until q.length()) {
            val o = q.getJSONObject(i)
            if (o.optString("u") != st.user.id) { keep.put(o); continue }
            if (post(st, o.getString("th"), o.getString("t"), o.getString("id"))) sent++ else keep.put(o)
        }
        if (sent > 0) Outbox.save(keep)
        return sent
    }

    /** رسالة جماعية لكل مستفيدي مخيم، كل واحد بمحادثته (كما sendAllBeneficiariesBroadcast). يعيد العدد أو -1 عند الفشل. */
    fun broadcast(st: AuthState, camp: String, text: String): Int = try {
        val bens = Fs.queryEq("users", listOf("role" to "beneficiary", "campId" to camp), st.idToken, 1000)
        val now = Clock.now()
        val docs = bens.map { b ->
            val id = java.util.UUID.randomUUID().toString()
            "camps/$camp/benMessages/$id" to mapOf<String, Any?>("id" to id, "threadId" to "ben_" + b.getString("id"), "beneficiaryId" to b.getString("id"),
                "beneficiaryName" to b.optString("fullName").ifEmpty { b.optString("username") }, "senderId" to st.user.id, "senderName" to st.user.fullName,
                "senderRole" to st.user.role, "campId" to camp, "text" to text, "timestamp" to now)
        }
        if (docs.isNotEmpty()) { Fs.setMany(docs, st.idToken); runCatching { Fs.pulse("m", camp, st.idToken) } }
        docs.size
    } catch (e: Exception) { -1 }

    /** true = أُرسلت أو حُفظت في الصادر لإعادة المحاولة. */
    fun send(st: AuthState, thread: String, text: String): Boolean {
        val id = java.util.UUID.randomUUID().toString()
        if (post(st, thread, text, id)) { runCatching { flush(st) }; return true }
        Outbox.add(st.user.id, thread, text, id)
        return true
    }

    private fun post(st: AuthState, thread: String, text: String, id: String): Boolean {
        return try {
            val u = st.user
            if (thread.startsWith("bt:")) {
                val (bc, btid) = thread.removePrefix("bt:").split(":", limit = 2)
                Fs.setWithServerTime("camps/$bc/benMessages/$id", mapOf("id" to id, "threadId" to btid, "senderId" to u.id, "senderName" to u.fullName,
                    "senderRole" to u.role, "campId" to bc, "text" to text, "timestamp" to Clock.now(), "beneficiaryId" to btid.removePrefix("ben_")), "st", st.idToken)
                runCatching { Fs.pulse("m", bc, st.idToken) }
                return true
            }
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
}
