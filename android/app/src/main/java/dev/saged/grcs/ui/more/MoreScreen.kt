package dev.saged.grcs.ui.more

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.Conn
import dev.saged.grcs.ui.components.ConnState
import dev.saged.grcs.ui.components.Hair
import dev.saged.grcs.ui.components.MenuRow
import dev.saged.grcs.ui.components.SettingCard
import dev.saged.grcs.ui.components.SegTabs
import dev.saged.grcs.ui.theme.ThemePref
import dev.saged.grcs.ui.icons.Fa
import dev.saged.grcs.ui.components.SettingsRow
import dev.saged.grcs.ui.components.Caption
import dev.saged.grcs.ui.components.SwitchRow
import dev.saged.grcs.ui.icons.QIcons
import androidx.compose.foundation.layout.widthIn
import dev.saged.grcs.BuildConfig
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.auth.Http
import dev.saged.grcs.config.BrandConfig
import dev.saged.grcs.data.BackupFile
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.Usage
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import dev.saged.grcs.ui.theme.UiPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun ver(s: String): List<Int> = Regex("""\d+""").findAll(s).take(3).map { it.value.toInt() }.toList()
private fun newer(a: String, b: String): Boolean {
    val x = ver(a); val y = ver(b)
    for (i in 0 until 3) { val d = (x.getOrElse(i) { 0 }) - (y.getOrElse(i) { 0 }); if (d != 0) return d > 0 }
    return false
}

@Composable
fun MoreScreen(auth: AuthState, data: DataRepo, onLogout: () -> Unit) {
    val p = LocalPalette.current
    var page by remember { mutableStateOf("main") }
    BackHandler(page != "main") { page = "main" }
    LaunchedEffect(dev.saged.grcs.ui.shell.ShellState.moreRoute) { dev.saged.grcs.ui.shell.ShellState.moreRoute?.let { page = it; dev.saged.grcs.ui.shell.ShellState.moreRoute = null } }
    Box(Modifier.fillMaxSize()) {
        when (page) {
            "backup" -> BackupPage(auth, data) { page = "main" }
            "update" -> UpdatePage { page = "main" }
            "import" -> ImportPage(auth, data) { page = "main" }
            "vuln" -> VulnPage(auth, data) { page = "main" }
            "quality" -> QualityPage(auth, data) { page = "main" }
            "staff" -> StaffPage(auth, data) { page = "main" }
            "export" -> ExportPage(auth, data) { page = "main" }
            "trash" -> TrashPage(auth, data) { page = "main" }
            "audit" -> AuditPage(auth, data) { page = "main" }
            "query" -> QueryPage(auth, data) { page = "main" }
            "similar" -> SimilarPage(auth, data) { page = "main" }
            "rapid" -> RapidPage(auth, data) { page = "main" }
            "aidrep" -> AidReportPage(auth, data) { page = "main" }
            "aid" -> AidScreen(auth, data) { page = "main" }
            "changepw" -> ChangePwPage(auth) { page = "main" }
            "caminfo" -> CampInfoPage(auth, data) { page = "main" }
            "tlog" -> TransferLogPage(auth, data) { page = "main" }
            "diag" -> SyncDiagPage(auth, data) { page = "main" }
            "danger" -> CampDangerPage(auth, data) { page = "main" }
            else -> Settings(auth, data, onLogout) { page = it }
        }
    }
}

@Composable
private fun Group(title: String?, tint: androidx.compose.ui.graphics.Color? = null, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        if (title != null) Text(title, Modifier.padding(horizontal = 6.dp), color = tint ?: p.mute, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        Column(Modifier.fillMaxWidth().clip(shape).background(p.surface).border(1.dp, p.line, shape), content = content)
    }
}

@Composable
private fun NavRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String? = null, tint: androidx.compose.ui.graphics.Color? = null, badge: String? = null, last: Boolean = false, onClick: () -> Unit) {
    val p = LocalPalette.current
    val c = tint ?: if (p.dark) p.tealL else p.tealD
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 60.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(c.copy(alpha = .14f)), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(17.dp), tint = c) }
        Column(Modifier.weight(1f)) {
            Text(title, color = tint ?: p.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (sub != null) Text(sub, color = p.mute, fontSize = 12.sp, lineHeight = 17.sp)
        }
        if (badge != null) Text(badge, Modifier.background(p.pri, RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 3.dp), color = p.onPri, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Icon(Fa.ChevronLeft, null, Modifier.size(12.dp), tint = p.mute)
    }
    if (!last) Box(Modifier.padding(start = 64.dp).fillMaxWidth().height(1.dp).background(p.line))
}

