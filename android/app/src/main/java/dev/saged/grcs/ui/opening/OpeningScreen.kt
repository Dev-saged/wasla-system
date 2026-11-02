package dev.saged.grcs.ui.opening

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.R
import dev.saged.grcs.config.BrandConfig
import dev.saged.grcs.ui.icons.tatreezStar
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.OklchMath
import dev.saged.grcs.ui.theme.rememberReduceMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun c(o: BrandConfig.Oklch): Color {
    val (r, g, b) = OklchMath.srgb(o)
    return Color(r, g, b)
}

/** الافتتاح: شمس الفجر ترتفع خلف الخيمة، والعنوان يظهر، ثم ينتقل التطبيق. اللمس يتخطى. */
@Composable
fun OpeningScreen(onDone: () -> Unit) {
    val o = BrandConfig.Opening
    val reduce = rememberReduceMotion()
    val rise = remember { Animatable(if (reduce) 0f else 1f) }
    val appear = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { rise.animateTo(0f, tween(1900, easing = CubicBezierEasing(.2f, .8f, .2f, 1f))) }
            launch { appear.animateTo(1f, tween(1000)) }
        }
        delay(if (reduce) 500 else 800)
        onDone()
    }
    val text = c(o.text)
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(c(o.skyTop), c(o.skyMid), c(o.horizon))))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                Modifier.statusBarsPadding().padding(top = 56.dp).graphicsLayer {
                    alpha = appear.value
                    translationY = (1f - appear.value) * 24.dp.toPx()
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BrandConfig.NAME_LINES.forEachIndexed { i, line ->
                    Text(
                        line, color = text, fontFamily = GrcsFonts.display,
                        fontSize = if (i == 0) 40.sp else 52.sp, lineHeight = if (i == 0) 46.sp else 58.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Canvas(Modifier.width(120.dp).height(14.dp)) {
                    val step = size.width / 7
                    repeat(7) { i ->
                        drawPath(
                            tatreezStar(Offset(step * (i + .5f), size.height / 2), if (i % 2 == 0) 7.dp.toPx() else 5.dp.toPx()),
                            color = c(o.accent),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(BrandConfig.TAGLINE, color = text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().aspectRatio(1000f / 532f).clipToBounds()) {
                Image(
                    painterResource(R.drawable.opening_sun), null, Modifier.fillMaxSize()
                        .graphicsLayer { translationY = rise.value * size.height * .55f },
                    contentScale = ContentScale.FillBounds,
                )
                Image(painterResource(R.drawable.opening_scene), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            }
            Column(
                Modifier.fillMaxWidth().background(c(o.ground)).navigationBarsPadding().padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("تطوير: ${BrandConfig.Dev.TITLE}", color = text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(BrandConfig.Dev.PLACE, color = text, fontSize = 11.sp)
            }
        }
    }
}
