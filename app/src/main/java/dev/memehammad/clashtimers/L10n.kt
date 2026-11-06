package dev.memehammad.clashtimers

import android.content.Context

/** App-wide access to translated texts (set up in [ClashApp.attachBaseContext]). */
object L10n {
    @Volatile
    var context: Context? = null
}

/** A translated text from res/values*/strings.xml. */
fun tr(id: Int, vararg args: Any): String {
    val c = L10n.context ?: error("L10n not set up")
    return if (args.isEmpty()) c.getString(id) else c.getString(id, *args)
}

/** A translated text that depends on a count ([n] is also the first format argument). */
fun trq(id: Int, n: Int, vararg args: Any): String {
    val c = L10n.context ?: error("L10n not set up")
    return c.resources.getQuantityString(id, n, n, *args)
}