@Composable
private fun Settings(auth: AuthState, data: DataRepo, onLogout: () -> Unit, go: (String) -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val role = auth.user.role
    val staff = role != "beneficiary"
    val adminDel = role == "admin" || role == "delegate"
    var cleaning by remember { mutableStateOf(false) }
    var askLogout by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val pending = remember(syncing) { data.campList(auth).sumOf { data.people.pendingCount(it.first) } }
    val fmt = remember { SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.US) }
    val lastBk = remember { runCatching { data.backups.list().maxOfOrNull { it.modified } }.getOrNull() }
    val up by dev.saged.grcs.data.Updater.state.collectAsState()
    val roleLabel = mapOf("admin" to "مسؤول عام", "delegate" to "مندوب مخيم", "dataentry" to "مدخل بيانات", "beneficiary" to "مستفيد")[role] ?: role
    val campName = remember { if (role == "admin") null else data.campList(auth).firstOrNull()?.second?.takeIf { it.isNotBlank() && it != auth.user.campId } }
    fun sync() {
        scope.launch {
            syncing = true; msg = null; ConnState.state = Conn.Sync
            val r = withContext(Dispatchers.IO) { runCatching { data.sync(auth) } }
            val ok = r.isSuccess && !r.getOrThrow().first.offline
            ConnState.state = if (ok) Conn.On else Conn.Off
            if (ok) ConnState.lastSync = System.currentTimeMillis()
            msg = if (ok) "اكتملت المزامنة" else "تعذّرت المزامنة — تحقق من الاتصال"
            syncing = false
        }
    }
    fun cleanIndex() {
        if (cleaning) return
        cleaning = true
        scope.launch {
            val n = withContext(Dispatchers.IO) { dev.saged.grcs.data.Maintenance.cleanNationalIndex(auth, data.campList(auth).map { it.first }.toSet()) }
            cleaning = false
            msg = if (n < 0) "تعذّر إتمام التنظيف — تحقق من الاتصال وحاول مجدداً" else if (n > 0) "تم تنظيف $n مدخل يتيم من الفهرس الوطني" else "الفهرس الوطني نظيف بالفعل — لا يوجد مدخلات يتيمة"
        }
    }
    Column(Modifier.fillMaxSize()) {
        AppHeader("الإعدادات", "", conn = null, mark = Fa.Gear, search = true)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                // بطاقة الحساب
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(p.navy, p.navyL))).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(54.dp).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(p.tealL, p.pri)), CircleShape), contentAlignment = Alignment.Center) {
                        Text(auth.user.fullName.trim().take(1).ifEmpty { "؟" }, color = androidx.compose.ui.graphics.Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(auth.user.fullName, color = androidx.compose.ui.graphics.Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(listOfNotNull(roleLabel, campName).joinToString(" · "), color = androidx.compose.ui.graphics.Color(0xFF9FC3DB), fontSize = 12.5.sp, maxLines = 1)
                    }
                    Text("v${BuildConfig.VERSION_NAME}", Modifier.background(androidx.compose.ui.graphics.Color.White.copy(alpha = .12f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp), color = androidx.compose.ui.graphics.Color.White, fontSize = 11.5.sp)
                }

                if (role != "dataentry") Group("المزامنة") {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        SettingsRow("آخر مزامنة ناجحة", if (ConnState.lastSync == 0L) "لم تتم بعد" else fmt.format(Date(ConnState.lastSync)))
                        SettingsRow("عمليات بانتظار المزامنة", pending.toString(), last = true)
                    }
                    Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                        Btn(if (syncing) "جارٍ المزامنة…" else "مزامنة مع المركز الآن", ::sync, Modifier.fillMaxWidth(), enabled = !syncing, icon = Fa.Rotate)
                        msg?.let { Caption(it, Modifier.padding(top = 8.dp)) }
                    }
                    if (staff) NavRow(Fa.Rotate, "تشخيص المزامنة", "فحص حالة الاتصال والمؤشرات", last = true) { go("diag") }
                }

                if (staff) Group("النسخ والحماية") {
                    NavRow(Fa.BoxArchive, "النسخ الاحتياطي", lastBk?.let { "آخر نسخة ${fmt.format(Date(it))}" } ?: "تصدير نسخة كاملة أو استعادتها", last = !adminDel && role == "dataentry") { go("backup") }
                    if (adminDel) NavRow(Fa.TrashArrowUp, "سلة المحذوفات", "استرجاع ما حُذف بالخطأ", last = role == "dataentry") { go("trash") }
                    if (role != "dataentry") NavRow(Fa.ClockRotateLeft, "سجل التغييرات", "من غيّر ماذا ومتى", last = true) { go("audit") }
                }

                if (role == "delegate" || role == "admin") Group("الإدارة") {
                    if (role == "delegate") NavRow(Fa.Tent, "معلومات المخيم", "عرض وتعديل بيانات المخيم", last = true) { go("caminfo") }
                    if (role == "admin") {
                        NavRow(Fa.ScaleBalanced, "مؤشر الهشاشة", "أوزان العوامل وحدود التصنيف — تُطبَّق على كل الأجهزة") { go("vuln") }
                        NavRow(Fa.FilterCircleDollar, "منشئ الاستعلامات", "بحث مركّب بشروط محفوظة") { go("query") }
                        NavRow(Fa.RightLeft, "سجل نقل الأسر", "حركة الأسر بين المخيمات", last = true) { go("tlog") }
                    }
                }

                Group("التفضيلات") {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("المظهر", color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        dev.saged.grcs.ui.components.SegBtns(listOf("تلقائي" to Fa.CircleHalfStroke, "فاتح" to Fa.Sun, "داكن" to Fa.Moon), ThemePref.mode) { ThemePref.set(it) }
                    }
                    if (staff) Column(Modifier.padding(horizontal = 14.dp)) {
                        Hair()
                        SwitchRow("اهتزاز خفيف", "عند الضغط والنجاح والتنبيه", UiPrefs.haptic) { UiPrefs.updateHaptic(it) }
                        Hair()
                        SwitchRow("صوت نقرة", "اختياري — يتوقف عند تفعيل «تقليل الحركة»", UiPrefs.click, last = true) { UiPrefs.updateClick(it) }
                    }
                }

                Group("الحساب والتطبيق") {
                    NavRow(Fa.Key, "تغيير كلمة المرور") { go("changepw") }
                    NavRow(Fa.Download, "التحديثات", "الإصدار الحالي v${BuildConfig.VERSION_NAME}", badge = if (up is dev.saged.grcs.data.UpState.None) null else "جديد") { go("update") }
                    NavRow(Fa.ArrowRightFromBracket, "تسجيل الخروج", tint = p.danger, last = true) { askLogout = true }
                }

                if (role == "delegate" || role == "admin") Group("منطقة الخطر", p.danger) {
                    if (role == "delegate") NavRow(Fa.Broom, "إفراغ جميع بيانات مخيمي", "مسح الأفراد والمستفيدين تمهيداً لإعادة الاستيراد — حساب المندوب لا يتأثر", tint = p.danger, last = true) { go("danger") }
                    if (role == "admin") {
                        NavRow(Fa.Broom, if (cleaning) "جارٍ الفحص..." else "تنظيف الفهرس الوطني", "يحذف مدخلات الفهرس العائدة لمخيمات لم تعد موجودة", tint = p.danger, last = true) { cleanIndex() }
                        msg?.let { Caption(it, Modifier.padding(14.dp)) }
                    }
                }

                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("منظومة مخيمات النزوح", color = p.head, fontSize = 13.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
                    Text("تطوير: ${BrandConfig.Dev.TITLE}", color = p.mute, fontSize = 12.sp, lineHeight = 24.sp)
                }
            }
        }
    }
    if (askLogout) dev.saged.grcs.ui.components.ConfirmSheet("هل تريد تسجيل الخروج من التطبيق؟", "تسجيل الخروج", "خروج", icon = Fa.ArrowRightFromBracket, onCancel = { askLogout = false }) { askLogout = false; onLogout() }
}

