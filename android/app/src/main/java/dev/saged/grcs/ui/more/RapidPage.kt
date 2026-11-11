package dev.saged.grcs.ui.more

import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.QAct
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.saged.grcs.ui.components.qcard
import dev.saged.grcs.ui.components.Btn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.Clock
import dev.saged.grcs.data.GenderInfer
import dev.saged.grcs.data.Stats
import dev.saged.grcs.ui.components.QField
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import org.json.JSONObject
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import dev.saged.grcs.ui.icons.Fa
import dev.saged.grcs.ui.components.BtnKind
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import kotlinx.coroutines.withContext

/** جلسة الإدخال السريع (Rapid في الويب): تبقى الأسرة الجارية والعدّاد ما دام التطبيق مفتوحاً. */
private object RapidSession {
    var camp = ""; var famId = ""; var head: JSONObject? = null; var members = 0; var count = 0
    val saved = ArrayList<JSONObject>()
}

/** الإدخال السريع — نقل مباشر لـrapidSave/rapidSetRel/rapidUndo في الويب، ويعمل على مخيم السياق الحالي فقط. */
@Composable
fun RapidPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val camps = remember { data.ctxCamps(auth) }
    val camp = camps.firstOrNull()?.first.orEmpty()
    if (RapidSession.camp != camp) { RapidSession.camp = camp; RapidSession.famId = ""; RapidSession.head = null; RapidSession.members = 0; RapidSession.count = 0; RapidSession.saved.clear() }
    var tick by remember { mutableStateOf(0) }
    var rel by remember { mutableStateOf(if (RapidSession.head != null) "son" else "head") }
    var nid by remember { mutableStateOf("") }
    var noId by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var other by remember { mutableStateOf<String?>(null) }
    var persons by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(camp, tick) { persons = withContext(Dispatchers.IO) { data.people.persons(camp) } }
    val byNid = remember(persons) { persons.filter { it.optString("nationalId").isNotEmpty() }.associateBy { it.optString("nationalId") } }
    val fam = RapidSession.head

    fun setRel(r: String) { rel = r; gender = when (r) { "son" -> "male"; "daughter", "wife" -> "female"; else -> gender } }
    /** rapidChain: أول ثلاثة أسماء من اسم رب الأسرة الذكر تُلحق باسم الابن/الابنة إن كُتب الاسم الأول فقط. */
    fun chain(): String { val h = fam ?: return ""; if (h.optString("gender") == "female") return ""; return h.optString("fullName").trim().split(Regex("\\s+")).take(3).joinToString(" ") }
    fun finalName(): String {
        val raw = name.trim().replace(Regex("\\s+"), " "); if (raw.isEmpty()) return ""
        val c = chain()
        return if (rel in listOf("son", "daughter") && c.isNotEmpty() && !raw.contains(' ')) "$raw $c" else raw
    }
    val n9 = dev.saged.grcs.data.Nid.norm(nid).let { if (it.all { c -> c in '0'..'9' }) it else "" }
    val local = byNid[n9]
    // فحص المخيمات الأخرى بعد توقف الكتابة (checkNationalIndex)
    androidx.compose.runtime.LaunchedEffect(n9) {
        other = null
        if (n9.isNotEmpty() && local == null && dev.saged.grcs.data.Nid.status(n9) == "valid" && !auth.offline) { kotlinx.coroutines.delay(550); other = withContext(Dispatchers.IO) { runCatching { data.checkNid(auth, n9, camp) }.getOrNull() } }
    }
    val nidOk = local == null && other == null
    val suggestions = remember(name, gender, rel, persons) {
        val raw = name.trim()
        if (raw.isEmpty() || raw.contains(' ')) emptyList() else {
            val g = gender.ifEmpty { when (rel) { "son" -> "male"; "daughter", "wife" -> "female"; else -> "" } }
            val k = GenderInfer.key(raw)
            persons.asSequence().map { it.optString("fullName").trim().split(Regex("\\s+"))[0] to it.optString("gender") }
                .filter { (f, gg) -> f.isNotEmpty() && GenderInfer.key(f).let { fk -> fk.startsWith(k) && fk != k } && (g.isEmpty() || gg.isEmpty() || gg == g) }
                .groupingBy { it.first }.eachCount().entries.sortedByDescending { it.value }.map { it.key }.distinctBy { GenderInfer.key(it) }.take(6).toList()
        }
    }

    fun save() {
        val nm = finalName()
        if (nm.isEmpty()) { msg = "الاسم مطلوب"; return }
        if (rel != "head" && fam == null) { msg = "أدخل رب الأسرة أولاً"; setRel("head"); return }
        if (n9.isEmpty() && !noId) { msg = "رقم الهوية مطلوب — أدخله أو حدّد «لا يوجد رقم هوية بعد»"; return }
        if (n9.isNotEmpty() && dev.saged.grcs.data.Nid.status(n9) == "format") { msg = "رقم الهوية يجب أن يكون 9 أرقام"; return }
        if (!nidOk) { msg = "رقم الهوية مسجّل مسبقاً"; return }
        busy = true; msg = ""
        scope.launch {
            val now = Clock.now(); val isHead = rel == "head"
            val g = when (rel) { "son" -> "male"; "daughter", "wife" -> "female"; else -> gender.ifEmpty { GenderInfer.infer(nm) } }
            val id = java.util.UUID.randomUUID().toString()
            val familyId = if (isHead) n9.ifEmpty { id } else RapidSession.famId
            val o = JSONObject().put("id", id).put("campId", camp).put("createdAt", now).put("createdBy", auth.user.id).put("fullName", nm).put("nationalId", n9)
                .put("birthDate", if (dob.isEmpty()) JSONObject.NULL else dob).put("calculatedAge", Stats.ageOfDob(dob) ?: JSONObject.NULL).put("gender", g)
                .put("isFamilyHead", isHead).put("relation", if (isHead) "head" else if (rel == "wife") "wife" else if (rel == "other") "other" else "child")
                .put("familyId", familyId).put("phone", if (isHead) phone.trim() else "").put("address", fam?.optString("address").orEmpty()).put("notes", "")
                .put("hasChronicDisease", false).put("chronicConditions", org.json.JSONArray()).put("diseaseDetails", "").put("hasDisability", false).put("disabilityDetails", "")
                .put("isPregnant", false).put("isBreastfeeding", false).put("maritalStatus", "").put("orderIndex", if (isHead) 0 else if (rel == "wife") 1 else 2 + maxOf(RapidSession.members, 1))
                .put("customFields", JSONObject()).put("updatedAt", now).put("updatedBy", auth.user.id)
            if (rel == "other") o.put("relationDetail", "فرد من الأسرة")
            if (rel == "son" || rel == "daughter") o.put("memberRelation", rel)
            if (n9.isEmpty() && noId) o.put("noIdYet", true)
            val saved = withContext(Dispatchers.IO) { data.people.save(camp, o).also { s -> runCatching { data.afterSave(auth, camp, s, false) }; data.pushNow(auth, camp) } }
            if (isHead) { RapidSession.famId = familyId; RapidSession.head = saved; RapidSession.members = 1 } else RapidSession.members++
            RapidSession.saved.add(0, saved); RapidSession.count++
            nid = ""; name = ""; dob = ""; phone = ""; noId = false
            setRel(if (isHead) "wife" else if (rel == "wife") "son" else rel)
            busy = false; tick++
        }
    }
    fun undo() {
        val last = RapidSession.saved.removeFirstOrNull() ?: return
        scope.launch {
            withContext(Dispatchers.IO) { data.people.delete(camp, last); data.afterSave(auth, camp, last, true); data.pushNow(auth, camp) }
            if (last.optBoolean("isFamilyHead")) { RapidSession.head = null; RapidSession.famId = ""; RapidSession.members = 0; setRel("head") } else RapidSession.members = maxOf(1, RapidSession.members - 1)
            RapidSession.count = maxOf(0, RapidSession.count - 1); msg = "تم التراجع عن آخر حفظ"; tick++
        }
    }

    dev.saged.grcs.ui.shell.Immersive()
    Column(Modifier.fillMaxSize()) {
        AppHeader("إدخال سريع", camps.firstOrNull()?.second.orEmpty(), conn = null, onBack = onBack) {
            Box(Modifier.size(38.dp).background(p.tealL, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) { Text("${RapidSession.count + tick * 0}", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth().qcard().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.Icon(if (fam != null) Fa.PeopleRoof else Fa.HouseChimneyMedical, null, Modifier.size(22.dp), tint = p.pri)
                Column(Modifier.weight(1f)) {
                    Text(if (fam != null) "أسرة: ${fam.optString("fullName")}" else "أسرة جديدة", color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(if (fam != null) "${RapidSession.members} فرد مسجّل — الإدخالات التالية تُضاف لهذه الأسرة" else "ابدأ برب الأسرة، ثم الزوجة والأبناء بالتتابع", color = p.mute, fontSize = 13.sp)
                }
                if (fam != null) Btn("أسرة جديدة", { RapidSession.head = null; RapidSession.famId = ""; RapidSession.members = 0; setRel("head"); tick++ }, kind = BtnKind.Outline, small = true, icon = Fa.Plus)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("head" to "رب أسرة", "wife" to "زوجة", "son" to "ابن", "daughter" to "ابنة", "other" to "فرد آخر").forEach { (k, l) -> QChip(l, rel == k) { setRel(k) } }
            }
            Column(Modifier.fillMaxWidth().qcard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    QField("رقم الهوية *", nid, { v -> nid = dev.saged.grcs.data.Nid.digits(v).filter { it in '0'..'9' }.take(9); if (nid.isNotEmpty()) noId = false }, type = KeyboardType.Number, enabled = !noId, placeholder = "9 أرقام")
                    if (n9.isNotEmpty()) {
                        val st = dev.saged.grcs.data.Nid.status(n9)
                        val (t, c) = when { local != null -> "مسجّل مسبقاً بهذا المخيم: ${local.optString("fullName")}" to p.danger; other != null -> other!! to p.danger; st == "valid" -> "رقم صالح" to p.success; st == "format" -> "الرقم يجب أن يكون 9 أرقام" to p.warn; else -> "لا يجتاز خانة التحقق — تأكد من الرقم" to p.warn }
                        Text(t, color = c, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                    }
                    Row(Modifier.padding(top = 6.dp).clickable { noId = !noId; if (noId) nid = "" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val sh = RoundedCornerShape(3.dp)
                        Box(Modifier.size(16.dp).background(if (noId) p.pri else p.surface, sh).border(1.5.dp, if (noId) p.pri else p.mute, sh), contentAlignment = Alignment.Center) { if (noId) androidx.compose.material3.Icon(Fa.Check, null, Modifier.size(10.dp), tint = androidx.compose.ui.graphics.Color.White) }
                        Text("لا يوجد رقم هوية بعد (مولود لم تصدر هويته)", color = p.mute, fontSize = 12.sp)
                    }
                }
                Column {
                    QField("الاسم الأول / الكامل", name, { name = it }, placeholder = "اكتب الاسم الأول واختر من الاقتراحات")
                    if (suggestions.isNotEmpty()) FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        suggestions.forEach { sgt -> Text(sgt, color = p.head, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.background(p.surface2, RoundedCornerShape(50)).border(1.dp, p.line, RoundedCornerShape(50)).clickable { name = sgt; if (rel == "head" || rel == "other") GenderInfer.infer(sgt).takeIf { it == "male" || it == "female" }?.let { gender = it } }.padding(horizontal = 12.dp, vertical = 6.dp)) }
                    }
                    val fn = finalName()
                    if (fn.isNotEmpty() && fn != name.trim()) Text("سيُحفظ باسم: $fn", color = p.mute, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
                // الجنس يُخفى للابن/الابنة/الزوجة لأنه معلوم من الصلة
                if (rel !in listOf("son", "daughter", "wife")) Column {
                    Text("الجنس", color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
                    dev.saged.grcs.ui.people.GenderSeg(gender, unknown = false) { gender = it }
                }
                Column {
                    dev.saged.grcs.ui.components.DateField("تاريخ الميلاد", dob, { dob = it }); Spacer(Modifier.height(4.dp))
                    dev.saged.grcs.ui.people.AgeChips(dob, gender.ifEmpty { "unknown" }, if (rel == "head") "head" else if (rel == "wife") "wife" else "child", rel == "head")
                }
                // الهاتف لرب الأسرة فقط
                if (rel == "head") QField("رقم الهاتف", phone, { phone = it }, type = KeyboardType.Phone)
                if (msg.isNotEmpty()) Text(msg, color = p.danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            dev.saged.grcs.ui.components.SectionTitle("آخر ما حُفظ")
            if (RapidSession.saved.isEmpty()) Text("لم يُحفظ شيء بعد في هذه الجلسة", color = p.mute, fontSize = 13.sp)
            RapidSession.saved.take(6).forEach { sp ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Icon(Fa.CircleCheck, null, Modifier.size(14.dp), tint = p.success)
                    Text(sp.optString("fullName"), color = p.ink, fontSize = 14.sp, modifier = Modifier.weight(1f)); Text(dev.saged.grcs.data.Rel.label(sp), color = p.mute, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(p.line))
        Row(Modifier.fillMaxWidth().background(p.surface).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (RapidSession.saved.isNotEmpty()) Btn("تراجع", { undo() }, kind = BtnKind.Outline, small = false, icon = Fa.RotateLeft)
            Btn(if (busy) "جارٍ الحفظ…" else "حفظ والتالي", { if (!busy) save() }, Modifier.weight(1f), enabled = !busy, icon = Fa.Check)
        }
    }
}
