package dev.saged.grcs.ui.messages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.BtnKind
import dev.saged.grcs.ui.components.IconTile
import dev.saged.grcs.ui.components.QCard
import dev.saged.grcs.ui.components.cardShadow
import dev.saged.grcs.ui.icons.Fa
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.Messages
import dev.saged.grcs.ui.components.QChip
import dev.saged.grcs.ui.components.QField
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessagesScreen(auth: AuthState, data: DataRepo, start: String? = null, onExit: (() -> Unit)? = null) {
    val p = LocalPalette.current
    val ben = auth.user.role == "beneficiary"
    val camps = remember { data.campList(auth) }
    var thread by remember { mutableStateOf<String?>(if (ben) "ben" else start) }
    val back = { if ((start != null || ben) && onExit != null) onExit() else thread = null }
    val repo = remember { Messages() }
    var sums by remember { mutableStateOf<Map<String, Messages.ThreadSummary>>(emptyMap()) }
    var glob by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(thread) {
        if (thread == null && !ben) withContext(Dispatchers.IO) {
            val g = repo.globalMessages(auth)
            glob = g.maxByOrNull { it.optLong("timestamp") }?.optString("text")
            sums = camps.associate { (id, _) -> id to runCatching { repo.summary(auth, id, g) }.getOrElse { Messages.ThreadSummary(null, 0, 0, null) } }
        }
    }
    BackHandler(thread != null && (!ben || onExit != null)) { back() }
    Box(Modifier.fillMaxSize()) {
        val t = thread
        if (t == null) Column(Modifier.fillMaxSize()) {
          AppHeader("الرسائل", "", conn = null, mark = dev.saged.grcs.ui.icons.Fa.Comments, search = auth.user.role != "beneficiary")
          Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val none = "لا توجد رسائل بعد"
            if (auth.user.role == "delegate" && camps.size == 1) {
                val sm = sums[camps[0].first]
                ThreadRow("المركز الرئيسي (الإدارة العامة)", sm?.last ?: none, Fa.BuildingColumns, sm?.unread ?: 0) { thread = camps[0].first }
                ThreadRow("رسائل المستفيدين", sm?.lastBen ?: none, Fa.Users, sm?.benUnread ?: 0) { thread = "bl:${camps[0].first}" }
            } else ThreadRow("رسالة جماعية لكل المخيمات", glob ?: none, Fa.Bullhorn) { thread = "global" }
            if (auth.user.role == "admin") {
                var open by remember { mutableStateOf(false) }
                var txt by remember { mutableStateOf("") }
                var busy by remember { mutableStateOf(false) }
                var res by remember { mutableStateOf("") }
                val sc = rememberCoroutineScope()
                if (!open) ThreadRow("رسالة جماعية لجميع المستفيدين", none, Fa.UsersRectangle) { open = true; res = "" }
                else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QField("نص الرسالة", txt, { txt = it }, multiline = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Btn(if (busy) "جارٍ الإرسال…" else "إرسال للجميع", {
                            if (!busy && txt.isNotBlank()) { busy = true; sc.launch {
                                val n = withContext(Dispatchers.IO) { repo.broadcastAll(auth, camps.map { it.first }, txt.trim()) }
                                busy = false
                                if (n >= 0) { res = "أُرسلت إلى $n مستفيد"; txt = ""; open = false } else res = "تعذّر الإرسال لبعض المخيمات — تحقق من الاتصال"
                            } }
                        }, small = true, icon = QIcons.Broadcast)
                        Btn("إلغاء", { open = false }, kind = BtnKind.Outline, small = true)
                    }
                }
                if (res.isNotEmpty()) Text(res, color = p.mute, fontSize = 12.sp)
            }
            if (!(auth.user.role == "delegate" && camps.size == 1)) camps.forEach { (id, n) -> val sm = sums[id]; val benOnly = sm != null && sm.unread == 0 && sm.benUnread > 0; ThreadRow(n, if (benOnly) "${sm!!.benUnread} رسالة جديدة من المستفيدين" else sm?.last ?: none, Fa.Tent, (sm?.unread ?: 0) + (sm?.benUnread ?: 0)) { thread = if (benOnly) "bl:$id" else id } }
          }
        } else if (t.startsWith("bl:")) BenList(auth, repo, t.removePrefix("bl:"), camps.firstOrNull { it.first == t.removePrefix("bl:") }?.second.orEmpty(), { back() }) { tid -> thread = "bt:${t.removePrefix("bl:")}:$tid" }
        else Chat(auth, t, if (t == "global") "رسائل عامة" else if (ben) "الإدارة" else if (t.startsWith("bt:")) "محادثة مستفيد" else camps.firstOrNull { it.first == t }?.second ?: t, onBenList = if (!ben && t != "global" && !t.startsWith("bt:")) ({ thread = "bl:$t" }) else null) { if (!ben) { if (t.startsWith("bt:")) thread = "bl:" + t.removePrefix("bt:").substringBefore(":") else back() } else back() }
    }
}

