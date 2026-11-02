package dev.saged.grcs.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.saged.grcs.R

/** لالزار للعناوين، وIBM Plex العربي للنصوص. */
object GrcsFonts {
    val display = FontFamily(Font(R.font.lalezar))
    val body = FontFamily(
        Font(R.font.plex_arabic_regular, FontWeight.Normal),
        Font(R.font.plex_arabic_medium, FontWeight.Medium),
        Font(R.font.plex_arabic_semibold, FontWeight.SemiBold),
        Font(R.font.plex_arabic_bold, FontWeight.Bold),
    )
}

/** احترام "إزالة الحركات": مقياس مدة الحركة صفر. */
@Composable
fun rememberReduceMotion(): Boolean {
    val ctx = LocalContext.current
    return remember { reduceMotion(ctx) }
}

private fun reduceMotion(ctx: Context) =
    Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
