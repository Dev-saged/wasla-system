package dev.saged.grcs.ui.ben

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import dev.saged.grcs.data.FormResult
import dev.saged.grcs.data.Merge
import dev.saged.grcs.data.Nid
import dev.saged.grcs.data.Stats
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.ConfirmSheet
import dev.saged.grcs.ui.components.DateField
import dev.saged.grcs.ui.components.QField
import dev.saged.grcs.ui.components.QSelect
import dev.saged.grcs.ui.components.Sheet
import dev.saged.grcs.ui.components.SwitchRow
import dev.saged.grcs.ui.icons.Fa
import dev.saged.grcs.ui.people.AgeChips
import dev.saged.grcs.ui.people.NidInput
import dev.saged.grcs.ui.people.NidState
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** BEN_REL_OPTIONS. */
private val benRel = listOf("child" to "ابن / ابنة", "nephew" to "ابن / ابنة أخ أو عم", "orphan" to "يتيم / يتيمة في رعاية الأسرة", "father" to "الأب", "mother" to "الأم", "brother" to "أخ", "sister" to "أخت", "grandparent" to "جد / جدة", "other" to "قريب آخر")
/** OTHER_RELATIONS. */
private val otherRel = mapOf("father" to "الأب", "mother" to "الأم", "brother" to "أخ", "sister" to "أخت", "grandparent" to "جد / جدة", "other" to "قريب آخر")

