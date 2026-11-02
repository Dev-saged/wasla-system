package dev.saged.grcs.ui.camps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.CampSummary
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette

/** المخيمات: ملخص كل مخيم من آخر نسخة محلية. */
@Composable
fun CampsScreen(auth: AuthState, data: DataRepo) {
    val p = LocalPalette.current
    val camps = remember { data.cached(auth).sortedByDescending { it.stats.total } }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        Text("المخيمات", color = p.ink, fontFamily = GrcsFonts.display, fontSize = 24.sp)
        if (camps.isEmpty()) Text("لا بيانات بعد — افتح الرئيسية للمزامنة", color = p.mute, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            items(camps, key = { it.id }) { Card(it) }
        }
    }
}

@Composable
private fun Card(c: CampSummary) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(c.name, color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("${c.stats.families} أسرة · ${c.stats.total} فرد", color = p.pri, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("أطفال ${c.stats.child}", color = p.mute, fontSize = 12.sp)
            Text("كبار ${c.stats.elderly}", color = p.mute, fontSize = 12.sp)
            Text("حوامل ${c.stats.pregnant}", color = p.mute, fontSize = 12.sp)
            Text("إعاقة ${c.stats.disability}", color = p.mute, fontSize = 12.sp)
        }
    }
}
