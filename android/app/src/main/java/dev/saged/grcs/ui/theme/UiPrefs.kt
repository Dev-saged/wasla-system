package dev.saged.grcs.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** تفضيلات التفاعل: اهتزاز خفيف وصوت نقرة، تُحفظ محلياً كما في إعدادات الويب. */
object UiPrefs {
    private var prefs: SharedPreferences? = null
    var haptic by mutableStateOf(true); private set
    var click by mutableStateOf(false); private set

    fun init(ctx: Context) {
        if (prefs != null) return
        prefs = ctx.applicationContext.getSharedPreferences("grcs_ui", Context.MODE_PRIVATE)
        haptic = prefs!!.getBoolean("haptic", true); click = prefs!!.getBoolean("click", false)
    }

    fun updateHaptic(v: Boolean) { haptic = v; prefs?.edit()?.putBoolean("haptic", v)?.apply() }
    fun updateClick(v: Boolean) { click = v; prefs?.edit()?.putBoolean("click", v)?.apply() }
}
