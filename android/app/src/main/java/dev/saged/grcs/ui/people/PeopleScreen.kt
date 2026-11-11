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
fun PeopleScreen(auth: AuthState, data: DataRepo, startCamp: String? = null, startFilter: String? = null, startFamily: String? = null, newTrigger: Int = 0, onExit: (() -> Unit)? = null) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val camps = remember { data.campList(auth) }
    var camp by remember { mutableStateOf(startCamp ?: camps.firstOrNull()?.first.orEmpty()) }
    var persons by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    var open by remember { mutableStateOf<String?>(startFamily) }
    var openSel by remember { mutableStateOf("") }
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

    LaunchedEffect(newTrigger) { if (newTrigger > 0 && auth.user.role != "beneficiary") edit = JSONObject().put("isFamilyHead", true).put("relation", "head") }
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
            ed != null -> EditScreen(ed, persons, nidErr, { edit = null; nidErr = null }) { o, ex -> save(o, o.optString("familyId").ifEmpty { "i_" + o.optString("id") }, ex) }
            allMode -> Column(Modifier.fillMaxSize()) {
                val matched = remember(allList, filter) { allList?.let { PFilter.apply(filter, it) } }
                AppHeader(PFilter.label(filter), if (matched == null) "جارٍ التحميل..." else "${matched.size} سجل — كل المخيمات", conn = null, mark = Fa.LayerGroup, endBack = onExit)
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
                AppHeader("الأفراد", camps.firstOrNull { it.first == camp }?.second ?: "", conn = null, mark = Fa.Users) { if (onExit != null) HeaderBtn(Fa.ArrowRight, "العودة للرئيسية", onExit) }
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
                        androidx.compose.material3.DropdownMenu(menu, { menu = false }) {
                            listOf("default" to "ترتيب الكشف", "vuln" to "الأكثر هشاشة أولاً").forEach { (k, l) -> androidx.compose.material3.DropdownMenuItem({ Text(l, fontSize = 14.sp) }, { sort = k; menu = false }) }
                        }
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
        if (m.optString("phone").isEmpty()) add(Fa.PhoneSlash to p.gray)
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
    var phone2 by remember { mutableStateOf(dev.saged.grcs.data.PFields.phone2(src)) }
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

    dev.saged.grcs.ui.shell.Immersive()
    Column(Modifier.fillMaxSize()) {
        AppHeader(if (isNew) "إضافة فرد جديد" else "تعديل بيانات فرد", "", conn = null, onBack = onBack, backIcon = Fa.Xmark)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FormCard("البيانات الأساسية", Fa.IdCard) {
                QField("الاسم الكامل *", name, { name = it })
                QField("رقم الهوية *", nid, { nid = it }, type = KeyboardType.Number)
                Row(Modifier.fillMaxWidth().clickable { noId = !noId; if (noId) nid = "" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sh = RoundedCornerShape(3.dp)
                    Box(Modifier.size(16.dp).background(if (noId) p.pri else p.surface, sh).border(1.5.dp, if (noId) p.pri else p.mute, sh), contentAlignment = Alignment.Center) { if (noId) androidx.compose.material3.Icon(Fa.Check, null, Modifier.size(10.dp), tint = Color.White) }
                    Text("لا يوجد رقم هوية بعد (مولود لم تصدر هويته)", color = p.mute, fontSize = 12.sp)
                }
                dev.saged.grcs.ui.components.DateField("تاريخ الميلاد", dob, { dob = it })
                val age = dobAge(normDigits(dob))
                Column {
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("العمر المحسوب", color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(age?.let { "$it سنة" } ?: "—", color = p.mute, fontSize = 13.sp)
                    }
                    if (age == null) Text("أدخل تاريخ الميلاد ليُصنَّف تلقائياً", color = p.mute, fontSize = 13.sp)
                    else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val (bg, fg, ic, t) = when { age < 18 -> listOf(Color(0xFFFEF3C7), Color(0xFF92400E), Fa.Child, "طفل"); age >= 60 -> listOf(Color(0xFFEDE9FE), Color(0xFF5B21B6), Fa.PersonCane, "كبير سن"); else -> listOf(Color(0xFFDBEAFE), Color(0xFF1E40AF), Fa.User, "بالغ") }
                        Row(Modifier.background(bg as Color, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            androidx.compose.material3.Icon(ic as androidx.compose.ui.graphics.vector.ImageVector, null, Modifier.size(11.dp), tint = fg as Color)
                            Text("$t · $age سنة", color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        val band = when { age < 1 -> "رضع <1"; age <= 5 -> "أطفال 1-5"; age < 18 -> "أطفال 6-18"; age <= 30 -> "شباب 18-30"; age < 60 -> "بالغون 30-60"; else -> "كبار السن +60" }
                        Text(band, color = p.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(p.surface2, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
                Column {
                    Text("الجنس", color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
                    GenderSeg(gender) { gender = it }
                }
            }
            FormCard("الأسرة", Fa.HouseChimney) {
                IconSwitch("رب الأسرة", Fa.Crown, head, enabled = false) {}
                if (head) {
                    dev.saged.grcs.ui.components.QSelect(shelter, listOf("" to "— اختر —") + shelterLabels, label = "نوع المأوى") { shelter = it }
                    Column {
                        dev.saged.grcs.ui.components.FilterLabelPlain("الحالة الاجتماعية", Fa.Ring)
                        val msIcons = listOf(Fa.Heart, Fa.UserShield, Fa.Ring, Fa.User)
                        dev.saged.grcs.ui.components.SegBtns(maritalLabels.mapIndexed { i, m -> m to msIcons.getOrNull(i) }, maritalLabels.indexOf(marital)) { marital = maritalLabels[it] }
                    }
                }
            }
            if (head) {
                FormCard("الزوج / الزوجة (اختياري)", Fa.PeoplePulling) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        QField("الاسم الكامل", sName, { sName = it }, placeholder = "اسم الزوج/ة رباعي")
                        QField("رقم الهوية (مطلوب إن أُدخل الاسم)", sNid, { sNid = it }, type = KeyboardType.Number)
                        dev.saged.grcs.ui.components.DateField("تاريخ الميلاد", sDob, { sDob = it })
                        val sFemale = gender == "male"
                        if (sName.isNotBlank() && sFemale) Column {
                            IconSwitch("حامل", Fa.BabyCarriage, sPreg) { sPreg = it }; IconSwitch("مرضعة", Fa.Baby, sBf) { sBf = it }
                        }
                    }
                }
                FormCard("أفراد الأسرة (اختياري)", Fa.Children) {
                kids.forEachIndexed { i, st ->
                    val k = st.value
                    QCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            QField("الاسم", k.name, { st.value = Kid(it, k.gender, k.dob, k.rel) })
                            dev.saged.grcs.ui.components.DateField("تاريخ الميلاد", k.dob, { st.value = Kid(k.name, k.gender, it, k.rel) })
                            GenderSeg(k.gender, unknown = false) { st.value = Kid(k.name, it, k.dob, k.rel) }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { relLabels.forEach { (rk, l) -> QChip(l, k.rel == rk) { st.value = Kid(k.name, k.gender, k.dob, rk) } } }
                            Btn("حذف", { kids.removeAt(i) }, kind = BtnKind.Danger, small = true, icon = Fa.Trash)
                        }
                    }
                }
                Row { Btn("إضافة فرد للأسرة", { kids.add(mutableStateOf(Kid("", "", "", "son"))) }, kind = BtnKind.Outline, small = true, icon = Fa.Plus) }
                }
            }
            FormCard("التواصل", Fa.AddressBook) {
                QField("رقم الهاتف الأساسي", phone, { phone = it }, type = KeyboardType.Phone)
                QField("رقم هاتف إضافي (اختياري)", phone2, { phone2 = it }, type = KeyboardType.Phone)
                QField("العنوان / الموقع", address, { address = it })
            }
            FormCard("الحالة الصحية", Fa.HeartPulse) {
                Column {
                    if (gender == "female") IconSwitch("حامل", Fa.BabyCarriage, preg) { preg = it }
                    if (gender == "female") IconSwitch("مرضعة", Fa.Baby, bf) { bf = it }
                    IconSwitch("مرض مزمن", Fa.NotesMedical, chronic) { chronic = it }
                    if (chronic) Column(Modifier.padding(top = 10.dp)) {
                        Text("اختر الحالة/الحالات المرضية", color = p.mute, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
                        FlowRow(Modifier.padding(top = 10.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            chronicLabels.forEach { (k, l) ->
                                val on = k in conds; val sh = RoundedCornerShape(50)
                                Text(l, color = if (on) p.danger else p.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(sh).background(if (on) p.danger.copy(alpha = if (p.dark) .13f else .06f) else p.surface2).border(1.5.dp, if (on) p.danger else p.line, sh).clickable { if (on) conds.remove(k) else conds.add(k) }.padding(horizontal = 14.dp, vertical = 7.dp))
                            }
                        }
                        QField("", diseaseDetails, { diseaseDetails = it }, Modifier.padding(top = 8.dp), placeholder = "تفاصيل إضافية / أخرى")
                    }
                    IconSwitch("إعاقة", Fa.Wheelchair, dis) { dis = it }
                    if (dis) QField("", disDetails, { disDetails = it }, Modifier.padding(top = 10.dp), placeholder = "تفاصيل الإعاقة")
                }
            }
            FormCard("ملاحظات", Fa.NoteSticky) { QField("", notes, { notes = it }, multiline = true, placeholder = "ملاحظات إضافية...") }
            (err ?: serverErr)?.let { Text(it, color = p.danger, fontSize = 13.sp) }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(p.line))
        Box(Modifier.fillMaxWidth().background(p.surface).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Btn("حفظ", {
            err = validate()
                if (err == null) {
                    val o = Merge.copy(src)
                    val n = normDigits(nid); val d = normDigits(dob)
                    o.put("fullName", name.trim()).put("nationalId", n).put("gender", gender).put("phone", phone.trim())
                        .put("birthDate", if (d.isEmpty()) JSONObject.NULL else d)
                        .put("calculatedAge", dobAge(d) ?: JSONObject.NULL)
                        .put("isPregnant", gender == "female" && preg).put("hasChronicDisease", chronic).put("hasDisability", dis)
                        .put("notes", notes.trim())
                        .put("isBreastfeeding", gender == "female" && bf).put("urgentNeed", urgent)
                        .put("chronicConditions", org.json.JSONArray(if (chronic) conds.toList() else emptyList<String>()))
                        .put("diseaseDetails", if (chronic) diseaseDetails.trim() else "").put("disabilityDetails", if (dis) disDetails.trim() else "")
                    dev.saged.grcs.data.PFields.setPhone2(o, normDigits(phone2).trim())
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
        }, Modifier.fillMaxWidth(), icon = Fa.Check)
        }
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
private fun FormCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    QCard(pad = 16.dp) {
        Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) androidx.compose.material3.Icon(icon, null, Modifier.size(17.dp), tint = p.pri)
            Text(title.substringBefore(" (اختياري)"), color = p.head, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            if (title.endsWith("(اختياري)")) Text("(اختياري)", color = p.mute, fontSize = 13.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

/** صف مفتاح بأيقونة (toggle-row). */
@Composable
private fun IconSwitch(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, on: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) { androidx.compose.material3.Icon(icon, null, Modifier.size(15.dp), tint = p.ink) }
        Text(label, color = p.ink, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        dev.saged.grcs.ui.components.QSwitch(on) { if (enabled) onChange(it) }
    }
}

/** أزرار الجنس المقسّمة بألوان الويب (أزرق للذكر، وردي للأنثى). */
@Composable
private fun GenderSeg(gender: String, unknown: Boolean = true, onSel: (String) -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(10.dp)
    val items = listOfNotNull(Triple("male", "ذكر", Fa.Mars), Triple("female", "أنثى", Fa.Venus), if (unknown) Triple("unknown", "غير محدد", Fa.Question) else null)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (k, l, ic) ->
            val on = gender == k
            val c = when { !on -> p.mute; k == "male" -> p.blue; k == "female" -> Color(0xFFDB2777); else -> if (p.dark) p.tealL else p.tealD }
            val bg = when { !on -> p.surface; k == "male" -> p.blue.copy(alpha = .09f); k == "female" -> Color(0xFFDB2777).copy(alpha = .06f); else -> p.pri.copy(alpha = .08f) }
            Row(Modifier.weight(1f).heightIn(min = 46.dp).clip(shape).background(bg).border(1.5.dp, if (!on) p.line else if (k == "unknown") p.pri else c, shape).clickable { onSel(k) }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(ic, null, Modifier.size(14.dp), tint = c); Spacer(Modifier.width(6.dp))
                Text(l, color = c, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}
