package dev.saged.grcs.data

import android.content.Context
import dev.saged.grcs.auth.AuthRepo
import dev.saged.grcs.auth.AuthState

class CampSummary(val id: String, val name: String, val stats: CampStats)

/** مزامنة قراءة فقط: يسحب التغييرات الجديدة (u >= آخر مزامنة − 60ث) ثم يحسب المؤشرات محلياً. */
class DataRepo(ctx: Context, private val auth: AuthRepo) {
    private val cache = FamCache(ctx)

    private fun campsOf(st: AuthState, token: String): List<Pair<String, String>> = when (st.user.role) {
        "admin" -> Fs.listCamps(token)
        "delegate", "dataentry" -> listOf(st.user.campId to st.user.campId)
        else -> emptyList()
    }

    /** الملخصات المخزنة دون شبكة. */
    fun cached(st: AuthState): List<CampSummary> {
        val names = cache.meta("camps")?.let { runCatching { org.json.JSONObject(it) }.getOrNull() }
        val ids = names?.keys()?.asSequence()?.toList() ?: if (st.user.campId.isNotEmpty()) listOf(st.user.campId) else emptyList()
        return ids.map { id -> summary(id, names?.optString(id).orEmpty().ifBlank { id }) }
    }

    fun sync(st0: AuthState, onProgress: (Int) -> Unit = {}): Pair<AuthState, List<CampSummary>> {
        var st = st0
        if (st.offline || st.expiresAt - System.currentTimeMillis() < 120_000) st = auth.restore() ?: st
        if (st.offline) return st to cached(st)
        fun <T> withToken(f: (String) -> T): T = try { f(st.idToken) } catch (e: FsException) {
            if (e.status != 401) throw e
            st = auth.restore() ?: throw e
            f(st.idToken)
        }
        val camps = withToken { campsOf(st, it) }
        cache.setMeta("camps", org.json.JSONObject().apply { camps.forEach { put(it.first, it.second) } }.toString())
        var n = 0
        for ((id, _) in camps) {
            val key = "since_$id"
            val since = cache.meta(key)
            var after: FamDoc? = null
            while (true) {
                val page = withToken { Fs.famPage(id, since, after, it) }
                if (page.isEmpty()) break
                cache.put(id, page); n += page.size; onProgress(n)
                page.last().u.takeIf { it.isNotEmpty() }?.let { cache.setMeta(key, it) }
                if (page.size < 300) break
                after = page.last()
            }
            val main = withToken { runCatching { Fs.get("camps/$id/data/main", it) }.getOrNull() }
            cache.setMeta("cleared_$id", Fs.intField(main, "clearedAt").toString())
        }
        return st to camps.map { summary(it.first, it.second) }
    }

    private fun summary(id: String, name: String): CampSummary {
        val cleared = cache.meta("cleared_$id")?.toLongOrNull() ?: 0L
        return CampSummary(id, name, Stats.compute(Stats.persons(cache.docs(id), cleared)))
    }

    fun clear() = cache.clear()
}
