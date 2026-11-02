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
import dev.saged.grcs.ui.people.EditScreen
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.Bar
import dev.saged.grcs.ui.components.SegTabs
import dev.saged.grcs.data.Messages
import dev.saged.grcs.data.Merge
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.QCard
import dev.saged.grcs.ui.components.SectionTitle
import dev.saged.grcs.ui.icons.QIcons
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
    var tab by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<JSONObject?>(null) }
    var msgs by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    LaunchedEffect(rev, tab) { if (tab == 3) msgs = withContext(Dispatchers.IO) { runCatching { Messages().loadBen(auth) }.getOrDefault(emptyList()) } }
    fun saveP(o: JSONObject, extras: List<JSONObject>) {
        scope.launch {
            withContext(Dispatchers.IO) {
                o.put("updatedBy", auth.user.id)
                val sv = data.people.save(camp, o); data.afterSave(auth, camp, sv, false)
                for (x in extras) { x.put("familyId", sv.optString("familyId")).put("updatedBy", auth.user.id).put("createdBy", auth.user.id); data.afterSave(auth, camp, data.people.save(camp, x), false) }
                data.pushNow(auth, camp)
            }
            editing = null; rev++
        }
    }
    val ed = editing
    if (ed != null) { EditScreen(ed, persons, null, { editing = null }) { o, ex -> saveP(o, ex) }; return }
    Column(Modifier.fillMaxSize()) {
      AppHeader(auth.user.fullName, if (busy) "جارٍ التحديث…" else "${persons.size} أفراد في أسرتك")
      Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { SegTabs(listOf("ملفي", "أسرتي", "طلباتي", "إشعاراتي"), tab, { tab = it }) }
      Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (tab) {
            0 -> if (head != null) ProfileTab(head, persons, { editing = head }, { o -> saveP(o, emptyList()) })
            1 -> {
                if (head != null) Btn("إضافة فرد جديد", { editing = JSONObject().put("familyId", head.optString("familyId")).put("isFamilyHead", false).put("relation", "son") }, Modifier.fillMaxWidth(), icon = QIcons.Plus)
                persons.forEach { m ->
                    QCard(onClick = { editing = m }) {
                        Text(m.optString("fullName"), color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(listOfNotNull(if (m.optBoolean("isFamilyHead")) "رب الأسرة" else null, m.optString("birthDate").takeIf { it.isNotEmpty() && it != "null" }).joinToString(" · "), color = p.mute, fontSize = 12.sp)
                        Aid.list(m).sortedByDescending { it.optLong("at") }.take(3).forEach { r ->
                            Text("استلام: " + (Aid.types.firstOrNull { it.k == r.optString("t") }?.label ?: r.optString("t")) + " · " + r.optString("d"), color = p.ok, fontSize = 12.sp)
                        }
                    }
                }
                Text("اضغط على أي فرد لتحديث بياناته — يصل التعديل للمندوب والإدارة", color = p.mute, fontSize = 12.sp)
            }
            2 -> if (head != null) {
            SectionTitle("طلباتي")
            Aid.requests(head).sortedByDescending { it.optLong("createdAt") }.forEach { r ->
                QCard {
                    Text((Aid.reqTypes.firstOrNull { it.first == r.optString("type") }?.second ?: r.optString("type")) + " — " + (Aid.reqStatus[r.optString("status")] ?: ""), color = p.ink, fontSize = 14.sp)
                    if (r.optString("details").isNotEmpty()) Text(r.optString("details"), color = p.mute, fontSize = 12.sp)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Aid.reqTypes.forEach { (k, l) -> QChip(l, type == k) { type = k } } }
            QField("تفاصيل الطلب", details, { details = it }, multiline = true)
            Btn("إرسال الطلب", {
                    if (details.trim().length < 3) return@Btn
                    val o = Aid.addRequest(head, type, details, auth.user.id).put("updatedBy", auth.user.id)
                    details = ""
                    scope.launch {
                        withContext(Dispatchers.IO) { data.people.save(camp, o); data.pushNow(auth, camp) }
                        rev++
                    }
            }, Modifier.fillMaxWidth(), icon = QIcons.Broadcast)
        }
            3 -> {
                if (msgs.isEmpty()) Text("لا إشعارات بعد", color = p.mute, fontSize = 13.sp)
                msgs.sortedByDescending { it.optLong("timestamp") }.forEach { m ->
                    QCard {
                        Text(m.optString("senderName"), color = p.pri, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(m.optString("text"), color = p.ink, fontSize = 14.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
      }
    }
}

@Composable
private fun ProfileTab(head: JSONObject, persons: List<JSONObject>, onFull: () -> Unit, onSave: (JSONObject) -> Unit) {
    val p = LocalPalette.current
    var phone by remember(head) { mutableStateOf(head.optString("phone")) }
    var phone2 by remember(head) { mutableStateOf(head.optString("phone2")) }
    var address by remember(head) { mutableStateOf(head.optString("address")) }
    var chronic by remember(head) { mutableStateOf(head.optBoolean("hasChronicDisease")) }
    var cd by remember(head) { mutableStateOf(head.optString("diseaseDetails")) }
    var dis by remember(head) { mutableStateOf(head.optBoolean("hasDisability")) }
    var dd by remember(head) { mutableStateOf(head.optString("disabilityDetails")) }
    var notes by remember(head) { mutableStateOf(head.optString("notes")) }
    var shelter by remember(head) { mutableStateOf(head.optString("shelterType")) }
    var living by remember(head) { mutableStateOf(head.optString("livingDetails")) }
    var urgent by remember(head) { mutableStateOf(head.optBoolean("urgentNeed")) }
    var ud by remember(head) { mutableStateOf(head.optString("urgentNeedDetails")) }
    val filled = listOf(head.optString("phone"), head.optString("address"), head.optString("nationalId"), head.optString("birthDate").takeIf { it != "null" }.orEmpty(), head.optString("shelterType")).count { it.isNotEmpty() }
    QCard {
        Text("اكتمال بياناتي", color = p.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Bar(filled / 5f, if (filled == 5) p.success else p.warn)
        Text("$filled من 5 حقول أساسية", color = p.mute, fontSize = 12.sp)
    }
    QCard {
        Text(head.optString("fullName"), color = p.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("الهوية: " + head.optString("nationalId").ifEmpty { "—" } + " · أفراد الأسرة: ${persons.size}", color = p.mute, fontSize = 12.sp)
    }
    QField("رقم الهاتف", phone, { phone = it }, type = androidx.compose.ui.text.input.KeyboardType.Phone)
    QField("رقم هاتف إضافي", phone2, { phone2 = it }, type = androidx.compose.ui.text.input.KeyboardType.Phone)
    QField("العنوان / الموقع الحالي", address, { address = it })
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QChip("أعاني من مرض مزمن", chronic) { chronic = !chronic }
        QChip("لدي إعاقة", dis) { dis = !dis }
        QChip("لدي احتياج طارئ", urgent) { urgent = !urgent }
    }
    if (chronic) QField("تفاصيل المرض", cd, { cd = it })
    if (dis) QField("تفاصيل الإعاقة", dd, { dd = it })
    if (urgent) QField("ما هو الاحتياج الطارئ؟", ud, { ud = it }, multiline = true)
    QField("ملاحظات صحية", notes, { notes = it }, multiline = true)
    Text("نوع المأوى", color = p.mute, fontSize = 12.sp)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("tent_ok" to "خيمة سليمة", "tent_damaged" to "خيمة تالفة", "shared" to "مأوى مشترك", "building_damaged" to "مبنى متضرر", "none" to "بلا مأوى").forEach { (k, l) -> QChip(l, shelter == k) { shelter = k } }
    }
    QField("تفاصيل الوضع المعيشي", living, { living = it }, multiline = true)
    Btn("حفظ ومزامنة", {
        val o = Merge.copy(head)
        o.put("phone", phone.trim()).put("phone2", phone2.trim()).put("address", address.trim())
            .put("hasChronicDisease", chronic).put("diseaseDetails", if (chronic) cd.trim() else "")
            .put("hasDisability", dis).put("disabilityDetails", if (dis) dd.trim() else "")
            .put("urgentNeed", urgent).put("urgentNeedDetails", if (urgent) ud.trim() else "")
            .put("notes", notes.trim()).put("shelterType", shelter).put("livingDetails", living.trim())
        onSave(o)
    }, Modifier.fillMaxWidth(), icon = QIcons.Check)
    Btn("تعديل البيانات الكاملة", onFull, Modifier.fillMaxWidth(), kind = BtnKind.Outline, icon = QIcons.Edit)
}
