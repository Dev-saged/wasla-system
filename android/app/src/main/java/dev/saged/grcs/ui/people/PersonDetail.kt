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
import dev.saged.grcs.ui.icons.Fa
import dev.saged.grcs.ui.components.cardShadow
import androidx.compose.foundation.border
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
    val items = buildList<Triple<String, androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.vector.ImageVector>> {
        if (a != null && a < 1) add(Triple("رضيع", p.danger, Fa.Baby))
        if (m.optBoolean("isPregnant")) add(Triple("حامل", p.pregnant, Fa.BabyCarriage))
        if (m.optBoolean("isBreastfeeding")) add(Triple("مرضعة", p.purple, Fa.Baby))
        if (m.optBoolean("hasChronicDisease")) add(Triple("مرض مزمن", p.warn, Fa.NotesMedical))
        if (a != null && a >= 60) add(Triple("كبير سن", p.purple, Fa.PersonCane))
        if (m.optBoolean("hasDisability")) add(Triple("إعاقة", p.blue, Fa.Wheelchair))
        if (ms.contains("أرمل")) add(Triple("أرمل/ة", p.gray, Fa.Ring))
        if (ms.contains("مطلق") || ms.contains("مطلّق")) add(Triple("مطلّق/ة", p.gray, Fa.HeartCrack))
        if (m.optString("nationalId").isEmpty()) add(Triple("بلا هوية", p.danger, Fa.IdCardClip))
        if (m.optString("phone").isEmpty()) add(Triple("بلا هاتف", p.gray, Fa.PhoneSlash))
    }
    if (items.isNotEmpty()) FlowRow(Modifier.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { items.forEach { (t, c, ic) -> Pill(t, c, icon = ic) } }
}

/** شارة مستوى الهشاشة داخل بطاقة المؤشر. */
@Composable
private fun TierChipFlat(tier: String) {
    val p = LocalPalette.current
    val c = when (tier) { "critical" -> p.acc; "high" -> p.tierHigh; "medium" -> p.tierMedium; else -> p.tierStable }
    Text(Vuln.tiers.firstOrNull { it.first == tier }?.second ?: tier, color = if (tier == "critical") androidx.compose.ui.graphics.Color(0xFF2E1F02) else c, fontSize = 10.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.clip(CircleShape).background(if (tier == "critical") c else c.copy(alpha = .16f)).padding(horizontal = 8.dp, vertical = 1.dp))
}

/** صف تفصيل (detail-row): تسمية رمادية وقيمة عريضة في الطرف، وخط سفلي. */
@Composable
private fun DetailRow(k: String, v: String) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(k, color = p.mute, fontSize = 14.sp)
        Text(v, color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(p.line))
}

