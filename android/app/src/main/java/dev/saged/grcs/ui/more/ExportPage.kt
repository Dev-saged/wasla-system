package dev.saged.grcs.ui.more

import dev.saged.grcs.ui.components.AppHeader
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
import dev.saged.grcs.data.ExportResult
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import kotlinx.coroutines.launch
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ExportPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val camps = remember { data.campList(auth) }
    var camp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var kind by remember { mutableStateOf("xlsx") }
    var heads by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("all") }
    var byFam by remember { mutableStateOf(false) }
    val cols = remember { androidx.compose.runtime.mutableStateListOf<String>().also { it.addAll(data.export.defaultCols) } }
    var busy by remember { mutableStateOf(false) }
    var res by remember { mutableStateOf<ExportResult?>(null) }
    Column(Modifier.fillMaxSize()) {
        AppHeader("تصدير البيانات", "", conn = null, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (camps.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QChip("كل المخيمات", camp == "*") { camp = "*"; res = null }; camps.forEach { (id, n) -> QChip(n, id == camp) { camp = id; res = null } } }
            Text("الصيغة", color = p.mute, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("xlsx" to "Excel", "csv" to "CSV", "pdf" to "PDF").forEach { (k, l) -> QChip(l, kind == k) { kind = k; res = null } } }
            Text("النوع", color = p.mute, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QChip("تصدير سريع", !custom) { custom = false; res = null }; QChip("تقرير مخصص", custom) { custom = true; res = null } }
            if (!custom) {
                Text("المحتوى", color = p.mute, fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QChip("كل الأفراد", !heads) { heads = false; res = null }; QChip("أرباب الأسر فقط", heads) { heads = true; res = null } }
            } else {
                Text("الفئة", color = p.mute, fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { dev.saged.grcs.data.PFilter.chips.filter { it.first != "head" }.forEach { (k, l) -> QChip(l, filter == k) { filter = k; res = null } } }
                Text("وحدة التقرير", color = p.mute, fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QChip("أفراد", !byFam) { byFam = false; res = null }; QChip("أسر (رب الأسرة)", byFam) { byFam = true; res = null } }
                Text("الأعمدة", color = p.mute, fontSize = 12.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { data.export.columns.forEach { c -> QChip(c.label, c.k in cols) { if (c.k in cols) cols.remove(c.k) else cols.add(c.k); res = null } } }
            }
            QAct(if (busy) "جارٍ التصدير…" else "تصدير", true) {
                if (!busy && camp.isNotEmpty()) { busy = true; scope.launch {
                    val cn = if (camp == "*") "كل المخيمات" else camps.firstOrNull { it.first == camp }?.second ?: camp
                    val ps = withContext(Dispatchers.IO) { if (camp == "*") camps.flatMap { data.people.persons(it.first) } else data.people.persons(camp) }
                    res = withContext(Dispatchers.IO) { if (custom) data.export.custom(kind, cn, ps, filter, byFam, cols.toList()) else data.export.run(kind, camp, cn, ps, heads) }
                    busy = false
                } }
            }
            res?.let { r ->
                if (!r.ok) Text("تعذّر الحفظ — تحقق من إذن التخزين", color = p.danger, fontSize = 13.sp)
                else {
                    Text("تم تصدير ${r.count} سجل: ${r.name}\nالمجلد: المستندات / تصدير ${dev.saged.grcs.config.BrandConfig.BACKUP_DIR}", color = p.ink, fontSize = 13.sp)
                    r.uri?.let { u -> QAct("مشاركة الملف", false) {
                        val mime = when (kind) { "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; "csv" -> "text/csv"; else -> "application/pdf" }
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, u).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "مشاركة"))
                    } }
                }
            }
        }
    }
}
