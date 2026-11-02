package dev.saged.grcs.ui.people

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.Pill
import dev.saged.grcs.ui.components.QCard
import dev.saged.grcs.ui.components.SearchBox
import dev.saged.grcs.ui.icons.QIcons
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import dev.saged.grcs.data.PFilter
import dev.saged.grcs.data.Stats
import dev.saged.grcs.ui.components.EmptyState
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.Aid
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.Merge
import dev.saged.grcs.data.Vuln
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.components.QField
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val relLabels = listOf("wife" to "زوجة", "husband" to "زوج", "son" to "ابن", "daughter" to "ابنة", "orphan" to "يتيم/ة", "other" to "آخر")
private val maritalLabels = listOf("متزوج/ة", "أرمل/ة", "مطلق/ة", "أعزب")

private fun normDigits(s: String): String = buildString { for (c in s) append(when (c) { in '٠'..'٩' -> '0' + (c - '٠'); in '۰'..'۹' -> '0' + (c - '۰'); else -> c }) }.trim()

private class Fam(val key: String, val head: JSONObject?, val members: List<JSONObject>)

private fun group(list: List<JSONObject>): List<Fam> =
    list.groupBy { it.optString("familyId").ifEmpty { "i_" + it.optString("id") } }.map { (k, v) ->
        Fam(k, v.firstOrNull { it.optBoolean("isFamilyHead") || it.optString("relation") == "head" }, v)
    }.sortedBy { it.head?.optString("fullName") ?: it.members.first().optString("fullName") }

