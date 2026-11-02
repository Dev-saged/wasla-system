package dev.saged.grcs.config

/** نقطة التحكم الوحيدة بالهوية: الاسم، الألوان (OKLCH)، الروابط. لا ألوان أو نصوص هوية خارجها. */
object BrandConfig {
    const val NAME = "منظومة إدارة المخيمات"
    const val NAME_LATIN = "GRCS"
    val NAME_LINES = listOf("منظومة إدارة", "المخيمات")
    const val TAGLINE = "سجل واحد لكل أسرة، في كل مخيم"
    const val UPDATE_REPO = "Dev-saged/wasla-system"

    /** مجلد النسخ الاحتياطية داخل المستندات على الجهاز. */
    const val BACKUP_DIR = "منظومة إدارة المخيمات"

    /** واتساب مالك المنصة لطلبات التفعيل والمميزات المدفوعة. */
    const val OWNER_WHATSAPP = "972597857250"

    /** Worker العمليات الإدارية (إنشاء الحسابات، النسخ الاحتياطي، التحديثات). */
    const val ADMIN_API = "https://grcs-push.dev-mr07.workers.dev"

    /** بطاقة المطوّر في "المزيد" وسطر أسفل شاشة الدخول. */
    object Dev {
        const val NAME = "ساجد العبادلة"
        const val TITLE = "المهندس ساجد العبادلة"
        const val PLACE = "صُنع بكل حب من فلسطين — غزة"
        const val YEAR = 2026
        val LINKS = listOf(
            "whatsapp" to "https://wa.me/972597857250",
            "telegram" to "https://t.me/+972597857250",
            "instagram" to "https://instagram.com/sajedd.0",
            "facebook" to "https://facebook.com/saged.t",
            "x" to "https://x.com/saged_al",
            "github" to "https://github.com/Dev-saged",
        )
    }

    /** كل لون زوج [فاتح، داكن] بصيغة OKLCH: إضاءة 0..1، تشبّع، زاوية. */
    data class Oklch(val l: Float, val c: Float, val h: Float)
    data class Tone(val light: Oklch, val dark: Oklch)

    private fun t(ll: Float, lc: Float, lh: Float, dl: Float, dc: Float, dh: Float) =
        Tone(Oklch(ll, lc, lh), Oklch(dl, dc, dh))

    /** ألوان شاشة الافتتاح: سماء الفجر، الأرض، ونص عاجي. */
    object Opening {
        val skyTop = Oklch(.28f, .11f, 268f)
        val skyMid = Oklch(.5f, .13f, 240f)
        val horizon = Oklch(.86f, .12f, 80f)
        val ground = Oklch(.24f, .07f, 255f)
        val text = Oklch(.97f, .02f, 90f)
        val accent = Oklch(.86f, .12f, 80f)
    }

    /** #RRGGBB → OKLCH (للحفاظ على فحص التباين الحالي). */
    private fun hex(h: String): Oklch {
        val v = h.removePrefix("#").toInt(16)
        fun lin(c: Int): Double { val x = c / 255.0; return if (x <= 0.04045) x / 12.92 else Math.pow((x + 0.055) / 1.055, 2.4) }
        val r = lin(v shr 16 and 255); val g = lin(v shr 8 and 255); val b = lin(v and 255)
        val l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        val L = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
        val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
        val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        val h2 = Math.toDegrees(Math.atan2(bb, a)).let { if (it < 0) it + 360 else it }
        return Oklch(L.toFloat(), Math.hypot(a, bb).toFloat(), h2.toFloat())
    }
    private fun x(light: String, dark: String) = Tone(hex(light), hex(dark))

    /** هوية GRCS: كحلي وتيل على ورق بارد؛ داكن "ليل الخيام". القيم مطابقة لنسخة الويب. */
    object Palette {
        val bg = x("#F1F5F9", "#07111F")
        val surface = x("#FFFFFF", "#0E1B2E")
        val surface2 = x("#F8FAFC", "#13243B")
        val ink = x("#1E293B", "#E3ECF6")
        val mute = x("#5B6B80", "#8FA3BA")
        val line = x("#E2E8F0", "#1F3450")
        val pri = x("#0E7490", "#22B8D6")
        val onPri = x("#FFFFFF", "#07111F")
        val acc = x("#E9B44C", "#E9B44C")
        val danger = x("#DC2626", "#F87171")
        val ok = x("#047857", "#34D399")
        val warn = x("#B45309", "#FBBF24")
        val navy = x("#0F2B52", "#0B1F3A")
        val navyL = x("#1E4272", "#1A3A63")
        val tealL = x("#0891B2", "#3FD0EA")
        val purple = x("#7C3AED", "#A78BFA")
        val blue = x("#2563EB", "#60A5FA")
        val pregnant = x("#EA580C", "#FB923C")
        val success = x("#059669", "#34D399")
        val gray = x("#475569", "#94A3B8")
        val tierHigh = x("#E0663A", "#F08A5D")
        val tierMedium = x("#3E8FB0", "#5FB0D0")
        val tierStable = x("#8594A8", "#7C8CA3")
    }
}
