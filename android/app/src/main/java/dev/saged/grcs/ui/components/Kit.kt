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

/** شارة الاتصال في الترويسة: نقطة ملوّنة ونص داخل حبّة شفافة. */
@Composable
fun ConnBadge(state: Conn) {
    val c = when (state) { Conn.On -> Color(0xFF22C55E); Conn.Sync -> Color(0xFFFBBF24); Conn.Off -> Color(0xFFEF4444) }
    val t = when (state) { Conn.On -> "متصل"; Conn.Sync -> "مزامنة"; Conn.Off -> "دون اتصال" }
    Row(
        Modifier.clip(CircleShape).background(Color.White.copy(alpha = .1f)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(7.dp).background(c, CircleShape))
        Text(t, color = Color.White, fontSize = 11.5.sp)
    }
}

/** ترويسة كحلية ثابتة: شعار متدرج، عنوانان، شارة اتصال، وأزرار اختيارية. */
@Composable
fun AppHeader(title: String, subtitle: String, conn: Conn? = ConnState.state, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().shadow(4.dp).background(p.navy).statusBarsPadding()
            .heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (onBack != null) HeaderBtn(QIcons.Back, "رجوع", onBack) else
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Brush.linearGradient(listOf(p.tealL, p.navyL)))
                    .border(1.dp, Color.White.copy(alpha = .25f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(QIcons.Camp, null, Modifier.size(18.dp), tint = Color.White) }
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotEmpty()) Text(subtitle, color = Color(0xFFBFD8E8), fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
        if (conn != null) ConnBadge(conn)
    }
}

@Composable
fun HeaderBtn(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .08f)).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(20.dp), tint = Color.White) }
}

/** بطاقة إحصاء: حافة ملوّنة في البداية، مربع أيقونة، رقم كبير يعدّ تصاعدياً، تسمية وسطر فرعي. */
@Composable
fun KpiCard(label: String, value: Int, color: Color, icon: ImageVector, modifier: Modifier = Modifier, sub: String? = null, index: Int = 0, onClick: (() -> Unit)? = null) {
    val p = LocalPalette.current
    val reduce = rememberReduceMotion()
    val n by animateIntAsState(value, tween(if (reduce) 0 else 800), label = "kpi")
    val enter = remember { androidx.compose.animation.core.Animatable(if (reduce) 1f else 0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!reduce) { kotlinx.coroutines.delay(index * 45L); enter.animateTo(1f, tween(420, easing = androidx.compose.animation.core.CubicBezierEasing(.2f, .8f, .2f, 1f))) }
    }
    val m = modifier.graphicsLayer { alpha = enter.value; translationY = (1f - enter.value) * 24f }.cardShadow().clip(CardShape).background(p.surface).let { if (onClick != null) it.clickable(onClick = onClick) else it }
    Row(m.height(IntrinsicSize.Min)) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(color))
        Column(Modifier.weight(1f).padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(fa(n), color = p.ink, fontFamily = GrcsFonts.display, fontSize = 26.sp, lineHeight = 30.sp)
                IconTile(icon, color)
            }
            Text(label, color = p.mute, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            if (sub != null) Text(sub, color = p.mute, fontSize = 11.sp)
        }
    }
}

/** رقم بالفاصلة ولاتيني. */
fun fa(n: Int): String = String.format(java.util.Locale.US, "%,d", n)

/** عنوان قسم: عنوان 16 وإجراء اختياري في الطرف. */
@Composable
fun SectionTitle(text: String, trailing: @Composable () -> Unit = {}) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        trailing()
    }
}

enum class BtnKind { Primary, Outline, Danger, Ghost, Navy }

/** زر 12 زاوية، ارتفاع 48، بأيقونة اختيارية. */
@Composable
fun Btn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: BtnKind = BtnKind.Primary, icon: ImageVector? = null, enabled: Boolean = true, small: Boolean = false) {
    val p = LocalPalette.current
    val (bg, fg, bd) = when (kind) {
        BtnKind.Primary -> Triple(p.pri, p.onPri, p.pri)
        BtnKind.Navy -> Triple(p.navy, Color.White, p.navy)
        BtnKind.Danger -> Triple(p.danger, Color.White, p.danger)
        BtnKind.Outline -> Triple(Color.Transparent, p.pri, p.pri)
        BtnKind.Ghost -> Triple(Color.Transparent, p.pri, Color.Transparent)
    }
    Row(
        modifier.heightIn(min = if (small) 44.dp else 48.dp).clip(BtnShape).background(if (enabled) bg else p.line)
            .border(1.5.dp, if (enabled) bd else p.line, BtnShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = if (small) 14.dp else 18.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp), tint = if (enabled) fg else p.mute); Spacer(Modifier.width(8.dp)) }
        Text(text, color = if (enabled) fg else p.mute, fontSize = if (small) 13.5.sp else 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

/** حبّة حالة صغيرة بخلفية شفافة من اللون. */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text, color = color, fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
        modifier = modifier.clip(CircleShape).background(color.copy(alpha = .14f)).padding(horizontal = 10.dp, vertical = 3.dp),
    )
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
fun SettingCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    QCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(18.dp), tint = p.pri)
            Text(title, color = p.ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        content()
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
        modifier.fillMaxWidth().heightIn(min = 48.dp).clip(BtnShape).background(p.surface).border(1.dp, p.line, BtnShape).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(QIcons.Search, null, Modifier.size(18.dp), tint = p.mute)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, color = p.mute, fontSize = 14.sp)
            androidx.compose.foundation.text.BasicTextField(
                value, onChange, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = p.ink, fontSize = 14.sp, fontFamily = GrcsFonts.body),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(p.pri), modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
