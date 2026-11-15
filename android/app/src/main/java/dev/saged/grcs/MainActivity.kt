package dev.saged.grcs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.saged.grcs.auth.AuthRepo
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.auth.SessionStore
import dev.saged.grcs.data.BackgroundJobs
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.Updater
import kotlinx.coroutines.launch
import dev.saged.grcs.ui.login.LoginScreen
import dev.saged.grcs.ui.opening.OpeningScreen
import dev.saged.grcs.ui.shell.AppShell
import dev.saged.grcs.ui.theme.GrcsTheme

class MainActivity : ComponentActivity() {
    private val notifPerm = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    private fun askNotifications() {
        if (android.os.Build.VERSION.SDK_INT < 33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val prefs = getSharedPreferences("ui", MODE_PRIVATE)
        if (prefs.getBoolean("asked_notif", false)) return
        prefs.edit().putBoolean("asked_notif", true).apply()
        notifPerm.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun handle(i: android.content.Intent?) {
        if (i?.getBooleanExtra(Updater.EXTRA_INSTALL, false) != true) return
        i.removeExtra(Updater.EXTRA_INSTALL)
        Updater.scope.launch { Updater.restore(applicationContext); Updater.install(this@MainActivity) }
    }

    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); handle(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        dev.saged.grcs.ui.theme.ThemePref.init(applicationContext)
        askNotifications()
        handle(intent)
        setContent {
            GrcsTheme {
                val repo = remember { AuthRepo(SessionStore(applicationContext)) }
                val data = remember { DataRepo(applicationContext, repo) }
                var opening by rememberSaveable { mutableStateOf(true) }
                var auth by remember { mutableStateOf<AuthState?>(null) }
                var restored by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    val local = withContext(Dispatchers.IO) { repo.restoreLocal() }
                    auth = local; restored = true
                    if (local != null) {
                        launch(Dispatchers.IO) { data.warm(local) }
                        val live = withContext(Dispatchers.IO) { repo.restore() }
                        if (live != null) { if (!live.offline) auth = live } else if (!repo.hasSession()) auth = null
                    }
                }
                LaunchedEffect(auth != null) {
                    if (auth != null) withContext(Dispatchers.IO) {
                        BackgroundJobs.schedule(applicationContext)
                        Updater.restore(applicationContext)
                        Updater.autoCheck(applicationContext, download = false)
                    }
                }
                AnimatedContent(
                    if (opening || !restored) 0 else if (auth == null) 1 else 2,
                    transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(200)) },
                    label = "root",
                ) {
                    when (it) {
                        0 -> OpeningScreen(restored) { opening = false }
                        1 -> LoginScreen(repo) { st -> auth = st }
                        else -> auth?.let { a -> AppShell(a, data) { repo.logout(); data.clear(); BackgroundJobs.cancelAll(applicationContext); auth = null } }
                    }
                }
            }
        }
    }
}
