package dev.saged.grcs.ui.more

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.Audit
import dev.saged.grcs.data.Cells
import dev.saged.grcs.data.NatRef
import dev.saged.grcs.data.RcResult
import dev.saged.grcs.data.Reconcile
import dev.saged.grcs.data.RefMeta
import dev.saged.grcs.data.RefParsed
import dev.saged.grcs.ui.components.CheckRow
import dev.saged.grcs.ui.components.ConfirmSheet
import dev.saged.grcs.ui.components.SectionTitle
import dev.saged.grcs.ui.components.Bar
import dev.saged.grcs.ui.icons.Fa
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import android.widget.Toast
import dev.saged.grcs.data.Xlsx
import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.QCard
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** المطابقة الوطنية — نقل مباشر لـrenderNatRefBox / natRefAutoSave / renderReconcilePreview / runReconcileImport. */
@Composable
fun ReconcilePage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var refs by remember { mutableStateOf<List<RefMeta>>(emptyList()) }
    var rev by remember { mutableIntStateOf(0) }
    var parsed by remember { mutableStateOf<RefParsed?>(null) }
    var addMembers by remember { mutableStateOf(false) }
    var replaceId by remember { mutableStateOf<String?>(null) }
    var delRef by remember { mutableStateOf<RefMeta?>(null) }
    var progress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var progressText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<RcResult?>(null) }
    var added by remember { mutableStateOf(false) }
    var repaired by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    fun toast(t: String) = Toast.makeText(ctx, t, Toast.LENGTH_LONG).show()
    val fmt = remember { java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.US) }

    LaunchedEffect(rev) { refs = withContext(Dispatchers.IO) { NatRef.list() } }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) { replaceId = null; return@rememberLauncherForActivityResult }
        scope.launch {
            busy = true; result = null
            val fileName = runCatching { ctx.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } }.getOrNull() ?: "كشف وطني"
            val rows = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openInputStream(uri)!!.use { s -> if (fileName.lowercase().endsWith(".csv")) Xlsx.readCsv(s.bufferedReader(Charsets.UTF_8).readText()) else Xlsx.read(s) } }.getOrNull()
            }
            if (rows == null || rows.size < 2) { toast("تعذّرت قراءة الملف — استخدم xlsx أو csv"); busy = false; replaceId = null; return@launch }
            val hi = Cells.headerRow(rows)
            val pr = RefParsed(rows[hi], rows.drop(hi + 1), fileName, Cells.modeOf(rows[hi]))
            val rid = replaceId ?: refs.firstOrNull { it.name == fileName }?.id
            replaceId = null
            val saved = withContext(Dispatchers.IO) { runCatching { NatRef.save(pr, rid, auth.user.id) }.isSuccess }
            toast(if (!saved) "تعذّر حفظ الكشف بالجهاز (المطابقة ستعمل)" else if (rid != null) "حُدّث الكشف المحفوظ بالجهاز" else "حُفظ الكشف بالجهاز — لن تحتاج لرفعه مرة أخرى")
            parsed = pr; addMembers = false; busy = false; rev++
        }
    }

    fun run() {
        val pr = parsed ?: return
        scope.launch {
            busy = true; repaired = 0; progress = 0 to pr.rows.size; progressText = "جارٍ تحميل الفهرس..."
            val cols = Cells.cols(pr.headers, pr.mode)
            val res = withContext(Dispatchers.IO) {
                val byCamp = data.campList(auth).associate { (cid, _) -> cid to data.people.persons(cid).map { JSONObject(it.toString()) } }
                val r = Reconcile.run(pr.rows, cols, pr.mode, addMembers, byCamp, auth.user.id) { d, t -> progress = d to t; progressText = "جارٍ المطابقة... ${Math.round(d * 100f / t)}%" }
                progressText = "جارٍ حفظ الإضافات..."
                r.writes.entries.groupBy({ it.value }, { it.key }).forEach { (camp, list) ->
                    val fx = dev.saged.grcs.data.FamilyRepair.run(byCamp[camp].orEmpty() + list.filter { n -> byCamp[camp].orEmpty().none { it === n } })
                    repaired += fx.first
                    (list + fx.second.onEach { it.put("updatedBy", auth.user.id) }).distinct().forEach { data.people.save(camp, it) }
                    progressText = "جارٍ تحديث بيانات الأسر والمزامنة..."
                    data.pushNow(auth, camp)
                    Audit.log(auth, camp, "import_excel", "HQ", "مطابقة وطنية", JSONObject().put("file", pr.name).put("matched", r.matched).put("headsEnriched", r.headsEnriched).put("membersEnriched", r.membersEnriched).put("spousesAdded", r.spousesAdded).put("childrenAdded", r.childrenAdded).put("unmatched", r.unmatched).put("skippedNoNid", r.skippedNoNid).put("mode", "reconcile").toString())
                }
                r
            }
            added = addMembers; result = res; parsed = null; progress = null; busy = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppHeader("مطابقة كشف وطني", "", conn = null, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val pr = parsed; val res = result; val pg = progress
            when {
                pg != null -> QCard {
                    Text(progressText, color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Bar(if (pg.second == 0) 0f else pg.first.toFloat() / pg.second, p.pri, Modifier.padding(vertical = 8.dp))
                    Text("${pg.first} / ${pg.second}", color = p.mute, fontSize = 12.sp)
                }
                res != null -> {
                    QCard {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("تمت مطابقة ${res.matched} من أرباب الأسر الموجودين مسبقاً بالنظام", color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            if (res.headsEnriched > 0) Text("اكتملت بيانات ${res.headsEnriched} رب أسرة (الحقول الفارغة فقط)", color = p.ink, fontSize = 13.5.sp)
                            if (res.membersEnriched > 0) Text("اكتملت بيانات ${res.membersEnriched} فرد موجود (هوية/ميلاد)", color = p.ink, fontSize = 13.5.sp)
                            if (added) Text("أُضيف ${res.spousesAdded} زوج/ة و${res.childrenAdded} ابن/ابنة لم يكونوا مسجَّلين", color = p.ink, fontSize = 13.5.sp)
                            else Text("لم يُنشأ أي سجل جديد — مطابقة وتكميل فقط", color = p.mute, fontSize = 13.sp)
                            if (repaired > 0) Text("أُصلحت $repaired أسرة كان أفرادها منفصلين عن رب الأسرة", color = p.ink, fontSize = 13.5.sp)
                            if (res.unmatched > 0) Text("${res.unmatched} صف غير متطابق (التفاصيل والسبب بالتقرير)", color = p.warn, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            if (res.skippedNoNid > 0) Text("${res.skippedNoNid} صف بلا رقم هوية لرب الأسرة — تعذّرت مطابقته", color = p.mute, fontSize = 13.sp)
                        }
                    }
                    if (res.unmatchedList.isNotEmpty()) Btn("تحميل تقرير السجلات المستبعدة", {
                        scope.launch {
                            val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US).format(java.util.Date())
                            val r = withContext(Dispatchers.IO) { data.export.save("مطابقة-وطنية-غير-متطابقين-$stamp.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", data.export.xlsx(res.unmatchedList, "غير متطابقين", listOf("اسم رب الأسرة", "رقم الهوية", "السبب")), res.unmatchedList.size) }
                            toast(if (r.ok) "حُفظ التقرير: ${r.name}" else "تعذّر الحفظ")
                        }
                    }, Modifier.fillMaxWidth(), kind = BtnKind.Outline, icon = Fa.FileExcel)
                    Btn("مطابقة كشف آخر", { result = null }, Modifier.fillMaxWidth(), icon = Fa.ArrowsRotate)
                }
                pr != null -> {
                    val cols = remember(pr) { Cells.cols(pr.headers, pr.mode) }
                    val withNid = remember(pr) { pr.rows.count { r -> cols.headNid?.let { Cells.clean(r.getOrNull(it)) }.orEmpty().isNotEmpty() } }
                    QCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                androidx.compose.material3.Icon(Fa.CircleCheck, null, Modifier.size(14.dp), tint = p.success)
                                Text("جاهز للمطابقة (${if (pr.mode == "horizontal") "شجرة عائلة كاملة" else "أسرة مختصرة"})", color = p.success, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("يُطابَق كل صف على رقم هوية رب أسرة موجود بأي مخيم، وتُكمَّل حقوله الفارغة وحقول أفراد أسرته الموجودين (هوية/ميلاد) من نفس السطر — بلا إنشاء أي سجل جديد وبلا تعديل بيانات قائمة. غير المتطابق يُدرج بتقرير مع السبب.", color = p.ink, fontSize = 13.sp)
                            CheckRow("إضافة الأفراد الناقصين أيضاً (الزوج/ة${if (pr.mode == "horizontal" && cols.children.isNotEmpty()) " والأبناء" else ""} غير المسجَّلين بأي مكان)", addMembers) { addMembers = it }
                            Text("${pr.rows.size} صف بالملف — $withNid منهم يحمل رقم هوية لرب الأسرة قابل للمطابقة", color = p.mute, fontSize = 13.sp)
                        }
                    }
                    QCard {
                        Row(Modifier.fillMaxWidth()) { listOf("رب الأسرة", "الهوية", "الزوجة").forEach { Text(it, Modifier.weight(1f), color = p.mute, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
                        pr.rows.take(3).forEach { r ->
                            fun v(i: Int?) = i?.let { Cells.clean(r.getOrNull(it)) }.orEmpty().ifEmpty { "—" }
                            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) { listOf(v(cols.headName), v(cols.headNid), v(cols.wifeName)).forEach { Text(it, Modifier.weight(1f), color = p.ink, fontSize = 12.5.sp, maxLines = 1) } }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Btn("ابدأ المطابقة", ::run, Modifier.weight(1f), icon = Fa.Play, enabled = !busy && cols.headName != null && cols.headNid != null)
                        Btn("إلغاء", { parsed = null }, Modifier.weight(1f), kind = BtnKind.Outline)
                    }
                    if (cols.headName == null || cols.headNid == null) Text("لم يُعثر على عمود اسم رب الأسرة أو رقم هويته في الملف", color = p.danger, fontSize = 13.sp)
                }
                else -> {
                    SectionTitle("الكشوف الوطنية المحفوظة بالجهاز") { Text(if (refs.isEmpty()) "لا يوجد" else "${refs.size} كشف", color = p.mute, fontSize = 12.sp) }
                    if (refs.isEmpty()) Text("ارفع الكشف الوطني الشامل مرة واحدة فقط — يُحفظ تلقائياً بالجهاز فلا تحتاج لاختياره من جديد، ويفعّل التحقق الفوري من الهويات بكل النماذج.", color = p.mute, fontSize = 13.sp)
                    refs.forEach { r ->
                        QCard {
                            Text(r.name, color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${dev.saged.grcs.ui.components.fa(r.count)} صف · حُفظ ${fmt.format(java.util.Date(r.savedAt))}", color = p.mute, fontSize = 12.sp)
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Btn("استخدام", { scope.launch { busy = true; val pr2 = withContext(Dispatchers.IO) { runCatching { NatRef.load(r.id) }.getOrNull() }; busy = false; if (pr2 == null) toast("تعذّر قراءة الكشف المحفوظ") else { parsed = pr2; addMembers = false } } }, Modifier.weight(1f), icon = Fa.Play, small = true, enabled = !busy)
                                Btn("استبدال", { replaceId = r.id; pick.launch(arrayOf("*/*")) }, Modifier.weight(1f), kind = BtnKind.Outline, icon = Fa.ArrowsRotate, small = true, enabled = !busy)
                                Btn("حذف", { delRef = r }, kind = BtnKind.Outline, icon = Fa.Trash, small = true, enabled = !busy)
                            }
                        }
                    }
                    if (refs.isNotEmpty()) Text("أو ارفع كشفاً جديداً بالأسفل (يُضاف للقائمة؛ نفس اسم الملف يستبدل المحفوظ).", color = p.mute, fontSize = 13.sp)
                    Btn(if (busy) "جارٍ المعالجة…" else "اختيار ملف Excel أو CSV", { pick.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth(), enabled = !busy, icon = QIcons.Import)
                }
            }
        }
    }
    delRef?.let { r ->
        ConfirmSheet("حذف الكشف «${r.name}» من الجهاز؟ يمكنك رفع غيره لاحقاً.", "حذف كشف محفوظ", "حذف", icon = Fa.Trash, onCancel = { delRef = null }) {
            delRef = null
            scope.launch { withContext(Dispatchers.IO) { NatRef.remove(r.id) }; toast("حُذف الكشف المحفوظ"); rev++ }
        }
    }
}
