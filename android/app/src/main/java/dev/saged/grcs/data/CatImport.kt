package dev.saged.grcs.data

import org.json.JSONArray
import org.json.JSONObject

/** توقيع ملف النظام: ورقة مخفية تحمل سجلات المخيم كاملة لإعادة استيراد مطابقة 100% (GRCS_SIG في الويب). */
object Sig {
    const val SHEET = "_grcs_data"
    private const val TAG = "GRCS-EXPORT"

    fun rows(camp: String, campName: String, persons: List<JSONObject>): List<List<String>> =
        listOf(listOf(TAG, "v1", camp, campName, System.currentTimeMillis().toString(), "native")) +
            persons.map { p -> listOf("P", JSONObject(p.toString()).also { it.remove("syncStatus") }.toString()) }

    class Parsed(val campId: String, val campName: String, val persons: List<JSONObject>)
    class Plan(val toWrite: List<JSONObject>, val pre: List<JSONObject>, val added: Int, val updated: Int, val excluded: List<List<String>>, val sameCamp: Boolean)

    /** runNativeImport: مخيم نفسه ← تحديث الموجود بمعرّفه؛ مخيم مختلف ← معرّفات جديدة وربط الأسر من جديد واستبعاد المسجّل بمخيم آخر (otherNids). */
    fun prepare(s: Parsed, camp: String, local: List<JSONObject>, otherNids: Set<String>, uid: String): Plan {
        val same = s.campId == camp
        var records = s.persons.map { JSONObject(it.toString()) }
        val excluded = ArrayList<List<String>>()
        if (!same) {
            val now = Clock.now(); val fam = HashMap<String, String>()
            records.forEach { p ->
                val f = p.optString("familyId")
                if (f.isNotEmpty() && f !in fam) fam[f] = java.util.UUID.randomUUID().toString()
                p.put("id", java.util.UUID.randomUUID().toString())
                if (f.isNotEmpty()) p.put("familyId", fam.getValue(f))
                p.put("createdAt", now).put("createdBy", uid)
            }
            records = records.filter { p ->
                val n = Nid.norm(p.optString("nationalId"))
                if (n.isNotEmpty() && n in otherNids) { excluded += listOf(p.optString("fullName"), n, "مسجّل بمخيم آخر"); false } else true
            }
        }
        val byId = local.associateBy { it.optString("id") }
        val pre = ArrayList<JSONObject>(); var added = 0; var updated = 0
        val out = records.map { p ->
            val lp = byId[p.optString("id")]
            val o = JSONObject(lp?.toString() ?: "{}")
            if (lp != null) { pre += JSONObject(lp.toString()); updated++ } else added++
            p.keys().forEach { k -> o.put(k, p.get(k)) }
            o.put("campId", camp).put("updatedBy", uid).remove("syncStatus")
            o
        }
        return Plan(out, pre, added, updated, excluded, same)
    }

    fun parse(rows: List<List<String>>?): Parsed? {
        val head = rows?.firstOrNull() ?: return null
        if (head.getOrNull(0) != TAG) return null
        val ps = rows.drop(1).filter { it.getOrNull(0) == "P" }.mapNotNull { r -> runCatching { JSONObject(r[1]) }.getOrNull() }
        return Parsed(head.getOrNull(2).orEmpty(), head.getOrNull(3).orEmpty(), ps)
    }
}

class CatType(val key: String, val label: String, val kind: String, val female: Boolean = false, val rel: String? = null)

class CatRow(val v: Map<String, String>, val extra: Map<Int, String>, val headers: List<String>) {
    fun s(k: String) = v[k].orEmpty()
    var gid: String? = null
}

class CatItem(val i: Int, val name: String, val nid: String, val row: CatRow, var status: String, val reason: String = "", val how: String = "", var person: JSONObject? = null, var head: JSONObject? = null) {
    var other = ""
}

class CatApplied(val writes: List<JSONObject>, val created: List<JSONObject>, val newHeads: List<JSONObject>, val counts: Map<String, Int>)

/** الكشوفات التصنيفية — نقل مباشر من الويب: تحليل الأعمدة، مطابقة متعددة الإشارات، ثم تنفيذ يكمّل الفارغ ولا يمحو. */
object Cat {
    val types = listOf(
        CatType("pregnant", "حوامل", "flag", female = true), CatType("breastfeeding", "مرضعات", "flag", female = true),
        CatType("widows", "أرامل ومطلقات", "head", female = true), CatType("under2", "أطفال دون سنتين", "member", rel = "child"),
        CatType("orphans", "أيتام", "member", rel = "orphan"), CatType("children", "أبناء وأطفال (عام)", "member", rel = "child"),
        CatType("adults18", "أبناء فوق 18", "member", rel = "child"), CatType("elderly", "كبار السن", "member", rel = "other"),
        CatType("disability", "ذوو إعاقة", "flag"), CatType("chronic", "أمراض مزمنة", "flag"), CatType("injured", "جرحى ومصابون", "flag"),
        CatType("martyr", "أسر شهداء", "family"), CatType("custom", "تصنيف مخصص", "flag"),
    )
    val by = types.associateBy { it.key }

