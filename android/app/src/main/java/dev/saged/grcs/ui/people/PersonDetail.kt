package dev.saged.grcs.ui.people

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.data.Aid
import dev.saged.grcs.data.AuditEntry
import dev.saged.grcs.data.PFields
import dev.saged.grcs.data.Stats
import dev.saged.grcs.data.Vuln
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.Pill
import dev.saged.grcs.ui.components.QCard
import dev.saged.grcs.ui.components.SectionTitle
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.theme.LocalPalette
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val shelterNames = mapOf("tent_ok" to "خيمة سليمة", "tent_damaged" to "خيمة تالفة", "shared" to "مأوى مشترك", "building_damaged" to "مبنى متضرر", "none" to "بلا مأوى")
private val chronicNames = mapOf("heart" to "قلب", "diabetes" to "سكري", "hypertension" to "ضغط الدم", "kidney" to "كلى", "cancer" to "سرطان", "respiratory" to "ربو / تنفسي", "liver" to "كبد", "thyroid" to "غدة درقية", "mental" to "نفسية", "blood" to "أمراض دم", "neuro" to "عصبية", "bone" to "مفاصل / عظام", "other" to "أخرى")
private val tagNames = mapOf("orphan" to "يتيم", "injured" to "جريح/مصاب", "martyr_family" to "أسرة شهيد")
private val orphanTypes = mapOf("father" to "يتيم الأب", "both" to "يتيم الأبوين")
private val reqStatusColor = mapOf("pending" to "w", "approved" to "s", "done" to "s", "rejected" to "d", "cancelled" to "g")

private fun JSONObject.strs(k: String): List<String> = optJSONArray(k)?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()

/** relationLabel في الويب. */
private fun relationOf(p: JSONObject): String {
    val g = p.optString("gender")
    return when (p.optString("relation")) {
        "head" -> "رب الأسرة"; "wife" -> "الزوجة"; "husband" -> "الزوج"
        "orphan" -> if (g == "female") "يتيمة في رعاية الأسرة" else "يتيم في رعاية الأسرة"
        else -> when (p.optString("memberRelation")) {
            "son" -> "ابن"; "daughter" -> "ابنة"; "nephew" -> "ابن أخ/عم"; "niece" -> "ابنة أخ/عم"
            else -> if (p.optString("relation") == "child") (if (g == "female") "ابنة" else if (g == "male") "ابن" else "ابن/ابنة") else p.optString("relationDetail").ifEmpty { "فرد من الأسرة" }
        }
    }
}

private fun fmtDate(ms: Long): String = if (ms <= 0) "—" else SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(ms))