@Composable
private fun BenList(auth: AuthState, repo: Messages, camp: String, campName: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    var rows by remember { mutableStateOf<List<Triple<String, String, String>>?>(null) }
    LaunchedEffect(camp) { rows = withContext(Dispatchers.IO) { runCatching { repo.benThreads(auth, camp) }.getOrDefault(emptyList()) } }
    Column(Modifier.fillMaxSize()) {
      AppHeader("رسائل مستفيدي $campName", "", conn = null, onBack = onBack)
      Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var bc by remember { mutableStateOf(false) }
        var bcText by remember { mutableStateOf("") }
        var bcBusy by remember { mutableStateOf(false) }
        var bcMsg by remember { mutableStateOf("") }
        val sc = rememberCoroutineScope()
        if (!bc) Btn("رسالة جماعية لكل مستفيدي هذا المخيم", { bc = true; bcMsg = "" }, Modifier.fillMaxWidth(), BtnKind.Primary, QIcons.Broadcast)
        else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            QField("نص الرسالة", bcText, { bcText = it }, multiline = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(if (bcBusy) "جارٍ الإرسال…" else "إرسال للجميع", {
                    if (!bcBusy && bcText.isNotBlank()) { bcBusy = true; sc.launch {
                        val n = withContext(Dispatchers.IO) { repo.broadcast(auth, camp, bcText.trim()) }
                        bcBusy = false
                        if (n >= 0) { bcMsg = "أُرسلت إلى $n مستفيد"; bcText = ""; bc = false } else bcMsg = "تعذّر الإرسال — تحقق من الاتصال"
                    } }
                }, small = true, icon = QIcons.Broadcast)
                Btn("إلغاء", { bc = false }, kind = BtnKind.Outline, small = true)
            }
        }
        if (bcMsg.isNotEmpty()) Text(bcMsg, color = p.mute, fontSize = 12.sp)
        if (rows == null) Text("جارٍ التحميل…", color = p.mute, fontSize = 13.sp) else if (rows!!.isEmpty()) dev.saged.grcs.ui.components.EmptyState(QIcons.Chat, "لا توجد رسائل من المستفيدين بعد", "ستظهر هنا أي رسالة يرسلها مستفيد لإدارة المخيم")
        rows?.forEach { (tid, name, last) ->
            val un = repo.unread[tid] ?: 0
            ThreadRow(name, last, Fa.User, un) { onOpen(tid) }
        }
      }
    }
}

@Composable
private fun ThreadRow(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, unread: Int = 0, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    Row(Modifier.fillMaxWidth().cardShadow().clip(shape).background(p.surface).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(p.navyL, p.navy))), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(19.dp), tint = Color.White) }
        Column(Modifier.weight(1f)) {
            Text(title, color = p.ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if (sub.isNotEmpty()) Text(sub, color = p.mute, fontSize = 12.5.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        if (unread > 0) Box(Modifier.heightIn(min = 18.dp).widthIn(min = 18.dp).background(p.danger, CircleShape).padding(horizontal = 5.dp), contentAlignment = Alignment.Center) { Text("$unread", color = Color.White, fontSize = 10.5.sp, lineHeight = 18.sp) }
    }
}

@Composable
private fun Chat(auth: AuthState, thread: String, title: String, hasBack: Boolean = true, onBenList: (() -> Unit)? = null, onBack: () -> Unit) {
    val p = LocalPalette.current
    val repo = remember { Messages() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var msgs by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    var rev by remember { mutableStateOf(0) }
    val ben = auth.user.role == "beneficiary"
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.US) }

    LaunchedEffect(thread, rev) {
        while (true) {
            runCatching { withContext(Dispatchers.IO) { if (ben) repo.loadBen(auth) else repo.load(auth, thread) } }.getOrNull()?.let {
                if (it.size != msgs.size || it.lastOrNull()?.optString("id") != msgs.lastOrNull()?.optString("id")) msgs = it
            }
            delay(15_000)
        }
    }
    LaunchedEffect(msgs.size) {
        if (msgs.isNotEmpty()) listState.scrollToItem(msgs.size - 1)
        val key = if (thread.startsWith("bt:")) thread.substringAfterLast(":") else thread
        msgs.maxOfOrNull { it.optLong("timestamp") }?.let { Messages.Seen.mark(auth.user.id, key, it) }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        AppHeader(title, "", conn = null, onBack = if (ben && !hasBack) null else onBack) { if (onBenList != null) dev.saged.grcs.ui.components.HeaderBtn(QIcons.People, "رسائل المستفيدين", onBenList) }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp), state = listState, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (msgs.isEmpty()) item { dev.saged.grcs.ui.components.EmptyState(QIcons.Chat, "لا توجد رسائل بعد", "ابدأ المحادثة الآن") }
            items(msgs, key = { it.getString("id") }) { m ->
                val out = m.optString("senderId") == auth.user.id
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (out) Arrangement.Start else Arrangement.End) {
                    Column(Modifier.fillMaxWidth(.82f).then(if (out) Modifier else Modifier.cardShadow()).background(if (out) p.pri else p.surface, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (out) 4.dp else 16.dp, bottomEnd = if (out) 16.dp else 4.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                        if (!out) Text(m.optString("senderName"), color = p.mute, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(m.optString("text"), color = if (out) p.onPri else p.ink, fontSize = 15.sp)
                        Text(fmt.format(Date(m.optLong("timestamp"))) + if (out) " · مُستلمة" else "", color = (if (out) p.onPri else p.mute).copy(alpha = .75f), fontSize = 10.sp)
                    }
                }
            }
        }
        val queued = Messages.Outbox.count(auth.user.id).also { rev.hashCode() }
        if (queued > 0) Text("$queued رسالة بانتظار الاتصال — ستُرسل تلقائياً", color = p.mute, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QField("اكتب رسالة", text, { text = it; failed = false }, Modifier.weight(1f))
            Btn("إرسال", {
                    val t = text.trim()
                    if (t.isNotEmpty()) scope.launch {
                        val ok = withContext(Dispatchers.IO) { repo.send(auth, thread, t) }
                        if (ok) { text = ""; rev++ } else failed = true
                    }
            }, icon = QIcons.Broadcast)
        }
    }
}
