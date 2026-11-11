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

/** شاشة البداية كما في الويب (screen-splash): خلفية كحلية دائرية، شارة، العنوان، سطر المطوّر وشريط تحميل. اللمس يتخطى. */
@Composable
fun OpeningScreen(ready: Boolean = true, onDone: () -> Unit) {
    val p = LocalPalette.current
    val reduce = rememberReduceMotion()
    val ease = CubicBezierEasing(.2f, .8f, .2f, 1f)
    val mark = remember { Animatable(if (reduce) 1f else 0f) }
    val t1 = remember { Animatable(if (reduce) 1f else 0f) }
    val t2 = remember { Animatable(if (reduce) 1f else 0f) }
    var minDone by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!reduce) coroutineScope {
            launch { mark.animateTo(1f, tween(600, easing = ease)) }
            launch { delay(150); t1.animateTo(1f, tween(600, easing = ease)) }
            launch { delay(420); t2.animateTo(1f, tween(600, easing = ease)) }
        }
        delay(if (reduce) 400 else 750)
        minDone = true
    }
    // تُغلق حالما تجهز الجلسة والبيانات (بحد أدنى قصير للحركة) بدل انتظار ثابت
    LaunchedEffect(minDone, ready) { if (minDone && ready) onDone() }
    val barT = rememberInfiniteTransition(label = "bar")
    val bar = if (reduce) .5f else barT.animateFloat(0f, 1f, infiniteRepeatable(tween(1150, easing = CubicBezierEasing(.45f, .05f, .25f, 1f)), RepeatMode.Restart), label = "x").value
    val glow = if (reduce) 1f else barT.animateFloat(.55f, 1f, infiniteRepeatable(tween(575, easing = LinearEasing), RepeatMode.Reverse), label = "g").value
    Box(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(p.navyL, p.navy), center = androidx.compose.ui.geometry.Offset(300f, 400f), radius = 1400f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(
                Modifier.graphicsLayer { alpha = mark.value; scaleX = .7f + .3f * mark.value; scaleY = .7f + .3f * mark.value; translationY = (1f - mark.value) * 10.dp.toPx() }
                    .size(96.dp).shadow(12.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(p.tealL, p.pri))),
                contentAlignment = Alignment.Center,
            ) { Icon(QIcons.Camp, null, Modifier.size(44.dp), tint = Color.White) }
            Text("منظومة مخيمات النزوح", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = t1.value; translationY = (1f - t1.value) * 8.dp.toPx() })
            Text("تطوير: ${BrandConfig.Dev.NAME}", color = Color(0xFF8FB4CC), fontSize = 12.5.sp,
                modifier = Modifier.graphicsLayer { alpha = t2.value; translationY = (1f - t2.value) * 8.dp.toPx() })
            // خط التحميل: مقطع مضيء يتمدد ثم ينكمش وهو يعبر المسار، بتوهج نابض
            val tealL = p.tealL
            androidx.compose.foundation.Canvas(Modifier.padding(top = 10.dp).width(200.dp).height(14.dp)) {
                val h = 4.dp.toPx(); val y = size.height / 2; val w = size.width
                drawLine(Color.White.copy(alpha = .14f), androidx.compose.ui.geometry.Offset(h / 2, y), androidx.compose.ui.geometry.Offset(w - h / 2, y), h, androidx.compose.ui.graphics.StrokeCap.Round)
                val len = w * (.18f + .34f * kotlin.math.sin(bar * Math.PI).toFloat())
                val head = -len + (w + len) * bar
                val a = (w - head).coerceIn(h / 2, w - h / 2); val b = (w - head - len).coerceIn(h / 2, w - h / 2)
                if (a - b > 1f) {
                    val br = Brush.horizontalGradient(listOf(tealL.copy(alpha = 0f), tealL, Color.White, tealL), startX = a, endX = b)
                    drawLine(Brush.horizontalGradient(listOf(tealL.copy(alpha = 0f), tealL.copy(alpha = .35f * glow)), startX = a, endX = b), androidx.compose.ui.geometry.Offset(a, y), androidx.compose.ui.geometry.Offset(b, y), h * 3.2f, androidx.compose.ui.graphics.StrokeCap.Round)
                    drawLine(br, androidx.compose.ui.geometry.Offset(a, y), androidx.compose.ui.geometry.Offset(b, y), h, androidx.compose.ui.graphics.StrokeCap.Round)
                    drawCircle(Color.White.copy(alpha = glow), h * .75f, androidx.compose.ui.geometry.Offset(b, y))
                }
            }
        }
    }
}
