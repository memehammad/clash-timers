package dev.memehammad.clashtimers

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

/** Starts and stops the Shizuku clipboard watcher ([ClipWatcher]). */
object AutoImport {
    enum class State { SHIZUKU_MISSING, NEEDS_PERMISSION, OFF, ON }

    private var appContext: Context? = null

    private val args by lazy {
        Shizuku.UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, ClipWatcher::class.java.name))
            .daemon(true)
            .processNameSuffix("watcher")
            .debuggable(false)
            .version(BuildConfig.VERSION_CODE)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val ctx = appContext ?: return
            if (binder == null || !binder.pingBinder()) return
            try {
                IClipWatcher.Stub.asInterface(binder).setToken(Store.importToken(ctx))
            } catch (_: Throwable) {
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    fun shizukuRunning(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    fun hasPermission(): Boolean = try {
        shizukuRunning() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    fun state(context: Context): State = when {
        !shizukuRunning() -> State.SHIZUKU_MISSING
        !hasPermission() -> State.NEEDS_PERMISSION
        Store.autoImport(context) -> State.ON
        else -> State.OFF
    }

    /** Called on app start and whenever Shizuku (re)starts: keep the watcher running if enabled. */
    fun ensure(context: Context) {
        appContext = context.applicationContext
        if (Store.autoImport(context) && hasPermission()) start(context)
    }

    fun start(context: Context) {
        appContext = context.applicationContext
        try {
            Shizuku.bindUserService(args, connection)
        } catch (_: Throwable) {
        }
    }

    fun stop(context: Context) {
        appContext = context.applicationContext
        try {
            Shizuku.unbindUserService(args, connection, true)
        } catch (_: Throwable) {
        }
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        Store.setAutoImport(context, enabled)
        if (enabled) start(context) else stop(context)
    }
}
