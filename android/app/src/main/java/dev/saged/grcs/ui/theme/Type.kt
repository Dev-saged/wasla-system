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

/** Cairo للجميع كما في نسخة الويب؛ display هو نفس الخط بوزن أثقل للأرقام والعناوين. */
object GrcsFonts {
    val display = FontFamily(Font(R.font.cairo_extrabold, FontWeight.Normal), Font(R.font.cairo_extrabold, FontWeight.Bold))
    val body = FontFamily(
        Font(R.font.cairo_regular, FontWeight.Normal),
        Font(R.font.cairo_medium, FontWeight.Medium),
        Font(R.font.cairo_semibold, FontWeight.SemiBold),
        Font(R.font.cairo_bold, FontWeight.Bold),
        Font(R.font.cairo_extrabold, FontWeight.ExtraBold),
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
