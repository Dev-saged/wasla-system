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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.Audit
import dev.saged.grcs.data.AuditEntry
import dev.saged.grcs.data.CampDiag
import dev.saged.grcs.data.CampInfo
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.Usage
import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.Bar
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.Conn
import dev.saged.grcs.ui.components.ConnState
import dev.saged.grcs.ui.components.EmptyState
import dev.saged.grcs.ui.components.QCard
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.components.QField
import dev.saged.grcs.ui.components.SectionTitle
import dev.saged.grcs.ui.components.fa
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.US)
private fun d(ms: Long) = if (ms <= 0) "—" else dateFmt.format(Date(ms))


/** معلومات المخيم: الاسم، المنطقة، المسؤول ونائبه ومدخل البيانات، وملاحظات. */
@Composable
fun CampInfoPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val camps = remember { data.ctxCamps(auth) }
    var camp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var info by remember { mutableStateOf<CampInfo?>(null) }
    var name by remember { mutableStateOf("") }; var region by remember { mutableStateOf("") }
    var sup by remember { mutableStateOf("") }; var supP by remember { mutableStateOf("") }
    var dep by remember { mutableStateOf("") }; var depP by remember { mutableStateOf("") }
    var de by remember { mutableStateOf("") }; var deP by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val fams = remember(camp) { data.cached(auth).firstOrNull { it.id == camp }?.stats?.families ?: 0 }
    LaunchedEffect(camp) {
        if (camp.isEmpty()) return@LaunchedEffect
        info = null
        val i = withContext(Dispatchers.IO) { data.campInfo(auth, camp) }
        info = i; name = i.name; region = i.region; sup = i.supervisor; supP = i.supervisorPhone; dep = i.deputy; depP = i.deputyPhone; de = i.dataEntry; deP = i.dataEntryPhone; notes = i.notes
    }
    Column(Modifier.fillMaxSize()) {
        AppHeader("معلومات المخيم", "", conn = null, mark = dev.saged.grcs.ui.icons.Fa.Tent, endBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (camps.size != 1) { EmptyState(dev.saged.grcs.ui.icons.Fa.Tent, "ادخل مخيماً أولاً", "معلومات المخيم تُعرض للمخيم الذي أنت بداخله — افتحه من «المخيمات»"); return@Column }
            QCard {
                QField("اسم المخيم الرسمي", name, { name = it }); Spacer(Modifier.height(8.dp))
                QField("رقم المخيم", camp, { }, enabled = false); Spacer(Modifier.height(8.dp))
                QField("المنطقة/الحي", region, { region = it })
            }
            QCard {
                Text("مسؤول المخيم", color = p.navy, fontSize = 16.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
                QField("الاسم", sup, { sup = it }); Spacer(Modifier.height(8.dp)); QField("الهاتف", supP, { supP = it }, type = KeyboardType.Phone)
            }
            QCard {
                Text("نائب المسؤول", color = p.navy, fontSize = 16.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
                QField("الاسم", dep, { dep = it }); Spacer(Modifier.height(8.dp)); QField("الهاتف", depP, { depP = it }, type = KeyboardType.Phone)
            }
            QCard {
                Text("مدخل البيانات", color = p.navy, fontSize = 16.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
                QField("الاسم", de, { de = it }); Spacer(Modifier.height(8.dp)); QField("الهاتف", deP, { deP = it }, type = KeyboardType.Phone)
            }
            QCard {
                listOf("تاريخ الإنشاء" to d(info?.createdAt ?: 0), "آخر تحديث" to d(info?.lastUpdated ?: 0), "عدد الأسر المسجلة" to fa(fams)).forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(k, color = p.mute, fontSize = 13.sp); Text(v, color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f, false))
                    }
                }
            }
            QCard {
                Text("ملاحظات عامة", color = p.navy, fontSize = 16.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
                QField("", notes, { notes = it }, multiline = true)
            }
            msg?.let { Text(it, color = if (it.startsWith("تم")) p.ok else p.danger, fontSize = 13.sp) }
            Btn(if (busy) "جارٍ الحفظ…" else "حفظ المعلومات", {
                if (camp.isNotEmpty() && !busy) scope.launch {
                    busy = true
                    val ok = withContext(Dispatchers.IO) { data.saveCampInfo(auth, CampInfo(camp, name.trim(), region.trim(), sup.trim(), supP.trim(), dep.trim(), depP.trim(), de.trim(), deP.trim(), notes.trim(), info?.createdAt ?: 0, 0)) }
                    msg = if (ok) "تم الحفظ — طُبِّق عند الجميع" else "تعذّر الحفظ — تحقق من الاتصال والصلاحية"
                    busy = false
                }
            }, Modifier.fillMaxWidth(), enabled = !busy && camp.isNotEmpty(), icon = QIcons.Check)
        }
    }
}

