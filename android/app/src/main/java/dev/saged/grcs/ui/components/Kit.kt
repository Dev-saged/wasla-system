package dev.saged.grcs.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import dev.saged.grcs.ui.icons.Fa
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import dev.saged.grcs.ui.theme.rememberReduceMotion

val CardShape = RoundedCornerShape(16.dp)
val BtnShape = RoundedCornerShape(12.dp)

/** ظل البطاقة كما في الويب: خفيف في الفاتح، أعمق في الليلي. */
@Composable
fun Modifier.cardShadow(): Modifier {
    val p = LocalPalette.current
    return shadow(if (p.dark) 6.dp else 3.dp, CardShape, ambientColor = p.navy.copy(alpha = .18f), spotColor = p.navy.copy(alpha = .22f))
}

/** بطاقة بيضاء (سطح) بزوايا 16؛ قابلة للنقر اختيارياً. */
@Composable
fun QCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, pad: Dp = 14.dp, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    val base = modifier.fillMaxWidth().cardShadow().clip(CardShape).background(p.surface)
    val m = if (onClick != null) base.clickable(onClick = onClick) else base
    Column(m.padding(pad), content = content)
}

/** مربع أيقونة ملوّن (32dp افتراضياً، زاوية 9). */
@Composable
fun IconTile(icon: ImageVector, color: Color, size: Dp = 32.dp, solid: Boolean = true) {
    val p = LocalPalette.current
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * .28f)).background(if (solid) color else color.copy(alpha = .14f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(size * .56f), tint = if (solid) Color.White else color) }
}

enum class Conn { On, Sync, Off }

/** حالة الاتصال العامة تُحدَّثها المزامنة وتقرؤها كل ترويسة. */
object ConnState {
    var state by androidx.compose.runtime.mutableStateOf(Conn.On)
    var lastSync by androidx.compose.runtime.mutableStateOf(0L)
}

/** شارة الاتصال في الترويسة (conn-badge): نقطة 7 بهالة، ونص 11.5. */
@Composable
fun ConnBadge(state: Conn) {
    val c = when (state) { Conn.On -> Color(0xFF22C55E); Conn.Sync -> Color(0xFFFBBF24); Conn.Off -> Color(0xFFEF4444) }
    val t = when (state) { Conn.On -> "متصل"; Conn.Sync -> "مزامنة جارية..."; Conn.Off -> "غير متصل" }
    Row(
        Modifier.clip(CircleShape).background(Color.White.copy(alpha = .1f)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(13.dp), contentAlignment = Alignment.Center) {
            if (state != Conn.Sync) Box(Modifier.size(13.dp).background(c.copy(alpha = .25f), CircleShape))
            Box(Modifier.size(7.dp).background(c, CircleShape))
        }
        Text(t, color = Color.White, fontSize = 11.5.sp)
    }
}

/** ترويسة كحلية ثابتة (app-header): زر رجوع اختياري، شعار متدرج بأيقونة الشاشة، عنوانان، أزرار، وشارة اتصال. */
@Composable
fun AppHeader(title: String, subtitle: String, conn: Conn? = ConnState.state, onBack: (() -> Unit)? = null, mark: ImageVector = Fa.PeopleRoof, backIcon: ImageVector = Fa.ArrowRight, endBack: (() -> Unit)? = null, search: Boolean = false, actions: @Composable RowScope.() -> Unit = {}) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().shadow(4.dp).background(p.navy).statusBarsPadding()
            .heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (onBack != null) HeaderBtn(backIcon, "رجوع", onBack) else Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Brush.linearGradient(listOf(p.tealL, p.navyL)))
                .border(1.dp, Color.White.copy(alpha = .25f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(mark, null, Modifier.size(19.dp), tint = Color.White) }
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotEmpty()) Text(subtitle, color = Color(0xFFBFD8E8), fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (search) HeaderBtn(Fa.MagnifyingGlass, "بحث شامل") { dev.saged.grcs.ui.shell.ShellState.search = true }
        actions()
        if (conn != null) ConnBadge(conn)
        if (endBack != null) HeaderBtn(Fa.ArrowRight, "رجوع", endBack)
    }
}

