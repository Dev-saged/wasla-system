package dev.saged.grcs.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AidType(val k: String, val label: String, val ask: String? = null)

/** المساعدات والطلبات: تُحفظ داخل سجل الفرد نفسه (aid / requests) وتُدمج بالاتحاد حسب id. */
object Aid {
    val types = listOf(
        AidType("food", "سلة غذائية"), AidType("cash", "مساعدة نقدية", "المبلغ"), AidType("milk", "حليب أطفال"),
        AidType("diapers", "حفاضات"), AidType("prenatal", "مكملات حوامل ومرضعات"), AidType("medicine", "أدوية أمراض مزمنة"),
        AidType("assist", "أدوات مساندة"), AidType("orphan", "رعاية الأيتام"), AidType("elder", "رعاية كبار السن"),
        AidType("clothes", "كسوة"), AidType("other", "أخرى"),
    )
    val reqTypes = listOf("food" to "غذاء", "medicine" to "دواء / علاج", "clothes" to "ملابس", "shelter" to "مأوى / خيمة", "cash" to "مساعدة مالية", "other" to "أخرى")
    val reqStatus = mapOf("pending" to "قيد المراجعة", "approved" to "مقبول", "done" to "تم التسليم", "rejected" to "مرفوض", "cancelled" to "ملغى")

    fun age(p: JSONObject): Int? = Stats.ageOf(p)

    fun eligible(p: JSONObject, k: String): Boolean {
        val a = age(p)
        return when (k) {
            "food", "cash" -> p.optBoolean("isFamilyHead")
            "milk", "diapers" -> a != null && a < 2
            "prenatal" -> p.optBoolean("isPregnant") || p.optBoolean("isBreastfeeding")
            "medicine" -> p.optBoolean("hasChronicDisease")
            "assist" -> p.optBoolean("hasDisability")
            "orphan" -> p.optJSONArray("tags")?.let { t -> (0 until t.length()).any { t.optString(it) == "orphan" } } == true
            "elder" -> a != null && a >= 60
            else -> true
        }
    }

    fun list(p: JSONObject): List<JSONObject> = p.optJSONArray("aid")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) }.filter { !it.optBoolean("void") } }.orEmpty()

    fun give(p: JSONObject, k: String, q: Double, note: String, userId: String): JSONObject {
        val o = Merge.copy(p)
        val rec = JSONObject().put("id", java.util.UUID.randomUUID().toString()).put("t", k)
            .put("d", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())).put("q", if (q == q.toLong().toDouble()) q.toLong() else q)
            .put("n", note.trim()).put("by", userId).put("at", Clock.now())
        val all = (o.optJSONArray("aid")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } } ?: emptyList()) + rec
        o.put("aid", JSONArray(all.takeLast(200)))
        return o
    }

    /** التراجع علامة void لا حذف، كي لا يعود السجل من جهاز آخر. */
    fun undo(p: JSONObject, entryId: String): JSONObject {
        val o = Merge.copy(p)
        val a = o.optJSONArray("aid") ?: return o
        for (i in 0 until a.length()) a.optJSONObject(i)?.let { if (it.optString("id") == entryId) it.put("void", true).put("at", Clock.now()) }
        return o
    }

    fun requests(p: JSONObject): List<JSONObject> = p.optJSONArray("requests")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } }.orEmpty()

    fun addRequest(p: JSONObject, type: String, details: String, userId: String): JSONObject {
        val o = Merge.copy(p); val now = Clock.now()
        val r = JSONObject().put("id", java.util.UUID.randomUUID().toString()).put("type", type).put("details", details.trim())
            .put("status", "pending").put("createdAt", now).put("updatedAt", now).put("createdBy", userId)
        o.put("requests", JSONArray(requests(o) + r)); return o
    }

    fun setRequestStatus(p: JSONObject, id: String, status: String, userId: String): JSONObject {
        val o = Merge.copy(p); val a = o.optJSONArray("requests") ?: return o
        for (i in 0 until a.length()) a.optJSONObject(i)?.let { if (it.optString("id") == id) it.put("status", status).put("updatedAt", Clock.now()).put("reviewedBy", userId) }
        return o
    }
}
