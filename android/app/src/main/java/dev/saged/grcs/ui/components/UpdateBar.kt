package dev.saged.grcs.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.data.UpState
import dev.saged.grcs.data.Updater
import dev.saged.grcs.ui.icons.Fa
import dev.saged.grcs.ui.theme.LocalPalette

/** شريط التحديث: متاح ← جارٍ التنزيل بنسبة ← جاهز للتثبيت. يبقى ظاهراً حتى التثبيت حتى لو مرّت أيام. */
@Composable
fun UpdateBar() {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val st by Updater.state.collectAsState()
    var hidden by remember { mutableStateOf<String?>(null) }
    val tag = when (val s = st) { is UpState.Available -> s.info.tag; is UpState.Downloading -> s.info.tag; is UpState.Ready -> s.info.tag; else -> "" }
    val show = st !is UpState.None && !(st is UpState.Available && hidden == tag)
    AnimatedVisibility(show, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Column(Modifier.fillMaxWidth().background(if (st is UpState.Ready) p.success else p.pri).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Fa.Download, null, Modifier.size(16.dp), tint = p.onPri)
                val msg = when (val s = st) {
                    is UpState.Available -> "إصدار جديد $tag متاح"
                    is UpState.Downloading -> "جارٍ تنزيل التحديث ${s.pct}%"
                    is UpState.Ready -> "اكتمل التنزيل — اضغط لتثبيت $tag"
                    else -> ""
                }
                Text(msg, Modifier.weight(1f), color = p.onPri, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                when (val s = st) {
                    is UpState.Available -> {
                        Text("لاحقاً", Modifier.heightIn(min = 44.dp).clickable { hidden = tag }.padding(horizontal = 10.dp, vertical = 12.dp), color = p.onPri.copy(alpha = .8f), fontSize = 13.sp)
                        Text("تنزيل", Modifier.heightIn(min = 44.dp).background(p.onPri.copy(alpha = .18f), RoundedCornerShape(10.dp)).clickable { Updater.start(ctx, s.info) }.padding(horizontal = 14.dp, vertical = 12.dp), color = p.onPri, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    is UpState.Ready -> Text("تثبيت", Modifier.heightIn(min = 44.dp).background(p.onPri.copy(alpha = .22f), RoundedCornerShape(10.dp)).clickable { Updater.install(ctx) }.padding(horizontal = 16.dp, vertical = 12.dp), color = p.onPri, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    else -> {}
                }
            }
            (st as? UpState.Downloading)?.let { Bar(it.pct / 100f, p.onPri, Modifier.padding(bottom = 4.dp), h = 4.dp) }
        }
    }
}

/** عند دخول التطبيق: نافذة «يوجد إصدار جديد» مرة لكل إصدار، وتبقى للتثبيت إن اكتمل التنزيل. */
@Composable
fun UpdatePrompt() {
    val ctx = LocalContext.current
    val st by Updater.state.collectAsState()
    val pr = remember { ctx.getSharedPreferences("ui", android.content.Context.MODE_PRIVATE) }
    var dismissed by remember { mutableStateOf<String?>(null) }
    val info = when (val s = st) { is UpState.Available -> s.info; is UpState.Ready -> s.info; else -> null } ?: return
    if (dismissed == info.tag || (st is UpState.Available && pr.getString("up_prompt", "") == info.tag)) return
    val ready = st is UpState.Ready
    ConfirmSheet(
        (if (info.notes.isNotEmpty()) info.notes else "إصدار أحدث من التطبيق جاهز.") + if (ready) "\n\nاكتمل التنزيل." else "",
        title = "إصدار جديد ${info.tag}", okText = if (ready) "تثبيت الآن" else "تنزيل الآن", okKind = BtnKind.Primary, icon = Fa.Download, cancelText = "لاحقاً",
        onCancel = { pr.edit().putString("up_prompt", info.tag).apply(); dismissed = info.tag },
        onOk = { pr.edit().putString("up_prompt", info.tag).apply(); dismissed = info.tag; if (ready) Updater.install(ctx) else Updater.start(ctx, info) },
    )
}