/** زر أيقونة في الترويسة (icon-btn): 38 بزاوية 10. */
@Composable
fun HeaderBtn(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .08f)).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(17.dp), tint = Color.White) }
}

/** بطاقة إحصاء (stat-card): حافة 4 ملوّنة في البداية، مربع أيقونة أعلى، رقم 26 يعدّ تصاعدياً، تسمية، سطر فرعي، وحلقة نسبة اختيارية في طرف النهاية. */
@Composable
fun KpiCard(label: String, value: Int, color: Color, icon: ImageVector, modifier: Modifier = Modifier, sub: String? = null, index: Int = 0, onClick: (() -> Unit)? = null, text: String? = null, ring: Float? = null) {
    val p = LocalPalette.current
    val reduce = rememberReduceMotion()
    val n by animateIntAsState(value, tween(if (reduce) 0 else 800), label = "kpi")
    val enter = remember { androidx.compose.animation.core.Animatable(if (reduce) 1f else 0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!reduce) { kotlinx.coroutines.delay(minOf(index, 8) * 30L); enter.animateTo(1f, tween(500, easing = androidx.compose.animation.core.CubicBezierEasing(.22f, .8f, .2f, 1f))) }
    }
    val m = modifier.graphicsLayer { alpha = enter.value; translationY = (1f - enter.value) * 12.dp.toPx(); val sc = .98f + .02f * enter.value; scaleX = sc; scaleY = sc }
        .cardShadow().clip(CardShape).background(p.surface).let { if (onClick != null) it.clickable(onClick = onClick) else it }
    Row(m.height(IntrinsicSize.Min)) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(color))
        Box(Modifier.weight(1f)) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(color), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(16.dp), tint = Color.White) }
                Spacer(Modifier.height(8.dp))
                if (text != null) Text(text, color = p.head, fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp, lineHeight = 22.sp)
                else Text(fa(n), color = p.head, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 31.sp)
                Text(label, color = p.mute, fontSize = 12.5.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                if (sub != null) Text(sub, color = p.mute, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp))
            }
            if (ring != null && text == null) Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) { MiniRing(ring) }
        }
    }
}

/** حلقة نسبة صغيرة 34 بلا نص (sc-ring). */
@Composable
fun MiniRing(frac: Float, size: Dp = 34.dp) {
    val p = LocalPalette.current
    val reduce = rememberReduceMotion()
    val f by animateFloatAsState(frac.coerceIn(0f, 1f), tween(if (reduce) 0 else 1000), label = "mring")
    Canvas(Modifier.size(size)) {
        val w = this.size.minDimension * .12f
        val sz = Size(this.size.width - w, this.size.height - w)
        drawArc(p.track, 0f, 360f, false, Offset(w / 2, w / 2), sz, style = Stroke(w))
        if (f > 0f) drawArc(p.pri, -90f, 360f * f, false, Offset(w / 2, w / 2), sz, style = Stroke(w, cap = StrokeCap.Round))
    }
}

/** رقم بالفاصلة ولاتيني. */
fun fa(n: Int): String = String.format(java.util.Locale.US, "%,d", n)

/** عنوان قسم: عنوان 16 وإجراء اختياري في الطرف. */
@Composable
fun SectionTitle(text: String, trailing: @Composable () -> Unit = {}) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = p.head, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        trailing()
    }
}

enum class BtnKind { Primary, Outline, Danger, Ghost, Navy }

