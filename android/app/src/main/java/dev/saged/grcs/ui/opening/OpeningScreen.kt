package dev.saged.grcs.ui.opening

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.config.BrandConfig
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.theme.LocalPalette
import dev.saged.grcs.ui.theme.rememberReduceMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** شاشة البداية: خلفية كحلية بتوهجات هادئة، شارة بهالة نابضة، العنوان، سطر المطوّر، وشريط تقدم حقيقي. اللمس يتخطى. */
@Composable
fun OpeningScreen(ready: Boolean = true, onDone: () -> Unit) {
    val p = LocalPalette.current
    val reduce = rememberReduceMotion()
    val ease = CubicBezierEasing(.2f, .8f, .2f, 1f)
    val mark = remember { Animatable(if (reduce) 1f else 0f) }
    val t1 = remember { Animatable(if (reduce) 1f else 0f) }
    val t2 = remember { Animatable(if (reduce) 1f else 0f) }
    val prog = remember { Animatable(if (reduce) 1f else 0f) }
    var minDone by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!reduce) coroutineScope {
            launch { mark.animateTo(1f, tween(700, easing = ease)) }
            launch { delay(200); t1.animateTo(1f, tween(700, easing = ease)) }
            launch { delay(500); t2.animateTo(1f, tween(700, easing = ease)) }
            launch { prog.animateTo(.92f, tween(1000, easing = CubicBezierEasing(.3f, .1f, .2f, 1f))) }
        }
        delay(if (reduce) 300 else 1000)
        minDone = true
    }
    LaunchedEffect(minDone, ready) { if (minDone && ready) { if (!reduce) prog.animateTo(1f, tween(220)); onDone() } }
    val inf = rememberInfiniteTransition(label = "amb")
    val pulse = if (reduce) 0f else inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "pulse").value
    val drift = if (reduce) .5f else inf.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse), label = "drift").value
    val shine = if (reduce) .5f else inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "shine").value
    val tealL = p.tealL
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.navyL, p.navy)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(tealL.copy(alpha = .20f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(size.width * (.2f + .25f * drift), size.height * .28f), radius = size.width * .8f), size.width * .8f, androidx.compose.ui.geometry.Offset(size.width * (.2f + .25f * drift), size.height * .28f))
            drawCircle(Brush.radialGradient(listOf(p.pri.copy(alpha = .22f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(size.width * (.9f - .25f * drift), size.height * .78f), radius = size.width * .9f), size.width * .9f, androidx.compose.ui.geometry.Offset(size.width * (.9f - .25f * drift), size.height * .78f))
        }
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    for (k in 0..1) {
                        val f = (pulse + k * .5f) % 1f
                        drawCircle(tealL.copy(alpha = (1f - f) * .30f * mark.value), 52.dp.toPx() + f * 38.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                    }
                }
                Box(
                    Modifier.graphicsLayer { alpha = mark.value; scaleX = .6f + .4f * mark.value; scaleY = .6f + .4f * mark.value }
                        .size(104.dp).shadow(18.dp, RoundedCornerShape(30.dp), ambientColor = tealL, spotColor = tealL).clip(RoundedCornerShape(30.dp)).background(Brush.linearGradient(listOf(p.tealL, p.pri))),
                    contentAlignment = Alignment.Center,
                ) { Icon(QIcons.Camp, null, Modifier.size(50.dp), tint = Color.White) }
            }
            Text("منظومة مخيمات النزوح", Modifier.padding(top = 14.dp).graphicsLayer { alpha = t1.value; translationY = (1f - t1.value) * 12.dp.toPx() },
                color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 36.sp)
            Box(Modifier.padding(top = 14.dp).graphicsLayer { alpha = t2.value; scaleX = t2.value }.width(34.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Brush.horizontalGradient(listOf(p.tealL, p.pri))))
            Text("تطوير", Modifier.padding(top = 12.dp).graphicsLayer { alpha = t2.value * .8f }, color = Color(0xFF7FA6C0), fontSize = 11.5.sp)
            Text(BrandConfig.Dev.NAME, Modifier.padding(top = 2.dp).graphicsLayer { alpha = t2.value; translationY = (1f - t2.value) * 8.dp.toPx() },
                color = Color(0xFFD6E6F2), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            androidx.compose.foundation.Canvas(Modifier.padding(top = 34.dp).width(220.dp).height(8.dp)) {
                val h = 5.dp.toPx(); val y = size.height / 2; val w = size.width
                drawRoundRect(Color.White.copy(alpha = .12f), androidx.compose.ui.geometry.Offset(0f, y - h / 2), androidx.compose.ui.geometry.Size(w, h), androidx.compose.ui.geometry.CornerRadius(h / 2))
                val fw = (w * prog.value).coerceAtLeast(h)
                // البداية من اليمين لأن الواجهة RTL
                val x0 = w - fw
                drawRoundRect(Brush.horizontalGradient(listOf(p.pri, tealL), startX = w, endX = x0), androidx.compose.ui.geometry.Offset(x0, y - h / 2), androidx.compose.ui.geometry.Size(fw, h), androidx.compose.ui.geometry.CornerRadius(h / 2))
                val sx = w - fw + fw * (1f - shine)
                drawRoundRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = .55f), Color.Transparent), startX = sx - 30.dp.toPx(), endX = sx + 30.dp.toPx()), androidx.compose.ui.geometry.Offset(x0, y - h / 2), androidx.compose.ui.geometry.Size(fw, h), androidx.compose.ui.geometry.CornerRadius(h / 2))
            }
            Text(if (ready && prog.value >= .99f) "جاهز" else "جارٍ تجهيز بياناتك", Modifier.padding(top = 10.dp).graphicsLayer { alpha = t2.value * .85f }, color = Color(0xFF7FA6C0), fontSize = 12.sp)
        }
    }
}
