package dev.saged.grcs.ui.more

import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.qcard
import dev.saged.grcs.ui.components.QAct
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.Cond
import dev.saged.grcs.data.Query
import dev.saged.grcs.data.Saved
import dev.saged.grcs.data.QResult
import dev.saged.grcs.ui.components.QField
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.launch
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.theme.GrcsFonts
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** منشئ الاستعلامات — نقل مباشر لـqbRender / qbRun / qbRenderPresets / qbSave / qbExport. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun QueryPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    fun toast(t: String) = android.widget.Toast.makeText(ctx, t, android.widget.Toast.LENGTH_LONG).show()
    val camps = remember { data.ctxCamps(auth).also { Query.camps = it } }
    val persons = remember { camps.flatMap { c -> data.people.persons(c.first).onEach { if (it.optString("campId").isEmpty()) it.put("campId", c.first) } } }
    var groups by remember { mutableStateOf(listOf(listOf(Cond("gender", "female")))) }
    var result by remember { mutableStateOf<QResult?>(null) }
    var saved by remember { mutableStateOf(data.savedQueries()) }
    var naming by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    var delSaved by remember { mutableStateOf<Saved?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(groups) { kotlinx.coroutines.delay(140); result = withContext(Dispatchers.Default) { Query.run(persons, groups) } }
    fun setCond(gi: Int, ci: Int, c: Cond?) {
        groups = groups.mapIndexed { i, g -> if (i != gi) g else if (c == null) g.filterIndexed { j, _ -> j != ci } else g.mapIndexed { j, x -> if (j == ci) c else x } }
            .let { gs -> if (c == null && gs[gi].isEmpty() && gs.size > 1) gs.filterIndexed { i, _ -> i != gi } else gs }
    }
    Column(Modifier.fillMaxSize()) {
        AppHeader("منشئ الاستعلامات", if (camps.size > 1) "كل المخيمات" else camps.firstOrNull()?.second.orEmpty(), conn = null, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Query.presets.forEach { c -> QChip(c.label, false) { groups = c.groups } }
                saved.forEach { sv ->
                    Row(Modifier.clip(RoundedCornerShape(50)).background(p.pri.copy(alpha = .1f)).combinedClickable(onClick = { groups = sv.groups }, onLongClick = { delSaved = sv }).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        androidx.compose.material3.Icon(dev.saged.grcs.ui.icons.Fa.Bookmark, null, Modifier.size(12.dp), tint = p.pri)
                        Text(sv.name, color = p.pri, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            groups.forEachIndexed { gi, g ->
                if (gi > 0) Text("أو", Modifier.fillMaxWidth(), color = p.mute, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Column(Modifier.fillMaxWidth().qcard().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    g.forEachIndexed { ci, c ->
                        val f = Query.fields.firstOrNull { it.k == c.k } ?: Query.fields[1]
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                dev.saged.grcs.ui.components.QSelect(f.k, Query.fields.map { it.k to it.label }) { k ->
                                    val nf = Query.fields.first { it.k == k }
                                    setCond(gi, ci, if (nf.range) Cond(k) else Cond(k, Query.optionsOf(nf).firstOrNull()?.first.orEmpty()))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (f.range) {
                                        QField("", c.min, { setCond(gi, ci, Cond(c.k, min = dev.saged.grcs.data.Nid.digits(it).filter(Char::isDigit), max = c.max)) }, Modifier.weight(1f), placeholder = "من", type = androidx.compose.ui.text.input.KeyboardType.Number)
                                        QField("", c.max, { setCond(gi, ci, Cond(c.k, min = c.min, max = dev.saged.grcs.data.Nid.digits(it).filter(Char::isDigit))) }, Modifier.weight(1f), placeholder = "إلى", type = androidx.compose.ui.text.input.KeyboardType.Number)
                                    } else {
                                        Box(Modifier.weight(.6f)) { dev.saged.grcs.ui.components.QSelect(if (c.not) "not" else "is", listOf("is" to "هو", "not" to "ليس")) { setCond(gi, ci, Cond(c.k, c.v, not = it == "not")) } }
                                        Box(Modifier.weight(1f)) { dev.saged.grcs.ui.components.QSelect(c.v, Query.optionsOf(f)) { setCond(gi, ci, Cond(c.k, it, not = c.not)) } }
                                    }
                                }
                            }
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).clickable { setCond(gi, ci, null) }, contentAlignment = Alignment.Center) { androidx.compose.material3.Icon(dev.saged.grcs.ui.icons.Fa.Xmark, "حذف الشرط", Modifier.size(14.dp), tint = p.mute) }
                        }
                    }
                    dev.saged.grcs.ui.components.Btn("شرط (و)", { groups = groups.mapIndexed { i, x -> if (i == gi) x + Cond("age") else x } }, kind = dev.saged.grcs.ui.components.BtnKind.Ghost, icon = dev.saged.grcs.ui.icons.Fa.Plus, small = true)
                }
            }
            dev.saged.grcs.ui.components.Btn("مجموعة بديلة (أو)", { groups = groups + listOf(listOf(Cond("gender", "female"))) }, Modifier.fillMaxWidth(), kind = dev.saged.grcs.ui.components.BtnKind.Outline, icon = dev.saged.grcs.ui.icons.Fa.Plus, small = true)
            val r = result
            val has = groups.any { it.isNotEmpty() }
            Row(Modifier.fillMaxWidth().background(p.navy, RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${r?.persons?.size ?: 0}", color = androidx.compose.ui.graphics.Color.White, fontFamily = GrcsFonts.display, fontSize = 28.sp)
                Text("فرد مطابق · ${r?.families ?: 0} أسرة", color = androidx.compose.ui.graphics.Color.White.copy(alpha = .8f), fontSize = 13.sp, modifier = Modifier.weight(1f))
                dev.saged.grcs.ui.components.Btn("تصدير", {
                    scope.launch {
                        busy = true
                        val e = withContext(Dispatchers.IO) {
                            val out = data.export.run("xlsx", camps.first().first, "استعلام", r!!.persons, false)
                            camps.firstOrNull()?.let { dev.saged.grcs.data.Audit.log(auth, it.first, "export", "HQ", "استعلام مخصص", org.json.JSONObject().put("type", "xlsx-query").put("count", r.persons.size).toString()) }
                            out
                        }
                        busy = false; toast(if (e.ok) "تم التصدير: ${e.name}" else "تعذّر التصدير")
                    }
                }, icon = dev.saged.grcs.ui.icons.Fa.FileExcel, small = true, enabled = !busy && r != null && r.persons.isNotEmpty())
                dev.saged.grcs.ui.components.Btn("حفظ", { saveName = ""; naming = true }, kind = dev.saged.grcs.ui.components.BtnKind.Outline, icon = dev.saged.grcs.ui.icons.Fa.Bookmark, small = true, enabled = has)
            }
            r?.persons?.take(60)?.forEach { x ->
                val fem = x.optString("gender") == "female"
                Row(Modifier.fillMaxWidth().qcard().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(40.dp).background(if (fem) p.pregnant else p.navy, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { androidx.compose.material3.Icon(if (fem) dev.saged.grcs.ui.icons.Fa.Venus else dev.saged.grcs.ui.icons.Fa.Mars, null, Modifier.size(16.dp), tint = androidx.compose.ui.graphics.Color.White) }
                    Column(Modifier.weight(1f)) {
                        Text(x.optString("fullName"), color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(dev.saged.grcs.data.Rel.label(x) + (dev.saged.grcs.data.Stats.ageOf(x)?.let { " · $it سنة" } ?: "") + " · " + x.optString("nationalId").ifEmpty { "بلا هوية" }, color = p.mute, fontSize = 12.sp)
                        if (camps.size > 1) Text(camps.firstOrNull { it.first == x.optString("campId") }?.second.orEmpty(), color = p.mute, fontSize = 12.sp)
                    }
                }
            }
            if ((r?.persons?.size ?: 0) > 60) Text("يظهر أول 60 — التصدير يشمل الكل", Modifier.fillMaxWidth(), color = p.mute, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
    if (naming) dev.saged.grcs.ui.components.Sheet({ naming = false }, "اسم الاستعلام") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            QField("", saveName, { saveName = it }, placeholder = "مثال: مسنات مريضات")
            dev.saged.grcs.ui.components.Btn("حفظ", {
                val n = saveName.trim().take(40)
                if (n.isNotEmpty()) { saved = (listOf(Saved(n, groups.filter { it.isNotEmpty() })) + saved.filter { it.name != n }).take(20); data.saveQueries(saved); naming = false; toast("تم حفظ الاستعلام — اضغط مطولاً عليه لحذفه") }
            }, Modifier.fillMaxWidth())
        }
    }
    delSaved?.let { sv -> dev.saged.grcs.ui.components.ConfirmSheet("حذف هذا الاستعلام المحفوظ؟", "حذف", "حذف", onCancel = { delSaved = null }) { saved = saved.filter { it !== sv }; data.saveQueries(saved); delSaved = null } }
}
