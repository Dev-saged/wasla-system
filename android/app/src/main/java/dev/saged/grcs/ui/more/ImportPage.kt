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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import dev.saged.grcs.data.Templates

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
    val camps = remember { data.campList(auth) }
    var camp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var rows by remember { mutableStateOf<List<List<String>>?>(null) }
    var map by remember { mutableStateOf<Map<String, Int>?>(null) }
    var tplBy by remember { mutableStateOf<String?>(null) }
    var prev by remember { mutableStateOf<ImportPreview?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val prefs = remember { ctx.getSharedPreferences("grcs_import", android.content.Context.MODE_PRIVATE) }
    var lastIds by remember(camp) { mutableStateOf(prefs.getStringSet("last_$camp", emptySet()).orEmpty()) }
    val headers = rows?.firstOrNull().orEmpty()
    val flat = rows != null && Importer.isFlat(headers)

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            msg = null; prev = null; rows = null; map = null; tplBy = null; busy = true
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val name = uri.toString().lowercase()
                    ctx.contentResolver.openInputStream(uri)!!.use { s ->
                        if (name.endsWith(".csv") || ctx.contentResolver.getType(uri)?.contains("csv") == true) Xlsx.readCsv(s.bufferedReader(Charsets.UTF_8).readText()) else Xlsx.read(s)
                    }
                }.getOrNull()
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
        prev = withContext(Dispatchers.IO) { Importer.preview(r, camp, data.people.persons(camp), auth.user.id, map) }
    }

    Column(Modifier.fillMaxSize()) {
        AppHeader("استيراد Excel", "", conn = null, mark = dev.saged.grcs.ui.icons.Fa.FileImport, endBack = onBack.takeIf { auth.user.role == "admin" })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (camps.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { camps.forEach { (id, n) -> QChip(n, id == camp) { camp = id } } }
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
                                DropdownMenu(open, { open = false }) {
                                    DropdownMenuItem(text = { Text("— غير مستخدم —") }, onClick = { map = map!! - f; tplBy = null; open = false })
                                    headers.forEachIndexed { i, h ->
                                        DropdownMenuItem(text = { Text(h.ifBlank { "(بلا عنوان)" }) }, onClick = { map = map!!.filterValues { it != i } + (f to i); tplBy = null; open = false })
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (lastIds.isNotEmpty()) Btn("تراجع عن آخر استيراد (${lastIds.size})", {
                    scope.launch {
                        busy = true
                        withContext(Dispatchers.IO) {
                            val del = data.people.persons(camp).filter { it.optString("id") in lastIds }
                            runCatching { data.trash.put(auth, camp, "import_undo", "تراجع استيراد", del) }
                            del.forEach { data.people.delete(camp, it); data.afterSave(auth, camp, it, true) }
                            data.pushNow(auth, camp)
                        }
                        prefs.edit().remove("last_$camp").apply(); lastIds = emptySet(); msg = "تم التراجع عن آخر استيراد"; busy = false
                    }
                }, kind = BtnKind.Danger, small = true, enabled = !busy)
            }
            prev?.let { r ->
                Column(Modifier.fillMaxWidth().qcard().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("سيُضاف ${r.toAdd.size} فرداً (${r.headCount} أسرة/رب أسرة)", color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("مكرر بالهوية (سيُتخطى): ${r.skippedExisting} · صفوف غير صالحة: ${r.skippedInvalid}", color = p.mute, fontSize = 12.sp)
                    r.mapped.forEach { Text(it, color = p.mute, fontSize = 11.sp) }
                }
                if (r.excluded.isNotEmpty()) Btn("تقرير المستبعدين (${r.excluded.size})", {
                    scope.launch { val res = withContext(Dispatchers.IO) { data.export.save("مستبعدون-الاستيراد.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", data.export.xlsx(r.excluded, "مستبعدون", headers + "السبب"), r.excluded.size) }; msg = if (res.ok) "حُفظ التقرير: ${res.name}" else "تعذّر الحفظ" }
                }, Modifier.fillMaxWidth(), kind = BtnKind.Outline, icon = QIcons.Export)
                if (r.toAdd.isNotEmpty()) Btn("تأكيد الإضافة", {
                    scope.launch {
                        busy = true
                        withContext(Dispatchers.IO) {
                            r.toAdd.forEach { data.people.save(camp, it) }
                            r.toAdd.filter { it.optBoolean("isFamilyHead") }.forEach { data.afterSave(auth, camp, it, false) }
                            data.pushNow(auth, camp)
                            prefs.edit().putStringSet("last_$camp", r.toAdd.map { it.optString("id") }.toSet()).apply()
                            map?.let { if (flat) Templates.remember(auth, Importer.fingerprint(headers), headers, it) }
                        }
                        lastIds = r.toAdd.map { it.optString("id") }.toSet(); msg = "تمت إضافة ${r.toAdd.size} فرداً"; prev = null; rows = null; map = null; busy = false
                    }
                }, Modifier.fillMaxWidth(), enabled = !busy, icon = QIcons.Check)
            }
            msg?.let { Text(it, color = p.ok, fontSize = 13.sp) }
        }
    }
}

private fun Modifier.dashed(c: androidx.compose.ui.graphics.Color, r: Int): Modifier = drawBehind {
    drawRoundRect(c, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
}
