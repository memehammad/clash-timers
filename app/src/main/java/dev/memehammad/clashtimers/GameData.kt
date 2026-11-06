package dev.memehammad.clashtimers

import android.content.Context
import org.json.JSONObject

/** Name plus upgrade duration (seconds) per target level, from assets/coc_data.json. */
class GameInfo(val name: String, val times: Map<Int, Long>)

object GameData {
    @Volatile
    private var cache: Map<Long, GameInfo>? = null

    fun get(context: Context): Map<Long, GameInfo> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val loaded = load(context.applicationContext)
            cache = loaded
            return loaded
        }
    }

    private fun load(context: Context): Map<Long, GameInfo> {
        val text = context.assets.open("coc_data.json").bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        // In-game names in the phone's language (from the game's own translations), where known.
        val names = try {
            val lang = java.util.Locale.getDefault().language
            JSONObject(context.assets.open("coc_names.json").bufferedReader().use { it.readText() }).optJSONObject(lang)
        } catch (e: Exception) {
            null
        }
        val map = HashMap<Long, GameInfo>(root.length() * 2)
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val entry = root.getJSONObject(key)
            val times = HashMap<Int, Long>()
            entry.optJSONObject("t")?.let { t ->
                val levels = t.keys()
                while (levels.hasNext()) {
                    val level = levels.next()
                    times[level.toInt()] = t.getLong(level)
                }
            }
            map[key.toLong()] = GameInfo(names?.optString(key)?.takeIf { it.isNotEmpty() } ?: entry.getString("n"), times)
        }
        return map
    }
}
