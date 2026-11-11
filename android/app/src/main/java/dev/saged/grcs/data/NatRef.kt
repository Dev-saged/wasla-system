package dev.saged.grcs.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** خريطة أعمدة كشف «الأسرة المختصرة» أو «شجرة العائلة» (detectFamilySummaryColumns / detectHorizontalColumns). */
class RefCols {
    var headName: Int? = null; var headNid: Int? = null; var headDob: Int? = null; var gender: Int? = null; var marital: Int? = null
    var phone: Int? = null; var phone1: Int? = null; var phone2: Int? = null
    var originAddress: Int? = null; var currentAddress: Int? = null; var neighborhood: Int? = null
    var wifeName: Int? = null; var wifeNid: Int? = null; var wifeDob: Int? = null
    var children: List<Importer.Kid> = emptyList()
}

class RefMeta(val id: String, val name: String, val mode: String, val headers: List<String>, val count: Int, val savedAt: Long)
class RefEntry(val nid: String, val name: String, val role: String, val dob: String?, val headName: String, val headNid: String, val ref: String)
class RefParsed(val headers: List<String>, val rows: List<List<String>>, val name: String, val mode: String)

/** أدوات نص الاستيراد كما في الويب. */
object Cells {
    /** normalizeArabic. */
    fun norm(s: String) = s.replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "").replace(Regex("[إأآٱ]"), "ا").replace('ى', 'ي').replace('ة', 'ه')
        .replace(Regex("[\\u200e\\u200f\\ufeff]"), "").replace(Regex("[/\\-_,،]"), " ").replace(Regex("\\s+"), " ").trim().lowercase()
    fun any(n: String, ks: List<String>) = ks.any { n.contains(norm(it)) }
    /** cleanCell. */
    fun clean(v: String?): String {
        val s = v.orEmpty().trim()
        return if (s.isEmpty() || Regex("^-+$").matches(s) || s in listOf("لا يوجد", "لايوجد", "غير موجود", "غير متوفر", "لا يوجد.")) "" else s
    }
    /** parseDateCell: يعيد yyyy-MM-dd أو null. */
    fun date(v: String?): String? = Importer.date(v.orEmpty()).ifEmpty { null }
    /** nameKey. */
    fun nameKey(s: String) = norm(s).replace('ؤ', 'و').replace('ئ', 'ي').replace(Regex("(^|\\s)(عبد|ابو)\\s+"), "$1$2").replace(Regex("\\s+"), " ").trim()

    private val marital = listOf(
        Triple(Regex("^(م)?تزوج(ه|ة)?$|^كتزوج(ه|ة)?$"), "متزوج/ة", ""), Triple(Regex("^مطلق(ه|ة)?$"), "مطلق/ة", "byending"), Triple(Regex("^(ا|أ)رمل(ه|ة)?$"), "أرمل/ة", "byending"),
        Triple(Regex("^منفصل(ه|ة)?$"), "منفصل/ة", "byending"), Triple(Regex("^(ا|أ)عزب$|^عازب$"), "أعزب", "male"), Triple(Regex("^عزباء$|^(آ|ا)نسة$|^(آ|ا)نسه$"), "عزباء", "female"),
    )
    private val fate = listOf(
        Regex("^شهيد(ه|ة)?$") to "شهيد/ة", Regex("^متوف(ى|ي|اه|اة|يه|ية)?$") to "متوفى/اة", Regex("^حاله خاصه$|^خاص$|^خاصه$|^خاصة$") to "حالة خاصة",
        Regex("^مجهول(ه|ة)?$|^غير معروف(ه|ة)?$") to "غير معروف/ة", Regex("^معتقل(ه|ة)?$") to "معتقل/ة", Regex("^مفقود(ه|ة)?$") to "مفقود/ة",
    )
    /** normalizeMaritalWord: (الصيغة المعتمدة، الجنس أو فارغ) أو null. */
    fun maritalWord(raw: String?): Pair<String, String>? {
        val s = raw.orEmpty().trim(); if (s.isEmpty()) return null
        val n = norm(s)
        for ((re, canon, g) in marital) if (re.matches(n)) return canon to (if (g == "byending") (if (Regex("[ةه]$").containsMatchIn(s)) "female" else "male") else g)
        return null
    }
    /** detectSpouseFatePlaceholder. */
    fun spouseFate(raw: String?): String? {
        val s = raw.orEmpty().trim(); if (s.isEmpty()) return null
        val n = norm(s)
        return fate.firstOrNull { it.first.matches(n) }?.second
    }
    /** parseGenderCell. */
    fun genderCell(v: String?): String {
        val s = v.orEmpty().trim()
        return if (Regex("^(ذكر|m|male|م)$", RegexOption.IGNORE_CASE).matches(s)) "male" else if (Regex("^(أنثى|انثى|f|female|ف)$", RegexOption.IGNORE_CASE).matches(s)) "female" else "unknown"
    }
    /** resolveGender. */
    fun gender(cell: String?, maritalRaw: String?, fullName: String): String {
        if (cell != null) genderCell(cell).let { if (it != "unknown") return it }
        maritalWord(maritalRaw)?.second?.takeIf { it.isNotEmpty() }?.let { return it }
        return GenderInfer.infer(fullName).ifEmpty { "unknown" }
    }

    private fun spouse(c: RefCols, n: String, i: Int): Boolean {
        if (!n.contains("زوجه")) return false
        if (c.wifeName == null && n.contains("اسم")) c.wifeName = i
        else if (c.wifeNid == null && Regex("هويه|بطاقه").containsMatchIn(n)) c.wifeNid = i
        else if (c.wifeDob == null && n.contains("ميلاد")) c.wifeDob = i
        return true
    }

    fun summaryCols(headers: List<String>): RefCols {
        val c = RefCols()
        headers.forEachIndexed { i, h ->
            val n = norm(h); if (n.isEmpty() || spouse(c, n, i)) return@forEachIndexed
            if (c.headName == null && (any(n, listOf("اسم رب الاسره", "اسم رب العائله", "الاسم الرباعي", "اسم الزوج رباعي", "اسم المستفيد رباعي", "اسم المستفيد")) || Regex("^(ال)?اسم( رباعي| كامل| الكامل)?$").matches(n))) c.headName = i
            else if (c.headNid == null && (any(n, listOf("هويه الزوج", "رقم الهويه", "رقم البطاقه", "هويه المستفيد", "هويه رب الاسره", "رقم هويه")) || Regex("^(ال)?هويه$").matches(n))) c.headNid = i
            else if (c.headDob == null && any(n, listOf("تاريخ الميلاد"))) c.headDob = i
            else if (c.gender == null && any(n, listOf("الجنس"))) c.gender = i
            else if (c.marital == null && any(n, listOf("الحاله الاجتماعيه"))) c.marital = i
            else if (c.phone == null && any(n, listOf("رقم التواصل", "هاتف", "جوال", "موبايل")) && !Regex("بديل|(جوال|هاتف|تواصل|موبايل)\\s*2|ثاني").containsMatchIn(n)) c.phone = i
            else if (c.phone2 == null && (any(n, listOf("رقم بديل", "الرقم البديل")) || Regex("(جوال|هاتف|تواصل|موبايل)\\s*2|(جوال|هاتف) ثاني").containsMatchIn(n))) c.phone2 = i
            else if (c.originAddress == null && any(n, listOf("السكن الاصلي", "مكان السكن الاصلي", "العنوان الاصلي", "العنوان السابق"))) c.originAddress = i
            else if (c.currentAddress == null && any(n, listOf("السكن الحالي", "مكان السكن الحالي", "العنوان الحالي", "العنوان بالتفصيل", "المنطقه", "الحي"))) c.currentAddress = i
        }
        return c
    }

    fun horizontalCols(headers: List<String>): RefCols {
        val c = RefCols(); c.children = Importer.childGroups(headers)
        val used = c.children.flatMap { listOf(it.name, it.nid, it.dob) }.filterNotNull().toSet()
        var password = false; var health = false; var origin = false
        headers.forEachIndexed { i, h ->
            if (i in used) return@forEachIndexed
            val n = norm(h); if (n.isEmpty() || spouse(c, n, i)) return@forEachIndexed
            if (c.headName == null && any(n, listOf("الاسم الرباعي", "اسم رب الاسره", "اسم المستفيد"))) c.headName = i
            else if (c.headNid == null && any(n, listOf("رقم الهوية", "هوية المستفيد"))) c.headNid = i
            else if (c.headDob == null && any(n, listOf("تاريخ الميلاد"))) c.headDob = i
            else if (!password && any(n, listOf("كلمة المرور", "password"))) password = true
            else if (c.phone1 == null && any(n, listOf("الجوال 1", "رقم التواصل", "هاتف", "جوال", "موبايل")) && !Regex("2|بديل").containsMatchIn(n)) c.phone1 = i
            else if (c.phone2 == null && any(n, listOf("الجوال 2", "هاتف 2", "رقم بديل"))) c.phone2 = i
            else if (c.gender == null && any(n, listOf("الجنس"))) c.gender = i
            else if (c.marital == null && any(n, listOf("الحالة الاجتماعية"))) c.marital = i
            else if (!health && any(n, listOf("الحالة الصحية"))) health = true
            else if (!origin && any(n, listOf("المدينة الأصلية", "السكن الاصلي"))) origin = true
            else if (c.neighborhood == null && any(n, listOf("الحي", "المنطقة"))) c.neighborhood = i
        }
        return c
    }

    fun cols(headers: List<String>, mode: String) = if (mode == "horizontal") horizontalCols(headers) else summaryCols(headers)
    fun modeOf(headers: List<String>) = if (Importer.childGroups(headers).isNotEmpty()) "horizontal" else "family_summary"
}

