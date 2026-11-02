package dev.saged.grcs.ui.camps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import dev.saged.grcs.data.PFilter
import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.QCard
import dev.saged.grcs.ui.components.fa
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** كشوفات التصنيفات لكل المخيمات: عدد كل فئة بالمجموع ولكل مخيم، وتحميل كشف Excel موحّد. */
@Composable
fun CatsPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val camps = remember { data.campList(auth) }
    var all by remember { mutableStateOf<Map<String, List<JSONObject>>?>(null) }
    var note by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { all = withContext(Dispatchers.IO) { camps.associate { it.first to data.people.persons(it.first) } } }
    val cats = PFilter.chips.filter { !it.first.startsWith("tier:") }
    Column(Modifier.fillMaxSize()) {
        AppHeader("كشوفات التصنيفات", "كل المخيمات", conn = null, onBack = onBack)
        if (note.isNotEmpty()) Text(note, color = p.mute, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cats, key = { it.first }) { (k, l) ->
                val per = all?.mapValues { PFilter.apply(k, it.value).size }
                QCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(l, color = p.ink, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                            Text(per?.let { m -> camps.joinToString(" · ") { c -> "${c.second} ${fa(m[c.first] ?: 0)}" } } ?: "جارٍ الحساب…", color = p.mute, fontSize = 11.5.sp)
                        }
                        Text(per?.values?.sum()?.let { fa(it) } ?: "", color = p.pri, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Btn("كشف", {
                            scope.launch {
                                val r = withContext(Dispatchers.IO) { data.export.custom("xlsx", "كل المخيمات", all.orEmpty().values.flatten(), k, false, data.export.defaultCols + listOf("phone2", "shelter")) }
                                note = if (r.ok) "تم حفظ ${r.count} سجل: ${r.name}" else "تعذّر الحفظ"
                            }
                        }, kind = BtnKind.Outline, small = true, icon = QIcons.Export)
                    }
                }
            }
        }
    }
}
