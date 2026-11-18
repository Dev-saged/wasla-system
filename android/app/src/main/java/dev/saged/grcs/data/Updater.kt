package dev.saged.grcs.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import dev.saged.grcs.BuildConfig
import dev.saged.grcs.MainActivity
import dev.saged.grcs.auth.Http
import dev.saged.grcs.config.BrandConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class UpInfo(val tag: String, val url: String, val name: String, val size: Long, val build: Int, val notes: String = "")

sealed interface UpState {
    data object None : UpState
    class Available(val info: UpInfo) : UpState
    class Downloading(val info: UpInfo, val pct: Int) : UpState
    class Ready(val info: UpInfo, val file: File) : UpState
}

/** تحديث التطبيق: فحص آخر إصدار، تنزيل مستأنَف بالخلفية، تحقق من الحزمة والتوقيع، إشعار، ثم تثبيت بنقرة. */
object Updater {
    val state = MutableStateFlow<UpState>(UpState.None)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    const val EXTRA_INSTALL = "install_update"
    private const val CH = "updates"
    private const val NOTE_ID = 7001
    private val busy = java.util.concurrent.atomic.AtomicBoolean(false)

    private fun dir(ctx: Context) = File(ctx.filesDir, "updates").apply { mkdirs() }
    private fun semver(s: String) = Regex("\\d+").findAll(s.substringBefore('-')).map { it.value.toInt() }.toList()
    private fun cmp(a: String, b: String): Int {
        val x = semver(a); val y = semver(b)
        for (i in 0 until maxOf(x.size, y.size)) { val d = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 }); if (d != 0) return d }
        return 0
    }
    private fun newer(i: UpInfo) = cmp(i.tag, BuildConfig.VERSION_NAME).let { it > 0 || (it == 0 && i.build > 0 && BuildConfig.BASE_CODE + i.build > BuildConfig.VERSION_CODE) }

    private fun infoOf(name: String, tag: String, url: String, size: Long, notes: String = "") = UpInfo(tag, url, name, size, Regex("-b(\\d+)\\.apk$").find(name)?.groupValues?.get(1)?.toIntOrNull() ?: 0, notes)
    private fun notesOf(body: String) = body.lines().map { it.replace(Regex("^[-*#>\\s]+"), "").trim() }.filter { it.isNotEmpty() && !it.startsWith("الإصدار") && !it.startsWith("المستودع") }.take(6).joinToString("\n• ", "• ")

    /** آخر إصدار منشور أحدث من المثبّت، أو null. */
    fun check(): UpInfo? {
        val j = runCatching { Http.getPublic("https://api.github.com/repos/${BrandConfig.UPDATE_REPO}/releases/latest") }.getOrNull()?.takeIf { it.code == 200 }?.json ?: return null
        val a = j.optJSONArray("assets") ?: return null
        for (i in 0 until a.length()) a.getJSONObject(i).let {
            if (it.optString("name").endsWith(".apk")) return infoOf(it.optString("name"), j.optString("tag_name"), it.optString("browser_download_url"), it.optLong("size"), notesOf(j.optString("body"))).takeIf(::newer)
        }
        return null
    }

    /** يصادق على الملف: نفس الحزمة، رقم نسخة أعلى، ونفس شهادة التوقيع المثبّتة. */
    fun verify(ctx: Context, f: File): Boolean = runCatching {
        val pm = ctx.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        val arc = pm.getPackageArchiveInfo(f.path, flags) ?: return false
        if (arc.packageName != ctx.packageName) return false
        val code = if (Build.VERSION.SDK_INT >= 28) arc.longVersionCode else @Suppress("DEPRECATION") arc.versionCode.toLong()
        if (code <= BuildConfig.VERSION_CODE) return false
        val cur = pm.getPackageInfo(ctx.packageName, flags)
        certs(arc) == certs(cur) && certs(cur).isNotEmpty()
    }.getOrDefault(false)

    private fun certs(p: android.content.pm.PackageInfo): Set<String> {
        val sigs = if (Build.VERSION.SDK_INT >= 28) p.signingInfo?.apkContentsSigners else @Suppress("DEPRECATION") p.signatures
        return sigs.orEmpty().map { MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).joinToString("") { b -> "%02x".format(b) } }.toSet()
    }

    /** عند فتح التطبيق: يلتقط حزمة منزَّلة سابقاً (حتى لو مرّت أيام) أو ينظّف القديمة. */
    fun restore(ctx: Context) {
        val files = dir(ctx).listFiles { f -> f.name.endsWith(".apk") }.orEmpty()
        val ok = files.filter { verify(ctx, it) }.maxByOrNull { it.lastModified() }
        files.filter { it != ok }.forEach { it.delete() }
        dir(ctx).listFiles { f -> f.name.endsWith(".part") }.orEmpty().filter { System.currentTimeMillis() - it.lastModified() > 7L * 86_400_000 }.forEach { it.delete() }
        if (ok != null) state.value = UpState.Ready(infoOf(ok.name, Regex("GRCS-(v[\\d.]+)").find(ok.name)?.groupValues?.get(1) ?: "", "", ok.length()), ok)
        else if (state.value is UpState.Ready) state.value = UpState.None
    }

    /** فحص صامت: يضع الحالة Available إن وُجد إصدار أحدث ولم يكن مُنزَّلاً. */
    fun autoCheck(ctx: Context, download: Boolean) {
        if (state.value is UpState.Ready || state.value is UpState.Downloading || !busy.compareAndSet(false, true)) return
        try {
            val i = check() ?: return
            state.value = UpState.Available(i)
            if (download) { busy.set(false); fetch(ctx, i) }
        } finally { busy.set(false) }
    }

    fun start(ctx: Context, i: UpInfo) { scope.launch { fetch(ctx, i) } }

    /** تنزيل مع استئناف؛ ينتهي بحالة Ready وإشعار «اضغط للتثبيت». */
    fun fetch(ctx: Context, i: UpInfo): Boolean {
        if (!busy.compareAndSet(false, true)) return false
        val app = ctx.applicationContext
        val part = File(dir(app), i.name + ".part"); val out = File(dir(app), i.name)
        try {
            state.value = UpState.Downloading(i, 0)
            if (out.exists() && verify(app, out)) { done(app, i, out); return true }
            val c = URL(i.url).openConnection() as HttpURLConnection
            c.connectTimeout = 20_000; c.readTimeout = 30_000
            val have = if (part.exists()) part.length() else 0L
            if (have > 0) c.setRequestProperty("Range", "bytes=$have-")
            val code = c.responseCode
            if (code !in 200..299) { state.value = UpState.Available(i); return false }
            val append = code == 206
            val total = (if (append) have else 0L) + c.contentLengthLong.coerceAtLeast(0)
            java.io.FileOutputStream(part, append).use { os ->
                c.inputStream.use { ins ->
                    val buf = ByteArray(64 * 1024); var got = if (append) have else 0L; var last = -1
                    while (true) {
                        val n = ins.read(buf); if (n < 0) break
                        os.write(buf, 0, n); got += n
                        val pct = if (total > 0) (got * 100 / total).toInt() else 0
                        if (pct != last) { last = pct; state.value = UpState.Downloading(i, pct) }
                    }
                }
            }
            if (!part.renameTo(out)) { state.value = UpState.Available(i); return false }
            if (!verify(app, out)) { out.delete(); state.value = UpState.None; return false }
            done(app, i, out); return true
        } catch (e: Exception) {
            state.value = UpState.Available(i); return false
        } finally { busy.set(false) }
    }

    private fun done(ctx: Context, i: UpInfo, f: File) { state.value = UpState.Ready(i, f); notifyReady(ctx, i) }

    private fun notifyReady(ctx: Context, i: UpInfo) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(CH, "التحديثات", NotificationManager.IMPORTANCE_DEFAULT))
        val pi = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).putExtra(EXTRA_INSTALL, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CH).setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("اكتمل تنزيل التحديث ${i.tag}").setContentText("اضغط لتثبيت الإصدار الجديد").setContentIntent(pi).setAutoCancel(true).build()
        runCatching { nm.notify(NOTE_ID, n) }
    }

    /** يفتح مثبّت النظام للحزمة الجاهزة؛ إن لم يُسمح بالتثبيت من التطبيق يفتح الإعداد المناسب. */
    fun install(ctx: Context): Boolean {
        val r = state.value as? UpState.Ready ?: return false
        if (!r.file.exists() || !verify(ctx, r.file)) { restore(ctx); return false }
        if (Build.VERSION.SDK_INT >= 26 && !ctx.packageManager.canRequestPackageInstalls()) {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return false
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.updates", r.file)
        ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
        NotificationManagerCompat.from(ctx).cancel(NOTE_ID)
        return true
    }
}
