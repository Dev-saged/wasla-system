package dev.saged.grcs.config

/** نقطة التحكم الوحيدة بالهوية: الاسم، الألوان (OKLCH)، الروابط. لا ألوان أو نصوص هوية خارجها. */
object BrandConfig {
    const val NAME = "منظومة إدارة المخيمات"
    const val NAME_LATIN = "GRCS"
    val NAME_LINES = listOf("منظومة إدارة", "المخيمات")
    const val TAGLINE = "سجل واحد لكل أسرة، في كل مخيم"
    const val UPDATE_REPO = "Dev-saged/wasla-system"

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

    /** الهوية "بوصلة": كوبالت على ورق بارد، وبرتقالي دافئ للتمييز. */
    object Palette {
        val bg = t(.975f, .006f, 250f, .17f, .03f, 255f)
        val surface = t(1f, 0f, 0f, .22f, .035f, 255f)
        val surface2 = t(.95f, .012f, 250f, .27f, .04f, 255f)
        val ink = t(.22f, .045f, 255f, .95f, .01f, 250f)
        val mute = t(.46f, .035f, 255f, .76f, .03f, 255f)
        val line = t(.9f, .01f, 250f, .33f, .035f, 255f)
        val pri = t(.47f, .17f, 262f, .79f, .12f, 255f)
        val onPri = t(.99f, .005f, 250f, .18f, .04f, 255f)
        val acc = t(.52f, .15f, 50f, .82f, .15f, 65f)
        val danger = t(.5f, .2f, 25f, .76f, .16f, 25f)
        val ok = t(.45f, .12f, 160f, .8f, .14f, 160f)
        val warn = t(.5f, .13f, 70f, .83f, .14f, 85f)
    }
}
