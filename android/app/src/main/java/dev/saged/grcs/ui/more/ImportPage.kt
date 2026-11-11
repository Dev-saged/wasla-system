package dev.saged.grcs.ui.more

import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.qcard
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.icons.QIcons
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import dev.saged.grcs.data.Templates
import org.json.JSONObject

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.ImportPreview
import dev.saged.grcs.data.Importer
import dev.saged.grcs.data.Xlsx
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** استيراد كشف Excel/CSV: معاينة أولاً ثم إضافة فقط (لا تعديل ولا حذف لسجلات موجودة). */
@Composable
fun ImportPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val camps = remember { data.ctxCamps(auth) }
    var camp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var rows by remember { mutableStateOf<List<List<String>>?>(null) }
    var map by remember { mutableStateOf<Map<String, Int>?>(null) }
    var tplBy by remember { mutableStateOf<String?>(null) }
    var prev by remember { mutableStateOf<ImportPreview?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var fileName by remember { mutableStateOf("") }
    var enr by remember { mutableStateOf<Importer.Enrich?>(null) }
    var origById by remember { mutableStateOf(mapOf<String, JSONObject>()) }
    var undoId by remember { mutableStateOf<String?>(null) }
    var askUndo by remember { mutableStateOf<String?>(null) }
    var history by remember { mutableStateOf<List<dev.saged.grcs.data.BatchRec>?>(null) }
    var picks by remember { mutableStateOf(setOf<String>()) }
    var diffOf by remember { mutableStateOf<Pair<dev.saged.grcs.data.BatchRec, dev.saged.grcs.data.BatchRec>?>(null) }
    val fmt = remember { java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.US) }
    fun toast(t: String) = android.widget.Toast.makeText(ctx, t, android.widget.Toast.LENGTH_LONG).show()
    val headers = rows?.firstOrNull().orEmpty()
    val flat = rows != null && Importer.isFlat(headers)

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            msg = null; prev = null; rows = null; map = null; tplBy = null; busy = true
            fileName = runCatching { ctx.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } }.getOrNull() ?: "ملف"
            undoId = null
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val name = fileName.lowercase()
                    ctx.contentResolver.openInputStream(uri)!!.use { s ->
                        if (name.endsWith(".csv") || ctx.contentResolver.getType(uri)?.contains("csv") == true) Xlsx.readCsv(s.bufferedReader(Charsets.UTF_8).readText()) else Xlsx.read(s)
                    }
                }.getOrNull()?.let { all -> all.drop(dev.saged.grcs.data.Cells.headerRow(all)) }
            }
            if (r == null || r.size < 2) { busy = false; msg = "تعذّرت قراءة الملف — استخدم xlsx أو csv"; return@launch }
            if (Importer.isFlat(r[0])) {
                val auto = Importer.auto(r[0])
                val tpl = withContext(Dispatchers.IO) { Templates.get(auth, Importer.fingerprint(r[0])) }
                if (tpl != null) {
                    val saved = tpl.first.mapNotNull { (f, h) -> r[0].indexOf(h).takeIf { it >= 0 }?.let { f to it } }.toMap()
                    map = auto + saved; tplBy = tpl.second.ifEmpty { "—" }
                } else map = auto
            }
            rows = r; busy = false
        }
    }
    LaunchedEffect(rows, map, camp) {
        val r = rows ?: return@LaunchedEffect
        val pair = withContext(Dispatchers.IO) {
            // المطابقة على كل المسجّلين المحمّلين (كل المخيمات للإدارة) حتى لا يتكرر رب أسرة بين مخيمين
            val scope0 = (if (auth.user.role == "admin") data.campList(auth).map { it.first } else listOf(camp)).distinct()
            val orig = scope0.associateWith { data.people.persons(it) }
            val copies = orig.mapValues { (_, l) -> l.map { JSONObject(it.toString()) } }
            val e = runCatching { Importer.enrich(r, map, copies, auth.user.id) }.getOrNull()
            origById = orig.values.flatten().associateBy { it.optString("id") }
            Importer.preview(r, camp, orig.values.flatten(), auth.user.id, map) to e
        }
        prev = pair.first; enr = pair.second
    }

    Column(Modifier.fillMaxSize()) {
        AppHeader("استيراد Excel", "", conn = null, mark = dev.saged.grcs.ui.icons.Fa.FileImport, endBack = onBack.takeIf { auth.user.role == "admin" })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (camps.size != 1) { dev.saged.grcs.ui.components.EmptyState(dev.saged.grcs.ui.icons.Fa.Tent, "ادخل مخيماً أولاً", "الاستيراد يكتب في المخيم الذي أنت بداخله — افتح المخيم من «المخيمات» ثم اختر الاستيراد"); return@Column }
            val tplSave = {
                scope.launch { val h = Importer.fieldLabels.map { it.second }; val r = withContext(Dispatchers.IO) { data.export.save("قالب-الاستيراد.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", data.export.xlsx(emptyList(), "قالب", h), 0) }; msg = if (r.ok) "حُفظ القالب في المستندات: ${r.name}" else "تعذّر الحفظ" }
                Unit
            }
            Column(Modifier.fillMaxWidth().dashed(p.tealL, 16).background(p.tealL.copy(alpha = .07f), RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الفورمة الموحّدة — الأسرع والأدق", color = p.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("حمّل الفورمة وعبّئها ثم ارفعها: يفهمها النظام بلا تخمين، ويرتّب كل فرد بمكانه، ويصنّفه تلقائياً من تاريخ ميلاده، ويخبرك بأي خطأ بأرقام الصفوف قبل أن يكتب شيئاً.", color = p.mute, fontSize = 13.sp)
                Btn("تحميل الفورمة", { tplSave() }, Modifier.fillMaxWidth(), BtnKind.Primary, QIcons.Export)
            }
            Column(Modifier.fillMaxWidth().dashed(p.line, 16).background(p.surface, RoundedCornerShape(16.dp)).clickable { pick.launch(arrayOf("*/*")) }.padding(vertical = 34.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (busy) "جارٍ القراءة…" else "اختر ملف Excel من الجهاز", color = p.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("يدعم: xlsx, xls, csv — أي نموذج كشف، والنماذج الجديدة تُحفظ تلقائياً", color = p.mute, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Btn("رفع كشف تصنيفي (حوامل، أيتام، أطفال، أرامل…)", { pick.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth(), BtnKind.Outline)
            if (flat && map != null) {
                Text(if (tplBy != null) "نموذج معروف — طُبّقت المطابقة المحفوظة ($tplBy)" else "راجع مطابقة الأعمدة؛ ستُحفظ كقالب لهذا النموذج", color = if (tplBy != null) p.ok else p.mute, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Column(Modifier.fillMaxWidth().qcard().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Importer.fieldLabels.forEach { (f, label) ->
                        var open by remember { mutableStateOf(false) }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Box {
                                Btn(map!![f]?.let { headers.getOrNull(it) }?.ifBlank { "(بلا عنوان)" } ?: "— غير مستخدم —", { open = true }, kind = BtnKind.Outline, small = true)
                                if (open) dev.saged.grcs.ui.components.PickSheet(label, listOf(-1 to "— غير مستخدم —") + headers.mapIndexed { i, h -> i to h.ifBlank { "(بلا عنوان)" } }, map!![f] ?: -1, { open = false }) { i -> map = if (i < 0) map!! - f else map!!.filterValues { it != i } + (f to i); tplBy = null }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("سجل الاستيراد والتراجع", { scope.launch { picks = emptySet(); history = withContext(Dispatchers.IO) { dev.saged.grcs.data.ImportBatch.latest(camp) } } }, Modifier.weight(1f), kind = BtnKind.Outline, icon = dev.saged.grcs.ui.icons.Fa.ClockRotateLeft, small = true)
                undoId?.let { id -> Btn("تراجع عن هذا الاستيراد", { askUndo = id }, Modifier.weight(1f), kind = BtnKind.Danger, icon = dev.saged.grcs.ui.icons.Fa.RotateLeft, small = true, enabled = !busy) }
            }
            prev?.let { r ->
                Column(Modifier.fillMaxWidth().qcard().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("سيُضاف ${r.toAdd.size} فرداً (${r.headCount} أسرة/رب أسرة)", color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    enr?.let { e -> if (e.writes.isNotEmpty()) Text("موجودون مسبقاً: ستكتمل بيانات ${e.heads} رب أسرة و${e.members} فرد (الحقول الفارغة فقط)" + if (e.added > 0) "، ويُضاف ${e.added} فرد ناقص لأسرهم" else "", color = p.ok, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    Text("مطابق بلا جديد (يُتخطى): ${(r.skippedExisting - (enr?.let { it.heads + it.members } ?: 0)).coerceAtLeast(0)} · صفوف غير صالحة: ${r.skippedInvalid}", color = p.mute, fontSize = 12.sp)
                    r.mapped.forEach { Text(it, color = p.mute, fontSize = 11.sp) }
                }
                if (r.excluded.isNotEmpty()) Btn("تقرير المستبعدين (${r.excluded.size})", {
                    scope.launch { val res = withContext(Dispatchers.IO) { data.export.save("مستبعدون-الاستيراد.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", data.export.xlsx(r.excluded, "مستبعدون", headers + "السبب"), r.excluded.size) }; msg = if (res.ok) "حُفظ التقرير: ${res.name}" else "تعذّر الحفظ" }
                }, Modifier.fillMaxWidth(), kind = BtnKind.Outline, icon = QIcons.Export)
                if (r.toAdd.isNotEmpty() || enr?.writes?.isNotEmpty() == true) Btn(if (r.toAdd.isEmpty()) "تأكيد تكميل البيانات" else "تأكيد الاستيراد", {
                    scope.launch {
                        busy = true
                        val e = enr
                        val bid = withContext(Dispatchers.IO) {
                            val at = System.currentTimeMillis()
                            val pre = ArrayList<JSONObject>(); val extra = ArrayList<JSONObject>()
                            e?.writes?.entries?.groupBy({ it.value }, { it.key })?.forEach { (c, l) ->
                                l.forEach { o -> origById[o.optString("id")]?.let { if (c == camp) pre += it }; if (c == camp) extra += o; data.afterSave(auth, c, data.people.save(c, o), false) }
                                if (c != camp) data.pushNow(auth, c)
                            }
                            r.toAdd.forEach { data.people.save(camp, it) }
                            r.toAdd.filter { it.optBoolean("isFamilyHead") }.forEach { data.afterSave(auth, camp, it, false) }
                            data.pushNow(auth, camp)
                            map?.let { if (flat) Templates.remember(auth, Importer.fingerprint(headers), headers, it) }
                            dev.saged.grcs.data.Audit.log(auth, camp, "import_excel", camp, fileName, JSONObject().put("added", r.toAdd.size + (e?.added ?: 0)).put("updated", (e?.heads ?: 0) + (e?.members ?: 0)).toString())
                            dev.saged.grcs.data.ImportBatch.commit(auth, camp, if (flat) "flat" else "horizontal", fileName, at, pre, r.toAdd + extra, r.toAdd + extra)
                        }
                        undoId = bid; msg = "تمت إضافة ${r.toAdd.size + (e?.added ?: 0)} فرداً" + (e?.let { if (it.heads + it.members > 0) " واكتملت بيانات ${it.heads} رب أسرة و${it.members} فرد موجود" else "" } ?: ""); enr = null; prev = null; rows = null; map = null; busy = false
                    }
                }, Modifier.fillMaxWidth(), enabled = !busy, icon = QIcons.Check)
            }
            msg?.let { Text(it, color = p.ok, fontSize = 13.sp) }
        }
    }
    askUndo?.let { id ->
        dev.saged.grcs.ui.components.ConfirmSheet("سيُحذف كل من أضافه هذا الاستيراد وتُعاد بيانات من حدّثهم لما كانت عليه قبله، مع حذف حسابات المستفيدين التي أنشأها. السجلات المعدّلة بعد الاستيراد تبقى كما هي.", "التراجع عن الاستيراد", "تراجع", icon = dev.saged.grcs.ui.icons.Fa.RotateLeft, onCancel = { askUndo = null }) {
            askUndo = null
            scope.launch {
                busy = true
                val res = withContext(Dispatchers.IO) { runCatching { dev.saged.grcs.data.ImportBatch.undo(auth, data, camp, id) } }
                busy = false
                res.onSuccess { u -> toast("تم التراجع: حُذف ${u.removed} وأُعيد ${u.restored}" + if (u.skipped.isNotEmpty()) " — تُرك ${u.skipped.size} سجل عُدّل بعد الاستيراد" else ""); if (undoId == id) undoId = null; history = null }
                    .onFailure { toast(it.message ?: "تعذّر التراجع") }
            }
        }
    }
    history?.let { list ->
        dev.saged.grcs.ui.components.Sheet({ history = null }, "سجل الاستيراد") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (list.isEmpty()) dev.saged.grcs.ui.components.EmptyState(dev.saged.grcs.ui.icons.Fa.ClockRotateLeft, "لا يوجد استيراد مسجّل بعد", "كل استيراد جديد يُسجَّل هنا ويمكن التراجع عنه أو مقارنته")
                else Text("اختر دفعتين للمقارنة بينهما، أو قارن أي دفعة بالتي قبلها", color = p.mute, fontSize = 12.sp)
                list.forEachIndexed { i, b ->
                    Row(Modifier.fillMaxWidth().qcard().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dev.saged.grcs.ui.components.CheckRow("", b.id in picks) { on -> picks = if (!on) picks - b.id else if (picks.size >= 2) picks else picks + b.id }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(b.fileName.ifEmpty { "ملف" }, color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${fmt.format(java.util.Date(b.at))} · ${dev.saged.grcs.data.ImportBatch.modeLabels[b.mode] ?: b.mode} · ${b.userName}", color = p.mute, fontSize = 12.sp)
                            Text("أُضيف ${b.added} · حُدِّث ${b.updated}" + if (b.undone) " · تم التراجع" else "", color = if (b.undone) p.danger else p.mute, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                list.getOrNull(i + 1)?.let { prevB -> Btn("مقارنة بالسابقة", { diffOf = prevB to b }, kind = BtnKind.Outline, icon = dev.saged.grcs.ui.icons.Fa.CodeCompare, small = true) }
                                if (dev.saged.grcs.data.ImportBatch.canUndo(b, list)) Btn("تراجع", { askUndo = b.id }, kind = BtnKind.Outline, icon = dev.saged.grcs.ui.icons.Fa.RotateLeft, small = true)
                            }
                        }
                    }
                }
                if (list.isNotEmpty()) Btn("مقارنة الدفعتين المحددتين", { val (x, y) = list.filter { it.id in picks }.sortedBy { it.at }; diffOf = x to y }, Modifier.fillMaxWidth(), icon = dev.saged.grcs.ui.icons.Fa.CodeCompare, enabled = picks.size == 2)
            }
        }
    }
    diffOf?.let { (older, newer) ->
        val d = remember(older, newer) { dev.saged.grcs.data.ImportBatch.diff(older, newer) }
        dev.saged.grcs.ui.components.Sheet({ diffOf = null }, "مقارنة كشفين") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${older.fileName} (${fmt.format(java.util.Date(older.at))}) ← ${newer.fileName} (${fmt.format(java.util.Date(newer.at))})", color = p.mute, fontSize = 12.sp)
                listOf(Triple("أُضيف", p.ok, d.added), Triple("اختفى", p.danger, d.removed), Triple("تغيّر", p.warn, d.changed)).forEach { (t, c, arr) ->
                    Column(Modifier.fillMaxWidth().qcard().padding(10.dp)) {
                        Text("$t ${arr.size}", color = c, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        arr.take(80).forEach { r -> Text(r.n + if (r.k.all { it.isDigit() } && r.k.isNotEmpty()) " — ${r.k}" else "", color = p.ink, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
                        if (arr.size > 80) Text("و${arr.size - 80} آخرون…", color = p.mute, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun Modifier.dashed(c: androidx.compose.ui.graphics.Color, r: Int): Modifier = drawBehind {
    drawRoundRect(c, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
}
