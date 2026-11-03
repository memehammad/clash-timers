package dev.memehammad.clashtimers

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Format {
    /** Show finish times ("Tomorrow 12:30") instead of countdowns. Set by [configure]. */
    @Volatile
    var clock = false
        private set

    @Volatile
    private var is24 = true

    /** Reads the setting and the phone's 12/24-hour preference. Cheap; call before formatting. */
    fun configure(context: Context) {
        clock = Store.timeMode(context) == 1
        is24 = android.text.format.DateFormat.is24HourFormat(context)
    }

    fun countdown(ms: Long): String {
        val s = (ms + 999) / 1000
        val d = s / 86400
        val h = (s % 86400) / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return when {
            d > 0 -> "${d}d ${h}h"
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m ${sec.toString().padStart(2, '0')}s"
            else -> "${sec}s"
        }
    }

    /** "18:45", "Tomorrow 12:30" ("Tmrw" when [short]), "Wed 09:10", "5 Nov 14:00". */
    fun at(endMs: Long, nowMs: Long, short: Boolean = false): String {
        val time = SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault()).format(Date(endMs))
        val end = Calendar.getInstance().apply { timeInMillis = endMs }
        val today = Calendar.getInstance().apply { timeInMillis = nowMs }
        val days = dayIndex(end) - dayIndex(today)
        return when {
            days <= 0 -> time
            days == 1L -> "${if (short) "Tmrw" else "Tomorrow"} $time"
            days < 7 -> "${SimpleDateFormat("EEE", Locale.getDefault()).format(Date(endMs))} $time"
            else -> "${SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(endMs))} $time"
        }
    }

    private fun dayIndex(c: Calendar): Long =
        c.get(Calendar.YEAR) * 400L + c.get(Calendar.DAY_OF_YEAR)

    /** Remaining time in the chosen style: "2h 22m" or "18:45". */
    fun left(leftMs: Long, nowMs: Long, short: Boolean = false): String =
        if (clock) at(nowMs + leftMs, nowMs, short) else countdown(leftMs)

    /** For sentences: "in 2h 22m" or "at 18:45" / "Tomorrow 12:30". */
    fun phrase(leftMs: Long, nowMs: Long): String {
        if (!clock) return "in ${countdown(leftMs)}"
        val t = at(nowMs + leftMs, nowMs)
        return if (t.first().isDigit()) "at $t" else t
    }

    fun ago(ms: Long): String {
        val min = ms / 60_000
        return when {
            min < 1 -> "just now"
            min < 60 -> "$min min ago"
            min < 1440 -> "${min / 60} h ago"
            else -> "${min / 1440} d ago"
        }
    }
}
