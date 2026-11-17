package dev.saged.grcs.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.saged.grcs.auth.AuthRepo
import dev.saged.grcs.auth.SessionStore
import java.util.concurrent.TimeUnit

/** مزامنة بالخلفية: ترفع المعلّق كاملاً ثم تسحب التغييرات (وتحدّث قائمة حسابات المستفيدين للأدمن). */
class SyncWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val app = applicationContext
        val repo = AuthRepo(SessionStore(app))
        val st = repo.restore() ?: return Result.success()
        if (st.offline) return Result.retry()
        return runCatching { DataRepo(app, repo).sync(st); Result.success() }.getOrElse { Result.retry() }
    }
}

/** فحص التحديث صامتاً وتنزيله بالخلفية ثم إشعار «اضغط للتثبيت». */
class UpdateWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        Updater.restore(applicationContext)
        Updater.autoCheck(applicationContext, download = true)
        return Result.success()
    }
}

object BackgroundJobs {
    private val net = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** كل 6 ساعات للمزامنة ومرة يومياً للتحديث؛ KEEP حتى لا تُعاد جدولتها عند كل فتح. */
    fun schedule(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        wm.cancelUniqueWork("sync12h")
        wm.enqueueUniquePeriodicWork("sync6h", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS, 1, TimeUnit.HOURS).setConstraints(net).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES).build())
        wm.enqueueUniquePeriodicWork("update24h", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UpdateWorker>(24, TimeUnit.HOURS, 4, TimeUnit.HOURS).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()).build())
    }

    /** مزامنة فور عودة الإنترنت بعد حفظ تعذّر رفعه (يُنفَّذ مرة واحدة مهما تكرر الطلب). */
    fun kick(ctx: Context) {
        WorkManager.getInstance(ctx).enqueueUniqueWork("sync-now", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(net).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES).build())
    }

    fun cancelAll(ctx: Context) { WorkManager.getInstance(ctx).apply { cancelUniqueWork("sync6h"); cancelUniqueWork("update24h"); cancelUniqueWork("sync-now") } }
}
