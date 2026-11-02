package dev.saged.grcs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.saged.grcs.config.BrandConfig
import dev.saged.grcs.ui.theme.GrcsTheme
import dev.saged.grcs.ui.theme.LocalPalette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GrcsTheme { Foundation() } }
    }
}

@Composable
private fun Foundation() {
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize().background(p.bg), contentAlignment = Alignment.Center) {
        Text(BrandConfig.NAME, color = p.ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}
