package dev.saged.grcs.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
class NatStreamTest {
    private fun xlsx(shared: List<String>, rows: String): File {
        val f = File.createTempFile("nat", ".xlsx")
        ZipOutputStream(f.outputStream()).use { z ->
            z.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            z.write(("<sst>" + shared.joinToString("") { "<si><t>$it</t></si>" } + "</sst>").toByteArray())
            z.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            z.write("<worksheet><sheetData>$rows</sheetData></worksheet>".toByteArray())
        }
        return f
    }

    @Test fun keepsOnlyWantedHeadsAndResolvesSharedStrings() {
        val sh = listOf("الاسم الرباعي", "رقم الهوية", "الجوال 1", "أحمد علي", "400000001", "سعيد خالد", "400000002", "595063429")
        val rows = """<row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c><c r="C1" t="s"><v>2</v></c></row>
            <row r="2"><c r="A2" t="s"><v>3</v></c><c r="B2" t="s"><v>4</v></c><c r="C2" t="s"><v>7</v></c></row>
            <row r="3"><c r="A3" t="s"><v>5</v></c><c r="B3" t="s"><v>6</v></c></row>
            <row r="4"><c r="A4" t="inlineStr"><is><t>رائد</t></is></c><c r="B4"><v>400000003</v></c></row>"""
        val out = NatStream.readXlsx(xlsx(sh, rows), setOf("400000001", "400000003")) { }!!
        assertEquals(listOf("الاسم الرباعي", "رقم الهوية", "الجوال 1"), out.headers)
        assertEquals(3, out.scanned)
        assertEquals(listOf(listOf("أحمد علي", "400000001", "595063429"), listOf("رائد", "400000003")), out.rows)
    }

    @Test fun phoneAndHousingNormalization() {
        assertEquals("0595063429", Cells.phone("595063429"))
        assertEquals("0595063429", Cells.phone("970595063429"))
        assertEquals("0595063429", Cells.phone("0595063429"))
        assertEquals("خيمة متضررة - ضرر كلي", Cells.housing("خيمة", "كلي"))
        assertEquals("باطون - ضرر جزئي", Cells.housing("باطون", "ضرر جزئي"))
        assertEquals("خيمة", Cells.housing("خيمة", "غير متضرر"))
        assertEquals("tent_damaged", Vuln.inferShelter(Cells.housing("خيمة", "جزئي")))
    }

    @Test fun realRegistrySampleStreams() {
        val path = System.getenv("GRCS_SAMPLE"); assumeTrue(path != null && File(path).exists())
        val t0 = System.currentTimeMillis()
        val all = NatStream.readXlsx(File(path!!), emptySet()) { }!!
        assertEquals(0, all.rows.size)
        val probe = Xlsx.read(File(path).inputStream()).drop(1).take(40).mapNotNull { it.getOrNull(Cells.cols(all.headers, Cells.modeOf(all.headers)).headNid ?: 0)?.let { n -> Nid.norm(n) } }.toSet()
        val out = NatStream.readXlsx(File(path), probe) { }!!
        println("scanned=${out.scanned} kept=${out.rows.size} ms=${System.currentTimeMillis() - t0} cols=${all.headers.size}")
        assertTrue(out.rows.size >= probe.size)
        assertTrue(out.scanned > 30000)
        val c = Cells.cols(out.headers, Cells.modeOf(out.headers))
        val row = out.rows.first { r -> Cells.clean(r.getOrNull(c.health ?: 0)) != "سليم" }
        val nid = Nid.norm(row[c.headNid!!])
        val head = org.json.JSONObject().put("id", "h1").put("campId", "c1").put("fullName", row[c.headName!!]).put("nationalId", nid).put("isFamilyHead", true).put("phone", "595063429")
        val r = Reconcile.run(listOf(row), c, "horizontal", true, mapOf("c1" to listOf(head)), "u") { _, _ -> }
        println("matched=${r.matched} phone=${head.optString("phone")} living=${head.optString("livingSituation")} disease=${head.optString("diseaseDetails")} origin=${head.optJSONObject("customFields")?.optString("السكن الأصلي")} addr=${head.optString("address")} added=${r.spousesAdded}+${r.childrenAdded}")
        assertEquals("0595063429", head.optString("phone"))
        assertTrue(head.optString("livingSituation").isNotEmpty())
    }
}
