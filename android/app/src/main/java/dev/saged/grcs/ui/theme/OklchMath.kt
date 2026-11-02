package dev.saged.grcs.ui.theme

import dev.saged.grcs.config.BrandConfig
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/** OKLCH → sRGB خطي ثم مُرمَّز، مع قصّ ما يخرج عن النطاق. */
object OklchMath {
    private fun linear(o: BrandConfig.Oklch): DoubleArray {
        val r = Math.toRadians(o.h.toDouble())
        val a = o.c * cos(r)
        val b = o.c * sin(r)
        val l = o.l.toDouble()
        val l3 = (l + 0.3963377774 * a + 0.2158037573 * b).pow(3)
        val m3 = (l - 0.1055613458 * a - 0.0638541728 * b).pow(3)
        val s3 = (l - 0.0894841775 * a - 1.2914855480 * b).pow(3)
        return doubleArrayOf(
            4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3,
            -1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3,
            -0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3,
        ).map { it.coerceIn(0.0, 1.0) }.toDoubleArray()
    }

    fun srgb(o: BrandConfig.Oklch): FloatArray = linear(o).map {
        val v = if (it <= 0.0031308) 12.92 * it else 1.055 * it.pow(1 / 2.4) - 0.055
        v.coerceIn(0.0, 1.0).toFloat()
    }.toFloatArray()

    fun luminance(o: BrandConfig.Oklch): Double {
        val (r, g, b) = linear(o)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    fun contrast(a: BrandConfig.Oklch, b: BrandConfig.Oklch): Double {
        val x = luminance(a)
        val y = luminance(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }
}
