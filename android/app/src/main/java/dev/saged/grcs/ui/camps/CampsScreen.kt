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
import dev.saged.grcs.ui.icons.Fa
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
    var del by remember { mutableStateOf<CampSummary?>(null) }
    var critFam by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var sub by remember { mutableStateOf("") }
    LaunchedEffect(dev.saged.grcs.ui.shell.ShellState.campsRoute) { dev.saged.grcs.ui.shell.ShellState.campsRoute?.let { sub = it; dev.saged.grcs.ui.shell.ShellState.campsRoute = null } }
    var q by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<Triple<String, String, org.json.JSONObject>>>(emptyList()) }
    var quality by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var infos by remember { mutableStateOf<Map<String, CampInfo>>(emptyMap()) }
    var note by remember { mutableStateOf("") }
    fun reload() { camps = data.cached(auth).sortedByDescending { it.stats.total } }
    val fmt = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US) }

    LaunchedEffect(camps.size) {
        quality = withContext(Dispatchers.IO) { camps.associate { c -> c.id to (runCatching { Quality.report(data.people.persons(c.id)).first }.getOrDefault(0)) } }
        critFam = withContext(Dispatchers.IO) { camps.associate { c -> c.id to (runCatching { dev.saged.grcs.data.Vuln.families(data.people.persons(c.id)).count { it.result.tier == "critical" } }.getOrDefault(0)) } }
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
        AppHeader("المخيمات", "", conn = null, mark = Fa.Tents, search = true)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (admin) {
                Btn("مطابقة كشف وطني شامل (مطابقة وتكميل بيانات)", { sub = "reconcile" }, Modifier.fillMaxWidth(), BtnKind.Outline, Fa.UsersViewfinder)
                Btn("كشوفات التصنيفات لكل المخيمات (أيتام، حوامل...)", { sub = "cats" }, Modifier.fillMaxWidth(), BtnKind.Outline, Fa.LayerGroup)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn("المساعدات — كل المخيمات", { sub = "aid" }, Modifier.weight(1f), BtnKind.Outline, Fa.BoxOpen)
                    Btn("مركز الجودة", { sub = "quality" }, Modifier.weight(1f), BtnKind.Outline, Fa.ShieldHalved)
                }
                Btn("تصدير بيانات المخيمات", { sub = "export" }, Modifier.fillMaxWidth(), BtnKind.Navy, Fa.FileExcel)
                SearchBox(q, { q = it }, "بحث موحّد: اسم شخص أو مخيم...")
                hits.forEach { (cid, cn, ps) ->
                    QCard(Modifier, { onFamily(cid, ps.optString("familyId").ifEmpty { "i_" + ps.optString("id") }) }) {
                        Text(ps.optString("fullName"), color = p.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(cn + " · " + ps.optString("nationalId").ifEmpty { "بلا هوية" }, color = p.mute, fontSize = 12.sp)
                    }
                }
            }
            if (note.isNotEmpty()) Text(note, color = p.mute, fontSize = 12.sp)
            if (camps.isEmpty()) EmptyState(Fa.Tent, "لا توجد مخيمات بعد", "أنشئ حساب مندوب مخيم من شاشة الحسابات")
            camps.filter { q.isBlank() || it.name.contains(q.trim()) }.forEach { c ->
                val info = infos[c.id]
                val crit = critFam[c.id] ?: 0
                QCard(Modifier, { sub = "detail:" + c.id }) {
                    Text(c.name, color = p.ink, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                    Text(info?.supervisor?.ifEmpty { null } ?: "—", color = p.mute, fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Stat(fa(c.stats.total), "فرد"); Stat(fa(c.stats.families), "أسرة")
                        Stat(info?.lastUpdated?.takeIf { it > 0 }?.let { fmt.format(Date(it)) } ?: "—", "آخر تحديث")
                        if (crit > 0) Stat(fa(crit), "أسرة حرجة", p.acc)
                    }
                    Row(Modifier.padding(top = 2.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("اكتمال البيانات", color = p.mute, fontSize = 12.sp)
                        val pct = quality[c.id] ?: 0
                        Ring(pct / 100f, if (pct >= 85) p.success else if (pct >= 60) p.warn else p.danger, 46.dp, "$pct%")
                    }
                    Btn("عرض التفاصيل", { sub = "detail:" + c.id }, Modifier.fillMaxWidth(), BtnKind.Outline, Fa.ArrowLeft, small = true)
                    Btn("تحميل كشف Excel", {
                        scope.launch {
                            val r = withContext(Dispatchers.IO) { data.export.run("xlsx", c.id, c.name, data.people.persons(c.id), false) }
                            note = if (r.ok) "تم حفظ ${r.count} سجل: ${r.name}" else "تعذّر الحفظ — تحقق من إذن التخزين"
                        }
                    }, Modifier.fillMaxWidth().padding(top = 8.dp), BtnKind.Outline, Fa.Download, small = true)
                    if (admin) Btn("حذف المخيم", { del = c }, Modifier.fillMaxWidth().padding(top = 8.dp), BtnKind.Danger, Fa.Trash, small = true)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    del?.let { c ->
        dev.saged.grcs.ui.components.ConfirmSheet("سيتم حذف مخيم \"${c.name}\" مع كل بياناته (الأفراد، الأسر، الرسائل، أي حسابات متبقية تابعة له) من هذا الجهاز ومن قاعدة البيانات المركزية. تبقى نسخة من المخيم وأفراده وحساباته في سلة المحذوفات 30 يوماً (الرسائل لا تُستعاد).", "حذف المخيم نهائياً", "حذف نهائي", icon = Fa.Trash, onCancel = { del = null }) {
            del = null
            scope.launch { val e = withContext(Dispatchers.IO) { data.wipeCamp(auth, c.id, c.name, true) }; note = e ?: "تم حذف المخيم"; reload() }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, color: androidx.compose.ui.graphics.Color? = null) {
    val p = LocalPalette.current
    Column {
        Text(value, color = color ?: p.head, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = p.mute, fontSize = 12.5.sp)
    }
}
