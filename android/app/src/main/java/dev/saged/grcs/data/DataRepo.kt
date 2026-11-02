package dev.saged.grcs.data

import android.content.Context
import dev.saged.grcs.auth.AuthRepo
import dev.saged.grcs.auth.AuthState
import org.json.JSONObject

class CampSummary(val id: String, val name: String, val stats: CampStats)

/** مزامنة قراءة فقط: يسحب التغييرات الجديدة (u >= آخر مزامنة − 60ث) ثم يحسب المؤشرات محلياً. */
class DataRepo(ctx: Context, private val auth: AuthRepo) {
    private val cache = FamCache(ctx)
    val people = People(cache)
    private val writer = FamWriter(cache)
    private val accounts = Accounts(cache)
    val backups = Backups(ctx)
    val export = Export(ctx)
    fun savedQueries() = Query.loadSaved(cache)
    fun saveQueries(l: List<Saved>) = Query.saveAll(cache, l)
    fun similar(camp: String) = Similar.find(people.persons(camp), Similar.ignored(cache))
    fun ignoreSimilar(a: JSONObject, b: JSONObject) = Similar.ignore(cache, a, b)
    val trash by lazy { Trash(this) }
    val transfer by lazy { Transfer(this) }
    private var secV = 2L

    init { Clock.init(ctx); Usage.init(ctx); Messages.Outbox.init(ctx); Messages.Seen.init(ctx); GenderInfer.init(ctx) }

    private fun campsOf(st: AuthState, token: String): List<Pair<String, String>> = when (st.user.role) {
        "admin" -> Fs.listCamps(token)
        "delegate", "dataentry" -> listOf(st.user.campId to st.user.campId)
        else -> emptyList()
    }

    /** المخيمات المتاحة للمستخدم من النسخة المحلية (المعرّف، الاسم). */
    fun campList(st: AuthState): List<Pair<String, String>> {
        val names = cache.meta("camps")?.let { runCatching { org.json.JSONObject(it) }.getOrNull() }
        val fromCache = names?.keys()?.asSequence()?.map { it to names.optString(it).ifBlank { it } }?.toList().orEmpty()
        return if (st.user.role == "admin") fromCache else listOf(st.user.campId to (names?.optString(st.user.campId)?.ifBlank { null } ?: st.user.campId))
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
        if (st.user.role == "beneficiary") return benSync(st)
        runCatching { withToken { Fs.get("config/app", it) } }.getOrNull()?.let { secV = Fs.intField(it, "secV") }
        for (c in cache.campsWithPending()) runCatching { withToken { t -> writer.push(c, t, st.user.id) } }
        val camps = withToken { campsOf(st, it) }
        for ((cid, cname) in camps) runCatching { withToken { t -> accounts.run(cid, cname, t, secV) } }
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

    /** المستفيد لا يستعلم المجموعة: يقرأ مستندات أسرته المختومة بالرمز فقط. */
    private fun benSync(st: AuthState): Pair<AuthState, List<CampSummary>> {
        val camp = st.user.campId
        val fams = auth.claims(st.idToken)?.optJSONArray("fams")?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty().filter { it.isNotEmpty() }
        runCatching { writer.push(camp, st.idToken, st.user.id) }
        val docs = Fs.famBatchGet(camp, fams, st.idToken).values.toList()
        cache.put(camp, docs)
        cache.setMeta("camps", org.json.JSONObject().put(camp, camp).toString())
        return st to listOf(summary(camp, camp))
    }

    private fun summary(id: String, name: String): CampSummary {
        return CampSummary(id, name, Stats.compute(people.persons(id)))
    }

    /** إدارة المخيمات (للأدمن): إنشاء مخيم بنفس حقول الويب. */
    fun createCamp(st: AuthState, name: String): String? {
        val n = name.trim(); if (n.isEmpty() || st.user.role != "admin") return null
        val id = java.util.UUID.randomUUID().toString(); val now = System.currentTimeMillis()
        val ok = runCatching {
            Fs.set("camps/$id", mapOf("id" to id, "name" to n, "region" to "", "supervisor" to "", "supervisorPhone" to "", "deputy" to "", "deputyPhone" to "",
                "dataEntry" to "", "dataEntryPhone" to "", "createdAt" to now, "lastUpdated" to now, "totalFamilies" to 0), st.idToken)
            Fs.pulse("g", null, st.idToken)
        }.isSuccess
        if (ok) putCampName(id, n)
        return if (ok) id else null
    }

    /** إعادة التسمية: المستند + اسم المخيم في حسابات المخيم (كما propagateCampName). */
    fun renameCamp(st: AuthState, id: String, name: String): Boolean {
        val n = name.trim(); if (n.isEmpty() || st.user.role != "admin") return false
        val ok = runCatching {
            Fs.patch("camps/$id", mapOf("name" to n, "lastUpdated" to System.currentTimeMillis()), st.idToken)
            Fs.pulse("g", null, st.idToken)
        }.isSuccess
        if (!ok) return false
        putCampName(id, n)
        runCatching {
            Fs.queryEq("users", listOf("campId" to id), st.idToken, 500).forEach { d ->
                val uid = d.getString("id")
                Fs.patch("users/$uid", mapOf("campName" to n), st.idToken)
            }
            Fs.pulse("u", null, st.idToken)
        }
        return true
    }

    private fun putCampName(id: String, n: String) {
        val m = cache.meta("camps")?.let { runCatching { org.json.JSONObject(it) }.getOrNull() } ?: org.json.JSONObject()
        cache.setMeta("camps", m.put(id, n).toString())
    }

    fun autoBackup(camp: String, name: String) {
        runCatching { backups.autoDaily(cache, camp, name, people.persons(camp)) }
    }

    fun clear() { if (!cache.hasPending()) cache.clear() }

    fun credentials(st: AuthState, nid: String, camp: String) = accounts.credentials(nid, camp, st.idToken)

    fun checkNid(st: AuthState, nid: String, camp: String): String? = accounts.checkNid(nid, camp, st.idToken)

    /** بعد حفظ/حذف فرد: يؤجّل الفهرس الوطني وحساب المستفيد ثم يحاول تنفيذهما فوراً. */
    fun afterSave(st: AuthState, camp: String, p: JSONObject, deleted: Boolean) {
        Thread { Audit.log(st, camp, if (deleted) "delete_person" else "edit_person", p.optString("id"), p.optString("fullName")) }.start()
        if (deleted) accounts.enqueueDelete(camp, p) else accounts.enqueueSave(camp, p)
        val name = campList(st).firstOrNull { it.first == camp }?.second ?: camp
        runCatching { accounts.run(camp, name, st.idToken, secV) }
    }

    /** رفع فوري لمخيم بعد تعديل (يعيد false إن تعذّر وبقي معلّقاً). */
    fun pushNow(st: AuthState, camp: String): Boolean = runCatching { writer.push(camp, st.idToken, st.user.id) }.getOrDefault(false)
}
