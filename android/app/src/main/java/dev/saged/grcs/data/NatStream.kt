package dev.saged.grcs.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FilterInputStream
import java.io.InputStream
import java.util.zip.ZipFile

/** قراءة السجل الوطني الضخم بتدفّق: مرور واحد على الورقة، ويُحتفظ فقط بصفوف أرباب الأسر المسجَّلين بالجهاز. */
object NatStream {
    class Out(val headers: List<String>, val rows: List<List<String>>, val scanned: Int)

    private const val SH = "\u0001"
    private class Counting(i: InputStream, val onBytes: (Long) -> Unit) : FilterInputStream(i) {
        var n = 0L
        override fun read(): Int = super.read().also { if (it >= 0) tick(1) }
        override fun read(b: ByteArray, o: Int, l: Int): Int = super.read(b, o, l).also { if (it > 0) tick(it.toLong()) }
        private fun tick(k: Long) { n += k; onBytes(n) }
    }

    fun readXlsx(file: File, wanted: Set<String>, progress: (Float) -> Unit): Out? = ZipFile(file).use { z ->
        val sheet = z.entries().asSequence().filter { Regex("xl/worksheets/sheet\\d+\\.xml").matches(it.name) }
            .minByOrNull { Regex("\\d+").find(it.name)!!.value.toInt() } ?: return null
        val sst = z.getEntry("xl/sharedStrings.xml")
        fun shared(onString: (Int, String) -> Boolean) {
            if (sst == null) return
            z.getInputStream(sst).use { s ->
                val p = Xml.newPullParser().apply { setInput(s, "UTF-8") }
                var i = 0; var inSi = false; var inT = false; val sb = StringBuilder()
                var ev = p.eventType
                while (ev != XmlPullParser.END_DOCUMENT) {
                    when (ev) {
                        XmlPullParser.START_TAG -> if (p.name == "si") { inSi = true; sb.setLength(0) } else if (p.name == "t" && inSi) inT = true
                        XmlPullParser.TEXT -> if (inT) sb.append(p.text)
                        XmlPullParser.END_TAG -> if (p.name == "t") inT = false else if (p.name == "si") { inSi = false; if (!onString(i++, sb.toString())) return }
                    }
                    ev = p.next()
                }
            }
        }
        fun scan(limitRows: Int, onBytes: (Long) -> Unit, onRow: (Int, List<String>) -> Boolean) {
            val src = Counting(z.getInputStream(sheet), onBytes)
            src.use { s ->
                val p = Xml.newPullParser().apply { setInput(s, "UTF-8") }
                var row = ArrayList<String>(); var type = ""; var ci = 0; var inV = false; var inT = false; var rn = 0; val sb = StringBuilder()
                var ev = p.eventType
                while (ev != XmlPullParser.END_DOCUMENT) {
                    when (ev) {
                        XmlPullParser.START_TAG -> when (p.name) {
                            "row" -> row = ArrayList()
                            "c" -> { type = p.getAttributeValue(null, "t").orEmpty(); ci = colOf(p.getAttributeValue(null, "r").orEmpty()); sb.setLength(0) }
                            "v" -> inV = true
                            "t" -> inT = true
                        }
                        XmlPullParser.TEXT -> if (inV || inT) sb.append(p.text)
                        XmlPullParser.END_TAG -> when (p.name) {
                            "v", "t" -> { inV = false; inT = false }
                            "c" -> { while (row.size < ci) row.add(""); row.add(if (type == "s") SH + sb.toString().trim() else sb.toString().trim()) }
                            "row" -> { if (!onRow(rn++, row) || rn >= limitRows) return }
                        }
                    }
                    ev = p.next()
                }
            }
        }

        // 1) صفوف العناوين: أول صفوف الورقة فقط
        val head = ArrayList<List<String>>()
        scan(15, {}) { _, r -> head += r; true }
        if (head.isEmpty()) return null
        val needHead = head.flatten().filter { it.startsWith(SH) }.mapNotNull { it.drop(1).toIntOrNull() }.toHashSet()
        val headStr = HashMap<Int, String>()
        shared { i, s -> if (i in needHead) headStr[i] = s; headStr.size < needHead.size }
        fun resolve(c: String, map: Map<Int, String>) = if (c.startsWith(SH)) map[c.drop(1).toIntOrNull() ?: -1].orEmpty().trim() else c
        val headRows = head.map { r -> r.map { resolve(it, headStr) } }
        val hi = Cells.headerRow(headRows)
        val headers = headRows[hi]
        val nidCol = Cells.cols(headers, Cells.modeOf(headers)).headNid ?: return Out(headers, emptyList(), 0)

        // 2) فهرس النصوص المشتركة: أي نص يطابق هوية مسجَّلة
        val hit = HashSet<Int>()
        shared { i, s -> if (s.length in 8..20 && Nid.norm(s) in wanted) hit += i; true }

        // 3) مرور واحد على الورقة: نحتفظ بصفوف المطابقين فقط
        val total = sheet.size.coerceAtLeast(1L).toFloat()
        var scanned = 0
        val kept = ArrayList<List<String>>()
        var lastTick = 0L
        scan(Int.MAX_VALUE, { b -> if (b - lastTick > 1_000_000) { lastTick = b; progress(b / total) } }) { n, r ->
            if (n > hi) {
                scanned++
                val c = r.getOrNull(nidCol).orEmpty()
                val ok = if (c.startsWith(SH)) (c.drop(1).toIntOrNull() ?: -1) in hit
                else c.isNotEmpty() && (Nid.norm(c) in wanted || c.toDoubleOrNull()?.let { Nid.norm(java.math.BigDecimal(it).toPlainString().substringBefore('.')) in wanted } == true)
                if (ok) kept += r
            }
            true
        }

        // 4) نحلّ النصوص المشتركة للصفوف المحتفَظ بها فقط
        val need = HashSet<Int>()
        for (r in kept) for (c in r) if (c.startsWith(SH)) c.drop(1).toIntOrNull()?.let { need += it }
        val str = HashMap<Int, String>(need.size * 2)
        if (need.isNotEmpty()) shared { i, s -> if (i in need) str[i] = s; str.size < need.size }
        progress(1f)
        Out(headers, kept.map { r -> r.map { resolve(it, str) } }, scanned)
    }

