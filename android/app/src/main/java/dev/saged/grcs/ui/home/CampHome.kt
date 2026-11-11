package dev.saged.grcs.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.data.Quality
import dev.saged.grcs.data.Vuln
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.KpiCard
import dev.saged.grcs.ui.components.QCard
import dev.saged.grcs.ui.components.SectionTitle
import dev.saged.grcs.ui.components.fa
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.theme.LocalPalette

/** أقسام رئيسية المخيم بترتيب الويب: الهشاشة (قابلة للنقر)، مهام الجودة، توزيع الجنس، توزيع الأعمار. */
@Composable
fun CampSections(tiers: Map<String, Int>, qpct: Int, qcounts: Map<String, Int>, sim: Int, male: Int, female: Int, infant: Int, child: Int, elderly: Int, total: Int, onFilter: (String) -> Unit, onGo: (String) -> Unit) {
    val p = LocalPalette.current
    val order = listOf("critical" to p.acc, "high" to p.tierHigh, "medium" to p.tierMedium, "stable" to p.tierStable)
    val n = order.sumOf { tiers[it.first] ?: 0 }
    if (n > 0) {
        SectionTitle("مؤشر الهشاشة") { Text("$n أسرة", color = p.mute, fontSize = 12.sp) }
        QCard {
            Row(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape)) {
                order.forEach { (k, c) -> val v = tiers[k] ?: 0; if (v > 0) Box(Modifier.weight(v.toFloat()).fillMaxHeight().background(c).clickable { onFilter("tier:$k") }) }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Vuln.tiers.forEach { (k, label) ->
                    val c = order.first { it.first == k }.second
                    Column(Modifier.weight(1f).heightIn(min = 44.dp).clickable { onFilter("tier:$k") }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(fa(tiers[k] ?: 0), color = p.ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(8.dp).background(c, CircleShape)); Text(label, color = p.mute, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }
    }
    val open = Quality.rules.filter { (qcounts[it.key] ?: 0) > 0 }
    val doneN = Quality.rules.size - open.size
    val pc = if (qpct < 60) p.danger else if (qpct < 85) p.warn else p.ok
    SectionTitle("مهام جودة البيانات") { Box(Modifier.background(pc.copy(alpha = .14f), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 3.dp)) { Text("$qpct% مكتمل", color = pc, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
    QCard {
        open.forEach { r ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onFilter("q:" + r.key) }, verticalAlignment = Alignment.CenterVertically) {
                Text(fa(qcounts[r.key] ?: 0), color = p.warn, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(p.warn.copy(alpha = .14f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 2.dp))
                Text(r.label, color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            }
        }
        if (sim > 0) Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onGo("similar") }, verticalAlignment = Alignment.CenterVertically) {
            Text("$sim", color = p.warn, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(p.warn.copy(alpha = .14f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 2.dp))
            Text("أسماء متشابهة محتملة التكرار", color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
        }
        if (open.isEmpty() && sim == 0) Text("كل مهام الجودة منجزة", color = p.ok, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
        if (doneN > 0 && open.isNotEmpty()) Text("$doneN من ${Quality.rules.size} مهام مكتملة", color = p.mute, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Btn("إدخال سريع", { onGo("rapid") }, Modifier.weight(1f), BtnKind.Outline, dev.saged.grcs.ui.icons.Fa.Bolt, small = true)
        Btn("سجل الاستيراد", { onGo("import") }, Modifier.weight(1f), BtnKind.Outline, dev.saged.grcs.ui.icons.Fa.ClockRotateLeft, small = true)
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Btn("مركز الجودة", { onGo("quality") }, Modifier.weight(1f), BtnKind.Outline, dev.saged.grcs.ui.icons.Fa.ShieldHalved, small = true)
        Btn("المساعدات", { onGo("aid") }, Modifier.weight(1f), BtnKind.Outline, dev.saged.grcs.ui.icons.Fa.BoxOpen, small = true)
    }
    SectionTitle("توزيع الجنس")
    Donut(male, female, (total - male - female).coerceAtLeast(0))
    SectionTitle("توزيع الأعمار")
    AgeBars(infant, child, (total - infant - child - elderly).coerceAtLeast(0), elderly)
}

@Composable
private fun Donut(male: Int, female: Int, unknown: Int) {
    val p = LocalPalette.current
    val a = p.navy.takeIf { !p.dark } ?: p.blue
    val segs = listOf(Triple("ذكور", male, a), Triple("إناث", female, p.tealL)) + if (unknown > 0) listOf(Triple("غير محدد", unknown, p.gray)) else emptyList()
    val tot = (male + female + unknown).coerceAtLeast(1)
    QCard {
        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(136.dp)) {
                val w = 22.dp.toPx(); var start = -90f
                segs.forEach { (_, v, c) -> val sw = 360f * v / tot; if (v > 0) drawArc(c, start, sw, false, topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Butt)); start += sw }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(fa(male + female + unknown), color = p.ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Text("إجمالي", color = p.mute, fontSize = 11.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            segs.forEach { (l, v, c) -> Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(10.dp).background(c, RoundedCornerShape(3.dp))); Text("$l: ", color = p.ink, fontSize = 11.sp); Text(fa(v), color = p.ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            } }
        }
    }
}

@Composable
private fun AgeBars(infant: Int, child: Int, adult: Int, elderly: Int) {
    val p = LocalPalette.current
    val cats = listOf(Triple("رضّع أقل من سنة", infant, Color(0xFFDC2626)), Triple("أطفال 1-18", child, Color(0xFFD97706)), Triple("بالغون 18-60", adult, Color(0xFF0E7490)), Triple("كبار السن +60", elderly, Color(0xFF7C3AED)))
    val mx = cats.maxOf { it.second }.coerceAtLeast(1)
    QCard {
        cats.forEach { (l, v, c) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(l, color = p.mute, fontSize = 11.sp, modifier = Modifier.width(104.dp))
                Box(Modifier.weight(1f).height(16.dp).background(p.surface2, RoundedCornerShape(5.dp))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth((v.toFloat() / mx).coerceAtLeast(.02f)).background(c.copy(alpha = .85f), RoundedCornerShape(5.dp)))
                }
                Text(fa(v), color = p.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(40.dp))
            }
        }
    }
}

/** رئيسية مدخل البيانات: بطاقتان (ما أدخله، اليوم) وبطاقة تلميح بزر إدخال سريع — كما في refreshDataEntryDashboard. */
@Composable
fun DataEntryBody(mine: Int, today: Int, onRapid: () -> Unit) {
    val p = LocalPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        KpiCard("إجمالي ما أدخلتَه", mine, p.tealL, QIcons.Person, Modifier.weight(1f), index = 0)
        KpiCard("اليوم", today, p.navy.takeIf { !p.dark } ?: p.blue, QIcons.Clock, Modifier.weight(1f), index = 1)
    }
    Spacer(Modifier.height(12.dp))
    QCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("استخدم زر «+» لإضافة فرد جديد، أو الإدخال السريع للأسر المتتابعة", color = p.mute, fontSize = 14.sp, textAlign = TextAlign.Center)
            Btn("إدخال سريع", onRapid, Modifier.fillMaxWidth(), BtnKind.Primary, QIcons.Bolt)
        }
    }
}
