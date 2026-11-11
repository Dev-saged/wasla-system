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
    /** المخيم الذي يتصفحه المسؤول حالياً (adminViewingCampId في الويب)؛ null = كل المخيمات. */
    @Volatile var scope: String? = null

    /** مخيمات السياق الحالي: مخيم واحد إن كان المسؤول داخل مخيم، وإلا كل ما يتاح للحساب. */
    fun ctxCamps(st: AuthState): List<Pair<String, String>> = campList(st).let { l -> scope?.let { c -> l.filter { it.first == c }.ifEmpty { l } } ?: l }

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

    /** معلومات المخيم (للأدمن والمندوب). */
    fun campInfo(st: AuthState, camp: String): CampInfo {
        val f = runCatching { Fs.get("camps/$camp", st.idToken) }.getOrNull()
        fun s(k: String) = Fs.stringField(f, k)
        val name = s("name").ifBlank { cache.meta("camps")?.let { runCatching { org.json.JSONObject(it).optString(camp) }.getOrNull() }.orEmpty().ifBlank { camp } }
        return CampInfo(camp, name, s("region"), s("supervisor"), s("supervisorPhone"), s("deputy"), s("deputyPhone"), s("dataEntry"), s("dataEntryPhone"), s("notes"),
            Fs.intField(f, "createdAt"), Fs.intField(f, "lastUpdated"))
    }

    fun saveCampInfo(st: AuthState, i: CampInfo): Boolean {
        val ok = runCatching {
            Fs.patch("camps/${i.id}", mapOf("name" to i.name.ifBlank { i.id }, "region" to i.region, "supervisor" to i.supervisor, "supervisorPhone" to i.supervisorPhone,
                "deputy" to i.deputy, "deputyPhone" to i.deputyPhone, "dataEntry" to i.dataEntry, "dataEntryPhone" to i.dataEntryPhone, "notes" to i.notes,
                "lastUpdated" to System.currentTimeMillis()), st.idToken)
            Fs.pulse("g", null, st.idToken)
        }.isSuccess
        if (ok) { putCampName(i.id, i.name.ifBlank { i.id }); Audit.log(st, i.id, "camp_info", i.id, i.name) }
        return ok
    }

    /** تشخيص المزامنة لكل مخيم: ما في الجهاز، المعلّق، الشواهد، مؤشر السحب وعلامة الإفراغ. */
    fun diagnostics(st: AuthState): List<CampDiag> = campList(st).map { (id, n) ->
        CampDiag(id, n, cache.docRows(id).size, cache.pending(id).size, cache.tombs(id).size, cache.meta("since_$id").orEmpty(), cache.meta("cleared_$id")?.toLongOrNull() ?: 0L)
    }

    /** يمسح مؤشرات السحب فتُعاد المزامنة كاملة من الخادم (لا يحذف شيئاً). */
    fun resetCursors(st: AuthState) { campList(st).forEach { (id, _) -> cache.setMeta("since_$id", "") } }

    /** إفراغ مخيم أو حذفه (أدمن فقط): نسخة احتياطية إلزامية ثم سلة المحذوفات ثم الخادم ثم الجهاز. يعيد رسالة الخطأ أو null عند النجاح. */
    fun wipeCamp(st: AuthState, camp: String, name: String, deleteCamp: Boolean): String? {
        if (st.user.role != "admin") return "العملية للمسؤول فقط"
        if (st.offline) return "تحتاج اتصالاً بالإنترنت ليُطبَّق عند الجميع"
        val persons = people.persons(camp)
        if (!backups.write(backups.fileName(camp, false), backups.build(camp, name, persons))) return "تعذّر حفظ النسخة الاحتياطية — امنح إذن التخزين ثم أعد المحاولة"
        if (persons.isNotEmpty() && !trash.put(st, camp, if (deleteCamp) "camp" else "clear", (if (deleteCamp) "حذف " else "إفراغ ") + name, persons)) return "تعذّر الحفظ في سلة المحذوفات — لم يُحذف شيء"
        val t = st.idToken
        return runCatching {
            val now = Clock.now()
            Audit.log(st, camp, if (deleteCamp) "delete_camp" else "clear_camp", camp, name, "${persons.size} فرد")
            if (!deleteCamp) Fs.set("camps/$camp/data/main", mapOf("v" to 2, "blob" to "", "count" to 0, "updatedAt" to now, "deletedIds" to emptyList<String>(), "clearedAt" to now), t)
            Fs.deleteMany(Fs.listIds("camps/$camp", "fam", t).map { "camps/$camp/fam/$it" }, t)
            val users = Fs.queryEq("users", listOf("campId" to camp), t, 1000).filter { deleteCamp || it.optString("role") == "beneficiary" }
            val paths = LinkedHashSet<String>()
            users.forEach { paths += "users/${it.getString("id")}"; paths += "creds/${it.getString("id")}" }
            persons.map { it.optString("nationalId") }.filter { it.isNotEmpty() }.forEach { paths += "nationalIds/$it" }
            Fs.deleteMany(paths.toList(), t)
            if (deleteCamp) {
                listOf("messages", "benMessages").forEach { c -> Fs.deleteMany(Fs.listIds("camps/$camp", c, t).map { "camps/$camp/$c/$it" }, t) }
                Fs.deleteMany(listOf("camps/$camp/data/main", "camps/$camp/data/v2", "camps/$camp/info/main", "camps/$camp"), t)
            } else runCatching { Fs.patch("camps/$camp", mapOf("totalFamilies" to 0, "lastUpdated" to now), t) }
            runCatching { Fs.pulse("c", camp, t); Fs.pulse("u", null, t); Fs.pulse("g", null, t) }
            cache.wipeCamp(camp)
            if (deleteCamp) {
                val m = cache.meta("camps")?.let { runCatching { org.json.JSONObject(it) }.getOrNull() } ?: org.json.JSONObject()
                m.remove(camp); cache.setMeta("camps", m.toString())
            } else cache.setMeta("cleared_$camp", now.toString())
            null
        }.getOrElse { "تعذّر إكمال العملية: ${it.message ?: "خطأ شبكة"} — النسخة محفوظة في الجهاز والسلة" }
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

    /**
     * حفظ نموذج الفرد كما في الويب: فحص المخيمات الأخرى للهويات الجديدة أو المعدّلة فقط (NidField.crossCamp)،
     * ثم حفظ الفرد والزوج/ة والأبناء، وحذف من أُزيل من النموذج، ثم الرفع. يعيد نص الخطأ أو null.
     */
    fun saveForm(st: AuthState, camp: String, r: FormResult): String? {
        val before = people.persons(camp).associateBy { it.optString("id") }
        if (!st.offline) for (x in listOf(r.main) + r.others) {
            val n = x.optString("nationalId")
            if (n.isEmpty() || Nid.status(n) != "valid" || n == before[x.optString("id")]?.optString("nationalId")) continue
            runCatching { checkNid(st, n, camp) }.getOrNull()?.let { return it }
        }
        val saved = people.save(camp, r.main)
        Thread { Audit.log(st, camp, if (r.isNew) "add_person" else "edit_person", saved.optString("id"), saved.optString("fullName")) }.start()
        accounts.enqueueSave(camp, saved)
        for (x in r.others) { x.put("familyId", saved.optString("familyId")); accounts.enqueueSave(camp, people.save(camp, x)) }
        for (x in r.removed) { runCatching { trash.put(st, camp, "person", x.optString("fullName").ifEmpty { "—" }, listOf(x)) }; people.delete(camp, x); accounts.enqueueDelete(camp, x) }
        val name = campList(st).firstOrNull { it.first == camp }?.second ?: camp
        runCatching { accounts.run(camp, name, st.idToken, secV) }
        pushNow(st, camp)
        return null
    }

    /** رفع فوري لمخيم بعد تعديل (يعيد false إن تعذّر وبقي معلّقاً). */
    fun pushNow(st: AuthState, camp: String): Boolean = runCatching { writer.push(camp, st.idToken, st.user.id) }.getOrDefault(false)
}