/** الكشوف الوطنية المحفوظة بالجهاز وفهرس التحقق الفوري من الهويات (NatRef). */
object NatRef {
    lateinit var dir: File
    @Volatile private var ix: Map<String, RefEntry>? = null
    @Volatile private var fam: Map<String, List<RefEntry>> = emptyMap()
    val roleLbl = mapOf("head" to "رب أسرة", "wife" to "زوجة", "child" to "ابن/ابنة")

    private fun metaOf(o: JSONObject) = RefMeta(o.optString("id"), o.optString("name"), o.optString("mode"), o.optJSONArray("headers").strings(), o.optInt("count"), o.optLong("savedAt"))
    private fun JSONArray?.strings() = if (this == null) emptyList() else (0 until length()).map { optString(it) }
    private fun file(id: String) = File(dir, "$id.json")
    private fun metaFile(id: String) = File(dir, "$id.meta")

    fun list(): List<RefMeta> = dir.listFiles { f -> f.name.endsWith(".meta") }.orEmpty().mapNotNull { runCatching { metaOf(JSONObject(it.readText())) }.getOrNull() }.sortedByDescending { it.savedAt }

    /** يحفظ كشفاً مُحلَّلاً؛ replaceId = استبدال كشف محفوظ. */
    fun save(p: RefParsed, replaceId: String?, by: String): String {
        dir.mkdirs()
        val id = replaceId ?: java.util.UUID.randomUUID().toString()
        file(id).writeText(JSONArray().also { a -> p.rows.forEach { r -> a.put(JSONArray(r)) } }.toString())
        metaFile(id).writeText(JSONObject().put("id", id).put("name", p.name.ifEmpty { "كشف وطني" }).put("mode", p.mode).put("headers", JSONArray(p.headers)).put("count", p.rows.size).put("savedAt", System.currentTimeMillis()).put("by", by).toString())
        invalidate()
        return id
    }

