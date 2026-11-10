package dev.saged.grcs.ui.people

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.TransferPlan
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.components.QField
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TransferDialog(auth: AuthState, data: DataRepo, src: String, camps: List<Pair<String, String>>, famKey: String, onClose: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    var dest by remember { mutableStateOf<String?>(null) }
    var reason by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf<TransferPlan?>(null) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(dest) { plan = withContext(Dispatchers.IO) { data.transfer.plan(auth, src, famKey, dest) } }
    dev.saged.grcs.ui.components.Sheet({ if (!busy) onClose() }) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("نقل الأسرة — ${plan?.head?.optString("fullName").orEmpty()} (${plan?.members?.size ?: 0} أفراد)", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { camps.filter { it.first != src }.forEach { (id, n) -> QChip(n, dest == id) { dest = id } } }
            QField("سبب النقل", reason, { reason = it })
            plan?.blocks?.forEach { Text(it, color = p.danger, fontSize = 13.sp) }
            plan?.warns?.forEach { Text(it, color = p.mute, fontSize = 12.sp) }
            result?.let { Text(it, color = if (done) p.ok else p.danger, fontSize = 13.sp) }
            if (!done) Box(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).background(if (dest == null || plan?.blocks?.isNotEmpty() == true || busy) p.line else p.pri, RoundedCornerShape(16.dp)).clickable(enabled = dest != null && plan?.blocks?.isEmpty() == true && !busy) {
                    val pl = plan ?: return@clickable
                    val d = dest ?: return@clickable
                    scope.launch {
                        busy = true
                        val err = withContext(Dispatchers.IO) { data.transfer.execute(auth, pl, d, camps.first { it.first == d }.second, camps.firstOrNull { it.first == src }?.second ?: src, reason) }
                        result = err ?: "تم النقل بنجاح"; done = err == null || err.startsWith("نُقلت"); busy = false
                    }
                },
                contentAlignment = Alignment.Center,
            ) { Text(if (busy) "جارٍ النقل…" else "تأكيد النقل", color = p.onPri, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            Text(if (done) "إغلاق" else "إلغاء", color = p.pri, fontSize = 14.sp, modifier = Modifier.heightIn(min = 44.dp).clickable(enabled = !busy) { onClose() }.padding(vertical = 12.dp))
        }
    }
}
