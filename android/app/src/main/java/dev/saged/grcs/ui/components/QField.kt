package dev.saged.grcs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Box
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
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette

/** حقل نص موحّد: تسمية صغيرة أعلى القيمة، إطار يتلوّن عند التركيز، ارتفاع لمس لا يقل عن 56dp. */
@Composable
fun QField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, type: KeyboardType = KeyboardType.Text, multiline: Boolean = false, enabled: Boolean = true) {
    val p = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label.isNotEmpty()) Text(label, color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier.fillMaxWidth().heightIn(min = if (multiline) 96.dp else 52.dp).background(if (enabled) p.surface else p.bg, shape)
                .border(if (focused) 2.dp else 1.dp, if (focused) p.pri else p.line, shape).padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                value, onChange, singleLine = !multiline, enabled = enabled,
                textStyle = TextStyle(color = p.ink, fontSize = 16.sp, fontFamily = GrcsFonts.body),
                cursorBrush = SolidColor(p.pri),
                keyboardOptions = KeyboardOptions(keyboardType = type),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            )
        }
    }
}

/** شريحة اختيار (للجنس، الصلة، الأعلام). */
@Composable
fun QChip(text: String, on: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.heightIn(min = 44.dp).background(if (on) p.pri else p.surface, shape).border(1.dp, if (on) p.pri else p.line, shape)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
    ) { Text(text, color = if (on) p.onPri else p.ink, fontSize = 14.sp) }
}
