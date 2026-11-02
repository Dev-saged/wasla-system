package dev.saged.grcs.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.Aid
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.EmptyState
import dev.saged.grcs.ui.components.Pill
import dev.saged.grcs.ui.components.QCard
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.components.SearchBox
import dev.saged.grcs.ui.components.fa
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private class AidRow(val camp: String, val campName: String, val p: JSONObject, val entry: JSONObject?)

/** شاشة المساعدات: المستحقون لنوع مساعدة ضمن فترة، مع حالة الاستلام والتسليم السريع والتراجع، عبر مخيم أو كل المخيمات. */
@Composable
fun AidScreen(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val camps = remember { data.campList(auth) }
    var camp by remember { mutableStateOf(if (camps.size > 1) "*" else camps.firstOrNull()?.first.orEmpty()) }
    var type by remember { mutableStateOf("food") }
    var period by remember { mutableStateOf("month") }
    var status by remember { mutableStateOf("all") }
    var q by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<AidRow>>(emptyList()) }
    var rev by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(true) }
    val canWrite = auth.user.role != "beneficiary"
    val t = Aid.types.first { it.k == type }

    LaunchedEffect(camp, type, period, rev) {
        busy = true
        rows = withContext(Dispatchers.IO) {
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val from = when (period) { "today" -> df.format(Date()); "month" -> df.format(Date()).substring(0, 8) + "01"; else -> "" }
            val ids = if (camp == "*") camps else camps.filter { it.first == camp }
            ids.flatMap { (cid, cn) ->
                data.people.persons(cid).filter { Aid.eligible(it, type) }.map { ps ->
                    AidRow(cid, cn, ps, Aid.list(ps).lastOrNull { it.optString("t") == type && it.optString("d") >= from })
                }
            }
        }
        busy = false
    }

    fun give(r: AidRow) = scope.launch {
        withContext(Dispatchers.IO) {
            val o = data.people.save(r.camp, Aid.give(r.p, type, 1.0, "", auth.user.id))
            data.afterSave(auth, r.camp, o, false); data.pushNow(auth, r.camp)
        }
        rev++
    }
    fun undo(r: AidRow) = scope.launch {
        withContext(Dispatchers.IO) {
            val o = data.people.save(r.camp, Aid.undo(r.p, r.entry!!.optString("id")))
            data.afterSave(auth, r.camp, o, false); data.pushNow(auth, r.camp)
        }
        rev++
    }

    val nq = q.trim()
    val shown = rows.filter { (status == "all" || (status == "got") == (it.entry != null)) && (nq.isEmpty() || it.p.optString("fullName").contains(nq) || it.p.optString("nationalId").contains(nq)) }
    val got = rows.count { it.entry != null }

    Column(Modifier.fillMaxSize()) {
        AppHeader("المساعدات", if (camp == "*") "ملخص كل المخيمات" else camps.firstOrNull { it.first == camp }?.second.orEmpty(), conn = null, onBack = if (camp != "*" && camps.size > 1) ({ camp = "*" }) else onBack)
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(Aid.types, key = { it.k }) { a -> QChip(a.label, type == a.k) { type = a.k } } }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("today" to "اليوم", "month" to "هذا الشهر", "all" to "كل الفترات").forEach { (d, l) -> QChip(l, period == d) { period = d } }
            }
            val basis = when (type) { "food", "cash" -> "لكل أسرة (رب الأسرة)"; "milk", "diapers" -> "لكل طفل دون سنتين"; "prenatal" -> "لكل حامل ومرضعة"; "medicine" -> "لكل مريض مزمن"; "assist" -> "لكل ذي إعاقة"; "orphan" -> "لكل يتيم"; "elder" -> "لكل مسن +60"; else -> "لكل فرد" }
            QCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("${fa(rows.size)} مؤهّل", color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("${fa(got)} استلم", color = p.success, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("${fa(rows.size - got)} متبقٍ", color = p.warn, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                dev.saged.grcs.ui.components.Bar(if (rows.isEmpty()) 0f else got.toFloat() / rows.size, p.success)
                Text("$basis — ${if (camp == "*") "كل المخيمات" else camps.firstOrNull { it.first == camp }?.second.orEmpty()} — ${if (rows.isEmpty()) 0 else Math.round(got * 100f / rows.size)}%", color = p.mute, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
            if (camp != "*") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QChip("الكل", status == "all") { status = "all" }; QChip("استلموا", status == "got") { status = "got" }; QChip("لم يستلموا", status == "not") { status = "not" }
                }
                SearchBox(q, { q = it }, "بحث بالاسم أو الهوية")
            }
        }
        if (camp == "*") {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                camps.forEach { (cid, cn) ->
                    val el = rows.count { it.camp == cid }; val gt = rows.count { it.camp == cid && it.entry != null }
                    QCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(cn, color = p.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${fa(gt)} / ${fa(el)} (${if (el == 0) 0 else Math.round(gt * 100f / el)}%)", color = p.mute, fontSize = 12.sp)
                                Spacer(Modifier.height(6.dp))
                                dev.saged.grcs.ui.components.Bar(if (el == 0) 0f else gt.toFloat() / el, p.success)
                            }
                            Spacer(Modifier.padding(horizontal = 6.dp))
                            Btn("فتح المخيم", { camp = cid }, kind = BtnKind.Outline, small = true)
                        }
                    }
                }
            }
            return@Column
        }
        if (!busy && shown.isEmpty()) EmptyState(QIcons.Heart, "لا يوجد مستحقون", "غيّر النوع أو الفترة أو الحالة")
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown.take(300), key = { it.camp + it.p.optString("id") }) { r ->
                QCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(r.p.optString("fullName"), color = p.ink, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, maxLines = 1)
                            Text((if (camp == "*") r.campName + " · " else "") + r.p.optString("nationalId").ifEmpty { "بلا هوية" }, color = p.mute, fontSize = 12.sp)
                        }
                        if (r.entry != null) {
                            Pill("استلم ${r.entry.optString("d")}", p.success)
                            if (canWrite) Btn("تراجع", { undo(r) }, kind = BtnKind.Ghost, small = true)
                        } else if (canWrite && t.ask == null) Btn("تسليم", { give(r) }, small = true, icon = QIcons.Check)
                        else Pill(if (t.ask != null) "افتح الأسرة" else "لم يستلم", p.warn)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