/** الأسر والأفراد: قائمة، تفصيل أسرة، إضافة/تعديل/حذف. التعديل محلي فوري ويُرفع في الخلفية. */
@Composable
fun PeopleScreen(auth: AuthState, data: DataRepo, startCamp: String? = null, startFilter: String? = null, startFamily: String? = null, onExit: (() -> Unit)? = null) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val camps = remember { data.campList(auth) }
    var camp by remember { mutableStateOf(startCamp ?: camps.firstOrNull()?.first.orEmpty()) }
    var persons by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    var open by remember { mutableStateOf<String?>(startFamily) }
    var filter by remember { mutableStateOf(startFilter ?: "head") }
    var sort by remember { mutableStateOf("default") }
    var exportNote by remember { mutableStateOf("") }
    var edit by remember { mutableStateOf<JSONObject?>(null) }
    var pending by remember { mutableIntStateOf(0) }
    var nidErr by remember { mutableStateOf<String?>(null) }
    var openTransfer by remember { mutableStateOf(false) }
    var rev by remember { mutableIntStateOf(0) }
    val canWrite = auth.user.role != "beneficiary"

    LaunchedEffect(camp, rev) {
        if (camp.isEmpty()) return@LaunchedEffect
        persons = withContext(Dispatchers.IO) { data.people.persons(camp) }
        pending = withContext(Dispatchers.IO) { data.people.pendingCount(camp) }
    }

    fun save(o: JSONObject, famKey: String?, extras: List<JSONObject> = emptyList()) {
        scope.launch {
            o.put("updatedBy", auth.user.id)
            if (o.optString("createdBy").isEmpty()) o.put("createdBy", auth.user.id)
            val n = o.optString("nationalId")
            if (n.isNotEmpty()) {
                val bad = withContext(Dispatchers.IO) { data.checkNid(auth, n, camp) }
                if (bad != null) { nidErr = bad; return@launch }
            }
            nidErr = null
            for (x in extras) {
                val xn = x.optString("nationalId")
                if (xn.isNotEmpty()) {
                    val bad = withContext(Dispatchers.IO) { data.checkNid(auth, xn, camp) }
                    if (bad != null) { nidErr = bad; return@launch }
                }
            }
            val saved = withContext(Dispatchers.IO) { data.people.save(camp, o) }
            withContext(Dispatchers.IO) { data.afterSave(auth, camp, saved, false) }
            for (x in extras) {
                x.put("updatedBy", auth.user.id)
                if (x.optString("createdBy").isEmpty()) x.put("createdBy", auth.user.id)
                x.put("familyId", saved.optString("familyId"))
                val sx = withContext(Dispatchers.IO) { data.people.save(camp, x) }
                withContext(Dispatchers.IO) { data.afterSave(auth, camp, sx, false) }
            }
            rev++; edit = null
            open = famKey ?: open
            withContext(Dispatchers.IO) { data.pushNow(auth, camp) }
            rev++
        }
    }

    fun remove(o: JSONObject) {
        scope.launch {
            withContext(Dispatchers.IO) { runCatching { data.trash.put(auth, camp, "person", o.optString("fullName").ifEmpty { "—" }, listOf(o)) }; data.people.delete(camp, o); data.afterSave(auth, camp, o, true) }
            rev++
            withContext(Dispatchers.IO) { data.pushNow(auth, camp) }
            rev++
        }
    }

    val fams = remember(persons) { group(persons) }
    BackHandler(edit != null || open != null) { if (edit != null) edit = null else open = null }

    Box(Modifier.fillMaxSize()) {
        val ed = edit
        val fk = open
        when {
            ed != null -> EditScreen(ed, persons, nidErr, { edit = null; nidErr = null }) { o, ex -> save(o, o.optString("familyId").ifEmpty { "i_" + o.optString("id") }, ex) }
            fk != null -> FamilyScreen(fams.firstOrNull { it.key == fk }, canWrite, auth.user.id, { open = null }, { edit = it }, { remove(it) }, { o -> save(o, fk) }, if (auth.user.role == "admin" && camps.size > 1) ({ openTransfer = true }) else null) { h -> data.credentials(auth, h.optString("nationalId"), camp) }
            else -> Column(Modifier.fillMaxSize()) {
                AppHeader("الأسر والأفراد", (camps.firstOrNull { it.first == camp }?.second ?: "") + if (pending > 0) " · معلّق $pending" else "", onBack = onExit)
                Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(12.dp))
                if (camps.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { camps.forEach { (id, n) -> QChip(n, id == camp) { camp = id } } }
                }
                Spacer(Modifier.height(8.dp))
                SearchBox(q, { q = it }, "بحث بالاسم أو الهوية أو الجوال")
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PFilter.chips, key = { it.first }) { (k, l) -> QChip(l, filter == k) { filter = k } }
                }
                Spacer(Modifier.height(6.dp))
                val qq = normDigits(q)
                val byFilter = remember(persons, filter) { PFilter.apply(filter, persons) }
                val matched = if (qq.isEmpty()) byFilter else byFilter.filter { m -> m.optString("fullName").contains(qq) || m.optString("nationalId").contains(qq) || m.optString("phone").contains(qq) }
                val vscore = remember(persons, sort) { if (sort == "vuln") Vuln.families(persons).associate { it.head.optString("id") to it.result.score } else emptyMap() }
                val shownP = remember(matched, sort) { if (sort == "vuln") matched.sortedByDescending { vscore[famKeyOf(it, persons)] ?: 0 } else matched }
                val headIds = remember(matched) { matched.map { it.optString("familyId").ifEmpty { "i_" + it.optString("id") } }.toHashSet() }
                val famList = remember(fams, headIds, sort) { fams.filter { it.key in headIds }.let { l -> if (sort == "vuln") l.sortedByDescending { Vuln.compute(it.members).score } else l } }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (filter == "head") "${famList.size} أسرة" else "${shownP.size} سجل", color = p.mute, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    QChip(if (sort == "vuln") "الأكثر هشاشة أولاً" else "ترتيب: الاسم", sort == "vuln") { sort = if (sort == "vuln") "default" else "vuln" }
                }
                Btn("تحميل هذا التصنيف", {
                    scope.launch {
                        val r = withContext(Dispatchers.IO) { data.export.custom("xlsx", camps.firstOrNull { it.first == camp }?.second ?: camp, persons, filter, filter == "head", data.export.defaultCols + listOf("phone2", "shelter")) }
                        exportNote = if (r.ok) "تم حفظ ${r.count} سجل: ${r.name}" else "تعذّر الحفظ"
                    }
                }, Modifier.fillMaxWidth(), kind = BtnKind.Outline, small = true, icon = QIcons.Export)
                if (exportNote.isNotEmpty()) Text(exportNote, color = p.mute, fontSize = 11.5.sp)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (filter == "head") items(famList.take(300), key = { it.key }) { f -> FamRow(f) { open = f.key } }
                    else {
                        if (shownP.isEmpty()) item { EmptyState(QIcons.People, "لا يوجد سجلات", "جرّب تغيير الفلتر أو البحث") }
                        items(shownP.take(300), key = { it.optString("id") }) { m -> PersonRow(m, persons, { open = famKeyOf(m, persons) }, { if (canWrite) edit = m }) }
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
                if (canWrite) Btn("أسرة جديدة", { edit = JSONObject().put("isFamilyHead", true).put("relation", "head") }, Modifier.fillMaxWidth().padding(vertical = 10.dp), icon = QIcons.Plus)
                }
            }
        }
        if (openTransfer) TransferDialog(auth, data, camp, camps, open.orEmpty()) { openTransfer = false; open = null; rev++ }
    }
}

