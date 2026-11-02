package dev.saged.grcs.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.CampStats
import dev.saged.grcs.data.CampSummary
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val roleLabel = mapOf("admin" to "مسؤول النظام", "delegate" to "مندوب", "dataentry" to "مدخل بيانات", "beneficiary" to "مستفيد")

/** الرئيسية: أرقام حقيقية من الخادم (قراءة فقط في هذه المرحلة). */
@Composable
fun HomeScreen(auth0: AuthState, data: DataRepo, onLogout: () -> Unit) {
    val p = LocalPalette.current
    var auth by remember { mutableStateOf(auth0) }
    var camps by remember { mutableStateOf(data.cached(auth0)) }
    var busy by remember { mutableStateOf(true) }
    var pulled by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(tick) {
        busy = true; error = null
        try {
            val (st, list) = withContext(Dispatchers.IO) { data.sync(auth) { pulled = it } }
            auth = st; camps = list
            withContext(Dispatchers.IO) { list.forEach { data.autoBackup(it.id, it.name) } }
            if (st.offline) error = "دون اتصال — تُعرض آخر نسخة محفوظة"
        } catch (e: Exception) {
            error = "تعذّرت المزامنة — تُعرض آخر نسخة محفوظة"
        } finally {
            busy = false
        }
    }

    val total = camps.fold(CampStats.ZERO) { a, c -> a + c.stats }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(auth.user.fullName, color = p.ink, fontFamily = GrcsFonts.display, fontSize = 24.sp)
                Text(roleLabel[auth.user.role] ?: auth.user.role, color = p.mute, fontSize = 13.sp)
            }
            Text("خروج", color = p.pri, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.heightIn(min = 44.dp).clickable { onLogout() }.padding(horizontal = 8.dp, vertical = 12.dp))
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().background(p.pri, RoundedCornerShape(24.dp)).padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Big("أسرة", total.families); Big("فرد", total.total)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            when { busy -> "جارٍ المزامنة… ($pulled)"; error != null -> error!!; else -> "محدّث" },
            color = if (error != null) p.warn else p.mute, fontSize = 12.sp,
            modifier = Modifier.heightIn(min = 44.dp).clickable(enabled = !busy) { tick++ }.padding(vertical = 12.dp),
        )
        Grid(
            listOf("ذكور" to total.male, "إناث" to total.female, "أطفال" to total.child, "رضّع" to total.infant,
                "كبار سن" to total.elderly, "حوامل" to total.pregnant, "أمراض مزمنة" to total.chronic,
                "ذوو إعاقة" to total.disability, "أرامل" to total.widowed, "مطلقون" to total.divorced,
                "بلا هوية" to total.noId, "أسر بلا جوال" to total.familiesNoPhone),
        )
        if (auth.user.role == "admin" && camps.size > 1) {
            Spacer(Modifier.height(20.dp))
            Text("المخيمات", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            camps.sortedByDescending { it.stats.total }.forEach { CampRow(it) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Big(label: String, v: Int) {
    val p = LocalPalette.current
    val n by animateIntAsState(v, tween(900), label = "n")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 12.dp)) {
        Text(n.toString(), color = p.onPri, fontFamily = GrcsFonts.display, fontSize = 44.sp)
        Text(label, color = p.onPri.copy(alpha = .85f), fontSize = 13.sp)
    }
}

@Composable
private fun Grid(items: List<Pair<String, Int>>) {
    val p = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (l, v) ->
                    Column(
                        Modifier.weight(1f).background(p.surface, RoundedCornerShape(16.dp)).padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(v.toString(), color = p.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(l, color = p.mute, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
                repeat(3 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CampRow(c: CampSummary) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).background(p.surface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(c.name, color = p.ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text("${c.stats.families} أسرة · ${c.stats.total} فرد", color = p.mute, fontSize = 12.sp)
    }
}
