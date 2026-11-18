package dev.saged.grcs.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

class ExFile(val name: String, val size: Long, val modified: Long, val uri: Uri?, val file: File?) {
    val kind: String get() = when {
        name.contains("كشف-الأسر") -> "كشف كامل"
        name.contains("أرباب") -> "أرباب الأسر"
        name.contains("-أفراد") -> "أفراد"
        name.contains("-أسر") -> "أسر"
        else -> "تقرير"
    }
    val mime: String get() = if (name.endsWith(".xlsx")) "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" else if (name.endsWith(".csv")) "text/csv" else "application/pdf"
}

/** الملفات المصدّرة في مجلد التطبيق (Documents/<مجلد التطبيق>/الكشوف)، الأحدث أولاً. */
object ExportFiles {
    fun list(ctx: Context): List<ExFile> = runCatching {
        val ex = Export(ctx)
        if (Build.VERSION.SDK_INT >= 29) {
            val out = ArrayList<ExFile>()
            val col = MediaStore.Files.getContentUri("external")
            ctx.contentResolver.query(col, arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_MODIFIED),
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?", arrayOf(ex.rel + "%"), "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")?.use { c ->
                while (c.moveToNext()) out += ExFile(c.getString(1), c.getLong(2), c.getLong(3) * 1000, android.content.ContentUris.withAppendedId(col, c.getLong(0)), null)
            }
            out
        } else ex.legacyDir.listFiles().orEmpty().filter { it.isFile }.sortedByDescending { it.lastModified() }.map { ExFile(it.name, it.length(), it.lastModified(), null, it) }
    }.getOrDefault(emptyList())

    fun delete(ctx: Context, f: ExFile): Boolean = runCatching {
        if (f.uri != null) ctx.contentResolver.delete(f.uri, null, null) > 0 else f.file?.delete() == true
    }.getOrDefault(false)

    fun uriOf(ctx: Context, f: ExFile): Uri? = f.uri ?: f.file?.let { runCatching { androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.updates", it) }.getOrNull() }
}
