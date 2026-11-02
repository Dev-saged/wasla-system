package dev.saged.grcs.ui.more

import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.qcard
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.Cond
import dev.saged.grcs.data.Query
import dev.saged.grcs.data.Saved
import dev.saged.grcs.data.QResult
import dev.saged.grcs.ui.components.QField
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.launch
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun QueryPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val camps = remember { data.campList(auth) }
    var camp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    val persons = remember(camp) { data.people.persons(camp) }
    var groups by remember { mutableStateOf<List<List<Cond>>>(emptyList()) }
    var result by remember { mutableStateOf<QResult?>(null) }
    var title by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(data.savedQueries()) }
    var fieldK by remember { mutableStateOf<String?>(null) }
    var vMin by remember { mutableStateOf("") }
    var vMax by remember { mutableStateOf("") }
    var saveName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var counts by remember(camp) { mutableStateOf<List<Int>?>(null) }
    LaunchedEffect(camp) { counts = withContext(Dispatchers.Default) { Query.categories.map { Query.run(persons, it.groups).persons.size } } }
    fun run(g: List<List<Cond>>, t: String) { groups = g; title = t; result = null; scope.launch { result = withContext(Dispatchers.Default) { Query.run(persons, g) } } }
    fun addCond(c: Cond, newGroup: Boolean) { run(if (newGroup || groups.isEmpty()) groups + listOf(listOf(c)) else groups.dropLast(1) + listOf(groups.last() + c), "استعلام مخصص"); fieldK = null; vMin = ""; vMax = "" }
    Column(Modifier.fillMaxSize()) {
        AppHeader("منشئ الاستعلامات", camps.firstOrNull { it.first == camp }?.second ?: "كل المخيمات", conn = null, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (camps.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { camps.forEach { (id, n) -> QChip(n, id == camp) { camp = id; groups = emptyList(); result = null } } }
            Text("الفئات", color = p.mute, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Query.categories.forEachIndexed { i, c -> QChip(c.label + (counts?.getOrNull(i)?.let { " · $it" } ?: ""), title == c.label) { run(c.groups, c.label) } } }
            Text("استعلامات جاهزة", color = p.mute, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Query.presets.forEach { c -> QChip(c.label, title == c.label) { run(c.groups, c.label) } } }
            if (saved.isNotEmpty()) {
                Text("المحفوظة", color = p.mute, fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { saved.forEach { s -> QChip(s.name, title == s.name) { run(s.groups, s.name) } } }
            }
            Text("بناء استعلام", color = p.mute, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Query.fields.forEach { f -> QChip(f.label, fieldK == f.k) { fieldK = if (fieldK == f.k) null else f.k; vMin = ""; vMax = "" } } }
            fieldK?.let { k ->
                val f = Query.fields.first { it.k == k }
                Column(Modifier.fillMaxWidth().qcard().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (f.range) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QField("من", vMin, { vMin = it.filter(Char::isDigit) }, Modifier.weight(1f)); QField("إلى", vMax, { vMax = it.filter(Char::isDigit) }, Modifier.weight(1f)) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QAct("و (تضييق)", true) { addCond(Cond(k, min = vMin, max = vMax), false) }
                            QAct("أو (مجموعة جديدة)", false) { addCond(Cond(k, min = vMin, max = vMax), true) }
                        }
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { f.options.forEach { (v, l) ->
                            QChip(l, false) { addCond(Cond(k, v), false) }
                        } }
                        Text("اضغط القيمة للتضييق (و). لإضافة مجموعة بديلة (أو) استخدم «أو» أدناه ثم القيمة.", color = p.mute, fontSize = 11.sp)
                    }
                }
            }
            if (groups.isNotEmpty()) {
                groups.forEachIndexed { gi, g -> Text((if (gi > 0) "أو  " else "") + g.joinToString("  و  ") { Query.describe(it) }, color = p.ink, fontSize = 12.sp) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QAct("مسح الشروط", false) { groups = emptyList(); result = null; title = "" }
                    QAct("شرط بديل (أو)", false) { groups = groups + listOf(emptyList()) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    QField("اسم للحفظ", saveName, { saveName = it }, Modifier.weight(1f))
                }
                if (note.isNotEmpty()) Text(note, color = p.mute, fontSize = 12.sp)
            }
            val r = result
            if (groups.isNotEmpty() && r == null) Text("جارٍ الحساب…", color = p.mute, fontSize = 13.sp)
            if (r != null) {
                var msg by remember(r) { mutableStateOf("") }
                Row(Modifier.fillMaxWidth().background(p.navy, RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${r.persons.size}", color = androidx.compose.ui.graphics.Color.White, fontFamily = GrcsFonts.display, fontSize = 30.sp)
                    Text("فرد مطابق · ${r.families} أسرة", color = androidx.compose.ui.graphics.Color.White.copy(alpha = .8f), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    if (r.persons.isNotEmpty()) QAct("تصدير", true) {
                        scope.launch { val e = withContext(Dispatchers.IO) { data.export.run("xlsx", camp, (title.ifEmpty { "استعلام" }) + "-" + (camps.firstOrNull { it.first == camp }?.second ?: camp), r.persons, false) }
                            msg = if (e.ok) "حُفظ: ${e.name}" else "تعذّر الحفظ" }
                    }
                    if (groups.isNotEmpty()) QAct("حفظ", false) { if (saveName.isNotBlank()) { saved = saved.filter { it.name != saveName.trim() } + Saved(saveName.trim(), groups.filter { it.isNotEmpty() }); data.saveQueries(saved); title = saveName.trim(); saveName = ""; note = "تم حفظ الاستعلام" } else note = "اكتب اسماً للحفظ أولاً" }
                }
                if (msg.isNotEmpty()) Text(msg, color = p.mute, fontSize = 12.sp)
                r.persons.take(100).forEach { x ->
                    val fem = x.optString("gender") == "female"
                    Row(Modifier.fillMaxWidth().qcard().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(44.dp).background(if (fem) p.pregnant else p.navy, RoundedCornerShape(12.dp)))
                        Column(Modifier.weight(1f)) {
                            Text(x.optString("fullName"), color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(listOf(x.optString("nationalId").ifEmpty { "بلا هوية" }, x.optString("phone").ifEmpty { "بلا هاتف" }).joinToString(" · "), color = p.mute, fontSize = 12.sp)
                            Text((camps.firstOrNull { it.first == camp }?.second ?: camp) + " · " + (dev.saged.grcs.data.Stats.ageOf(x)?.let { "$it سنة" } ?: "—"), color = p.mute, fontSize = 12.sp)
                        }
                    }
                }
                if (r.persons.size > 100) Text("يُعرض أول 100 — صدّر النتيجة للقائمة الكاملة", color = p.mute, fontSize = 11.sp)
            }
        }
    }
}
