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
import dev.saged.grcs.data.Aid
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AidReportPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val camps = remember { data.ctxCamps(auth) }
    var camp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var days by remember { mutableStateOf(30) }
    var rows by remember { mutableStateOf<List<Aid.Row>>(emptyList()) }
    LaunchedEffect(days) { rows = withContext(Dispatchers.IO) { Aid.report(camps.flatMap { data.people.persons(it.first) }, days) } }
    Column(Modifier.fillMaxSize()) {
        AppHeader("تقرير المساعدات", "", conn = null, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7 to "7 أيام", 30 to "30 يوماً", 90 to "90 يوماً", 0 to "الكل").forEach { (d, l) -> QChip(l, d == days) { days = d } }
            }
            if (rows.isEmpty()) Text("لا مساعدات مسجّلة في هذه الفترة", color = p.mute, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
            rows.forEach { r ->
                Row(Modifier.fillMaxWidth().qcard().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.label, color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text("${r.people} مستفيد", color = p.mute, fontSize = 12.sp)
                    }
                    Text(if (r.qty == r.qty.toLong().toDouble()) "${r.qty.toLong()}" else "%.1f".format(r.qty), color = p.pri, fontFamily = GrcsFonts.display, fontSize = 22.sp)
                }
            }
        }
    }
}
