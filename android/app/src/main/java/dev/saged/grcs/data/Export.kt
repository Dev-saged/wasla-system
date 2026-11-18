package dev.saged.grcs.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dev.saged.grcs.config.BrandConfig
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportResult(val ok: Boolean, val name: String, val uri: Uri?, val count: Int)

/** تصدير الأفراد إلى xlsx وcsv وpdf في المستندات/«تصدير اسم التطبيق» (مجلد منفصل كي لا يظهر في قائمة النسخ الاحتياطية). */
class Export(private val ctx: Context) {
    internal val rel = "Documents/${BrandConfig.BACKUP_DIR}/الكشوف"
    internal val legacyDir get() = File(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), BrandConfig.BACKUP_DIR), "الكشوف")
    /** نسبة التقدم لمهمة تصدير جارية (20..85 أثناء بناء الملف). */
    @Volatile var onProgress: (Int) -> Unit = {}

    val headers = listOf("الاسم", "رقم الهوية", "تاريخ الميلاد", "العمر", "الجنس", "الصلة", "الجوال", "العنوان", "الحالة الاجتماعية", "رب أسرة", "رقم الأسرة")
    private val rels = mapOf("head" to "رب الأسرة", "wife" to "زوجة", "husband" to "زوج", "son" to "ابن", "daughter" to "ابنة", "child" to "ابن/ابنة")

    fun rows(persons: List<JSONObject>, headsOnly: Boolean): List<List<String>> =
        persons.filter { !headsOnly || it.optBoolean("isFamilyHead") }
            .sortedWith(compareBy({ it.optString("familyId") }, { !it.optBoolean("isFamilyHead") }, { it.optString("fullName") }))
            .map { p ->
                listOf(p.optString("fullName"), p.optString("nationalId"), p.optString("birthDate").takeIf { it != "null" }.orEmpty(), Stats.ageOf(p)?.toString().orEmpty(),
                    when (p.optString("gender")) { "male" -> "ذكر"; "female" -> "أنثى"; else -> "" }, rels[p.optString("relation")] ?: p.optString("relation"),
                    p.optString("phone"), p.optString("address"), p.optString("maritalStatus"), if (p.optBoolean("isFamilyHead")) "نعم" else "", p.optString("familyId"))
            }

    private fun csvCell(s: String) = "\"" + s.replace("\"", "\"\"").let { if (it.firstOrNull() in listOf('=', '+', '-', '@')) "'$it" else it } + "\""

    fun csv(rows: List<List<String>>, hdr: List<String> = headers): ByteArray {
        val sb = StringBuilder("﻿")
        (listOf(hdr) + rows).forEach { r -> sb.append(r.joinToString(",") { csvCell(it) }).append("\r\n") }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").filter { it.code >= 32 || it == '\n' }
    private fun col(i: Int): String { var n = i + 1; val sb = StringBuilder(); while (n > 0) { val r = (n - 1) % 26; sb.append('A' + r); n = (n - 1) / 26 }; return sb.reverse().toString() }

    private val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    /** أنماط: 0 افتراضي، 1..4 ترويسة بأربع ألوان، 5 جسم، 6 جسم مظلّل. */
    private val stylesXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><styleSheet xmlns=\"$NS\"><fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Arial\"/></font><font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Arial\"/></font></fonts>" +
        "<fills count=\"8\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>" +
        listOf("FF16365C", "FF0E7C86", "FF5B6B8C", "FF8A5A2B", "FFF1F5F9", "FFFFFFFF").joinToString("") { "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"$it\"/></patternFill></fill>" } + "</fills>" +
        "<borders count=\"2\"><border><left/><right/><top/><bottom/><diagonal/></border><border><left style=\"thin\"><color rgb=\"FFD5DDE6\"/></left><right style=\"thin\"><color rgb=\"FFD5DDE6\"/></right><top style=\"thin\"><color rgb=\"FFD5DDE6\"/></top><bottom style=\"thin\"><color rgb=\"FFD5DDE6\"/></bottom><diagonal/></border></borders>" +
        "<cellStyleXfs count=\"1\"><xf/></cellStyleXfs><cellXfs count=\"7\"><xf/>" +
        (2..5).joinToString("") { "<xf fontId=\"1\" fillId=\"$it\" borderId=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>" } +
        "<xf fontId=\"0\" fillId=\"7\" borderId=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"right\" vertical=\"center\"/></xf>" +
        "<xf fontId=\"0\" fillId=\"6\" borderId=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"right\" vertical=\"center\"/></xf></cellXfs></styleSheet>"

    fun xlsx(rows: List<List<String>>, sheet: String, hdr: List<String> = headers, hidden: List<List<String>>? = null, bands: List<Int>? = null, freezeCols: Int = 0): ByteArray {
        val bo = ByteArrayOutputStream()
        ZipOutputStream(bo).use { z ->
            fun put(n: String, s: String) { z.putNextEntry(ZipEntry(n)); z.write(s.toByteArray(Charsets.UTF_8)); z.closeEntry() }
            put("[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>${if (hidden != null) "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" else ""}</Types>")
            put("_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>")
            put("xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"${esc(sheet.take(31).replace(Regex("[\\[\\]*?/\\\\:]"), " "))}\" sheetId=\"1\" r:id=\"rId1\"/>${if (hidden != null) "<sheet name=\"${Sig.SHEET}\" sheetId=\"2\" state=\"hidden\" r:id=\"rId2\"/>" else ""}</sheets></workbook>")
            put("xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>${if (hidden != null) "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>" else ""}<Relationship Id=\"rId9\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>")
            put("xl/styles.xml", stylesXml)
            val sb = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView rightToLeft=\"1\" workbookViewId=\"0\"><pane ${if (freezeCols > 0) "xSplit=\"$freezeCols\" " else ""}ySplit=\"1\" topLeftCell=\"${col(freezeCols)}2\" activePane=\"${if (freezeCols > 0) "bottomRight" else "bottomLeft"}\" state=\"frozen\"/></sheetView></sheetViews><sheetFormatPr defaultRowHeight=\"20\"/><cols>${hdr.indices.joinToString("") { i -> val w = (listOf(hdr[i].length + 4) + rows.take(300).map { it.getOrNull(i).orEmpty().length + 3 }).max().coerceIn(8, 34); "<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"$w\" customWidth=\"1\"/>" }}</cols><sheetData>")
            (listOf(hdr) + rows).forEachIndexed { ri, r ->
                if (ri % 300 == 0) onProgress(20 + 65 * ri / (rows.size + 1))
                sb.append("<row r=\"${ri + 1}\"${if (ri == 0) " ht=\"32\" customHeight=\"1\"" else ""}>")
                r.forEachIndexed { ci, v -> val st = if (ri == 0) 1 + ((bands?.getOrNull(ci) ?: 0) % 4) else if (ri % 2 == 0) 6 else 5; sb.append("<c r=\"${col(ci)}${ri + 1}\" s=\"$st\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(v)}</t></is></c>") }
                sb.append("</row>")
            }
            put("xl/worksheets/sheet1.xml", sb.append("</sheetData><autoFilter ref=\"A1:${col(hdr.size - 1)}${rows.size + 1}\"/></worksheet>").toString())
            if (hidden != null) {
                val hb = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
                hidden.forEachIndexed { ri, r ->
                    hb.append("<row r=\"${ri + 1}\">")
                    r.forEachIndexed { ci, v -> hb.append("<c r=\"${col(ci)}${ri + 1}\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(v)}</t></is></c>") }
                    hb.append("</row>")
                }
                put("xl/worksheets/sheet2.xml", hb.append("</sheetData></worksheet>").toString())
            }
        }
        return bo.toByteArray()
    }

    /** PDF أفقي A4: جدول من اليمين لليسار بأعمدة الاسم والهوية والعمر والجنس والصلة والجوال. */
    fun pdf(rows: List<List<String>>, title: String, hdr: List<String> = headers): ByteArray {
        val idx = listOf(0, 1, 3, 4, 5, 6)
        val widths = listOf(250f, 120f, 40f, 50f, 90f, 100f)
        val w = 842; val h = 595; val margin = 28f; val rowH = 20f
        val doc = PdfDocument()
        val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = 0xFF111111.toInt(); typeface = Typeface.DEFAULT }
        val bold = Paint(txt).apply { typeface = Typeface.DEFAULT_BOLD }
        val perPage = ((h - 2 * margin - 40f) / rowH).toInt() - 1
        val pages = rows.chunked(perPage).ifEmpty { listOf(emptyList()) }
        pages.forEachIndexed { pi, chunk ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, pi + 1).create())
            val c = page.canvas
            c.drawText("$title — ${pi + 1}/${pages.size}", w - margin, margin + 8f, bold.apply { textAlign = Paint.Align.RIGHT; textSize = 12f })
            bold.textSize = 10f
            var y = margin + 36f
            fun line(vals: List<String>, p: Paint) {
                var x = w - margin
                idx.forEachIndexed { k, i ->
                    val cell = vals.getOrElse(i) { "" }
                    p.textAlign = Paint.Align.RIGHT
                    c.drawText(if (p.measureText(cell) > widths[k] - 6) cell.take(((widths[k] - 6) / 5.2f).toInt()) else cell, x - 3f, y, p)
                    x -= widths[k]
                }
                y += rowH
            }
            line(hdr, bold); chunk.forEach { line(it, txt) }
            doc.finishPage(page)
        }
        val bo = ByteArrayOutputStream(); doc.writeTo(bo); doc.close()
        return bo.toByteArray()
    }

    fun save(name: String, mime: String, bytes: ByteArray, count: Int): ExportResult = runCatching {
        if (Build.VERSION.SDK_INT >= 29) {
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name); put(MediaStore.MediaColumns.MIME_TYPE, mime); put(MediaStore.MediaColumns.RELATIVE_PATH, rel)
            }
            val uri = ctx.contentResolver.insert(MediaStore.Files.getContentUri("external"), cv) ?: return ExportResult(false, name, null, 0)
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return ExportResult(false, name, null, 0)
            ExportResult(true, name, uri, count)
        } else { legacyDir.mkdirs(); File(legacyDir, name).writeBytes(bytes); ExportResult(true, name, null, count) }
    }.getOrDefault(ExportResult(false, name, null, 0))

    val familyHeaders = listOf("رب الأسرة", "رقم الهوية", "الجوال", "جوال إضافي", "العنوان", "الحالة الاجتماعية", "المأوى", "عدد الأفراد", "الزوج/ة", "الأبناء والأفراد")

    /** صف واحد لكل أسرة: رب الأسرة وبيانات التواصل والمأوى وعدد الأفراد وأسماء الباقين. */
    fun familyRows(persons: List<JSONObject>, onlyFamily: String? = null): List<List<String>> =
        persons.groupBy { it.optString("familyId").ifEmpty { "i_" + it.optString("id") } }
            .filter { onlyFamily == null || it.key == onlyFamily }
            .mapNotNull { (_, ms) ->
                val h = ms.firstOrNull { it.optBoolean("isFamilyHead") } ?: return@mapNotNull null
                val spouse = ms.firstOrNull { it.optString("relation") in listOf("wife", "husband", "spouse") }
                listOf(h.optString("fullName"), h.optString("nationalId"), h.optString("phone"), PFields.phone2(h), h.optString("address"), h.optString("maritalStatus"),
                    shelters[h.optString("shelterType")].orEmpty(), ms.size.toString(), spouse?.optString("fullName").orEmpty(),
                    ms.filter { it !== h && it !== spouse }.joinToString("، ") { it.optString("fullName") })
            }.sortedBy { it[0] }

    /** يحفظ صفوفاً جاهزة بصيغة kind ويعيد النتيجة. */
    /** أرباب الأسر فقط: أعمدة رب الأسرة من الكشف الأفقي دون الزوجة والأبناء. */
    fun headsOnly(persons: List<JSONObject>): Wide { val w = wide(persons); val n = 14; return Wide(w.headers.take(n), w.bands.take(n), w.rows.map { it.take(n) }) }

    fun runRows(kind: String, baseName: String, title: String, rows: List<List<String>>, hdr: List<String>, bands: List<Int>? = null): ExportResult {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        val base = "$baseName-$stamp".replace(Regex("[\\\\/:*?\"<>|]"), "-")
        return when (kind) {
            "xlsx" -> save("$base.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx(rows, title, hdr, null, bands, if (bands != null) 3 else 0), rows.size)
            "csv" -> save("$base.csv", "text/csv", csv(rows, hdr), rows.size)
            else -> save("$base.pdf", "application/pdf", pdf(rows, title, hdr), rows.size)
        }
    }

    class Col(val k: String, val label: String, val get: (JSONObject) -> String)

    private fun yn(b: Boolean) = if (b) "نعم" else ""
    private fun aidSum(p: JSONObject) = Aid.list(p).size.toString().takeIf { it != "0" }.orEmpty()
    private val shelters = mapOf("tent_ok" to "خيمة سليمة", "tent_damaged" to "خيمة تالفة", "shared" to "مأوى مشترك", "building_damaged" to "مبنى متضرر", "none" to "بلا مأوى")

    /** أعمدة التقرير المخصص كما في الويب: الأساسية أولاً ثم الصحية والمأوى والمساعدات. */
    val columns = listOf(
        Col("name", "الاسم") { it.optString("fullName") }, Col("nid", "رقم الهوية") { it.optString("nationalId") },
        Col("dob", "تاريخ الميلاد") { it.optString("birthDate").takeIf { v -> v != "null" }.orEmpty() }, Col("age", "العمر") { Stats.ageOf(it)?.toString().orEmpty() },
        Col("gender", "الجنس") { when (it.optString("gender")) { "male" -> "ذكر"; "female" -> "أنثى"; else -> "" } },
        Col("rel", "الصلة") { rels[it.optString("relation")] ?: it.optString("relation") },
        Col("phone", "الجوال") { it.optString("phone") }, Col("phone2", "جوال إضافي") { PFields.phone2(it) },
        Col("addr", "العنوان") { it.optString("address") }, Col("marital", "الحالة الاجتماعية") { it.optString("maritalStatus") },
        Col("shelter", "المأوى") { shelters[it.optString("shelterType")].orEmpty() },
        Col("preg", "حامل") { yn(it.optBoolean("isPregnant")) }, Col("bf", "مرضعة") { yn(it.optBoolean("isBreastfeeding")) },
        Col("chronic", "مرض مزمن") { p -> if (p.optBoolean("hasChronicDisease")) (p.optString("diseaseDetails").ifEmpty { "نعم" }) else "" },
        Col("dis", "إعاقة") { p -> if (p.optBoolean("hasDisability")) (p.optString("disabilityDetails").ifEmpty { "نعم" }) else "" },
        Col("urgent", "حالة طارئة") { yn(it.optBoolean("urgentNeed")) },
        Col("aid", "عدد المساعدات") { aidSum(it) }, Col("notes", "ملاحظات") { it.optString("notes") },
        Col("fid", "رقم الأسرة") { it.optString("familyId") },
    )
    val defaultCols = listOf("name", "nid", "age", "gender", "rel", "phone", "addr")

    /** تقرير مخصص: الفلتر يحدد الأفراد، byFamily يكتفي برب الأسرة الذي تنتمي إليه سجلات مطابقة. */
    fun custom(kind: String, campName: String, persons: List<JSONObject>, filter: String, byFamily: Boolean, keys: List<String>): ExportResult {
        var sel = PFilter.apply(filter, persons)
        if (byFamily) { val fids = sel.map { it.optString("familyId") }.toHashSet(); sel = persons.filter { it.optBoolean("isFamilyHead") && it.optString("familyId") in fids } }
        val cols = columns.filter { it.k in keys }.ifEmpty { columns.filter { it.k in defaultCols } }
        val r = sel.sortedWith(compareBy({ it.optString("familyId") }, { !it.optBoolean("isFamilyHead") }, { it.optString("fullName") })).map { p -> cols.map { it.get(p) } }
        val h = cols.map { it.label }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        val base = "$campName-${PFilter.labels[filter] ?: filter}-$stamp".replace(Regex("[\\\\/:*?\"<>|]"), "-")
        return when (kind) {
            "xlsx" -> save("$base.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx(r, campName, h), r.size)
            "csv" -> save("$base.csv", "text/csv", csv(r, h), r.size)
            else -> save("$base.pdf", "application/pdf", pdf(r, campName, h), r.size)
        }
    }

    class Wide(val headers: List<String>, val bands: List<Int>, val rows: List<List<String>>)

    /** كشف أفقي: صف لكل أسرة (رب الأسرة بكل بياناته + الزوجة + الأبناء مرتّبين بالعمر + أفراد آخرون) بنفس نموذج الاستيراد. */
    fun wide(persons: List<JSONObject>): Wide {
        class Fam(val head: JSONObject, val spouse: JSONObject?, val kids: List<JSONObject>, val others: List<JSONObject>)
        val fams = persons.groupBy { it.optString("familyId").ifEmpty { "i_" + it.optString("id") } }.values.map { ms ->
            val head = ms.firstOrNull { it.optBoolean("isFamilyHead") } ?: ms.first()
            val spouse = ms.firstOrNull { it !== head && it.optString("relation") in listOf("wife", "husband", "spouse") }
            val kids = ms.filter { it !== head && it !== spouse && it.optString("relation") in listOf("son", "daughter", "child") }
                .sortedWith(compareBy({ it.optString("birthDate").takeIf { d -> d.length >= 4 && d != "null" } ?: "9999" }, { it.optString("fullName") }))
            Fam(head, spouse, kids, ms.filter { it !== head && it !== spouse && it !in kids })
        }.sortedBy { it.head.optString("fullName") }
        val maxKids = fams.maxOfOrNull { it.kids.size } ?: 0
        val h = ArrayList<String>(); val b = ArrayList<Int>()
        fun add(label: String, band: Int) { h += label; b += band }
        listOf("م", "الاسم الرباعي", "رقم الهوية", "تاريخ الميلاد", "العمر", "الجنس", "الحالة الاجتماعية", "الجوال 1", "الجوال 2", "العنوان", "الحالة الصحية", "المأوى", "ملاحظات", "عدد أفراد الأسرة").forEach { add(it, 0) }
        listOf("اسم الزوجة", "رقم هوية الزوجة", "تاريخ ميلاد الزوجة", "جوال الزوجة").forEach { add(it, 1) }
        for (k in 1..maxKids) { val band = 2 + (k % 2); add("اسم الابن $k", band); add("رقم هوية الابن $k", band); add("تاريخ ميلاد الابن $k", band); add("جنس الابن $k", band) }
        add("أفراد آخرون", 0)
        fun dob(p: JSONObject) = p.optString("birthDate").takeIf { it != "null" }.orEmpty()
        fun sex(p: JSONObject) = when (p.optString("gender")) { "male" -> "ذكر"; "female" -> "أنثى"; else -> "" }
        fun health(p: JSONObject) = listOfNotNull(
            if (p.optBoolean("hasChronicDisease")) p.optString("diseaseDetails").ifEmpty { "مرض مزمن" } else null,
            if (p.optBoolean("hasDisability")) "إعاقة: " + p.optString("disabilityDetails").ifEmpty { "نعم" } else null).joinToString("، ")
        val rows = fams.mapIndexed { i, f ->
            val hd = f.head; val r = ArrayList<String>()
            r += listOf((i + 1).toString(), hd.optString("fullName"), hd.optString("nationalId"), dob(hd), Stats.ageOf(hd)?.toString().orEmpty(), sex(hd), hd.optString("maritalStatus"),
                hd.optString("phone"), PFields.phone2(hd), hd.optString("address"), health(hd), shelters[hd.optString("shelterType")] ?: PFields.living(hd), hd.optString("notes"), (1 + (f.spouse?.let { 1 } ?: 0) + f.kids.size + f.others.size).toString())
            r += listOf(f.spouse?.optString("fullName").orEmpty(), f.spouse?.optString("nationalId").orEmpty(), f.spouse?.let(::dob).orEmpty(), f.spouse?.optString("phone").orEmpty())
            for (k in 0 until maxKids) { val c = f.kids.getOrNull(k); r += listOf(c?.optString("fullName").orEmpty(), c?.optString("nationalId").orEmpty(), c?.let(::dob).orEmpty(), c?.let(::sex).orEmpty()) }
            r += f.others.joinToString("؛ ") { o -> listOf(o.optString("fullName"), rels[o.optString("relation")] ?: o.optString("relation"), o.optString("nationalId")).filter { it.isNotEmpty() }.joinToString(" — ") }
            r
        }
        return Wide(h, b, rows)
    }

    fun runWide(kind: String, camp: String, campName: String, persons: List<JSONObject>): ExportResult {
        val w = wide(persons)
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        val base = "$campName-كشف-الأسر-$stamp".replace(Regex("[\\\\/:*?\"<>|]"), "-")
        return if (kind == "csv") save("$base.csv", "text/csv", csv(w.rows, w.headers), w.rows.size)
        else save("$base.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx(w.rows, campName, w.headers, Sig.rows(camp, campName, persons), w.bands, 3), w.rows.size)
    }

    fun run(kind: String, camp: String, campName: String, persons: List<JSONObject>, headsOnly: Boolean): ExportResult {
        val r = rows(persons, headsOnly)
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        val base = "$campName-$stamp".replace(Regex("[\\\\/:*?\"<>|]"), "-")
        return when (kind) {
            "xlsx" -> save("$base.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx(r, campName, headers, if (headsOnly) null else Sig.rows(camp, campName, persons)), r.size)
            "csv" -> save("$base.csv", "text/csv", csv(r), r.size)
            else -> save("$base.pdf", "application/pdf", pdf(r, campName), r.size)
        }
    }
}
