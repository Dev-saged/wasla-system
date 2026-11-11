package dev.saged.grcs.ui.people

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.data.Clock
import dev.saged.grcs.data.Derive
import dev.saged.grcs.data.FormResult
import dev.saged.grcs.data.GenderInfer
import dev.saged.grcs.data.Merge
import dev.saged.grcs.data.Nid
import dev.saged.grcs.data.PFields
import dev.saged.grcs.data.Rel
import dev.saged.grcs.data.Stats
import dev.saged.grcs.data.Vuln
import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.DateField
import dev.saged.grcs.ui.components.FilterLabelPlain
import dev.saged.grcs.ui.components.QCard
import dev.saged.grcs.ui.components.QField
import dev.saged.grcs.ui.components.QSelect
import dev.saged.grcs.ui.components.QSwitch
import dev.saged.grcs.ui.components.SegBtns
import dev.saged.grcs.ui.icons.Fa
import dev.saged.grcs.ui.theme.LocalPalette
import org.json.JSONArray
import org.json.JSONObject

internal val shelterTypes = listOf("tent_ok" to "خيمة سليمة", "tent_damaged" to "خيمة تالفة", "shared" to "مأوى مشترك", "building_damaged" to "مبنى متضرر", "none" to "بلا مأوى")
internal val chronicConditions = listOf("heart" to "قلب", "diabetes" to "سكري", "hypertension" to "ضغط الدم", "kidney" to "كلى", "cancer" to "سرطان", "respiratory" to "ربو / جهاز تنفسي",
    "liver" to "كبد", "thyroid" to "غدة درقية", "mental" to "أمراض نفسية", "blood" to "أمراض دم", "neuro" to "أمراض عصبية", "bone" to "مفاصل / عظام", "other" to "أخرى")
private val maritalKeys = listOf("married" to "متزوج/ة", "widowed" to "أرمل/ة", "divorced" to "مطلق/ة", "single" to "أعزب")

/** حالة حقل هوية واحد (NidField): الرقم، «لا هوية بعد»، القيمة الأصلية، ومعرّف صاحب السجل. */
private class NidState(orig: String, none: Boolean, val self: String) {
    val orig = orig
    var value by mutableStateOf(orig)
    var none by mutableStateOf(orig.isEmpty() && none)
    val nid get() = Nid.norm(value).let { if (it.isNotEmpty() && it.all { c -> c in '0'..'9' }) it else "" }
}

private class ChildRow(val id: String?, name: String, nid: String, noId: Boolean, dob: String, note: String, rel: String) {
    var name by mutableStateOf(name); var dob by mutableStateOf(dob); var note by mutableStateOf(note); var rel by mutableStateOf(rel)
    val nid = NidState(nid, noId, id.orEmpty())
}