    val roles = linkedMapOf("sName" to "اسم الشخص", "sNid" to "هوية الشخص", "dob" to "تاريخ الميلاد", "gName" to "اسم الوصي/ولي الأمر/الزوج", "gNid" to "هوية الوصي/ولي الأمر",
        "gRel" to "صلة الوصي", "phone" to "جوال", "phone2" to "جوال 2", "marital" to "الحالة الاجتماعية", "due" to "موعد الولادة المتوقع", "pregStart" to "بداية الحمل",
        "count" to "عدد الأفراد", "details" to "التفاصيل/النوع", "orphanType" to "نوع اليتم", "gender" to "الجنس", "extra" to "حقل إضافي", "skip" to "تجاهل")
    val statusLabels = mapOf("update" to "حُدِّث", "add" to "أُضيف لأسرته", "create" to "أُنشئ رب أسرة", "same" to "مطابق بلا تغيير", "unmatched" to "غير متطابق",
        "amb" to "أكثر من احتمال", "dup" to "مكرر بالملف", "other_camp" to "مسجّل بمخيم آخر")

    private fun n(s: String?) = Cells.norm(s.orEmpty())
    private fun digits(s: String) = Nid.digits(s).replace(Regex("\\D"), "")
    fun phoneKey(v: String?): String = digits(v.orEmpty()).let { if (it.length >= 9) it.takeLast(9) else "" }
    private fun tokens(s: String) = Cells.nameKey(s).split(' ').filter { it.isNotEmpty() }
    private fun famKey(p: JSONObject) = p.optString("familyId").let { if (it.isNotEmpty()) p.optString("campId") + "|" + it else "ind|" + p.optString("id") }
    private fun has(p: JSONObject, k: String) = p.optString(k).let { it.isNotEmpty() && it != "null" }
    private fun sequence(s: String) = Regex("^#$|^الرقم$|^رقم متسلسل$|^رقم تسلسلي$|^م$|^ر\\.?م$|^no\\.?$|^index$").containsMatchIn(s)

    fun orphanType(v: String?): Pair<String?, Boolean> {
        val s = n(v); if (s.isEmpty()) return null to false
        val sole = Regex("ناجي|وحيد").containsMatchIn(s)
        return (if (sole || Regex("ابوين|الوالدين|كلاهما|اب وام|الاب والام").containsMatchIn(s)) "both" else "father") to sole
    }

    private class Prof(val n: Int, val nid: Double, val phone: Double, val date: Double, val future: Double, val name: Double, val marital: Double, val small: Double)

    private fun profile(rows: List<List<String>>, i: Int): Prof {
        var c = 0; var nid = 0; var phone = 0; var date = 0; var future = 0; var name = 0; var marital = 0; var small = 0
        for (r in rows.take(80)) {
            val cell = Cells.clean(r.getOrNull(i)); if (cell.isEmpty()) continue
            c++
            val d = digits(cell)
            if (Regex("^0?5[6-9]\\d{7}$").matches(d) && d.length == cell.replace(Regex("[\\s\\-]"), "").length) phone++
            else if (Nid.status(cell) == "valid") nid++
            val dateLike = Regex("[/\\-.]").containsMatchIn(cell) || cell.toDoubleOrNull()?.let { it > 20000 && it < 80000 } == true
            val dt = if (dateLike) Cells.date(cell) else null
            if (dt != null) { date++; if (dt > java.time.LocalDate.now().toString()) future++ }
            if (Regex("[؀-ۿ]").containsMatchIn(cell) && !Regex("\\d").containsMatchIn(cell) && cell.trim().split(Regex("\\s+")).size >= 2) name++
            if (Cells.maritalWord(cell) != null) marital++
            if (Regex("^\\d{1,2}$").matches(cell)) small++
        }
        fun r(x: Int) = if (c > 0) x.toDouble() / c else 0.0
        return Prof(c, r(nid), r(phone), r(date), r(future), r(name), r(marital), r(small))
    }

