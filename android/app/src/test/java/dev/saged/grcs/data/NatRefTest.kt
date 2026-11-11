package dev.saged.grcs.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
class NatRefTest {
    private val headers = listOf("اسم رب الأسرة", "رقم الهوية", "اسم الزوجة", "هوية الزوجة", "تاريخ الميلاد", "رقم الجوال")

    @Test
    fun entriesIndexHeadAndWife() {
        val ix = HashMap<String, RefEntry>(); val fam = HashMap<String, List<RefEntry>>()
        NatRef.entries("اختبار", headers, "family_summary", listOf(listOf("محمد أحمد", "123456782", "سارة علي", "400000009", "", ""), listOf("", "", "", "", "", "")), ix, fam)
        assertEquals(2, ix.size)
        assertEquals("wife", ix["400000009"]?.role)
        assertEquals(2, fam["123456782"]?.size)
    }

    @Test
    fun reconcileFillsEmptyFieldsOnly() {
        val head = JSONObject().put("id", "h1").put("fullName", "محمد أحمد").put("nationalId", "123456782").put("isFamilyHead", true).put("relation", "head").put("familyId", "F1").put("phone", "0599000000")
        val wife = JSONObject().put("id", "w1").put("fullName", "سارة علي").put("nationalId", "").put("relation", "wife").put("familyId", "F1")
        val rows = listOf(
            listOf("محمد أحمد", "123456782", "سارة علي", "400000009", "1980-05-01", "0598111111"),
            listOf("خالد يوسف", "999999998", "", "", "", ""),
            listOf("بلا هوية", "", "", "", "", ""),
        )
        val c = Cells.cols(headers, "family_summary")
        val r = Reconcile.run(rows, c, "family_summary", false, mapOf("C1" to listOf(head, wife)), "u1") { _, _ -> }
        assertEquals(1, r.matched); assertEquals(1, r.unmatched); assertEquals(1, r.skippedNoNid)
        assertEquals("0599000000", head.optString("phone"))
        assertEquals("1980-05-01", head.optString("birthDate"))
        assertEquals("400000009", wife.optString("nationalId"))
        assertEquals(1, r.headsEnriched); assertEquals(1, r.membersEnriched)
        assertEquals(2, r.writes.size)
        assertTrue(r.unmatchedList.any { it[2] == "غير موجود بالنظام" })
    }

    @Test
    fun reconcileAddsMissingSpouseWhenAsked() {
        val head = JSONObject().put("id", "h1").put("fullName", "محمد أحمد").put("nationalId", "123456782").put("isFamilyHead", true).put("relation", "head").put("familyId", "F1")
        val rows = listOf(listOf("محمد أحمد", "123456782", "سارة علي", "400000009", "", ""))
        val c = Cells.cols(headers, "family_summary")
        assertEquals(0, Reconcile.run(rows, c, "family_summary", false, mapOf("C1" to listOf(JSONObject(head.toString()))), "u1") { _, _ -> }.spousesAdded)
        val r = Reconcile.run(rows, c, "family_summary", true, mapOf("C1" to listOf(head)), "u1") { _, _ -> }
        assertEquals(1, r.spousesAdded)
        assertEquals("wife", r.writes.keys.first { it !== head }.optString("relation"))
    }
}
