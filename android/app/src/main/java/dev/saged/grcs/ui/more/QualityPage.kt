package dev.saged.grcs.ui.more

import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.qcard
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import dev.saged.grcs.data.Stats
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import org.json.JSONObject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.Issue
import dev.saged.grcs.data.Quality
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun QualityPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val camps = remember { data.ctxCamps(auth) }
    var camp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var persons by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var pct by remember { mutableStateOf(100) }
    var issues by remember { mutableStateOf<List<Issue>>(emptyList()) }
    var shown by remember { mutableStateOf(mapOf<String, Int>()) }
    var rev by remember { mutableStateOf(0) }
    var edit by remember { mutableStateOf<JSONObject?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(camp, rev) {
        val ps = withContext(Dispatchers.IO) { camps.flatMap { data.people.persons(it.first) } }
        val r = withContext(Dispatchers.IO) { Quality.report(ps) }
        persons = ps; pct = r.first; issues = r.second
    }
    edit?.let { ed ->
        dev.saged.grcs.ui.people.PersonForm(ed, persons.filter { it.optString("campId").let { c -> c.isEmpty() || c == ed.optString("campId").ifEmpty { camp } } }, ed.optString("campId").ifEmpty { camp }, auth.user.id, err, { edit = null; err = null }) { r ->
            scope.launch {
                val e = withContext(Dispatchers.IO) { data.saveForm(auth, r.main.optString("campId").ifEmpty { camp }, r) }
                if (e != null) err = e else { edit = null; err = null; rev++ }
            }
        }
        return
    }
    val heads = remember(persons) { persons.filter { it.optBoolean("isFamilyHead") }.associateBy { it.optString("familyId") } }
    fun line(x: JSONObject, extra: String): String {
        val r = x.optString("relation")
        val role = if (x.optBoolean("isFamilyHead")) "رب الأسرة" else when (r) { "wife" -> "الزوجة"; "husband" -> "الزوج"; else -> "فرد من الأسرة" }
        val a = Stats.ageOf(x)?.let { "$it سنة" }
        val tail = extra.ifEmpty { if (x.optBoolean("isFamilyHead")) x.optString("nationalId") else heads[x.optString("familyId")]?.optString("fullName")?.let { "أسرة $it" }.orEmpty() }
        return listOfNotNull(role, a, tail.ifEmpty { null }).joinToString(" · ")
    }
    val campName = if (camps.size > 1) "كل المخيمات" else camps.firstOrNull()?.second ?: camp
    val notes = issues.filter { it.key != "stale" }.sumOf { it.people.size }
    Column(Modifier.fillMaxSize()) {
        AppHeader("مركز الجودة", "$campName — ${persons.size} فرد", conn = null, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val pc = if (pct < 60) p.danger else if (pct < 85) p.warn else p.ok
            Row(Modifier.fillMaxWidth().qcard().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("اكتمال جودة البيانات", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("$notes ملاحظة تحتاج إصلاحاً", color = p.mute, fontSize = 13.sp)
                }
                Box(Modifier.background(pc.copy(alpha = .14f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 8.dp)) { Text("$pct%", color = pc, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            }
            issues.forEach { i ->
                val n = i.people.size
                val lim = shown[i.key] ?: 25
                Column(Modifier.fillMaxWidth().qcard().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(i.label, color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        val c = if (n == 0) p.ok else p.danger
                        Box(Modifier.background(c.copy(alpha = .14f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) { Text("$n", color = c, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                    if (n > 0) {
                        Text(i.hint, color = p.mute, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        i.people.take(lim).forEach { x ->
                            val extra = when (i.key) { "bad" -> x.optString("nationalId"); "dob" -> x.optString("birthDate") + " — " + Quality.dobWhy(x, heads[x.optString("familyId")]); else -> "" }
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(x.optString("fullName"), color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(line(x, extra), color = p.mute, fontSize = 12.sp)
                                }
                                Btn(i.action, { edit = x }, kind = BtnKind.Outline, small = true)
                            }
                        }
                        if (n > lim) Btn("عرض المزيد (${n - lim})", { shown = shown + (i.key to lim + 25) }, Modifier.fillMaxWidth(), BtnKind.Outline, small = true)
                    }
                }
            }
        }
    }
}