@Composable
private fun Back(title: String, onBack: () -> Unit) {
    val p = LocalPalette.current
    AppHeader(title, "", conn = null, onBack = onBack)
}

@Composable
private fun UpdatePage(onBack: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val st by dev.saged.grcs.data.Updater.state.collectAsState()
    var status by remember { mutableStateOf("جارٍ الفحص…") }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { dev.saged.grcs.data.Updater.restore(ctx); dev.saged.grcs.data.Updater.autoCheck(ctx, download = false) }
        status = if (dev.saged.grcs.data.Updater.state.value is dev.saged.grcs.data.UpState.None) "أنت على أحدث إصدار (${BuildConfig.VERSION_NAME})" else ""
    }
    Column(Modifier.fillMaxSize()) {
        Back("التحديثات", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (val s = st) {
                is dev.saged.grcs.data.UpState.None -> Text(status, color = p.ink, fontSize = 15.sp)
                is dev.saged.grcs.data.UpState.Available -> {
                    Text("إصدار جديد متاح: ${s.info.tag}", color = p.ink, fontSize = 15.sp)
                    Btn("تنزيل التحديث", { dev.saged.grcs.data.Updater.start(ctx, s.info) }, Modifier.fillMaxWidth(), icon = Fa.Download)
                }
                is dev.saged.grcs.data.UpState.Downloading -> {
                    Text("جارٍ تنزيل ${s.info.tag}… ${s.pct}%", color = p.ink, fontSize = 15.sp)
                    dev.saged.grcs.ui.components.Bar(s.pct / 100f, p.pri)
                }
                is dev.saged.grcs.data.UpState.Ready -> {
                    Text("اكتمل تنزيل ${s.info.tag}", color = p.ink, fontSize = 15.sp)
                    Btn("تثبيت الآن", { dev.saged.grcs.data.Updater.install(ctx) }, Modifier.fillMaxWidth(), icon = Fa.Download)
                }
            }
            Text("يُنزَّل التحديث تلقائياً بالخلفية ويُثبَّت فوق التطبيق الحالي دون فقد البيانات.", color = p.mute, fontSize = 12.sp)
        }
    }
}

