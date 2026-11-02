package dev.saged.grcs.ui.camps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import dev.saged.grcs.data.CampInfo
import dev.saged.grcs.data.CampSummary
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.Quality
import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.EmptyState
import dev.saged.grcs.ui.components.HeaderBtn
import dev.saged.grcs.ui.components.QCard
import dev.saged.grcs.ui.components.QField
import dev.saged.grcs.ui.components.Ring
import dev.saged.grcs.ui.components.SearchBox
import dev.saged.grcs.ui.components.fa
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** شاشة المخيمات للمدير كما في الويب: أدوات عامة أعلى الشاشة، بحث موحّد، ثم بطاقة لكل مخيم (أرقام، اكتمال، تفاصيل، كشف، حذف). */
@Composable
fun CampsScreen(auth: AuthState, data: DataRepo, onOpen: (String) -> Unit = {}, onFamily: (String, String) -> Unit = { _, _ -> }) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    var camps by remember { mutableStateOf(data.cached(auth).sortedByDescending { it.stats.total }) }
    val admin = auth.user.role == "admin"
    var edit by remember { mutableStateOf<CampSummary?>(null) }
    var creating by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var sub by remember { mutableStateOf("") }
    var q by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<Triple<String, String, org.json.JSONObject>>>(emptyList()) }
    var quality by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var infos by remember { mutableStateOf<Map<String, CampInfo>>(emptyMap()) }
    var note by remember { mutableStateOf("") }
    fun reload() { camps = data.cached(auth).sortedByDescending { it.stats.total } }
    val fmt = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US) }

    LaunchedEffect(camps.size) {
        quality = withContext(Dispatchers.IO) { camps.associate { c -> c.id to (runCatching { Quality.report(data.people.persons(c.id)).first }.getOrDefault(0)) } }
        infos = withContext(Dispatchers.IO) { camps.mapNotNull { c -> runCatching { c.id to data.campInfo(auth, c.id) }.getOrNull() }.toMap() }
    }
    LaunchedEffect(q) {
        hits = if (q.trim().length < 2) emptyList() else withContext(Dispatchers.IO) {
            val n = q.trim()
            data.campList(auth).flatMap { (cid, cn) -> data.people.persons(cid).filter { it.optString("fullName").contains(n) || it.optString("nationalId").contains(n) }.map { Triple(cid, cn, it) } }.take(30)
        }
    }
    when {
        sub == "cats" -> { CatsPage(auth, data) { sub = "" }; return }
        sub == "aid" -> { dev.saged.grcs.ui.more.AidScreen(auth, data) { sub = "" }; return }
        sub == "quality" -> { dev.saged.grcs.ui.more.QualityPage(auth, data) { sub = "" }; return }
        sub == "export" -> { dev.saged.grcs.ui.more.ExportPage(auth, data) { sub = "" }; return }
        sub == "reconcile" -> { dev.saged.grcs.ui.more.ReconcilePage(auth, data) { sub = "" }; return }
        sub == "danger" -> { dev.saged.grcs.ui.more.CampDangerPage(auth, data) { sub = ""; reload() }; return }
        sub.startsWith("detail:") -> { dev.saged.grcs.ui.home.HomeScreen(auth, data, {}, { }, { }, onlyCamp = sub.removePrefix("detail:"), onBack = { sub = "" }, onPeople = { onOpen(sub.removePrefix("detail:")) }); return }
    }
    Column(Modifier.fillMaxSize()) {
        AppHeader("المخيمات", "${camps.size} مخيم · ${fa(camps.sumOf { it.stats.families })} أسرة") {
            if (admin) HeaderBtn(QIcons.Plus, "مخيم جديد") { creating = true }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (admin) {
                Btn("مطابقة كشف وطني شامل (مطابقة وتكميل بيانات)", { sub = "reconcile" }, Modifier.fillMaxWidth(), BtnKind.Outline, QIcons.Search)
                Btn("كشوفات التصنيفات لكل المخيمات (أيتام، حوامل…)", { sub = "cats" }, Modifier.fillMaxWidth(), BtnKind.Outline, QIcons.Filter)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn("المساعدات — كل المخيمات", { sub = "aid" }, Modifier.weight(1f), BtnKind.Outline, QIcons.Heart)
                    Btn("مركز الجودة", { sub = "quality" }, Modifier.weight(1f), BtnKind.Outline, QIcons.Check)
                }
                Btn("تصدير بيانات المخيمات", { sub = "export" }, Modifier.fillMaxWidth(), BtnKind.Navy, QIcons.Export)
                SearchBox(q, { q = it }, "بحث موحّد: اسم شخص أو مخيم…")
                hits.forEach { (cid, cn, ps) ->
                    QCard(Modifier, { onFamily(cid, ps.optString("familyId").ifEmpty { "i_" + ps.optString("id") }) }) {
                        Text(ps.optString("fullName"), color = p.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(cn + " · " + ps.optString("nationalId").ifEmpty { "بلا هوية" }, color = p.mute, fontSize = 12.sp)
                    }
                }
            }
            if (note.isNotEmpty()) Text(note, color = p.mute, fontSize = 12.sp)
            if (camps.isEmpty()) EmptyState(QIcons.Camp, "لا بيانات بعد", "افتح الرئيسية للمزامنة")
            camps.filter { q.isBlank() || it.name.contains(q.trim()) }.forEach { c ->
                val info = infos[c.id]
                QCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.name, color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                        if (admin) androidx.compose.foundation.layout.Box(Modifier.size(44.dp).clickable { edit = c }, contentAlignment = Alignment.Center) { androidx.compose.material3.Icon(QIcons.Edit, "تعديل الاسم", Modifier.size(20.dp), tint = p.mute) }
                    }
                    Text(listOfNotNull(info?.supervisor?.ifEmpty { null }, info?.supervisorPhone?.ifEmpty { null }).joinToString(" — ").ifEmpty { "—" }, color = p.mute, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Stat(fa(c.stats.total), "فرد"); Stat(fa(c.stats.families), "أسرة")
                        Stat(info?.lastUpdated?.takeIf { it > 0 }?.let { fmt.format(Date(it)) } ?: "—", "آخر تحديث")
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("اكتمال البيانات", color = p.mute, fontSize = 12.sp)
                        val pct = quality[c.id] ?: 0
                        Ring(pct / 100f, if (pct >= 85) p.success else if (pct >= 60) p.warn else p.danger, 52.dp, "$pct%")
                    }
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Btn("عرض التفاصيل", { sub = "detail:" + c.id }, Modifier.fillMaxWidth(), BtnKind.Outline)
                        Btn("تحميل كشف Excel", {
                            scope.launch {
                                val r = withContext(Dispatchers.IO) { data.export.run("xlsx", c.id, c.name, data.people.persons(c.id), false) }
                                note = if (r.ok) "تم حفظ ${r.count} سجل: ${r.name}" else "تعذّر الحفظ — تحقق من إذن التخزين"
                            }
                        }, Modifier.fillMaxWidth(), BtnKind.Outline)
                        if (admin) Btn("حذف المخيم", { sub = "danger" }, Modifier.fillMaxWidth(), BtnKind.Danger)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (creating || edit != null) {
        var name by remember(edit, creating) { mutableStateOf(edit?.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { if (!busy) { creating = false; edit = null } },
            title = { Text(if (creating) "مخيم جديد" else "تعديل اسم المخيم") },
            text = { QField("اسم المخيم", name, { name = it }) },
            confirmButton = {
                TextButton(enabled = !busy && name.isNotBlank(), onClick = {
                    busy = true
                    val target = edit
                    scope.launch {
                        withContext(Dispatchers.IO) { if (target == null) data.createCamp(auth, name) != null else data.renameCamp(auth, target.id, name) }
                        busy = false; creating = false; edit = null; reload()
                    }
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { creating = false; edit = null }) { Text("إلغاء") } },
        )
    }
}

@Composable
private fun Stat(value: String, label: String) {
    val p = LocalPalette.current
    Column {
        Text(value, color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = p.mute, fontSize = 11.5.sp)
    }
}
