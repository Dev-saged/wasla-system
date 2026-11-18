package dev.saged.grcs.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class JobState(val title: String, val pct: Int, val label: String, val result: ExportResult? = null)

/** مهام التصدير: تعمل بخدمة أمامية بإشعار تقدّم، فتكتمل حتى لو غادر المستخدم التطبيق. */
object ExportJobs {
    val state = MutableStateFlow<JobState?>(null)
    internal var block: (() -> ExportResult)? = null
    internal var title = ""
    @Volatile internal var cancelled = false
    fun cancel() { cancelled = true }

    /** يبدأ مهمة؛ block تستقبل دالة تقدّم (نسبة، وصف) وتعيد النتيجة. */
    fun start(ctx: Context, title: String, block: ((Int, String) -> Unit) -> ExportResult): Boolean {
        if (state.value?.result == null && state.value != null) return false
        this.title = title; cancelled = false
        this.block = { block { pp, ll -> if (cancelled) throw java.util.concurrent.CancellationException(); ExportService.report(ctx.applicationContext, title, pp, ll) } }
        state.value = JobState(title, 0, "جارٍ التجهيز")
        val i = Intent(ctx, ExportService::class.java)
        if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        return true
    }
    fun clear() { if (state.value?.result != null) state.value = null }
}

class ExportService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val t = ExportJobs.title
        val n = build(this, t, 0, "جارٍ التجهيز", null)
        if (Build.VERSION.SDK_INT >= 29) startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else startForeground(NID, n)
        val job = ExportJobs.block
        if (job == null) { stopSelf(); return START_NOT_STICKY }
        scope.launch {
            val r = runCatching { job() }.getOrElse { ExportResult(false, if (it is java.util.concurrent.CancellationException) "ألغي" else t, null, 0) }
            if (ExportJobs.cancelled) { ExportJobs.state.value = null; ExportJobs.block = null; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return@launch }
            ExportJobs.state.value = JobState(t, 100, if (r.ok) "اكتمل" else "تعذّر الحفظ", r)
            ExportJobs.block = null
            stopForeground(STOP_FOREGROUND_DETACH)
            val done = build(this@ExportService, t, 100, if (r.ok) "اكتمل — اضغط لفتح الملف" else "تعذّر الحفظ — تحقق من إذن التخزين", r)
            runCatching { NotificationManagerCompat.from(this@ExportService).notify(NID, done) }
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() { scope.coroutineContext[kotlinx.coroutines.Job]?.cancel(); super.onDestroy() }

    companion object {
        private const val NID = 7002
        private const val CH = "exports"
        private var lastPct = -1

        fun report(ctx: Context, title: String, pct: Int, label: String) {
            if (pct == lastPct) return
            lastPct = pct
            ExportJobs.state.value = JobState(title, pct, label)
            val nm = NotificationManagerCompat.from(ctx)
            if (nm.areNotificationsEnabled()) runCatching { nm.notify(NID, build(ctx, title, pct, label, null)) }
        }

        fun build(ctx: Context, title: String, pct: Int, label: String, r: ExportResult?): Notification {
            if (Build.VERSION.SDK_INT >= 26) (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(NotificationChannel(CH, "التصدير", NotificationManager.IMPORTANCE_LOW))
            val b = NotificationCompat.Builder(ctx, CH).setSmallIcon(if (r == null) android.R.drawable.stat_sys_download else android.R.drawable.stat_sys_download_done)
                .setContentTitle(title).setContentText(if (r == null) "$label — $pct%" else label).setOnlyAlertOnce(true).setOngoing(r == null).setAutoCancel(true)
            if (r == null) b.setProgress(100, pct, false)
            else if (r.ok && r.uri != null) {
                val mime = if (r.name.endsWith(".xlsx")) "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" else if (r.name.endsWith(".csv")) "text/csv" else "application/pdf"
                b.setContentIntent(PendingIntent.getActivity(ctx, 1, Intent(Intent.ACTION_VIEW).setDataAndType(r.uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
            return b.build()
        }
    }
}
