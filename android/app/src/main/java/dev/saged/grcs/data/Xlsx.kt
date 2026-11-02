package dev.saged.grcs.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

/** قارئ xlsx خفيف (الورقة الأولى فقط) دون مكتبات: يفكّ الضغط ويقرأ sharedStrings والخلايا. */
object Xlsx {
    fun read(input: InputStream): List<List<String>> {
        var shared = listOf<String>()
        val sheets = sortedMapOf<String, ByteArray>()
        ZipInputStream(input).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                val n = e.name
                if (n == "xl/sharedStrings.xml" || Regex("xl/worksheets/sheet\\d+\\.xml").matches(n)) {
                    val b = z.readBytes()
                    if (n == "xl/sharedStrings.xml") shared = parseShared(b) else sheets[n] = b
                }
            }
        }
        val first = sheets.entries.minByOrNull { Regex("\\d+").find(it.key)!!.value.toInt() }?.value ?: return emptyList()
        return parseSheet(first, shared)
    }

    private fun parseShared(b: ByteArray): List<String> {
        val out = ArrayList<String>()
        val p = Xml.newPullParser().apply { setInput(b.inputStream(), "UTF-8") }
        var inSi = false; var inT = false; val sb = StringBuilder()
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            when (ev) {
                XmlPullParser.START_TAG -> if (p.name == "si") { inSi = true; sb.setLength(0) } else if (p.name == "t" && inSi) inT = true
                XmlPullParser.TEXT -> if (inT) sb.append(p.text)
                XmlPullParser.END_TAG -> if (p.name == "t") inT = false else if (p.name == "si") { inSi = false; out += sb.toString() }
            }
            ev = p.next()
        }
        return out
    }

    private fun col(ref: String): Int {
        var n = 0
        for (c in ref) { if (c in 'A'..'Z') n = n * 26 + (c - 'A' + 1) else break }
        return n - 1
    }

    private fun parseSheet(b: ByteArray, shared: List<String>): List<List<String>> {
        val rows = ArrayList<List<String>>()
        val p = Xml.newPullParser().apply { setInput(b.inputStream(), "UTF-8") }
        var row = ArrayList<String>(); var type = ""; var ci = 0; var inV = false; var inT = false; val sb = StringBuilder()
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            when (ev) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> row = ArrayList()
                    "c" -> { type = p.getAttributeValue(null, "t").orEmpty(); ci = col(p.getAttributeValue(null, "r").orEmpty()); sb.setLength(0) }
                    "v" -> inV = true
                    "t" -> inT = true
                }
                XmlPullParser.TEXT -> if (inV || inT) sb.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "v", "t" -> { inV = false; inT = false }
                    "c" -> {
                        val v = if (type == "s") shared.getOrElse(sb.toString().trim().toIntOrNull() ?: -1) { "" } else sb.toString()
                        while (row.size < ci) row.add("")
                        row.add(v.trim())
                    }
                    "row" -> rows += row
                }
            }
            ev = p.next()
        }
        return rows
    }

    /** CSV بسيط (فاصلة أو فاصلة منقوطة، اقتباس مزدوج). */
    fun readCsv(text: String): List<List<String>> {
        val sep = if (text.lineSequence().first().count { it == ';' } > text.lineSequence().first().count { it == ',' }) ';' else ','
        val rows = ArrayList<List<String>>(); var row = ArrayList<String>(); val cur = StringBuilder(); var q = false
        val t = text.removePrefix("﻿")
        var i = 0
        while (i < t.length) {
            val c = t[i]
            when {
                q && c == '"' && i + 1 < t.length && t[i + 1] == '"' -> { cur.append('"'); i++ }
                c == '"' -> q = !q
                !q && c == sep -> { row.add(cur.toString().trim()); cur.setLength(0) }
                !q && (c == '\n' || c == '\r') -> { if (c == '\n' || cur.isNotEmpty() || row.isNotEmpty()) { row.add(cur.toString().trim()); cur.setLength(0); if (row.any { it.isNotEmpty() }) rows += row; row = ArrayList() } }
                else -> cur.append(c)
            }
            i++
        }
        if (cur.isNotEmpty() || row.isNotEmpty()) { row.add(cur.toString().trim()); if (row.any { it.isNotEmpty() }) rows += row }
        return rows
    }
}