    fun load(id: String): RefParsed? {
        val m = runCatching { metaOf(JSONObject(metaFile(id).readText())) }.getOrNull() ?: return null
        val a = runCatching { JSONArray(file(id).readText()) }.getOrNull() ?: return null
        return RefParsed(m.headers, (0 until a.length()).map { a.optJSONArray(it).strings() }, m.name, m.mode)
    }

    fun remove(id: String) { file(id).delete(); metaFile(id).delete(); invalidate() }
    private fun invalidate() { ix = null; fam = emptyMap() }

    private fun ok(n: String) = Regex("^\\d{8,9}$").matches(n)

    internal fun entries(name: String, headers: List<String>, mode: String, rows: List<List<String>>, ix: MutableMap<String, RefEntry>, fam: MutableMap<String, List<RefEntry>>) {
        val c = Cells.cols(headers, mode)
        for (r in rows) {
            fun v(i: Int?) = if (i == null) "" else Cells.clean(r.getOrNull(i))
            val hn = v(c.headName); val hid = Nid.norm(v(c.headNid))
            val members = ArrayList<RefEntry>()
            fun put(nid: String, nm: String, role: String, dob: String?) {
                if (!ok(nid)) return
                val e = RefEntry(nid, nm, role, dob, hn, if (ok(hid)) hid else "", name)
                members += e
                if (nid !in ix) ix[nid] = e
            }
            put(hid, hn, "head", c.headDob?.let { Cells.date(r.getOrNull(it)) })
            put(Nid.norm(v(c.wifeNid)), v(c.wifeName), "wife", c.wifeDob?.let { Cells.date(r.getOrNull(it)) })
            for (k in c.children) put(Nid.norm(v(k.nid)), v(k.name), "child", k.dob?.let { Cells.date(r.getOrNull(it)) })
            if (ok(hid) && hid !in fam) fam[hid] = members
        }
    }

