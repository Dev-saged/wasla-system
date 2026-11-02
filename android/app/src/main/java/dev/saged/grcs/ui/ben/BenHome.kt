package dev.saged.grcs.ui.ben

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import dev.saged.grcs.ui.components.HeaderBtn
import dev.saged.grcs.ui.components.Ring
import dev.saged.grcs.data.PFields
import dev.saged.grcs.data.Stats
import androidx.activity.compose.BackHandler
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.Aid
import dev.saged.grcs.ui.people.EditScreen
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.Bar
import dev.saged.grcs.ui.components.SegTabs
import dev.saged.grcs.data.Messages
import dev.saged.grcs.data.Merge
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.QCard
import dev.saged.grcs.ui.components.SectionTitle
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.components.QField
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val reqLabels = Aid.reqTypes.toMap()
private val shelters = listOf("tent_ok" to "خيمة سليمة", "tent_damaged" to "خيمة تالفة", "shared" to "مأوى مشترك", "building_damaged" to "مبنى متضرر", "none" to "بلا مأوى")
private val relLabel = mapOf("head" to "رب الأسرة", "wife" to "زوجة", "husband" to "زوج", "son" to "ابن", "daughter" to "ابنة", "orphan" to "يتيم/ة", "child" to "ابن/ة", "other" to "قريب")

private fun isOrphan(m: JSONObject) = m.optJSONArray("tags")?.let { a -> (0 until a.length()).any { a.optString(it) == "orphan" } } == true
private fun yn(b: Boolean, t: String) = if (b) t else null

private fun detailRows(x: JSONObject): List<Pair<String, String>> {
    val age = Stats.ageOf(x); val r = mutableListOf<Pair<String, String>>()
    r += "الاسم الكامل" to x.optString("fullName")
    x.optString("nationalId").takeIf { it.isNotEmpty() }?.let { r += "رقم الهوية" to it }
    x.optString("birthDate").takeIf { it.isNotEmpty() && it != "null" }?.let { r += "تاريخ الميلاد" to it }
    age?.let { r += "العمر" to "$it سنة" }
    x.optString("gender").takeIf { it == "male" || it == "female" }?.let { r += "الجنس" to if (it == "male") "ذكر" else "أنثى" }
    (relLabel[x.optString("relation")] ?: if (x.optBoolean("isFamilyHead")) "رب الأسرة" else null)?.let { r += "العلاقة بالأسرة" to it }
    x.optString("familyId").takeIf { it.isNotEmpty() }?.let { r += "رقم الأسرة" to it }
    x.optString("phone").takeIf { it.isNotEmpty() }?.let { r += "رقم الهاتف" to it }
    x.optString("address").takeIf { it.isNotEmpty() }?.let { r += "العنوان" to it }
    x.optString("maritalStatus").takeIf { it.isNotEmpty() }?.let { r += "الحالة الاجتماعية" to it }
    if (x.optString("gender") == "female" && x.optBoolean("isPregnant")) r += "الحالة" to "حامل"
    if (x.optString("gender") == "female" && x.optBoolean("isBreastfeeding")) r += "الحالة" to "مرضعة"
    if (x.optBoolean("hasChronicDisease")) r += "مرض مزمن" to x.optString("diseaseDetails").ifEmpty { "نعم" }
    if (x.optBoolean("hasDisability")) r += "إعاقة" to x.optString("disabilityDetails").ifEmpty { "نعم" }
    x.optString("notes").takeIf { it.isNotEmpty() }?.let { r += "ملاحظات" to it }
    if (x.optBoolean("isFamilyHead")) shelters.firstOrNull { it.first == x.optString("shelterType") }?.let { r += "نوع المأوى" to it.second }
    PFields.living(x).takeIf { it.isNotEmpty() }?.let { r += "الوضع المعيشي" to it }
    if (x.optBoolean("urgentNeed")) r += "احتياج طارئ" to x.optString("urgentNeedDetails").ifEmpty { "نعم" }
    PFields.phone2(x).takeIf { it.isNotEmpty() }?.let { r += "هاتف إضافي" to it }
    return r
}

