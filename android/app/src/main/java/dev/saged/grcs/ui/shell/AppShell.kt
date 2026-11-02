package dev.saged.grcs.ui.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.ui.home.HomeScreen
import dev.saged.grcs.ui.messages.MessagesScreen
import dev.saged.grcs.ui.people.PeopleScreen
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.theme.LocalPalette
import dev.saged.grcs.ui.theme.rememberReduceMotion

private enum class Dest(val label: String, val icon: ImageVector, val note: String) {
    Home("الرئيسية", QIcons.Home, "لوحة الدور: تُبنى في المرحلة ٦"),
    Camps("المخيمات", QIcons.Camp, "المؤسسات والمخيمات: المرحلة ٥"),
    People("الأفراد", QIcons.People, "سجل الأسر والأفراد: المرحلة ٥"),
    Chat("الرسائل", QIcons.Chat, "رسائل الإدارة والمندوبين: المرحلة ٥"),
    More("المزيد", QIcons.More, "الصلاحيات والنسخ الاحتياطي والتحديث: المراحل ٧ إلى ٩"),
}

private val Silk = CubicBezierEasing(.2f, .8f, .2f, 1f)

/** القشرة المعتمدة: شاشة واحدة لكل وظيفة، وشريط سفلي بخمسة عناصر ومؤشر ينزلق. */
@Composable
fun AppShell(auth: AuthState, data: DataRepo, onLogout: () -> Unit) {
    val p = LocalPalette.current
    val reduce = rememberReduceMotion()
    var sel by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(p.bg)) {
        AnimatedContent(
            sel, Modifier.weight(1f),
            transitionSpec = {
                if (reduce) EnterTransition.None togetherWith ExitTransition.None else {
                    val dir = if (targetState > initialState) -1 else 1
                    (slideInHorizontally(tween(420, easing = Silk)) { it * dir / 4 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(300, easing = Silk)) { -it * dir / 4 } + fadeOut(tween(200)))
                }
            },
            label = "dest",
        ) { i -> when (i) { 0 -> HomeScreen(auth, data, onLogout); 2 -> PeopleScreen(auth, data); 3 -> MessagesScreen(auth, data); else -> Placeholder(Dest.entries[i]) } }
        NavBar(sel) { sel = it }
    }
}

@Composable
private fun Placeholder(d: Dest) {
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 32.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(p.surface2), contentAlignment = Alignment.Center) {
            Icon(d.icon, null, Modifier.size(34.dp), tint = p.pri)
        }
        Spacer(Modifier.height(14.dp))
        Text(d.label, color = p.ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(d.note, color = p.mute, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun NavBar(sel: Int, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    val haptic = LocalHapticFeedback.current
    Column(Modifier.fillMaxWidth().background(p.surface).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
        BoxWithConstraints(Modifier.fillMaxWidth().height(58.dp)) {
            val w = maxWidth / Dest.entries.size
            val x by animateDpAsState(w * sel, tween(450, easing = Silk), label = "ind")
            Box(
                Modifier.offset(x = x + (w - 36.dp) / 2).width(36.dp).height(3.dp)
                    .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)).background(p.pri),
            )
            Row(Modifier.fillMaxSize()) {
                Dest.entries.forEachIndexed { i, d ->
                    val on = i == sel
                    val tint by animateColorAsState(if (on) p.pri else p.mute, tween(250), label = "tint")
                    Column(
                        Modifier.weight(1f).fillMaxSize().clickable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab,
                        ) {
                            if (!on) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(i)
                        },
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(d.icon, null, Modifier.size(22.dp), tint = tint)
                        Text(d.label, color = tint, fontSize = 11.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