    /** analyzeCatColumns: دور لكل عمود من العنوان ومحتواه. */
    fun analyze(headers: List<String>, rows: List<List<String>>, key: String): MutableList<String> {
        val cat = by[key] ?: by["custom"]!!
        val headish = cat.kind == "head" || cat.kind == "family"
        val guardRe = if (headish) Regex("ولي|وصي|والد|المعيل|الكفيل") else Regex("ولي|وصي|(^|\\s)(ال)?(اب|ام)(\\s|$)|والد|(^|\\s)(ال)?زوج(\\s|$)|رب (ال)?اسره|رب (ال)?عائله|المعيل|الكفيل")
        val subjRe = Regex("طفل|يتيم|حامل|مرضع|المراه|ارمل|مستفيد|الفرد|مصاب|جريح|مريض|الشخص|المسن")
        val map = MutableList(headers.size) { "extra" }
        headers.forEachIndexed { i, h ->
            val nm = n(h); val p = profile(rows, i)
            if (p.n == 0 || sequence(nm)) { map[i] = "skip"; return@forEachIndexed }
            val g = guardRe.containsMatchIn(nm)
            fun d(re: String) = Regex(re).containsMatchIn(nm)
            map[i] = when {
                d("جنس") -> "gender"
                d("حاله اجتماعيه|الحاله الاجتماعيه") || p.marital > .6 -> "marital"
                d("نوع اليتم|اليتم") -> "orphanType"
                d("صله|القرابه") -> "gRel"
                d("جوال|هاتف|تواصل|موبايل|واتس") || (p.phone > .6 && !d("هويه")) -> "phone"
                d("هويه|بطاقه|رقم وطني") || p.nid > .6 -> if (g) "gNid" else "sNid"
                d("عدد") || p.small > .8 -> "count"
                key == "pregnant" && d("ولاده|الوضع|متوقع") -> "due"
                d("حمل") && p.date > .5 -> "pregStart"
                d("ميلاد|تاريخ") || p.date > .6 -> if (key == "pregnant" && p.future > .3) "due" else "dob"
                d("شهيد|ملاحظ") -> "details"
                d("نوع|تفاصيل|وصف|تشخيص|المرض|الاعاقه|الاصابه") && p.name < .9 -> "details"
                d("اسم") || p.name > .6 -> if (g && !(headish && d("رب"))) "gName" else "sName"
                else -> "extra"
            }
        }
        fun first(r: String) = map.indexOf(r)
        var ph = 0
        map.forEachIndexed { i, r -> if (r == "phone") { ph++; if (ph == 2) map[i] = "phone2" else if (ph > 2) map[i] = "extra" } }
        val sNames = map.indices.filter { map[it] == "sName" }
        if (sNames.size > 1) {
            val two = first("gName") < 0 && !headish
            if (two) map[sNames[1]] = "gName"
            sNames.drop(if (two) 2 else 1).forEach { map[it] = "extra" }
        }
        map.indices.forEach { i ->
            if (map[i] != "sNid" || subjRe.containsMatchIn(n(headers[i]))) return@forEach
            var j = i - 1; while (j >= 0 && map[j] != "sName" && map[j] != "gName") j--
            if (j >= 0 && map[j] == "gName" && first("gNid") < 0) map[i] = "gNid"
        }
        val sNids = map.indices.filter { map[it] == "sNid" }
        if (sNids.size > 1) { if (first("gNid") < 0 && first("gName") >= 0) map[sNids[1]] = "gNid"; sNids.drop(2).forEach { map[it] = "extra" } }
        listOf("dob", "due", "pregStart", "marital", "count", "gender", "orphanType", "gRel", "gName", "gNid").forEach { r -> map.indices.filter { map[it] == r }.drop(1).forEach { map[it] = "extra" } }
        if (first("sName") < 0) { val k = map.indices.firstOrNull { map[it] == "extra" && profile(rows, it).name > .5 }; if (k != null) map[k] = "sName" }
        return map
    }

