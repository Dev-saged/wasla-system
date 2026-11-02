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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.saged.grcs.ui.opening.OpeningScreen
import dev.saged.grcs.ui.shell.AppShell
import dev.saged.grcs.ui.theme.GrcsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GrcsTheme {
                var opening by rememberSaveable { mutableStateOf(true) }
                AnimatedContent(
                    opening,
                    transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(400)) },
                    label = "root",
                ) { if (it) OpeningScreen { opening = false } else AppShell() }
            }
        }
    }
}
