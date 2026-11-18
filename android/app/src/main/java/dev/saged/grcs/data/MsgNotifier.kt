package dev.saged.grcs.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.saged.grcs.MainActivity
import dev.saged.grcs.auth.AuthState
import org.json.JSONObject

/** إشعارات الرسائل: يقارن نبض m/g بآخر توقيع، فإن تغيّر يجلب أحدث الرسائل ويعرض إشعاراً لكل محادثة جديدة. */
object MsgNotifier {
    const val EXTRA_CHAT = "open_chat"
    private const val CH = "messages"
    private const val GROUP = "grcs_msgs"
    private const val SUMMARY = 7100

    private fun prefs(c: Context) = c.getSharedPreferences("grcs_notif", Context.MODE_PRIVATE)

    private fun sig(st: AuthState): String? {
        val f = runCatching { Fs.get("sync/pulse", st.idToken) }.getOrNull() ?: return null
        val admin = st.user.role == "admin"
        val parts = ArrayList<String>()
        for (scope in listOf("m", "g")) {
            val node = f.optJSONObject(scope) ?: continue
            node.optString("integerValue").takeIf { it.isNotEmpty() }?.let { parts += "$scope=$it" }
            val m = node.optJSONObject("mapValue")?.optJSONObject("fields") ?: continue
            for (k in m.keys()) if (admin || k == st.user.campId) parts += "$scope.$k=${m.getJSONObject(k).optString("integerValue")}"
        }
        return parts.sorted().joinToString("|")
    }

    private fun fetch(st: AuthState, camps: List<String>): List<JSONObject> {
        val me = st.user
        val out = ArrayList<JSONObject>()
        if (me.role == "beneficiary") {
            Fs.recentMessages("camps/${me.campId}", "benMessages", st.idToken, 20).filterTo(out) { it.optString("threadId") == "ben_" + me.id }
        } else {
            for (c in camps) {
                runCatching { out += Fs.recentMessages("camps/$c", "messages", st.idToken, 15) }
                runCatching { Fs.recentMessages("camps/$c", "benMessages", st.idToken, 20).filterTo(out) { it.optString("threadId").startsWith("ben_") } }
            }
            runCatching { out += Fs.recentMessages("globalMessages/global", "messages", st.idToken, 10) }
        }
        return out.distinctBy { it.optString("id") }
    }

    /** يعيد عدد الرسائل الجديدة؛ لا يعرض إشعاراً إن كان التطبيق ظاهراً (الشارة داخل التطبيق تكفي). */
    fun check(ctx: Context, st: AuthState, camps: List<String>): Int {
        if (st.offline) return 0
        val app = ctx.applicationContext; val pr = prefs(app)
        val s = sig(st) ?: return 0
        val kS = "sig_" + st.user.id; val kT = "ts_" + st.user.id
        if (s == pr.getString(kS, null)) return 0
        val since = pr.getLong(kT, 0L)
        val all = runCatching { fetch(st, camps) }.getOrNull() ?: return 0
        val top = maxOf(since, all.maxOfOrNull { it.optLong("timestamp") } ?: 0L, if (since == 0L) Clock.now() else 0L)
        val fresh = if (since == 0L) emptyList() else all.filter {
            val th = it.optString("threadId").ifEmpty { it.optString("campId") }
            it.optString("senderId") != st.user.id && it.optLong("timestamp") > since && it.optLong("timestamp") > Messages.Seen.get(st.user.id, th)
        }.sortedBy { it.optLong("timestamp") }
        pr.edit().putString(kS, s).putLong(kT, top).apply()
        if (fresh.isNotEmpty() && !dev.saged.grcs.Fg.on) show(app, fresh)
        return fresh.size
    }

    private fun show(ctx: Context, fresh: List<JSONObject>) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(CH, "الرسائل", NotificationManager.IMPORTANCE_HIGH))
        val pi = PendingIntent.getActivity(ctx, 1, Intent(ctx, MainActivity::class.java).putExtra(EXTRA_CHAT, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val groups = fresh.groupBy { it.optString("threadId").ifEmpty { it.optString("campId") } }
        for ((th, ms) in groups) {
            val last = ms.last()
            val who = last.optString("senderName").ifEmpty { "رسالة جديدة" }
            val b = NotificationCompat.Builder(ctx, CH).setSmallIcon(android.R.drawable.stat_notify_chat).setContentTitle(who)
                .setContentText(last.optString("text")).setContentIntent(pi).setAutoCancel(true).setGroup(GROUP).setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH).setWhen(last.optLong("timestamp")).setNumber(ms.size)
            if (ms.size > 1) b.setStyle(NotificationCompat.InboxStyle().also { st -> ms.takeLast(5).forEach { st.addLine(it.optString("text")) } }.setSummaryText("${ms.size} رسائل"))
            else b.setStyle(NotificationCompat.BigTextStyle().bigText(last.optString("text")))
            runCatching { nm.notify(7200 + (th.hashCode() and 0xFFF), b.build()) }
        }
        if (groups.size > 1) runCatching {
            nm.notify(SUMMARY, NotificationCompat.Builder(ctx, CH).setSmallIcon(android.R.drawable.stat_notify_chat).setContentTitle("رسائل جديدة")
                .setContentText("${fresh.size} رسالة من ${groups.size} محادثات").setGroup(GROUP).setGroupSummary(true).setContentIntent(pi).setAutoCancel(true).build())
        }
    }

}
