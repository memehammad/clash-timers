package dev.memehammad.clashtimers.wear

import android.content.Context
import org.json.JSONObject

/** One kind of worker (builders, lab, ...) and when each running job ends. */
data class Slot(val key: String, val label: String, val total: Int, val ends: List<Long>) {
    /** Busy workers at [nowMs]. */
    fun busy(nowMs: Long): Int = ends.count { it > nowMs }.coerceAtMost(total)
    fun free(nowMs: Long): Int = (total - busy(nowMs)).coerceAtLeast(0)

    /** When the next worker frees up, if all are busy at [nowMs]. */
    fun nextFree(nowMs: Long): Long? {
        val running = ends.filter { it > nowMs }.sorted()
        return if (running.size >= total) running[running.size - total] else null
    }

    /** Times after [nowMs] where the free count changes. */
    fun changes(nowMs: Long): List<Long> = ends.filter { it > nowMs }.sorted()
}

data class Job(val name: String, val detail: String, val endMs: Long, val section: String, val extra: Boolean)

/** What the phone sends: a ready-made summary of the current village. */
data class Summary(val tag: String, val name: String, val exportedMs: Long, val slots: List<Slot>, val jobs: List<Job>) {
    val builders: Slot? get() = slots.firstOrNull { it.key == "BUILDER" }

    /** Every moment something changes (for tile and complication timelines). */
    fun changes(nowMs: Long): List<Long> = slots.flatMap { it.changes(nowMs) }.distinct().sorted()

    companion object {
        private const val PREFS = "wear"

        fun parse(json: String): Summary? = try {
            val o = JSONObject(json)
            val slots = o.optJSONArray("slots")?.let { a ->
                (0 until a.length()).map { i ->
                    val s = a.getJSONObject(i)
                    val e = s.optJSONArray("ends")
                    Slot(
                        s.getString("key"), s.getString("label"), s.getInt("total"),
                        e?.let { (0 until it.length()).map { k -> it.getLong(k) } }.orEmpty(),
                    )
                }
            }.orEmpty()
            val jobs = o.optJSONArray("jobs")?.let { a ->
                (0 until a.length()).map { i ->
                    val j = a.getJSONObject(i)
                    Job(j.getString("name"), j.optString("detail"), j.getLong("end"), j.optString("section"), j.optBoolean("extra"))
                }
            }.orEmpty()
            Summary(o.optString("tag"), o.optString("name"), o.optLong("exported"), slots, jobs)
        } catch (e: Exception) {
            null
        }

        fun save(context: Context, json: String) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("summary", json).commit()

        fun load(context: Context): Summary? =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("summary", null)?.let { parse(it) }
    }
}

/** "2h 14m", "45m", "30s". */
fun shortTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val d = s / 86400
    val h = s % 86400 / 3600
    val m = s % 3600 / 60
    return when {
        d > 0 -> "${d}d ${h}h"
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m"
        else -> "${s}s"
    }
}
