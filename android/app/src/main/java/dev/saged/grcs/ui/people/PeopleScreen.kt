package dev.saged.grcs.ui.people

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.Merge
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.components.QField
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val relLabels = listOf("wife" to "زوجة", "husband" to "زوج", "son" to "ابن", "daughter" to "ابنة", "orphan" to "يتيم/ة", "other" to "آخر")
private val maritalLabels = listOf("متزوج/ة", "أرمل/ة", "مطلق/ة", "أعزب")

private fun normDigits(s: String): String = buildString { for (c in s) append(when (c) { in '٠'..'٩' -> '0' + (c - '٠'); in '۰'..'۹' -> '0' + (c - '۰'); else -> c }) }.trim()

private class Fam(val key: String, val head: JSONObject?, val members: List<JSONObject>)

private fun group(list: List<JSONObject>): List<Fam> =
    list.groupBy { it.optString("familyId").ifEmpty { "i_" + it.optString("id") } }.map { (k, v) ->
        Fam(k, v.firstOrNull { it.optBoolean("isFamilyHead") || it.optString("relation") == "head" }, v)
    }.sortedBy { it.head?.optString("fullName") ?: it.members.first().optString("fullName") }

/** الأسر والأفراد: قائمة، تفصيل أسرة، إضافة/تعديل/حذف. التعديل محلي فوري ويُرفع في الخلفية. */
@Composable
fun PeopleScreen(auth: AuthState, data: DataRepo) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val camps = remember { data.campList(auth) }
    var camp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var persons by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    var open by remember { mutableStateOf<String?>(null) }
    var edit by remember { mutableStateOf<JSONObject?>(null) }
    var pending by remember { mutableIntStateOf(0) }
    var nidErr by remember { mutableStateOf<String?>(null) }
    var rev by remember { mutableIntStateOf(0) }
    val canWrite = auth.user.role != "beneficiary"

    LaunchedEffect(camp, rev) {
        if (camp.isEmpty()) return@LaunchedEffect
        persons = withContext(Dispatchers.IO) { data.people.persons(camp) }
        pending = withContext(Dispatchers.IO) { data.people.pendingCount(camp) }
    }

    fun save(o: JSONObject, famKey: String?) {
        scope.launch {
            o.put("updatedBy", auth.user.id)
            if (o.optString("createdBy").isEmpty()) o.put("createdBy", auth.user.id)
            val n = o.optString("nationalId")
            if (n.isNotEmpty()) {
                val bad = withContext(Dispatchers.IO) { data.checkNid(auth, n, camp) }
                if (bad != null) { nidErr = bad; return@launch }
            }
            nidErr = null
            val saved = withContext(Dispatchers.IO) { data.people.save(camp, o) }
            withContext(Dispatchers.IO) { data.afterSave(auth, camp, saved, false) }
            rev++; edit = null
            open = famKey ?: open
            withContext(Dispatchers.IO) { data.pushNow(auth, camp) }
            rev++
        }
    }

    fun remove(o: JSONObject) {
        scope.launch {
            withContext(Dispatchers.IO) { data.people.delete(camp, o); data.afterSave(auth, camp, o, true) }
            rev++
            withContext(Dispatchers.IO) { data.pushNow(auth, camp) }
            rev++
        }
    }

    val fams = remember(persons) { group(persons) }
    BackHandler(edit != null || open != null) { if (edit != null) edit = null else open = null }

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        val ed = edit
        val fk = open
        when {
            ed != null -> EditScreen(ed, persons, nidErr, { edit = null; nidErr = null }) { o -> save(o, o.optString("familyId").ifEmpty { "i_" + o.optString("id") }) }
            fk != null -> FamilyScreen(fams.firstOrNull { it.key == fk }, canWrite, { open = null }, { edit = it }, { remove(it) })
            else -> Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("الأسر والأفراد", color = p.ink, fontFamily = GrcsFonts.display, fontSize = 24.sp, modifier = Modifier.weight(1f))
                    if (pending > 0) Text("معلّق $pending", color = p.warn, fontSize = 12.sp)
                }
                if (camps.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { camps.forEach { (id, n) -> QChip(n, id == camp) { camp = id } } }
                }
                Spacer(Modifier.height(8.dp))
                QField("بحث بالاسم أو الهوية أو الجوال", q, { q = it })
                Spacer(Modifier.height(8.dp))
                val qq = normDigits(q)
                val shown = if (qq.isEmpty()) fams else fams.filter { f ->
                    f.members.any { m -> m.optString("fullName").contains(qq) || m.optString("nationalId").contains(qq) || m.optString("phone").contains(qq) }
                }
                Text("${shown.size} أسرة", color = p.mute, fontSize = 12.sp)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(shown.take(300), key = { it.key }) { f -> FamRow(f) { open = f.key } }
                    item { Spacer(Modifier.height(72.dp)) }
                }
                if (canWrite) Box(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp).heightIn(min = 52.dp).background(p.pri, RoundedCornerShape(16.dp))
                        .clickable { edit = JSONObject().put("isFamilyHead", true).put("relation", "head") },
                    contentAlignment = Alignment.Center,
                ) { Text("أسرة جديدة", color = p.onPri, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun FamRow(f: Fam, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).background(p.surface, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text((f.head ?: f.members.first()).optString("fullName"), color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text((f.head ?: f.members.first()).optString("phone").ifEmpty { "بلا جوال" }, color = p.mute, fontSize = 12.sp)
        }
        Text("${f.members.size} فرد", color = p.pri, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("رجوع", color = p.pri, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onBack).padding(12.dp))
        Text(title, color = p.ink, fontFamily = GrcsFonts.display, fontSize = 20.sp)
    }
}

@Composable
private fun FamilyScreen(f: Fam?, canWrite: Boolean, onBack: () -> Unit, onEdit: (JSONObject) -> Unit, onDelete: (JSONObject) -> Unit) {
    val p = LocalPalette.current
    var confirm by remember { mutableStateOf<JSONObject?>(null) }
    Column(Modifier.fillMaxSize()) {
        TopBar(f?.head?.optString("fullName") ?: "أسرة", onBack)
        if (f == null) { Text("لا توجد بيانات", color = p.mute, modifier = Modifier.padding(24.dp)); return }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(f.members.sortedByDescending { it.optBoolean("isFamilyHead") }, key = { it.optString("id") }) { m ->
                Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(16.dp)).padding(14.dp)) {
                    Text(m.optString("fullName"), color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    val tags = buildList {
                        if (m.optBoolean("isFamilyHead")) add("رب الأسرة") else relLabels.firstOrNull { it.first == m.optString("relation") }?.let { add(it.second) }
                        m.optString("birthDate").takeIf { it.isNotEmpty() }?.let { add(it) }
                        m.optString("nationalId").takeIf { it.isNotEmpty() }?.let { add(it) }
                        if (m.optBoolean("isPregnant")) add("حامل")
                        if (m.optBoolean("hasChronicDisease")) add("مرض مزمن")
                        if (m.optBoolean("hasDisability")) add("إعاقة")
                    }
                    Text(tags.joinToString(" · "), color = p.mute, fontSize = 12.sp)
                    if (canWrite) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("تعديل", color = p.pri, fontSize = 13.sp, modifier = Modifier.heightIn(min = 44.dp).clickable { onEdit(m) }.padding(vertical = 12.dp, horizontal = 8.dp))
                        Text("حذف", color = p.danger, fontSize = 13.sp, modifier = Modifier.heightIn(min = 44.dp).clickable { confirm = m }.padding(vertical = 12.dp, horizontal = 8.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        if (canWrite) Box(
            Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 52.dp).background(p.pri, RoundedCornerShape(16.dp)).clickable {
                val fid = f.head?.optString("familyId").orEmpty().ifEmpty { f.members.first().optString("familyId") }
                onEdit(JSONObject().put("familyId", fid).put("isFamilyHead", false).put("relation", "son"))
            },
            contentAlignment = Alignment.Center,
        ) { Text("إضافة فرد", color = p.onPri, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
    }
    confirm?.let { m ->
        Box(Modifier.fillMaxSize().background(p.ink.copy(alpha = .5f)).clickable { confirm = null }, contentAlignment = Alignment.Center) {
            Column(Modifier.padding(32.dp).background(p.surface, RoundedCornerShape(20.dp)).padding(20.dp).clickable(enabled = false) {}) {
                Text("حذف ${m.optString("fullName")}؟", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("حذف", color = p.onPri, modifier = Modifier.heightIn(min = 44.dp).background(p.danger, RoundedCornerShape(12.dp)).clickable { onDelete(m); confirm = null }.padding(horizontal = 24.dp, vertical = 12.dp))
                    Text("إلغاء", color = p.ink, modifier = Modifier.heightIn(min = 44.dp).clickable { confirm = null }.padding(horizontal = 16.dp, vertical = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun EditScreen(src: JSONObject, all: List<JSONObject>, serverErr: String?, onBack: () -> Unit, onSave: (JSONObject) -> Unit) {
    val p = LocalPalette.current
    val isNew = src.optString("id").isEmpty()
    val head = src.optBoolean("isFamilyHead")
    var name by remember { mutableStateOf(src.optString("fullName")) }
    var nid by remember { mutableStateOf(src.optString("nationalId")) }
    var dob by remember { mutableStateOf(src.optString("birthDate")) }
    var gender by remember { mutableStateOf(src.optString("gender")) }
    var phone by remember { mutableStateOf(src.optString("phone")) }
    var address by remember { mutableStateOf(src.optString("address")) }
    var notes by remember { mutableStateOf(src.optString("notes")) }
    var marital by remember { mutableStateOf(src.optString("maritalStatus")) }
    var rel by remember { mutableStateOf(src.optString("relation")) }
    var preg by remember { mutableStateOf(src.optBoolean("isPregnant")) }
    var chronic by remember { mutableStateOf(src.optBoolean("hasChronicDisease")) }
    var dis by remember { mutableStateOf(src.optBoolean("hasDisability")) }
    var err by remember { mutableStateOf<String?>(null) }

    fun validate(): String? {
        val n = normDigits(nid)
        if (name.trim().length < 2) return "أدخل الاسم"
        if (gender.isEmpty()) return "اختر الجنس"
        if (n.isNotEmpty() && !Regex("^\\d{9}$").matches(n)) return "رقم الهوية 9 أرقام"
        if (n.isNotEmpty() && all.any { it.optString("nationalId") == n && it.optString("id") != src.optString("id") }) return "رقم الهوية مسجّل لشخص آخر"
        val d = normDigits(dob)
        if (d.isNotEmpty() && !Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(d)) return "التاريخ بصيغة 1990-05-21"
        return null
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(if (isNew) (if (head) "أسرة جديدة" else "فرد جديد") else "تعديل", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            QField("الاسم الكامل", name, { name = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QChip("ذكر", gender == "male") { gender = "male" }
                QChip("أنثى", gender == "female") { gender = "female" }
            }
            QField("رقم الهوية", nid, { nid = it }, type = KeyboardType.Number)
            QField("تاريخ الميلاد (1990-05-21)", dob, { dob = it }, type = KeyboardType.Number)
            QField("الجوال", phone, { phone = it }, type = KeyboardType.Phone)
            if (head) {
                QField("العنوان", address, { address = it })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { maritalLabels.forEach { m -> QChip(m, marital == m) { marital = m } } }
            } else if (!src.optBoolean("isFamilyHead")) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { relLabels.forEach { (k, l) -> QChip(l, rel == k) { rel = k } } }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (gender == "female") QChip("حامل", preg) { preg = !preg }
                QChip("مرض مزمن", chronic) { chronic = !chronic }
                QChip("إعاقة", dis) { dis = !dis }
            }
            QField("ملاحظات", notes, { notes = it }, multiline = true)
            (err ?: serverErr)?.let { Text(it, color = p.danger, fontSize = 13.sp) }
            Spacer(Modifier.height(8.dp))
        }
        Box(
            Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 52.dp).background(p.pri, RoundedCornerShape(16.dp)).clickable {
                err = validate()
                if (err == null) {
                    val o = Merge.copy(src)
                    val n = normDigits(nid); val d = normDigits(dob)
                    o.put("fullName", name.trim()).put("nationalId", n).put("gender", gender).put("phone", phone.trim())
                        .put("birthDate", if (d.isEmpty()) JSONObject.NULL else d)
                        .put("calculatedAge", dobAge(d) ?: JSONObject.NULL)
                        .put("isPregnant", gender == "female" && preg).put("hasChronicDisease", chronic).put("hasDisability", dis)
                        .put("notes", notes.trim())
                    if (head) {
                        o.put("address", address.trim()).put("maritalStatus", marital).put("isFamilyHead", true).put("relation", "head")
                        if (o.optString("familyId").isEmpty()) o.put("familyId", n.ifEmpty { java.util.UUID.randomUUID().toString() })
                    } else if (!o.optBoolean("isFamilyHead")) o.put("relation", rel.ifEmpty { "other" })
                    if (o.optString("id").isEmpty()) o.put("id", java.util.UUID.randomUUID().toString())
                    onSave(o)
                }
            },
            contentAlignment = Alignment.Center,
        ) { Text("حفظ", color = p.onPri, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
    }
}

private fun dobAge(d: String): Int? {
    val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").find(d) ?: return null
    val t = java.util.Calendar.getInstance()
    var a = t.get(java.util.Calendar.YEAR) - m.groupValues[1].toInt()
    val dm = t.get(java.util.Calendar.MONTH) + 1 - m.groupValues[2].toInt()
    if (dm < 0 || (dm == 0 && t.get(java.util.Calendar.DAY_OF_MONTH) < m.groupValues[3].toInt())) a--
    return a.coerceAtLeast(0)
}
