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

    val Settings by lazy { icon("settings", "M4 7h9M17 7h3M4 17h3M11 17h9", "M13,7 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0", "M7,17 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0") }
    val Key by lazy { icon("key", "M15 8.5a4.5 4.5 0 1 0-4.2 4.5L4 19.8V21h2.2v-1.8H8v-1.8h1.8l1.6-1.6A4.5 4.5 0 0 0 15 8.5Z", "M16.2 7.6h.01") }
    val Export by lazy { icon("export", "M12 4v11M7.5 10.5 12 15l4.5-4.5M5 19.5h14") }
    val Import by lazy { icon("import", "M12 15V4M7.5 8.5 12 4l4.5 4.5M5 19.5h14") }
    val Search by lazy { icon("search", "M10.5 17a6.5 6.5 0 1 1 0-13 6.5 6.5 0 0 1 0 13ZM20 20l-4.8-4.8") }
    val Sync by lazy { icon("sync", "M20 11a8 8 0 0 0-14.5-3.5L4 9M4 4v5h5M4 13a8 8 0 0 0 14.5 3.5L20 15M20 20v-5h-5") }
    val Trash by lazy { icon("trash", "M4.5 7h15M9.5 7V4.5h5V7M6.5 7l.8 12.5h9.4L17.5 7M10 11v5M14 11v5") }
    val Shield by lazy { icon("shield", "M12 3l7 3v5c0 4.5-3 8-7 10-4-2-7-5.5-7-10V6l7-3Z", "M9.2 12.2l2 2 3.6-3.8") }
    val Alert by lazy { icon("alert", "M12 4 2.8 19.5h18.4L12 4ZM12 10v4.5M12 17.2v.1") }
    val Check by lazy { icon("check", "M5 12.5l4.5 4.5L19 7.5") }
    val Heart by lazy { icon("heart", "M12 20s-7.5-4.6-7.5-10A4.3 4.3 0 0 1 12 7.3 4.3 4.3 0 0 1 19.5 10c0 5.4-7.5 10-7.5 10Z") }
    val Plus by lazy { icon("plus", "M12 5v14M5 12h14") }
    val Back by lazy { icon("back", "M9 6l6 6-6 6") }
    val Chev by lazy { icon("chev", "M15 6l-6 6 6 6") }
    val Close by lazy { icon("close", "M6 6l12 12M18 6 6 18") }
    val Broadcast by lazy { icon("broadcast", "M4 10v4l9 4V6l-9 4Z", "M16.5 9.5a4 4 0 0 1 0 5M19 7a7.5 7.5 0 0 1 0 10") }
    val Male by lazy { icon("male", "M5.5,14 a4.5,4.5 0 1,0 9,0 a4.5,4.5 0 1,0 -9,0", "M14 10l6-6M15 4h5v5") }
    val Female by lazy { icon("female", "M7,10 a5,5 0 1,0 10,0 a5,5 0 1,0 -10,0", "M12 15v6M9 18h6") }
    val Person by lazy { icon("person", "M8.5,7.5 a3.5,3.5 0 1,0 7,0 a3.5,3.5 0 1,0 -7,0", "M5 20c.5-3.5 3.4-5.5 7-5.5s6.5 2 7 5.5") }
    val Phone by lazy { icon("phone", "M7 3.5h10a1 1 0 0 1 1 1v15a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1v-15a1 1 0 0 1 1-1ZM11 18h2") }
    val IdCard by lazy { icon("idcard", "M3.5 6h17v12h-17ZM7 10.5h4M7 13.5h3", "M14.5,10 a1.5,1.5 0 1,0 3,0 a1.5,1.5 0 1,0 -3,0", "M14 14.5c.3-1 1-1.5 2-1.5s1.7.5 2 1.5") }
    val Moon by lazy { icon("moon", "M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5Z") }
    val Logout by lazy { icon("logout", "M9 4.5H5.5v15H9M14 8l4 4-4 4M18 12H9.5") }
    val Info by lazy { icon("info", "M3.5,12 a8.5,8.5 0 1,0 17,0 a8.5,8.5 0 1,0 -17,0", "M12 11v5.5M12 7.8v.1") }
    val Report by lazy { icon("report", "M6 5.5h12v15H6zM9 3h6v3H9zM9 11h6M9 14.5h6") }
    val Backup by lazy { icon("backup", "M4.5 6.5c0-1.4 3.4-2.5 7.5-2.5s7.5 1.1 7.5 2.5S15.6 9 12 9 4.5 7.9 4.5 6.5ZM4.5 6.5v11c0 1.4 3.4 2.5 7.5 2.5s7.5-1.1 7.5-2.5v-11M4.5 12c0 1.4 3.4 2.5 7.5 2.5s7.5-1.1 7.5-2.5") }
    val Edit by lazy { icon("edit", "M4 20h4L19 9l-4-4L4 16v4ZM13.5 6.5l4 4") }
    val Eye by lazy { icon("eye", "M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z", "M9.2,12 a2.8,2.8 0 1,0 5.6,0 a2.8,2.8 0 1,0 -5.6,0") }
    val Lock by lazy { icon("lock", "M6 11h12v9H6zM8.5 11V8a3.5 3.5 0 0 1 7 0v3") }
    val Transfer by lazy { icon("transfer", "M4 8h13M13.5 4.5 17 8l-3.5 3.5M20 16H7M10.5 12.5 7 16l3.5 3.5") }
    val Clock by lazy { icon("clock", "M3.5,12 a8.5,8.5 0 1,0 17,0 a8.5,8.5 0 1,0 -17,0", "M12 7.5V12l3 2") }
    val Filter by lazy { icon("filter", "M4 5.5h16l-6 7.5v5.5l-4-2V13L4 5.5Z") }
    val Copy by lazy { icon("copy", "M8.5 8.5h10v10h-10zM5.5 15.5v-10h10") }
    val Bolt by lazy { icon("bolt", "M13 3 5 13.5h6L10 21l8-10.5h-6L13 3Z") }
    val Chart by lazy { icon("chart", "M5 20V10M12 20V4M19 20v-7") }
    val Baby by lazy { icon("baby", "M8,10 a4,4 0 1,0 8,0 a4,4 0 1,0 -8,0", "M9.8 9.8h.01M14.2 9.8h.01M10.2 12.2c1 .8 2.6.8 3.6 0M6 20c.7-2.4 2.9-3.7 6-3.7s5.3 1.3 6 3.7") }
    val Pregnant by lazy { icon("pregnant", "M10,5 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0", "M12 8.5c-2 0-3 1.5-3 3.5v1.5c-1 .8-1.5 2-1.5 3.5V21h9v-3.5c0-1.5-.5-2.7-1.5-3.5V12c0-2-1-3.5-3-3.5Z") }
    val Elder by lazy { icon("elder", "M10,5 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0", "M12 8.5v6M9 9.5l3-1 3 1M12 14.5l-2.5 6M12 14.5l2.5 6") }
    val Support by lazy { icon("support", "M5 11.5a7 7 0 0 1 14 0M4 12h2.5v5H4zM17.5 12H20v5h-2.5zM17.5 17c0 1.7-2 3-5.5 3") }
}