/** buildPersonDetailRows في الويب بنفس الترتيب والشروط. */
private fun detailRows(p: JSONObject): List<Pair<String, String>> = buildList {
    val age = Stats.ageOf(p)
    val g = p.optString("gender")
    add("الاسم الكامل" to p.optString("fullName"))
    p.optString("nationalId").takeIf { it.isNotEmpty() }?.let { add("رقم الهوية" to it) }
    p.optString("birthDate").takeIf { it.isNotEmpty() }?.let { add("تاريخ الميلاد" to it) }
    if (age != null) add("العمر" to "$age سنة")
    if (g.isNotEmpty() && g != "unknown") add("الجنس" to if (g == "male") "ذكر" else "أنثى")
    val rel = if (p.optString("relation").isNotEmpty()) relationOf(p) else if (p.optBoolean("isFamilyHead")) "رب الأسرة" else ""
    if (rel.isNotEmpty()) add("العلاقة بالأسرة" to rel)
    p.optString("familyId").takeIf { it.isNotEmpty() }?.let { add("رقم الأسرة" to it) }
    p.optString("phone").takeIf { it.isNotEmpty() }?.let { add("رقم الهاتف" to it) }
    p.optString("address").takeIf { it.isNotEmpty() }?.let { add("العنوان" to it) }
    p.optString("maritalStatus").takeIf { it.isNotEmpty() }?.let { add("الحالة الاجتماعية" to it) }
    if (g == "female" && p.optBoolean("isPregnant")) add("الحالة" to "حامل")
    p.optJSONObject("pregnancy")?.optString("due")?.takeIf { p.optBoolean("isPregnant") && it.isNotEmpty() }?.let { add("موعد الولادة المتوقع" to it) }
    val tags = p.strs("tags")
    if ("orphan" in tags) {
        val oi = p.optJSONObject("tagData")?.optJSONObject("orphan") ?: JSONObject()
        add("اليُتم" to (orphanTypes[oi.optString("type")] ?: orphanTypes.getValue("father")) + if (oi.optBoolean("soleSurvivor")) " — الناجي الوحيد" else "")
        oi.optString("guardianName").takeIf { it.isNotEmpty() }?.let { add("الوصي" to it + if (oi.optString("guardianRel").isNotEmpty()) " (${oi.optString("guardianRel")})" else "") }
        oi.optString("guardianNid").takeIf { it.isNotEmpty() }?.let { add("هوية الوصي" to it) }
    }
    val other = tags.filter { it != "orphan" }
    if (other.isNotEmpty()) add("التصنيفات" to other.joinToString("، ") { tagNames[it] ?: if (it.startsWith("c:")) it.drop(2) else it })
    if (g == "female" && p.optBoolean("isBreastfeeding")) add("الحالة" to "مرضعة")
    if (p.optBoolean("hasChronicDisease")) {
        val d = listOfNotNull(p.strs("chronicConditions").mapNotNull { chronicNames[it] }.joinToString("، ").takeIf { it.isNotEmpty() }, p.optString("diseaseDetails").takeIf { it.isNotEmpty() }).joinToString(" — ")
        add("مرض مزمن" to d.ifEmpty { "نعم" })
    }
    if (p.optBoolean("hasDisability")) add("إعاقة" to p.optString("disabilityDetails").ifEmpty { "نعم" })
    p.optString("notes").takeIf { it.isNotEmpty() }?.let { add("ملاحظات" to it) }
    if (p.optBoolean("isFamilyHead")) Vuln.effectiveShelter(p).takeIf { it.isNotEmpty() }?.let { add("نوع المأوى" to (shelterNames[it] ?: it) + if (p.optString("shelterType").isEmpty()) " (مستنتج من الوصف)" else "") }
    PFields.living(p).takeIf { it.isNotEmpty() }?.let { add("الوضع المعيشي" to it) }
    if (p.optBoolean("urgentNeed")) add("احتياج طارئ" to p.optString("urgentNeedDetails").ifEmpty { "نعم" })
    PFields.phone2(p).takeIf { it.isNotEmpty() }?.let { add("هاتف إضافي" to it) }
    p.optJSONObject("customFields")?.optString("حالة الزوج/ة")?.takeIf { it.isNotEmpty() }?.let { add("حالة الزوج/ة" to it) }
}

/** criticalBadgesRow في الويب. */
@Composable
private fun CriticalBadges(m: JSONObject) {
    val p = LocalPalette.current
    val a = Stats.ageOf(m)
    val ms = m.optString("maritalStatus")
    val items = buildList<Pair<String, androidx.compose.ui.graphics.Color>> {
        if (a != null && a < 1) add("رضيع" to p.danger)
        if (m.optBoolean("isPregnant")) add("حامل" to p.pregnant)
        if (m.optBoolean("isBreastfeeding")) add("مرضعة" to p.purple)
        if (m.optBoolean("hasChronicDisease")) add("مرض مزمن" to p.warn)
        if (a != null && a >= 60) add("كبير سن" to p.purple)
        if (m.optBoolean("hasDisability")) add("إعاقة" to p.blue)
        if (ms.contains("أرمل")) add("أرمل/ة" to p.gray)
        if (ms.contains("مطلق")) add("مطلّق/ة" to p.gray)
        if (m.optString("nationalId").isEmpty()) add("بلا هوية" to p.danger)
        if (m.optString("phone").isEmpty()) add("بلا هاتف" to p.gray)
    }
    if (items.isNotEmpty()) FlowRow(Modifier.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { items.forEach { (t, c) -> Pill(t, c) } }
}

@Composable
private fun DetailRow(k: String, v: String) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(k, color = p.mute, fontSize = 13.sp, modifier = Modifier.weight(.45f))
        Text(v, color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(.55f))
    }
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(p.line))
}

