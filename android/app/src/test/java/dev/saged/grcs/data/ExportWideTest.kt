package dev.saged.grcs.data

import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
class ExportWideTest {
    private fun p(id: String, name: String, rel: String, fam: String, nid: String = "", head: Boolean = false, dob: String = "") =
        JSONObject().put("id", id).put("fullName", name).put("relation", rel).put("familyId", fam).put("nationalId", nid).put("isFamilyHead", head).put("birthDate", dob)

    private val data = listOf(
        p("1", "محمد أحمد", "head", "F1", "123456782", true), p("2", "سارة علي", "wife", "F1", "400000009"),
        p("3", "يوسف محمد", "son", "F1", "", false, "2015-01-01"), p("4", "ليلى محمد", "daughter", "F1", "", false, "2012-01-01"),
        p("5", "خالد سعيد", "head", "F2", "999999998", true))

    @Test fun oneRowPerFamilyWithChildrenInline() {
        val w = Export(ApplicationProvider.getApplicationContext()).wide(data)
        assertEquals(2, w.rows.size)
        assertTrue(w.headers.containsAll(listOf("اسم الزوجة", "اسم الابن 1", "اسم الابن 2")))
        assertEquals(w.headers.size, w.rows[0].size)
        assertEquals(w.headers.size, w.bands.size)
        val r = w.rows.first { it[1] == "محمد أحمد" }
        assertEquals("ليلى محمد", r[w.headers.indexOf("اسم الابن 1")])
    }

    @Test fun headsOnlyHasNoSpouseOrChildren() {
        val w = Export(ApplicationProvider.getApplicationContext()).headsOnly(data)
        assertEquals(2, w.rows.size)
        assertTrue(w.headers.none { it.contains("الابن") || it.contains("الزوجة") })
    }
}
