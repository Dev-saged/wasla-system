package dev.saged.grcs.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportFilesTest {
    private fun f(n: String) = ExFile(n, 1, 0, null, null)
    @Test fun kindsAndMime() {
        assertEquals("كشف كامل", f("م-كشف-الأسر-1.xlsx").kind)
        assertEquals("أرباب الأسر", f("م-أرباب-الأسر.xlsx").kind)
        assertEquals("أفراد", f("م-أفراد.csv").kind)
        assertEquals("text/csv", f("a.csv").mime)
    }
}
