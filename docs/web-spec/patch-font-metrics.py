"""يضبط المقاييس الرأسية لخط Cairo (صعود 695، نزول 305 = 1em) حتى يُحترم ارتفاع السطر المحدد كما في CSS: python3 patch-font-metrics.py <font dir>"""
import sys, glob
from fontTools.ttLib import TTFont
for f in glob.glob(sys.argv[1] + "/cairo_*.ttf"):
    t = TTFont(f); h, o = t["hhea"], t["OS/2"]
    h.ascent, h.descent, h.lineGap = 695, -305, 0
    o.sTypoAscender, o.sTypoDescender, o.sTypoLineGap = 695, -305, 0
    o.usWinAscent, o.usWinDescent = 695, 305
    t.save(f); print(f)
