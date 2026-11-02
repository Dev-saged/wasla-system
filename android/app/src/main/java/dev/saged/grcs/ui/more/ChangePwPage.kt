package dev.saged.grcs.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthRepo
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.auth.SessionStore
import dev.saged.grcs.ui.components.AppHeader
import dev.saged.grcs.ui.components.Btn
import dev.saged.grcs.ui.components.QField
import dev.saged.grcs.ui.icons.QIcons
import dev.saged.grcs.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChangePwPage(auth: AuthState, onBack: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var conf by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        AppHeader("تغيير كلمة المرور", "", conn = null, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            QField("كلمة المرور الحالية", old, { old = it }, type = KeyboardType.Password)
            QField("كلمة المرور الجديدة (6 أحرف فأكثر)", new, { new = it }, type = KeyboardType.Password)
            QField("تأكيد كلمة المرور الجديدة", conf, { conf = it }, type = KeyboardType.Password)
            msg?.let { Text(it, color = if (ok) p.success else p.danger, fontSize = 13.sp) }
            Btn(if (busy) "جارٍ الحفظ…" else "حفظ", {
                if (busy) return@Btn
                if (new != conf) { msg = "التأكيد غير مطابق"; ok = false; return@Btn }
                busy = true
                scope.launch {
                    val r = withContext(Dispatchers.IO) { AuthRepo(SessionStore(ctx.applicationContext)).changePassword(auth, old, new) }
                    ok = r == null; msg = r ?: "تم تغيير كلمة المرور"; busy = false
                    if (ok) { old = ""; new = ""; conf = "" }
                }
            }, Modifier.fillMaxWidth(), icon = QIcons.Key)
        }
    }
}
