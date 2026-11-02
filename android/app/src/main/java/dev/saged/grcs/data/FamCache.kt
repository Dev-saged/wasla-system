package dev.saged.grcs.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** نسخة محلية من مستندات الأسر كما هي على الخادم (للقراءة الآن؛ الكتابة تُبنى فوقها لاحقاً). */
class FamCache(ctx: Context) : SQLiteOpenHelper(ctx, "grcs_cache.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE fam(camp TEXT NOT NULL, id TEXT NOT NULL, j TEXT, d TEXT, f TEXT, fid TEXT, u TEXT, PRIMARY KEY(camp,id))")
        db.execSQL("CREATE TABLE meta(k TEXT PRIMARY KEY, v TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {}

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

    fun clear() {
        writableDatabase.execSQL("DELETE FROM fam"); writableDatabase.execSQL("DELETE FROM meta")
    }
}
