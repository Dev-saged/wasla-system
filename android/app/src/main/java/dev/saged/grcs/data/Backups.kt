package dev.saged.grcs.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dev.saged.grcs.config.BrandConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupFile(val name: String, val size: Long, val modified: Long, val uri: Uri?, val file: File?)

/** نسخ احتياطية JSON على الجهاز: المستندات/«اسم التطبيق». أندرويد 10+ عبر MediaStore، وما قبله ملف مباشر (يحتاج إذن التخزين). */
class Backups(private val ctx: Context) {
    private val rel = "Documents/${BrandConfig.BACKUP_DIR}"
    private val legacyDir get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), BrandConfig.BACKUP_DIR)

    fun needsPermission() = Build.VERSION.SDK_INT < 29

    fun build(camp: String, campName: String, persons: List<JSONObject>): String =
        JSONObject().put("app", "grcs").put("v", 1).put("camp", camp).put("campName", campName)
            .put("exportedAt", System.currentTimeMillis()).put("count", persons.size).put("persons", JSONArray(persons)).toString()

    fun write(name: String, text: String): Boolean = runCatching {
        val bytes = text.toByteArray(Charsets.UTF_8)
        if (Build.VERSION.SDK_INT >= 29) {
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name); put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, rel)
            }
            val uri = ctx.contentResolver.insert(MediaStore.Files.getContentUri("external"), cv) ?: return false
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return false
        } else {
            legacyDir.mkdirs(); File(legacyDir, name).writeBytes(bytes)
        }
        true
    }.getOrDefault(false)

    fun list(): List<BackupFile> = runCatching {
        val out = ArrayList<BackupFile>()
        if (Build.VERSION.SDK_INT >= 29) {
            val coll = MediaStore.Files.getContentUri("external")
            ctx.contentResolver.query(coll, arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_MODIFIED),
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?", arrayOf("$rel%"), "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")?.use { c ->
                while (c.moveToNext()) out += BackupFile(c.getString(1), c.getLong(2), c.getLong(3) * 1000, ContentUris.withAppendedId(coll, c.getLong(0)), null)
            }
        } else legacyDir.listFiles { f -> f.extension == "json" }?.sortedByDescending { it.lastModified() }?.forEach { out += BackupFile(it.name, it.length(), it.lastModified(), null, it) }
        out
    }.getOrDefault(emptyList())

    fun read(b: BackupFile): String? = runCatching {
        if (b.uri != null) ctx.contentResolver.openInputStream(b.uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } else b.file?.readText()
    }.getOrNull()

    fun fileName(camp: String, auto: Boolean) = "${if (auto) "auto" else "نسخة"}-$camp-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".json"

    /** نسخة تلقائية مرة كل يوم لكل مخيم (تُحفظ آخر 14 فقط لكل مخيم). */
    fun autoDaily(cache: FamCache, camp: String, campName: String, persons: List<JSONObject>) {
        val day = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        if (persons.isEmpty() || cache.meta("autobak_$camp") == day) return
        if (!write(fileName(camp, true), build(camp, campName, persons))) return
        cache.setMeta("autobak_$camp", day)
        list().filter { it.name.startsWith("auto-$camp-") }.drop(14).forEach { b ->
            runCatching { if (b.uri != null) ctx.contentResolver.delete(b.uri, null, null) else b.file?.delete() }
        }
    }
}
