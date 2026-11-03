package dev.mathias.clashtimers

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import android.os.IBinder
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Method
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * Shizuku user service. Runs in its own process with shell (ADB) rights, which Android allows to
 * read the clipboard in the background. It checks the clipboard's timestamp every couple of
 * seconds; when it changes while Clash of Clans is in front and the text is a village export,
 * it hands the text to [ImportReceiver] with `am broadcast`.
 *
 * Only the clipboard description is polled, which is cheap and silent. The content is read once
 * per new copy, so Android's "pasted from your clipboard" notice appears at most then.
 */
class ClipWatcher : IClipWatcher.Stub() {

    @Volatile
    private var token: String? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.addHiddenApiExemptions("")
            } catch (_: Throwable) {
            }
        }
        thread(name = "clip-watcher") { loop() }
    }

    override fun setToken(token: String?) {
        this.token = token
    }

    override fun destroy() {
        exitProcess(0)
    }

    private fun loop() {
        val clipboard = try {
            Clipboard()
        } catch (e: Throwable) {
            System.err.println("ClashTimers watcher: clipboard unavailable: $e")
            return
        }
        var last = clipboard.stampOrNull()
        while (true) {
            try {
                Thread.sleep(POLL_MS)
                val stamp = clipboard.stampOrNull() ?: continue
                if (stamp == last) continue
                last = stamp
                if (!clashInFront()) continue
                val text = clipboard.textOrNull() ?: continue
                if (!VillageParser.looksLikeVillage(text)) continue
                send(text)
            } catch (e: InterruptedException) {
                return
            } catch (e: Throwable) {
                System.err.println("ClashTimers watcher: $e")
            }
        }
    }

    private fun send(text: String) {
        val t = token ?: return
        val cmd = listOf(
            "am", "broadcast",
            "-n", "$PACKAGE/.ImportReceiver",
            "-a", ImportReceiver.ACTION,
            "--es", ImportReceiver.EXTRA_TOKEN, t,
            "--es", ImportReceiver.EXTRA_JSON, text,
        )
        ProcessBuilder(cmd).redirectErrorStream(true).start().apply {
            inputStream.readBytes()
            waitFor()
        }
    }

    /** True if Clash of Clans is the resumed app (or if that can't be determined). */
    private fun clashInFront(): Boolean = try {
        val p = ProcessBuilder("dumpsys", "activity", "activities").redirectErrorStream(true).start()
        val lines = p.inputStream.bufferedReader().useLines { seq ->
            seq.filter { "ResumedActivity" in it }.take(4).toList()
        }
        p.destroy()
        lines.isEmpty() || lines.any { CLASH in it }
    } catch (_: Throwable) {
        true
    }

    /** Hidden IClipboard calls as the shell package; argument lists differ between Android versions. */
    private class Clipboard {
        private val service: Any
        private val getDescription: Method
        private val getClip: Method

        init {
            val binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, "clipboard") as IBinder
            service = Class.forName("android.content.IClipboard\$Stub")
                .getMethod("asInterface", IBinder::class.java)
                .invoke(null, binder)!!
            val methods = service.javaClass.methods
            getDescription = methods.first { it.name == "getPrimaryClipDescription" }
            getClip = methods.first { it.name == "getPrimaryClip" }
        }

        /** Fills (callingPackage, [attributionTag], userId, [deviceId]) by parameter type. */
        private fun args(m: Method): Array<Any?> {
            var strings = 0
            var ints = 0
            return m.parameterTypes.map { type ->
                when (type) {
                    String::class.java -> if (strings++ == 0) SHELL else null
                    Int::class.javaPrimitiveType -> { ints++; 0 } // user 0, default device 0
                    else -> null
                }
            }.toTypedArray()
        }

        fun stampOrNull(): Long? {
            val d = getDescription.invoke(service, *args(getDescription)) as ClipDescription? ?: return null
            if (!d.hasMimeType("text/*")) return -1L
            return d.timestamp
        }

        fun textOrNull(): String? {
            val clip = getClip.invoke(service, *args(getClip)) as ClipData? ?: return null
            if (clip.itemCount == 0) return null
            return clip.getItemAt(0).text?.toString()
        }
    }

    companion object {
        private const val POLL_MS = 2000L
        private const val SHELL = "com.android.shell"
        private const val PACKAGE = "dev.mathias.clashtimers"
        private const val CLASH = "com.supercell.clashofclans"
    }
}
