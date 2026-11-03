package dev.saged.grcs.ui.more

import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.qcard
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
import dev.saged.grcs.data.Audit
import dev.saged.grcs.data.AuditEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuditPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val camps = remember { data.campList(auth) }
    var camp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var rows by remember { mutableStateOf<List<AuditEntry>?>(null) }
    var error by remember { mutableStateOf(false) }
    var cat by remember { mutableStateOf("all") }
    var q by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val cats = listOf("all" to "الكل", "add" to "إضافة", "edit" to "تعديل", "del" to "حذف", "aid" to "مساعدات", "io" to "استيراد وتصدير", "acc" to "حسابات ودخول")
    fun catOf(a: String) = when {
        a.startsWith("add") -> "add"; a.startsWith("edit") || a == "update_user" || a == "camp_info" -> "edit"
        a.startsWith("delete") || a.contains("trash") || a == "clear_camp" -> "del"; a.startsWith("aid") -> "aid"
        a == "import_excel" || a == "export" || a == "restore_backup" -> "io"; a == "login" || a == "create_user" -> "acc"; else -> "edit"
    }
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    LaunchedEffect(camp) {
        rows = null; error = false
        rows = withContext(Dispatchers.IO) { runCatching { Audit.list(auth, camp, 300).filter { auth.user.role == "admin" || it.at >= System.currentTimeMillis() - 30L * 86_400_000L } }.getOrElse { error = true; emptyList() } }
    }
    Column(Modifier.fillMaxSize()) {
        AppHeader("سجل التغييرات", "", conn = null, onBack = onBack) {
            dev.saged.grcs.ui.components.HeaderBtn(dev.saged.grcs.ui.icons.QIcons.Export, "تصدير") {
                val list = rows.orEmpty().filter { (cat == "all" || catOf(it.action) == cat) && (q.isBlank() || it.userName.contains(q.trim()) || it.targetName.contains(q.trim()) || (Audit.labels[it.action] ?: it.action).contains(q.trim())) }
                scope.launch {
                    val r = withContext(Dispatchers.IO) { data.export.save("سجل-النشاط.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", data.export.xlsx(list.map { listOf(Audit.labels[it.action] ?: it.action, it.targetName, it.userName, fmt.format(Date(it.at)), it.detail) }, "سجل النشاط", listOf("الإجراء", "الهدف", "المستخدم", "الوقت", "تفاصيل")), list.size) }
                    note = if (r.ok) "تم حفظ ${r.count} سجل: ${r.name}" else "تعذّر الحفظ"
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (camps.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { camps.forEach { (id, n) -> QChip(n, id == camp) { camp = id } } }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { cats.forEach { (k, l) -> QChip(l, cat == k) { cat = k } } }
            if (auth.user.role == "admin") dev.saged.grcs.ui.components.SearchBox(q, { q = it }, "بحث بالمستخدم أو الإجراء...")
            if (note.isNotEmpty()) Text(note, color = p.mute, fontSize = 12.sp)
            if (error) Text("تعذّر تحميل السجل — تحقق من الاتصال", color = p.danger, fontSize = 13.sp)
            else if (rows == null) Text("جارٍ التحميل…", color = p.mute, fontSize = 13.sp)
            else if (rows!!.isEmpty()) Text("لا نشاط مسجّل", color = p.mute, fontSize = 13.sp)
            val day = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val today = day.format(Date()); val yest = day.format(Date(System.currentTimeMillis() - 86_400_000L))
            var lastDay = ""
            rows?.filter { (cat == "all" || catOf(it.action) == cat) && (q.isBlank() || it.userName.contains(q.trim()) || it.targetName.contains(q.trim()) || (Audit.labels[it.action] ?: it.action).contains(q.trim())) }?.take(200)?.forEach { e ->
                val d = day.format(Date(e.at))
                if (d != lastDay) { lastDay = d; Text(if (d == today) "اليوم" else if (d == yest) "أمس" else d, color = p.pri, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }
                Column(Modifier.fillMaxWidth().qcard().padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(Audit.labels[e.action] ?: e.action, color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(fmt.format(Date(e.at)), color = p.mute, fontSize = 12.sp)
                    }
                    Text(e.userName.ifEmpty { "—" } + if (e.targetName.isNotEmpty()) " · ${e.targetName}" else "", color = p.mute, fontSize = 12.sp)
                    if (e.detail.isNotEmpty()) Text(e.detail, color = p.mute, fontSize = 11.5.sp, maxLines = 2)
                }
            }
        }
    }
}
