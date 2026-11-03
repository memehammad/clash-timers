package dev.memehammad.clashtimers

object Format {
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
