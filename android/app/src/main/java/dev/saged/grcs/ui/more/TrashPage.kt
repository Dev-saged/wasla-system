package dev.saged.grcs.ui.more

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
import dev.saged.grcs.data.TrashItem
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TrashPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<TrashItem>?>(null) }
    var rev by remember { mutableStateOf(0) }
    var note by remember { mutableStateOf("") }
    var confirmPurge by remember { mutableStateOf<String?>(null) }
    val fmt = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }
    val kinds = mapOf("person" to "فرد", "family" to "أسرة", "clear" to "إفراغ مخيم", "camp" to "مخيم", "account" to "حساب")
    LaunchedEffect(rev) { items = withContext(Dispatchers.IO) { runCatching { data.trash.list(auth) }.getOrNull() ?: emptyList() } }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("رجوع", color = p.pri, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onBack).padding(12.dp))
            Text("سلة المحذوفات", color = p.ink, fontFamily = GrcsFonts.display, fontSize = 20.sp)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("تُحفظ المحذوفات 30 يوماً ثم تُمسح تلقائياً", color = p.mute, fontSize = 12.sp)
            if (note.isNotEmpty()) Text(note, color = p.ink, fontSize = 12.sp)
            if (items == null) Text("جارٍ التحميل…", color = p.mute, fontSize = 13.sp)
            else if (items!!.isEmpty()) Text("السلة فارغة", color = p.mute, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
            items?.forEach { it ->
                Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(16.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(it.label, color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text((kinds[it.kind] ?: it.kind) + " · ${it.persons} فرد · " + fmt.format(Date(it.at)) + (it.byName.takeIf { n -> n.isNotEmpty() }?.let { n -> " · $n" } ?: ""), color = p.mute, fontSize = 12.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (it.kind == "person" || it.kind == "family" || it.kind == "clear") QChip("استعادة", true) {
                            scope.launch { note = runCatching { withContext(Dispatchers.IO) { data.trash.restore(auth, it) } }.fold({ n -> "استُعيد $n فرد" }, { "تعذّرت الاستعادة — تحقق من الاتصال" }); rev++ }
                        } else Text("تُستعاد من نسخة الويب", color = p.mute, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                        if (confirmPurge == it.tid) QChip("تأكيد الحذف النهائي", false) {
                            scope.launch { note = runCatching { withContext(Dispatchers.IO) { data.trash.purge(auth, listOf(it)) } }.fold({ "حُذف نهائياً" }, { "تعذّر الحذف" }); confirmPurge = null; rev++ }
                        } else QChip("حذف نهائي", false) { confirmPurge = it.tid }
                    }
                }
            }
        }
    }
}
