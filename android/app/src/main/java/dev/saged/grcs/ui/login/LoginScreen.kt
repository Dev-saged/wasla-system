package dev.saged.grcs.ui.login

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.saged.grcs.auth.AuthException
import dev.saged.grcs.auth.AuthRepo
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.config.BrandConfig
import dev.saged.grcs.ui.icons.tatreezStar
import dev.saged.grcs.ui.theme.GrcsFonts
import dev.saged.grcs.ui.theme.LocalPalette
import dev.saged.grcs.ui.theme.rememberReduceMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(repo: AuthRepo, onDone: (AuthState) -> Unit) {
    val p = LocalPalette.current
    val reduce = rememberReduceMotion()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val enter = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduce) enter.animateTo(1f, tween(700)) }

    fun submit() {
        if (busy) return
        busy = true; error = null
        scope.launch {
            try {
                val st = withContext(Dispatchers.IO) { repo.login(user, pass) }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDone(st)
            } catch (e: AuthException) {
                error = e.message
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } finally {
                busy = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(p.bg)) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width * .5f, size.height * .1f)
            tatreezStar(c, size.width * .9f).let { drawPath(it, p.pri.copy(alpha = .07f)) }
            tatreezStar(c, size.width * .55f).let { drawPath(it, p.acc.copy(alpha = .08f)) }
        }
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
                .graphicsLayer { alpha = enter.value; translationY = (1f - enter.value) * 40f },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))
            Canvas(Modifier.size(64.dp)) { drawPath(tatreezStar(center, size.minDimension / 2f), p.acc) }
            Spacer(Modifier.height(20.dp))
            Text(
                BrandConfig.NAME_LINES.joinToString("\n"), color = p.ink, fontFamily = GrcsFonts.display,
                fontSize = 38.sp, lineHeight = 44.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(BrandConfig.TAGLINE, color = p.mute, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(40.dp))

            Field("اسم المستخدم", user, { user = it; error = null }, KeyboardType.Text, ImeAction.Next, false, null) {}
            Spacer(Modifier.height(12.dp))
            Field(
                "كلمة المرور", pass, { pass = it; error = null }, KeyboardType.Password, ImeAction.Go, !show,
                if (show) "إخفاء" else "إظهار", { show = !show },
            ) { submit() }

            Box(Modifier.fillMaxWidth().padding(top = 12.dp).height(22.dp)) {
                error?.let { Text(it, color = p.danger, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth().height(56.dp).background(if (busy) p.pri.copy(alpha = .6f) else p.pri, RoundedCornerShape(16.dp))
                    .clickable(enabled = !busy) { submit() },
                contentAlignment = Alignment.Center,
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(24.dp), color = p.onPri, strokeWidth = 2.5.dp)
                else Text("دخول", color = p.onPri, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(48.dp))
            Text(
                "تطوير ${BrandConfig.Dev.NAME}", color = p.mute, fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun Field(
    label: String, value: String, onChange: (String) -> Unit, type: KeyboardType, ime: ImeAction,
    hidden: Boolean, toggle: String?, onToggle: () -> Unit = {}, onGo: () -> Unit,
) {
    val p = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxWidth().height(60.dp).background(p.surface, RoundedCornerShape(16.dp))
            .border(if (focused) 2.dp else 1.dp, if (focused) p.pri else p.line, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth(if (toggle != null) .8f else 1f)) {
            if (value.isNotEmpty() || focused) Text(label, color = if (focused) p.pri else p.mute, fontSize = 11.sp)
            BasicTextField(
                value, onChange, singleLine = true,
                textStyle = TextStyle(color = p.ink, fontSize = 16.sp, fontFamily = GrcsFonts.body),
                cursorBrush = SolidColor(p.pri),
                visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = type, imeAction = ime, autoCorrectEnabled = false),
                keyboardActions = KeyboardActions(onGo = { onGo() }),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty() && !focused) Text(label, color = p.mute, fontSize = 16.sp)
                        inner()
                    }
                },
            )
        }
        if (toggle != null) {
            Text(
                toggle, color = p.pri, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterEnd).clickable { onToggle() }.padding(8.dp),
            )
        }
    }
}