private fun famKeyOf(m: JSONObject, all: List<JSONObject>) = m.optString("familyId").ifEmpty { "i_" + m.optString("id") }

/** بطاقة فرد: صورة الجنس، الاسم والعمر وأسرة رب الأسرة، شارات الحالة، وزر التعديل. */
@Composable
private fun PersonRow(m: JSONObject, all: List<JSONObject>, onOpen: () -> Unit, onEdit: () -> Unit) {
    val p = LocalPalette.current
    val a = Stats.ageOf(m)
    val female = m.optString("gender") == "female"; val male = m.optString("gender") == "male"
    val gc = if (female) p.purple else if (male) p.blue else p.gray
    val head = remember(m) { all.firstOrNull { it.optString("familyId") == m.optString("familyId") && it.optBoolean("isFamilyHead") } }
    val badges = buildList {
        if (m.optBoolean("isPregnant")) add("حامل" to p.pregnant)
        if (m.optBoolean("isBreastfeeding")) add("مرضعة" to p.purple)
        if (m.optBoolean("hasChronicDisease")) add("مزمن" to p.warn)
        if (m.optBoolean("hasDisability")) add("إعاقة" to p.blue)
        if (a != null && a < 1) add("رضيع" to p.danger)
        if (m.optString("maritalStatus").let { it.contains("أرمل") || it.contains("مطلق") }) add((if (m.optString("maritalStatus").contains("أرمل")) "أرمل/ة" else "مطلّق/ة") to p.gray)
        if (m.optString("phone").isEmpty()) add("بلا هاتف" to p.gray)
        if (m.optBoolean("urgentNeed")) add("طارئ" to p.danger)
    }
    QCard(Modifier, onOpen, 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(gc.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(if (female) QIcons.Female else if (male) QIcons.Male else QIcons.Person, null, Modifier.size(22.dp), tint = gc)
            }
            Column(Modifier.weight(1f)) {
                Text(m.optString("fullName"), color = p.ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text((a?.let { "$it سنة" } ?: "العمر غير محدد") + (if (!m.optBoolean("isFamilyHead") && head != null) " · أسرة ${head.optString("fullName")}" else ""), color = p.mute, fontSize = 12.sp, maxLines = 1)
                if (badges.isNotEmpty()) FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { badges.forEach { (t, c) -> Pill(t, c) } }
            }
            Box(Modifier.size(44.dp).clickable(onClick = onEdit), contentAlignment = Alignment.Center) { androidx.compose.material3.Icon(QIcons.Edit, "تعديل", Modifier.size(20.dp), tint = p.mute) }
        }
    }
}