/** سجل نقل الأسر بين المخيمات (من سجل النشاط). */
@Composable
fun TransferLogPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val camps = remember { data.ctxCamps(auth) }
    var rows by remember { mutableStateOf<List<AuditEntry>?>(null) }
    LaunchedEffect(Unit) {
        rows = withContext(Dispatchers.IO) { camps.flatMap { c -> runCatching { Audit.list(auth, c.first, 300).filter { it.action == "family_transfer" || it.action == "transfer_family" } }.getOrDefault(emptyList()) }.distinctBy { it.targetName + it.at }.sortedByDescending { it.at } }
    }
    Column(Modifier.fillMaxSize()) {
        AppHeader("سجل نقل الأسر", "", conn = null, onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val r = rows
            if (r == null) Text("جارٍ التحميل…", color = p.mute, fontSize = 13.sp)
            else if (r.isEmpty()) EmptyState(QIcons.Transfer, "لا عمليات نقل", "ستظهر هنا الأسر المنقولة من هذا المخيم أو إليه")
            else r.forEach { e ->
                QCard {
                    Text(e.targetName.ifEmpty { "أسرة" }, color = p.ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                    if (e.detail.isNotEmpty()) Text(e.detail, color = p.mute, fontSize = 12.sp)
                    Text("${e.userName} · ${d(e.at)}", color = p.mute, fontSize = 11.5.sp)
                }
            }
        }
    }
}

