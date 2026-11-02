package dev.saged.grcs.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import dev.saged.grcs.config.BrandConfig

@Immutable
data class Palette(
    val bg: Color, val surface: Color, val surface2: Color,
    val ink: Color, val mute: Color, val line: Color,
    val pri: Color, val onPri: Color, val acc: Color,
    val danger: Color, val ok: Color, val warn: Color,
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
        dark = dark,
    )
}

val LocalPalette = staticCompositionLocalOf { paletteOf(dark = true) }

/** المظهر تلقائي يتبع الجهاز، والاتجاه من اليمين لليسار دائماً. */
@Composable
fun GrcsTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
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
    ) {
        MaterialTheme(colorScheme = scheme) {
            ProvideTextStyle(TextStyle(fontFamily = GrcsFonts.body), content)
        }
    }
}
