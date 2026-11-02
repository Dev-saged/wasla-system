package dev.saged.grcs.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
    var days by remember { mutableIntStateOf(30) }
    var status by remember { mutableStateOf("all") }
    var q by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<AidRow>>(emptyList()) }
    var rev by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(true) }
    val canWrite = auth.user.role != "beneficiary"
    val t = Aid.types.first { it.k == type }

    LaunchedEffect(camp, type, days, rev) {
        busy = true
        rows = withContext(Dispatchers.IO) {
            val from = if (days > 0) SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - days * 86_400_000L)) else ""
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
        AppHeader("المساعدات", "${t.label} · استلم ${fa(got)} من ${fa(rows.size)}", conn = null, onBack = onBack)
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (camps.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QChip("كل المخيمات", camp == "*") { camp = "*" }
                camps.forEach { (id, n) -> QChip(n, camp == id) { camp = id } }
            }
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(Aid.types, key = { it.k }) { a -> QChip(a.label, type == a.k) { type = a.k } } }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7 to "آخر 7 أيام", 30 to "آخر 30 يوماً", 90 to "آخر 90 يوماً", 0 to "الكل").forEach { (d, l) -> QChip(l, days == d) { days = d } }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QChip("الكل", status == "all") { status = "all" }; QChip("استلموا", status == "got") { status = "got" }; QChip("لم يستلموا", status == "not") { status = "not" }
            }
            SearchBox(q, { q = it }, "بحث بالاسم أو الهوية")
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
