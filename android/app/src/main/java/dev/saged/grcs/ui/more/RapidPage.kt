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
import kotlinx.coroutines.withContext

@Composable
fun RapidPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val camps = remember { data.campList(auth) }
    var camp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var rel by remember { mutableStateOf("head") }
    var nid by remember { mutableStateOf("") }
    var noId by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var famId by remember { mutableStateOf("") }
    var famName by remember { mutableStateOf("") }
    var count by remember { mutableStateOf(0) }
    var last by remember { mutableStateOf<JSONObject?>(null) }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val log = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var known by remember(camp) { mutableStateOf<Set<String>>(emptySet()) }
    androidx.compose.runtime.LaunchedEffect(camp, count) { known = withContext(Dispatchers.IO) { data.people.persons(camp).map { it.optString("nationalId") }.filter { it.isNotEmpty() }.toSet() } }
    fun digits(s: String) = s.map { if (it in '٠'..'٩') '0' + (it - '٠') else it }.filter { it.isDigit() }.joinToString("")
    fun reset() { nid = ""; noId = false; name = ""; gender = ""; dob = ""; phone = "" }
    fun save() {
        val n = name.trim(); val id = digits(nid)
        if (n.length < 2) { msg = "أدخل الاسم"; return }
        if (!noId && id.length < 9) { msg = "رقم الهوية 9 أرقام على الأقل أو فعّل «بلا هوية»"; return }
        if (rel != "head" && famId.isEmpty()) { msg = "ابدأ برب الأسرة أولاً"; return }
        if (dob.isNotEmpty() && !Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(dob)) { msg = "التاريخ بصيغة 1990-05-21"; return }
        busy = true
        scope.launch {
            val err = withContext(Dispatchers.IO) {
                val existing = data.people.persons(camp)
                if (id.isNotEmpty() && existing.any { it.optString("nationalId") == id }) "الهوية موجودة مسبقاً في هذا المخيم"
                else if (id.isNotEmpty()) data.checkNid(auth, id, camp)
                else null
            }
            if (err != null) { msg = err; busy = false; return@launch }
            val head = rel == "head"
            val fid = if (head) id.ifEmpty { java.util.UUID.randomUUID().toString() } else famId
            val g = gender.ifEmpty { when (rel) { "wife", "daughter" -> "female"; "son" -> "male"; else -> GenderInfer.infer(n) } }
            val now = Clock.now()
            val o = JSONObject().put("id", java.util.UUID.randomUUID().toString()).put("campId", camp).put("fullName", n).put("nationalId", id)
                .put("birthDate", if (dob.isEmpty()) JSONObject.NULL else dob).put("calculatedAge", Stats.ageOfDob(dob) ?: JSONObject.NULL).put("gender", g)
                .put("phone", digits(phone)).put("address", "").put("notes", "").put("isFamilyHead", head).put("familyId", fid)
                .put("relation", rel).put("maritalStatus", "").put("isPregnant", false).put("isBreastfeeding", false)
                .put("hasChronicDisease", false).put("hasDisability", false).put("createdAt", now).put("createdBy", auth.user.id).put("updatedBy", auth.user.id)
            val saved = withContext(Dispatchers.IO) { data.people.save(camp, o).also { s -> if (head) runCatching { data.afterSave(auth, camp, s, false) }; data.pushNow(auth, camp) } }
            if (head) { famId = fid; famName = n }
            last = saved; count++; log.add(0, n + " — " + (if (head) "رب أسرة" else rel)); msg = "حُفظ: $n"; reset(); busy = false
            if (head) rel = "wife"
        }
    }
    Column(Modifier.fillMaxSize()) {
        AppHeader("إدخال سريع", camps.firstOrNull { it.first == camp }?.second.orEmpty(), conn = null, onBack = onBack) {
            androidx.compose.foundation.layout.Box(Modifier.size(44.dp).background(p.tealL, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) { Text("$count", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (camps.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { camps.forEach { (id, n) -> QChip(n, id == camp) { camp = id; famId = ""; famName = "" } } }
            Column(Modifier.fillMaxWidth().qcard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (famName.isEmpty()) "أسرة جديدة" else "أسرة $famName", color = p.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(if (famName.isEmpty()) "ابدأ برب الأسرة، ثم الزوجة والأبناء بالتتابع" else "أضف الزوجة والأبناء بالتتابع", color = p.mute, fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("head" to "رب أسرة", "wife" to "زوجة", "son" to "ابن", "daughter" to "ابنة", "other" to "فرد آخر").forEach { (k, l) -> QChip(l, rel == k) { rel = k } }
            }
            Column(Modifier.fillMaxWidth().qcard().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                QField("رقم الهوية *", nid, { nid = it }, type = KeyboardType.Number)
                if (!noId && nid.isNotEmpty()) {
                    val d = digits(nid)
                    Text(if (d.length < 9) "${d.length}/9 أرقام" else if (d in known) "الهوية مسجّلة مسبقاً" else "الهوية متاحة", color = if (d.length >= 9 && d in known) p.danger else if (d.length >= 9) p.success else p.mute, fontSize = 12.sp)
                }
                Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable { noId = !noId }, verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(noId, { noId = it })
                    Text("لا يوجد رقم هوية بعد (مولود لم تصدر هويته)", color = p.mute, fontSize = 13.sp)
                }
                QField("الاسم الأول / الكامل", name, { name = it })
                if (rel != "head" && famName.isNotEmpty() && name.isNotBlank() && name.trim().split(" ").size < 3) {
                    val sur = famName.trim().split(" ").drop(1).joinToString(" ")
                    if (sur.isNotEmpty()) QChip("إضافة «$sur»", false) { name = name.trim() + " " + sur }
                }
                Text("الجنس", color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("male" to "ذكر", "female" to "أنثى").forEach { (k, l) ->
                        Box(Modifier.weight(1f).heightIn(min = 52.dp).background(if (gender == k) p.navy else p.surface, RoundedCornerShape(14.dp)).border(1.dp, if (gender == k) p.navy else p.line, RoundedCornerShape(14.dp)).clickable { gender = if (gender == k) "" else k }, contentAlignment = Alignment.Center) {
                            Text(l, color = if (gender == k) androidx.compose.ui.graphics.Color.White else p.mute, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
                QField("تاريخ الميلاد", dob, { dob = it }, type = KeyboardType.Number)
                Text("أدخل تاريخ الميلاد (1990-05-21) ليُصنَّف تلقائياً", color = p.mute, fontSize = 12.sp)
                QField("رقم الهاتف", phone, { phone = it }, type = KeyboardType.Phone)
                if (msg.isNotEmpty()) Text(msg, color = p.mute, fontSize = 12.sp)
                if (last != null || famId.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (last != null) QAct("تراجع عن الأخير", false) {
                        val l = last!!; last = null
                        scope.launch { withContext(Dispatchers.IO) { data.people.delete(camp, l); data.afterSave(auth, camp, l, true); data.pushNow(auth, camp) }; count = maxOf(0, count - 1); msg = "تم التراجع"
                            if (l.optBoolean("isFamilyHead")) { famId = ""; famName = ""; rel = "head" } }
                    }
                    if (famId.isNotEmpty()) QAct("أسرة جديدة", false) { famId = ""; famName = ""; rel = "head"; reset() }
                }
            }
            if (log.isNotEmpty()) {
                Text("آخر ما حُفظ", color = p.mute, fontSize = 12.sp)
                log.take(8).forEach { Text("• $it", color = p.ink, fontSize = 13.sp) }
            }
        }
        Box(Modifier.fillMaxWidth().background(p.surface).padding(12.dp)) {
            Btn(if (busy) "جارٍ الحفظ…" else "حفظ والتالي", { if (!busy) save() }, Modifier.fillMaxWidth(), enabled = !busy)
        }
    }
}
