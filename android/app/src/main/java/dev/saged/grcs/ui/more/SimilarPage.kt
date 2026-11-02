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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.SimPair
import dev.saged.grcs.data.Similar
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SimilarPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val camps = remember { data.campList(auth) }
    var camp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var pairs by remember { mutableStateOf<List<SimPair>?>(null) }
    var rev by remember { mutableStateOf(0) }
    var note by remember { mutableStateOf("") }
    LaunchedEffect(camp, rev) { pairs = null; pairs = withContext(Dispatchers.Default) { data.similar(camp) } }
    fun drop(o: JSONObject) {
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching { data.trash.put(auth, camp, "person", o.optString("fullName").ifEmpty { "—" }, listOf(o)) }
                data.people.delete(camp, o); data.afterSave(auth, camp, o, true); data.pushNow(auth, camp)
            }
            note = "حُذف السجل المكرر (محفوظ في السلة 30 يوماً)"; rev++
        }
    }
    @Composable fun Side(x: JSONObject, keep: Boolean) {
        Column(Modifier.fillMaxWidth().background(if (keep) p.bg else p.surface, RoundedCornerShape(12.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(x.optString("fullName"), color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(listOfNotNull(if (x.optBoolean("isFamilyHead")) "رب أسرة" else null, x.optString("nationalId").takeIf { it.isNotEmpty() },
                x.optString("birthDate").takeIf { it.isNotEmpty() && it != "null" }, x.optString("phone").takeIf { it.isNotEmpty() }).joinToString(" · ").ifEmpty { "بلا بيانات إضافية" }, color = p.mute, fontSize = 11.sp)
        }
    }
    Column(Modifier.fillMaxSize()) {
        AppHeader("أسماء متشابهة", "${camps.firstOrNull { it.first == camp }?.second ?: camp} — ${pairs?.size ?: 0} زوج", conn = null, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (camps.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { camps.forEach { (id, n) -> QChip(n, id == camp) { camp = id } } }
            Text("سجلات داخل نفس الأسرة يُرجَّح أنها لنفس الشخص (اختلاف همزة أو تشكيل أو حرف). الدمج يُبقي السجل المختار ويكمل حقوله الفارغة من الآخر.", color = p.mute, fontSize = 13.sp)
            if (note.isNotEmpty()) Text(note, color = p.ok, fontSize = 12.sp)
            if (pairs == null) Text("جارٍ الفحص…", color = p.mute, fontSize = 13.sp)
            else if (pairs!!.isEmpty()) Column(Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("لا توجد أسماء متشابهة", color = p.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("البيانات نظيفة من التكرار المحتمل داخل الأسر", color = p.mute, fontSize = 14.sp)
            }
            pairs?.take(80)?.forEach { pr ->
                val keepA = Similar.keepFirst(pr)
                Column(Modifier.fillMaxWidth().qcard().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Side(pr.a, keepA); Side(pr.b, !keepA)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QAct("احذف الأول", false) { drop(pr.a) }
                        QAct("احذف الثاني", false) { drop(pr.b) }
                        QAct("ليسا مكررين", false) { data.ignoreSimilar(pr.a, pr.b); rev++ }
                    }
                }
            }
        }
    }
}
