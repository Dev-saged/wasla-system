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
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.focus.onFocusChanged
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

    val W = androidx.compose.ui.graphics.Color.White
    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(p.navy, Color(0xFF0A1D38), Color(0xFF0C5A73)), start = Offset(0f, 0f), end = Offset(1200f, 2400f))), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp)
                .graphicsLayer { alpha = enter.value; translationY = (1f - enter.value) * 40f },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            Column(
                Modifier.widthIn(max = 380.dp).fillMaxWidth().background(W.copy(alpha = .06f), RoundedCornerShape(20.dp)).border(1.dp, W.copy(alpha = .12f), RoundedCornerShape(20.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(60.dp).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(p.tealL, Color(0xFF0C5A73))), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(dev.saged.grcs.ui.icons.QIcons.Shield, null, Modifier.size(30.dp), tint = W)
                }
                Spacer(Modifier.height(14.dp))
                Text("منظومة مخيمات النزوح", color = W, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text("تسجيل الدخول للمتابعة", color = Color(0xFFBFD8E8), fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                error?.let {
                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).background(Color(0x29DC2626), RoundedCornerShape(10.dp)).border(1.dp, Color(0x66DC2626), RoundedCornerShape(10.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(it, color = Color(0xFFFCA5A5), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Field("اسم المستخدم", user, { user = it; error = null }, KeyboardType.Text, ImeAction.Next, false, null, {}) {}
                Spacer(Modifier.height(14.dp))
                Field("كلمة المرور", pass, { pass = it; error = null }, KeyboardType.Password, ImeAction.Go, !show, if (show) "إخفاء" else "إظهار", { show = !show }) { submit() }
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.fillMaxWidth().height(52.dp).background(if (busy) p.tealL.copy(alpha = .6f) else p.tealL, RoundedCornerShape(12.dp)).clickable(enabled = !busy) { submit() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(24.dp), color = W, strokeWidth = 2.5.dp)
                    else Text("دخول", color = W, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(22.dp))
                Text("منظومة مخيمات النزوح — v${dev.saged.grcs.BuildConfig.VERSION_NAME}\nتطوير: المهندس ساجد العبادلة", color = Color(0xFF8FB4CC), fontSize = 11.5.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Field(
    label: String, value: String, onChange: (String) -> Unit, type: KeyboardType, ime: ImeAction,
    hidden: Boolean, toggle: String?, onToggle: () -> Unit, onGo: () -> Unit,
) {
    val p = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Color(0xFFDCEAF3), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(
            Modifier.fillMaxWidth().height(52.dp).background(Color.White.copy(alpha = .94f), RoundedCornerShape(10.dp))
                .border(1.5.dp, if (focused) p.tealL else Color.Transparent, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value, onChange, singleLine = true, modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                textStyle = TextStyle(color = Color(0xFF1E293B), fontSize = 16.sp, fontFamily = GrcsFonts.body),
                cursorBrush = SolidColor(p.tealL),
                visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = type, imeAction = ime, autoCorrectEnabled = false),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { onGo() }),
            )
            if (toggle != null) Text(toggle, color = Color(0xFF0F2B52), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.heightIn(min = 44.dp).clickable(onClick = onToggle).padding(start = 8.dp).wrapContentHeight(Alignment.CenterVertically))
        }
    }
}
