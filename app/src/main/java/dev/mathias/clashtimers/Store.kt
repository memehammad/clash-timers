package dev.mathias.clashtimers

import android.content.Context
import android.content.SharedPreferences

object Store {
    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("village", Context.MODE_PRIVATE)

    fun raw(context: Context): String? = prefs(context).getString("raw", null)

    fun load(context: Context): Village? = raw(context)?.let { VillageParser.parse(context, it) }

    fun save(context: Context, raw: String) {
        prefs(context).edit().putString("raw", raw).apply()
    }

    fun alarmCodes(context: Context): Set<String> =
        prefs(context).getStringSet("alarms", emptySet()) ?: emptySet()

    fun setAlarmCodes(context: Context, codes: Set<String>) {
        prefs(context).edit().putStringSet("alarms", codes).apply()
    }

    /** Home builder count set by the user; 0 = auto-detect from the export. */
    fun builderOverride(context: Context): Int = prefs(context).getInt("builders", 0)

    fun setBuilderOverride(context: Context, count: Int) {
        prefs(context).edit().putInt("builders", count.coerceAtLeast(0)).apply()
    }

    /** Timestamp of the last clipboard entry already checked, so each copy is read only once. */
    fun clipStamp(context: Context): Long = prefs(context).getLong("clip_ts", 0L)

    fun setClipStamp(context: Context, stamp: Long) {
        prefs(context).edit().putLong("clip_ts", stamp).apply()
    }
}

object Importer {
    enum class Result { IMPORTED, UNCHANGED, INVALID }

    fun import(context: Context, text: CharSequence?): Result {
        if (text == null || !VillageParser.looksLikeVillage(text)) return Result.INVALID
        val raw = text.toString().trim()
        val village = VillageParser.parse(context, raw) ?: return Result.INVALID

        val current = Store.load(context)
        if (current != null && current.tag == village.tag && village.timestamp <= current.timestamp) {
            return Result.UNCHANGED
        }

        Store.save(context, raw)
        Scheduler.reschedule(context, village)
        WidgetProvider.updateAll(context)
        return Result.IMPORTED
    }
}
