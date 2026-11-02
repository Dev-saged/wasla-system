package dev.saged.grcs.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.data.CampSummary
import dev.saged.grcs.ui.components.QCard
import dev.saged.grcs.ui.components.SectionTitle
import dev.saged.grcs.ui.components.fa
import dev.saged.grcs.ui.theme.LocalPalette

/** مخططات لوحة المدير: ترتيب المخيمات بالسكان، والحالات الحرجة (رضّع + حوامل + مزمنون) لكل مخيم. */
@Composable
fun HomeCharts(camps: List<CampSummary>, critical: Map<String, Int>) {
    val p = LocalPalette.current
    val top = camps.sortedByDescending { it.stats.total }.take(10)
    val maxT = (top.maxOfOrNull { it.stats.total } ?: 1).coerceAtLeast(1)
    SectionTitle("ترتيب المخيمات بعدد السكان")
    QCard {
        top.forEach { c ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(c.name, color = p.ink, fontSize = 12.sp, maxLines = 1, modifier = Modifier.width(92.dp))
                Box(Modifier.weight(1f).height(14.dp).background(p.surface2, RoundedCornerShape(7.dp))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(c.stats.total.toFloat() / maxT).background(p.tealL, RoundedCornerShape(7.dp)))
                }
                Text(fa(c.stats.total), color = p.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
            }
        }
    }
    val crit = camps.map { it to (critical[it.id] ?: 0) }.sortedByDescending { it.second }.take(10)
    val maxC = (crit.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    SectionTitle("الحالات الحرجة حسب المخيم")
    QCard {
        Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            crit.forEach { (c, n) ->
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text(fa(n), color = p.danger, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height((110 * n.toFloat() / maxC).coerceAtLeast(3f).dp).background(p.danger, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
                    Spacer(Modifier.height(4.dp))
                    Text(c.name, color = p.mute, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
    }
}

/** الهرم السكاني بفئات 5 سنوات: الذكور يميناً والإناث يساراً كما في الويب (RTL: ذكور أولاً من اليمين). */
@Composable
fun Pyramid(bins: List<Pair<Int, Int>>, title: String) {
    val p = LocalPalette.current
    val mx = bins.maxOf { maxOf(it.first, it.second) }.coerceAtLeast(1)
    SectionTitle(title)
    QCard {
        Row(Modifier.fillMaxWidth()) {
            Text("ذكور", color = p.navy.takeIf { !p.dark } ?: p.blue, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("إناث", color = p.tealL, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        bins.indices.reversed().forEach { i ->
            val (m, f) = bins[i]
            val label = if (i == 16) "80+" else "${i * 5}-${i * 5 + 4}"
            Row(Modifier.fillMaxWidth().padding(vertical = 1.5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(10.dp), contentAlignment = Alignment.CenterStart) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(m.toFloat() / mx).background(p.navy.takeIf { !p.dark } ?: p.blue, RoundedCornerShape(3.dp)))
                }
                Text(label, color = p.mute, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.width(38.dp))
                Box(Modifier.weight(1f).height(10.dp), contentAlignment = Alignment.CenterEnd) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(f.toFloat() / mx).background(p.tealL, RoundedCornerShape(3.dp)))
                }
            }
        }
    }
}
