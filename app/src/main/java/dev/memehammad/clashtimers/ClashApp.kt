package dev.memehammad.clashtimers

import android.app.Application
import rikka.shizuku.Shizuku

class ClashApp : Application() {
    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        // Before any provider (Smartspacer, widgets) runs, so texts are always available.
        L10n.context = base
    }

    override fun onCreate() {
        super.onCreate()
        Format.configure(this)
        // Shizuku hands us its binder whenever it starts (also after a reboot): restart the watcher.
        Shizuku.addBinderReceivedListenerSticky { AutoImport.ensure(this) }
    }
}
