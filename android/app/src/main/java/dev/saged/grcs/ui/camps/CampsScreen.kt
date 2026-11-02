package dev.saged.grcs.ui.camps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.components.QField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.CampSummary
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette

/** المخيمات: ملخص كل مخيم من آخر نسخة محلية. */
@Composable
fun CampsScreen(auth: AuthState, data: DataRepo, onOpen: (String) -> Unit = {}) {
    val p = LocalPalette.current
    var camps by remember { mutableStateOf(data.cached(auth).sortedByDescending { it.stats.total }) }
    val admin = auth.user.role == "admin"
    var edit by remember { mutableStateOf<CampSummary?>(null) }
    var creating by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun reload() { camps = data.cached(auth).sortedByDescending { it.stats.total } }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("المخيمات", color = p.ink, fontFamily = GrcsFonts.display, fontSize = 24.sp)
            if (admin) QChip("مخيم جديد", false) { creating = true }
        }
        if (camps.isEmpty()) Text("لا بيانات بعد — افتح الرئيسية للمزامنة", color = p.mute, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            items(camps, key = { it.id }) { c -> Card(c, admin, { onOpen(c.id) }) { edit = c } }
        }
    }
    if (creating || edit != null) {
        var name by remember(edit, creating) { mutableStateOf(edit?.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { if (!busy) { creating = false; edit = null } },
            title = { Text(if (creating) "مخيم جديد" else "تعديل اسم المخيم") },
            text = { QField("اسم المخيم", name, { name = it }) },
            confirmButton = {
                TextButton(enabled = !busy && name.isNotBlank(), onClick = {
                    busy = true
                    val target = edit
                    scope.launch {
                        withContext(Dispatchers.IO) { if (target == null) data.createCamp(auth, name) != null else data.renameCamp(auth, target.id, name) }
                        busy = false; creating = false; edit = null; reload()
                    }
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { creating = false; edit = null }) { Text("إلغاء") } },
        )
    }
}

@Composable
private fun Card(c: CampSummary, admin: Boolean, onOpen: () -> Unit, onEdit: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().clickable(onClick = onOpen).background(p.surface, RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(c.name, color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (admin) QChip("تعديل الاسم", false, onEdit)
        }
        Text("${c.stats.families} أسرة · ${c.stats.total} فرد", color = p.pri, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("أطفال ${c.stats.child}", color = p.mute, fontSize = 12.sp)
            Text("كبار ${c.stats.elderly}", color = p.mute, fontSize = 12.sp)
            Text("حوامل ${c.stats.pregnant}", color = p.mute, fontSize = 12.sp)
            Text("إعاقة ${c.stats.disability}", color = p.mute, fontSize = 12.sp)
        }
    }
}