/** نموذج الفرد — نقل مباشر لـopenPersonForm وحفظه في الويب: رب الأسرة مع الزوج/ة وأفراد الأسرة في نموذج واحد. */
@Composable
internal fun PersonForm(src: JSONObject, all: List<JSONObject>, campId: String, uid: String, serverErr: String?, onBack: () -> Unit, onSave: (FormResult) -> Unit) {
    val p = LocalPalette.current
    val editing = src.optString("id").isNotEmpty()
    var name by remember { mutableStateOf(src.optString("fullName")) }
    val nid = remember { NidState(src.optString("nationalId"), src.optBoolean("noIdYet"), src.optString("id")) }
    var dob by remember { mutableStateOf(src.optString("birthDate").takeIf { it != "null" }.orEmpty()) }
    var gender by remember { mutableStateOf(src.optString("gender")) }
    var head by remember { mutableStateOf(if (editing) src.optBoolean("isFamilyHead") else true) }
    var phone by remember { mutableStateOf(src.optString("phone")) }
    var phone2 by remember { mutableStateOf(src.optJSONObject("customFields")?.optString("هاتف إضافي").orEmpty()) }
    var address by remember { mutableStateOf(src.optString("address")) }
    var preg by remember { mutableStateOf(src.optBoolean("isPregnant")) }
    var bf by remember { mutableStateOf(src.optBoolean("isBreastfeeding")) }
    var chronic by remember { mutableStateOf(src.optBoolean("hasChronicDisease")) }
    val conds = remember { mutableStateListOf<String>().also { l -> src.optJSONArray("chronicConditions")?.let { a -> for (i in 0 until a.length()) l.add(a.optString(i)) } } }
    var diseaseDetails by remember { mutableStateOf(src.optString("diseaseDetails")) }
    var dis by remember { mutableStateOf(src.optBoolean("hasDisability")) }
    var disDetails by remember { mutableStateOf(src.optString("disabilityDetails")) }
    var notes by remember { mutableStateOf(src.optString("notes")) }
    var shelter by remember { mutableStateOf(if (editing && src.optBoolean("isFamilyHead")) Vuln.effectiveShelter(src) else "") }
    var ms by remember { mutableStateOf(maritalKeys.firstOrNull { it.second == src.optString("maritalStatus") }?.first ?: src.optString("maritalStatus").ifEmpty { "married" }) }

    // أفراد الأسرة الحاليون: الزوج/ة ثم الباقون كصفوف قابلة للتعديل والحذف
    val members = remember { if (editing && src.optBoolean("isFamilyHead") && src.optString("familyId").isNotEmpty()) all.filter { it.optString("familyId") == src.optString("familyId") && it.optString("id") != src.optString("id") && it.optString("campId").let { c -> c.isEmpty() || c == src.optString("campId") } } else emptyList() }
    val spouse0 = remember { members.firstOrNull { it.optString("relation").let { r -> r == "wife" || r == "husband" } } ?: members.firstOrNull { !it.optBoolean("isFamilyHead") && !Rel.isChild(it) && (Stats.ageOf(it)?.let { a -> a >= 15 } ?: true) } }
    val spouseShown0 = remember { src.optBoolean("isFamilyHead") && ms == "married" }
    val spouse = if (spouseShown0) spouse0 else null
    var sName by remember { mutableStateOf(spouse?.optString("fullName").orEmpty()) }
    val sNid = remember { NidState(spouse?.optString("nationalId").orEmpty(), spouse?.optBoolean("noIdYet") == true, spouse?.optString("id").orEmpty()) }
    var sDob by remember { mutableStateOf(spouse?.optString("birthDate")?.takeIf { it != "null" }.orEmpty()) }
    var sPreg by remember { mutableStateOf(spouse?.optBoolean("isPregnant") == true) }
    var sBf by remember { mutableStateOf(spouse?.optBoolean("isBreastfeeding") == true) }
    val kids = remember {
        mutableStateListOf<ChildRow>().also { l ->
            members.filter { it.optString("id") != spouse?.optString("id") && it.optString("relation").let { r -> r != "wife" && r != "husband" } }.sortedBy { it.optInt("orderIndex", 99) }.forEach { c ->
                val rv = c.optString("memberRelation").ifEmpty { if (c.optString("relation") == "orphan") "orphan" else c.optString("relation").takeIf { it in listOf("son", "daughter", "nephew", "niece") } ?: if (c.optString("gender") == "female") "daughter" else "son" }
                l.add(ChildRow(c.optString("id"), c.optString("fullName"), c.optString("nationalId"), c.optBoolean("noIdYet"), c.optString("birthDate").takeIf { it != "null" }.orEmpty(), c.optString("notes"), rv))
            }
        }
    }
    val removedIds = remember { mutableStateListOf<String>() }
    var err by remember { mutableStateOf<String?>(null) }

    val age = Stats.ageOfDob(dob)
    val maternal = gender == "female" && (age == null || age >= 18)
    val spouseGender = if (gender == "male") "female" else if (gender == "female") "male" else ""
    val sAge = Stats.ageOfDob(sDob)
    val sMaternal = spouseGender == "female" && (sAge == null || sAge >= 18)
    val spouseCard = head && ms == "married"
    val byNid = remember(all) { all.filter { it.optString("nationalId").isNotEmpty() }.associateBy { Nid.norm(it.optString("nationalId")) } }

    /** NidField.validate: يعيد نص الخطأ أو null، ويعدّ السجلات القديمة بلا هوية التي لم تحجب الحفظ. */
    var skipped = 0
    fun validateNids(rows: List<Triple<NidState, String, Boolean>>): String? {
        val seen = HashSet<String>(); skipped = 0
        for ((st, label, legacy) in rows) {
            val n = st.nid; val none = st.none && n.isEmpty(); val who = if (label.isNotEmpty()) " ($label)" else ""
            if (n.isEmpty() && !none && legacy && st.orig.isEmpty()) { skipped++; continue }
            if (n.isEmpty() && !none) return "رقم الهوية مطلوب$who — أدخله أو حدّد «لا يوجد رقم هوية بعد»"
            if (n.isEmpty()) continue
            if (Nid.status(n) == "format") return "رقم الهوية$who يجب أن يكون 9 أرقام"
            val dup = byNid[n]
            if (dup != null && dup.optString("id") != st.self) return "رقم الهوية$who مسجّل مسبقاً: ${dup.optString("fullName")}"
            if (!seen.add(n)) return "رقم الهوية$who مكرر داخل النموذج نفسه"
        }
        return null
    }

    fun submit() {
        val fullName = name.trim()
        if (fullName.isEmpty()) { err = "الاسم الكامل مطلوب"; return }
        if (campId.isEmpty()) { err = "لا يوجد مخيم محدد لإضافة الفرد إليه"; return }
        val g = gender.ifEmpty { "unknown" }
        val rows = ArrayList<Triple<NidState, String, Boolean>>()
        rows += Triple(nid, if (head) "رب الأسرة" else "الفرد", false)
        if (spouseCard && sName.isNotBlank()) rows += Triple(sNid, "الزوج/ة", spouse != null)
        if (head) kids.filter { it.name.isNotBlank() }.forEach { rows += Triple(it.nid, it.name.trim().split(Regex("\\s+"))[0], it.id != null) }
        validateNids(rows)?.let { err = it; return }
        err = null
        val now = Clock.now()
        val person = if (editing) Merge.copy(src) else JSONObject().put("id", java.util.UUID.randomUUID().toString()).put("campId", campId).put("createdAt", now).put("createdBy", uid)
        val cf = person.optJSONObject("customFields")?.let { JSONObject(it.toString()) } ?: JSONObject()
        if (phone2.isNotBlank()) cf.put("هاتف إضافي", phone2.trim()) else cf.remove("هاتف إضافي")
        val nationalId = nid.nid
        val familyId = person.optString("familyId").ifEmpty { if (head) nationalId.ifEmpty { person.optString("id") } else "" }
        val marital = maritalKeys.firstOrNull { it.first == ms }?.second ?: person.optString("maritalStatus")
        person.put("fullName", fullName).put("nationalId", nationalId)
            .put("birthDate", if (dob.isEmpty()) JSONObject.NULL else dob).put("calculatedAge", Stats.ageOfDob(dob) ?: JSONObject.NULL).put("gender", g)
            .put("isPregnant", g == "female" && maternal && preg).put("isBreastfeeding", g == "female" && maternal && bf)
            .put("hasChronicDisease", chronic).put("chronicConditions", JSONArray(conds.toList())).put("diseaseDetails", diseaseDetails.trim())
            .put("hasDisability", dis).put("disabilityDetails", disDetails.trim())
            .put("isFamilyHead", head).put("phone", if (head) phone.trim() else person.optString("phone")).put("address", address.trim()).put("notes", notes.trim())
            .put("familyId", familyId).put("relation", person.optString("relation").ifEmpty { if (head) "head" else "" })
            .put("shelterType", if (head) shelter else person.optString("shelterType")).put("maritalStatus", if (head) marital else person.optString("maritalStatus"))
            .put("customFields", cf).put("updatedAt", now).put("updatedBy", uid)
        if (nid.none && nationalId.isEmpty()) person.put("noIdYet", true) else person.remove("noIdYet")

        val others = ArrayList<JSONObject>(); val removed = ArrayList<JSONObject>()
        if (head) {
            // الزوج/ة — إنشاء أو تحديث، أو حذف إن تُرك الاسم فارغاً
            if (spouseCard && sName.isNotBlank()) {
                val sg = spouseGender.ifEmpty { GenderInfer.infer(sName.trim()) }
                val sp = spouse?.let { Merge.copy(it) } ?: JSONObject().put("id", java.util.UUID.randomUUID().toString()).put("campId", campId).put("createdAt", now).put("createdBy", uid)
                sp.put("fullName", sName.trim()).put("nationalId", sNid.nid).put("birthDate", if (sDob.isEmpty()) JSONObject.NULL else sDob).put("calculatedAge", Stats.ageOfDob(sDob) ?: JSONObject.NULL)
                    .put("gender", sg).put("isFamilyHead", false).put("relation", sp.optString("relation").ifEmpty { "wife" }).put("familyId", familyId).put("campId", campId)
                    .put("phone", sp.optString("phone")).put("address", address.trim()).put("notes", sp.optString("notes"))
                    .put("hasChronicDisease", sp.optBoolean("hasChronicDisease")).put("chronicConditions", sp.optJSONArray("chronicConditions") ?: JSONArray()).put("diseaseDetails", sp.optString("diseaseDetails"))
                    .put("hasDisability", sp.optBoolean("hasDisability")).put("disabilityDetails", sp.optString("disabilityDetails"))
                    .put("isPregnant", sg == "female" && sMaternal && sPreg).put("isBreastfeeding", sg == "female" && sMaternal && sBf)
                    .put("maritalStatus", person.optString("maritalStatus")).put("customFields", sp.optJSONObject("customFields") ?: JSONObject()).put("updatedAt", now).put("updatedBy", uid)
                if (sNid.none && sNid.nid.isEmpty()) sp.put("noIdYet", true) else sp.remove("noIdYet")
                others += sp
            } else if (spouse != null && spouseCard) removed += spouse
            // أفراد الأسرة — إنشاء/تحديث لكل صف فيه اسم، وحذف من أُزيل من القائمة
            removedIds.forEach { id -> members.firstOrNull { it.optString("id") == id }?.let { removed += it } }
            var ci = 0
            for (c in kids) {
                if (c.name.isBlank()) continue
                val old = c.id?.let { id -> members.firstOrNull { it.optString("id") == id } }
                val ch = old?.let { Merge.copy(it) } ?: JSONObject().put("id", java.util.UUID.randomUUID().toString()).put("campId", campId).put("createdAt", now).put("createdBy", uid)
                val cg = Rel.genderOf(c.rel).ifEmpty { ch.optString("gender").takeIf { it.isNotEmpty() && it != "unknown" } ?: GenderInfer.infer(c.name.trim()) }
                ch.put("fullName", c.name.trim()).put("nationalId", c.nid.nid).put("birthDate", if (c.dob.isEmpty()) JSONObject.NULL else c.dob).put("calculatedAge", Stats.ageOfDob(c.dob) ?: JSONObject.NULL)
                    .put("gender", cg).put("isFamilyHead", false).put("relation", if (c.rel == "orphan") "orphan" else "child").put("familyId", familyId).put("campId", campId)
                    .put("phone", "").put("address", address.trim()).put("notes", c.note.trim())
                    .put("hasChronicDisease", ch.optBoolean("hasChronicDisease")).put("diseaseDetails", ch.optString("diseaseDetails"))
                    .put("hasDisability", ch.optBoolean("hasDisability")).put("disabilityDetails", ch.optString("disabilityDetails"))
                    .put("isPregnant", false).put("isBreastfeeding", false).put("maritalStatus", "").put("orderIndex", 2 + ci)
                    .put("customFields", ch.optJSONObject("customFields") ?: JSONObject()).put("updatedAt", now).put("updatedBy", uid).put("memberRelation", c.rel)
                if (c.nid.nid.isNotEmpty()) ch.remove("noIdYet") else if (c.nid.none) ch.put("noIdYet", true)
                others += ch; ci++
            }
        }
        onSave(FormResult(person, others, removed, !editing, skipped))
    }

    dev.saged.grcs.ui.shell.Immersive()
    Column(Modifier.fillMaxSize().imePadding()) {
        AppHeader(if (editing) "تعديل بيانات فرد" else "إضافة فرد جديد", "", conn = null, onBack = onBack, backIcon = Fa.Xmark)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card("البيانات الأساسية", Fa.IdCard) {
                QField("الاسم الكامل *", name, { name = it })
                NidInput("رقم الهوية *", nid, byNid)
                DateField("تاريخ الميلاد", dob, { dob = it })
                Column {
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("العمر المحسوب", color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Text(age?.let { "$it سنة" } ?: "—", color = p.mute, fontSize = 13.sp)
                    }
                    AgeChips(dob, gender, if (head) "head" else src.optString("relation"), head)
                }
                Column {
                    Text("الجنس", color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
                    GenderSeg(gender) { gender = it }
                }
            }
            Card("الأسرة", Fa.HouseChimney) {
                IconSwitch("رب الأسرة", Fa.Crown, head) { head = it }
                if (head) {
                    QSelect(shelter, listOf("" to "غير محدد") + shelterTypes, label = "نوع المأوى") { shelter = it }
                    Column {
                        FilterLabelPlain("الحالة الاجتماعية", Fa.Ring)
                        val icons = listOf(Fa.Heart, Fa.UserShield, Fa.Ring, Fa.User)
                        SegBtns(maritalKeys.mapIndexed { i, m -> m.second to icons[i] }, maritalKeys.indexOfFirst { it.first == ms }) { ms = maritalKeys[it].first }
                    }
                }
            }
            if (spouseCard) Card("الزوج / الزوجة", Fa.PeoplePulling, "(اختياري)") {
                QField("الاسم الكامل", sName, { sName = it }, placeholder = "اسم الزوج/ة رباعي")
                NidInput("رقم الهوية", sNid, byNid, "(مطلوب إن أُدخل الاسم)")
                Column { DateField("تاريخ الميلاد", sDob, { sDob = it }); Spacer(Modifier.height(4.dp)); AgeChips(sDob, spouseGender.ifEmpty { "unknown" }, "wife", false) }
                if (sMaternal) Column { IconSwitch("حامل", Fa.BabyCarriage, sPreg) { sPreg = it }; IconSwitch("مرضعة", Fa.Baby, sBf) { sBf = it } }
            }
            if (head) Card("أفراد الأسرة", Fa.Children, "(اختياري)") {
                kids.forEachIndexed { i, c ->
                    val sh = RoundedCornerShape(10.dp)
                    Column(Modifier.fillMaxWidth().border(1.dp, p.line, sh).padding(10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QSelect(c.rel, Rel.child, Modifier.weight(1f)) { c.rel = it }
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).clickable { kids.removeAt(i); c.id?.let { removedIds.add(it) } }, contentAlignment = Alignment.Center) { Icon(Fa.Trash, "حذف", Modifier.size(15.dp), tint = p.danger) }
                        }
                        QField("الاسم الكامل", c.name, { c.name = it }, placeholder = "الاسم الرباعي")
                        NidInput("رقم الهوية", c.nid, byNid, "(مطلوب إن أُدخل الاسم)")
                        Column { DateField("تاريخ الميلاد", c.dob, { c.dob = it }); Spacer(Modifier.height(4.dp)); AgeChips(c.dob, Rel.genderOf(c.rel).ifEmpty { "unknown" }, if (c.rel == "orphan") "orphan" else "child", false) }
                        QField("ملاحظة صحية (اختياري)", c.note, { c.note = it }, placeholder = "مثال: يعاني من ربو")
                    }
                }
                Row { Btn("إضافة فرد للأسرة", { kids.add(ChildRow(null, "", "", false, "", "", "son")) }, kind = BtnKind.Outline, small = true, icon = Fa.Plus) }
            }
            Card("التواصل", Fa.AddressBook) {
                // الهاتف يخص رب الأسرة فقط
                if (head) QField("رقم الهاتف الأساسي", phone, { phone = it }, type = KeyboardType.Phone)
                if (head) QField("رقم هاتف إضافي (اختياري)", phone2, { phone2 = it }, type = KeyboardType.Phone)
                QField("العنوان / الموقع", address, { address = it })
            }
            Card("الحالة الصحية", Fa.HeartPulse) {
                Column {
                    if (maternal) IconSwitch("حامل", Fa.BabyCarriage, preg) { preg = it }
                    if (maternal) IconSwitch("مرضعة", Fa.Baby, bf) { bf = it }
                    IconSwitch("مرض مزمن", Fa.NotesMedical, chronic) { chronic = it }
                    if (chronic) Column(Modifier.padding(top = 10.dp)) {
                        Text("اختر الحالة/الحالات المرضية", color = p.mute, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
                        FlowRow(Modifier.padding(top = 10.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            chronicConditions.forEach { (k, l) ->
                                val on = k in conds; val sh = RoundedCornerShape(50)
                                Text(l, color = if (on) p.danger else p.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(sh).background(if (on) p.danger.copy(alpha = if (p.dark) .13f else .06f) else p.surface2).border(1.5.dp, if (on) p.danger else p.line, sh).clickable { if (on) conds.remove(k) else conds.add(k) }.padding(horizontal = 14.dp, vertical = 7.dp))
                            }
                        }
                        QField("", diseaseDetails, { diseaseDetails = it }, Modifier.padding(top = 8.dp), placeholder = "تفاصيل إضافية / أخرى")
                    }
                    IconSwitch("إعاقة", Fa.Wheelchair, dis) { dis = it }
                    if (dis) QField("", disDetails, { disDetails = it }, Modifier.padding(top = 10.dp), placeholder = "تفاصيل الإعاقة")
                }
            }
            Card("ملاحظات", Fa.NoteSticky) { QField("", notes, { notes = it }, multiline = true, placeholder = "ملاحظات إضافية...") }
            (err ?: serverErr)?.let { Text(it, color = p.danger, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold) }
        }
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(p.line))
        Box(Modifier.fillMaxWidth().background(p.surface).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) { Btn("حفظ", { submit() }, Modifier.fillMaxWidth(), icon = Fa.Check) }
    }
}

