package dev.saged.grcs.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** أيقونات الواجهة مرسومة يدوياً على شبكة 24 بخط 1.7 وأطراف مدوّرة. */
object QIcons {
    private fun icon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach {
                addPath(
                    PathParser().parsePathString(it).toNodes(),
                    stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f,
                    strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun round(x: Float, y: Float) =
        "M${x + 1.6f},$y h2.8 a1.6,1.6 0 0 1 1.6,1.6 v2.8 a1.6,1.6 0 0 1 -1.6,1.6 h-2.8 a1.6,1.6 0 0 1 -1.6,-1.6 v-2.8 a1.6,1.6 0 0 1 1.6,-1.6 Z"

    val Home by lazy { icon("home", "M3.5 11 12 4l8.5 7M6 9.5V20h12V9.5M10 20v-5.5h4V20") }
    val Camp by lazy { icon("camp", "M12 4 3.5 19.5h17L12 4ZM12 19.5l-3-5.5a3.4 3.4 0 0 1 6 0l-3 5.5ZM2 21h20") }
    val People by lazy {
        icon("people", "M5.8,8 a3.2,3.2 0 1,0 6.4,0 a3.2,3.2 0 1,0 -6.4,0",
            "M3 20c.4-3.6 3-5.6 6-5.6s5.6 2 6 5.6M16 5.2a3 3 0 0 1 0 5.6M18.4 14.9c1.4.9 2.3 2.4 2.6 4.6")
    }
    val Chat by lazy { icon("chat", "M4 5.5h16v10H12l-4.5 4v-4H4v-10ZM8 9.5h8M8 12.5h5") }
    val More by lazy {
        icon("more", round(4.5f, 4.5f), round(13.5f, 4.5f), round(4.5f, 13.5f), "M16.5 13.5v6M13.5 16.5h6")
    }
}
