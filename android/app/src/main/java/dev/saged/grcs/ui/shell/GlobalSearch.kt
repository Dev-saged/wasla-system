package dev.saged.grcs.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.ui.components.EmptyState
import dev.saged.grcs.ui.icons.Fa
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import dev.saged.grcs.ui.theme.UiPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

private class GsAction(val label: String, val icon: ImageVector, val go: () -> Unit)

private fun key(s: String) = s.trim().replace(Regex("[أإآ]"), "ا").replace("ى", "ي").replace("ة", "ه").lowercase()

/** البحث الشامل من الترويسة (openGlobalSearch): أفراد ومخيمات وإجراءات حسب الصلاحية. */
@Composable
fun GlobalSearch(auth: AuthState, data: DataRepo, onClose: () -> Unit, goTab: (String) -> Unit, goCamp: (String) -> Unit, goPerson: (JSONObject) -> Unit) {
    val p = LocalPalette.current
    val role = auth.user.role; val admin = role == "admin"
    var q by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("all") }
    var people by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    val camps = remember { data.campList(auth) }
    val focus = remember { FocusRequester() }
    fun act(l: String, ic: ImageVector, go: () -> Unit) = GsAction(l, ic) { onClose(); go() }
    val actions = remember {
        buildList {
            if (!admin) {
                add(act("الأفراد", Fa.Users) { goTab("People") })
                add(act("إدخال سريع", Fa.Bolt) { ShellState.homeRoute = "rapid"; goTab("Home") })
                add(act("استيراد Excel", Fa.FileArrowUp) { goTab("import") })
                add(act("الأسماء المتشابهة", Fa.UserGroup) { ShellState.homeRoute = "similar"; goTab("Home") })
            }
            add(act("مركز الجودة", Fa.ShieldHalved) { if (admin) { ShellState.campsRoute = "quality"; goTab("Camps") } else { ShellState.homeRoute = "quality"; goTab("Home") } })
            add(act("المساعدات", Fa.BoxOpen) { if (admin) { ShellState.campsRoute = "aid"; goTab("Camps") } else { ShellState.homeRoute = "aid"; goTab("Home") } })
            if (role == "delegate") add(act("تصدير تقرير", Fa.FileExport) { goTab("Export") })
            if (admin) {
                add(act("المخيمات", Fa.Tents) { goTab("Camps") })
                add(act("الحسابات", Fa.UsersGear) { goTab("Accounts") })
                add(act("سجل نقل الأسر", Fa.RightLeft) { ShellState.moreRoute = "tlog"; goTab("Settings") })
                add(act("منشئ الاستعلامات", Fa.Filter) { ShellState.moreRoute = "query"; goTab("Settings") })
                add(act("أوزان مؤشر الهشاشة", Fa.Sliders) { ShellState.moreRoute = "vuln"; goTab("Settings") })
            }
            if (role != "dataentry") add(act("الرسائل", Fa.Comments) { goTab("Chat") })
            add(act("تشخيص المزامنة", Fa.Rotate) { ShellState.moreRoute = "diag"; goTab("Settings") })
            add(GsAction("تبديل الصوت عند النقر", Fa.VolumeHigh) { UiPrefs.updateClick(!UiPrefs.click); onClose() })
            add(GsAction("تبديل الاهتزاز", Fa.MobileScreen) { UiPrefs.updateHaptic(!UiPrefs.haptic); onClose() })
            add(act("الإعدادات", Fa.Gear) { goTab("Settings") })
        }
    }
    var m = mode; var term = q.trim()
    if (term.startsWith(">")) { m = "a"; term = term.drop(1).trim() } else if (term.startsWith("@") && admin) { m = "c"; term = term.drop(1).trim() }
    val qk = key(term)
    LaunchedEffect(term, m) {
        people = if (term.isEmpty() || (m != "all" && m != "p")) emptyList() else {
            delay(70)
            withContext(Dispatchers.IO) {
                val digits = term.filter(Char::isDigit)
                camps.flatMap { (id, _) -> runCatching { data.people.persons(id) }.getOrDefault(emptyList()) }
                    .filter { key(it.optString("fullName")).contains(qk) || (digits.isNotEmpty() && (it.optString("nationalId").contains(digits) || it.optString("phone").contains(digits))) }.take(25)
            }
        }
    }
    val acts = if (m == "all" || m == "a") actions.filter { term.isEmpty() || key(it.label).contains(qk) } else emptyList()
    val cs = if (admin && (m == "all" || m == "c") && !(term.isEmpty() && m == "all")) camps.filter { term.isEmpty() || key(it.second).contains(qk) }.take(8) else emptyList()
    Dialog(onClose, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        (androidx.compose.ui.platform.LocalView.current.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
        LaunchedEffect(Unit) { delay(60); runCatching { focus.requestFocus() } }
        Box(Modifier.fillMaxSize().background(Color(0x8C0A1423)).clickable(remember { MutableInteractionSource() }, null, onClick = onClose), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 680.dp).fillMaxWidth().heightIn(max = LocalConfiguration.current.screenHeightDp.dp * .86f).clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)).background(p.surface)
                    .clickable(remember { MutableInteractionSource() }, null) {}.statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            ) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.surface2).border(1.5.dp, p.line, RoundedCornerShape(14.dp)).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Fa.MagnifyingGlass, null, Modifier.padding(start = 6.dp).size(16.dp), tint = p.mute)
                    Box(Modifier.weight(1f).heightIn(min = 50.dp).padding(horizontal = 4.dp), contentAlignment = Alignment.CenterStart) {
                        if (q.isEmpty()) Text("اسم، هوية، هاتف، مخيم، أو أمر…  (>أمر  @مخيم)", color = p.mute, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        BasicTextField(q, { q = it }, singleLine = true, cursorBrush = SolidColor(p.pri), modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            textStyle = TextStyle(color = p.ink, fontSize = 16.sp, fontFamily = GrcsFonts.body, lineHeight = 1.6.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)))
                    }
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClose), contentAlignment = Alignment.Center) { Icon(Fa.Xmark, "إغلاق", Modifier.size(16.dp), tint = p.mute) }
                }
                Row(Modifier.padding(start = 2.dp, end = 2.dp, top = 8.dp, bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOfNotNull("all" to "الكل", "p" to "أفراد", if (admin) "c" to "مخيمات" else null, "a" to "إجراءات").forEach { (k, l) ->
                        val on = mode == k
                        Box(Modifier.heightIn(min = 32.dp).clip(CircleShape).background(if (on) p.pri else p.surface2).border(1.dp, if (on) p.pri else p.line, CircleShape).clickable { mode = k }.padding(horizontal = 12.dp, vertical = 5.dp), contentAlignment = Alignment.Center) {
                            Text(l, color = if (on) Color.White else p.mute, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Column(Modifier.padding(top = 8.dp, bottom = 6.dp).verticalScroll(rememberScrollState())) {
                    if (acts.isNotEmpty()) { Sec(if (term.isEmpty() && m == "all") "إجراءات سريعة" else "إجراءات"); acts.forEach { a -> Item(a.icon, a.label, "أمر", true, a.go) } }
                    if (cs.isNotEmpty()) { Sec("مخيمات"); cs.forEach { (id, n) -> Item(Fa.Tent, n, "", false) { onClose(); goCamp(id) } } }
                    if (people.isNotEmpty()) {
                        Sec("أفراد")
                        people.forEach { ps ->
                            val g = ps.optString("gender")
                            val sub = listOf(ps.optString("nationalId"), ps.optString("phone"), if (admin) camps.firstOrNull { it.first == ps.optString("campId") }?.second.orEmpty() else "").filter { it.isNotEmpty() }.joinToString(" · ")
                            Item(if (g == "female") Fa.Venus else if (g == "male") Fa.Mars else Fa.User, ps.optString("fullName"), sub, false) { onClose(); goPerson(ps) }
                        }
                    }
                    val n = acts.size + cs.size + people.size
                    if (n == 0) EmptyState(Fa.MagnifyingGlass, "لا نتائج لـ \"$term\"", "جرّب جزءاً من الاسم أو رقم الهوية أو الهاتف")
                    else if (term.isNotEmpty()) Text("$n نتيجة", color = p.mute, fontSize = 11.5.sp, modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun Sec(t: String) = Text(t, color = LocalPalette.current.mute, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 4.dp))

@Composable
private fun Item(icon: ImageVector, title: String, sub: String, isCmd: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(p.surface2), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(15.dp), tint = p.pri) }
        Text(title, color = p.ink, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (isCmd) Text(sub, color = p.mute, fontSize = 10.5.sp, modifier = Modifier.border(1.dp, p.line, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp))
        else if (sub.isNotEmpty()) Text(sub, color = p.mute, fontSize = 11.5.sp, maxLines = 1)
    }
}
