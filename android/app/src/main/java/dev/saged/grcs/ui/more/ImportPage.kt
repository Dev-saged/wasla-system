package dev.saged.grcs.ui.more

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.ImportPreview
import dev.saged.grcs.data.Importer
import dev.saged.grcs.data.Xlsx
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** استيراد كشف Excel/CSV: معاينة أولاً ثم إضافة فقط (لا تعديل ولا حذف لسجلات موجودة). */
@Composable
fun ImportPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val camps = remember { data.campList(auth) }
    var camp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var prev by remember { mutableStateOf<ImportPreview?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            msg = null; prev = null; busy = true
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val name = uri.toString().lowercase()
                    val rows = ctx.contentResolver.openInputStream(uri)!!.use { s ->
                        if (name.endsWith(".csv") || ctx.contentResolver.getType(uri)?.contains("csv") == true) Xlsx.readCsv(s.bufferedReader(Charsets.UTF_8).readText()) else Xlsx.read(s)
                    }
                    Importer.preview(rows, camp, data.people.persons(camp), auth.user.id)
                }.getOrNull()
            }
            busy = false
            if (r == null) msg = "تعذّرت قراءة الملف — استخدم xlsx أو csv" else prev = r
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("رجوع", color = p.pri, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onBack).padding(12.dp))
            Text("استيراد كشف", color = p.ink, fontFamily = GrcsFonts.display, fontSize = 20.sp)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (camps.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { camps.forEach { (id, n) -> QChip(n, id == camp) { camp = id; prev = null } } }
            Text("صف لكل شخص. أعمدة معروفة: الاسم، رقم الهوية، الجوال، تاريخ الميلاد، الجنس، الحالة الاجتماعية، هوية رب الأسرة (للأفراد التابعين).", color = p.mute, fontSize = 12.sp)
            Box(Modifier.fillMaxWidth().heightIn(min = 52.dp).background(p.surface2, RoundedCornerShape(16.dp)).clickable { pick.launch(arrayOf("*/*")) }, contentAlignment = Alignment.Center) {
                Text(if (busy) "جارٍ القراءة…" else "اختيار ملف", color = p.pri, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            prev?.let { r ->
                Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(16.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("سيُضاف ${r.toAdd.size} فرداً (${r.headCount} أسرة/رب أسرة)", color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("مكرر بالهوية (سيُتخطى): ${r.skippedExisting} · صفوف غير صالحة: ${r.skippedInvalid}", color = p.mute, fontSize = 12.sp)
                    r.mapped.forEach { Text(it, color = p.mute, fontSize = 11.sp) }
                }
                if (r.toAdd.isNotEmpty()) Box(Modifier.fillMaxWidth().heightIn(min = 52.dp).background(p.pri, RoundedCornerShape(16.dp)).clickable(enabled = !busy) {
                    scope.launch {
                        busy = true
                        withContext(Dispatchers.IO) {
                            r.toAdd.forEach { data.people.save(camp, it) }
                            r.toAdd.filter { it.optBoolean("isFamilyHead") }.forEach { data.afterSave(auth, camp, it, false) }
                            data.pushNow(auth, camp)
                        }
                        msg = "تمت إضافة ${r.toAdd.size} فرداً"; prev = null; busy = false
                    }
                }, contentAlignment = Alignment.Center) { Text("تأكيد الإضافة", color = p.onPri, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
            msg?.let { Text(it, color = p.ok, fontSize = 13.sp) }
        }
    }
}
