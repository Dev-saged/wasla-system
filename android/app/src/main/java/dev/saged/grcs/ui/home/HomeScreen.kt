package dev.saged.grcs.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.config.BrandConfig
import dev.saged.grcs.data.CampStats
import dev.saged.grcs.data.CampSummary
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.Quality
import dev.saged.grcs.data.Vuln
import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.Bar
import dev.saged.grcs.ui.components.Conn
import dev.saged.grcs.ui.components.ConnState
import dev.saged.grcs.ui.components.EmptyState
import dev.saged.grcs.ui.components.HeaderBtn
import dev.saged.grcs.data.PFilter
import dev.saged.grcs.ui.components.KpiCard
import dev.saged.grcs.ui.components.QCard
import dev.saged.grcs.ui.components.Ring
import dev.saged.grcs.ui.components.SectionTitle
import dev.saged.grcs.ui.components.fa
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val roleLabel = mapOf("admin" to "مسؤول النظام", "delegate" to "مندوب", "dataentry" to "مدخل بيانات", "beneficiary" to "مستفيد")

private class Insight(val tiers: Map<String, Int>, val quality: Map<String, Int>)

private fun insight(data: DataRepo, token: String, camps: List<CampSummary>): Insight {
    runCatching { Vuln.load(token) }
    val tiers = HashMap<String, Int>()
    val q = HashMap<String, Int>()
    camps.forEach { c ->
        val ps = data.people.persons(c.id)
        if (ps.isEmpty()) return@forEach
        runCatching { Vuln.families(ps).forEach { f -> tiers[f.result.tier] = (tiers[f.result.tier] ?: 0) + 1 } }
        runCatching { q[c.id] = Quality.report(ps).first }
    }
    return Insight(tiers, q)
}

