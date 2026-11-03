package dev.mathias.clashtimers

import android.app.Application
import rikka.shizuku.Shizuku

class ClashApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Shizuku hands us its binder whenever it starts (also after a reboot): restart the watcher.
        Shizuku.addBinderReceivedListenerSticky { AutoImport.ensure(this) }
    }
}
