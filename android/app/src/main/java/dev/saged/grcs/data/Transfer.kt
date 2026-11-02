package dev.saged.grcs.data

import dev.saged.grcs.auth.AuthState
import org.json.JSONArray
import org.json.JSONObject

class TransferPlan(val src: String, val famKey: String, val members: List<JSONObject>, val head: JSONObject, val blocks: List<String>, val warns: List<String>)

/** نقل أسرة بين مخيمين (إدارة فقط): فحص تعارض الهويات، كتابة الهدف أولاً ثم إزالة المصدر، ونقل حساب المستفيد وسجل دائم. */
class Transfer(private val repo: DataRepo) {

    fun plan(st: AuthState, src: String, famKey: String, dest: String?): TransferPlan {
        val all = repo.people.persons(src)
        val members = all.filter { it.optString("familyId").ifEmpty { "i_" + it.optString("id") } == famKey }
            .sortedByDescending { it.optBoolean("isFamilyHead") }
        val head = members.firstOrNull() ?: JSONObject()
        val blocks = ArrayList<String>(); val warns = ArrayList<String>()
        if (dest != null) {
            if (dest == src) blocks += "المخيم الهدف هو نفسه المخيم الحالي"
            val destPeople = repo.people.persons(dest)
            val famId = members.firstOrNull()?.optString("familyId").orEmpty()
            if (famId.isNotEmpty() && destPeople.any { it.optString("familyId") == famId }) blocks += "معرّف الأسرة موجود مسبقاً بالمخيم الهدف"
            val ids = members.map { it.optString("id") }.toSet()
            val seen = HashSet<String>()
            for (m in members) {
                val n = m.optString("nationalId"); if (n.isEmpty()) continue
                if (!seen.add(n)) blocks += "هوية مكررة داخل الأسرة: $n"
                destPeople.firstOrNull { it.optString("nationalId") == n && it.optString("id") !in ids }?.let { blocks += "الهوية $n مسجّلة مسبقاً لـ «${it.optString("fullName")}» في المخيم الهدف" }
                runCatching { Fs.get("nationalIds/$n", st.idToken) }.getOrNull()?.let { f ->
                    val c = Fs.stringField(f, "campId")
                    if (c.isNotEmpty() && c != src && c != dest) blocks += "الهوية $n مسجّلة في مخيم آخر"
                }
            }
        }
        val aidN = members.sumOf { Aid.list(it).size }
        if (aidN > 0) warns += "سجل المساعدات ($aidN تسليم) سينتقل مع الأسرة"
        val reqN = members.sumOf { m -> Aid.requests(m).count { it.optString("status") == "pending" } }
        if (reqN > 0) warns += "$reqN طلب مفتوح سينتقل لمندوب المخيم الجديد"
        warns += "حساب المستفيد سيُنقل للمخيم الجديد بنفس كلمة المرور"
        return TransferPlan(src, famKey, members, head, blocks, warns)
    }

    /** يعيد رسالة خطأ أو null عند النجاح. */
    fun execute(st: AuthState, plan: TransferPlan, dest: String, destName: String, srcName: String, reason: String): String? {
        if (st.user.role != "admin") return "النقل بين المخيمات للإدارة فقط"
        val p2 = plan(st, plan.src, plan.famKey, dest)
        if (p2.blocks.isNotEmpty()) return p2.blocks.first()
        if (p2.members.isEmpty()) return "الأسرة غير موجودة"
        val token = st.idToken
        val transferId = java.util.UUID.randomUUID().toString()
        val now = Clock.now()
        // 1) كتابة الهدف أولاً
        val moved = p2.members.map { m ->
            val o = Merge.copy(m).put("campId", dest).put("updatedBy", st.user.id)
            val tr = (m.optJSONArray("transfers")?.let { a -> (0 until a.length()).map { a.get(it) } } ?: emptyList()) +
                JSONObject().put("id", transferId).put("from", plan.src).put("to", dest).put("at", now).put("by", st.user.id)
            o.put("transfers", JSONArray(tr))
        }
        moved.forEach { repo.people.save(dest, it) }
        if (!repo.pushNow(st, dest) || repo.people.pendingCount(dest) > 0) return "تعذّر رفع الأسرة للمخيم الهدف — لم يُحذف شيء من المخيم الأصلي، أعد المحاولة"
        // 2) إزالة المصدر
        p2.members.forEach { repo.people.delete(plan.src, it) }
        val srcOk = repo.pushNow(st, plan.src) && repo.people.pendingCount(plan.src) == 0
        // 3) حساب المستفيد والفهرس الوطني والسجل
        val head = p2.head
        val nid = head.optString("nationalId")
        if (nid.isNotEmpty()) runCatching {
            Fs.queryEq("users", listOf("campId" to plan.src, "username" to nid), token).firstOrNull { it.optString("role") == "beneficiary" }?.let { u ->
                Fs.patch("users/${u.getString("id")}", mapOf("campId" to dest, "campName" to destName), token)
                runCatching { Fs.patch("creds/${u.getString("id")}", mapOf("campId" to dest), token) }
                runCatching { Fs.pulse("u", dest, token) }
            }
        }
        moved.filter { it.optString("nationalId").isNotEmpty() }.forEach { m ->
            runCatching { Fs.patch("nationalIds/${m.getString("nationalId")}", mapOf("campId" to dest, "campName" to destName), token) }
        }
        val info = JSONObject().put("transferId", transferId).put("from", JSONObject().put("id", plan.src).put("name", srcName))
            .put("to", JSONObject().put("id", dest).put("name", destName)).put("familyId", head.optString("familyId")).put("headId", head.optString("id"))
            .put("headName", head.optString("fullName")).put("count", moved.size).put("reason", reason.trim().take(300)).put("synced", srcOk)
        for (cid in listOf(plan.src, dest)) runCatching {
            val id = java.util.UUID.randomUUID().toString()
            Fs.set("camps/$cid/activityLogs/$id", mapOf("id" to id, "action" to "family_transfer", "userId" to st.user.id, "userName" to st.user.fullName,
                "campId" to cid, "targetId" to head.optString("id"), "targetName" to head.optString("fullName"), "newValue" to info.toString(),
                "timestamp" to Clock.now(), "device" to "android"), token)
        }
        return if (srcOk) null else "نُقلت الأسرة للهدف، وإزالتها من المخيم الأصلي ستكتمل عند المزامنة القادمة"
    }
}
