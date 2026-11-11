package dev.saged.grcs.ui.shell

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.ui.ben.BenHome
import dev.saged.grcs.ui.camps.CampsScreen
import dev.saged.grcs.ui.home.HomeScreen
import dev.saged.grcs.ui.icons.Fa
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.messages.MessagesScreen
import dev.saged.grcs.ui.more.ExportPage
import dev.saged.grcs.ui.more.MoreScreen
import dev.saged.grcs.ui.more.StaffPage
import androidx.compose.foundation.layout.padding
import dev.saged.grcs.ui.people.PeopleScreen
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.LaunchedEffect
import dev.saged.grcs.ui.theme.LocalPalette
import dev.saged.grcs.ui.theme.rememberReduceMotion

/** عدّاد الشاشات التي تخفي الشريط السفلي وزر الإضافة (نموذج الفرد والمحادثة) كما في الويب. */
object ShellState {
    var immersive by androidx.compose.runtime.mutableIntStateOf(0)
    var search by mutableStateOf(false)
    var homeRoute by mutableStateOf<String?>(null)
    var moreRoute by mutableStateOf<String?>(null)
    var campsRoute by mutableStateOf<String?>(null)
    /** انتقال مباشر لفرد: المخيم، مفتاح الأسرة، معرّف الفرد. */
    var jump by mutableStateOf<Triple<String, String, String>?>(null)
}

/** يخفي الشريط السفلي ما دامت الشاشة معروضة. */
@Composable
fun Immersive() { androidx.compose.runtime.DisposableEffect(Unit) { ShellState.immersive++; onDispose { ShellState.immersive-- } } }

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("الرئيسية", Fa.House), Camps("المخيمات", Fa.Tents), People("الأفراد", Fa.Users),
    Chat("الرسائل", Fa.Comments), Accounts("الحسابات", Fa.UsersGear), Export("تصدير", Fa.ChartSimple),
    Settings("الإعدادات", Fa.Gear),
}

/** مجموعات التبويب السفلي حسب الدور كما في الويب (NAV_SETS). */
private fun tabsFor(role: String) = when (role) {
    "admin" -> listOf(Tab.Home, Tab.Camps, Tab.Chat, Tab.Accounts, Tab.Settings)
    "delegate" -> listOf(Tab.Home, Tab.People, Tab.Chat, Tab.Export, Tab.Settings)
    "dataentry" -> listOf(Tab.Home, Tab.People, Tab.Settings)
    else -> listOf(Tab.Home, Tab.Chat, Tab.Settings)
}

private val Silk = CubicBezierEasing(.2f, .8f, .2f, 1f)