    /** detectCatFromTable: تخمين نوع الكشف لاقتراحه بدل الاستيراد العادي. */
    fun detect(headers: List<String>, rows: List<List<String>>): String? {
        val all = n(headers.filter { !Regex("عدد|اعداد").containsMatchIn(n(it)) }.joinToString(" | "))
        fun d(re: String) = Regex(re).containsMatchIn(all)
        if (d("يتيم|وصي")) return "orphans"
        if (d("حامل|الحمل")) return "pregnant"
        if (d("مرضع")) return "breastfeeding"
        if (d("شهيد")) return "martyr"
        if (d("جريح|مصاب|اصابه")) return "injured"
        if (d("اعاقه") && !d("زوجه")) return "disability"
        if (d("مزمن") && !d("زوجه")) return "chronic"
        if (d("مسن|كبار السن")) return "elderly"
        if (d("ارمل|مطلق")) return "widows"
        val mi = headers.indexOfFirst { Regex("حاله اجتماعيه").containsMatchIn(n(it)) }
        if (mi >= 0 && !d("زوجه")) {
            val vals = rows.take(60).map { n(it.getOrNull(mi)) }.filter { it.isNotEmpty() }
            if (vals.isNotEmpty() && vals.count { Regex("ارمل|مطلق|منفصل").containsMatchIn(it) }.toDouble() / vals.size > .7) return "widows"
        }
        if (d("اسم (ال)?طفل|اسم (ال)?ابن|اسم المولود") && !d("زوجه")) {
            val di = headers.indexOfFirst { Regex("ميلاد").containsMatchIn(n(it)) }
            if (di >= 0) {
                val now = java.time.LocalDate.now()
                val ages = rows.take(80).mapNotNull { Cells.date(it.getOrNull(di)) }.mapNotNull { runCatching { java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.parse(it), now) / 365.25 }.getOrNull() }
                if (ages.isNotEmpty() && ages.count { it < 2.5 }.toDouble() / ages.size > .8) return "under2"
                if (ages.isNotEmpty() && ages.count { it >= 18 }.toDouble() / ages.size > .8) return "adults18"
            }
            return "children"
        }
        return null
    }

    fun rowValues(row: List<String>, map: List<String>, headers: List<String>): CatRow {
        val v = HashMap<String, String>(); val ex = LinkedHashMap<Int, String>()
        map.forEachIndexed { i, role ->
            if (role == "skip") return@forEachIndexed
            val raw = row.getOrNull(i).orEmpty(); val c = Cells.clean(raw); if (c.isEmpty()) return@forEachIndexed
            when (role) {
                "extra" -> ex[i] = c
                "sNid", "gNid" -> v[role] = Nid.norm(c)
                "dob", "due", "pregStart" -> Cells.date(raw)?.let { v[role] = it }
                "phone", "phone2" -> v[role] = digits(c).replace(Regex("^(5\\d{8})$"), "0$1")
                else -> v[role] = c
            }
        }
        return CatRow(v, ex, headers)
    }

    class Index(val all: List<JSONObject>) {
        val byNid = HashMap<String, JSONObject>(); val byName = HashMap<String, MutableList<JSONObject>>(); val byPhone = HashMap<String, MutableList<JSONObject>>()
        val byLast = HashMap<String, MutableList<JSONObject>>(); val fam = HashMap<String, MutableList<JSONObject>>()
        init {
            fun <T> MutableMap<String, MutableList<T>>.push(k: String, x: T) { if (k.isNotEmpty()) getOrPut(k) { ArrayList() }.add(x) }
            for (p in all) {
                val nid = Nid.norm(p.optString("nationalId")); if (nid.isNotEmpty() && nid !in byNid) byNid[nid] = p
                byName.push(Cells.nameKey(p.optString("fullName")), p)
                tokens(p.optString("fullName")).lastOrNull()?.let { byLast.push(it, p) }
                if (p.optBoolean("isFamilyHead")) {
                    byPhone.push(phoneKey(p.optString("phone")), p)
                    val p2 = p.optJSONObject("customFields")?.optString("الجوال 2").orEmpty()
                    if (phoneKey(p2) != phoneKey(p.optString("phone"))) byPhone.push(phoneKey(p2), p)
                }
                fam.push(famKey(p), p)
            }
        }
    }

    private fun famHead(ix: Index, p: JSONObject): JSONObject? = ix.fam[famKey(p)].orEmpty().firstOrNull { it.optBoolean("isFamilyHead") } ?: p.takeIf { it.optBoolean("isFamilyHead") }
    private fun probe(name: String, dob: String? = null) = JSONObject().put("fullName", name).also { if (dob != null) it.put("birthDate", dob) }

    private class Found(val p: JSONObject?, val how: String = "", val amb: List<JSONObject> = emptyList())

    private fun findSubject(ix: Index, R: CatRow): Found? {
        if (R.s("sNid").isNotEmpty()) ix.byNid[R.s("sNid")]?.let { return Found(it, "رقم الهوية") }
        val name = R.s("sName"); if (name.isEmpty()) return null
        val sn = R.s("sNid")
        fun ok(p: JSONObject) = sn.isEmpty() || Nid.norm(p.optString("nationalId")).isEmpty() || Nid.norm(p.optString("nationalId")) == sn
        var c = ix.byName[Cells.nameKey(name)].orEmpty().filter { ok(it) }
        if (c.size > 1 && R.s("dob").isNotEmpty()) c.filter { it.optString("birthDate") == R.s("dob") }.takeIf { it.isNotEmpty() }?.let { c = it }
        if (c.size == 1) return Found(c[0], "الاسم")
        if (c.size > 1) return Found(null, amb = c)
        val t = tokens(name); var best: JSONObject? = null; var bs = 0.0; var second = 0.0
        for (p in ix.byLast[t.lastOrNull()].orEmpty()) {
            if (!ok(p)) continue
            val sc = Similar.score(probe(name, R.s("dob").ifEmpty { null }), p)
            if (sc > bs) { second = bs; bs = sc; best = p } else if (sc > second) second = sc
        }
        return if (bs >= 0.92 && bs - second >= 0.04) Found(best, "تشابه الاسم") else null
    }

    private class Cand(var score: Int = 0, val why: LinkedHashSet<String> = LinkedHashSet(), var via: JSONObject)
    private class Fam(val via: JSONObject, val head: JSONObject?, val why: Set<String>, val amb: List<JSONObject> = emptyList())

    /** catFindFamily: إشارات مستقلة بأوزان — هوية الوصي > اسمه > الجوال > تسلسل الاسم. */
    private fun findFamily(ix: Index, R: CatRow): Fam? {
        val cand = LinkedHashMap<String, Cand>()
        fun add(p: JSONObject?, sc: Int, why: String) { if (p == null) return; val c = cand.getOrPut(famKey(p)) { Cand(via = p) }; c.score += sc; c.why += why; if (sc >= 60) c.via = p }
        if (R.s("gNid").isNotEmpty()) add(ix.byNid[R.s("gNid")], 100, "هوية الوصي")
        if (R.s("gName").isNotEmpty()) {
            val gs = ix.byName[Cells.nameKey(R.s("gName"))].orEmpty()
            if (gs.size == 1) add(gs[0], 60, "اسم الوصي")
            else if (gs.size > 1) gs.forEach { add(it, 25, "اسم الوصي") }
            else {
                val t = tokens(R.s("gName"))
                val hits = ix.byLast[t.lastOrNull()].orEmpty().filter { Similar.score(probe(R.s("gName")), it) >= 0.9 }
                if (hits.size == 1) add(hits[0], 45, "اسم الوصي") else hits.forEach { add(it, 15, "اسم الوصي") }
            }
        }
        val pk = phoneKey(R.s("phone"))
        if (pk.isNotEmpty()) { val hs = ix.byPhone[pk].orEmpty(); if (hs.size == 1) add(hs[0], 40, "الجوال") else hs.forEach { add(it, 15, "الجوال") } }
        val t = tokens(R.s("sName"))
        if (t.size >= 3) {
            val hits = ix.byLast[t.last()].orEmpty().filter { p ->
                if (p.optString("gender") == "female") return@filter false
                val u = tokens(p.optString("fullName"))
                u.size >= 2 && u[0] == t[1] && (t.size < 4 || u[1] == t[2])
            }
            if (hits.size == 1) add(hits[0], 35, "تسلسل الاسم") else hits.forEach { add(it, 10, "تسلسل الاسم") }
        }
        val list = cand.values.sortedByDescending { it.score }
        if (list.isEmpty()) return null
        val top = list[0]; val second = list.getOrNull(1)
        if (top.score >= 100 && (second == null || second.score < 100)) return Fam(top.via, famHead(ix, top.via), top.why)
        if (top.score >= 35 && (second == null || top.score - second.score >= 25)) return Fam(top.via, famHead(ix, top.via), top.why)
        return Fam(top.via, null, emptySet(), list.take(3).map { famHead(ix, it.via) ?: it.via })
    }

    private fun findInFamily(members: List<JSONObject>, name: String, nid: String): JSONObject? {
        if (nid.isNotEmpty()) members.firstOrNull { Nid.norm(it.optString("nationalId")) == nid }?.let { return it }
        if (name.isEmpty()) return null
        val k = Cells.nameKey(name)
        members.filter { Cells.nameKey(it.optString("fullName")) == k }.singleOrNull()?.let { return it }
        var best: JSONObject? = null; var bs = 0.0
        for (p in members) { if (nid.isNotEmpty() && Nid.norm(p.optString("nationalId")).isNotEmpty() && Nid.norm(p.optString("nationalId")) != nid) continue; val sc = Similar.score(probe(name), p); if (sc > bs) { bs = sc; best = p } }
        return if (bs >= 0.9) best else null
    }

    /** أثر الكشف على سجل الفرد: يكمّل الفارغ ويرفع الأعلام ويضيف التصنيف ولا يمحو بيانات قائمة. true إن تغيّر شيء. */
    fun effect(cat: CatType, p: JSONObject, R: CatRow, customLabel: String): Boolean {
        var ch = false
        fun set(k: String, v: Any) { if (p.opt(k) != v) { p.put(k, v); ch = true } }
        fun fill(k: String, v: String) { if (v.isNotEmpty() && !has(p, k)) { p.put(k, v); ch = true } }
        fun cf(): JSONObject = p.optJSONObject("customFields") ?: JSONObject().also { p.put("customFields", it) }
        fun addTag(t: String, data: Map<String, Any?>) {
            val tags = p.optJSONArray("tags") ?: JSONArray()
            if ((0 until tags.length()).none { tags.optString(it) == t }) { tags.put(t); p.put("tags", tags); ch = true }
            val td = p.optJSONObject("tagData") ?: JSONObject()
            val cur = td.optJSONObject(t) ?: JSONObject(); val nx = JSONObject(cur.toString())
            data.forEach { (k, v) -> if (v != null && v != "") nx.put(k, v) }
            if (nx.toString() != cur.toString()) { td.put(t, nx); p.put("tagData", td); ch = true }
        }
        if (R.s("dob").isNotEmpty() && !has(p, "birthDate")) { p.put("birthDate", R.s("dob")); Stats.ageOfDob(R.s("dob"))?.let { p.put("calculatedAge", it) }; ch = true }
        if (R.s("sNid").isNotEmpty() && Nid.norm(p.optString("nationalId")).isEmpty()) { p.put("nationalId", R.s("sNid")); ch = true }
        if (p.optString("gender").let { it.isEmpty() || it == "unknown" }) {
            val g = if (cat.female) "female" else Cells.gender(R.s("gender").ifEmpty { null }, R.s("marital"), p.optString("fullName"))
            if (g != "unknown") { p.put("gender", g); ch = true }
        }
        if (p.optBoolean("isFamilyHead")) {
            fill("phone", R.s("phone"))
            if (R.s("phone2").isNotEmpty() && R.s("phone2") != p.optString("phone") && cf().optString("الجوال 2").isEmpty()) { cf().put("الجوال 2", R.s("phone2")); ch = true }
        }
        when (cat.key) {
            "pregnant" -> {
                set("isPregnant", true)
                if (R.s("due").isNotEmpty() || R.s("pregStart").isNotEmpty()) {
                    val cur = p.optJSONObject("pregnancy") ?: JSONObject(); val nx = JSONObject(cur.toString())
                    if (R.s("due").isNotEmpty()) nx.put("due", R.s("due")); if (R.s("pregStart").isNotEmpty()) nx.put("start", R.s("pregStart"))
                    if (nx.toString() != cur.toString()) { p.put("pregnancy", nx); ch = true }
                }
            }
            "breastfeeding" -> set("isBreastfeeding", true)
            "widows" -> {
                val mw = Cells.maritalWord(R.s("marital"))
                if (mw != null) { if (p.optString("maritalStatus") != mw.first) { p.put("maritalStatus", mw.first); ch = true } }
                else if (!Regex("ارمل|مطلق|منفصل").containsMatchIn(n(p.optString("maritalStatus")))) { p.put("maritalStatus", "أرمل/ة"); ch = true }
                if (R.s("count").isNotEmpty() && cf().optString("عدد الأفراد (كشف)").isEmpty()) { cf().put("عدد الأفراد (كشف)", R.s("count")); ch = true }
            }
            "disability" -> { set("hasDisability", true); fill("disabilityDetails", R.s("details")) }
            "chronic" -> { set("hasChronicDisease", true); fill("diseaseDetails", R.s("details")) }
            "injured" -> addTag("injured", mapOf("details" to R.s("details")))
            "martyr" -> addTag("martyr_family", mapOf("details" to R.s("details")))
            "orphans" -> {
                val ot = orphanType(R.s("orphanType").ifEmpty { null })
                addTag("orphan", mapOf("type" to (ot.first ?: p.optJSONObject("tagData")?.optJSONObject("orphan")?.optString("type")?.ifEmpty { null } ?: "father"),
                    "soleSurvivor" to (if (ot.second) true else null), "guardianName" to R.s("gName"), "guardianNid" to R.s("gNid"), "guardianRel" to R.s("gRel"),
                    "guardianPersonId" to R.gid, "guardianPhone" to R.s("phone")))
            }
            "custom" -> if (customLabel.isNotEmpty()) addTag("c:$customLabel", if (R.s("details").isNotEmpty()) mapOf("details" to R.s("details")) else emptyMap())
        }
        if (R.extra.isNotEmpty()) {
            var any = false
            R.extra.forEach { (i, v) -> val k = R.headers.getOrNull(i).orEmpty().ifEmpty { "عمود ${i + 1}" }; if (cf().optString(k).isEmpty()) { cf().put(k, v); any = true } }
            if (any) ch = true
        }
        return ch
    }

    private fun needsChange(cat: CatType, p: JSONObject, R: CatRow, label: String) = effect(cat, JSONObject(p.toString()), R, label)

    /** بناء الخطة بلا كتابة. persons يجب أن تكون نسخاً عميقة (التنفيذ يعدّلها). cross يفحص هوية جديدة في مخيم آخر ويعيد اسم المخيم أو null. */
    fun plan(rows: List<List<String>>, headers: List<String>, map: List<String>, catKey: String, customLabel: String, camp: String, persons: List<JSONObject>, cross: ((String) -> String?)? = null): Pair<List<CatItem>, Index> {
        val cat = by.getValue(catKey)
        val ix = Index(persons); val seen = HashSet<String>(); val plan = ArrayList<CatItem>()
        var i = 0
        for (row in rows) {
            i++
            val R = rowValues(row, map, headers)
            if (R.s("sName").isEmpty() && R.s("sNid").isEmpty()) continue
            fun item(status: String, reason: String = "", how: String = "", person: JSONObject? = null, head: JSONObject? = null) = CatItem(i + 1, R.s("sName"), R.s("sNid"), R, status, reason, how, person, head)
            val dk = R.s("sNid").ifEmpty { Cells.nameKey(R.s("sName")) + "|" + R.s("dob") }
            if (!seen.add(dk)) { plan += item("dup", "نفس الشخص مكرر بالملف"); continue }
            var fam: Fam? = null
            if (R.s("gNid").isNotEmpty() || R.s("gName").isNotEmpty()) { fam = findFamily(ix, R); if (fam?.head != null) R.gid = fam.via.optString("id").ifEmpty { fam.head.optString("id") } }
            val s = findSubject(ix, R)
            if (s?.p != null) {
                val p = s.p
                val tgt = if (cat.kind == "family") (famHead(ix, p) ?: p) else p
                plan += item(if (needsChange(cat, tgt, R, customLabel)) "update" else "same", person = tgt, head = if (cat.kind == "family") tgt else famHead(ix, p), how = s.how)
                continue
            }
            if (s != null && s.amb.isNotEmpty()) { plan += item("amb", "أكثر من شخص بنفس الاسم: " + s.amb.take(3).joinToString("، ") { it.optString("fullName") + (it.optString("nationalId").takeIf { x -> x.isNotEmpty() }?.let { x -> " ($x)" } ?: "") }); continue }
            if (cat.kind == "member") {
                if (fam == null) fam = findFamily(ix, R)
                if (fam != null && fam.amb.isNotEmpty()) { plan += item("amb", "أكثر من أسرة محتملة: " + fam.amb.joinToString("، ") { it.optString("fullName") }); continue }
                val head = fam?.head
                if (fam == null || head == null) { plan += item("unmatched", if (R.s("gName").isNotEmpty() || R.s("gNid").isNotEmpty()) "ولي الأمر/الوصي غير موجود بالنظام" else "لا توجد بيانات تدل على أسرته (هوية/اسم ولي الأمر/جوال)"); continue }
                val inFam = findInFamily(ix.fam[famKey(head)] ?: listOf(head), R.s("sName"), R.s("sNid"))
                if (inFam != null) { plan += item(if (needsChange(cat, inFam, R, customLabel)) "update" else "same", person = inFam, head = head, how = "داخل أسرة " + head.optString("fullName")); continue }
                plan += item("add", head = head, how = fam.why.joinToString(" + "))
                continue
            }
            if (cat.kind == "head") { plan += item("create", how = "غير مسجّلة — تصبح رب أسرة"); continue }
            val fh = fam?.head
            if (fh != null) {
                val inFam = findInFamily(ix.fam[famKey(fh)] ?: listOf(fh), R.s("sName"), R.s("sNid"))
                val target = if (cat.kind == "family") fh else inFam
                if (target != null) { plan += item(if (needsChange(cat, target, R, customLabel)) "update" else "same", person = target, head = fh, how = "عبر " + fam.why.joinToString(" + ")); continue }
            }
            plan += item("unmatched", if (R.s("sNid").isNotEmpty()) "رقم الهوية غير موجود بالنظام" else "الاسم غير موجود بالنظام (بلا رقم هوية)")
        }
        if (cross != null) {
            val pool = java.util.concurrent.Executors.newFixedThreadPool(8)
            try {
                plan.filter { (it.status == "add" || it.status == "create") && it.nid.isNotEmpty() }
                    .map { x -> x to pool.submit<String?> { runCatching { cross(x.nid) }.getOrNull() } }
                    .forEach { (x, f) -> f.get()?.let { where -> x.status = "other_camp"; x.other = where } }
            } finally { pool.shutdown() }
        }
        return plan to ix
    }

    private fun blank(cat: CatType, camp: String, fid: String, R: CatRow, now: Long, uid: String, extra: Map<String, Any?>): JSONObject {
        val o = JSONObject().put("id", java.util.UUID.randomUUID().toString()).put("campId", camp).put("createdAt", now).put("createdBy", uid)
            .put("fullName", R.s("sName")).put("nationalId", R.s("sNid")).put("birthDate", R.s("dob").ifEmpty { null } ?: JSONObject.NULL)
            .put("gender", if (cat.female) "female" else Cells.gender(R.s("gender").ifEmpty { null }, R.s("marital"), R.s("sName"))).put("isFamilyHead", false).put("familyId", fid)
            .put("phone", "").put("address", "").put("notes", "").put("maritalStatus", "").put("customFields", JSONObject())
            .put("hasChronicDisease", false).put("chronicConditions", JSONArray()).put("diseaseDetails", "").put("hasDisability", false).put("disabilityDetails", "")
            .put("isPregnant", false).put("isBreastfeeding", false)
        Stats.ageOfDob(R.s("dob"))?.let { o.put("calculatedAge", it) }
        extra.forEach { (k, v) -> if (v != null) o.put(k, v) }
        return o
    }

    /** تنفيذ الخطة على نسخ الأفراد في الفهرس؛ يعيد ما يجب حفظه. */
    fun apply(plan: List<CatItem>, ix: Index, catKey: String, customLabel: String, camp: String, uid: String): CatApplied {
        val cat = by.getValue(catKey); val now = Clock.now()
        val writes = LinkedHashMap<String, JSONObject>(); val created = ArrayList<JSONObject>(); val newHeads = ArrayList<JSONObject>()
        fun mark(p: JSONObject) { p.put("updatedBy", uid); writes[p.optString("id")] = p }
        for (x in plan) when (x.status) {
            "update" -> x.person?.let { if (effect(cat, it, x.row, customLabel)) mark(it) }
            "add" -> {
                val h = x.head ?: continue
                if (!has(h, "familyId")) { h.put("familyId", Nid.norm(h.optString("nationalId")).ifEmpty { h.optString("id") }); mark(h) }
                val members = ix.fam.getOrPut(famKey(h)) { arrayListOf(h) }
                val rel = cat.rel ?: "child"
                val p = blank(cat, h.optString("campId").ifEmpty { camp }, h.optString("familyId"), x.row, now, uid, mapOf("relation" to rel, "address" to h.optString("address"), "orderIndex" to members.size + 1,
                    "relationDetail" to when (rel) { "other" -> if (cat.key == "elderly") "مسن/ة" else ""; "orphan" -> "يتيم في رعاية الأسرة"; else -> null }))
                effect(cat, p, x.row, customLabel)
                members += p; mark(p); created += p; x.person = p
            }
            "create" -> {
                val fid = x.row.s("sNid").ifEmpty { java.util.UUID.randomUUID().toString() }
                val p = blank(cat, camp, fid, x.row, now, uid, mapOf("isFamilyHead" to true, "relation" to "head", "orderIndex" to 0))
                effect(cat, p, x.row, customLabel)
                mark(p); created += p; newHeads += p; x.person = p; x.head = p
            }
        }
        val counts = HashMap<String, Int>(); plan.forEach { counts[it.status] = (counts[it.status] ?: 0) + 1 }
        return CatApplied(writes.values.toList(), created, newHeads, counts)
    }

    /** صفوف تقرير المطابقة. */
    fun reportRows(plan: List<CatItem>): List<List<String>> = plan.map { x ->
        listOf(x.i.toString(), x.name, x.nid, statusLabels[x.status] ?: x.status, (if (x.status == "other_camp") "مسجّل بمخيم ${x.other}" else null) ?: x.reason.ifEmpty { x.how },
            x.head?.optString("fullName").orEmpty(), x.head?.optString("nationalId").orEmpty())
    }
    val reportHeader = listOf("الصف", "الاسم", "رقم الهوية", "النتيجة", "طريقة المطابقة / السبب", "رب الأسرة", "هوية رب الأسرة")
}
