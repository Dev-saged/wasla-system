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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.EmptyState
import dev.saged.grcs.ui.components.HeaderBtn
import dev.saged.grcs.ui.components.IconTile
import dev.saged.grcs.ui.components.Pill
import dev.saged.grcs.ui.components.QCard
import dev.saged.grcs.ui.components.fa
import dev.saged.grcs.ui.icons.QIcons
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
    Column(Modifier.fillMaxSize()) {
      AppHeader("المخيمات", "${camps.size} مخيم · ${camps.sumOf { it.stats.families }} أسرة") {
        if (admin) HeaderBtn(QIcons.Plus, "مخيم جديد") { creating = true }
      }
      if (camps.isEmpty()) EmptyState(QIcons.Camp, "لا بيانات بعد", "افتح الرئيسية للمزامنة")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(16.dp)) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Card(c: CampSummary, admin: Boolean, onOpen: () -> Unit, onEdit: () -> Unit) {
    val p = LocalPalette.current
    QCard(Modifier, onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(QIcons.Camp, p.pri, 40.dp)
            Column(Modifier.weight(1f)) {
                Text(c.name, color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("${fa(c.stats.families)} أسرة · ${fa(c.stats.total)} فرد", color = p.pri, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
            if (admin) Box(Modifier.size(44.dp).clickable(onClick = onEdit), contentAlignment = Alignment.Center) { Icon(QIcons.Edit, "تعديل الاسم", Modifier.size(20.dp), tint = p.mute) }
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill("أطفال ${c.stats.child}", p.warn); Pill("كبار ${c.stats.elderly}", p.gray); Pill("حوامل ${c.stats.pregnant}", p.pregnant); Pill("إعاقة ${c.stats.disability}", p.purple)
        }
    }
}
