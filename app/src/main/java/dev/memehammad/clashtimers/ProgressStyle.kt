package dev.memehammad.clashtimers

import android.content.Context

/** Progress bar style for the app and widgets (setting). */
object ProgressStyle {
    const val WAVY = 0
    const val STRAIGHT = 1
    const val OFF = 2

    @Volatile
    var current = WAVY
        private set

    fun configure(context: Context) {
        current = Store.progressStyle(context)
    }
}
