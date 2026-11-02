package dev.saged.grcs.ui.icons

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** نجمة التطريز ذات الثمانية رؤوس: مدخل الهوية البصرية. */
fun tatreezStar(c: Offset, outer: Float, inner: Float = outer * .3f): Path = Path().apply {
    for (i in 0 until 16) {
        val a = (i * 22.5 - 90) * PI / 180
        val r = if (i % 2 == 0) outer else inner
        val x = c.x + r * cos(a).toFloat()
        val y = c.y + r * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
