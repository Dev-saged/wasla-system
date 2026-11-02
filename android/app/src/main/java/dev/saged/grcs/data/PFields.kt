package dev.saged.grcs.data

import org.json.JSONObject

/** حقول تُخزَّن في customFields كما في الويب (هاتف إضافي) مع قراءة القديم. */
object PFields {
    private const val P2 = "هاتف إضافي"
    fun phone2(p: JSONObject): String {
        val c = p.optJSONObject("customFields")
        return c?.optString(P2).orEmpty().ifEmpty { c?.optString("الجوال 2").orEmpty() }.ifEmpty { c?.optString("رقم التواصل 2").orEmpty() }.ifEmpty { p.optString("phone2") }
    }
    fun setPhone2(o: JSONObject, v: String) {
        val c = o.optJSONObject("customFields") ?: JSONObject()
        if (v.isNotEmpty()) c.put(P2, v) else { c.remove(P2); c.remove("الجوال 2") }
        o.put("customFields", c); o.remove("phone2")
    }
    fun living(p: JSONObject) = p.optString("livingSituation").ifEmpty { p.optString("livingDetails") }
}
