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
import dev.saged.grcs.ui.icons.Fa
import dev.saged.grcs.ui.components.HeaderBtn
import dev.saged.grcs.ui.components.cardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
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
    val camps = remember { data.ctxCamps(auth) }
    var camp by remember { mutableStateOf(startCamp ?: camps.firstOrNull()?.first.orEmpty()) }
    var persons by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    var open by remember { mutableStateOf<String?>(startFamily) }
    var openSel by remember { mutableStateOf("") }
    LaunchedEffect(dev.saged.grcs.ui.shell.ShellState.jump) { dev.saged.grcs.ui.shell.ShellState.jump?.let { (c, fk, pid) -> if (camps.any { it.first == c }) { camp = c; openSel = pid; open = fk }; dev.saged.grcs.ui.shell.ShellState.jump = null } }
    var filter by remember { mutableStateOf(startFilter ?: "head") }
    var sort by remember { mutableStateOf("default") }
    var exportNote by remember { mutableStateOf("") }
    var edit by remember { mutableStateOf<JSONObject?>(null) }
    var pending by remember { mutableIntStateOf(0) }
    var nidErr by remember { mutableStateOf<String?>(null) }
    var openTransfer by remember { mutableStateOf(false) }
    var cats by remember { mutableStateOf(false) }
    var allMode by remember { mutableStateOf(auth.user.role == "admin" && startCamp == null && startFilter != null) }
    var fromAll by remember { mutableStateOf(false) }
    var infos by remember { mutableStateOf<Map<String, dev.saged.grcs.data.CampInfo>>(emptyMap()) }
    var allList by remember { mutableStateOf<List<JSONObject>?>(null) }
    var rev by remember { mutableIntStateOf(0) }
    val canWrite = auth.user.role != "beneficiary"

    LaunchedEffect(dev.saged.grcs.ui.shell.ShellState.newPerson) { if (dev.saged.grcs.ui.shell.ShellState.newPerson) { dev.saged.grcs.ui.shell.ShellState.newPerson = false; if (auth.user.role != "beneficiary") { edit = JSONObject().put("isFamilyHead", true).put("relation", "head") } } }
    LaunchedEffect(allMode) {
        if (allMode) infos = withContext(Dispatchers.IO) { camps.mapNotNull { c -> runCatching { c.first to data.campInfo(auth, c.first) }.getOrNull() }.toMap() }
        if (allMode) allList = withContext(Dispatchers.IO) { camps.flatMap { (cid, _) -> data.people.persons(cid).map { x -> x.put("campId", cid) } } }
    }
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
    BackHandler(edit != null || open != null) { if (edit != null) edit = null else { open = null; if (fromAll) { fromAll = false; allMode = true } } }

    Box(Modifier.fillMaxSize()) {
        val ed = edit
        val fk = open
        when {
            cats -> dev.saged.grcs.ui.camps.CatsPage(auth, data) { cats = false }
            ed != null -> PersonForm(ed, persons, camp, auth.user.id, nidErr, { edit = null; nidErr = null }) { r ->
                scope.launch {
                    val e = withContext(Dispatchers.IO) { data.saveForm(auth, camp, r) }
                    if (e != null) nidErr = e
                    else { nidErr = null; edit = null; rev++; exportNote = if (r.skippedNids > 0) "${r.skippedNids} من أفراد الأسرة بلا رقم هوية — أكمله قريباً" else if (r.isNew) "تمت إضافة الفرد بنجاح" else "تم تحديث البيانات بنجاح" }
                }
            }
            allMode -> Column(Modifier.fillMaxSize()) {
                val matched = remember(allList, filter) { allList?.let { PFilter.apply(filter, it) } }
                AppHeader(PFilter.label(filter), if (matched == null) "جارٍ التحميل..." else "${matched.size} سجل — كل المخيمات", conn = null, mark = Fa.LayerGroup, endBack = onExit, search = true)
                Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(12.dp))
                    Btn("تحميل كشف هذا التصنيف (كل المخيمات)", {
                        scope.launch {
                            val r = withContext(Dispatchers.IO) { data.export.custom("xlsx", "كل المخيمات", matched.orEmpty(), filter, false, data.export.defaultCols + listOf("phone2", "shelter")) }
                            exportNote = if (r.ok) "تم حفظ ${r.count} سجل: ${r.name}" else "تعذّر الحفظ"
                        }
                    }, Modifier.fillMaxWidth(), icon = QIcons.Export, enabled = matched != null)
                    if (exportNote.isNotEmpty()) Text(exportNote, color = p.mute, fontSize = 11.5.sp)
                    Spacer(Modifier.height(8.dp))
                    if (matched != null && matched.isEmpty()) EmptyState(QIcons.People, "لا يوجد سجلات بهذا التصنيف", "جرّب تصنيفاً آخر من الرئيسية")
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(matched.orEmpty().take(300), key = { it.optString("id") }) { m ->
                            val cid = m.optString("campId"); val inf = infos[cid]
                            val fem = m.optString("gender") == "female"; val mal = m.optString("gender") == "male"
                            val gc = if (fem) p.pregnant else if (mal) p.blue else p.gray
                            QCard(Modifier, { camp = cid; fromAll = true; allMode = false; open = famKeyOf(m, emptyList()) }, 12.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(Modifier.size(42.dp).clip(CircleShape).background(gc.copy(alpha = .16f)), contentAlignment = Alignment.Center) { androidx.compose.material3.Icon(if (fem) QIcons.Female else if (mal) QIcons.Male else QIcons.Person, null, Modifier.size(22.dp), tint = gc) }
                                    Column(Modifier.weight(1f)) {
                                        Text(m.optString("fullName").ifEmpty { "—" }, color = p.ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text((m.optString("nationalId").takeIf { it.isNotEmpty() }?.let { "$it · " } ?: "") + m.optString("phone").ifEmpty { "بلا هاتف" }, color = p.mute, fontSize = 12.sp)
                                        Text((camps.firstOrNull { it.first == cid }?.second ?: cid) + " · " + (inf?.supervisor?.ifEmpty { null } ?: "—") + (inf?.supervisorPhone?.takeIf { it.isNotEmpty() }?.let { " — $it" } ?: ""), color = p.mute, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
            else -> Column(Modifier.fillMaxSize()) {
                AppHeader("الأفراد", camps.firstOrNull { it.first == camp }?.second ?: "", conn = null, mark = Fa.Users, search = true) { if (onExit != null) HeaderBtn(Fa.ArrowRight, "العودة للرئيسية", onExit) }
                Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(16.dp))
                SearchBox(q, { q = it }, "بحث بالاسم أو رقم الهوية...")
                Spacer(Modifier.height(12.dp))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PFilter.chips, key = { it.first }) { (k, l) ->
                        val tc = when (k) { "tier:critical" -> p.acc; "tier:high" -> p.tierHigh; "tier:medium" -> p.tierMedium; "tier:stable" -> p.tierStable; else -> null }
                        QChip(l, filter == k, tc, if (k == "tier:critical") androidx.compose.ui.graphics.Color(0xFF2E1F02) else null) { filter = k }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row { Btn("كل التصنيفات والكشوفات", { cats = true }, kind = BtnKind.Ghost, small = true, icon = Fa.LayerGroup) }
                Spacer(Modifier.height(8.dp))
                if (PFilter.chips.none { it.first == filter }) Row(Modifier.padding(bottom = 10.dp).clip(CircleShape).background(p.navy).padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Icon(Fa.Filter, null, Modifier.size(12.dp), tint = androidx.compose.ui.graphics.Color.White)
                    Text(PFilter.label(filter), color = androidx.compose.ui.graphics.Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.size(28.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color.White.copy(alpha = .14f)).clickable { filter = "head" }, contentAlignment = Alignment.Center) { androidx.compose.material3.Icon(Fa.Xmark, "إلغاء الفلتر", Modifier.size(12.dp), tint = androidx.compose.ui.graphics.Color.White) }
                }
                val qq = normDigits(q)
                val byFilter = remember(persons, filter) { PFilter.apply(filter, persons) }
                val matched = if (qq.isEmpty()) byFilter else byFilter.filter { m -> m.optString("fullName").contains(qq) || m.optString("nationalId").contains(qq) || m.optString("phone").contains(qq) }
                val vfam = remember(persons) { runCatching { Vuln.families(persons).associate { it.head.optString("id") to it.result } }.getOrDefault(emptyMap()) }
                val vscore = remember(persons, vfam) { persons.associate { m -> m.optString("id") to (vfam[m.optString("id")]?.score ?: 0) } }
                val shownP = remember(matched, sort) { if (sort == "vuln") matched.sortedWith(compareByDescending<JSONObject> { vscore[it.optString("id")] ?: 0 }.thenByDescending { it.optBoolean("isFamilyHead") }) else matched }
                Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${shownP.size} سجل", color = p.mute, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Box(Modifier.padding(end = 8.dp)) {
                        var menu by remember { mutableStateOf(false) }
                        Row(Modifier.heightIn(min = 38.dp).clip(RoundedCornerShape(10.dp)).background(p.surface).border(1.5.dp, p.line, RoundedCornerShape(10.dp)).clickable { menu = true }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (sort == "vuln") "الأكثر هشاشة أولاً" else "ترتيب الكشف", color = p.ink, fontSize = 13.sp)
                            androidx.compose.material3.Icon(Fa.ChevronDown, null, Modifier.size(10.dp), tint = p.ink)
                        }
                        if (menu) dev.saged.grcs.ui.components.PickSheet("الترتيب", listOf("default" to "ترتيب الكشف", "vuln" to "الأكثر هشاشة أولاً"), sort, { menu = false }) { sort = it }
                    }
                    Btn("تحميل هذا التصنيف", {
                        scope.launch {
                            val r = withContext(Dispatchers.IO) { data.export.custom("xlsx", camps.firstOrNull { it.first == camp }?.second ?: camp, persons, filter, filter == "head", data.export.defaultCols + listOf("phone2", "shelter")) }
                            exportNote = if (r.ok) "تم تحميل كشف التصنيف بنجاح" else "تعذّر تصدير الكشف — حاول مجدداً"
                        }
                    }, kind = BtnKind.Outline, small = true, icon = Fa.Download)
                }
                if (exportNote.isNotEmpty()) Text(exportNote, color = p.mute, fontSize = 11.5.sp)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 2.dp)) {
                    if (shownP.isEmpty()) item { EmptyState(Fa.UserSlash, "لا يوجد سجلات", "جرّب تغيير الفلتر أو أضف فرداً جديداً") }
                    items(shownP.take(300), key = { it.optString("id") }) { m -> PersonRow(m, persons, vfam[m.optString("id")]?.tier.takeIf { auth.user.role != "beneficiary" }, canWrite, { openSel = m.optString("id"); open = famKeyOf(m, persons) }, { if (canWrite) edit = m }) }
                    item { Spacer(Modifier.height(88.dp)) }
                }
                }
            }
        }
        if (fk != null && ed == null && !cats) FamilyScreen(fams.firstOrNull { it.key == fk }, openSel, canWrite, auth.user.role, auth.user.id, { open = null; if (fromAll) { fromAll = false; allMode = true } }, { edit = it }, { remove(it) }, { o -> save(o, fk) }, if (auth.user.role == "admin" && camps.size > 1) ({ openTransfer = true }) else null, { h -> data.credentials(auth, h.optString("nationalId"), camp) }) { m -> withContext(Dispatchers.IO) { dev.saged.grcs.data.Audit.list(auth, camp, 300).filter { it.targetName == m.optString("fullName") }.take(10) } }
        if (openTransfer) TransferDialog(auth, data, camp, camps, open.orEmpty()) { openTransfer = false; open = null; rev++ }
    }
}