/** زر (btn): زاوية 12، ارتفاع 48 (38 للصغير)، الأساسي متدرج تيل بظل، والمحدد سطح بحد رفيع ونص كحلي. */
@Composable
fun Btn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: BtnKind = BtnKind.Primary, icon: ImageVector? = null, enabled: Boolean = true, small: Boolean = false) {
    val p = LocalPalette.current
    val fg = when (kind) { BtnKind.Primary, BtnKind.Navy, BtnKind.Danger -> Color.White; BtnKind.Outline -> p.head; BtnKind.Ghost -> if (p.dark) p.tealL else p.tealD }
    val base = when (kind) {
        BtnKind.Primary -> modifier.shadow(5.dp, BtnShape, ambientColor = p.pri, spotColor = p.pri).clip(BtnShape).background(Brush.linearGradient(listOf(p.tealL, p.tealD)))
        BtnKind.Navy -> modifier.shadow(5.dp, BtnShape, ambientColor = p.navy, spotColor = p.navy).clip(BtnShape).background(p.navy)
        BtnKind.Danger -> modifier.clip(BtnShape).background(p.danger)
        BtnKind.Outline -> modifier.clip(BtnShape).background(p.surface).border(1.5.dp, p.line, BtnShape)
        BtnKind.Ghost -> modifier.clip(BtnShape)
    }
    Row(
        base.graphicsLayer { alpha = if (enabled) 1f else .5f }.heightIn(min = if (small) 38.dp else if (kind == BtnKind.Ghost) 40.dp else 48.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = if (kind == BtnKind.Ghost) 10.dp else if (small) 14.dp else 20.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(if (small) 14.dp else 16.dp), tint = fg); Spacer(Modifier.width(8.dp)) }
        Text(text, color = fg, fontSize = if (small) 13.5.sp else 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

/** حبّة حالة (pill): 11.5 عريض، خلفية فاتحة من اللون، وأيقونة اختيارية. */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val p = LocalPalette.current
    Row(
        modifier.clip(CircleShape).background(color.copy(alpha = if (p.dark) .13f else .08f)).padding(horizontal = 11.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(11.dp), tint = color)
        Text(text, color = color, fontSize = 11.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold)
    }
}

/** تبويبات مقسّمة (Segmented) كما في شاشة الحسابات. */
@Composable
fun SegTabs(items: List<String>, sel: Int, onSel: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.surface2).border(1.dp, p.line, RoundedCornerShape(12.dp)).padding(3.dp)) {
        items.forEachIndexed { i, t ->
            val on = i == sel
            Box(
                Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (on) p.pri.copy(alpha = if (p.dark) .16f else 1f) else Color.Transparent)
                    .clickable(role = Role.Tab) { onSel(i) }.padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(t, color = if (on) (if (p.dark) p.tealL else p.onPri) else p.mute, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

/** حلقة تقدّم بنسبة 0..1 مع نص في المنتصف. */
@Composable
fun Ring(frac: Float, color: Color, size: Dp = 56.dp, label: String? = null) {
    val p = LocalPalette.current
    val reduce = rememberReduceMotion()
    val f by animateFloatAsState(frac.coerceIn(0f, 1f), tween(if (reduce) 0 else 900), label = "ring")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.minDimension * .12f
            val inset = w / 2
            val sz = Size(this.size.width - w, this.size.height - w)
            drawArc(p.line, 0f, 360f, false, Offset(inset, inset), sz, style = Stroke(w))
            drawArc(color, -90f, 360f * f, false, Offset(inset, inset), sz, style = Stroke(w, cap = StrokeCap.Round))
        }
        Text(label ?: "${(f * 100).toInt()}%", color = p.ink, fontSize = (size.value * .25f).sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** شريط تقدّم أفقي بمسار. */
@Composable
fun Bar(frac: Float, color: Color, modifier: Modifier = Modifier, h: Dp = 8.dp) {
    val p = LocalPalette.current
    val reduce = rememberReduceMotion()
    val f by animateFloatAsState(frac.coerceIn(0f, 1f), tween(if (reduce) 0 else 700), label = "bar")
    Box(modifier.fillMaxWidth().height(h).clip(CircleShape).background(p.line)) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(CircleShape).background(color))
    }
}

/** صف إعدادات: أيقونة، عنوان ووصف، وسهم. */
@Composable
fun MenuRow(icon: ImageVector, title: String, sub: String? = null, color: Color? = null, trailing: @Composable () -> Unit = { Icon(QIcons.Chev, null, Modifier.size(18.dp), tint = LocalPalette.current.mute) }, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, color ?: p.pri, 36.dp, solid = false)
        Column(Modifier.weight(1f)) {
            Text(title, color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (sub != null) Text(sub, color = p.mute, fontSize = 11.5.sp)
        }
        trailing()
    }
}

/** بطاقة إعدادات بعنوان وأيقونة، ومحتواها صفوف. */
@Composable
fun SettingCard(title: String, icon: ImageVector, iconTint: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    QCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, null, Modifier.size(14.dp), tint = iconTint ?: p.head)
            Text(title, color = p.head, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

/** ورقة سفلية (modal-sheet): غشاء داكن، سطح بزاوية 22 من الأعلى، ارتفاع أقصى 88%، مقبض، وعنوان مع زر إغلاق. */
@Composable
fun Sheet(onClose: () -> Unit, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    val reduce = rememberReduceMotion()
    val maxH = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * .88f
    androidx.compose.ui.window.Dialog(onClose, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window?.setDimAmount(0f)
        val enter = remember { androidx.compose.animation.core.Animatable(if (reduce) 0f else 1f) }
        androidx.compose.runtime.LaunchedEffect(Unit) { enter.animateTo(0f, tween(280, easing = androidx.compose.animation.core.CubicBezierEasing(.4f, 0f, .2f, 1f))) }
        Box(Modifier.fillMaxSize().background(Color(0x8C0A1423).copy(alpha = .55f * (1f - enter.value))).clickable(remember { MutableInteractionSource() }, null, onClick = onClose), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().heightIn(max = maxH).graphicsLayer { translationY = size.height * enter.value }
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)).background(p.surface).clickable(remember { MutableInteractionSource() }, null) {}
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp).size(44.dp, 5.dp).background(p.line, CircleShape))
                if (title != null) Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = p.head, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(p.surface2).clickable(role = Role.Button, onClick = onClose), contentAlignment = Alignment.Center) { Icon(Fa.Xmark, "إغلاق", Modifier.size(15.dp), tint = p.head) }
                }
                content()
            }
        }
    }
}

