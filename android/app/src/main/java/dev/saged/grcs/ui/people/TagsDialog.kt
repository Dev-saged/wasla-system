package dev.saged.grcs.ui.people

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.data.Merge
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.Pill
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.components.QField
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.theme.LocalPalette
import org.json.JSONArray
import org.json.JSONObject

private fun JSONObject.tagList(): List<String> = optJSONArray("tags")?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()

/** محرر التصنيفات (openTagsEditor): يتيم وبياناته، جريح، أسرة شهيد، وتصنيفات مخصصة. */
@Composable
internal fun TagsDialog(person: JSONObject, onClose: () -> Unit, onSave: (JSONObject) -> Unit) {
    val p = LocalPalette.current
    val tags = remember { person.tagList() }
    val o = remember { person.optJSONObject("tagData")?.optJSONObject("orphan") ?: JSONObject() }
    var orphan by remember { mutableStateOf("orphan" in tags) }
    var otype by remember { mutableStateOf(if (o.optString("type") == "both") "both" else "father") }
    var sole by remember { mutableStateOf(o.optBoolean("soleSurvivor")) }
    var gName by remember { mutableStateOf(o.optString("guardianName")) }
    var gNid by remember { mutableStateOf(o.optString("guardianNid")) }
    var gRel by remember { mutableStateOf(o.optString("guardianRel")) }
    var gPhone by remember { mutableStateOf(o.optString("guardianPhone")) }
    var injured by remember { mutableStateOf("injured" in tags) }
    var martyr by remember { mutableStateOf("martyr_family" in tags) }
    val custom = remember { mutableStateListOf<String>().also { it.addAll(tags.filter { t -> t.startsWith("c:") }) } }
    var newTag by remember { mutableStateOf("") }
    @Composable fun Line(label: String, on: Boolean, ch: (Boolean) -> Unit) = Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); Switch(on, ch)
    }
    dev.saged.grcs.ui.components.Sheet(onClose, "تصنيفات ${person.optString("fullName")}") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Line("يتيم", orphan) { orphan = it }
            if (orphan) {
                Text("نوع اليتم", color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QChip("يتيم الأب", otype == "father") { otype = "father" }; QChip("يتيم الأبوين", otype == "both") { otype = "both" } }
                Line("الناجي الوحيد من أسرته", sole) { sole = it; if (it) otype = "both" }
                QField("اسم الوصي", gName, { gName = it })
                QField("هوية الوصي", gNid, { gNid = it.filter(Char::isDigit).take(12) }, type = KeyboardType.Number)
                QField("صلة الوصي بالطفل", gRel, { gRel = it.take(30) })
                QField("جوال الوصي", gPhone, { gPhone = it.filter { c -> c.isDigit() || c == '+' }.take(15) }, type = KeyboardType.Phone)
            }
            Line("جريح/مصاب", injured) { injured = it }
            Line("أسرة شهيد", martyr) { martyr = it }
            Text("تصنيفات مخصصة", color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (custom.isEmpty()) Text("لا توجد تصنيفات مخصصة", color = p.mute, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { custom.toList().forEach { t -> Box(Modifier.clickable { custom.remove(t) }) { Pill(t.drop(2) + "  ×", p.pri) } } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                QField("", newTag, { newTag = it.take(40) }, Modifier.weight(1f))
                Btn("إضافة", { val v = newTag.trim().replace(Regex("\\s+"), " "); if (v.isNotEmpty() && "c:$v" !in custom) custom.add("c:$v"); newTag = "" }, kind = BtnKind.Outline, small = true)
            }
            Btn("حفظ", {
                val out = Merge.copy(person)
                val td = out.optJSONObject("tagData") ?: JSONObject()
                val nt = ArrayList<String>()
                if (orphan) {
                    nt.add("orphan")
                    td.put("orphan", JSONObject().put("type", otype).apply { if (sole) put("soleSurvivor", true) }
                        .put("guardianName", gName.trim().take(80)).put("guardianNid", gNid).put("guardianRel", gRel.trim()).put("guardianPhone", gPhone))
                } else td.remove("orphan")
                if (injured) nt.add("injured") else td.remove("injured")
                if (martyr) nt.add("martyr_family") else td.remove("martyr_family")
                nt.addAll(custom)
                td.keys().asSequence().toList().forEach { k -> if (k.startsWith("c:") && k !in custom) td.remove(k) }
                out.put("tags", JSONArray(nt)).put("tagData", td)
                onSave(out)
            }, Modifier.fillMaxWidth(), icon = QIcons.Check)
        }
    }
}
