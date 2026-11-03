package dev.saged.grcs.ui.opening

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
fun OpeningScreen(onDone: () -> Unit) {
    val p = LocalPalette.current
    val reduce = rememberReduceMotion()
    val ease = CubicBezierEasing(.2f, .8f, .2f, 1f)
    val mark = remember { Animatable(if (reduce) 1f else 0f) }
    val t1 = remember { Animatable(if (reduce) 1f else 0f) }
    val t2 = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduce) coroutineScope {
            launch { mark.animateTo(1f, tween(600, easing = ease)) }
            launch { delay(150); t1.animateTo(1f, tween(600, easing = ease)) }
            launch { delay(420); t2.animateTo(1f, tween(600, easing = ease)) }
        }
        delay(if (reduce) 600 else 1300)
        onDone()
    }
    val bar = if (reduce) 0f else rememberInfiniteTransition(label = "bar").animateFloat(-1f, 2.86f, infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart), label = "x").value
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
            Box(Modifier.padding(top = 6.dp).width(160.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = .15f))) {
                Box(Modifier.fillMaxWidth(.35f).height(3.dp).graphicsLayer { translationX = bar * 160.dp.toPx() * .35f }.clip(RoundedCornerShape(2.dp)).background(p.tealL))
            }
        }
    }
}
