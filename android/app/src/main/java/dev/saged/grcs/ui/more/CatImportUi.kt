package dev.saged.grcs.ui.more

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.Audit
import dev.saged.grcs.data.Cat
import dev.saged.grcs.data.CatApplied
import dev.saged.grcs.data.CatItem
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.ImportBatch
import dev.saged.grcs.data.Xlsx
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.PickSheet
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.components.QField
import dev.saged.grcs.ui.components.Sheet
import dev.saged.grcs.ui.components.qcard
import dev.saged.grcs.ui.icons.Fa
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

private class CatTable(val headers: List<String>, val rows: List<List<String>>, val fileName: String)

/** رفع كشف تصنيفي (openCatPicker + openCatAnalysis + showCatResult في الويب): اختيار النوع ← تحليل الأعمدة ← معاينة النتيجة ← تنفيذ وتقرير وتراجع. */
@Composable
fun CatImportFlow(auth: AuthState, data: DataRepo, camp: String, onClose: () -> Unit, onUndo: (String) -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var catKey by remember { mutableStateOf<String?>(null) }
    var custom by remember { mutableStateOf("") }
    var table by remember { mutableStateOf<CatTable?>(null) }
    var map by remember { mutableStateOf<List<String>>(emptyList()) }
    var plan by remember { mutableStateOf<Pair<List<CatItem>, Cat.Index>?>(null) }
    var pickCol by remember { mutableStateOf<Int?>(null) }
    var showCols by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf<Triple<CatApplied, String?, List<CatItem>>?>(null) }
    val crossMemo = remember { ConcurrentHashMap<String, String>() }
    fun toast(t: String) = android.widget.Toast.makeText(ctx, t, android.widget.Toast.LENGTH_LONG).show()
    val label = catKey?.let { k -> if (k == "custom") custom.trim() else Cat.by.getValue(k).label }.orEmpty()

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true; err = null
            val name = runCatching { ctx.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } }.getOrNull() ?: "ملف"
            val all = withContext(Dispatchers.IO) { Xlsx.load(ctx, uri, name) }
            if (all == null || all.size < 2) { busy = false; err = "تعذّرت قراءة الملف أو لا توجد بيانات صالحة — استخدم xlsx أو csv"; return@launch }
            val h = all[0].map { it.trim() }
            val rows = all.drop(1).filter { r -> r.any { it.isNotBlank() } }
            map = withContext(Dispatchers.IO) { Cat.analyze(h, rows, catKey!!) }
            table = CatTable(h, rows, name)
            busy = false
        }
    }

    LaunchedEffect(table, map, catKey, custom) {
        val t = table ?: return@LaunchedEffect
        val k = catKey ?: return@LaunchedEffect
        plan = null
        if (!map.contains("sName") && !map.contains("sNid")) return@LaunchedEffect
        plan = withContext(Dispatchers.IO) {
            val persons = data.people.persons(camp).map { JSONObject(it.toString()) }
            val cross: ((String) -> String?)? = if (auth.offline) null else { nid -> crossMemo.getOrPut(nid) { data.checkNid(auth, nid, camp)?.substringAfter(": ").orEmpty() }.ifEmpty { null } }
            Cat.plan(t.rows, t.headers, map, k, custom.trim(), camp, persons, cross)
        }
    }

    val guess = remember(table) { table?.let { Cat.detect(it.headers, it.rows) } }

    done?.let { (res, batchId, items) ->
        Sheet(onClose, "اكتمل كشف $label") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Stats(res.counts)
                Text("لم يُنشأ أي رب أسرة من هذا الكشف" + (res.counts["create"]?.let { " عدا $it غير مسجَّلة سابقاً (كشف أرامل)" } ?: "") + ". التقرير يحوي نتيجة كل صف وسببها.", color = p.mute, fontSize = 12.5.sp)
                Btn("تحميل تقرير المطابقة", {
                    scope.launch {
                        val r = withContext(Dispatchers.IO) { data.export.save("مطابقة-$label.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", data.export.xlsx(Cat.reportRows(items), "نتيجة المطابقة", Cat.reportHeader), items.size) }
                        toast(if (r.ok) "حُفظ التقرير: ${r.name}" else "تعذّر الحفظ")
                    }
                }, Modifier.fillMaxWidth(), icon = QIcons.Export)
                batchId?.let { id -> Btn("تراجع عن هذا الكشف", { onUndo(id); onClose() }, Modifier.fillMaxWidth(), BtnKind.Outline, icon = Fa.RotateLeft) }
                Btn("تم", onClose, Modifier.fillMaxWidth(), BtnKind.Outline)
            }
        }
        return
    }

    val t = table
    if (t == null) {
        Sheet(onClose, "رفع كشف تصنيفي") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("اختر نوع الكشف — النظام يحلل الأعمدة تلقائياً ويطابق كل صف مع صاحبه وأسرته، ويعرض النتيجة قبل أي تعديل.", color = p.mute, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Cat.types.forEach { c -> QChip(c.label, catKey == c.key) { catKey = c.key } }
                }
                if (catKey == "custom") QField("اسم التصنيف", custom, { custom = it.filter { ch -> ch !in "<>\"'`\\{}[]" }.take(40) }, placeholder = "مثال: أسر أسرى")
                err?.let { Text(it, color = p.danger, fontSize = 13.sp) }
                Btn(if (busy) "جارٍ القراءة…" else "اختيار ملف الكشف", { pick.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth(), icon = Fa.FileImport, enabled = !busy && catKey != null && (catKey != "custom" || custom.isNotBlank()))
            }
        }
        return
    }

    Sheet(onClose, "كشف $label") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${t.fileName} — ${t.rows.size} صف", color = p.mute, fontSize = 12.5.sp)
            if (guess != null && guess != catKey && catKey != "custom") {
                Row(Modifier.fillMaxWidth().qcard().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الملف يبدو كشف «${Cat.by.getValue(guess).label}»", color = p.ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Btn("اعتماده", { catKey = guess; scope.launch { map = withContext(Dispatchers.IO) { Cat.analyze(t.headers, t.rows, guess) } } }, kind = BtnKind.Outline, small = true)
                }
            }
            Btn(if (showCols) "إخفاء الأعمدة" else "الأعمدة كما فهمها النظام", { showCols = !showCols }, Modifier.fillMaxWidth(), BtnKind.Outline, small = true, icon = Fa.TableColumns)
            if (showCols) Column(Modifier.fillMaxWidth().qcard().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                t.headers.forEachIndexed { i, h ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(h.ifBlank { "عمود ${i + 1}" }, color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Btn(Cat.roles[map.getOrNull(i)] ?: "حقل إضافي", { pickCol = i }, kind = BtnKind.Outline, small = true)
                    }
                }
            }
            val pl = plan
            if (!map.contains("sName") && !map.contains("sNid")) Text("حدد عمود اسم الشخص أو رقم هويته من قائمة الأعمدة", color = p.danger, fontSize = 13.sp)
            else if (pl == null) Text("جارٍ التحليل والمطابقة…", color = p.mute, fontSize = 13.sp)
            else {
                val items = pl.first
                val by = items.groupBy { it.status }
                Stats(items.groupingBy { it.status }.eachCount())
                listOf("add", "create", "update", "unmatched", "amb", "other_camp", "dup", "same").forEach { k ->
                    val l = by[k].orEmpty(); if (l.isEmpty()) return@forEach
                    Column(Modifier.fillMaxWidth().qcard().padding(10.dp)) {
                        Text("${Cat.statusLabels[k]} ${l.size}", color = if (k in listOf("add", "create", "update")) p.ok else if (k == "same") p.mute else p.warn, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        l.take(60).forEach { x ->
                            Text(x.name.ifEmpty { x.nid } + (if (x.nid.isNotEmpty() && x.name.isNotEmpty()) " — ${x.nid}" else ""), color = p.ink, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                            val why = if (k == "other_camp") "مسجّل بمخيم ${x.other}" else x.reason.ifEmpty { x.how }
                            val head = x.head?.takeIf { h -> h !== x.person }?.let { h -> " · رب الأسرة: " + h.optString("fullName") }.orEmpty()
                            if (why.isNotEmpty() || head.isNotEmpty()) Text(why + head, color = p.mute, fontSize = 11.5.sp)
                        }
                        if (l.size > 60) Text("و${l.size - 60} آخرون…", color = p.mute, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                Btn(if (busy) "جارٍ التنفيذ…" else "تنفيذ المطابقة", {
                    scope.launch {
                        busy = true
                        val k = catKey!!
                        val res = withContext(Dispatchers.IO) {
                            val originals = data.people.persons(camp).associateBy { it.optString("id") }
                            val a = Cat.apply(items, pl.second, k, custom.trim(), camp, auth.user.id)
                            val at = System.currentTimeMillis()
                            val pre = a.writes.mapNotNull { w -> originals[w.optString("id")]?.let { JSONObject(it.toString()) } }
                            a.writes.forEach { w -> val s = data.people.save(camp, w); if (s.optBoolean("isFamilyHead") || w in a.created) data.afterSave(auth, camp, s, false) }
                            data.pushNow(auth, camp)
                            Audit.log(auth, camp, "category_import", k, label, JSONObject(a.counts as Map<*, *>).put("file", t.fileName).toString())
                            a to ImportBatch.commit(auth, camp, "category", t.fileName, at, pre, a.writes, items.map { JSONObject().put("fullName", it.name).put("nationalId", it.nid) })
                        }
                        busy = false
                        done = Triple(res.first, res.second, items)
                    }
                }, Modifier.fillMaxWidth(), icon = QIcons.Check, enabled = !busy && ((by["update"]?.size ?: 0) + (by["add"]?.size ?: 0) + (by["create"]?.size ?: 0)) > 0)
            }
        }
    }
    pickCol?.let { i ->
        PickSheet("دور العمود: " + t.headers.getOrElse(i) { "" }.ifBlank { "عمود ${i + 1}" }, Cat.roles.map { it.key to it.value }, map.getOrNull(i), { pickCol = null }) { r ->
            map = map.toMutableList().also { m -> m[i] = r }; pickCol = null
        }
    }
}

@Composable
private fun Stats(c: Map<String, Int>) {
    val p = LocalPalette.current
    fun n(k: String) = c[k] ?: 0
    val cells = listOf(
        Triple("حُدِّث", n("update"), p.warn), Triple("أُضيف / أُنشئ", n("add") + n("create"), p.ok),
        Triple("مطابق مسبقاً", n("same"), p.mute), Triple("لم يُعدَّل", n("unmatched") + n("amb") + n("other_camp") + n("dup"), p.danger),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.forEach { (t, v, col) ->
            Column(Modifier.weight(1f).qcard().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(v.toString(), color = col, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(t, color = p.mute, fontSize = 11.sp)
            }
        }
    }
}
