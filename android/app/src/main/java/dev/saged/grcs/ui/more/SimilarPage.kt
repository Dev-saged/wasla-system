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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SimilarPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val camps = remember { data.ctxCamps(auth) }
    var camp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var pairs by remember { mutableStateOf<List<SimPair>?>(null) }
    var rev by remember { mutableStateOf(0) }
    var note by remember { mutableStateOf("") }
    LaunchedEffect(rev) { pairs = null; pairs = withContext(Dispatchers.Default) { camps.flatMap { data.similar(it.first) } } }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    fun toast(t: String) = android.widget.Toast.makeText(ctx, t, android.widget.Toast.LENGTH_LONG).show()
    var sel by remember { mutableStateOf(mapOf<String, String>()) }
    var ask by remember { mutableStateOf<Pair<JSONObject, JSONObject>?>(null) }
    fun merge(keep: JSONObject, drop: JSONObject) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val c = keep.optString("campId").ifEmpty { camp }
                val k = dev.saged.grcs.data.Merge.persons(keep, drop).put("updatedBy", auth.user.id)
                data.people.delete(c, drop); data.afterSave(auth, c, drop, true)
                data.afterSave(auth, c, data.people.save(c, k), false)
                dev.saged.grcs.data.Audit.log(auth, c, "merge_person", k.optString("id"), k.optString("fullName"))
                data.pushNow(auth, c)
            }
            toast("تم الدمج"); rev++
        }
    }
    @Composable fun Side(x: JSONObject, keep: Boolean, onPick: () -> Unit) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (keep) p.pri.copy(alpha = .08f) else p.surface).clickable(onClick = onPick).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(18.dp).border(2.dp, if (keep) p.pri else p.mute, RoundedCornerShape(50)), contentAlignment = Alignment.Center) { if (keep) Box(Modifier.size(9.dp).background(p.pri, RoundedCornerShape(50))) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(x.optString("fullName"), color = p.ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                Text(dev.saged.grcs.data.Rel.label(x) + x.optString("birthDate").let { if (it.isNotEmpty() && it != "null") " · $it" else "" } + x.optString("nationalId").let { if (it.isNotEmpty()) " · $it" else " · بلا هوية" }, color = p.mute, fontSize = 12.sp)
            }
        }
    }
    ask?.let { (keep, drop) ->
        dev.saged.grcs.ui.components.ConfirmSheet("سيُبقى \"${keep.optString("fullName")}\" ويُحذف \"${drop.optString("fullName")}\" بعد نقل أي بيانات ناقصة منه.", "دمج سجلين", "دمج", dev.saged.grcs.ui.components.BtnKind.Primary, dev.saged.grcs.ui.icons.Fa.CodeMerge, onCancel = { ask = null }) { ask = null; merge(keep, drop) }
    }
    Column(Modifier.fillMaxSize()) {
        AppHeader("أسماء متشابهة", "${if (camps.size > 1) "كل المخيمات" else camps.firstOrNull()?.second ?: camp} — ${pairs?.size ?: 0} زوج", conn = null, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("سجلات داخل نفس الأسرة يُرجَّح أنها لنفس الشخص (اختلاف همزة أو تشكيل أو حرف). الدمج يُبقي السجل المختار ويكمل حقوله الفارغة من الآخر.", color = p.mute, fontSize = 13.sp)
            if (note.isNotEmpty()) Text(note, color = p.ok, fontSize = 12.sp)
            if (pairs == null) Text("جارٍ الفحص…", color = p.mute, fontSize = 13.sp)
            else if (pairs!!.isEmpty()) Column(Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("لا توجد أسماء متشابهة", color = p.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("البيانات نظيفة من التكرار المحتمل داخل الأسر", color = p.mute, fontSize = 14.sp)
            }
            pairs?.take(80)?.forEach { pr ->
                val key = listOf(pr.a.optString("id"), pr.b.optString("id")).sorted().joinToString("|")
                val keepA = (sel[key] ?: if (Similar.keepFirst(pr)) pr.a.optString("id") else pr.b.optString("id")) == pr.a.optString("id")
                Column(Modifier.fillMaxWidth().qcard().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dev.saged.grcs.ui.components.Bar(pr.score.toFloat(), p.pri, Modifier.weight(1f))
                        Text("${Math.round(pr.score * 100)}%", color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Side(pr.a, keepA) { sel = sel + (key to pr.a.optString("id")) }
                    Side(pr.b, !keepA) { sel = sel + (key to pr.b.optString("id")) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dev.saged.grcs.ui.components.Btn("ليسا نفس الشخص", { data.ignoreSimilar(pr.a, pr.b); rev++ }, Modifier.weight(1f), kind = dev.saged.grcs.ui.components.BtnKind.Outline, small = true)
                        dev.saged.grcs.ui.components.Btn("دمج بالمختار", {
                            val keep = if (keepA) pr.a else pr.b; val drop = if (keepA) pr.b else pr.a
                            if (drop.optBoolean("isFamilyHead")) toast("لا يمكن حذف رب الأسرة بالدمج — اختر سجله للإبقاء") else ask = keep to drop
                        }, Modifier.weight(1f), icon = dev.saged.grcs.ui.icons.Fa.CodeMerge, small = true)
                    }
                }
            }
        }
    }
}
