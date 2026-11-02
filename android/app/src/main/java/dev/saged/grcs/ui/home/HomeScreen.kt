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

private class Insight(
    val tiers: Map<String, Int>, val quality: Map<String, Int>, val counts: Map<String, Int> = emptyMap(),
    val criticalCamps: Int = 0, val critical: Map<String, Int> = emptyMap(), val pyramid: List<Pair<Int, Int>> = emptyList(),
)

private val countKeys = listOf("orphan", "child", "preschool", "school", "youth", "mid")

private fun insight(data: DataRepo, token: String, camps: List<CampSummary>): Insight {
    runCatching { Vuln.load(token) }
    val tiers = HashMap<String, Int>()
    val q = HashMap<String, Int>()
    val counts = HashMap<String, Int>(); val critical = HashMap<String, Int>(); var critCamps = 0
    val pyr = Array(17) { IntArray(2) }
    camps.forEach { c ->
        val ps = data.people.persons(c.id)
        if (ps.isEmpty()) return@forEach
        runCatching { Vuln.families(ps).forEach { f -> tiers[f.result.tier] = (tiers[f.result.tier] ?: 0) + 1 } }
        runCatching { q[c.id] = Quality.report(ps).first }
        countKeys.forEach { k -> counts[k] = (counts[k] ?: 0) + dev.saged.grcs.data.PFilter.apply(k, ps).size }
        val st = c.stats
        critical[c.id] = st.infant + st.pregnant + st.chronic
        if (st.infant >= 50 || st.pregnant >= 30 || st.chronic >= 100) critCamps++
        ps.forEach { m -> dev.saged.grcs.data.Stats.ageOf(m)?.let { a -> val b = (a / 5).coerceAtMost(16); pyr[b][if (m.optString("gender") == "female") 1 else 0]++ } }
    }
    return Insight(tiers, q, counts, critCamps, critical, pyr.map { it[0] to it[1] })
}