@Composable
private fun Card(title: String, icon: ImageVector, hint: String? = null, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    QCard(pad = 16.dp) {
        Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(17.dp), tint = p.pri)
            Text(title, color = p.head, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (hint != null) Text(hint, color = p.mute, fontSize = 13.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

/** حقل الهوية الموحّد (NidField): 9 أرقام، حالة فورية (مكرر/صالح/خانة التحقق)، وخانة «لا يوجد رقم هوية بعد». */
@Composable
private fun NidInput(label: String, st: NidState, byNid: Map<String, JSONObject>, hint: String? = null) {
    val p = LocalPalette.current
    Column {
        QField(if (hint != null) "$label $hint" else label, st.value, { v -> st.value = Nid.digits(v).filter { it in '0'..'9' }.take(9); if (st.value.isNotEmpty()) st.none = false }, type = KeyboardType.Number, enabled = !st.none)
        val n = st.nid
        if (n.isNotEmpty()) {
            val dup = byNid[n]?.takeIf { it.optString("id") != st.self }
            val s = Nid.status(n)
            val (t, c) = when { dup != null -> "مسجّل مسبقاً بهذا المخيم: ${dup.optString("fullName")}" to p.danger; s == "valid" -> "رقم صالح" to p.success; s == "format" -> "الرقم يجب أن يكون 9 أرقام" to p.warn; else -> "لا يجتاز خانة التحقق — تأكد من الرقم" to p.warn }
            Text(t, color = c, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        }
        Row(Modifier.padding(top = 6.dp).clickable { st.none = !st.none; if (st.none) st.value = "" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val sh = RoundedCornerShape(3.dp)
            Box(Modifier.size(16.dp).background(if (st.none) p.pri else p.surface, sh).border(1.5.dp, if (st.none) p.pri else p.mute, sh), contentAlignment = Alignment.Center) { if (st.none) Icon(Fa.Check, null, Modifier.size(10.dp), tint = Color.White) }
            Text("لا يوجد رقم هوية بعد (مولود لم تصدر هويته)", color = p.mute, fontSize = 12.sp)
        }
    }
}

/** Derive.previewHtml: تصنيف فوري من تاريخ الميلاد والجنس. */
@Composable
internal fun AgeChips(dob: String, gender: String, relation: String, head: Boolean) {
    val p = LocalPalette.current
    val age = Stats.ageOfDob(dob)
    if (age == null) { Text("أدخل تاريخ الميلاد ليُصنَّف تلقائياً", color = p.mute, fontSize = 13.sp); return }
    val probe = JSONObject().put("birthDate", dob).put("gender", gender).put("relation", relation).put("isFamilyHead", head)
    val keys = Derive.keys(probe).filter { it in listOf("infant", "under2", "preschool", "child", "school", "youth", "midAdult", "elderly") }
    val (bg, fg, ic, t) = when { age < 18 -> listOf(Color(0xFFFEF3C7), Color(0xFF92400E), Fa.Child, "طفل"); age >= 60 -> listOf(Color(0xFFEDE9FE), Color(0xFF5B21B6), Fa.PersonCane, "كبير سن"); else -> listOf(Color(0xFFDBEAFE), Color(0xFF1E40AF), Fa.User, "بالغ") }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.background(bg as Color, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(ic as ImageVector, null, Modifier.size(11.dp), tint = fg as Color)
            Text("$t · $age سنة", color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        keys.forEach { k -> Text(dev.saged.grcs.data.PFilter.label(k), color = p.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(p.surface2, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp)) }
    }
}

/** صف مفتاح بأيقونة (toggle-row). */
@Composable
internal fun IconSwitch(label: String, icon: ImageVector, on: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(15.dp), tint = p.ink) }
        Text(label, color = p.ink, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        QSwitch(on, onChange)
    }
}

/** أزرار الجنس المقسّمة بألوان الويب (أزرق للذكر، وردي للأنثى). */
@Composable
internal fun GenderSeg(gender: String, unknown: Boolean = true, onSel: (String) -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(10.dp)
    val items = listOfNotNull(Triple("male", "ذكر", Fa.Mars), Triple("female", "أنثى", Fa.Venus), if (unknown) Triple("unknown", "غير محدد", Fa.Question) else null)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (k, l, ic) ->
            val on = gender == k
            val c = when { !on -> p.mute; k == "male" -> p.blue; k == "female" -> Color(0xFFDB2777); else -> if (p.dark) p.tealL else p.tealD }
            val bg = when { !on -> p.surface; k == "male" -> p.blue.copy(alpha = .09f); k == "female" -> Color(0xFFDB2777).copy(alpha = .06f); else -> p.pri.copy(alpha = .08f) }
            Row(Modifier.weight(1f).heightIn(min = 46.dp).clip(shape).background(bg).border(1.5.dp, if (!on) p.line else if (k == "unknown") p.pri else c, shape).clickable { onSel(k) }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(ic, null, Modifier.size(14.dp), tint = c); Spacer(Modifier.width(6.dp))
                Text(l, color = c, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}
