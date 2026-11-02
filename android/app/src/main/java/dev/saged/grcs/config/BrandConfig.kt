package dev.saged.grcs.config

/** نقطة التحكم الوحيدة بالهوية: الاسم، الألوان (OKLCH)، الروابط. لا ألوان أو نصوص هوية خارجها. */
object BrandConfig {
    const val NAME = "منظومة إدارة المخيمات"
    const val NAME_LATIN = "GRCS"
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

    object Palette {
        val bg = t(.975f, .008f, 95f, .17f, .02f, 225f)
        val surface = t(1f, 0f, 0f, .22f, .025f, 225f)
        val surface2 = t(.95f, .01f, 95f, .27f, .028f, 225f)
        val ink = t(.24f, .03f, 230f, .95f, .01f, 95f)
        val mute = t(.46f, .03f, 230f, .74f, .03f, 225f)
        val line = t(.89f, .01f, 95f, .32f, .025f, 225f)
        val pri = t(.45f, .09f, 200f, .78f, .11f, 190f)
        val onPri = t(.985f, .01f, 95f, .18f, .03f, 220f)
        val danger = t(.5f, .2f, 25f, .74f, .16f, 25f)
        val ok = t(.46f, .12f, 155f, .78f, .14f, 155f)
        val warn = t(.5f, .13f, 65f, .82f, .14f, 80f)
    }
}