/** الرئيسية بتصميم الويب: ترويسة كحلية، بطاقات إحصاء بحافة ملوّنة، مؤشر الهشاشة، جودة البيانات لكل مخيم. */
@Composable
fun HomeScreen(auth0: AuthState, data: DataRepo, onLogout: () -> Unit, onKpi: (String) -> Unit = {}, onCamps: () -> Unit = {}, onlyCamp: String? = null, onBack: (() -> Unit)? = null, onPeople: () -> Unit = {}) {
    val p = LocalPalette.current
    var auth by remember { mutableStateOf(auth0) }
    var camps by remember { mutableStateOf(data.cached(auth0).let { l -> if (onlyCamp != null) l.filter { it.id == onlyCamp } else l }) }
    var sub by remember { mutableStateOf("") }
    var info by remember { mutableStateOf<dev.saged.grcs.data.CampInfo?>(null) }
    androidx.compose.runtime.LaunchedEffect(onlyCamp) { if (onlyCamp != null) info = withContext(Dispatchers.IO) { runCatching { data.campInfo(auth0, onlyCamp) }.getOrNull() } }
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
            auth = st; camps = if (onlyCamp != null) list.filter { it.id == onlyCamp } else list
            withContext(Dispatchers.IO) { list.forEach { data.autoBackup(it.id, it.name) } }
            withContext(Dispatchers.IO) { runCatching { insight(data, st.idToken, list) } }.getOrNull()?.let { ins = it }
            if (st.offline) { error = "دون اتصال — تُعرض آخر نسخة محفوظة"; ConnState.state = Conn.Off } else { ConnState.state = Conn.On; ConnState.lastSync = System.currentTimeMillis() }
        } catch (e: Exception) {
            error = "تعذّرت المزامنة — تُعرض آخر نسخة محفوظة"; ConnState.state = Conn.Off
        } finally {
            busy = false
        }
    }

    when (sub) {
        "import" -> { dev.saged.grcs.ui.more.ImportPage(auth, data) { sub = "" }; return }
        "export" -> { dev.saged.grcs.ui.more.ExportPage(auth, data) { sub = "" }; return }
        "caminfo" -> { dev.saged.grcs.ui.more.CampInfoPage(auth, data) { sub = "" }; return }
    }
    val t = camps.fold(CampStats.ZERO) { a, c -> a + c.stats }
    Column(Modifier.fillMaxSize()) {
        AppHeader(if (onlyCamp != null) camps.firstOrNull()?.name ?: BrandConfig.NAME else if (auth.user.role == "admin") "كل المخيمات" else BrandConfig.NAME, "${auth.user.fullName} · ${roleLabel[auth.user.role] ?: auth.user.role}", onBack = onBack) {
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
                if (onlyCamp != null) info?.let { i ->
                    QCard {
                        Text("معلومات المخيم", color = p.ink, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                        Spacer(Modifier.height(6.dp))
                        listOf("المنطقة/الحي" to i.region, "مسؤول المخيم" to listOf(i.supervisor, i.supervisorPhone).filter { it.isNotEmpty() }.joinToString(" — "),
                            "نائب المسؤول" to listOf(i.deputy, i.deputyPhone).filter { it.isNotEmpty() }.joinToString(" — "),
                            "مدخل البيانات" to listOf(i.dataEntry, i.dataEntryPhone).filter { it.isNotEmpty() }.joinToString(" — "),
                            "عدد الأسر المسجَّلة" to fa(camps.firstOrNull()?.stats?.families ?: 0)).forEach { (k, v) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(k, color = p.mute, fontSize = 12.5.sp); Text(v.ifEmpty { "—" }, color = p.ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dev.saged.grcs.ui.components.Btn("الأفراد", onPeople, Modifier.weight(1f), dev.saged.grcs.ui.components.BtnKind.Outline, small = true)
                        dev.saged.grcs.ui.components.Btn("استيراد", { sub = "import" }, Modifier.weight(1f), dev.saged.grcs.ui.components.BtnKind.Outline, small = true)
                        dev.saged.grcs.ui.components.Btn("تحميل كشف", { sub = "export" }, Modifier.weight(1f), dev.saged.grcs.ui.components.BtnKind.Outline, small = true)
                    }
                    Spacer(Modifier.height(8.dp))
                    dev.saged.grcs.ui.components.Btn("تعديل معلومات المخيم", { sub = "caminfo" }, Modifier.fillMaxWidth(), dev.saged.grcs.ui.components.BtnKind.Outline, small = true)
                    Spacer(Modifier.height(12.dp))
                }
                if (camps.isEmpty() || t.total == 0 && !busy) {
                    EmptyState(QIcons.Camp, "لا بيانات بعد", "ستظهر الأرقام فور اكتمال أول مزامنة")
                } else {
                    val c = ins?.counts.orEmpty()
                    val admin = auth.user.role == "admin" && onlyCamp == null
                    val items = if (admin) listOf(
                        K("إجمالي المخيمات", camps.size, p.navy.takeIf { !p.dark } ?: p.blue, QIcons.Camp, "camps"), K("إجمالي الأفراد", t.total, p.tealL, QIcons.People, "all"),
                        K("إجمالي الأسر", t.families, p.navy.takeIf { !p.dark } ?: p.blue, QIcons.Home, "head"), K("مخيمات بحالات حرجة", ins?.criticalCamps ?: 0, p.danger, QIcons.Alert, "criticalCamps"),
                        K("رضّع أقل من سنة", t.infant, p.danger, QIcons.Baby, "infant"), K("أيتام", c["orphan"] ?: 0, p.warn, QIcons.Person, "orphan"),
                        K("أطفال 6-18", c["school"] ?: 0, p.warn, QIcons.Person, "school"), K("كبار السن +60", t.elderly, p.purple, QIcons.Elder, "elderly"),
                        K("حوامل", t.pregnant, p.pregnant, QIcons.Pregnant, "pregnant"), K("أمراض مزمنة", t.chronic, p.warn, QIcons.Heart, "chronic"),
                        K("ذوو إعاقة", t.disability, p.blue, QIcons.Support, "disability"), K("أطفال 1-5 سنوات", c["preschool"] ?: 0, p.danger, QIcons.Person, "preschool"),
                        K("شباب 18-30", c["youth"] ?: 0, p.tealL, QIcons.Person, "youth"), K("بالغون 30-60", c["mid"] ?: 0, p.navy.takeIf { !p.dark } ?: p.blue, QIcons.Person, "mid"),
                        K("أرامل", t.widowed, p.gray, QIcons.Person, "widowed"), K("مطلّقات/ين", t.divorced, p.gray, QIcons.Person, "divorced"),
                        K("أسر بلا هاتف", t.familiesNoPhone, p.gray, QIcons.Phone, "nophone"), K("بلا رقم هوية", t.noId, p.danger, QIcons.IdCard, "noid"),
                    ) else listOf(
                        K("إجمالي الأفراد", t.total, p.navy.takeIf { !p.dark } ?: p.blue, QIcons.People, "all"), K("إجمالي الأسر", t.families, p.tealL, QIcons.Home, "head"),
                        K("الذكور", t.male, p.navy.takeIf { !p.dark } ?: p.blue, QIcons.Male, "male", pct(t.male, t.total)), K("الإناث", t.female, p.tealL, QIcons.Female, "female", pct(t.female, t.total)),
                        K("رضّع أقل من سنة", t.infant, p.danger, QIcons.Baby, "infant"), K("أيتام", c["orphan"] ?: 0, p.warn, QIcons.Person, "orphan"),
                        K("أطفال 1-18", c["child"] ?: 0, p.warn, QIcons.Person, "child"), K("كبار السن +60", t.elderly, p.purple, QIcons.Elder, "elderly"),
                        K("حوامل", t.pregnant, p.pregnant, QIcons.Pregnant, "pregnant"), K("أمراض مزمنة", t.chronic, p.warn, QIcons.Heart, "chronic"),
                        K("ذوو إعاقة", t.disability, p.blue, QIcons.Support, "disability"), K("أسر بلا هاتف", t.familiesNoPhone, p.gray, QIcons.Phone, "nophone"),
                        K("أطفال 1-5 سنوات", c["preschool"] ?: 0, p.danger, QIcons.Person, "preschool"), K("أطفال 6-18 (سن مدرسي)", c["school"] ?: 0, p.warn, QIcons.Person, "school"),
                        K("شباب 18-30", c["youth"] ?: 0, p.tealL, QIcons.Person, "youth"), K("بالغون 30-60", c["mid"] ?: 0, p.navy.takeIf { !p.dark } ?: p.blue, QIcons.Person, "mid"),
                        K("أرامل", t.widowed, p.gray, QIcons.Person, "widowed"), K("مطلّقات/ين", t.divorced, p.gray, QIcons.Person, "divorced"),
                        K("بلا رقم هوية", t.noId, p.danger, QIcons.IdCard, "noid"),
                    )
                    Grid(items, t.total) { k -> if (k == "camps") onCamps() else if (k != "criticalCamps") onKpi(k) }
                    ins?.let { if (it.tiers.isNotEmpty()) Risk(it.tiers, auth.user.role == "admin") }
                    if (camps.size > 1 || ins?.quality?.isNotEmpty() == true) ins?.let { Quality(camps, it.quality) }
                    if (auth.user.role == "admin" && camps.size > 1) ins?.let { HomeCharts(camps, it.critical) }
                    ins?.let { if (it.pyramid.any { b -> b.first + b.second > 0 }) Pyramid(it.pyramid, if (auth.user.role == "admin") "الهرم السكاني — كل المخيمات" else "الهرم السكاني") }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private class K(val label: String, val value: Int, val color: Color, val icon: ImageVector, val key: String, val sub: String? = null)

private fun pct(a: Int, b: Int) = if (b == 0) "0%" else "${fa(Math.round(a * 100f / b))}%"

@Composable
private fun Grid(items: List<K>, total: Int, onKpi: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEachIndexed { ri, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { ci, it -> KpiCard(it.label, it.value, it.color, it.icon, Modifier.weight(1f), sub = it.sub, index = ri * 2 + ci, onClick = { onKpi(it.key) }) }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Risk(tiers: Map<String, Int>, admin: Boolean) {
    val p = LocalPalette.current
    val order = listOf("critical" to p.acc, "high" to p.tierHigh, "medium" to p.tierMedium, "stable" to p.tierStable)
    val total = order.sumOf { tiers[it.first] ?: 0 }.coerceAtLeast(1)
    SectionTitle(if (admin) "مؤشر الهشاشة — كل المخيمات" else "مؤشر الهشاشة")
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
    SectionTitle("جودة البيانات حسب المخيم")
    QCard {
        camps.filter { q.containsKey(it.id) }.sortedBy { q[it.id] ?: 0 }.forEachIndexed { i, c ->
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