private fun famKeyOf(m: JSONObject, all: List<JSONObject>) = m.optString("familyId").ifEmpty { "i_" + m.optString("id") }

/** بطاقة فرد (person-card): صورة الجنس المتدرجة، الاسم وشارة الهشاشة، العمر وأسرة رب الأسرة، مربعات الحالة، وزر التعديل. */
@Composable
private fun PersonRow(m: JSONObject, all: List<JSONObject>, tier: String?, canEdit: Boolean, onOpen: () -> Unit, onEdit: () -> Unit) {
    val p = LocalPalette.current
    val a = Stats.ageOf(m)
    val female = m.optString("gender") == "female"; val male = m.optString("gender") == "male"
    val grad = if (male) listOf(p.blue, Color(0xFF1D4ED8)) else if (female) listOf(Color(0xFFDB2777), Color(0xFF9D174D)) else listOf(p.mute, Color(0xFF475569))
    val head = remember(m) { all.firstOrNull { it.optString("familyId") == m.optString("familyId") && it.optBoolean("isFamilyHead") } }
    val ms = m.optString("maritalStatus")
    val tags = m.optJSONArray("tags")?.let { t -> (0 until t.length()).map { t.optString(it) } }.orEmpty()
    val badges = buildList {
        if (m.optBoolean("isPregnant")) add(Fa.BabyCarriage to p.pregnant)
        if (m.optBoolean("isBreastfeeding")) add(Fa.Baby to p.purple)
        if (m.optBoolean("hasChronicDisease")) add(Fa.NotesMedical to p.warn)
        if (m.optBoolean("hasDisability")) add(Fa.Wheelchair to p.blue)
        if (a != null && a < 1) add(Fa.Baby to p.danger)
        if (ms == "widowed" || ms == "divorced" || ms.contains("أرمل") || ms.contains("مطلق") || ms.contains("مطلّق")) add(Fa.Ring to p.gray)
        if (m.optBoolean("isFamilyHead") && m.optString("phone").isEmpty()) add(Fa.PhoneSlash to p.gray)
        if (m.optJSONArray("requests")?.let { r -> (0 until r.length()).any { r.optJSONObject(it)?.optString("status") == "pending" } } == true) add(Fa.HandHoldingHeart to p.danger)
        if (m.optBoolean("urgentNeed")) add(Fa.TriangleExclamation to p.danger)
        tags.forEach { t -> add(when (t) { "orphan" -> Fa.ChildReaching to p.warn; "injured" -> Fa.UserInjured to p.danger; "martyr_family" -> Fa.Dove to p.gray; else -> Fa.Tag to p.blue }) }
    }
    val shape = RoundedCornerShape(14.dp)
    Row(Modifier.fillMaxWidth().cardShadow().clip(shape).background(p.surface).clickable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(grad)), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(if (female) Fa.Venus else if (male) Fa.Mars else Fa.Question, null, Modifier.size(19.dp), tint = Color.White)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(m.optString("fullName").ifEmpty { "—" }, color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (tier != null && m.optBoolean("isFamilyHead")) TierChip(tier)
            }
            Text((a?.let { "$it سنة" } ?: "العمر غير محدد") + (if (!m.optBoolean("isFamilyHead") && head != null) " · أسرة ${head.optString("fullName")}" else ""), color = p.mute, fontSize = 12.5.sp, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
            if (badges.isNotEmpty()) FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                badges.forEach { (ic, c) -> Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(c), contentAlignment = Alignment.Center) { androidx.compose.material3.Icon(ic, null, Modifier.size(12.dp), tint = Color.White) } }
            }
        }
        if (canEdit) Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(p.surface2).clickable(onClick = onEdit), contentAlignment = Alignment.Center) { androidx.compose.material3.Icon(Fa.Pen, "تعديل", Modifier.size(15.dp), tint = p.head) }
    }
}

