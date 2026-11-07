package dev.memehammad.clashtimers

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/** Saved exports (one per village tag) and settings. */
object Store {
    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("village", Context.MODE_PRIVATE)

    /** Notified whenever saved exports change (e.g. an auto-import while the app is open). */
    fun register(context: Context, l: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs(context).registerOnSharedPreferenceChangeListener(l)

    fun unregister(context: Context, l: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs(context).unregisterOnSharedPreferenceChangeListener(l)

    /** Whether the first-run tutorial was seen (or skipped). */
    fun tutorialSeen(context: Context): Boolean = prefs(context).getBoolean("tutorial_seen", false)

    fun setTutorialSeen(context: Context, seen: Boolean = true) =
        prefs(context).edit().putBoolean("tutorial_seen", seen).apply()

    // --- Villages ------------------------------------------------------------------------------

    /** tag -> raw export JSON. Migrates the single export saved by older versions. */
    private fun rawMap(context: Context): MutableMap<String, String> {
        val p = prefs(context)
        val out = LinkedHashMap<String, String>()
        p.getString("villages", null)?.let { s ->
            val o = JSONObject(s)
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                out[k] = o.getString(k)
            }
        }
        if (out.isEmpty()) {
            p.getString("raw", null)?.let { old ->
                val tag = try { JSONObject(old).optString("tag") } catch (_: Exception) { "" }
                if (tag.isNotEmpty()) {
                    out[tag] = old
                    writeRawMap(context, out)
                    setCurrentTag(context, tag)
                }
            }
        }
        return out
    }

    private fun writeRawMap(context: Context, map: Map<String, String>) {
        val o = JSONObject()
        for ((k, v) in map) o.put(k, v)
        prefs(context).edit().putString("villages", o.toString()).remove("raw").apply()
    }

    fun tags(context: Context): List<String> = rawMap(context).keys.toList()

    fun loadAll(context: Context): List<Village> =
        rawMap(context).values.mapNotNull { VillageParser.parse(context, it) }

    fun load(context: Context, tag: String): Village? =
        rawMap(context)[tag]?.let { VillageParser.parse(context, it) }

    /** The village selected in the app (falls back to the first one). */
    fun load(context: Context): Village? {
        val map = rawMap(context)
        val tag = currentTag(context)?.takeIf { it in map } ?: map.keys.firstOrNull() ?: return null
        return map[tag]?.let { VillageParser.parse(context, it) }
    }

    fun save(context: Context, tag: String, raw: String) {
        val map = rawMap(context)
        map[tag] = raw
        writeRawMap(context, map)
    }

    fun remove(context: Context, tag: String) {
        val map = rawMap(context)
        map.remove(tag)
        writeRawMap(context, map)
        if (currentTag(context) == tag) setCurrentTag(context, map.keys.firstOrNull())
    }

    fun currentTag(context: Context): String? = prefs(context).getString("current", null)

    fun setCurrentTag(context: Context, tag: String?) {
        prefs(context).edit().putString("current", tag).apply()
    }

    /** Optional nickname for a village; the tag otherwise. */
    fun name(context: Context, tag: String): String =
        prefs(context).getString("name_$tag", null)?.takeIf { it.isNotBlank() } ?: tag

    fun setName(context: Context, tag: String, name: String?) {
        prefs(context).edit().putString("name_$tag", name?.trim()).apply()
    }

    // --- Widgets -------------------------------------------------------------------------------

    /** Village shown by a widget; null = follow the app's selection. */
    fun widgetTag(context: Context, widgetId: Int): String? = prefs(context).getString("widget_$widgetId", null)

    fun setWidgetTag(context: Context, widgetId: Int, tag: String?) {
        prefs(context).edit().putString("widget_$widgetId", tag).apply()
    }

    fun widgetOptions(context: Context, widgetId: Int): WidgetOptions {
        val bits = prefs(context).getInt("widget_opts_$widgetId", WidgetOptions.ALL)
        return WidgetOptions(bits)
    }

    fun setWidgetOptions(context: Context, widgetId: Int, options: WidgetOptions) {
        prefs(context).edit().putInt("widget_opts_$widgetId", options.bits or WidgetOptions.SAVED_V2).apply()
    }

    fun loadForWidget(context: Context, widgetId: Int): Village? =
        widgetTag(context, widgetId)?.let { load(context, it) } ?: load(context)

    // --- Settings ------------------------------------------------------------------------------

    fun alarmCodes(context: Context): Set<String> =
        prefs(context).getStringSet("alarms", emptySet()) ?: emptySet()

    fun setAlarmCodes(context: Context, codes: Set<String>) {
        prefs(context).edit().putStringSet("alarms", codes).apply()
    }

    /** Auto-import through the Shizuku clipboard watcher. */
    fun autoImport(context: Context): Boolean = prefs(context).getBoolean("auto_import", false)

    fun setAutoImport(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("auto_import", enabled).apply()
    }

    /** Sections whose "finished" notifications are turned off (Section names). */
    fun mutedSections(context: Context): Set<String> = prefs(context).getStringSet("muted_sections", emptySet()).orEmpty()

    fun setMutedSections(context: Context, muted: Set<String>) {
        prefs(context).edit().putStringSet("muted_sections", muted.toSet()).apply()
    }

    /** Remind again when a builder stays idle this many minutes; 0 = off. */
    fun reminderMinutes(context: Context): Int = prefs(context).getInt("reminder_min", 0)

    fun setReminderMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt("reminder_min", minutes).apply()
    }