@Composable
private fun Pill(t: String, c: Color) {
    Text(t, color = c, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(c.copy(alpha = .14f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp))
}

@Composable
private fun FormCard(title: String, content: @Composable () -> Unit) {
    val p = LocalPalette.current
    QCard { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, color = p.navy.takeIf { !p.dark } ?: p.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold); content() } }
}

@Composable
private fun SwitchRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = p.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(on, onChange)
    }
}

/** شاشة المستفيد بنفس بنية الويب: شارات، حلقة الاكتمال، زر التواصل، أربعة تبويبات، المظهر، كلمة المرور، الخروج. */
@Composable
fun BenHome(auth0: AuthState, data: DataRepo, onLogout: () -> Unit = {}) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    var auth by remember { mutableStateOf(auth0) }
    var persons by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var rev by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(true) }
    var type by remember { mutableStateOf("food") }
    var details by remember { mutableStateOf("") }
    var screen by remember { mutableStateOf("main") }
    var tab by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<JSONObject?>(null) }
    var msgs by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var info by remember { mutableStateOf<dev.saged.grcs.data.CampInfo?>(null) }
    val camp = auth.user.campId

    LaunchedEffect(rev) {
        busy = true
        persons = withContext(Dispatchers.IO) { data.people.persons(camp) }
        runCatching { withContext(Dispatchers.IO) { data.sync(auth) } }.getOrNull()?.let { auth = it.first }
        persons = withContext(Dispatchers.IO) { data.people.persons(camp) }
        info = withContext(Dispatchers.IO) { runCatching { data.campInfo(auth, camp) }.getOrNull() }
        msgs = withContext(Dispatchers.IO) { runCatching { Messages().loadBen(auth) }.getOrDefault(emptyList()) }
        busy = false
    }
    val head = persons.firstOrNull { it.optBoolean("isFamilyHead") } ?: persons.firstOrNull()
    val family = persons.filter { it.optString("id") != head?.optString("id") }
    fun saveP(o: JSONObject, extras: List<JSONObject>) {
        scope.launch {
            withContext(Dispatchers.IO) {
                o.put("updatedBy", auth.user.id)
                val sv = data.people.save(camp, o); data.afterSave(auth, camp, sv, false)
                for (x in extras) { x.put("familyId", sv.optString("familyId")).put("updatedBy", auth.user.id).put("createdBy", auth.user.id); data.afterSave(auth, camp, data.people.save(camp, x), false) }
                data.pushNow(auth, camp)
            }
            editing = null; rev++
        }
    }
    BackHandler(screen != "main" || editing != null) { if (editing != null) editing = null else screen = "main" }
    editing?.let { ed -> EditScreen(ed, persons, null, { editing = null }) { o, ex -> saveP(o, ex) }; return }
    if (screen == "chat") { dev.saged.grcs.ui.messages.MessagesScreen(auth, data, null) { screen = "main" }; return }
    if (screen == "changepw") { dev.saged.grcs.ui.more.ChangePwPage(auth) { screen = "main" }; return }
    val campName = info?.name?.ifEmpty { null } ?: data.campList(auth).firstOrNull()?.second.orEmpty()
    Column(Modifier.fillMaxSize()) {
        AppHeader(auth.user.fullName, if (campName.isEmpty()) "—" else if (campName.trim().startsWith("مخيم")) campName else "مخيم $campName") { HeaderBtn(QIcons.Logout, "تسجيل الخروج", onLogout) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (head == null) {
                if (busy) Text("جارٍ التحميل…", color = p.mute, fontSize = 13.sp)
                else QCard { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text("تعذّر إيجاد بياناتك", color = p.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("تواصل مع مندوب مخيمك", color = p.mute, fontSize = 13.sp) } }
            } else {
                // شارات الحالة
                val age = Stats.ageOf(head)
                val pills = buildList<Pair<String, Color>> {
                    if (age != null && age < 1) add("رضيع" to p.danger)
                    if (head.optBoolean("isPregnant")) add("حامل" to p.pregnant)
                    if (head.optBoolean("isBreastfeeding")) add("مرضعة" to p.purple)
                    if (head.optBoolean("hasChronicDisease")) add("مرض مزمن" to p.warn)
                    if (age != null && age >= 60) add("كبير سن" to p.purple)
                    if (head.optBoolean("hasDisability")) add("إعاقة" to p.blue)
                    if (head.optString("maritalStatus").contains("أرمل")) add("أرمل/ة" to p.gray)
                    if (head.optString("maritalStatus").contains("مطلق")) add("مطلّق/ة" to p.gray)
                    if (head.optString("nationalId").isEmpty()) add("بلا هوية" to p.danger)
                    if (head.optString("phone").isEmpty()) add("بلا هاتف" to p.gray)
                }
                if (pills.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { pills.forEach { (t, c) -> Pill(t, c) } }
                Completeness(head, family) { t -> tab = t }
                Btn("تواصل مع إدارة المخيم", { screen = "chat" }, Modifier.fillMaxWidth(), icon = QIcons.Chat)
                SegTabs(listOf("ملفي", "أسرتي", "طلباتي", "إشعاراتي"), tab, { tab = it })
                when (tab) {
                    0 -> ProfileTab(head, onSave = { o -> saveP(o, emptyList()) })
                    1 -> FamilyTab(head, family, onAdd = { editing = JSONObject().put("familyId", head.optString("familyId")).put("isFamilyHead", false).put("relation", "son") }, onOpen = { editing = it })
                    2 -> RequestsTab(head, type, { type = it }, details, { details = it }) { o -> scope.launch { withContext(Dispatchers.IO) { data.people.save(camp, o.put("updatedBy", auth.user.id)); data.pushNow(auth, camp) }; details = ""; rev++ } }
                    3 -> NotifyTab(head, msgs, auth.user.id)
                }
            }
            FormCard("المظهر") { dev.saged.grcs.ui.components.SegTabs(listOf("تلقائي", "فاتح", "داكن"), dev.saged.grcs.ui.theme.ThemePref.mode, { dev.saged.grcs.ui.theme.ThemePref.set(it) }) }
            Btn("تغيير كلمة المرور", { screen = "changepw" }, Modifier.fillMaxWidth(), BtnKind.Outline, QIcons.Key)
            Btn("تسجيل الخروج", onLogout, Modifier.fillMaxWidth(), BtnKind.Danger, QIcons.Logout)
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("منظومة مخيمات النزوح", color = p.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("تطوير: المهندس ساجد العبادلة", color = p.mute, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun Completeness(head: JSONObject, family: List<JSONObject>, goTab: (Int) -> Unit) {
    val p = LocalPalette.current
    class Item(val ok: Boolean, val label: String, val tab: Int)
    val items = buildList {
        add(Item(head.optString("phone").isNotEmpty(), "رقم الهاتف", 0)); add(Item(PFields.phone2(head).isNotEmpty(), "هاتف إضافي", 0))
        add(Item(head.optString("address").isNotEmpty(), "العنوان", 0)); add(Item(head.optString("shelterType").isNotEmpty(), "نوع المأوى", 0))
        family.filter { it.optString("relation") != "wife" && !it.optBoolean("isFamilyHead") }.forEach { m -> add(Item(m.optString("birthDate").let { it.isNotEmpty() && it != "null" }, "تاريخ ميلاد " + m.optString("fullName").substringBefore(" ").ifEmpty { "فرد" }, 1)) }
        family.filter { it.optString("nationalId").isEmpty() && !it.optBoolean("noIdYet") }.forEach { m -> add(Item(false, "هوية " + m.optString("fullName").substringBefore(" ").ifEmpty { "فرد" }, 1)) }
    }
    val done = items.count { it.ok }; val pct = if (items.isEmpty()) 100 else Math.round(done * 100f / items.size)
    val missing = items.filter { !it.ok }
    val gaps = buildList {
        if (head.optString("birthDate").let { it.isEmpty() || it == "null" }) add("تاريخ ميلادك")
        family.filter { it.optString("relation") == "wife" && it.optString("birthDate").let { d -> d.isEmpty() || d == "null" } }.forEach { add("تاريخ ميلاد الزوجة") }
    }
    QCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Ring(pct / 100f, if (pct == 100) p.success else p.tealL, 64.dp, "$pct%")
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (pct == 100) "ملفك مكتمل" else "اكتمال ملفك", color = p.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (missing.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    missing.forEach { m -> Text(m.label, color = p.warn, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.heightIn(min = 32.dp).background(p.warn.copy(alpha = .14f), RoundedCornerShape(50)).clickable { goTab(m.tab) }.padding(horizontal = 10.dp, vertical = 6.dp)) }
                } else Text("كل البيانات المطلوبة منك موجودة — شكراً لك", color = p.mute, fontSize = 12.sp)
                if (gaps.isNotEmpty()) Text("يكملها المندوب: " + gaps.joinToString("، "), color = p.mute, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ProfileTab(head: JSONObject, onSave: (JSONObject) -> Unit) {
    val p = LocalPalette.current
    var phone by remember(head) { mutableStateOf(head.optString("phone")) }
    var phone2 by remember(head) { mutableStateOf(PFields.phone2(head)) }
    var address by remember(head) { mutableStateOf(head.optString("address")) }
    var chronic by remember(head) { mutableStateOf(head.optBoolean("hasChronicDisease")) }
    var cd by remember(head) { mutableStateOf(head.optString("diseaseDetails")) }
    var dis by remember(head) { mutableStateOf(head.optBoolean("hasDisability")) }
    var dd by remember(head) { mutableStateOf(head.optString("disabilityDetails")) }
    var notes by remember(head) { mutableStateOf(head.optString("notes")) }
    var shelter by remember(head) { mutableStateOf(head.optString("shelterType")) }
    var living by remember(head) { mutableStateOf(PFields.living(head)) }
    var urgent by remember(head) { mutableStateOf(head.optBoolean("urgentNeed")) }
    var ud by remember(head) { mutableStateOf(head.optString("urgentNeedDetails")) }
    FormCard("بياناتي") {
        detailRows(head).forEach { (k, v) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(k, color = p.mute, fontSize = 13.sp); Text(v, color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, false))
            }
        }
        Text("الاسم ورقم الهوية ثابتان — للتصحيح تواصل مع المندوب", color = p.mute, fontSize = 12.sp)
    }
    FormCard("تعديل بياناتي") {
        QField("رقم الهاتف", phone, { phone = it }, type = androidx.compose.ui.text.input.KeyboardType.Phone)
        QField("رقم هاتف إضافي", phone2, { phone2 = it }, type = androidx.compose.ui.text.input.KeyboardType.Phone)
        QField("العنوان / الموقع الحالي", address, { address = it })
        SwitchRow("أعاني من مرض مزمن", chronic) { chronic = it }
        if (chronic) QField("تفاصيل المرض", cd, { cd = it })
        SwitchRow("لدي إعاقة", dis) { dis = it }
        if (dis) QField("تفاصيل الإعاقة", dd, { dd = it })
        QField("ملاحظات صحية", notes, { notes = it }, multiline = true)
        Text("نوع المأوى", color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { shelters.forEach { (k, l) -> QChip(l, shelter == k) { shelter = if (shelter == k) "" else k } } }
        QField("تفاصيل الوضع المعيشي", living, { living = it }, multiline = true)
        SwitchRow("لدي احتياج طارئ", urgent) { urgent = it }
        if (urgent) QField("ما هو الاحتياج الطارئ؟", ud, { ud = it }, multiline = true)
        Btn("حفظ ومزامنة", {
            val o = Merge.copy(head)
            o.put("phone", phone.trim()).put("address", address.trim())
                .put("hasChronicDisease", chronic).put("diseaseDetails", if (chronic) cd.trim() else "")
                .put("hasDisability", dis).put("disabilityDetails", if (dis) dd.trim() else "")
                .put("urgentNeed", urgent).put("urgentNeedDetails", if (urgent) ud.trim() else "")
                .put("notes", notes.trim()).put("shelterType", shelter).put("livingSituation", living.trim())
            PFields.setPhone2(o, phone2.trim())
            onSave(o)
        }, Modifier.fillMaxWidth(), icon = QIcons.Check)
    }
}

@Composable
private fun FamilyTab(head: JSONObject, family: List<JSONObject>, onAdd: () -> Unit, onOpen: (JSONObject) -> Unit) {
    val p = LocalPalette.current
    Btn("إضافة فرد جديد", onAdd, Modifier.fillMaxWidth(), icon = QIcons.Plus)
    val orphans = family.filter { isOrphan(it) }
    if (orphans.isNotEmpty()) FormCard("أطفال أيتام في رعايتك") {
        orphans.forEach { o ->
            val t = o.optJSONObject("tagData")?.optJSONObject("orphan")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(o.optString("fullName") + (Stats.ageOf(o)?.let { " · $it سنة" } ?: ""), color = p.mute, fontSize = 13.sp)
                Text((if (t?.optString("type") == "both") "يتيم الأبوين" else "يتيم الأب") + if (t?.optBoolean("soleSurvivor") == true) " · الناجي الوحيد" else "", color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text("هؤلاء الأطفال مسجّلون بأسرتك كأيتام في رعايتك — أي تصحيح تواصل فيه مع المندوب", color = p.mute, fontSize = 12.sp)
    }
    if (family.isEmpty()) QCard { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text("لا يوجد أفراد آخرون", color = p.ink, fontWeight = FontWeight.Bold); Text("أضف فرداً من الزر بالأعلى — يُحفظ تلقائياً ضمن أسرتك", color = p.mute, fontSize = 12.sp) } }
    val groups = listOf<Pair<String, (JSONObject) -> Boolean>>(
        "الزوج / الزوجة" to { m -> m.optString("relation").let { it == "wife" || it == "husband" } },
        "الأطفال (دون 18)" to { m -> (Stats.ageOf(m) ?: 99) < 18 },
        "البالغون" to { m -> Stats.ageOf(m)?.let { it in 18..59 } == true },
        "كبار السن (60+)" to { m -> (Stats.ageOf(m) ?: 0) >= 60 },
        "بلا تاريخ ميلاد" to { _ -> true },
    )
    val buckets = groups.map { mutableListOf<JSONObject>() }
    family.forEach { m -> buckets[groups.indexOfFirst { it.second(m) }].add(m) }
    groups.forEachIndexed { i, g ->
        if (buckets[i].isEmpty()) return@forEachIndexed
        Text(g.first + "  " + buckets[i].size, color = p.mute, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        buckets[i].forEach { m ->
            val female = m.optString("gender") == "female"
            Row(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(16.dp)).clickable { onOpen(m) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).background(if (m.optString("gender") == "male") p.navy else if (female) p.pregnant else p.gray, RoundedCornerShape(12.dp)))
                Column(Modifier.weight(1f)) {
                    Text(m.optString("fullName"), color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text((relLabel[m.optString("relation")] ?: "قريب") + (Stats.ageOf(m)?.let { " · $it سنة" } ?: "") + (if (m.optBoolean("isPregnant")) " · حامل" else "") + (if (m.optBoolean("isBreastfeeding")) " · مرضعة" else "") + (if (isOrphan(m)) " · يتيم" else ""), color = p.mute, fontSize = 12.sp)
                }
            }
        }
    }
    Text("اضغط على أي فرد لتحديث بياناته — كل تعديل يصل للمندوب والإدارة فوراً", color = p.mute, fontSize = 12.sp)
}

@Composable
private fun RequestsTab(head: JSONObject, type: String, onType: (String) -> Unit, details: String, onDetails: (String) -> Unit, onSend: (JSONObject) -> Unit) {
    val p = LocalPalette.current
    var cancelId by remember { mutableStateOf<String?>(null) }
    FormCard("طلب جديد") {
        Text("نوع الطلب", color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Aid.reqTypes.forEach { (k, l) -> QChip(l, type == k) { onType(k) } } }
        QField("التفاصيل", details, onDetails, multiline = true)
        Btn("إرسال الطلب", { if (details.trim().isNotEmpty()) onSend(Aid.addRequest(head, type, details.trim(), head.optString("updatedBy"))) }, Modifier.fillMaxWidth(), icon = QIcons.Broadcast)
    }
    SectionTitle("طلباتي السابقة")
    val reqs = Aid.requests(head).sortedByDescending { it.optLong("createdAt") }
    if (reqs.isEmpty()) QCard { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text("لا توجد طلبات", color = p.ink, fontWeight = FontWeight.Bold); Text("أرسل أول طلب من النموذج بالأعلى", color = p.mute, fontSize = 12.sp) } }
    reqs.forEach { r ->
        val st = r.optString("status")
        val c = when (st) { "pending" -> p.warn; "approved", "done" -> p.success; "rejected" -> p.danger; else -> p.gray }
        QCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(reqLabels[r.optString("type")] ?: r.optString("type"), color = p.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Pill(Aid.reqStatus[st] ?: st, c)
            }
            if (r.optString("details").isNotEmpty()) Text(r.optString("details"), color = p.mute, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            if (r.optString("reply").isNotEmpty()) Text("↩ " + r.optString("reply"), color = p.ink, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun NotifyTab(head: JSONObject, msgs: List<JSONObject>, uid: String) {
    val p = LocalPalette.current
    FormCard("الإشعارات") { Text("تصلك رسائل الإدارة وتحديثات الطلبات هنا عند فتح التطبيق.", color = p.mute, fontSize = 13.sp) }
    SectionTitle("آخر الإشعارات")
    data class F(val t: Long, val title: String, val body: String)
    val feed = buildList {
        msgs.filter { it.optString("senderId") != uid }.forEach { add(F(it.optLong("timestamp"), "رسالة من إدارة المخيم", it.optString("text"))) }
        Aid.requests(head).filter { it.optString("status") != "pending" && it.optString("status") != "cancelled" && it.optLong("updatedAt") > 0 }.forEach { add(F(it.optLong("updatedAt"), "طلب " + (reqLabels[it.optString("type")] ?: "") + ": " + (Aid.reqStatus[it.optString("status")] ?: ""), it.optString("reply"))) }
    }.sortedByDescending { it.t }.take(10)
    val fmt = remember { java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.US) }
    if (feed.isEmpty()) Text("لا توجد إشعارات بعد", color = p.mute, fontSize = 13.sp)
    feed.forEach { f ->
        QCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(f.title, color = p.ink, fontWeight = FontWeight.Bold, fontSize = 13.5.sp); Text(fmt.format(java.util.Date(f.t)), color = p.mute, fontSize = 11.sp) }
            if (f.body.isNotEmpty()) Text(f.body, color = p.mute, fontSize = 12.sp, maxLines = 1)
        }
    }
    SectionTitle("سجل التعديلات على بياناتي")
    Text("لا يوجد سجل تعديلات بعد", color = p.mute, fontSize = 13.sp)
}
