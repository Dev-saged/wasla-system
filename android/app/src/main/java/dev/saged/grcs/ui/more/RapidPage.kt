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
            last = saved; count++; msg = "حُفظ: $n"; reset(); busy = false
            if (head) rel = "wife"
        }
    }
    Column(Modifier.fillMaxSize()) {
        AppHeader("إدخال سريع — $count", "", conn = null, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (camps.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { camps.forEach { (id, n) -> QChip(n, id == camp) { camp = id; famId = ""; famName = "" } } }
            if (famName.isNotEmpty()) Text("الأسرة الحالية: $famName", color = p.pri, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("head" to "رب أسرة", "wife" to "زوجة", "son" to "ابن", "daughter" to "ابنة", "other" to "فرد آخر").forEach { (k, l) -> QChip(l, rel == k) { rel = k } }
            }
            QField("رقم الهوية", nid, { nid = it }, type = KeyboardType.Number)
            QChip(if (noId) "بلا هوية ✓" else "بلا هوية (مولود لم تصدر هويته)", noId) { noId = !noId }
            QField("الاسم الكامل", name, { name = it })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QChip("ذكر", gender == "male") { gender = if (gender == "male") "" else "male" }; QChip("أنثى", gender == "female") { gender = if (gender == "female") "" else "female" } }
            QField("تاريخ الميلاد (1990-05-21)", dob, { dob = it }, type = KeyboardType.Number)
            QField("الجوال", phone, { phone = it }, type = KeyboardType.Phone)
            if (msg.isNotEmpty()) Text(msg, color = p.mute, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QAct(if (busy) "جارٍ الحفظ…" else "حفظ والتالي", true) { if (!busy) save() }
                if (last != null) QAct("تراجع عن الأخير", false) {
                    val l = last!!; last = null
                    scope.launch { withContext(Dispatchers.IO) { data.people.delete(camp, l); data.afterSave(auth, camp, l, true); data.pushNow(auth, camp) }; count = maxOf(0, count - 1); msg = "تم التراجع"
                        if (l.optBoolean("isFamilyHead")) { famId = ""; famName = ""; rel = "head" } }
                }
                if (famId.isNotEmpty()) QAct("أسرة جديدة", false) { famId = ""; famName = ""; rel = "head"; reset() }
            }
        }
    }
}
