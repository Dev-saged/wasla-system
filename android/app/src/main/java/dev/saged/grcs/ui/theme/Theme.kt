package dev.saged.grcs.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.em
import dev.saged.grcs.config.BrandConfig

@Immutable
data class Palette(
    val bg: Color, val surface: Color, val surface2: Color,
    val ink: Color, val mute: Color, val line: Color,
    val pri: Color, val onPri: Color, val acc: Color,
    val danger: Color, val ok: Color, val warn: Color,
    val navy: Color, val navyL: Color, val tealL: Color, val purple: Color, val blue: Color,
    val pregnant: Color, val success: Color, val gray: Color,
    val tierHigh: Color, val tierMedium: Color, val tierStable: Color,
    val tealD: Color, val head: Color, val track: Color,
    val dark: Boolean,
)

private fun color(o: BrandConfig.Oklch): Color {
    val (r, g, b) = OklchMath.srgb(o)
    return Color(r, g, b)
}

fun paletteOf(dark: Boolean): Palette {
    val p = BrandConfig.Palette
    fun pick(t: BrandConfig.Tone) = color(if (dark) t.dark else t.light)
    return Palette(
        bg = pick(p.bg), surface = pick(p.surface), surface2 = pick(p.surface2),
        ink = pick(p.ink), mute = pick(p.mute), line = pick(p.line),
        pri = pick(p.pri), onPri = pick(p.onPri), acc = pick(p.acc),
        danger = pick(p.danger), ok = pick(p.ok), warn = pick(p.warn),
        navy = pick(p.navy), navyL = pick(p.navyL), tealL = pick(p.tealL), purple = pick(p.purple), blue = pick(p.blue),
        pregnant = pick(p.pregnant), success = pick(p.success), gray = pick(p.gray),
        tierHigh = pick(p.tierHigh), tierMedium = pick(p.tierMedium), tierStable = pick(p.tierStable),
        tealD = pick(p.tealD), head = pick(p.head), track = pick(p.track),
        dark = dark,
    )
}

/** تفضيل المظهر: 0 تلقائي (الجهاز)، 1 فاتح، 2 ليلي «ليل الخيام». */
object ThemePref {
    private var prefs: android.content.SharedPreferences? = null
    var mode by androidx.compose.runtime.mutableIntStateOf(0)
    fun init(ctx: android.content.Context) {
        prefs = ctx.getSharedPreferences("grcs_theme", android.content.Context.MODE_PRIVATE)
        mode = prefs?.getInt("mode", 0) ?: 0
    }
    fun set(m: Int) { mode = m; prefs?.edit()?.putInt("mode", m)?.apply() }
}

val LocalPalette = staticCompositionLocalOf { paletteOf(dark = true) }

/** المظهر تلقائي يتبع الجهاز، والاتجاه من اليمين لليسار دائماً. */
@Composable
fun GrcsTheme(content: @Composable () -> Unit) {
    val dark = when (ThemePref.mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val p = paletteOf(dark)
    val scheme = if (dark) {
        darkColorScheme(primary = p.pri, onPrimary = p.onPri, background = p.bg, onBackground = p.ink,
            surface = p.surface, onSurface = p.ink, error = p.danger)
    } else {
        lightColorScheme(primary = p.pri, onPrimary = p.onPri, background = p.bg, onBackground = p.ink,
            surface = p.surface, onSurface = p.ink, error = p.danger)
    }
    CompositionLocalProvider(
        LocalPalette provides p,
        LocalLayoutDirection provides LayoutDirection.Rtl,
        // تكبير خط النظام يُحدّ حتى لا تتجاوز الأزرار والنصوص عرض الشاشة
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.platform.LocalDensity.current.let { androidx.compose.ui.unit.Density(it.density, it.fontScale.coerceAtMost(1.1f)) },
    ) {
        MaterialTheme(colorScheme = scheme) {
            // سطر النص كما في CSS: ارتفاع 1.6 يُحترم حرفياً حتى لسطر واحد
            ProvideTextStyle(TextStyle(fontFamily = GrcsFonts.body, lineHeight = 1.6.em,
                lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.None),
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)), content)
        }
    }
}