    /** CSV بالتدفق سطراً سطراً. */
    fun readCsv(file: File, wanted: Set<String>, progress: (Float) -> Unit): Out? {
        val total = file.length().coerceAtLeast(1L).toFloat(); var read = 0L
        file.bufferedReader(Charsets.UTF_8).use { br ->
            val first = br.readLine()?.removePrefix("﻿") ?: return null
            val sep = if (first.count { it == ';' } > first.count { it == ',' }) ';' else ','
            val headers = split(first, sep)
            val nidCol = Cells.cols(headers, Cells.modeOf(headers)).headNid ?: return Out(headers, emptyList(), 0)
            val kept = ArrayList<List<String>>(); var scanned = 0
            while (true) {
                val line = br.readLine() ?: break
                read += line.length + 1
                if (scanned % 5000 == 0) progress(read / total)
                if (line.isBlank()) continue
                scanned++
                val r = split(line, sep)
                if (Nid.norm(r.getOrNull(nidCol).orEmpty()) in wanted) kept += r
            }
            progress(1f)
            return Out(headers, kept, scanned)
        }
    }

    private fun split(line: String, sep: Char): List<String> {
        val out = ArrayList<String>(); val cur = StringBuilder(); var q = false; var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                q && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> { cur.append('"'); i++ }
                c == '"' -> q = !q
                !q && c == sep -> { out += cur.toString().trim(); cur.setLength(0) }
                else -> cur.append(c)
            }
            i++
        }
        out += cur.toString().trim()
        return out
    }

    private fun colOf(ref: String): Int {
        var n = 0
        for (c in ref) { if (c in 'A'..'Z') n = n * 26 + (c - 'A' + 1) else break }
        return n - 1
    }
}
