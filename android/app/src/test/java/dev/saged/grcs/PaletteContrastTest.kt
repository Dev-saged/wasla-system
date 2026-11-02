package dev.saged.grcs

import dev.saged.grcs.config.BrandConfig
import dev.saged.grcs.ui.theme.OklchMath
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteContrastTest {
    private val p = BrandConfig.Palette

    private fun check(name: String, fg: BrandConfig.Tone, bg: BrandConfig.Tone) {
        val light = OklchMath.contrast(fg.light, bg.light)
        val dark = OklchMath.contrast(fg.dark, bg.dark)
        assertTrue("$name light $light", light >= 4.5)
        assertTrue("$name dark $dark", dark >= 4.5)
    }

    @Test
    fun textPairsMeetAA() {
        check("ink/bg", p.ink, p.bg)
        check("ink/surface", p.ink, p.surface)
        check("mute/bg", p.mute, p.bg)
        check("mute/surface", p.mute, p.surface)
        check("onPri/pri", p.onPri, p.pri)
        check("danger/surface", p.danger, p.surface)
        check("ok/surface", p.ok, p.surface)
        check("warn/surface", p.warn, p.surface)
    }
}
