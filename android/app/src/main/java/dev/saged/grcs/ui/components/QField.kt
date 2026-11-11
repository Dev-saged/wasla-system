package dev.saged.grcs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette

/** حقل نص موحّد: تسمية صغيرة أعلى القيمة، إطار يتلوّن عند التركيز، ارتفاع لمس لا يقل عن 56dp. */
@Composable
fun QField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, type: KeyboardType = KeyboardType.Text, multiline: Boolean = false, enabled: Boolean = true, placeholder: String = "", icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    val p = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Column(modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) Text(label, color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth().heightIn(min = if (multiline) 96.dp else 48.dp).background(if (focused) p.pri.copy(alpha = .18f) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(13.dp)).padding(if (focused) 0.dp else 0.dp)
                .background(if (enabled) p.surface else p.surface2, shape).border(1.5.dp, if (focused) p.pri else p.line, shape).padding(horizontal = 14.dp, vertical = if (multiline) 12.dp else 0.dp),
            verticalAlignment = if (multiline) androidx.compose.ui.Alignment.Top else androidx.compose.ui.Alignment.CenterVertically,
        ) {
            if (icon != null) androidx.compose.material3.Icon(icon, null, Modifier.padding(end = 12.dp).size(15.dp), tint = p.mute)
            Box(Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, color = p.mute, fontSize = 16.sp, maxLines = 1)
                BasicTextField(
                    value, onChange, singleLine = !multiline, enabled = enabled,
                    textStyle = TextStyle(color = p.ink, fontSize = 16.sp, fontFamily = GrcsFonts.body, lineHeight = 1.6.em, lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.None)),
                    cursorBrush = SolidColor(p.pri),
                    keyboardOptions = KeyboardOptions(keyboardType = type),
                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                )
            }
        }
    }
}

/** قائمة اختيار بمظهر select في الويب: 48 ارتفاعاً، حد 1.5، وسهم صغير. */
@Composable
fun <T> QSelect(value: T, options: List<Pair<T, String>>, modifier: Modifier = Modifier, label: String = "", onSelect: (T) -> Unit) {
    val p = LocalPalette.current
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Column(modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) Text(label, color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
        Box {
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).background(p.surface, shape).border(1.5.dp, p.line, shape).clickable { open = true }.padding(horizontal = 14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(options.firstOrNull { it.first == value }?.second.orEmpty(), color = p.ink, fontSize = 16.sp, maxLines = 1, modifier = Modifier.weight(1f))
                androidx.compose.material3.Icon(dev.saged.grcs.ui.icons.Fa.ChevronDown, null, Modifier.size(11.dp), tint = p.ink)
            }
            androidx.compose.material3.DropdownMenu(open, { open = false }) {
                options.forEach { (k, l) -> androidx.compose.material3.DropdownMenuItem({ Text(l, fontSize = 15.sp) }, { onSelect(k); open = false }) }
            }
        }
    }
}

/** عنوان مجموعة تصفية (filter-group label): أيقونة ونص 14 عريض كحلي. */
@Composable
fun FilterLabel(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    val p = LocalPalette.current
    androidx.compose.foundation.layout.Row(Modifier.padding(bottom = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (icon != null) androidx.compose.material3.Icon(icon, null, Modifier.size(14.dp), tint = p.head)
        Text(text, color = p.head, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/** تسمية حقل بأيقونة صغيرة (field label). */
@Composable
fun FilterLabelPlain(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val p = LocalPalette.current
    androidx.compose.foundation.layout.Row(Modifier.padding(bottom = 6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        androidx.compose.material3.Icon(icon, null, Modifier.size(13.dp), tint = p.ink)
        Text(text, color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** صف خانة اختيار (check-row): مربع 20 ونص 14. */
@Composable
fun CheckRow(text: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    val sh = RoundedCornerShape(4.dp)
    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().clickable { onChange(!on) }.padding(horizontal = 2.dp, vertical = 9.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(20.dp).background(if (on) p.pri else p.surface, sh).border(1.5.dp, if (on) p.pri else p.mute, sh), contentAlignment = androidx.compose.ui.Alignment.Center) { if (on) androidx.compose.material3.Icon(dev.saged.grcs.ui.icons.Fa.Check, null, Modifier.size(12.dp), tint = androidx.compose.ui.graphics.Color.White) }
        Text(text, color = p.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

/** شريحة اختيار (للجنس، الصلة، الأعلام). */
@Composable
fun QChip(text: String, on: Boolean, activeColor: androidx.compose.ui.graphics.Color? = null, activeText: androidx.compose.ui.graphics.Color? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(50)
    val bg = if (on) activeColor ?: p.navy else p.surface
    Box(
        Modifier.background(bg, shape).border(1.5.dp, if (on) bg else p.line, shape)
            .clickable(onClick = onClick).heightIn(min = 44.dp).padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = androidx.compose.ui.Alignment.Center,
    ) { Text(text, color = if (on) activeText ?: androidx.compose.ui.graphics.Color.White else p.mute, fontSize = 13.5.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, maxLines = 1) }
}

/** حقل تاريخ بمنتقي أصلي (input type=date في الويب): الصيغة yyyy-MM-dd. */
@Composable
fun DateField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Box(modifier.clickable {
        val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").find(value)
        val c = java.util.Calendar.getInstance()
        val y = m?.groupValues?.get(1)?.toInt() ?: (c.get(java.util.Calendar.YEAR) - 20)
        val mo = (m?.groupValues?.get(2)?.toInt() ?: (c.get(java.util.Calendar.MONTH) + 1)) - 1
        val d = m?.groupValues?.get(3)?.toInt() ?: c.get(java.util.Calendar.DAY_OF_MONTH)
        android.app.DatePickerDialog(ctx, { _, yy, mm, dd -> onChange("%04d-%02d-%02d".format(yy, mm + 1, dd)) }, y, mo, d).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
    }) {
        val p = LocalPalette.current
        val shape = RoundedCornerShape(10.dp)
        val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").find(value)
        Column(Modifier.fillMaxWidth()) {
            if (label.isNotEmpty()) Text(label, color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).background(p.surface, shape).border(1.5.dp, p.line, shape).padding(horizontal = 14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(if (m != null) "${m.groupValues[3]}/${m.groupValues[2]}/${m.groupValues[1]}" else "يوم/شهر/سنة", color = if (m != null) p.ink else p.mute, fontSize = 16.sp, modifier = Modifier.weight(1f))
                androidx.compose.material3.Icon(dev.saged.grcs.ui.icons.Fa.CalendarDays, null, Modifier.size(15.dp), tint = p.ink)
            }
        }
    }
}
