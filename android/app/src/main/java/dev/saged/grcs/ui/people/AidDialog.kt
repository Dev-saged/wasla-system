package dev.saged.grcs.ui.people

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.data.Aid
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.components.QField
import dev.saged.grcs.ui.theme.LocalPalette
import org.json.JSONObject
import java.util.Locale

/** تسجيل مساعدة لفرد مع عرض سجله والتراجع (علامة void). */
@Composable
fun AidDialog(person: JSONObject, uid: String, onClose: () -> Unit, onSave: (JSONObject) -> Unit) {
    val p = LocalPalette.current
    var type by remember { mutableStateOf(Aid.types.firstOrNull { Aid.eligible(person, it.k) }?.k ?: "other") }
    var qty by remember { mutableStateOf("1") }
    var note by remember { mutableStateOf("") }
    val t = Aid.types.first { it.k == type }
    dev.saged.grcs.ui.components.Sheet(onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("مساعدة — " + person.optString("fullName"), color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Aid.types.filter { Aid.eligible(person, it.k) }.forEach { QChip(it.label, it.k == type) { type = it.k } }
            }
            QField(t.ask ?: "الكمية", qty, { qty = it }, type = KeyboardType.Decimal)
            QField("ملاحظة", note, { note = it })
            Box(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).background(p.pri, RoundedCornerShape(16.dp)).clickable {
                    val q = qty.replace(',', '.').toDoubleOrNull() ?: 1.0
                    onSave(Aid.give(person, type, q, note, uid))
                },
                contentAlignment = Alignment.Center,
            ) { Text("تسجيل", color = p.onPri, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            val past = Aid.list(person).sortedByDescending { it.optLong("at") }.take(8)
            if (past.isNotEmpty()) Text("السجل", color = p.mute, fontSize = 12.sp)
            past.forEach { r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text((Aid.types.firstOrNull { it.k == r.optString("t") }?.label ?: r.optString("t")) + " · " + r.optString("d") + " · " + String.format(Locale.US, "%s", r.opt("q")), color = p.ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("تراجع", color = p.danger, fontSize = 12.sp, modifier = Modifier.heightIn(min = 44.dp).clickable { onSave(Aid.undo(person, r.optString("id"))) }.padding(10.dp))
                }
            }
            Spacer(Modifier.height(2.dp))
            Text("إغلاق", color = p.pri, fontSize = 14.sp, modifier = Modifier.heightIn(min = 44.dp).clickable(onClick = onClose).padding(vertical = 12.dp))
        }
    }
}
