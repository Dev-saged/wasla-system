package dev.saged.grcs.ui.more

import dev.saged.grcs.ui.components.AppHeader
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
import dev.saged.grcs.data.Issue
import dev.saged.grcs.data.Quality
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun QualityPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val camps = remember { data.campList(auth) }
    var camp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var pct by remember { mutableStateOf(100) }
    var issues by remember { mutableStateOf<List<Issue>>(emptyList()) }
    var open by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(camp) {
        val r = withContext(Dispatchers.IO) { Quality.report(data.people.persons(camp)) }
        pct = r.first; issues = r.second; open = null
    }
    Column(Modifier.fillMaxSize()) {
        AppHeader("جودة البيانات", "", conn = null, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (camps.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { camps.forEach { (id, n) -> QChip(n, id == camp) { camp = id } } }
            Text("$pct٪", color = if (pct >= 85) p.ok else p.warn, fontFamily = GrcsFonts.display, fontSize = 44.sp)
            issues.forEach { i ->
                Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(16.dp)).clickable { open = if (open == i.key) null else i.key }.padding(14.dp)) {
                    Row { Text(i.label, color = p.ink, fontSize = 15.sp, modifier = Modifier.weight(1f)); Text("${i.people.size}", color = if (i.people.isEmpty()) p.ok else p.danger, fontWeight = FontWeight.Bold) }
                    if (open == i.key) i.people.take(60).forEach { Text(it.optString("fullName") + (it.optString("nationalId").takeIf { n -> n.isNotEmpty() }?.let { n -> " · $n" } ?: ""), color = p.mute, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
        }
    }
}
