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
import dev.saged.grcs.data.Staff
import dev.saged.grcs.ui.components.QField
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import org.json.JSONObject
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StaffPage(auth: AuthState, onBack: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<JSONObject>?>(null) }
    var rev by remember { mutableStateOf(0) }
    var pwFor by remember { mutableStateOf<String?>(null) }
    var pw by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    LaunchedEffect(rev) { rows = withContext(Dispatchers.IO) { Staff.list(auth) } }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("رجوع", color = p.pri, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onBack).padding(12.dp))
            Text("حسابات الطاقم", color = p.ink, fontFamily = GrcsFonts.display, fontSize = 20.sp)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (note.isNotEmpty()) Text(note, color = p.mute, fontSize = 12.sp)
            if (rows == null) Text("جارٍ التحميل…", color = p.mute, fontSize = 13.sp)
            rows?.forEach { u ->
                val id = u.getString("id"); val active = u.optString("isActive") != "false"
                Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(16.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(u.optString("fullName"), color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(Staff.roles.firstOrNull { it.first == u.optString("role") }?.second.orEmpty() + " · " + u.optString("username") +
                        (u.optString("campName").takeIf { it.isNotEmpty() }?.let { " · $it" } ?: "") + if (active) "" else " · معطّل", color = if (active) p.mute else p.danger, fontSize = 12.sp)
                    if (id != auth.user.id) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QChip(if (active) "تعطيل" else "تفعيل", false) { scope.launch { note = if (withContext(Dispatchers.IO) { Staff.setActive(auth, u, !active) }) "تم" else "تعذّر التنفيذ"; rev++ } }
                        QChip("كلمة مرور جديدة", pwFor == id) { pwFor = if (pwFor == id) null else id; pw = "" }
                    }
                    if (pwFor == id) {
                        QField("كلمة المرور (6 أحرف على الأقل)", pw, { pw = it })
                        QChip("حفظ", true) { scope.launch { note = if (withContext(Dispatchers.IO) { Staff.resetPassword(auth, u, pw) }) "تم تحديث كلمة المرور" else "تعذّر التحديث"; pwFor = null; pw = "" } }
                    }
                }
            }
        }
    }
}