/** نافذة تأكيد (confirmDialog): أيقونة تحذير، عنوان، رسالة، وزرّا إلغاء وتأكيد. */
@Composable
fun ConfirmSheet(message: String, title: String = "تأكيد", okText: String = "تأكيد", okKind: BtnKind = BtnKind.Danger, icon: ImageVector = Fa.TriangleExclamation, cancelText: String = "إلغاء", onCancel: () -> Unit, onOk: () -> Unit) {
    val p = LocalPalette.current
    Sheet(onCancel) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(40.dp), tint = if (okKind == BtnKind.Danger) p.danger else p.pri)
            Text(title, color = p.head, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp, bottom = 8.dp))
            Text(message, color = p.ink, fontSize = 14.5.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn(cancelText, onCancel, Modifier.weight(1f), BtnKind.Outline)
                Btn(okText, onOk, Modifier.weight(1f), okKind)
            }
        }
    }
}

/** شريط التنبيه الأصفر أعلى الشاشة (offline-banner). */
@Composable
fun OfflineBanner(text: String) {
    val p = LocalPalette.current
    Text(text, color = p.warn, fontSize = 12.5.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (p.dark) p.warn.copy(alpha = .12f) else Color(0xFFFFFBEB)).padding(horizontal = 14.dp, vertical = 9.dp))
}

/** صف إعدادات (settings-row): تسمية 14 وقيمة رمادية 12.5 وخط سفلي اختياري. */
@Composable
fun SettingsRow(label: String, value: String, last: Boolean = false) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 11.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = p.ink, fontSize = 14.sp)
        Text(value, color = p.mute, fontSize = 12.5.sp)
    }
    if (!last) Hair()
}

