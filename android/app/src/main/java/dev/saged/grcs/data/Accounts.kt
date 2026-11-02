package dev.saged.grcs.data

import android.util.Base64
import dev.saged.grcs.auth.Pbkdf2
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/** الفهرس الوطني وحسابات المستفيدين التلقائية (كما في الويب). الفشل يُؤجَّل في قائمة ويُعاد عند المزامنة. */
class Accounts(private val cache: FamCache) {
    private val defaultPw = "123456789"

    fun checkNid(nid: String, camp: String, token: String): String? = try {
        val f = Fs.get("nationalIds/$nid", token)
        val other = Fs.stringField(f, "campId")
        if (f != null && other.isNotEmpty() && other != camp) "الهوية مسجّلة في مخيم آخر: ${Fs.stringField(f, "campName").ifEmpty { other }}" else null
    } catch (e: Exception) { null }

    /** بيانات دخول رب الأسرة: (اسم المستخدم، كلمة المرور) أو null إن لم توجد/لا صلاحية. */
    fun credentials(nid: String, camp: String, token: String): Pair<String, String>? = try {
        val u = Fs.queryEq("users", listOf("campId" to camp, "username" to nid), token).firstOrNull()
        val c = u?.let { Fs.get("creds/${it.getString("id")}", token) }
        val pw = Fs.stringField(c, "pw")
        if (pw.isEmpty()) null else nid to pw
    } catch (e: Exception) { null }

    private fun sha256(s: String) = MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun todo(camp: String): JSONArray = runCatching { JSONArray(cache.meta("todo_$camp") ?: "[]") }.getOrDefault(JSONArray())
    private fun saveTodo(camp: String, a: JSONArray) = cache.setMeta("todo_$camp", a.toString())

    fun enqueueSave(camp: String, p: JSONObject) {
        if (p.optString("nationalId").isEmpty()) return
        val a = todo(camp)
        a.put(JSONObject().put("t", "nid").put("p", p.toString()))
        if (p.optBoolean("isFamilyHead")) a.put(JSONObject().put("t", "ben").put("p", p.toString()))
        saveTodo(camp, a)
    }

    fun enqueueDelete(camp: String, p: JSONObject) {
        val nid = p.optString("nationalId"); if (nid.isEmpty()) return
        saveTodo(camp, todo(camp).put(JSONObject().put("t", "nidDel").put("nid", nid)))
    }

    /** ينفّذ القائمة المؤجلة؛ ما يفشل بسبب الشبكة يبقى للمرة القادمة. */
    fun run(camp: String, campName: String, token: String, secV: Long) {
        val a = todo(camp); if (a.length() == 0) return
        val keep = JSONArray()
        for (i in 0 until a.length()) {
            val it = a.getJSONObject(i)
            try {
                when (it.getString("t")) {
                    "nid" -> {
                        val p = JSONObject(it.getString("p"))
                        Fs.set("nationalIds/${p.getString("nationalId")}", mapOf("nid" to p.getString("nationalId"), "campId" to camp,
                            "campName" to campName, "personId" to p.getString("id"), "fullName" to p.optString("fullName"), "phone" to ""), token)
                    }
                    "nidDel" -> {
                        val f = Fs.get("nationalIds/${it.getString("nid")}", token)
                        if (f != null && Fs.stringField(f, "campId") == camp) Fs.delete("nationalIds/${it.getString("nid")}", token)
                    }
                    "ben" -> if (secV >= 2) ensureBeneficiary(JSONObject(it.getString("p")), camp, campName, token)
                }
            } catch (e: FsException) {
                if (e.status != 403 && e.status != 400) keep.put(it)
            } catch (e: Exception) { keep.put(it) }
        }
        saveTodo(camp, keep)
    }

    private fun ensureBeneficiary(head: JSONObject, camp: String, campName: String, token: String) {
        val nid = head.optString("nationalId"); if (nid.isEmpty()) return
        val found = Fs.queryEq("users", listOf("campId" to camp, "username" to nid), token).firstOrNull()
        val famId = head.optString("familyId").ifEmpty { nid }
        if (found != null) {
            if (found.optString("role") != "beneficiary") return
            val m = hashMapOf<String, Any?>("personId" to head.getString("id"), "campId" to camp, "campName" to campName,
                "fullName" to head.optString("fullName"), "familyId" to famId)
            head.optString("phone").takeIf { it.isNotEmpty() }?.let { m["phone"] = it }
            Fs.patch("users/${found.getString("id")}", m, token)
            runCatching { Fs.pulse("u", camp, token) }
            return
        }
        val id = java.util.UUID.randomUUID().toString()
        val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        val hash = Pbkdf2.hash(defaultPw, salt)
        val now = Clock.now()
        Fs.set("users/$id", mapOf("id" to id, "username" to nid, "salt" to salt, "verifier" to sha256("grcs:v1:$hash"), "role" to "beneficiary",
            "campId" to camp, "campName" to campName, "fullName" to head.optString("fullName"), "phone" to head.optString("phone"),
            "personId" to head.getString("id"), "familyId" to famId, "createdAt" to now, "isActive" to true, "mustChangePassword" to true,
            "pwUpdatedAt" to now), token)
        Fs.set("creds/$id", mapOf("uid" to id, "pw" to defaultPw, "username" to nid, "role" to "beneficiary", "campId" to camp,
            "fullName" to head.optString("fullName"), "updatedAt" to now, "by" to ""), token)
        runCatching { Fs.pulse("u", camp, token) }
    }
}