    /** يبني الفهرس من كل الكشوف المحفوظة (يُستدعى خارج خيط الواجهة). */
    @Synchronized
    fun ensure(): Map<String, RefEntry> {
        ix?.let { return it }
        val i = HashMap<String, RefEntry>(); val f = HashMap<String, List<RefEntry>>()
        if (::dir.isInitialized) for (m in list()) load(m.id)?.let { entries(m.name, it.headers, it.mode, it.rows, i, f) }
        fam = f; ix = i
        return i
    }

    fun ready() = ix != null
    fun size() = ix?.size ?: 0
    fun peek(nid: String) = ix?.get(Nid.norm(nid))
    fun famOf(headNid: String) = fam[Nid.norm(headNid)].orEmpty()

    /** حالة التحقق الفوري لحقل هوية: (وارد؟، النص) أو null إن لا مرجع محفوظ. */
    fun hint(n: String): Pair<Boolean, String>? {
        val m = ix ?: return null
        if (!ok(n) || m.isEmpty()) return null
        val hit = m[n] ?: return false to "غير وارد بالكشف الوطني المخزّن"
        return true to "وارد بالكشف الوطني: ${hit.name.ifEmpty { "—" }} (${roleLbl[hit.role]})"
    }

    /** هوية مقترحة لفرد بلا هوية: من أسرة رب أسرته بالكشف، باسم مطابق وحيد وغير مستخدم. */
    fun suggestFor(p: JSONObject, head: JSONObject?, usedNids: Set<String>): RefEntry? {
        if (fam.isEmpty() || p.optString("fullName").isEmpty() || p.optBoolean("isFamilyHead")) return null
        val hn = head?.let { Nid.norm(it.optString("nationalId")) }.orEmpty(); if (hn.isEmpty()) return null
        val k = Cells.nameKey(p.optString("fullName"))
        val cands = famOf(hn).filter { it.role != "head" && it.nid !in usedNids && (Cells.nameKey(it.name) == k || Similar.jw(Cells.nameKey(it.name), k) >= 0.93) }
        return cands.singleOrNull()
    }
}

class RcResult {
    var matched = 0; var unmatched = 0; var spousesAdded = 0; var childrenAdded = 0; var skippedNoNid = 0; var headsEnriched = 0; var membersEnriched = 0
    val unmatchedList = ArrayList<List<String>>()
    val writes = LinkedHashMap<JSONObject, String>()
}

/** المطابقة الوطنية — نقل مباشر لـrunReconcileImport: تكميل الحقول الفارغة فقط، وإضافة الناقصين اختيارياً. */
object Reconcile {
    private fun has(p: JSONObject, k: String) = p.optString(k).let { it.isNotEmpty() && it != "null" }
    private fun nid(p: JSONObject) = Nid.norm(p.optString("nationalId"))

    /** findInFamily: الهوية أولاً ثم الاسم المطبّع ثم التشابه. */
    private fun find(members: List<JSONObject>, name: String, nid: String, pred: (JSONObject) -> Boolean): JSONObject? {
        val pool = members.filter(pred)
        if (nid.isNotEmpty()) pool.firstOrNull { nid(it) == nid }?.let { return it }
        if (name.isEmpty()) return null
        val k = Cells.nameKey(name)
        pool.filter { Cells.nameKey(it.optString("fullName")) == k }.singleOrNull()?.let { return it }
        var best: JSONObject? = null; var bs = 0.0
        val probe = JSONObject().put("fullName", name)
        for (p in pool) { if (nid.isNotEmpty() && nid(p).isNotEmpty() && nid(p) != nid) continue; val sc = Similar.score(probe, p); if (sc > bs) { bs = sc; best = p } }
        return if (bs >= 0.9) best else null
    }