/** القشرة المعتمدة: محتوى الدور وشريط سفلي بتبويبات الدور ومؤشر ينزلق. */
@Composable
fun AppShell(auth: AuthState, data: DataRepo, onLogout: () -> Unit) {
    val p = LocalPalette.current
    val reduce = rememberReduceMotion()
    val tabs = remember(auth.user.role) { tabsFor(auth.user.role) }
    var sel by rememberSaveable { mutableStateOf(0) }
    var openCamp by remember { mutableStateOf<String?>(null) }
    var homeFilter by remember { mutableStateOf<String?>(null) }
    dev.saged.grcs.ui.theme.UiPrefs.init(androidx.compose.ui.platform.LocalContext.current)
    var unread by remember { mutableStateOf(0) }
    LaunchedEffect(sel) {
        while (true) {
            unread = withContext(Dispatchers.IO) { dev.saged.grcs.data.Messages().unreadTotal(auth, data.campList(auth).map { it.first }) }
            kotlinx.coroutines.delay(180_000)
        }
    }
    var openFam by remember { mutableStateOf<String?>(null) }
    var newTrig by remember { mutableStateOf(0) }
    var importing by remember { mutableStateOf(false) }
    val tab = tabs[sel.coerceIn(0, tabs.lastIndex)]
    val ben = auth.user.role == "beneficiary"
    BackHandler(importing) { importing = false }
    BackHandler(sel != 0 && openCamp == null && !importing) { sel = 0 }
    Column(Modifier.fillMaxSize().background(p.bg)) {
        Box(Modifier.weight(1f)) {
        if (importing) { dev.saged.grcs.ui.more.ImportPage(auth, data) { importing = false }; return@Box }
        AnimatedContent(
            tab, Modifier.fillMaxSize(),
            transitionSpec = {
                if (reduce) EnterTransition.None togetherWith ExitTransition.None else {
                    val dir = if (tabs.indexOf(targetState) > tabs.indexOf(initialState)) -1 else 1
                    (slideInHorizontally(tween(420, easing = Silk)) { it * dir / 4 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(300, easing = Silk)) { -it * dir / 4 } + fadeOut(tween(200)))
                }
            },
            label = "dest",
        ) { t ->
            when (t) {
                Tab.Home -> if (ben) BenHome(auth, data, onLogout) else if (homeFilter != null) PeopleScreen(auth, data, startFilter = homeFilter, newTrigger = newTrig) { homeFilter = null } else HomeScreen(auth, data, onLogout, { homeFilter = it }, { sel = tabs.indexOf(Tab.Camps).coerceAtLeast(0) })
                Tab.Camps -> if (openCamp != null) PeopleScreen(auth, data, openCamp, startFamily = openFam, newTrigger = newTrig) { openCamp = null; openFam = null } else CampsScreen(auth, data, { openCamp = it }) { c, f -> openFam = f; openCamp = c }
                Tab.People -> PeopleScreen(auth, data, newTrigger = newTrig)
                Tab.Chat -> MessagesScreen(auth, data)
                Tab.Accounts -> StaffPage(auth, data) { sel = 0 }
                Tab.Export -> ExportPage(auth, data) { sel = 0 }
                Tab.Settings -> MoreScreen(auth, data, onLogout)
            }
        }
        val field = auth.user.role == "delegate" || auth.user.role == "dataentry" || (auth.user.role == "admin" && openCamp != null)
        if (field && !importing && ShellState.immersive == 0 && (tab == Tab.Home || tab == Tab.People || (tab == Tab.Camps && openCamp != null))) Column(Modifier.align(Alignment.BottomEnd).padding(start = 16.dp, end = 16.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(44.dp).shadow(4.dp, CircleShape).background(p.surface, CircleShape).border(1.5.dp, p.line, CircleShape).clickable { importing = true }, contentAlignment = Alignment.Center) { Icon(Fa.FileArrowUp, "استيراد Excel", tint = p.head, modifier = Modifier.size(16.dp)) }
            Box(Modifier.size(56.dp).shadow(8.dp, CircleShape).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(p.tealL, p.tealD)), CircleShape).clickable { if (tab == Tab.Home) { homeFilter = null; sel = tabs.indexOf(Tab.People).coerceAtLeast(0) }; newTrig++ }, contentAlignment = Alignment.Center) { Icon(Fa.Plus, "إضافة فرد", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(22.dp)) }
        }
        }
        if (ShellState.search && !ben) GlobalSearch(auth, data, { ShellState.search = false },
            goTab = { t -> importing = t == "import"; if (t != "import") { openCamp = null; homeFilter = null; sel = tabs.indexOfFirst { it.name == t }.coerceAtLeast(0) } },
            goCamp = { id -> openCamp = null; ShellState.campsRoute = "detail:$id"; sel = tabs.indexOf(Tab.Camps).coerceAtLeast(0) },
            goPerson = { ps ->
                val camp = ps.optString("campId"); homeFilter = null; importing = false
                ShellState.jump = Triple(camp, ps.optString("familyId").ifEmpty { "i_" + ps.optString("id") }, ps.optString("id"))
                if (auth.user.role == "admin") { openCamp = camp; sel = tabs.indexOf(Tab.Camps).coerceAtLeast(0) } else sel = tabs.indexOf(Tab.People).coerceAtLeast(0)
            })
        if (!ben && ShellState.immersive == 0) NavBar(tabs, sel, if (tabs[sel] == Tab.Chat) 0 else unread) { sel = it; openCamp = null; homeFilter = null; importing = false }
    }
}

@Composable
private fun NavBar(tabs: List<Tab>, sel: Int, unread: Int, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    val haptic = LocalHapticFeedback.current
    Column(Modifier.fillMaxWidth().shadow(8.dp).background(p.surface).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
        BoxWithConstraints(Modifier.fillMaxWidth().height(64.dp)) {
            val w = maxWidth / tabs.size
            val x by animateDpAsState(w * sel, tween(450, easing = Silk), label = "ind")
            Box(
                Modifier.offset(x = x + (w - 40.dp) / 2).width(40.dp).height(3.dp)
                    .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)).background(p.pri),
            )
            Row(Modifier.fillMaxSize()) {
                tabs.forEachIndexed { i, d ->
                    val on = i == sel
                    val tint by animateColorAsState(if (on) p.head else p.mute, tween(200), label = "tint")
                    val ic by animateColorAsState(if (on) p.head else p.mute, tween(200), label = "ic")
                    val lift by animateDpAsState(if (on) (-2).dp else 0.dp, tween(300, easing = Silk), label = "lift")
                    Column(
                        Modifier.weight(1f).fillMaxSize().clickable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab,
                        ) {
                            if (!on && dev.saged.grcs.ui.theme.UiPrefs.haptic) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(i)
                        },
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box {
                            Icon(d.icon, null, Modifier.offset(y = lift).size(if (d.icon.viewportWidth > 512f) 24.dp else 19.dp), tint = ic)
                            if (d == Tab.Chat && unread > 0) Text(if (unread > 99) "99+" else "$unread", color = androidx.compose.ui.graphics.Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp).clip(RoundedCornerShape(8.dp)).background(p.danger).padding(horizontal = 4.dp))
                        }
                        Text(d.label, color = tint, fontSize = 10.5.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }
    }
}
