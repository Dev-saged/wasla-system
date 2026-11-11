package dev.saged.grcs.ui.more

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.ExportResult
import dev.saged.grcs.data.PFilter
import dev.saged.grcs.data.Stats
import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.components.QField
import dev.saged.grcs.ui.components.SectionTitle
import dev.saged.grcs.ui.icons.Fa
import dev.saged.grcs.ui.components.FilterLabel
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** تصدير تقرير مخصص كما في الويب: حسب الأسرة أو حسب الفرد بتصفية مفصّلة، أسرة محددة، عدّاد، Excel وCSV وطباعة وملفات محفوظة. */
@Composable
fun ExportPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val camps = remember { data.campList(auth) }
    var camp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var mode by remember { mutableStateOf("family") }
    var gender by remember { mutableStateOf("all") }
    var age by remember { mutableStateOf("all") }
    var ageMin by remember { mutableStateOf("") }
    var ageMax by remember { mutableStateOf("") }
    val flags = remember { mutableStateListOf<String>() }
    var family by remember { mutableStateOf("all") }
    var famMenu by remember { mutableStateOf(false) }
    var persons by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var res by remember { mutableStateOf<ExportResult?>(null) }
    val saved = remember { mutableStateListOf<Pair<String, android.net.Uri?>>() }

    LaunchedEffect(camp) {
        persons = withContext(Dispatchers.IO) { if (camp == "*") camps.flatMap { data.people.persons(it.first) } else data.people.persons(camp) }
        family = "all"
    }
    val heads = remember(persons) { persons.filter { it.optBoolean("isFamilyHead") }.sortedBy { it.optString("fullName") } }
    fun famKey(m: JSONObject) = m.optString("familyId").ifEmpty { "i_" + m.optString("id") }

    fun match(m: JSONObject): Boolean {
        if (gender != "all" && m.optString("gender") != gender) return false
        val a = Stats.ageOf(m)
        when (age) {
            "infant" -> if (a == null || a >= 1) return false
            "preschool" -> if (a == null || a !in 1..5) return false
            "school" -> if (a == null || a !in 6..18) return false
            "youth" -> if (a == null || a !in 18..30) return false
            "midAdult" -> if (a == null || a !in 30..60) return false
            "elderly" -> if (a == null || a < 60) return false
            "custom" -> { val lo = ageMin.toIntOrNull() ?: 0; val hi = ageMax.toIntOrNull() ?: 200; if (a == null || a < lo || a > hi) return false }
        }
        for (f in flags) if (PFilter.apply(f, listOf(m)).isEmpty()) return false
        return true
    }
    val selected = remember(persons, mode, gender, age, ageMin, ageMax, flags.toList(), family) {
        val base = if (family != "all") persons.filter { famKey(it) == family } else persons
        if (mode == "individual") base.filter { match(it) } else base
    }
    val count = if (mode == "family") selected.map { famKey(it) }.toSet().size else selected.size

    fun doExport(kind: String) {
        if (busy) return
        busy = true
        scope.launch {
            val cn = if (camp == "*") "كل المخيمات" else camps.firstOrNull { it.first == camp }?.second ?: camp
            val r = withContext(Dispatchers.IO) {
                if (mode == "family") data.export.runRows(kind, "$cn-أسر", cn, data.export.familyRows(selected), data.export.familyHeaders)
                else {
                    val cols = data.export.columns.filter { it.k in listOf("name", "nid", "dob", "age", "gender", "rel", "phone", "phone2", "addr", "marital", "shelter", "preg", "bf", "chronic", "dis") }
                    data.export.runRows(kind, "$cn-أفراد", cn, selected.map { m -> cols.map { it.get(m) } }, cols.map { it.label })
                }
            }
            res = r; if (r.ok) saved.add(0, r.name to r.uri); busy = false
        }
    }
    fun share(name: String, uri: android.net.Uri?) {
        if (uri == null) return
        val mime = if (name.endsWith(".xlsx")) "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" else if (name.endsWith(".csv")) "text/csv" else "application/pdf"
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "مشاركة"))
    }

    Column(Modifier.fillMaxSize()) {
        AppHeader("تصدير تقرير مخصص", "", conn = null, mark = dev.saged.grcs.ui.icons.Fa.FileExport, endBack = onBack.takeIf { auth.user.role == "admin" })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (camps.size > 1) {
                FilterLabel("المخيم", Fa.Tent)
                FlowRow(Modifier.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { QChip("كل المخيمات", camp == "*") { camp = "*" }; camps.forEach { (id, n) -> QChip(n, id == camp) { camp = id } } }
            }
            FilterLabel("طريقة العرض", Fa.TableList)
            androidx.compose.foundation.lazy.LazyRow(Modifier.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { QChip("حسب الأسرة (صف واحد لكل أسرة)", mode == "family") { mode = "family" } }
                item { QChip("حسب الفرد (تصفية مفصّلة)", mode == "individual") { mode = "individual" } }
            }
            if (mode == "individual") {
                FilterLabel("الجنس", Fa.VenusMars)
                dev.saged.grcs.ui.components.QSelect(gender, listOf("all" to "الكل", "male" to "ذكور", "female" to "إناث"), Modifier.padding(bottom = 14.dp)) { gender = it }
                FilterLabel("الفئة العمرية", Fa.CakeCandles)
                dev.saged.grcs.ui.components.QSelect(age, listOf("all" to "الكل", "infant" to "أقل من سنة", "preschool" to "1-5 سنوات", "school" to "6-18 سنة", "youth" to "شباب 18-30", "midAdult" to "بالغون 30-60", "elderly" to "كبار السن +60", "custom" to "نطاق مخصص"), Modifier.padding(bottom = if (age == "custom") 8.dp else 14.dp)) { age = it }
                if (age == "custom") Row(Modifier.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QField("", ageMin, { ageMin = it.filter { c -> c.isDigit() } }, Modifier.weight(1f), KeyboardType.Number, placeholder = "من")
                    QField("", ageMax, { ageMax = it.filter { c -> c.isDigit() } }, Modifier.weight(1f), KeyboardType.Number, placeholder = "إلى")
                }
                Column(Modifier.padding(bottom = 14.dp)) { listOf("pregnant" to "الحوامل فقط", "chronic" to "المرضى المزمنون فقط", "disability" to "ذوو الإعاقة فقط", "widowed" to "الأرامل فقط", "divorced" to "المطلّقات/ين فقط", "nophone" to "بدون هاتف", "noid" to "بدون رقم هوية").forEach { (k, l) -> dev.saged.grcs.ui.components.CheckRow(l, k in flags) { if (it) flags.add(k) else flags.remove(k) } } }
            }
            FilterLabel("أسرة محددة", Fa.House)
            dev.saged.grcs.ui.components.QSelect(family, listOf("all" to "كل الأسر") + heads.take(300).map { famKey(it) to it.optString("fullName") }) { family = it }
            Row(Modifier.padding(vertical = 14.dp).fillMaxWidth().background(p.navy, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("سيتم تصدير ", color = androidx.compose.ui.graphics.Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(if (mode == "family") "$count أسرة" else "$count", color = androidx.compose.ui.graphics.Color(0xFF7DD3E8), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(" سجل", color = androidx.compose.ui.graphics.Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn("Excel", { doExport("xlsx") }, Modifier.weight(1f), BtnKind.Primary, Fa.FileExcel, enabled = !busy)
                Btn("CSV", { doExport("csv") }, Modifier.weight(1f), BtnKind.Outline, Fa.FileCsv, enabled = !busy)
            }
            Btn("طباعة", { doExport("pdf") }, Modifier.fillMaxWidth().padding(top = 8.dp), BtnKind.Navy, Fa.Print, enabled = !busy)
            res?.let { if (!it.ok) Text("تعذّر الحفظ — تحقق من إذن التخزين", color = p.danger, fontSize = 13.sp) }
            SectionTitle("ملفاتي المحفوظة على الجهاز")
            if (saved.isEmpty()) Text("لا توجد ملفات محفوظة على هذا الجهاز بعد", color = p.mute, fontSize = 13.sp)
            saved.forEach { (n, u) ->
                Row(Modifier.padding(bottom = 8.dp).fillMaxWidth().heightIn(min = 48.dp).background(p.surface, RoundedCornerShape(14.dp)).clickable { share(n, u) }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(n, color = p.ink, fontSize = 13.sp, modifier = Modifier.weight(1f)); Text("مشاركة", color = p.pri, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
                    }
    }
}