@Composable
private fun FamRow(f: Fam, onClick: () -> Unit) {
    val p = LocalPalette.current
    val h = f.head ?: f.members.first()
    val r = remember(f.members) { Vuln.compute(f.members) }
    val tc = when (r.tier) { "critical" -> p.acc; "high" -> p.tierHigh; "medium" -> p.tierMedium; else -> p.tierStable }
    QCard(Modifier, onClick, 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(tc.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                Text(h.optString("fullName").trim().take(1), color = tc, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column(Modifier.weight(1f)) {
                Text(h.optString("fullName"), color = p.ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(h.optString("phone").ifEmpty { "بلا جوال" }, color = p.mute, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (r.tier != "stable") Pill(Vuln.tiers.first { it.first == r.tier }.second, if (r.tier == "critical") p.warn else tc)
                Text("${f.members.size} فرد", color = p.pri, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) = AppHeader(title, "", conn = null, onBack = onBack)

@Composable
private fun FamilyScreen(f: Fam?, canWrite: Boolean, uid: String, onBack: () -> Unit, onEdit: (JSONObject) -> Unit, onDelete: (JSONObject) -> Unit, onSave: (JSONObject) -> Unit, onTransfer: (() -> Unit)?, loadCreds: (JSONObject) -> Pair<String, String>?) {
    val scope = rememberCoroutineScope()
    var creds by remember { mutableStateOf<String?>(null) }
    var aidFor by remember { mutableStateOf<JSONObject?>(null) }
    val p = LocalPalette.current
    var confirm by remember { mutableStateOf<JSONObject?>(null) }
    Column(Modifier.fillMaxSize()) {
        TopBar(f?.head?.optString("fullName") ?: "أسرة", onBack)
        if (f == null) { Text("لا توجد بيانات", color = p.mute, modifier = Modifier.padding(24.dp)); return }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(f.members.sortedByDescending { it.optBoolean("isFamilyHead") }, key = { it.optString("id") }) { m ->
                QCard {
                    Text(m.optString("fullName"), color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    val tags = buildList {
                        if (m.optBoolean("isFamilyHead")) add("رب الأسرة") else relLabels.firstOrNull { it.first == m.optString("relation") }?.let { add(it.second) }
                        m.optString("birthDate").takeIf { it.isNotEmpty() }?.let { add(it) }
                        m.optString("nationalId").takeIf { it.isNotEmpty() }?.let { add(it) }
                        if (m.optBoolean("isPregnant")) add("حامل")
                        if (m.optBoolean("hasChronicDisease")) add("مرض مزمن")
                        if (m.optBoolean("hasDisability")) add("إعاقة")
                    }
                    Text(tags.joinToString(" · "), color = p.mute, fontSize = 12.sp)
                    if (m.optBoolean("isFamilyHead")) Aid.requests(m).sortedByDescending { it.optLong("updatedAt") }.forEach { r ->
                        Column(Modifier.fillMaxWidth().padding(top = 8.dp).background(p.surface2, RoundedCornerShape(12.dp)).padding(10.dp)) {
                            Text((Aid.reqTypes.firstOrNull { it.first == r.optString("type") }?.second ?: r.optString("type")) + " — " + (Aid.reqStatus[r.optString("status")] ?: r.optString("status")), color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            if (r.optString("details").isNotEmpty()) Text(r.optString("details"), color = p.mute, fontSize = 12.sp)
                            if (canWrite && r.optString("status") in listOf("pending", "approved")) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val next = if (r.optString("status") == "pending") listOf("approved" to "قبول", "rejected" to "رفض") else listOf("done" to "تم التسليم")
                                next.forEach { (st, l) -> Text(l, color = p.pri, fontSize = 13.sp, modifier = Modifier.heightIn(min = 44.dp).clickable { onSave(Aid.setRequestStatus(m, r.optString("id"), st, uid)) }.padding(vertical = 12.dp, horizontal = 8.dp)) }
                            }
                        }
                    }
                    if (canWrite) FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (m.optBoolean("isFamilyHead") && m.optString("nationalId").isNotEmpty()) Btn("بيانات الدخول", {
                            scope.launch { creds = withContext(Dispatchers.IO) { loadCreds(m) }?.let { "اسم المستخدم: ${it.first}\nكلمة المرور: ${it.second}" } ?: "لا تتوفر بيانات دخول لهذه الأسرة" }
                        }, kind = BtnKind.Outline, icon = QIcons.Key, small = true)
                        Btn("مساعدة", { aidFor = m }, kind = BtnKind.Outline, icon = QIcons.Heart, small = true)
                        Btn("تعديل", { onEdit(m) }, kind = BtnKind.Outline, icon = QIcons.Edit, small = true)
                        Btn("حذف", { confirm = m }, kind = BtnKind.Danger, icon = QIcons.Trash, small = true)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        if (onTransfer != null) Btn("نقل الأسرة لمخيم آخر", onTransfer, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), BtnKind.Outline, QIcons.Transfer)
        if (canWrite) Btn("إضافة فرد", {
            val fid = f.head?.optString("familyId").orEmpty().ifEmpty { f.members.first().optString("familyId") }
            onEdit(JSONObject().put("familyId", fid).put("isFamilyHead", false).put("relation", "son"))
        }, Modifier.fillMaxWidth().padding(16.dp), icon = QIcons.Plus)
    }
    aidFor?.let { m -> AidDialog(m, uid, { aidFor = null }) { o -> onSave(o); aidFor = null } }
    creds?.let { t ->
        Box(Modifier.fillMaxSize().background(p.ink.copy(alpha = .5f)).clickable { creds = null }, contentAlignment = Alignment.Center) {
            Column(Modifier.padding(32.dp).background(p.surface, RoundedCornerShape(20.dp)).padding(20.dp)) {
                Text("بيانات الدخول", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Text(t, color = p.ink, fontSize = 15.sp)
                Text("إغلاق", color = p.pri, modifier = Modifier.heightIn(min = 44.dp).clickable { creds = null }.padding(top = 14.dp))
            }
        }
    }
    confirm?.let { m ->
        Box(Modifier.fillMaxSize().background(p.ink.copy(alpha = .5f)).clickable { confirm = null }, contentAlignment = Alignment.Center) {
            Column(Modifier.padding(32.dp).background(p.surface, RoundedCornerShape(20.dp)).padding(20.dp).clickable(enabled = false) {}) {
                Text("حذف ${m.optString("fullName")}؟", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("حذف", color = p.onPri, modifier = Modifier.heightIn(min = 44.dp).background(p.danger, RoundedCornerShape(12.dp)).clickable { onDelete(m); confirm = null }.padding(horizontal = 24.dp, vertical = 12.dp))
                    Text("إلغاء", color = p.ink, modifier = Modifier.heightIn(min = 44.dp).clickable { confirm = null }.padding(horizontal = 16.dp, vertical = 12.dp))
                }
            }
        }
    }
}

@Composable
internal fun EditScreen(src: JSONObject, all: List<JSONObject>, serverErr: String?, onBack: () -> Unit, onSave: (JSONObject, List<JSONObject>) -> Unit) {
    val p = LocalPalette.current
    val isNew = src.optString("id").isEmpty()
    val head = src.optBoolean("isFamilyHead")
    var name by remember { mutableStateOf(src.optString("fullName")) }
    var nid by remember { mutableStateOf(src.optString("nationalId")) }
    var dob by remember { mutableStateOf(src.optString("birthDate")) }
    var gender by remember { mutableStateOf(src.optString("gender")) }
    var phone by remember { mutableStateOf(src.optString("phone")) }
    var address by remember { mutableStateOf(src.optString("address")) }
    var notes by remember { mutableStateOf(src.optString("notes")) }
    var marital by remember { mutableStateOf(src.optString("maritalStatus")) }
    var rel by remember { mutableStateOf(src.optString("relation")) }
    var preg by remember { mutableStateOf(src.optBoolean("isPregnant")) }
    var chronic by remember { mutableStateOf(src.optBoolean("hasChronicDisease")) }
    var dis by remember { mutableStateOf(src.optBoolean("hasDisability")) }
    var phone2 by remember { mutableStateOf(src.optString("phone2")) }
    var shelter by remember { mutableStateOf(src.optString("shelterType")) }
    var bf by remember { mutableStateOf(src.optBoolean("isBreastfeeding")) }
    var urgent by remember { mutableStateOf(src.optBoolean("urgentNeed")) }
    var diseaseDetails by remember { mutableStateOf(src.optString("diseaseDetails")) }
    var disDetails by remember { mutableStateOf(src.optString("disabilityDetails")) }
    val conds = remember { mutableStateListOf<String>().also { l -> src.optJSONArray("chronicConditions")?.let { a -> for (i in 0 until a.length()) l.add(a.optString(i)) } } }
    val oldSpouse = remember(src) { if (head) all.firstOrNull { it.optString("familyId") == src.optString("familyId") && it.optString("relation") in listOf("wife", "husband", "spouse") } else null }
    var sName by remember { mutableStateOf(oldSpouse?.optString("fullName").orEmpty()) }
    var sNid by remember { mutableStateOf(oldSpouse?.optString("nationalId").orEmpty()) }
    var sDob by remember { mutableStateOf(oldSpouse?.optString("birthDate")?.takeIf { it != "null" }.orEmpty()) }
    var sPreg by remember { mutableStateOf(oldSpouse?.optBoolean("isPregnant") == true) }
    var sBf by remember { mutableStateOf(oldSpouse?.optBoolean("isBreastfeeding") == true) }
    class Kid(var name: String, var gender: String, var dob: String, var rel: String)
    val kids = remember { mutableStateListOf<MutableState<Kid>>() }
    var noId by remember { mutableStateOf(!isNew && src.optString("nationalId").isEmpty()) }
    var err by remember { mutableStateOf<String?>(null) }

    fun validate(): String? {
        val n = normDigits(nid)
        if (name.trim().length < 2) return "أدخل الاسم"
        if (gender.isEmpty()) return "اختر الجنس"
        if (!noId && normDigits(nid).isEmpty()) return "رقم الهوية مطلوب أو فعّل «لا يوجد رقم هوية»"
        if (n.isNotEmpty() && !Regex("^\\d{9}$").matches(n)) return "رقم الهوية 9 أرقام"
        if (n.isNotEmpty() && all.any { it.optString("nationalId") == n && it.optString("id") != src.optString("id") }) return "رقم الهوية مسجّل لشخص آخر"
        val d = normDigits(dob)
        if (d.isNotEmpty() && !Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(d)) return "التاريخ بصيغة 1990-05-21"
        if (phone2.isNotEmpty() && normDigits(phone2).length < 9) return "الجوال الإضافي غير صحيح"
        if (head && sName.isNotBlank()) {
            val sn = normDigits(sNid)
            if (sn.isEmpty()) return "رقم هوية الزوج/ة مطلوب"
            if (!Regex("^\\d{9}$").matches(sn)) return "هوية الزوج/ة 9 أرقام"
            if (sn == normDigits(nid) || all.any { it.optString("nationalId") == sn && it.optString("id") != oldSpouse?.optString("id") }) return "هوية الزوج/ة مسجّلة"
        }
        for (k in kids) {
            val kd = normDigits(k.value.dob)
            if (k.value.name.isBlank()) return "اسم الفرد المضاف مطلوب"
            if (k.value.gender.isEmpty()) return "جنس ${k.value.name} مطلوب"
            if (kd.isNotEmpty() && !Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(kd)) return "تاريخ ${k.value.name} بصيغة 1990-05-21"
        }
        return null
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(if (isNew) (if (head) "أسرة جديدة" else "فرد جديد") else "تعديل", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FormCard("البيانات الأساسية") {
                QField("الاسم الكامل *", name, { name = it })
                QField("رقم الهوية *", nid, { nid = it }, type = KeyboardType.Number)
                Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable { noId = !noId; if (noId) nid = "" }, verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(noId, { noId = it; if (it) nid = "" })
                    Text("لا يوجد رقم هوية بعد (مولود لم تصدر هويته)", color = p.mute, fontSize = 12.sp)
                }
                QField("تاريخ الميلاد (1990-05-21)", dob, { dob = it }, type = KeyboardType.Number)
                val age = dobAge(normDigits(dob))
                Text("العمر المحسوب: " + (age?.let { "$it سنة" } ?: "—"), color = p.mute, fontSize = 12.5.sp)
                Text("الجنس", color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QChip("ذكر", gender == "male") { gender = "male" }
                    QChip("أنثى", gender == "female") { gender = "female" }
                    QChip("غير محدد", gender == "unknown") { gender = "unknown" }
                }
            }
            FormCard("الأسرة") {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("رب الأسرة", color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(head, null, enabled = false)
                }
                if (head) {
                    Text("الحالة الاجتماعية", color = p.mute, fontSize = 12.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { maritalLabels.forEach { m -> QChip(m, marital == m) { marital = m } } }
                    Text("نوع المأوى", color = p.mute, fontSize = 12.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { shelterLabels.forEach { (k, l) -> QChip(l, shelter == k) { shelter = if (shelter == k) "" else k } } }
                } else if (!src.optBoolean("isFamilyHead")) {
                    Text("صلة القرابة برب الأسرة", color = p.mute, fontSize = 12.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { relLabels.forEach { (k, l) -> QChip(l, rel == k) { rel = k } } }
                }
            }
            if (head) {
                FormCard("الزوج / الزوجة (اختياري)") {
                    Text("", color = p.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        QField("الاسم الكامل", sName, { sName = it })
                        QField("رقم الهوية", sNid, { sNid = it }, type = KeyboardType.Number)
                        QField("تاريخ الميلاد (1990-05-21)", sDob, { sDob = it }, type = KeyboardType.Number)
                        val sFemale = gender == "male"
                        if (sName.isNotBlank() && sFemale) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QChip("حامل", sPreg) { sPreg = !sPreg }; QChip("مرضعة", sBf) { sBf = !sBf }
                        }
                    }
                }
                Text("أفراد الأسرة المضافون", color = p.mute, fontSize = 12.sp)
                kids.forEachIndexed { i, st ->
                    val k = st.value
                    QCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            QField("الاسم", k.name, { st.value = Kid(it, k.gender, k.dob, k.rel) })
                            QField("تاريخ الميلاد (1990-05-21)", k.dob, { st.value = Kid(k.name, k.gender, it, k.rel) }, type = KeyboardType.Number)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                QChip("ذكر", k.gender == "male") { st.value = Kid(k.name, "male", k.dob, k.rel) }
                                QChip("أنثى", k.gender == "female") { st.value = Kid(k.name, "female", k.dob, k.rel) }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { relLabels.forEach { (rk, l) -> QChip(l, k.rel == rk) { st.value = Kid(k.name, k.gender, k.dob, rk) } } }
                            Btn("حذف", { kids.removeAt(i) }, kind = BtnKind.Danger)
                        }
                    }
                }
                Btn("إضافة فرد للأسرة", { kids.add(mutableStateOf(Kid("", "", "", "son"))) }, kind = BtnKind.Outline, icon = QIcons.Plus)
            }
            FormCard("التواصل") {
                QField("رقم الهاتف الأساسي", phone, { phone = it }, type = KeyboardType.Phone)
                QField("رقم هاتف إضافي (اختياري)", phone2, { phone2 = it }, type = KeyboardType.Phone)
                if (head) QField("العنوان / الموقع", address, { address = it })
            }
            FormCard("الحالة الصحية") {
                if (gender == "female") ToggleLine("حامل", preg) { preg = it }
                if (gender == "female") ToggleLine("مرضعة", bf) { bf = it }
                ToggleLine("مرض مزمن", chronic) { chronic = it }
                if (chronic) {
                    Text("اختر الحالة/الحالات المرضية", color = p.mute, fontSize = 12.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chronicLabels.forEach { (k, l) -> QChip(l, k in conds) { if (k in conds) conds.remove(k) else conds.add(k) } } }
                    QField("تفاصيل إضافية / أخرى", diseaseDetails, { diseaseDetails = it })
                }
                ToggleLine("إعاقة", dis) { dis = it }
                if (dis) QField("تفاصيل الإعاقة", disDetails, { disDetails = it })
                ToggleLine("حالة طارئة", urgent) { urgent = it }
            }
            FormCard("ملاحظات") { QField("ملاحظات إضافية…", notes, { notes = it }, multiline = true) }
            (err ?: serverErr)?.let { Text(it, color = p.danger, fontSize = 13.sp) }
            Spacer(Modifier.height(8.dp))
        }
        Btn("حفظ", {
            err = validate()
                if (err == null) {
                    val o = Merge.copy(src)
                    val n = normDigits(nid); val d = normDigits(dob)
                    o.put("fullName", name.trim()).put("nationalId", n).put("gender", gender).put("phone", phone.trim())
                        .put("birthDate", if (d.isEmpty()) JSONObject.NULL else d)
                        .put("calculatedAge", dobAge(d) ?: JSONObject.NULL)
                        .put("isPregnant", gender == "female" && preg).put("hasChronicDisease", chronic).put("hasDisability", dis)
                        .put("notes", notes.trim()).put("phone2", normDigits(phone2).trim())
                        .put("isBreastfeeding", gender == "female" && bf).put("urgentNeed", urgent)
                        .put("chronicConditions", org.json.JSONArray(if (chronic) conds.toList() else emptyList<String>()))
                        .put("diseaseDetails", if (chronic) diseaseDetails.trim() else "").put("disabilityDetails", if (dis) disDetails.trim() else "")
                    if (head) {
                        o.put("shelterType", shelter).put("address", address.trim()).put("maritalStatus", marital).put("isFamilyHead", true).put("relation", "head")
                        if (o.optString("familyId").isEmpty()) o.put("familyId", n.ifEmpty { java.util.UUID.randomUUID().toString() })
                    } else if (!o.optBoolean("isFamilyHead")) o.put("relation", rel.ifEmpty { "other" })
                    if (o.optString("id").isEmpty()) o.put("id", java.util.UUID.randomUUID().toString())
                    val fid = o.optString("familyId")
                    val ex = mutableListOf<JSONObject>()
                    if (head && sName.isNotBlank()) {
                        val sd = normDigits(sDob); val sg = if (gender == "male") "female" else if (gender == "female") "male" else ""
                        val sp = if (oldSpouse != null) Merge.copy(oldSpouse) else JSONObject().put("relation", if (gender == "male") "wife" else "husband").put("isFamilyHead", false)
                        sp.put("fullName", sName.trim()).put("nationalId", normDigits(sNid)).put("gender", sg)
                            .put("birthDate", if (sd.isEmpty()) JSONObject.NULL else sd).put("calculatedAge", dobAge(sd) ?: JSONObject.NULL)
                            .put("address", address.trim()).put("maritalStatus", marital)
                            .put("isPregnant", sg == "female" && sPreg).put("isBreastfeeding", sg == "female" && sBf)
                        ex.add(sp)
                    }
                    for (st in kids) {
                        val k = st.value; val kd = normDigits(k.dob)
                        ex.add(JSONObject().put("fullName", k.name.trim()).put("gender", k.gender).put("relation", k.rel).put("isFamilyHead", false)
                            .put("birthDate", if (kd.isEmpty()) JSONObject.NULL else kd).put("calculatedAge", dobAge(kd) ?: JSONObject.NULL)
                            .put("nationalId", "").put("phone", "").put("address", address.trim()).put("maritalStatus", "")
                            .put("isPregnant", false).put("hasChronicDisease", false).put("hasDisability", false).put("notes", ""))
                    }
                    onSave(o, ex)
                }
        }, Modifier.fillMaxWidth().padding(16.dp), icon = QIcons.Check)
    }
}

private val shelterLabels = listOf("tent_ok" to "خيمة سليمة", "tent_damaged" to "خيمة تالفة", "shared" to "مأوى مشترك", "building_damaged" to "مبنى متضرر", "none" to "بلا مأوى")
private val chronicLabels = listOf("heart" to "قلب", "diabetes" to "سكري", "hypertension" to "ضغط الدم", "kidney" to "كلى", "cancer" to "سرطان", "respiratory" to "ربو / تنفسي", "liver" to "كبد", "thyroid" to "غدة درقية", "mental" to "نفسية", "blood" to "أمراض دم", "neuro" to "عصبية", "bone" to "مفاصل / عظام", "other" to "أخرى")

private fun dobAge(d: String): Int? {
    val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").find(d) ?: return null
    val t = java.util.Calendar.getInstance()
    var a = t.get(java.util.Calendar.YEAR) - m.groupValues[1].toInt()
    val dm = t.get(java.util.Calendar.MONTH) + 1 - m.groupValues[2].toInt()
    if (dm < 0 || (dm == 0 && t.get(java.util.Calendar.DAY_OF_MONTH) < m.groupValues[3].toInt())) a--
    return a.coerceAtLeast(0)
}

@Composable
private fun FormCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    QCard {
        Text(title, color = p.navy.takeIf { !p.dark } ?: p.ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun ToggleLine(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(on, onChange)
    }
}
