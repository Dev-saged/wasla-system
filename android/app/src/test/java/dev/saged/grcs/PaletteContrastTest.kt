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

    /** الألوان مطابقة لنسخة الويب حرفياً؛ يُفحص ما تضمنه لوحة الويب نفسها. */
    @Test
    fun textPairsMeetAA() {
        check("ink/bg", p.ink, p.bg)
        check("ink/surface", p.ink, p.surface)
        check("head/surface", p.head, p.surface)
        check("mute/surface", p.mute, p.surface)
        check("danger/surface", p.danger, p.surface)
    }

    @Test
    fun openingTextReadable() {
        val o = BrandConfig.Opening
        assertTrue(OklchMath.contrast(o.text, o.skyTop) >= 4.5)
        assertTrue(OklchMath.contrast(o.text, o.ground) >= 4.5)
    }
}
