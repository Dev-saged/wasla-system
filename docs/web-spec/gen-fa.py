"""يولّد Fa.kt من أيقونات Font Awesome Free 6.5.1 (solid) المستعملة في index.html: python3 gen-fa.py <index.html> <svgs/solid> <Fa.kt>"""
import re, sys, os
html, svgd, out = sys.argv[1:]
names = sorted({n for n in re.findall(r"fa-([a-z0-9-]+)", open(html, encoding="utf-8").read()) if os.path.exists(f"{svgd}/{n}.svg")})
names = sorted(set(names) | {"eye-slash", "chevron-right", "chevron-up", "circle", "bars", "ellipsis-vertical", "calendar-days", "moon", "sun", "phone", "location-dot", "share-nodes", "file-pdf", "floppy-disk", "star"})
def ident(n): return "".join(p.capitalize() for p in n.split("-")).replace("0", "Zero")
L = ["package dev.saged.grcs.ui.icons", "", "import androidx.compose.ui.graphics.Color", "import androidx.compose.ui.graphics.SolidColor", "import androidx.compose.ui.graphics.vector.ImageVector", "import androidx.compose.ui.graphics.vector.PathParser", "import androidx.compose.ui.unit.dp", "",
     "/** أيقونات Font Awesome Free 6.5.1 (solid، رخصة CC BY 4.0) المستعملة في نسخة الويب، بنفس أسمائها. ملف مولَّد: docs/web-spec/gen-fa.py */", "object Fa {",
     "    private fun fa(name: String, w: Float, d: String): ImageVector {", "        val s = maxOf(w, 512f)",
     "        return ImageVector.Builder(name, 24.dp, 24.dp, s, s).apply {", "            addGroup(translationX = (s - w) / 2f, translationY = (s - 512f) / 2f)", "            addPath(PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color.Black))", "            clearGroup()", "        }.build()", "    }"]
for n in names:
    s = open(f"{svgd}/{n}.svg").read()
    w = re.search(r'viewBox="0 0 (\d+) 512"', s).group(1); d = re.search(r'<path d="([^"]+)"', s).group(1)
    L.append(f'    val {ident(n)} by lazy {{ fa("{n}", {w}f, "{d}") }}')
L.append("}")
open(out, "w").write("\n".join(L) + "\n"); print(len(names), "icons")