    /** Ongoing "next free builder" notification (a Live Update on Android 16). */
    fun liveUpdate(context: Context): Boolean = prefs(context).getBoolean("live_update", false)

    fun setLiveUpdate(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("live_update", enabled).apply()
    }

    /** Hours after something finishes (since the export) before the re-export hint; 0 = never. */
    fun staleHours(context: Context): Int = prefs(context).getInt("stale_hours", 12)

    fun setStaleHours(context: Context, hours: Int) {
        prefs(context).edit().putInt("stale_hours", hours).apply()
    }

    /** Check GitHub for new versions (on app open, at most every 12 h). */
    fun updateCheck(context: Context): Boolean = prefs(context).getBoolean("update_check", true)

    fun setUpdateCheck(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean("update_check", on).apply()
    }

    fun latestVersion(context: Context): String? = prefs(context).getString("latest_version", null)

    fun lastUpdateCheck(context: Context): Long = prefs(context).getLong("last_update_check", 0L)

    fun setLatestVersion(context: Context, version: String, at: Long) {
        prefs(context).edit().putString("latest_version", version).putLong("last_update_check", at).apply()
    }

    /** Progress bars: 0 wavy, 1 straight, 2 off. */
    fun progressStyle(context: Context): Int = prefs(context).getInt("progress_style", 0)

    fun setProgressStyle(context: Context, style: Int) {
        prefs(context).edit().putInt("progress_style", style).apply()
    }

    /** How remaining times are shown: 0 countdown, 1 finish time. */
    fun timeMode(context: Context): Int = prefs(context).getInt("time_mode", 0)

    fun setTimeMode(context: Context, mode: Int) {
        prefs(context).edit().putInt("time_mode", mode).apply()
    }

    /** What the Live Update status bar chip shows: 0 countdown, 1 finish time, 2 free builders. */
    fun liveChip(context: Context): Int = prefs(context).getInt("live_chip", 0)

    fun setLiveChip(context: Context, mode: Int) {
        prefs(context).edit().putInt("live_chip", mode).apply()
    }

    /** Random secret shared with the watcher, so only it can push exports to [ImportReceiver]. */
    fun importToken(context: Context): String {
        val p = prefs(context)
        p.getString("import_token", null)?.let { return it }
        val token = java.util.UUID.randomUUID().toString()
        p.edit().putString("import_token", token).apply()
        return token
    }

    /** Home builder count set by the user for a village; 0 = auto-detect from the export. */
    fun builderOverride(context: Context, tag: String): Int =
        prefs(context).getInt("builders_$tag", prefs(context).getInt("builders", 0))

    fun setBuilderOverride(context: Context, tag: String, count: Int) {
        prefs(context).edit().putInt("builders_$tag", count.coerceAtLeast(0)).apply()
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

        val existing = Store.load(context, village.tag)
        // A new export always becomes the selected village.
        Store.setCurrentTag(context, village.tag)
        if (existing != null && village.timestamp <= existing.timestamp) {
            Refresh.all(context)
            return Result.UNCHANGED
        }

        Store.save(context, village.tag, raw)
        Refresh.all(context)
        return Result.IMPORTED
    }
}

/** Re-derives everything from the saved exports: alarms, widgets, live notification. */
object Refresh {
    fun all(context: Context) {
        Scheduler.reschedule(context)
        WidgetProvider.updateAll(context)
        LiveStatus.update(context)
    }
}
