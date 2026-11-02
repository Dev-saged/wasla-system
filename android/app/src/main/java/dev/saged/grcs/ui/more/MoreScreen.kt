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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
            "caminfo" -> CampInfoPage(auth, data) { page = "main" }
            "tlog" -> TransferLogPage(auth, data) { page = "main" }
            "diag" -> SyncDiagPage(auth, data) { page = "main" }
            "danger" -> CampDangerPage(auth, data) { page = "main" }
            else -> Settings(auth, data, onLogout) { page = it }
        }
    }
}

@Composable
private fun Settings(auth: AuthState, data: DataRepo, onLogout: () -> Unit, go: (String) -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val role = auth.user.role
    val staff = role != "beneficiary"
    val adminDel = role == "admin" || role == "delegate"
    var syncing by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val pending = remember(syncing) { data.campList(auth).sumOf { data.people.pendingCount(it.first) } }
    val fmt = remember { SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.US) }
    Column(Modifier.fillMaxSize()) {
        AppHeader("الإعدادات", auth.user.fullName)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingCard("المزامنة", QIcons.Sync) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("آخر مزامنة", color = p.mute, fontSize = 12.5.sp)
                        Text(if (ConnState.lastSync == 0L) "—" else fmt.format(Date(ConnState.lastSync)), color = p.ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("تغييرات بانتظار الرفع", color = p.mute, fontSize = 12.5.sp)
                        Text(pending.toString(), color = if (pending > 0) p.warn else p.ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                    MenuRow(QIcons.Chart, "تشخيص المزامنة", "حالة كل مخيم والحصة اليومية", p.blue) { go("diag") }
                    Spacer(Modifier.height(8.dp))
                    Btn(if (syncing) "جارٍ المزامنة…" else "مزامنة مع المركز الآن", {
                        scope.launch {
                            syncing = true; msg = null; ConnState.state = Conn.Sync
                            val r = withContext(Dispatchers.IO) { runCatching { data.sync(auth) } }
                            val ok = r.isSuccess && !r.getOrThrow().first.offline
                            ConnState.state = if (ok) Conn.On else Conn.Off
                            if (ok) ConnState.lastSync = System.currentTimeMillis()
                            msg = if (ok) "اكتملت المزامنة" else "تعذّرت المزامنة — تحقق من الاتصال"
                            syncing = false
                        }
                    }, Modifier.fillMaxWidth(), enabled = !syncing, icon = QIcons.Sync)
                    msg?.let { Text(it, color = if (it.startsWith("اكتملت")) p.ok else p.warn, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
                    Text("استهلاك هذا الجهاز اليوم: ${Usage.reads()} قراءة · ${Usage.writes()} كتابة", color = p.mute, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                }
                if (staff) SettingCard("النسخ الاحتياطي", QIcons.Backup) {
                    MenuRow(QIcons.Backup, "نسخ احتياطي واستعادة", "نسخة يومية تلقائية في المستندات ← ${BrandConfig.BACKUP_DIR}") { go("backup") }
                    Hair()
                    MenuRow(QIcons.Export, "تصدير البيانات", "Excel · CSV · PDF") { go("export") }
                    Hair()
                    MenuRow(QIcons.Import, "استيراد كشف Excel", "أسر ومعيلون وأفراد") { go("import") }
                }
                if (adminDel) SettingCard("الحماية من فقدان البيانات", QIcons.Shield) {
                    MenuRow(QIcons.Trash, "سلة المحذوفات", "استعادة ما حُذف خلال ٣٠ يوماً", p.danger) { go("trash") }
                    Hair()
                    MenuRow(QIcons.Clock, "سجل النشاط", "من فعل ماذا ومتى", p.blue) { go("audit") }
                }
                if (staff) SettingCard("أدوات البيانات", QIcons.Chart) {
                    MenuRow(QIcons.Bolt, "إدخال سريع", null, p.warn) { go("rapid") }
                    Hair()
                    MenuRow(QIcons.Alert, "مؤشر الهشاشة", "ترتيب الأسر حسب الاحتياج", p.acc) { go("vuln") }
                    Hair()
                    MenuRow(QIcons.Check, "جودة البيانات", "نقص وتعارض وقيم مشبوهة", p.success) { go("quality") }
                    Hair()
                    MenuRow(QIcons.Filter, "الفئات والاستعلامات", "فئات جاهزة واستعلامات محفوظة", p.purple) { go("query") }
                    Hair()
                    MenuRow(QIcons.Copy, "الأسماء المتشابهة", "كشف التكرار المحتمل", p.gray) { go("similar") }
                    Hair()
                    MenuRow(QIcons.Report, "تقرير المساعدات", null, p.blue) { go("aidrep") }
                }
                if (adminDel) SettingCard("المخيم", QIcons.Camp) {
                    MenuRow(QIcons.Info, "معلومات المخيم", "المسؤول ونائبه ومدخل البيانات") { go("caminfo") }
                    Hair()
                    MenuRow(QIcons.Transfer, "سجل نقل الأسر", "الأسر المنقولة بين المخيمات", p.purple) { go("tlog") }
                }
                if (role == "admin") SettingCard("الإدارة", QIcons.Key) {
                    MenuRow(QIcons.Key, "حسابات الطاقم", "مناديب ومدخلو بيانات") { go("staff") }
                    Hair()
                    MenuRow(QIcons.Alert, "إفراغ وحذف المخيمات", "نسخة إلزامية + سلة + تأكيد بالاسم", p.danger) { go("danger") }
                }
                SettingCard("المظهر", QIcons.Moon) {
                    SegTabs(listOf("تلقائي", "فاتح", "ليلي"), ThemePref.mode, { ThemePref.set(it) })
                }
                SettingCard("حول التطبيق", QIcons.Info) {
                    MenuRow(QIcons.Sync, "التحديثات", "الإصدار ${BuildConfig.VERSION_NAME}") { go("update") }
                }
                Btn("تسجيل الخروج", onLogout, Modifier.fillMaxWidth(), BtnKind.Danger, QIcons.Logout)
                Text("تطوير ${BrandConfig.Dev.NAME}", color = p.mute, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
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
    var status by remember { mutableStateOf("جارٍ الفحص…") }
    var apk by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val r = withContext(Dispatchers.IO) { runCatching { Http.getPublic("https://api.github.com/repos/${BrandConfig.UPDATE_REPO}/releases/latest") }.getOrNull() }
        val j = r?.json
        if (j == null || r.code != 200) { status = "تعذّر الفحص الآن"; return@LaunchedEffect }
        val tag = j.optString("tag_name")
        val assets = j.optJSONArray("assets")
        var url: String? = null
        if (assets != null) for (i in 0 until assets.length()) assets.getJSONObject(i).let { if (it.optString("name").endsWith(".apk")) url = it.optString("browser_download_url") }
        if (newer(tag, BuildConfig.VERSION_NAME) && url != null) { status = "إصدار جديد متاح: $tag"; apk = url } else status = "أنت على أحدث إصدار (${BuildConfig.VERSION_NAME})"
    }
    Column(Modifier.fillMaxSize()) {
        Back("التحديثات", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(status, color = p.ink, fontSize = 15.sp)
            apk?.let { u ->
                Box(Modifier.fillMaxWidth().heightIn(min = 52.dp).background(p.pri, RoundedCornerShape(16.dp)).clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) }, contentAlignment = Alignment.Center) {
                    Text("تنزيل التحديث", color = p.onPri, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("التحديث يثبّت فوق التطبيق الحالي دون فقد البيانات.", color = p.mute, fontSize = 12.sp)
            }
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