/** الرئيسية بتصميم الويب: ترويسة كحلية، بطاقات إحصاء بحافة ملوّنة، مؤشر الهشاشة، جودة البيانات لكل مخيم. */
@Composable
fun HomeScreen(auth0: AuthState, data: DataRepo, onLogout: () -> Unit, onKpi: (String) -> Unit = {}) {
    val p = LocalPalette.current
    var auth by remember { mutableStateOf(auth0) }
    var camps by remember { mutableStateOf(data.cached(auth0)) }
    var ins by remember { mutableStateOf<Insight?>(null) }
    var busy by remember { mutableStateOf(true) }
    var pulled by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(tick) {
        busy = true; error = null; ConnState.state = Conn.Sync
        withContext(Dispatchers.IO) { runCatching { insight(data, auth.idToken, camps) } }.getOrNull()?.let { ins = it }
        try {
            val (st, list) = withContext(Dispatchers.IO) { data.sync(auth) { pulled = it } }
            auth = st; camps = list
            withContext(Dispatchers.IO) { list.forEach { data.autoBackup(it.id, it.name) } }
            withContext(Dispatchers.IO) { runCatching { insight(data, st.idToken, list) } }.getOrNull()?.let { ins = it }
            if (st.offline) { error = "دون اتصال — تُعرض آخر نسخة محفوظة"; ConnState.state = Conn.Off } else { ConnState.state = Conn.On; ConnState.lastSync = System.currentTimeMillis() }
        } catch (e: Exception) {
            error = "تعذّرت المزامنة — تُعرض آخر نسخة محفوظة"; ConnState.state = Conn.Off
        } finally {
            busy = false
        }
    }

    val t = camps.fold(CampStats.ZERO) { a, c -> a + c.stats }
    Column(Modifier.fillMaxSize()) {
        AppHeader(BrandConfig.NAME, "${auth.user.fullName} · ${roleLabel[auth.user.role] ?: auth.user.role}") {
            HeaderBtn(QIcons.Sync, "مزامنة") { if (!busy) tick++ }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                if (busy || error != null) {
                    Text(
                        if (busy) "جارٍ المزامنة… ($pulled)" else error.orEmpty(),
                        color = if (busy) p.mute else p.warn, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                if (camps.isEmpty() || t.total == 0 && !busy) {
                    EmptyState(QIcons.Camp, "لا بيانات بعد", "ستظهر الأرقام فور اكتمال أول مزامنة")
                } else {
                    Grid(
                        listOf(
                            K("أسرة", t.families, p.pri, QIcons.Home), K("فرد", t.total, p.navy.takeIf { !p.dark } ?: p.blue, QIcons.People),
                            K("ذكور", t.male, p.blue, QIcons.Male), K("إناث", t.female, p.purple, QIcons.Female),
                            K("أطفال", t.child, p.warn, QIcons.Person), K("رضّع", t.infant, p.tealL, QIcons.Baby),
                            K("كبار سن", t.elderly, p.gray, QIcons.Elder), K("حوامل", t.pregnant, p.pregnant, QIcons.Pregnant),
                            K("أمراض مزمنة", t.chronic, p.danger, QIcons.Heart), K("ذوو إعاقة", t.disability, p.purple, QIcons.Support),
                            K("أرامل", t.widowed, p.gray, QIcons.Person), K("مطلقون", t.divorced, p.gray, QIcons.Person),
                            K("بلا هوية", t.noId, p.warn, QIcons.IdCard), K("أسر بلا جوال", t.familiesNoPhone, p.danger, QIcons.Phone),
                        ),
                        onKpi,
                    )
                    ins?.let { if (it.tiers.isNotEmpty()) Risk(it.tiers) }
                    if (camps.size > 1 || ins?.quality?.isNotEmpty() == true) ins?.let { Quality(camps, it.quality) }
                    if (auth.user.role == "admin" && camps.size > 1) {
                        SectionTitle("المخيمات")
                        camps.sortedByDescending { it.stats.total }.forEach { c ->
                            QCard(Modifier.padding(bottom = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(c.name, color = p.ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                                        Text("${fa(c.stats.families)} أسرة · ${fa(c.stats.total)} فرد", color = p.mute, fontSize = 12.sp)
                                    }
                                    Ring(if (t.total == 0) 0f else c.stats.total.toFloat() / t.total, p.pri, 48.dp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private class K(val label: String, val value: Int, val color: Color, val icon: ImageVector) { val key = PFilter.kpiKey[label] ?: "all" }

@Composable
private fun Grid(items: List<K>, onKpi: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEachIndexed { ri, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { ci, it -> KpiCard(it.label, it.value, it.color, it.icon, Modifier.weight(1f), index = ri * 2 + ci, onClick = { onKpi(it.key) }) }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Risk(tiers: Map<String, Int>) {
    val p = LocalPalette.current
    val order = listOf("critical" to p.acc, "high" to p.tierHigh, "medium" to p.tierMedium, "stable" to p.tierStable)
    val total = order.sumOf { tiers[it.first] ?: 0 }.coerceAtLeast(1)
    SectionTitle("مؤشر الهشاشة")
    QCard {
        Row(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape)) {
            order.forEach { (k, c) ->
                val n = tiers[k] ?: 0
                if (n > 0) Box(Modifier.weight(n.toFloat()).fillMaxSize().background(c))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Vuln.tiers.forEach { (k, label) ->
                val c = order.first { it.first == k }.second
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(fa(tiers[k] ?: 0), color = p.ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.height(8.dp).widthIn(min = 8.dp, max = 8.dp).background(c, CircleShape))
                        Text(label, color = p.mute, fontSize = 11.5.sp)
                    }
                }
            }
        }
        Text("إجمالي ${fa(total)} أسرة مُقيَّمة", color = p.mute, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun Quality(camps: List<CampSummary>, q: Map<String, Int>) {
    val p = LocalPalette.current
    SectionTitle("جودة البيانات")
    QCard {
        camps.filter { q.containsKey(it.id) }.forEachIndexed { i, c ->
            val s = q[c.id] ?: 0
            val col = if (s >= 80) p.success else if (s >= 60) p.warn else p.danger
            if (i > 0) Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(c.name, color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("$s%", color = col, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(6.dp))
            Bar(s / 100f, col)
        }
    }
}