/** نافذة تفاصيل الفرد (openPersonDetail): الأقسام بترتيب الويب وأزرار الإجراءات حسب الدور. */
@Composable
internal fun PersonDetailBody(
    sel: JSONObject, family: List<JSONObject>, role: String, uid: String,
    onOpenMember: (JSONObject) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onTransfer: (() -> Unit)?,
    onSave: (JSONObject) -> Unit, onAid: () -> Unit, loadCreds: (JSONObject) -> Pair<String, String>?, loadAudit: suspend (JSONObject) -> List<AuditEntry>,
) {
    val p = LocalPalette.current
    val staff = role in listOf("admin", "delegate", "dataentry")
    val canEdit = staff
    val canDelete = role == "admin" || role == "delegate"
    val head = sel.optBoolean("isFamilyHead")
    var creds by remember(sel.optString("id")) { mutableStateOf<Pair<String, String>?>(null) }
    var showPw by remember(sel.optString("id")) { mutableStateOf(false) }
    var audit by remember(sel.optString("id")) { mutableStateOf<List<AuditEntry>?>(null) }
    LaunchedEffect(sel.optString("id")) {
        if (head && (role == "admin" || role == "delegate") && sel.optString("nationalId").isNotEmpty()) creds = loadCreds(sel)
        audit = runCatching { loadAudit(sel) }.getOrDefault(emptyList())
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        CriticalBadges(sel)
        if (staff && head) {
            val v = remember(family, sel) { Vuln.compute(listOf(sel) + family) }
            val tc = when (v.tier) { "critical" -> p.danger; "high" -> p.tierHigh; "medium" -> p.tierMedium; else -> p.tierStable }
            QCard(Modifier.padding(bottom = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.foundation.layout.Box(Modifier.size(56.dp).clip(CircleShape).background(tc.copy(alpha = .16f)), contentAlignment = Alignment.Center) { Text("${v.score}", color = tc, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }
                    Column { Text("مؤشر الهشاشة", color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold); Pill(Vuln.tiers.first { it.first == v.tier }.second, tc) }
                }
                if (v.factors.isEmpty()) Text("لا توجد عوامل هشاشة مسجّلة لهذه الأسرة", color = p.mute, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                else v.factors.forEach { (l, pts) -> Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(l, color = p.ink, fontSize = 13.sp); Text("+$pts", color = tc, fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
            }
        }
        creds?.let { c ->
            QCard(Modifier.padding(bottom = 14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(QIcons.Key, null, Modifier.size(18.dp), tint = p.pri)
                    Text("حساب الدخول", color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                DetailRow("اسم المستخدم", c.first)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (showPw) c.second else "••••••••", color = p.ink, fontSize = 14.sp)
                    Text(if (showPw) "إخفاء" else "إظهار", color = p.pri, fontSize = 13.sp, modifier = Modifier.heightIn(min = 44.dp).clickable { showPw = !showPw }.padding(horizontal = 8.dp, vertical = 12.dp))
                }
            }
        }
        detailRows(sel).forEach { (k, v) -> DetailRow(k, v) }

        val reqs = Aid.requests(sel).sortedByDescending { it.optLong("createdAt", it.optLong("updatedAt")) }
        if (reqs.isNotEmpty()) {
            SectionTitle("طلبات الأسرة")
            reqs.forEach { r ->
                val st = r.optString("status")
                val c = when (reqStatusColor[st]) { "w" -> p.warn; "s" -> p.success; "d" -> p.danger; else -> p.gray }
                QCard(Modifier.padding(bottom = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(Aid.reqTypes.firstOrNull { it.first == r.optString("type") }?.second ?: r.optString("type"), color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Pill(Aid.reqStatus[st] ?: st, c)
                    }
                    r.optString("details").takeIf { it.isNotEmpty() }?.let { Text(it, color = p.mute, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
                    r.optString("reply").takeIf { it.isNotEmpty() }?.let { Text(it, color = p.mute, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
                    Text(fmtDate(r.optLong("createdAt")), color = p.mute, fontSize = 11.5.sp, modifier = Modifier.padding(top = 6.dp))
                    if ((role == "admin" || role == "delegate") && st in listOf("pending", "approved")) FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (st == "pending") Btn("قبول", { onSave(Aid.setRequestStatus(sel, r.optString("id"), "approved", uid)) }, small = true)
                        Btn("تم التسليم", { onSave(Aid.setRequestStatus(sel, r.optString("id"), "done", uid)) }, kind = BtnKind.Outline, small = true)
                        if (st == "pending") Btn("رفض", { onSave(Aid.setRequestStatus(sel, r.optString("id"), "rejected", uid)) }, kind = BtnKind.Danger, small = true)
                    }
                }
            }
        }

        if (staff) {
            SectionTitle("المساعدات") { if (canEdit) Btn("تسجيل مساعدة", onAid, kind = BtnKind.Outline, icon = QIcons.Plus, small = true) }
            val elig = Aid.types.filter { it.k != "other" && it.k != "clothes" && Aid.eligible(sel, it.k) }
            if (elig.isNotEmpty()) Text("مؤهّل لـ: " + elig.joinToString("، ") { it.label }, color = p.mute, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            val items = Aid.list(sel).sortedByDescending { it.optLong("at") }
            if (items.isEmpty()) Text("لم تُسجَّل مساعدات لهذا الفرد بعد", color = p.mute, fontSize = 12.sp)
            items.take(8).forEach { r ->
                val t = Aid.types.firstOrNull { it.k == r.optString("t") }
                val q = r.optDouble("q", 1.0)
                DetailRow((t?.label ?: r.optString("t")) + r.optString("n").let { if (it.isNotEmpty()) " — $it" else "" }, r.optString("d") + if (q != 1.0) " · " + (if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()) else "")
            }
            if (items.size > 8) Text("+ ${items.size - 8} سجلات أقدم", color = p.mute, fontSize = 12.sp)
        }

        SectionTitle("أفراد الأسرة")
        if (family.isEmpty()) Text("لا يوجد أفراد آخرون مسجَّلون بهذه الأسرة", color = p.mute, fontSize = 12.sp)
        family.forEach { f ->
            val fem = f.optString("gender") == "female"; val mal = f.optString("gender") == "male"
            val gc = if (fem) p.pregnant else if (mal) p.blue else p.gray
            QCard(Modifier.padding(bottom = 6.dp), { onOpenMember(f) }, 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.foundation.layout.Box(Modifier.size(38.dp).clip(CircleShape).background(gc.copy(alpha = .16f)), contentAlignment = Alignment.Center) { Icon(if (fem) QIcons.Female else if (mal) QIcons.Male else QIcons.Person, null, Modifier.size(20.dp), tint = gc) }
                    Column(Modifier.weight(1f)) {
                        Text(f.optString("fullName"), color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(relationOf(f) + (Stats.ageOf(f)?.let { " · $it سنة" } ?: ""), color = p.mute, fontSize = 12.sp)
                    }
                }
            }
        }

        SectionTitle("سجل التعديلات")
        val au = audit
        if (au == null) Text("جارٍ التحميل…", color = p.mute, fontSize = 12.sp)
        else if (au.isEmpty()) Text("لا يوجد سجل تعديلات لهذا الفرد بعد", color = p.mute, fontSize = 12.sp)
        else au.forEach { e ->
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(dev.saged.grcs.data.Audit.labels[e.action] ?: e.action, color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(fmtDate(e.at), color = p.mute, fontSize = 11.5.sp)
                }
                Text(e.userName.ifEmpty { "—" }, color = p.mute, fontSize = 12.sp)
            }
        }

        FlowRow(Modifier.padding(top = 16.dp, bottom = 80.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canEdit) Btn("تعديل", onEdit, kind = BtnKind.Outline, icon = QIcons.Edit)
            if (role == "admin" && onTransfer != null) Btn("نقل الأسرة", onTransfer, kind = BtnKind.Outline, icon = QIcons.Transfer)
            if (canDelete) Btn("حذف", onDelete, kind = BtnKind.Danger, icon = QIcons.Trash)
        }
    }
}
