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

private class AcctType(val role: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val hint: String, val short: String)
/** ACCOUNT_TYPES. */
private val acctTypes = listOf(
    AcctType("delegate", Fa.UserTie, "مندوب مخيم", "يدير مخيماً كاملاً", "مندوب"), AcctType("dataentry", Fa.Keyboard, "مدخل بيانات", "يُدخل بيانات مخيم موجود", "مدخل بيانات"),
    AcctType("admin", Fa.UserShield, "الإدارة", "صلاحية على كل المخيمات", "إدارة"), AcctType("beneficiary", Fa.HouseUser, "مستفيد", "رب أسرة بمخيم", "مستفيد"),
)

@Composable
fun StaffPage(auth: AuthState, data: dev.saged.grcs.data.DataRepo, onPerson: ((JSONObject) -> Unit)? = null, onBack: () -> Unit) {
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
    var nRole by remember { mutableStateOf("") }
    var nNewCamp by remember { mutableStateOf(true) }
    var nCampName by remember { mutableStateOf("") }
    var nName by remember { mutableStateOf("") }
    var nUser by remember { mutableStateOf("") }
    var nPw by remember { mutableStateOf("") }
    var nPhone by remember { mutableStateOf("") }
    var nCamp by remember { mutableStateOf("") }
    var ben by remember { mutableStateOf<List<org.json.JSONObject>?>(null) }
    LaunchedEffect(rev) { rows = withContext(Dispatchers.IO) { Staff.list(auth) } }
    LaunchedEffect(rev, tab) { if (tab == 2 && ben == null) ben = withContext(Dispatchers.IO) { Staff.allBeneficiaries(auth, camps) } }
    LaunchedEffect(rev) { if (rev > 0 && tab == 2) ben = withContext(Dispatchers.IO) { Staff.allBeneficiaries(auth, camps) } }
    Column(Modifier.fillMaxSize()) {
        AppHeader("إدارة الحسابات", "", conn = null, mark = dev.saged.grcs.ui.icons.Fa.UsersGear, search = true)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!adding) Btn("إنشاء حساب جديد", { nRole = ""; note = ""; adding = true }, Modifier.fillMaxWidth(), icon = Fa.UserPlus)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("مناديب ومدخلو بيانات", "المدراء", "المستفيدون").forEachIndexed { i, l -> QChip(l, tab == i) { tab = i } } }
            SearchBox(query, { query = it }, "بحث بالاسم، اسم المستخدم، أو المخيم...")
            if (note.isNotEmpty()) Text(note, color = p.mute, fontSize = 12.sp)
            if (tab == 2) {
                if (ben == null) Text("جارٍ التحميل…", color = p.mute, fontSize = 13.sp)
                else Text("${ben!!.size} حساب مستفيد — يُنشأ تلقائياً لكل رب أسرة", color = p.mute, fontSize = 12.sp)
            }
            if (rows == null) Text("جارٍ التحميل…", color = p.mute, fontSize = 13.sp)
            val shown = (if (tab == 2) ben else rows)?.filter { (tab == 2 || (it.optString("role") == "admin") == (tab == 1)) && (query.isBlank() || it.optString("fullName").contains(query.trim()) || it.optString("username").contains(query.trim(), true) || it.optString("campName").contains(query.trim())) }
            if (shown != null && shown.isEmpty()) dev.saged.grcs.ui.components.EmptyState(Fa.UsersSlash, "لا توجد حسابات في هذا القسم", "جرّب تبويباً آخر أو امسح كلمة البحث")
            if (shown != null && shown.size > 150) Text("يُعرض أول 150 من ${shown.size} — اكتب في البحث للوصول لحساب محدد", color = p.mute, fontSize = 12.5.sp)
            shown?.take(150)?.forEach { u ->
                val id = u.getString("id"); val active = u.optString("isActive") != "false"
                val openBen: (() -> Unit)? = if (tab == 2 && onPerson != null && u.optString("personId").isNotEmpty() && u.optString("campId").isNotEmpty()) ({
                    scope.launch {
                        val ps = withContext(Dispatchers.IO) { data.people.persons(u.optString("campId")).firstOrNull { it.optString("id") == u.optString("personId") } }
                        if (ps != null) onPerson(JSONObject(ps.toString()).put("campId", u.optString("campId"))) else note = "بيانات هذا المستفيد غير محمّلة على الجهاز — افتح مخيمه أولاً"
                    }
                }) else null
                QCard(onClick = openBen) { Column {
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
    if (adding) dev.saged.grcs.ui.components.Sheet({ adding = false }, "إنشاء حساب جديد") { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // openNewUserModal: نوع الحساب أولاً ثم حقول كل نوع
        if (nRole.isEmpty()) Text("اختر نوع الحساب أولاً", color = p.mute, fontSize = 13.sp)
        acctTypes.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { t ->
            val on = nRole == t.role; val sh = RoundedCornerShape(14.dp)
            Column(Modifier.weight(1f).clip(sh).background(if (on) p.pri.copy(alpha = .1f) else p.surface).border(1.5.dp, if (on) p.pri else p.line, sh).clickable { nRole = t.role; note = "" }.padding(horizontal = 10.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                androidx.compose.material3.Icon(t.icon, null, Modifier.size(20.dp), tint = if (on) p.pri else p.mute)
                Text(t.label, color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(t.hint, color = p.mute, fontSize = 11.5.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } } }
        if (nRole.isNotEmpty()) {
            val orphan = camps.filter { c -> rows.orEmpty().none { it.optString("role") == "delegate" && it.optString("campId") == c.first } }
            val none = listOf("" to "بدون مخيم (يُحدَّد لاحقاً)")
            when (nRole) {
                "delegate" -> {
                    dev.saged.grcs.ui.components.SegBtns(listOf("مخيم جديد" to Fa.Tent, ("مخيم بلا مندوب" + if (orphan.isNotEmpty()) " (${orphan.size})" else "") to Fa.Tents), if (nNewCamp || orphan.isEmpty()) 0 else 1) { nNewCamp = it == 0 || orphan.isEmpty(); if (!nNewCamp) nCamp = orphan.first().first }
                    if (nNewCamp || orphan.isEmpty()) QField("اسم المخيم الجديد", nCampName, { nCampName = it }, placeholder = "مثال: مخيم الأمل")
                    else dev.saged.grcs.ui.components.QSelect(nCamp.takeIf { c -> orphan.any { it.first == c } } ?: orphan.first().first, orphan, label = "المخيم") { nCamp = it }
                }
                "dataentry" -> dev.saged.grcs.ui.components.QSelect(nCamp, none + camps, label = "المخيم الذي سيُدخل بياناته") { nCamp = it }
                "beneficiary" -> dev.saged.grcs.ui.components.QSelect(nCamp, none + camps, label = "المخيم (اختياري)") { nCamp = it }
                else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { androidx.compose.material3.Icon(Fa.ShieldHalved, null, Modifier.size(14.dp), tint = p.success); Text("حساب إدارة: يرى ويدير كل المخيمات والحسابات", color = p.success, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
            if (nRole != "delegate" && nRole != "admin" && nCamp.isEmpty()) Text("بدون مخيم: الحساب يُنشأ ويظهر في القوائم، لكنه لا يرى بيانات أي مخيم حتى يُربط بمخيم من «تعديل».", color = p.mute, fontSize = 12.sp)
            QField("الاسم الكامل", nName, { nName = it })
            QField("اسم المستخدم", nUser, { nUser = it })
            QField("كلمة المرور المبدئية", nPw, { nPw = it }, placeholder = "6 أحرف على الأقل")
            QField("رقم الهاتف", nPhone, { nPhone = it }, type = androidx.compose.ui.text.input.KeyboardType.Phone)
            if (note.isNotEmpty()) Text(note, color = p.danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Btn(acctTypes.first { it.role == nRole }.let { "إنشاء حساب " + it.short }, { scope.launch {
                val newCamp = nRole == "delegate" && (nNewCamp || orphan.isEmpty())
                val cid = if (nRole == "admin" || newCamp) "" else nCamp
                val err = withContext(Dispatchers.IO) { Staff.create(auth, nRole, nName, nUser, nPw, nPhone, cid, if (newCamp) nCampName else camps.firstOrNull { it.first == cid }?.second.orEmpty(), newCamp) }
                note = err.orEmpty()
                if (err == null) { adding = false; tab = if (nRole == "admin") 1 else if (nRole == "beneficiary") 2 else 0; nRole = ""; nName = ""; nUser = ""; nPw = ""; nPhone = ""; nCampName = ""; nCamp = ""; ben = null; rev++ }
            } }, Modifier.fillMaxWidth(), icon = Fa.Check)
        }
    } }
    byId(editFor)?.let { u -> val id = u.optString("id"); dev.saged.grcs.ui.components.Sheet({ editFor = null }, "تعديل المستخدم") { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (u.optString("role") != "beneficiary") FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Staff.roles.forEach { (r, l) -> QChip(l, eRole == r) { eRole = r } } }
                        if (eRole != "admin") dev.saged.grcs.ui.components.QSelect(eCamp.takeIf { c -> camps.any { it.first == c } }.orEmpty(), (if (eRole == "delegate") emptyList() else listOf("" to "بدون مخيم")) + camps, label = if (eRole == "dataentry") "المخيم الذي يُدخل بياناته" else "المخيم") { eCamp = it }
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
