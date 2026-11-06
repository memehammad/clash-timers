package dev.memehammad.clashtimers

import android.app.Activity
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast

/**
 * Invisible screen behind the widget's import button. Android only lets the app in front read
 * the clipboard, so this comes to the front for a moment, imports, and closes again.
 */
class ImportActivity : Activity() {
    private var done = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || done) return
        done = true
        val cm = getSystemService(ClipboardManager::class.java)
        val text = try {
            cm?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)
        } catch (e: Exception) {
            null
        }
        val msg = when (Importer.import(this, text)) {
            Importer.Result.IMPORTED -> R.string.imported
            Importer.Result.UNCHANGED -> R.string.up_to_date
            Importer.Result.INVALID -> R.string.import_hint
        }
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        finish()
        overridePendingTransition(0, 0)
    }
}
