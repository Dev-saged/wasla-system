package dev.saged.grcs.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** نسخة محلية من مستندات الأسر كما هي على الخادم (للقراءة الآن؛ الكتابة تُبنى فوقها لاحقاً). */
class FamCache(ctx: Context) : SQLiteOpenHelper(ctx, "grcs_cache.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE fam(camp TEXT NOT NULL, id TEXT NOT NULL, j TEXT, d TEXT, f TEXT, fid TEXT, u TEXT, PRIMARY KEY(camp,id))")
        db.execSQL("CREATE TABLE meta(k TEXT PRIMARY KEY, v TEXT)")
        pendTables(db)
    }

    private fun pendTables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS pend(camp TEXT NOT NULL, pid TEXT NOT NULL, doc TEXT, j TEXT NOT NULL, upd INTEGER, PRIMARY KEY(camp,pid))")
        db.execSQL("CREATE TABLE IF NOT EXISTS tomb(camp TEXT NOT NULL, pid TEXT NOT NULL, doc TEXT, at INTEGER, PRIMARY KEY(camp,pid))")
    }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) { pendTables(db) }

    class Pend(val id: String, val doc: String, val json: String, val upd: Long)
    class Tomb(val id: String, val doc: String, val at: Long)

    fun putPend(camp: String, id: String, doc: String, json: String, upd: Long) {
        writableDatabase.insertWithOnConflict("pend", null, ContentValues().apply {
            put("camp", camp); put("pid", id); put("doc", doc); put("j", json); put("upd", upd)
        }, SQLiteDatabase.CONFLICT_REPLACE)
        writableDatabase.delete("tomb", "camp=? AND pid=?", arrayOf(camp, id))
    }

    fun putTomb(camp: String, id: String, doc: String, at: Long) {
        writableDatabase.insertWithOnConflict("tomb", null, ContentValues().apply {
            put("camp", camp); put("pid", id); put("doc", doc); put("at", at)
        }, SQLiteDatabase.CONFLICT_REPLACE)
        writableDatabase.delete("pend", "camp=? AND pid=?", arrayOf(camp, id))
    }

    fun pending(camp: String): List<Pend> {
        val out = ArrayList<Pend>()
        readableDatabase.rawQuery("SELECT pid,doc,j,upd FROM pend WHERE camp=?", arrayOf(camp)).use {
            while (it.moveToNext()) out += Pend(it.getString(0), it.getString(1).orEmpty(), it.getString(2), it.getLong(3))
        }
        return out
    }

    fun tombs(camp: String): List<Tomb> {
        val out = ArrayList<Tomb>()
        readableDatabase.rawQuery("SELECT pid,doc,at FROM tomb WHERE camp=?", arrayOf(camp)).use {
            while (it.moveToNext()) out += Tomb(it.getString(0), it.getString(1).orEmpty(), it.getLong(2))
        }
        return out
    }

    /** يحذف علامة معلّقة فقط إن لم تتغيّر منذ أخذ اللقطة (حتى لا يضيع تعديل أُجري أثناء الرفع). */
    fun donePend(camp: String, id: String, upd: Long) { writableDatabase.delete("pend", "camp=? AND pid=? AND upd=?", arrayOf(camp, id, upd.toString())) }
    fun doneTomb(camp: String, id: String, at: Long) { writableDatabase.delete("tomb", "camp=? AND pid=? AND at=?", arrayOf(camp, id, at.toString())) }

    fun campsWithPending(): List<String> {
        val out = LinkedHashSet<String>()
        readableDatabase.rawQuery("SELECT camp FROM pend UNION SELECT camp FROM tomb", null).use { while (it.moveToNext()) out += it.getString(0) }
        return out.toList()
    }

    /** صفوف المستندات: (المعرّف، j, d). */
    fun docRows(camp: String): List<Triple<String, String, String>> {
        val out = ArrayList<Triple<String, String, String>>()
        readableDatabase.rawQuery("SELECT id,j,d FROM fam WHERE camp=?", arrayOf(camp)).use { while (it.moveToNext()) out += Triple(it.getString(0), it.getString(1).orEmpty(), it.getString(2).orEmpty()) }
        return out
    }

    fun put(camp: String, docs: List<FamDoc>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (d in docs) db.insertWithOnConflict("fam", null, ContentValues().apply {
                put("camp", camp); put("id", d.id); put("j", d.j); put("d", d.d); put("f", d.f); put("fid", d.fid); put("u", d.u)
            }, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun meta(k: String): String? = readableDatabase.rawQuery("SELECT v FROM meta WHERE k=?", arrayOf(k)).use { if (it.moveToFirst()) it.getString(0) else null }

    fun setMeta(k: String, v: String) {
        writableDatabase.insertWithOnConflict("meta", null, ContentValues().apply { put("k", k); put("v", v) }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /** كل (j, d) لمخيم. */
    fun docs(camp: String): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        readableDatabase.rawQuery("SELECT j,d FROM fam WHERE camp=?", arrayOf(camp)).use { while (it.moveToNext()) out += it.getString(0).orEmpty() to it.getString(1).orEmpty() }
        return out
    }

    /** عند الخروج: يُبقي التعديلات غير المرفوعة حتى لا تضيع. */
    fun clear() {
        writableDatabase.execSQL("DELETE FROM fam"); writableDatabase.execSQL("DELETE FROM meta")
    }

    /** يمحو كل ما محليّاً لمخيم (أسر ومعلّق وشواهد ومؤشرات). */
    fun wipeCamp(camp: String) {
        val db = writableDatabase
        db.delete("fam", "camp=?", arrayOf(camp)); db.delete("pend", "camp=?", arrayOf(camp)); db.delete("tomb", "camp=?", arrayOf(camp))
        db.delete("meta", "k IN (?,?)", arrayOf("since_$camp", "cleared_$camp"))
    }

    fun hasPending() = campsWithPending().isNotEmpty()
}
