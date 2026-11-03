package dev.memehammad.clashtimers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/** Receives village exports from the clipboard watcher. Ignores anything without the right token. */
class ImportReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val token = intent.getStringExtra(EXTRA_TOKEN) ?: return
        if (token != Store.importToken(context)) return
        val json = intent.getStringExtra(EXTRA_JSON) ?: return
        if (Importer.import(context, json) == Importer.Result.IMPORTED) {
            Toast.makeText(context, R.string.auto_imported, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ACTION = "dev.memehammad.clashtimers.IMPORT"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_JSON = "json"
    }
}