/** نافذة فرد الأسرة للمستفيد — نقل مباشر لـopenBenMemberModal. */
@Composable
internal fun BenMemberSheet(auth: AuthState, data: DataRepo, camp: String, head: JSONObject, family: List<JSONObject>, member: JSONObject?, onClose: () -> Unit, onDone: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val isNew = member == null
    val m0 = remember { member ?: JSONObject().put("gender", "unknown").put("relation", "child") }
    val isWife = m0.optString("relation") == "wife"
    val nid0 = Nid.norm(m0.optString("nationalId"))
    val canRename = isNew || (m0.optString("nationalId").isEmpty() && !isWife)
    val canSetId = isNew || nid0.isEmpty()
    val canDelete = !isNew && !isWife && !m0.optBoolean("isFamilyHead")
    val askGender = isNew || m0.optString("gender").let { it == "unknown" || it.isEmpty() }
    val pool = remember { (listOf(head) + family).filter { it.optString("nationalId").isNotEmpty() }.associateBy { Nid.norm(it.optString("nationalId")) } }

    var name by remember { mutableStateOf(m0.optString("fullName")) }
    val nid = remember { NidState("", !isNew && m0.optBoolean("noIdYet"), m0.optString("id")) }
    var rel by remember { mutableStateOf("child") }
    var gender by remember { mutableStateOf(m0.optString("gender").takeIf { it == "female" } ?: if (askGender) "male" else m0.optString("gender")) }
    var dob by remember { mutableStateOf(m0.optString("birthDate").takeIf { it != "null" }.orEmpty()) }
    var preg by remember { mutableStateOf(m0.optBoolean("isPregnant")) }
    var breast by remember { mutableStateOf(m0.optBoolean("isBreastfeeding")) }
    var chronic by remember { mutableStateOf(m0.optBoolean("hasChronicDisease")) }
    var chronicD by remember { mutableStateOf(m0.optString("diseaseDetails")) }
    var dis by remember { mutableStateOf(m0.optBoolean("hasDisability")) }
    var disD by remember { mutableStateOf(m0.optString("disabilityDetails")) }
    var notes by remember { mutableStateOf(m0.optString("notes")) }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var del by remember { mutableIntStateOf(0) }

    val age = Stats.ageOfDob(dob)
    val matNew = gender == "female" && age != null && age >= 15
    val showMat = if (isNew) matNew else (isWife || m0.optString("gender") == "female")
    fun toast(t: String) = Toast.makeText(ctx, t, Toast.LENGTH_LONG).show()

    fun save() {
        val fullName = if (canRename) name.trim() else m0.optString("fullName")
        if (fullName.isEmpty()) { err = "الاسم مطلوب"; return }
        val d = dob.ifEmpty { null }
        if (isNew && d == null) { err = "تاريخ الميلاد مطلوب — منه يُحدَّد تصنيف الفرد ومكانه"; return }
        if (isNew && age != null && age < 15 && rel in listOf("father", "mother", "grandparent")) { err = "العمر لا يتفق مع صلة القرابة المختارة"; return }
        val n = nid.nid; val none = nid.none && n.isEmpty()
        if (canSetId && (isNew || n.isNotEmpty() || none)) {
            val e = when {
                n.isEmpty() && !none -> "رقم الهوية مطلوب — أدخله أو حدّد «لا يوجد رقم هوية بعد»"
                n.isEmpty() -> null
                Nid.status(n) == "format" -> "رقم الهوية يجب أن يكون 9 أرقام"
                pool[n]?.takeIf { it.optString("id") != nid.self } != null -> "رقم الهوية مسجّل مسبقاً: ${pool[n]!!.optString("fullName")}"
                else -> null
            }
            if (e != null) { err = e; return }
        }
        err = null; busy = true
        scope.launch {
            val res = runCatching {
                withContext(Dispatchers.IO) {
                    val h0 = data.people.persons(camp).firstOrNull { it.optString("id") == head.optString("id") } ?: head
                    if (h0.optString("familyId").isEmpty()) {
                        h0.put("familyId", h0.optString("nationalId").ifEmpty { java.util.UUID.randomUUID().toString() }).put("updatedBy", auth.user.id)
                        data.afterSave(auth, camp, data.people.save(camp, h0), false)
                    }
                    val o = if (isNew) JSONObject().put("familyId", h0.optString("familyId")).put("createdBy", auth.user.id).put("isFamilyHead", false).put("nationalId", "").put("phone", "").put("address", h0.optString("address")).put("customFields", JSONObject())
                    else Merge.copy(data.people.persons(camp).firstOrNull { it.optString("id") == m0.optString("id") } ?: m0)
                    o.put("fullName", fullName).put("updatedBy", auth.user.id)
                    if (askGender) o.put("gender", gender)
                    if (canSetId) { o.put("nationalId", n); o.put("noIdYet", none) }
                    if (isNew) {
                        val female = o.optString("gender") == "female"
                        when (rel) {
                            "child" -> o.put("relation", "child").put("memberRelation", if (female) "daughter" else "son")
                            "nephew" -> o.put("relation", "child").put("memberRelation", if (female) "niece" else "nephew")
                            "orphan" -> o.put("relation", "orphan").put("memberRelation", "orphan")
                            else -> o.put("relation", "other").put("relationDetail", otherRel[rel])
                        }
                        o.put("orderIndex", 2 + family.size)
                    }
                    if (!isWife) { o.put("birthDate", d ?: JSONObject.NULL); Stats.ageOfDob(d.orEmpty())?.let { o.put("calculatedAge", it) } }
                    if (o.optString("gender") == "female" || o.optString("relation") == "wife") { if (showMat) { o.put("isPregnant", preg); o.put("isBreastfeeding", breast) } }
                    else { o.put("isPregnant", false); o.put("isBreastfeeding", false) }
                    o.put("hasChronicDisease", chronic).put("diseaseDetails", if (chronic) chronicD.trim() else "")
                    o.put("hasDisability", dis).put("disabilityDetails", if (dis) disD.trim() else "")
                    o.put("notes", notes.trim())
                    data.saveForm(auth, camp, FormResult(o, emptyList(), emptyList(), isNew, 0))
                }
            }
            busy = false
            val e = res.getOrNull()
            when {
                res.isFailure -> err = "تعذّر الحفظ — حاول مجدداً"
                e != null -> err = e
                else -> { toast(if (!auth.offline) (if (isNew) "تمت الإضافة ووصلت للمندوب" else "تم الحفظ ووصل للمندوب") else "حُفظ — سيصل للمندوب عند الاتصال"); onDone() }
            }
        }
    }

    Sheet(onClose, if (isNew) "إضافة فرد جديد" else m0.optString("fullName")) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (canRename) QField("الاسم الكامل *", name, { name = it })
            else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Fa.Lock, null, Modifier.size(13.dp), tint = p.mute)
                Text(m0.optString("fullName") + m0.optString("nationalId").let { if (it.isNotEmpty()) " · $it" else "" }, color = p.mute, fontSize = 13.sp)
            }
            if (canSetId) NidInput(if (isNew) "رقم الهوية *" else "رقم الهوية", nid, pool) { e -> if (canRename && name.isBlank() && e.name.isNotEmpty()) name = e.name; if (!isWife && dob.isEmpty() && e.dob != null) dob = e.dob }
            if (isNew) QSelect(rel, benRel, label = "صلة القرابة") { rel = it }
            if (askGender) QSelect(gender, listOf("male" to "ذكر", "female" to "أنثى"), label = "الجنس") { gender = it; if (it != "female") { preg = false; breast = false } }
            if (!isWife) {
                DateField(if (isNew) "تاريخ الميلاد *" else "تاريخ الميلاد", dob, { dob = it })
                AgeChips(dob, gender, "child", false)
            }
            Column {
                if (showMat) { SwitchRow("حامل", null, preg) { preg = it }; SwitchRow("مرضعة", null, breast) { breast = it } }
                SwitchRow("مرض مزمن", null, chronic) { chronic = it }
                if (chronic) QField("تفاصيل المرض", chronicD, { chronicD = it }, Modifier.padding(vertical = 8.dp))
                SwitchRow("إعاقة", null, dis, last = !dis) { dis = it }
                if (dis) QField("تفاصيل الإعاقة", disD, { disD = it }, Modifier.padding(top = 8.dp))
            }
            QField("ملاحظات", notes, { notes = it }, multiline = true)
            err?.let { Text(it, color = p.danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("حفظ", ::save, Modifier.weight(1f), icon = Fa.Check, enabled = !busy)
                if (canDelete) Btn("حذف", { del = 1 }, Modifier.weight(1f), BtnKind.Danger, Fa.Trash, enabled = !busy)
            }
        }
    }
    if (del == 1) ConfirmSheet("حذف \"${m0.optString("fullName")}\" من أسرتك نهائياً؟", "حذف فرد", "حذف نهائي", onCancel = { del = 0 }) { del = 2 }
    if (del == 2) ConfirmSheet("تأكيد أخير: سيُحذف السجل عند المندوب والإدارة أيضاً.", "تأكيد الحذف", "نعم، احذف", onCancel = { del = 0 }) {
        del = 0; busy = true
        scope.launch {
            val ok = runCatching { withContext(Dispatchers.IO) { data.people.delete(camp, m0); data.afterSave(auth, camp, m0, true); data.pushNow(auth, camp) } }.isSuccess
            busy = false
            if (ok) { toast("تم الحذف"); onDone() } else err = "تعذّر الحذف"
        }
    }
}
