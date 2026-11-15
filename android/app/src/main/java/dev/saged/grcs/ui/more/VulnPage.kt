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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.Vuln
import dev.saged.grcs.data.VulnFamily
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** مؤشر الهشاشة: توزيع الأسر على المستويات وقائمة مرتبة بالأشد حاجة. */
@Composable
fun VulnPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val camps = remember { data.campList(auth) }
    var camp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var all by remember { mutableStateOf<List<VulnFamily>>(emptyList()) }
    var tier by remember { mutableStateOf("critical") }
    LaunchedEffect(camp) {
        all = withContext(Dispatchers.IO) {
            runCatching { Vuln.load(auth.idToken) }
            Vuln.families(camps.flatMap { data.people.persons(it.first) })
        }
    }
    fun color(t: String): Color = when (t) { "critical" -> p.danger; "high" -> p.acc; "medium" -> p.warn; else -> p.ok }
    Column(Modifier.fillMaxSize()) {
        AppHeader("مؤشر الهشاشة", "", conn = null, onBack = onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Vuln.tiers.forEach { (k, l) -> QChip("$l ${all.count { it.result.tier == k }}", tier == k) { tier = k } }
            }
        }
        LazyColumn(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(all.filter { it.result.tier == tier }.take(300), key = { it.head.optString("id") }) { f ->
                Column(Modifier.fillMaxWidth().qcard().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(f.head.optString("fullName"), color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text("${f.result.score}", color = color(f.result.tier), fontFamily = GrcsFonts.display, fontSize = 22.sp)
                    }
                    Text("${f.members} أفراد · " + f.result.factors.joinToString("، ") { it.first }, color = p.mute, fontSize = 12.sp)
                }
            }
        }
    }
}