/** شارة مستوى الهشاشة بجانب الاسم (tier-chip). */
@Composable
internal fun TierChip(tier: String) {
    val p = LocalPalette.current
    val c = when (tier) { "critical" -> p.acc; "high" -> p.tierHigh; "medium" -> p.tierMedium; else -> p.tierStable }
    Text(Vuln.tiers.firstOrNull { it.first == tier }?.second ?: tier, color = if (tier == "critical") Color(0xFF2E1F02) else c, fontSize = 10.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1,
        modifier = Modifier.padding(start = 6.dp).clip(CircleShape).background(if (tier == "critical") c else c.copy(alpha = .16f)).padding(horizontal = 8.dp, vertical = 1.dp))
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) = AppHeader(title, "", conn = null, onBack = onBack)

@Composable
private fun FamilyScreen(f: Fam?, startSel: String, canWrite: Boolean, role: String, uid: String, onBack: () -> Unit, onEdit: (JSONObject) -> Unit, onDelete: (JSONObject) -> Unit, onSave: (JSONObject) -> Unit, onTransfer: (() -> Unit)?, loadCreds: (JSONObject) -> Pair<String, String>?, loadAudit: suspend (JSONObject) -> List<dev.saged.grcs.data.AuditEntry>) {
    var selId by remember(startSel) { mutableStateOf(startSel) }
    var aidFor by remember { mutableStateOf<JSONObject?>(null) }
    var tagsFor by remember { mutableStateOf<JSONObject?>(null) }
    val p = LocalPalette.current
    var confirm by remember { mutableStateOf<JSONObject?>(null) }
    if (f == null) return
    val cur = f.members.firstOrNull { it.optString("id") == selId } ?: f.head ?: f.members.first()
    val tg = tagsFor; val ad = aidFor
    when {
        tg != null -> TagsDialog(tg, { tagsFor = null }) { o -> onSave(o); tagsFor = null }
        ad != null -> AidDialog(ad, uid, { aidFor = null }) { o -> onSave(o); aidFor = null }
        confirm != null -> {}
        else -> dev.saged.grcs.ui.components.Sheet(onBack, cur.optString("fullName")) {
            PersonDetailBody(cur, f.members.filter { it.optString("id") != cur.optString("id") }.sortedWith(compareBy({ it.optInt("orderIndex", 9) }, { -(Stats.ageOf(it) ?: 0) })), role, uid,
                { selId = it.optString("id") }, { onEdit(cur) }, { confirm = cur }, onTransfer, onSave, { aidFor = cur }, { tagsFor = cur }, loadCreds, loadAudit)
        }
    }
    confirm?.let { m ->
        val others = if (m.optBoolean("isFamilyHead")) f.members.size - 1 else 0
        dev.saged.grcs.ui.components.ConfirmSheet(if (others > 0) "هذا رب أسرة — سيُحذف هو و$others من أفراد أسرته نهائياً. لا يمكن التراجع." else "سيتم حذف هذا السجل نهائياً ولا يمكن التراجع.", "حذف فرد", "حذف نهائي", onCancel = { confirm = null }) {
            if (m.optBoolean("isFamilyHead")) f.members.forEach(onDelete) else onDelete(m); confirm = null
        }
    }
}