/** نافذة تفاصيل الفرد (openPersonDetail): الأقسام بترتيب الويب وأزرار الإجراءات حسب الدور. */
@Composable
internal fun PersonDetailBody(
    sel: JSONObject, family: List<JSONObject>, role: String, uid: String,
    onOpenMember: (JSONObject) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onTransfer: (() -> Unit)?,
    onSave: (JSONObject) -> Unit, onAid: () -> Unit, onTags: () -> Unit, loadCreds: (JSONObject) -> Pair<String, String>?, loadAudit: suspend (JSONObject) -> List<AuditEntry>,
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
    Column(Modifier.fillMaxWidth()) {
        CriticalBadges(sel)
        if (staff && head) {
            val v = remember(family, sel) { Vuln.compute(listOf(sel) + family) }
            val tc = when (v.tier) { "critical" -> p.acc; "high" -> p.tierHigh; "medium" -> p.tierMedium; else -> p.tierStable }
            val box = RoundedCornerShape(16.dp)
            Column(Modifier.padding(bottom = 14.dp).fillMaxWidth().clip(box).background(if (v.tier == "critical") p.acc.copy(alpha = .14f) else p.surface2).border(1.dp, if (v.tier == "critical") p.acc.copy(alpha = .55f) else p.line, box).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    androidx.compose.foundation.layout.Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.Canvas(Modifier.size(58.dp)) {
                            drawCircle(p.track)
                            drawArc(tc, -90f, 3.6f * v.score, true)
                            drawCircle(p.surface, radius = size.minDimension / 2 - 6.dp.toPx())
                        }
                        Text("${v.score}", color = p.head, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Text("مؤشر الهشاشة", color = p.mute, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                        androidx.compose.foundation.layout.Box(Modifier.padding(start = 0.dp)) { TierChipFlat(v.tier) }
                    }
                }
                if (v.factors.isEmpty()) Text("لا توجد عوامل هشاشة مسجّلة لهذه الأسرة", color = p.mute, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                else Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    v.factors.forEach { (l, pts) -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(p.surface).padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(l, color = p.ink, fontSize = 13.sp); Text("+$pts", color = p.head, fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
                }
            }
        }
        creds?.let { c ->
            val box = RoundedCornerShape(14.dp)
            Column(Modifier.padding(bottom = 14.dp).fillMaxWidth().clip(box).background(p.surface2).border(1.dp, p.line, box).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Fa.UserLock, null, Modifier.size(17.dp), tint = p.pri)
                    Text("حساب الدخول", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Pill("مفعّل", p.success)
                }
                DetailRow("اسم المستخدم", c.first)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val pill = RoundedCornerShape(50)
                    Row(Modifier.weight(1f).heightIn(min = 34.dp).clip(pill).background(p.pri.copy(alpha = .1f)).border(1.dp, p.pri.copy(alpha = .45f), pill).clickable { showPw = !showPw }.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(if (showPw) Fa.EyeSlash else Fa.Key, null, Modifier.size(12.dp), tint = if (p.dark) p.tealL else p.tealD)
                        Text(if (showPw) c.second else "إظهار كلمة المرور", color = if (p.dark) p.tealL else p.tealD, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
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
        if (family.isEmpty()) Text("لا يوجد أفراد آخرون مسجَّلون بهذه الأسرة", color = p.mute, fontSize = 13.sp)
        family.forEach { f ->
            val fem = f.optString("gender") == "female"; val mal = f.optString("gender") == "male"
            val grad = if (mal) listOf(p.blue, androidx.compose.ui.graphics.Color(0xFF1D4ED8)) else if (fem) listOf(androidx.compose.ui.graphics.Color(0xFFDB2777), androidx.compose.ui.graphics.Color(0xFF9D174D)) else listOf(p.mute, androidx.compose.ui.graphics.Color(0xFF475569))
            Row(Modifier.padding(bottom = 6.dp).fillMaxWidth().cardShadow().clip(RoundedCornerShape(14.dp)).background(p.surface).clickable { onOpenMember(f) }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(grad)), contentAlignment = Alignment.Center) { Icon(if (fem) Fa.Venus else if (mal) Fa.Mars else Fa.Question, null, Modifier.size(19.dp), tint = androidx.compose.ui.graphics.Color.White) }
                Column(Modifier.weight(1f)) {
                    Text(f.optString("fullName"), color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(relationOf(f) + (Stats.ageOf(f)?.let { " · $it سنة" } ?: ""), color = p.mute, fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        SectionTitle("سجل التعديلات")
        val au = audit
        if (au == null) Text("جارٍ التحميل…", color = p.mute, fontSize = 13.sp)
        else if (au.isEmpty()) Text("لا يوجد سجل تعديلات لهذا الفرد بعد", color = p.mute, fontSize = 13.sp)
        else au.forEach { e ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(dev.saged.grcs.data.Audit.labels[e.action] ?: e.action, color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(fmtDate(e.at), color = p.mute, fontSize = 13.sp)
                }
                Text(e.userName.ifEmpty { "—" }, color = p.mute, fontSize = 13.sp)
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(p.line))
        }

        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (canEdit) Btn("تعديل", onEdit, Modifier.weight(1f), kind = BtnKind.Outline, icon = Fa.Pen)
            if (canEdit) Btn("التصنيفات", onTags, Modifier.weight(1f), kind = BtnKind.Outline, icon = Fa.Tags)
            if (role == "admin" && onTransfer != null) Btn("نقل الأسرة", onTransfer, Modifier.weight(1f), kind = BtnKind.Outline, icon = Fa.RightLeft)
            if (canDelete) Btn("حذف", onDelete, Modifier.weight(1f), kind = BtnKind.Danger, icon = Fa.Trash)
        }
    }
}
