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
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.Messages
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
fun MessagesScreen(auth: AuthState, data: DataRepo) {
    val p = LocalPalette.current
    val ben = auth.user.role == "beneficiary"
    val camps = remember { data.campList(auth) }
    var thread by remember { mutableStateOf<String?>(if (ben) "ben" else null) }
    val repo = remember { Messages() }
    BackHandler(thread != null && !ben) { thread = null }
    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        val t = thread
        if (t == null) Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("الرسائل", color = p.ink, fontFamily = GrcsFonts.display, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            ThreadRow("رسائل عامة لكل المخيمات") { thread = "global" }
            camps.forEach { (id, n) -> ThreadRow(n) { thread = id }; ThreadRow("مستفيدو $n") { thread = "bl:$id" } }
        } else if (t.startsWith("bl:")) BenList(auth, repo, t.removePrefix("bl:"), camps.firstOrNull { it.first == t.removePrefix("bl:") }?.second.orEmpty(), { thread = null }) { tid -> thread = "bt:${t.removePrefix("bl:")}:$tid" }
        else Chat(auth, t, if (t == "global") "رسائل عامة" else if (ben) "الإدارة" else if (t.startsWith("bt:")) "محادثة مستفيد" else camps.firstOrNull { it.first == t }?.second ?: t) { if (!ben) thread = if (t.startsWith("bt:")) "bl:" + t.removePrefix("bt:").substringBefore(":") else null }
    }
}

@Composable
private fun BenList(auth: AuthState, repo: Messages, camp: String, campName: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    var rows by remember { mutableStateOf<List<Triple<String, String, String>>?>(null) }
    LaunchedEffect(camp) { rows = withContext(Dispatchers.IO) { runCatching { repo.benThreads(auth, camp) }.getOrDefault(emptyList()) } }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("رجوع", color = p.pri, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onBack))
        Text("مستفيدو $campName", color = p.ink, fontFamily = GrcsFonts.display, fontSize = 22.sp)
        if (rows == null) Text("جارٍ التحميل…", color = p.mute, fontSize = 13.sp) else if (rows!!.isEmpty()) Text("لا محادثات بعد", color = p.mute, fontSize = 13.sp)
        rows?.forEach { (tid, name, last) ->
            Column(Modifier.fillMaxWidth().heightIn(min = 60.dp).background(p.surface, RoundedCornerShape(16.dp)).clickable { onOpen(tid) }.padding(14.dp)) {
                Text(name, color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(last, color = p.mute, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ThreadRow(title: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxWidth().heightIn(min = 60.dp).background(p.surface, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(16.dp), contentAlignment = Alignment.CenterStart) {
        Text(title, color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Chat(auth: AuthState, thread: String, title: String, onBack: () -> Unit) {
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
    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) listState.scrollToItem(msgs.size - 1) }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!ben) Text("رجوع", color = p.pri, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onBack).padding(12.dp))
            Text(title, color = p.ink, fontFamily = GrcsFonts.display, fontSize = 20.sp, modifier = Modifier.padding(start = if (ben) 12.dp else 0.dp))
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp), state = listState, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (msgs.isEmpty()) item { Text("لا توجد رسائل بعد", color = p.mute, fontSize = 13.sp, modifier = Modifier.padding(24.dp)) }
            items(msgs, key = { it.getString("id") }) { m ->
                val out = m.optString("senderId") == auth.user.id
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (out) Arrangement.Start else Arrangement.End) {
                    Column(Modifier.fillMaxWidth(.82f).background(if (out) p.pri else p.surface, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                        if (!out) Text(m.optString("senderName"), color = p.mute, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(m.optString("text"), color = if (out) p.onPri else p.ink, fontSize = 15.sp)
                        Text(fmt.format(Date(m.optLong("timestamp"))), color = (if (out) p.onPri else p.mute).copy(alpha = .75f), fontSize = 10.sp)
                    }
                }
            }
        }
        if (failed) Text("تعذّر الإرسال — تحقق من الاتصال وأعد المحاولة", color = p.danger, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QField("اكتب رسالة", text, { text = it; failed = false }, Modifier.weight(1f))
            Box(
                Modifier.heightIn(min = 56.dp).background(p.pri, RoundedCornerShape(14.dp)).clickable {
                    val t = text.trim()
                    if (t.isNotEmpty()) scope.launch {
                        val ok = withContext(Dispatchers.IO) { repo.send(auth, thread, t) }
                        if (ok) { text = ""; rev++ } else failed = true
                    }
                }.padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) { Text("إرسال", color = p.onPri, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}