/** نص توضيحي رمادي 13 (caption). */
@Composable
fun Caption(text: String, modifier: Modifier = Modifier) = Text(text, color = LocalPalette.current.mute, fontSize = 13.sp, modifier = modifier)

/** مفتاح تبديل (switch): 46×26 وكرة بيضاء 20. */
@Composable
fun QSwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    val x by androidx.compose.animation.core.animateDpAsState(if (on) 23.dp else 3.dp, tween(200), label = "sw")
    Box(Modifier.size(46.dp, 26.dp).clip(RoundedCornerShape(14.dp)).background(if (on) p.pri else p.line).clickable(role = Role.Switch) { onChange(!on) }) {
        Box(Modifier.padding(start = x, top = 3.dp).size(20.dp).shadow(1.dp, CircleShape).background(Color.White, CircleShape))
    }
}

/** صف مفتاح بعنوان ووصف صغير (fx-row) بخط سفلي متقطع. */
@Composable
fun SwitchRow(title: String, sub: String?, on: Boolean, last: Boolean = false, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, color = p.ink, fontSize = 16.sp)
            if (sub != null) Text(sub, color = p.mute, fontSize = 12.sp)
        }
        QSwitch(on, onChange)
    }
    if (!last) Canvas(Modifier.fillMaxWidth().height(1.dp)) { drawLine(p.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))) }
}

/** أزرار مقسّمة (seg-btns): ارتفاع 46، حد 1.5، والمحدد بحد تيل وخلفية خفيفة. */
@Composable
fun SegBtns(items: List<Pair<String, ImageVector?>>, sel: Int, modifier: Modifier = Modifier, onSel: (Int) -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(10.dp)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { i, (t, ic) ->
            val on = i == sel
            val fg = if (on) (if (p.dark) p.tealL else p.tealD) else p.mute
            Row(Modifier.weight(1f).heightIn(min = 46.dp).clip(shape).background(if (on) p.pri.copy(alpha = if (p.dark) .12f else .08f) else p.surface).border(1.5.dp, if (on) p.pri else p.line, shape).clickable(role = Role.Tab) { onSel(i) },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                if (ic != null) { Icon(ic, null, Modifier.size(14.dp), tint = fg); Spacer(Modifier.width(6.dp)) }
                Text(t, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

/** فاصل رفيع. */
@Composable
fun Hair() { Box(Modifier.fillMaxWidth().height(1.dp).background(LocalPalette.current.line)) }

/** حالة فارغة بأيقونة وعنوان ووصف. */
@Composable
fun EmptyState(icon: ImageVector, title: String, sub: String = "") {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconTile(icon, p.pri, 64.dp, solid = false)
        Spacer(Modifier.height(12.dp))
        Text(title, color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (sub.isNotEmpty()) Text(sub, color = p.mute, fontSize = 12.5.sp, textAlign = TextAlign.Center)
    }
}

/** حقل بحث بزاوية 12 وأيقونة. */
@Composable
fun SearchBox(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp)).background(p.surface).border(1.5.dp, p.line, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(QIcons.Search, null, Modifier.size(15.dp), tint = p.mute)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, color = p.mute, fontSize = 14.sp)
            androidx.compose.foundation.text.BasicTextField(
                value, onChange, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = p.ink, fontSize = 16.sp, fontFamily = GrcsFonts.body, lineHeight = 1.6.em, lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.None)),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(p.pri), modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** مظهر البطاقة كمعدِّل: ظل + زاوية 16 + سطح. */
@Composable
fun Modifier.qcard(): Modifier = cardShadow().clip(CardShape).background(LocalPalette.current.surface)

/** زر إجراء صغير بصيغة الشريحة القديمة (نص، أساسي؟، إجراء). */
@Composable
fun QAct(text: String, primary: Boolean, onClick: () -> Unit) = Btn(text, onClick, kind = if (primary) BtnKind.Primary else BtnKind.Outline, small = true)
