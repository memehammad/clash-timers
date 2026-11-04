package dev.memehammad.clashtimers

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Optional update check: asks GitHub for the latest release tag (nothing else is sent) and
 * compares it with this build. At most every 12 hours, on app open.
 */
object Updates {
    const val REPO_URL = "https://github.com/memehammad/clash-timers"
    const val RELEASES_URL = "$REPO_URL/releases/latest"
    private const val API = "https://api.github.com/repos/memehammad/clash-timers/releases/latest"
    private const val INTERVAL_MS = 12 * 3600_000L

    data class Release(val version: String, val url: String)

    /** Newer release than this build, or null. Uses a cached result between checks. */
    suspend fun check(context: Context, force: Boolean = false): Release? {
        if (!Store.updateCheck(context)) return null
        val now = System.currentTimeMillis()
        val cached = Store.latestVersion(context)
        if (!force && cached != null && now - Store.lastUpdateCheck(context) < INTERVAL_MS) {
            return cached.takeIf { isNewer(it, BuildConfig.VERSION_NAME) }?.let { Release(it, RELEASES_URL) }
        }
        val latest = withContext(Dispatchers.IO) { fetch() } ?: return null
        Store.setLatestVersion(context, latest.version, now)
        return latest.takeIf { isNewer(it.version, BuildConfig.VERSION_NAME) }
    }

    private fun fetch(): Release? = try {
        val c = URL(API).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("User-Agent", "ClashTimers")
        val body = c.inputStream.bufferedReader().use { it.readText() }
        c.disconnect()
        val o = JSONObject(body)
        Release(o.getString("tag_name").removePrefix("v"), o.optString("html_url", RELEASES_URL))
    } catch (_: Exception) {
        null
    }

    /** Compares dotted versions numerically ("1.0.21" vs "1.0.9"). */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.split('.').map { it.toIntOrNull() ?: 0 }
        val b = current.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