    fun run(rows: List<List<String>>, c: RefCols, mode: String, addMembers: Boolean, byCamp: Map<String, List<JSONObject>>, uid: String, progress: (Int, Int) -> Unit): RcResult {
        val res = RcResult()
        val campOf = java.util.IdentityHashMap<JSONObject, String>()
        val headByNid = HashMap<String, JSONObject>(); val anyByNid = HashMap<String, JSONObject>()
        val membersByFamily = HashMap<String, MutableList<JSONObject>>()
        for ((camp, ps) in byCamp) for (p in ps) {
            campOf[p] = camp
            val n = nid(p)
            if (n.isNotEmpty()) { if (n !in anyByNid) anyByNid[n] = p; if (p.optBoolean("isFamilyHead")) headByNid[n] = p }
            if (has(p, "familyId")) membersByFamily.getOrPut(camp + "|" + p.optString("familyId")) { ArrayList() }.add(p)
        }
        val phoneIdx = c.phone1 ?: c.phone
        val addressIdx = c.neighborhood ?: c.currentAddress ?: c.originAddress
        val now = Clock.now()
        fun touch(p: JSONObject, camp: String) { p.put("updatedBy", uid); res.writes[p] = camp }
        fun custom(p: JSONObject) = p.optJSONObject("customFields") ?: JSONObject().also { p.put("customFields", it) }
        fun setDob(p: JSONObject, d: String) { p.put("birthDate", d); Stats.ageOfDob(d)?.let { p.put("calculatedAge", it) } }
        fun blank(name: String, nidV: String, dob: String?, gender: String, relation: String, camp: String, fid: String, order: Int, head: JSONObject) = JSONObject()
            .put("id", java.util.UUID.randomUUID().toString()).put("campId", camp).put("createdAt", now).put("createdBy", uid).put("fullName", name).put("nationalId", nidV)
            .put("birthDate", dob ?: JSONObject.NULL).also { o -> dob?.let { Stats.ageOfDob(it) }?.let { o.put("calculatedAge", it) } }
            .put("gender", gender).put("isFamilyHead", false).put("relation", relation).put("familyId", fid).put("orderIndex", order).put("phone", "").put("address", head.optString("address")).put("notes", "")
            .put("hasChronicDisease", false).put("chronicConditions", JSONArray()).put("diseaseDetails", "").put("hasDisability", false).put("disabilityDetails", "").put("isPregnant", false).put("isBreastfeeding", false)
            .put("maritalStatus", if (relation == "wife") head.optString("maritalStatus") else "").put("customFields", JSONObject())

        rows.forEachIndexed { done, row ->
            if ((done + 1) % 30 == 0) progress(done + 1, rows.size)
            fun v(i: Int?) = if (i == null) "" else Cells.clean(row.getOrNull(i))
            val headName = v(c.headName); if (headName.isEmpty()) return@forEachIndexed
            val headNid = Nid.norm(v(c.headNid))
            if (headNid.isEmpty()) { res.skippedNoNid++; res.unmatchedList += listOf(headName, "", "بلا رقم هوية لرب الأسرة"); return@forEachIndexed }
            val head = headByNid[headNid]
            if (head == null) {
                val other = anyByNid[headNid]
                res.unmatched++
                res.unmatchedList += listOf(headName, headNid, if (other != null) "مسجّل كفرد (${Rel.label(other)}) داخل أسرة وليس رب أسرة" else "غير موجود بالنظام")
                return@forEachIndexed
            }
            res.matched++
            var headChanged = false
            if (!has(head, "familyId")) { head.put("familyId", nid(head).ifEmpty { head.optString("id") }); headChanged = true }
            val camp = campOf[head]!!; val fid = head.optString("familyId")
            val fm = membersByFamily.getOrPut("$camp|$fid") { ArrayList() }
            if (fm.none { it === head }) fm.add(head)

            fun fill(p: JSONObject, k: String, x: String): Boolean { if (x.isNotEmpty() && !has(p, k)) { p.put(k, x); return true }; return false }
            if (fill(head, "phone", v(phoneIdx))) headChanged = true
            if (!has(head, "birthDate")) c.headDob?.let { Cells.date(row.getOrNull(it)) }?.let { setDob(head, it); headChanged = true }
            if (fill(head, "address", v(addressIdx))) headChanged = true
            val maritalRaw = v(c.marital)
            val mw = Cells.maritalWord(maritalRaw)
            if (mw != null && !has(head, "maritalStatus")) { head.put("maritalStatus", mw.first); headChanged = true }
            if (head.optString("gender").let { it.isEmpty() || it == "unknown" }) { val g = Cells.gender(c.gender?.let { row.getOrNull(it) }, maritalRaw, head.optString("fullName")); if (g != "unknown") { head.put("gender", g); headChanged = true } }
            val ph2 = v(c.phone2)
            if (ph2.isNotEmpty() && ph2 != head.optString("phone") && custom(head).optString("الجوال 2").isEmpty()) { custom(head).put("الجوال 2", ph2); headChanged = true }

            val wifeRaw = v(c.wifeName)
            val wifeFate = if (Cells.maritalWord(wifeRaw) != null) null else Cells.spouseFate(wifeRaw)
            if (wifeFate != null && custom(head).optString("حالة الزوج/ة").isEmpty()) { custom(head).put("حالة الزوج/ة", wifeFate); headChanged = true }
            if (headChanged) { touch(head, camp); res.headsEnriched++ }

            if (wifeRaw.isNotEmpty() && Cells.maritalWord(wifeRaw) == null && wifeFate == null) {
                val wNid = Nid.norm(v(c.wifeNid)); val wDob = c.wifeDob?.let { Cells.date(row.getOrNull(it)) }
                val wives = fm.filter { it.optString("relation") == "wife" }
                val spouse = find(fm, wifeRaw, wNid) { !it.optBoolean("isFamilyHead") && it.optString("relation").let { r -> r == "wife" || r == "husband" || r.isEmpty() } }
                    ?: if (wNid.isNotEmpty()) null else wives.singleOrNull()
                if (spouse != null) {
                    var ch = false
                    if (wNid.isNotEmpty() && nid(spouse).isEmpty() && wNid !in anyByNid) { spouse.put("nationalId", wNid); anyByNid[wNid] = spouse; ch = true }
                    if (wDob != null && !has(spouse, "birthDate")) { setDob(spouse, wDob); ch = true }
                    if (ch) { touch(spouse, camp); res.membersEnriched++ }
                } else if (addMembers && !(wNid.isNotEmpty() && wNid in anyByNid)) {
                    val sp = blank(wifeRaw, wNid, wDob, "female", "wife", camp, fid, 1, head)
                    touch(sp, camp); fm.add(sp); if (wNid.isNotEmpty()) anyByNid[wNid] = sp
                    res.spousesAdded++
                }
            }

            if (mode == "horizontal" && c.children.isNotEmpty()) {
                var ci = fm.size
                for (k in c.children) {
                    val cName = v(k.name); if (cName.isEmpty()) continue
                    val cNid = Nid.norm(v(k.nid)); val cDob = k.dob?.let { Cells.date(row.getOrNull(it)) }
                    val kid = find(fm, cName, cNid) { !it.optBoolean("isFamilyHead") && it.optString("relation").let { r -> r != "wife" && r != "husband" } }
                    if (kid != null) {
                        var chg = false
                        if (cNid.isNotEmpty() && nid(kid).isEmpty() && cNid !in anyByNid) { kid.put("nationalId", cNid); anyByNid[cNid] = kid; chg = true }
                        if (cDob != null && !has(kid, "birthDate")) { setDob(kid, cDob); chg = true }
                        if (kid.optString("gender").let { it.isEmpty() || it == "unknown" }) { val g = GenderInfer.infer(kid.optString("fullName")); if (g.isNotEmpty() && g != "unknown") { kid.put("gender", g); chg = true } }
                        if (chg) { touch(kid, camp); res.membersEnriched++ }
                        continue
                    }
                    if (!addMembers || (cNid.isNotEmpty() && cNid in anyByNid)) continue
                    val child = blank(cName, cNid, cDob, Cells.gender(null, null, cName), "child", camp, fid, 2 + ci, head)
                    touch(child, camp); fm.add(child); if (cNid.isNotEmpty()) anyByNid[cNid] = child
                    res.childrenAdded++; ci++
                }
            }
        }
        return res
    }
}
