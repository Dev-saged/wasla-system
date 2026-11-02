package dev.saged.grcs.data

import android.content.Context
import org.json.JSONObject

/** تخمين الجنس من الاسم الأول (معجم الويب نفسه من assets/gender_lex.json)؛ يعيد male أو female أو "" عند عدم اليقين. */
object GenderInfer {
    private var female = emptySet<String>(); private var male = emptySet<String>()
    private var compound = emptySet<String>(); private var marked = emptySet<String>()

    private fun key(s: String) = s.replace(Regex("[\\u064B-\\u065F\\u0670\\u0640\\u200e\\u200f\\ufeff]"), "").replace(Regex("[إأآٱ]"), "ا").replace('ى', 'ي').replace('ة', 'ه')
        .replace(Regex("[/\\-_,،]"), " ").replace(Regex("\\s+"), " ").trim().lowercase()
        .replace('ؤ', 'و').replace('ئ', 'ي').replace(Regex("(^|\\s)(عبد|ابو)\\s+"), "$1$2").replace(Regex("\\s+"), " ").trim()

    fun init(ctx: Context) {
        if (female.isNotEmpty()) return
        val o = JSONObject(ctx.assets.open("gender_lex.json").bufferedReader().use { it.readText() })
        fun set(vararg ks: String) = ks.flatMap { k -> o.optJSONArray(k)?.let { a -> (0 until a.length()).map { i -> key(a.getString(i)).replace(" ", "") } }.orEmpty() }.toHashSet()
        female = set("FEMALE_FIRST_NAMES", "FEMALE_EXTRA")
        male = set("MALE_FIRST_NAMES", "MALE_EXTRA") - female
        compound = set("MALE_COMPOUND_FIRST_NAMES"); marked = set("MALE_MARKED")
    }

    fun infer(fullName: String): String {
        if (female.isEmpty()) return ""
        val t = key(fullName).split(' ').filter { it.isNotEmpty() }
        if (t.isEmpty()) return ""
        val first = t[0]; val two = if (t.size > 1) first + t[1] else ""
        if (Regex("^(عبد|ابو).").containsMatchIn(first) || (t.getOrNull(1) == "الدين" || t.getOrNull(1) == "الله")) return "male"
        if (two.isNotEmpty() && two in compound) return "male"
        if (first in female) return "female"
        if (first in male || first in marked) return "male"
        val st = first.removePrefix("ال")
        if (st in female) return "female"
        if (st in male) return "male"
        if (fullName.trim().split(Regex("\\s+")).firstOrNull()?.endsWith("ى") == true) return "female"
        if (first.length >= 3 && (first.endsWith("ه") || first.endsWith("اء"))) return "female"
        return ""
    }
}
