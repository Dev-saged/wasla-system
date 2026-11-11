package dev.saged.grcs.ui.more

import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.Pill
import dev.saged.grcs.ui.components.QCard
import dev.saged.grcs.ui.components.SearchBox
import dev.saged.grcs.ui.components.SegTabs
import dev.saged.grcs.ui.icons.Fa
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import dev.saged.grcs.ui.icons.QIcons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.Staff
import dev.saged.grcs.ui.components.QField
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import org.json.JSONObject
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StaffPage(auth: AuthState, data: dev.saged.grcs.data.DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<JSONObject>?>(null) }
    var rev by remember { mutableStateOf(0) }
    var pwFor by remember { mutableStateOf<String?>(null) }
    var pw by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var editFor by remember { mutableStateOf<String?>(null) }
    var eName by remember { mutableStateOf("") }
    var eUser by remember { mutableStateOf("") }
    var ePhone by remember { mutableStateOf("") }
    var eRole by remember { mutableStateOf("delegate") }
    var eCamp by remember { mutableStateOf("") }
    var delFor by remember { mutableStateOf<String?>(null) }
    var logFor by remember { mutableStateOf<String?>(null) }
    var logRows by remember { mutableStateOf<List<dev.saged.grcs.data.AuditEntry>?>(null) }
    var clearFor by remember { mutableStateOf<JSONObject?>(null) }
    var showPw by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    val camps = remember { data.campList(auth) }
    var adding by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var nRole by remember { mutableStateOf("delegate") }
    var nName by remember { mutableStateOf("") }
    var nUser by remember { mutableStateOf("") }
    var nPw by remember { mutableStateOf("") }
    var nPhone by remember { mutableStateOf("") }
    var nCamp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var benCamp by remember { mutableStateOf(camps.firstOrNull()?.first.orEmpty()) }
    var ben by remember { mutableStateOf<List<org.json.JSONObject>?>(null) }
    LaunchedEffect(rev) { rows = withContext(Dispatchers.IO) { Staff.list(auth) } }
    LaunchedEffect(rev, tab, benCamp) { if (tab == 2) { ben = null; ben = withContext(Dispatchers.IO) { Staff.beneficiaries(auth, benCamp) } } }
    Column(Modifier.fillMaxSize()) {
        AppHeader("إدارة الحسابات", "", conn = null, mark = dev.saged.grcs.ui.icons.Fa.UsersGear, search = true)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tab != 2 && !adding) Btn("إنشاء مستخدم جديد", { adding = true }, Modifier.fillMaxWidth(), icon = Fa.UserPlus)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("مناديب ومدخلو بيانات", "المدراء", "المستفيدون").forEachIndexed { i, l -> QChip(l, tab == i) { tab = i } } }
            SearchBox(query, { query = it }, "بحث بالاسم، اسم المستخدم، أو المخيم...")
            if (note.isNotEmpty()) Text(note, color = p.mute, fontSize = 12.sp)
            if (tab == 2 && camps.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { camps.forEach { (id, n) -> QChip(n, benCamp == id) { benCamp = id } } }
            if (tab == 2) {
                if (ben == null) Text("جارٍ التحميل…", color = p.mute, fontSize = 13.sp)
                else Text("${ben!!.size} حساب مستفيد — يُنشأ تلقائياً لكل رب أسرة", color = p.mute, fontSize = 12.sp)
            }
            if (rows == null) Text("جارٍ التحميل…", color = p.mute, fontSize = 13.sp)
            val shown = (if (tab == 2) ben else rows)?.filter { (tab == 2 || (it.optString("role") == "admin") == (tab == 1)) && (query.isBlank() || it.optString("fullName").contains(query.trim()) || it.optString("username").contains(query.trim(), true) || it.optString("campName").contains(query.trim())) }
            if (shown != null && shown.isEmpty()) dev.saged.grcs.ui.components.EmptyState(Fa.UsersSlash, "لا توجد حسابات في هذا القسم", "جرّب تبويباً آخر أو امسح كلمة البحث")
            shown?.forEach { u ->
                val id = u.getString("id"); val active = u.optString("isActive") != "false"
                QCard { Column {
                    Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(u.optString("fullName").ifEmpty { u.optString("username") }, color = p.ink, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                            Text(u.optString("username") + " — " + (mapOf("admin" to "مسؤول عام", "delegate" to "مندوب مخيم", "dataentry" to "مدخل بيانات")[u.optString("role")] ?: "مستفيد") + (u.optString("campName").takeIf { it.isNotEmpty() }?.let { " · $it" } ?: ""), color = p.mute, fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp))
                            val pwShown = showPw[id]
                            if (showPw.containsKey(id) && pwShown == null) Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                androidx.compose.material3.Icon(Fa.HourglassHalf, null, Modifier.size(10.dp), tint = p.warn); Text("لم يُفعَّل بعد", color = p.warn, fontSize = 12.5.sp)
                            } else {
                                val pill = androidx.compose.foundation.shape.RoundedCornerShape(50)
                                Row(Modifier.padding(top = 6.dp).heightIn(min = 34.dp).clip(pill).background(p.pri.copy(alpha = .1f)).border(1.dp, p.pri.copy(alpha = .45f), pill).clickable { scope.launch { val v = withContext(Dispatchers.IO) { Staff.password(auth, id) }; showPw = showPw + (id to v) } }.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    androidx.compose.material3.Icon(Fa.Key, null, Modifier.size(12.dp), tint = if (p.dark) p.tealL else p.tealD)
                                    Text(pwShown ?: "إظهار كلمة المرور", color = if (p.dark) p.tealL else p.tealD, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Pill(if (active) "مفعّل" else "معطّل", if (active) p.success else p.gray)
                    }
                    if (id != auth.user.id) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Btn("تعديل", { editFor = id; eName = u.optString("fullName"); eUser = u.optString("username"); ePhone = u.optString("phone"); eRole = u.optString("role"); eCamp = u.optString("campId") }, Modifier.weight(1f), BtnKind.Outline, Fa.Pen, small = true)
                            Btn("كلمة المرور", { pwFor = id; pw = "" }, Modifier.weight(1f), BtnKind.Outline, Fa.Key, small = true)
                        }
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Btn(if (active) "تعطيل" else "تفعيل", { scope.launch { note = if (withContext(Dispatchers.IO) { Staff.setActive(auth, u, !active) }) "تم" else "تعذّر التنفيذ"; rev++ } }, Modifier.weight(1f), if (active) BtnKind.Danger else BtnKind.Primary, if (active) Fa.Lock else Fa.LockOpen, small = true)
                            Btn("حذف", { delFor = id }, Modifier.weight(1f), BtnKind.Danger, Fa.Trash, small = true)
                        }
                    }
                    if (u.optString("role") in listOf("delegate", "dataentry")) Btn("سجل النشاط", {
                        run { logFor = id; logRows = null; scope.launch { logRows = withContext(Dispatchers.IO) { runCatching { dev.saged.grcs.data.Audit.list(auth, u.optString("campId"), 300).filter { it.userName == u.optString("fullName") } }.getOrDefault(emptyList()) } } }
                    }, Modifier.fillMaxWidth().padding(top = 8.dp), BtnKind.Outline, Fa.ClockRotateLeft, small = true)
                    if (u.optString("role") == "delegate" && u.optString("campId").isNotEmpty()) {
                        val sh = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        Row(Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(min = 38.dp).clip(sh).background(p.surface).border(1.5.dp, p.danger, sh).clickable { clearFor = u }.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(Fa.Broom, null, Modifier.size(14.dp), tint = p.danger); androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                            Text("إفراغ كل مستفيدي ${u.optString("campName").ifEmpty { "هذا المخيم" }} (لإعادة الاستيراد)", color = p.danger, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                } }
            }
        }
    }
    val all = rows.orEmpty() + ben.orEmpty()
    fun byId(x: String?) = all.firstOrNull { it.optString("id") == x }
    if (adding) dev.saged.grcs.ui.components.Sheet({ adding = false }, "إنشاء مستخدم جديد") { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Staff.roles.forEach { (r, l) -> QChip(l, nRole == r) { nRole = r } } }
                if (nRole != "admin") FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { camps.forEach { (id, n) -> QChip(n, nCamp == id) { nCamp = id } } }
                QField("الاسم الكامل", nName, { nName = it })
                QField("اسم المستخدم", nUser, { nUser = it })
                QField("كلمة المرور (6 أحرف على الأقل)", nPw, { nPw = it })
                QField("الجوال", nPhone, { nPhone = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn("إنشاء", { scope.launch {
                        val err = withContext(Dispatchers.IO) { Staff.create(auth, nRole, nName, nUser, nPw, nPhone, nCamp, camps.firstOrNull { it.first == nCamp }?.second.orEmpty()) }
                        note = err ?: "تم إنشاء الحساب"
                        if (err == null) { adding = false; nName = ""; nUser = ""; nPw = ""; nPhone = ""; rev++ }
                    } }, small = true, icon = QIcons.Check)
                    Btn("إلغاء", { adding = false }, kind = BtnKind.Outline, small = true)
                }
            } }
    byId(editFor)?.let { u -> val id = u.optString("id"); dev.saged.grcs.ui.components.Sheet({ editFor = null }, "تعديل المستخدم") { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (u.optString("role") != "beneficiary") FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Staff.roles.forEach { (r, l) -> QChip(l, eRole == r) { eRole = r } } }
                        if (eRole != "admin") FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { camps.forEach { (cid, n) -> QChip(n, eCamp == cid) { eCamp = cid } } }
                        QField("الاسم الكامل", eName, { eName = it }); QField("اسم المستخدم", eUser, { eUser = it }); QField("رقم الهاتف", ePhone, { ePhone = it })
                        Btn("حفظ التعديلات", { scope.launch {
                            val err = withContext(Dispatchers.IO) { Staff.update(auth, u, eName, eUser, ePhone, eRole, eCamp, camps.firstOrNull { it.first == eCamp }?.second.orEmpty()) }
                            note = err ?: "تم حفظ التعديلات بنجاح"; if (err == null) { editFor = null; rev++ }
                        } }, small = true, icon = QIcons.Check)
                        } } }
    byId(delFor)?.let { u -> dev.saged.grcs.ui.components.ConfirmSheet("سيتم حذف حساب «${u.optString("fullName")}» نهائياً" + if (u.optString("role") in listOf("delegate", "dataentry")) " ومعه حسابات مستفيدي مخيمه. بيانات الأفراد نفسها تبقى كما هي." else ". بيانات مخيمه تبقى كما هي.", "حذف الحساب", "حذف نهائي", icon = Fa.Trash, onCancel = { delFor = null }) { scope.launch { val n = withContext(Dispatchers.IO) { Staff.delete(auth, u) }; note = if (n < 0) "تعذّر الحذف" else if (n > 0) "تم حذف الحساب و$n حساب مستفيد تابع له" else "تم حذف الحساب بنجاح"; delFor = null; rev++ } } }
    byId(logFor)?.let { u -> dev.saged.grcs.ui.components.Sheet({ logFor = null }, "سجل نشاط: ${u.optString("fullName").ifEmpty { u.optString("username") }}") { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val lr = logRows
                        if (lr == null) Text("جارٍ التحميل…", color = p.mute, fontSize = 12.sp)
                        else {
                            val now = System.currentTimeMillis(); val day = 86_400_000L
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                listOf("اليوم" to lr.count { now - it.at < day }, "هذا الأسبوع" to lr.count { now - it.at < 7 * day }, "الإجمالي" to lr.size).forEach { (l, n) -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$n", color = p.ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold); Text(l, color = p.mute, fontSize = 11.sp) } }
                            }
                            if (lr.isEmpty()) Text("لا يوجد نشاط مسجَّل لهذا الحساب بعد", color = p.mute, fontSize = 12.sp)
                            else {
                                Text("حسب نوع الإجراء", color = p.mute, fontSize = 12.sp)
                                lr.groupingBy { it.action }.eachCount().forEach { (a, n) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(dev.saged.grcs.data.Audit.labels[a] ?: a, color = p.ink, fontSize = 13.sp); Text("$n", color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
                                Text("آخر 30 إجراء", color = p.mute, fontSize = 12.sp)
                                lr.take(30).forEach { e -> Text((dev.saged.grcs.data.Audit.labels[e.action] ?: e.action) + (if (e.targetName.isNotEmpty()) " · ${e.targetName}" else "") + " · " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(e.at)), color = p.mute, fontSize = 12.sp) }
                            }
                        }
                        } } }
    byId(pwFor)?.let { u -> dev.saged.grcs.ui.components.Sheet({ pwFor = null }, "إعادة تعيين كلمة المرور") { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        QField("كلمة المرور (6 أحرف على الأقل)", pw, { pw = it })
                        Btn("حفظ", { scope.launch { note = if (withContext(Dispatchers.IO) { Staff.resetPassword(auth, u, pw) }) "تم تحديث كلمة المرور" else "تعذّر التحديث"; pwFor = null; pw = "" } }, small = true, icon = QIcons.Check)
                        } } }

    clearFor?.let { u ->
        val cid = u.optString("campId"); val cname = u.optString("campName").ifEmpty { cid }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { clearFor = null },
            title = { Text("إفراغ بيانات المخيم") },
            text = { Text("سيتم حذف جميع الأفراد وحسابات المستفيدين في مخيم «$cname» من هذا الجهاز ومن قاعدة البيانات المركزية، وتُحفظ نسخة احتياطية وسلة محذوفات.") },
            confirmButton = { androidx.compose.material3.TextButton({ clearFor = null; scope.launch { note = withContext(Dispatchers.IO) { data.wipeCamp(auth, cid, cname, false) } ?: "تم إفراغ «$cname»"; rev++ } }) { Text("إفراغ نهائياً", color = p.danger) } },
            dismissButton = { androidx.compose.material3.TextButton({ clearFor = null }) { Text("إلغاء") } },
        )
    }
}
