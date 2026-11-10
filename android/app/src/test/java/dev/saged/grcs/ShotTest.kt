package dev.saged.grcs

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import dev.saged.grcs.auth.AuthRepo
import dev.saged.grcs.auth.AuthState
import dev.saged.grcs.auth.AuthUser
import dev.saged.grcs.auth.SessionStore
import dev.saged.grcs.data.DataRepo
import dev.saged.grcs.data.FamCache
import dev.saged.grcs.ui.login.LoginScreen
import dev.saged.grcs.ui.shell.AppShell
import dev.saged.grcs.ui.theme.GrcsTheme
import dev.saged.grcs.ui.theme.ThemePref
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** لقطات مرجعية بنفس بيانات docs/web-spec/fixture.json للمقارنة مع لقطات الويب. تعمل فقط مع GRCS_SHOTS=مجلد. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ar-w412dp-h915dp-mdpi")
class ShotTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val out get() = System.getenv("GRCS_SHOTS")

    private fun settle(n: Int = 8) { repeat(n) { rule.waitForIdle(); Thread.sleep(120); rule.mainClock.advanceTimeBy(400) }; rule.waitForIdle() }
    private fun shot(name: String) { settle(); com.github.takahirom.roborazzi.captureScreenRoboImage("$out/$name.png") }
    /** i = -1: العنصر الأدنى الظاهر على الشاشة (الشريط السفلي). */
    private fun tap(text: String, i: Int = 0): Boolean {
        val n = rule.onAllNodesWithText(text)
        val all = n.fetchSemanticsNodes()
        if (all.size <= maxOf(i, 0)) { println("MISSING[$text]"); return false }
        val idx = if (i >= 0) i else all.indices.filter { all[it].boundsInRoot.top < 915f }.maxByOrNull { all[it].boundsInRoot.top } ?: 0
        n[idx].performClick(); settle(); return true
    }

    private fun run(role: String, steps: () -> Unit) {
        assumeTrue(out != null)
        val ctx = rule.activity.applicationContext
        val fx = JSONObject(File("../../docs/web-spec/fixture.json").readText())
        val cache = FamCache(ctx)
        val camps = fx.getJSONArray("camps"); val names = JSONObject()
        for (i in 0 until camps.length()) camps.getJSONObject(i).let { names.put(it.getString("id"), it.getString("name")) }
        cache.setMeta("camps", names.toString())
        val ps = fx.getJSONArray("persons")
        for (i in 0 until ps.length()) ps.getJSONObject(i).let { cache.putPend(it.getString("campId"), it.getString("id"), it.getString("familyId"), it.toString(), it.optLong("updatedAt")) }
        val u = fx.getJSONObject("users").getJSONObject(role).apply { put("fullName", optString("displayName")) }
        val auth = AuthState(AuthUser(u), "", Long.MAX_VALUE, true)
        val repo = AuthRepo(SessionStore(ctx)); val data = DataRepo(ctx, repo)
        ThemePref.init(ctx); ThemePref.set(if (System.getenv("GRCS_DARK") != null) 2 else 1)
        rule.mainClock.autoAdvance = false
        rule.setContent { GrcsTheme { AppShell(auth, data) {} } }
        settle(20); shot("$role-home"); if (System.getenv("GRCS_DUMP") != null) rule.onAllNodes(androidx.compose.ui.test.SemanticsMatcher("any") { it.boundsInRoot.top > 800f }).fetchSemanticsNodes().forEach { println("NODE ${it.boundsInRoot} ${it.config}") }; steps()
    }

    @Test fun login() {
        assumeTrue(out != null)
        val ctx = rule.activity.applicationContext
        ThemePref.init(ctx); ThemePref.set(1); rule.mainClock.autoAdvance = false
        rule.setContent { GrcsTheme { LoginScreen(AuthRepo(SessionStore(ctx))) {} } }
        shot("admin-login")
    }
    @Test fun admin() = run("admin") { for ((t, n) in listOf("المخيمات" to "camps-admin", "الرسائل" to "messages-list", "الحسابات" to "accounts-admin", "الإعدادات" to "settings")) { tap(t, -1); shot("admin-$n") } }
    @Test fun delegate() = run("delegate") { for ((t, n) in listOf("الأفراد" to "persons", "الرسائل" to "messages-list", "تصدير" to "export", "الإعدادات" to "settings")) { tap(t, -1); shot("delegate-$n") } }
    @Test fun delegatePeople() = run("delegate") { tap("الأفراد", -1); settle(10); shot("delegate-persons2"); if (tap("محمد أحمد سالم النجار")) shot("delegate-person-detail") }
    @Test fun dataentry() = run("dataentry") { tap("الأفراد", -1); shot("dataentry-persons"); tap("الإعدادات", -1); shot("dataentry-settings") }
    @Test fun beneficiary() = run("beneficiary") { }
}
