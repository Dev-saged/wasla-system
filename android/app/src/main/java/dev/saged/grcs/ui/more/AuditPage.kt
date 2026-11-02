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
import dev.saged.grcs.data.Audit
import dev.saged.grcs.data.AuditEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AuditPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val camps = remember { data.campList(auth) }
    var camp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var rows by remember { mutableStateOf<List<AuditEntry>?>(null) }
    var error by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("MM/dd HH:mm", Locale.US) }
    LaunchedEffect(camp) {
        rows = null; error = false
        rows = withContext(Dispatchers.IO) { runCatching { Audit.list(auth, camp) }.getOrElse { error = true; emptyList() } }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("رجوع", color = p.pri, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onBack).padding(12.dp))
            Text("سجل النشاط", color = p.ink, fontFamily = GrcsFonts.display, fontSize = 20.sp)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (camps.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { camps.forEach { (id, n) -> QChip(n, id == camp) { camp = id } } }
            if (error) Text("تعذّر تحميل السجل — تحقق من الاتصال", color = p.danger, fontSize = 13.sp)
            else if (rows == null) Text("جارٍ التحميل…", color = p.mute, fontSize = 13.sp)
            else if (rows!!.isEmpty()) Text("لا نشاط مسجّل", color = p.mute, fontSize = 13.sp)
            rows?.forEach { e ->
                Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(16.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text((Audit.labels[e.action] ?: e.action) + if (e.targetName.isNotEmpty()) " — ${e.targetName}" else "", color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(e.userName + " · " + fmt.format(Date(e.at)), color = p.mute, fontSize = 12.sp)
                }
            }
        }
    }
}