/** تشخيص المزامنة: حالة كل مخيم محلياً، العدّادات اليومية مقابل الحصة المجانية، وإعادة السحب الكامل. */
@Composable
fun SyncDiagPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<CampDiag>>(emptyList()) }
    var rev by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(rev) { rows = withContext(Dispatchers.IO) { data.diagnostics(auth) } }
    val reads = Usage.reads(); val writes = Usage.writes()
    Column(Modifier.fillMaxSize()) {
        AppHeader("تشخيص المزامنة", "", conn = null, onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            QCard {
                listOf("الاتصال" to when (ConnState.state) { Conn.On -> "متصل"; Conn.Sync -> "جارٍ المزامنة"; Conn.Off -> "دون اتصال" },
                    "آخر مزامنة" to d(ConnState.lastSync), "انتهاء رمز الدخول" to d(auth.expiresAt), "الحساب" to auth.user.username).forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(k, color = p.mute, fontSize = 12.5.sp); Text(v, color = p.ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            SectionTitle("حصة قاعدة البيانات اليومية (الحد المجاني للمشروع كله)")
            QCard {
                Text("قراءات هذا الجهاز: ${fa(reads.toInt())} من 50,000", color = p.ink, fontSize = 12.5.sp)
                Bar(reads / 50_000f, if (reads > 35_000) p.warn else p.pri, Modifier.padding(top = 6.dp, bottom = 10.dp))
                Text("كتابات هذا الجهاز: ${fa(writes.toInt())} من 20,000", color = p.ink, fontSize = 12.5.sp)
                Bar(writes / 20_000f, if (writes > 14_000) p.warn else p.pri, Modifier.padding(top = 6.dp))
                Text("العدّاد لهذا الجهاز فقط؛ الحصة موزعة على كل أجهزة المشروع.", color = p.mute, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
            }
            SectionTitle("المخيمات")
            rows.forEach { r ->
                QCard {
                    Text(r.name, color = p.ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                    listOf("مستندات أسر محفوظة" to fa(r.local), "تعديلات بانتظار الرفع" to fa(r.pending), "حذوفات بانتظار الرفع" to fa(r.tombs),
                        "مؤشر السحب" to r.since.ifEmpty { "—" }, "علامة الإفراغ" to d(r.clearedAt)).forEach { (k, v) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(k, color = p.mute, fontSize = 12.sp); Text(v, color = if (k.startsWith("تعديلات") && r.pending > 0) p.warn else p.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f, false))
                        }
                    }
                }
            }
            msg?.let { Text(it, color = p.mute, fontSize = 12.5.sp) }
            Btn(if (busy) "جارٍ السحب…" else "إعادة سحب كامل من الخادم", {
                if (!busy) scope.launch {
                    busy = true; msg = null
                    val ok = withContext(Dispatchers.IO) { runCatching { data.resetCursors(auth); data.sync(auth).first.offline.not() }.getOrDefault(false) }
                    msg = if (ok) "اكتمل السحب الكامل" else "تعذّر السحب — تحقق من الاتصال"
                    busy = false; rev++
                }
            }, Modifier.fillMaxWidth(), BtnKind.Outline, QIcons.Sync, enabled = !busy)
        }
    }
}

/** إفراغ بيانات المخيم أو حذفه نهائياً: أدمن فقط، نسخة إلزامية، سلة، وتأكيد بكتابة اسم المخيم. */
@Composable
fun CampDangerPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    var rev by remember { mutableStateOf(0) }
    val camps = remember(rev) { data.cached(auth) }
    var target by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var typed by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        AppHeader("إفراغ وحذف المخيمات", "", conn = null, onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("قبل أي عملية تُحفظ نسخة احتياطية في المستندات ونسخة في سلة المحذوفات (30 يوماً). تُطبَّق العملية عند كل الأجهزة.", color = p.mute, fontSize = 12.sp)
            msg?.let { Text(it, color = if (it.startsWith("تم")) p.ok else p.danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            if (camps.isEmpty()) EmptyState(QIcons.Camp, "لا مخيمات")
            camps.forEach { c ->
                QCard {
                    Text(c.name, color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("${fa(c.stats.families)} أسرة · ${fa(c.stats.total)} فرد", color = p.mute, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Btn("إفراغ البيانات", { target = c.id to false; typed = "" }, Modifier.weight(1f), BtnKind.Outline, QIcons.Trash, small = true)
                        if (auth.user.role == "admin") Btn("حذف المخيم", { target = c.id to true; typed = "" }, Modifier.weight(1f), BtnKind.Danger, QIcons.Alert, small = true)
                    }
                }
            }
        }
    }
    target?.let { (id, del) ->
        val c = camps.firstOrNull { it.id == id }
        val nm = c?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { if (!busy) target = null },
            title = { Text(if (del) "حذف المخيم نهائياً" else "إفراغ بيانات المخيم") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (del) "سيُحذف المخيم «$nm» مع كل أفراده وأسره وحساباته ورسائله من الخادم ومن كل الأجهزة." else "ستُحذف كل الأسر وحسابات المستفيدين في «$nm» من الخادم ومن كل الأجهزة، وتبقى حسابات الطاقم.", fontSize = 13.sp)
                    QField("اكتب اسم المخيم للتأكيد", typed, { typed = it })
                }
            },
            confirmButton = {
                TextButton(enabled = !busy && typed.trim() == nm.trim(), onClick = {
                    busy = true
                    scope.launch {
                        val err = withContext(Dispatchers.IO) { data.wipeCamp(auth, id, nm, del) }
                        msg = err ?: if (del) "تم حذف المخيم «$nm»" else "تم إفراغ «$nm»"
                        busy = false; target = null; rev++
                    }
                }) { Text(if (busy) "جارٍ التنفيذ…" else if (del) "حذف نهائي" else "إفراغ نهائي", color = p.danger) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { target = null }) { Text("إلغاء") } },
        )
    }
}