@Composable
private fun BackupPage(auth: AuthState, data: DataRepo, onBack: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val camps = remember { data.campList(auth) }
    var files by remember { mutableStateOf<List<BackupFile>>(emptyList()) }
    var msg by remember { mutableStateOf<String?>(null) }
    var rev by remember { mutableStateOf(0) }
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { rev++ }
    LaunchedEffect(rev) { files = withContext(Dispatchers.IO) { data.backups.list() } }

    fun create() {
        scope.launch {
            if (data.backups.needsPermission()) { perm.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) }
            val ok = withContext(Dispatchers.IO) {
                camps.map { (id, n) -> data.backups.write(data.backups.fileName(id, false), data.backups.build(id, n, data.people.persons(id))) }.all { it }
            }
            msg = if (ok) "تم حفظ النسخة في المستندات ← ${BrandConfig.BACKUP_DIR}" else "تعذّر الحفظ — تحقق من إذن التخزين"
            rev++
        }
    }

    fun restore(b: BackupFile) {
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    val j = JSONObject(data.backups.read(b) ?: error("read"))
                    val camp = j.getString("camp")
                    if (camps.none { it.first == camp }) return@runCatching -1
                    val cur = data.people.persons(camp).associateBy { it.getString("id") }
                    val arr = j.getJSONArray("persons"); var n = 0
                    for (i in 0 until arr.length()) if (data.people.restore(camp, arr.getJSONObject(i), cur)) n++
                    if (n > 0) data.pushNow(auth, camp)
                    n
                }.getOrDefault(-2)
            }
            msg = when { res == -1 -> "هذه النسخة لمخيم غير متاح لحسابك"; res < 0 -> "ملف نسخة غير صالح"; res == 0 -> "لا جديد للاستعادة — البيانات الحالية أحدث أو مطابقة"; else -> "استُعيد $res سجلاً (الأحدث يفوز، لا حذف لأي بيانات)" }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Back("النسخ الاحتياطي", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth().heightIn(min = 52.dp).background(p.pri, RoundedCornerShape(16.dp)).clickable { create() }, contentAlignment = Alignment.Center) {
                Text("إنشاء نسخة الآن", color = p.onPri, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("المجلد: المستندات ← ${BrandConfig.BACKUP_DIR}. تُنشأ نسخة تلقائية يومياً.", color = p.mute, fontSize = 12.sp)
            msg?.let { Text(it, color = p.ok, fontSize = 13.sp) }
            Spacer(Modifier.height(4.dp))
            if (files.isEmpty()) Text("لا نسخ بعد", color = p.mute, fontSize = 13.sp)
            files.forEach { b ->
                Row(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(16.dp)).padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                        Text(b.name, color = p.ink, fontSize = 13.sp)
                        Text("${fmt.format(Date(b.modified))} · ${b.size / 1024} ك.ب", color = p.mute, fontSize = 11.sp)
                    }
                    Text("استعادة", color = p.pri, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.heightIn(min = 48.dp).clickable { restore(b) }.padding(horizontal = 14.dp, vertical = 14.dp))
                }
            }
        }
    }
}

