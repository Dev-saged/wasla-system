package dev.saged.grcs.ui.more

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.Importer
import dev.saged.grcs.data.Xlsx
import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.QCard
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private class Match(val camp: String, val person: JSONObject, val patch: Map<String, String>)

/** مطابقة كشف وطني شامل: تُطابَق الهويات مع كل المخيمات وتُكمَّل الحقول الفارغة فقط (الجوال، الميلاد، الجنس) دون استبدال أي قيمة موجودة. */
@Composable
fun ReconcilePage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var matches by remember { mutableStateOf<List<Match>?>(null) }
    var unmatched by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var header by remember { mutableStateOf<List<String>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var msg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true; msg = null; matches = null
            val rows = withContext(Dispatchers.IO) {
                runCatching {
                    val name = uri.toString().lowercase()
                    ctx.contentResolver.openInputStream(uri)!!.use { s -> if (name.endsWith(".csv")) Xlsx.readCsv(s.bufferedReader(Charsets.UTF_8).readText()) else Xlsx.read(s) }
                }.getOrNull()
            }
            if (rows == null || rows.size < 2) { msg = "تعذّرت قراءة الملف — استخدم xlsx أو csv"; busy = false; return@launch }
            val map = Importer.auto(rows[0])
            val nidI = map["nid"]
            if (nidI == null) { msg = "لم يُعثر على عمود رقم الهوية في الملف"; busy = false; return@launch }
            val res = withContext(Dispatchers.IO) {
                val idx = HashMap<String, Pair<String, JSONObject>>()
                data.campList(auth).forEach { (cid, _) -> data.people.persons(cid).forEach { m -> m.optString("nationalId").takeIf { it.isNotEmpty() }?.let { idx[it] = cid to m } } }
                fun digits(s: String) = s.map { if (it in '٠'..'٩') '0' + (it - '٠') else it }.filter { it.isDigit() }.joinToString("")
                val ms = ArrayList<Match>(); val un = ArrayList<List<String>>()
                for (r in rows.drop(1)) {
                    val nid = digits(r.getOrNull(nidI).orEmpty())
                    if (nid.isEmpty()) continue
                    val hit = idx[nid]
                    if (hit == null) { un += r; continue }
                    val patch = HashMap<String, String>()
                    fun cell(k: String) = map[k]?.let { r.getOrNull(it) }.orEmpty().trim()
                    val ph = digits(cell("phone")); if (ph.isNotEmpty() && hit.second.optString("phone").isEmpty()) patch["phone"] = ph
                    val dob = cell("dob"); if (Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(dob) && hit.second.optString("birthDate").let { it.isEmpty() || it == "null" }) patch["birthDate"] = dob
                    val g = cell("gender"); val gg = if (g.startsWith("ذ")) "male" else if (g.startsWith("أ") || g.startsWith("ا")) "female" else ""
                    if (gg.isNotEmpty() && hit.second.optString("gender").isEmpty()) patch["gender"] = gg
                    if (patch.isNotEmpty()) ms += Match(hit.first, hit.second, patch)
                }
                Triple(ms, un, rows.size - 1)
            }
            matches = res.first; unmatched = res.second; total = res.third; header = rows[0]; busy = false
        }
    }
    Column(Modifier.fillMaxSize()) {
        AppHeader("مطابقة كشف وطني", "", conn = null, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ارفع كشفاً يحوي عمود رقم الهوية. تُطابَق الهويات في كل المخيمات وتُكمَّل الحقول الفارغة فقط.", color = p.mute, fontSize = 12.5.sp)
            Btn(if (busy) "جارٍ المعالجة…" else "اختيار ملف Excel أو CSV", { pick.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth(), enabled = !busy, icon = QIcons.Import)
            matches?.let { ms ->
                QCard {
                    Text("صفوف الكشف: $total", color = p.ink, fontSize = 14.sp)
                    Text("سجلات سيُكمَّل نقصها: ${ms.size}", color = p.ink, fontSize = 14.sp)
                    Text("هويات غير موجودة في أي مخيم: ${unmatched.size}", color = p.mute, fontSize = 13.sp)
                }
                if (ms.isNotEmpty()) Btn("تطبيق التكميل", {
                    scope.launch {
                        busy = true
                        withContext(Dispatchers.IO) {
                            ms.groupBy { it.camp }.forEach { (camp, list) ->
                                list.forEach { m ->
                                    val o = org.json.JSONObject(m.person.toString())
                                    m.patch.forEach { (k, v) -> o.put(k, v) }
                                    o.put("updatedBy", auth.user.id)
                                    data.people.save(camp, o)
                                }
                                data.pushNow(auth, camp)
                            }
                        }
                        msg = "تم تكميل ${ms.size} سجلاً"; matches = null; busy = false
                    }
                }, Modifier.fillMaxWidth(), enabled = !busy, icon = QIcons.Check)
                if (unmatched.isNotEmpty()) Btn("تحميل تقرير غير المطابَقين", {
                    scope.launch {
                        val r = withContext(Dispatchers.IO) { data.export.save("غير-مطابقين.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", data.export.xlsx(unmatched, "غير مطابقين", header), unmatched.size) }
                        msg = if (r.ok) "حُفظ التقرير: ${r.name}" else "تعذّر الحفظ"
                    }
                }, Modifier.fillMaxWidth(), kind = BtnKind.Outline, icon = QIcons.Export)
            }
            msg?.let { Text(it, color = p.ink, fontSize = 13.sp) }
        }
    }
}
