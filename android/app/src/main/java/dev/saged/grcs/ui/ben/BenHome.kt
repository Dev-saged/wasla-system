package dev.saged.grcs.ui.ben

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import dev.saged.grcs.data.Aid
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.components.QField
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** شاشة المستفيد: أسرته، مساعداته، وتقديم الطلبات ومتابعة حالتها. */
@Composable
fun BenHome(auth0: AuthState, data: DataRepo) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    var auth by remember { mutableStateOf(auth0) }
    var persons by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var rev by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(true) }
    var type by remember { mutableStateOf("food") }
    var details by remember { mutableStateOf("") }
    val camp = auth.user.campId

    LaunchedEffect(rev) {
        busy = true
        persons = withContext(Dispatchers.IO) { data.people.persons(camp) }
        runCatching { withContext(Dispatchers.IO) { data.sync(auth) } }.getOrNull()?.let { auth = it.first }
        persons = withContext(Dispatchers.IO) { data.people.persons(camp) }
        busy = false
    }

    val head = persons.firstOrNull { it.optBoolean("isFamilyHead") } ?: persons.firstOrNull()
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(auth.user.fullName, color = p.ink, fontFamily = GrcsFonts.display, fontSize = 24.sp)
        Text(if (busy) "جارٍ التحديث…" else "${persons.size} أفراد في أسرتك", color = p.mute, fontSize = 13.sp)
        persons.forEach { m ->
            Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(16.dp)).padding(14.dp)) {
                Text(m.optString("fullName"), color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(listOfNotNull(if (m.optBoolean("isFamilyHead")) "رب الأسرة" else null, m.optString("birthDate").ifEmpty { null }).joinToString(" · "), color = p.mute, fontSize = 12.sp)
                Aid.list(m).sortedByDescending { it.optLong("at") }.take(3).forEach { r ->
                    Text("استلام: " + (Aid.types.firstOrNull { it.k == r.optString("t") }?.label ?: r.optString("t")) + " · " + r.optString("d"), color = p.ok, fontSize = 12.sp)
                }
            }
        }
        if (head != null) {
            Spacer(Modifier.height(6.dp))
            Text("طلباتي", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Aid.requests(head).sortedByDescending { it.optLong("createdAt") }.forEach { r ->
                Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(14.dp)).padding(12.dp)) {
                    Text((Aid.reqTypes.firstOrNull { it.first == r.optString("type") }?.second ?: r.optString("type")) + " — " + (Aid.reqStatus[r.optString("status")] ?: ""), color = p.ink, fontSize = 14.sp)
                    if (r.optString("details").isNotEmpty()) Text(r.optString("details"), color = p.mute, fontSize = 12.sp)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Aid.reqTypes.forEach { (k, l) -> QChip(l, type == k) { type = k } } }
            QField("تفاصيل الطلب", details, { details = it }, multiline = true)
            Box(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).background(p.pri, RoundedCornerShape(16.dp)).clickable {
                    if (details.trim().length < 3) return@clickable
                    val o = Aid.addRequest(head, type, details, auth.user.id).put("updatedBy", auth.user.id)
                    details = ""
                    scope.launch {
                        withContext(Dispatchers.IO) { data.people.save(camp, o); data.pushNow(auth, camp) }
                        rev++
                    }
                },
                contentAlignment = Alignment.Center,
            ) { Text("إرسال الطلب", color = p.onPri, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        }
        Spacer(Modifier.height(24.dp))
    }
}
