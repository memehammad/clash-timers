package dev.mathias.clashtimers

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

/**
 * Alarms for every saved village: one per running timer (notification at the finish time),
 * one when the export turns stale (widget refresh) and, if enabled, an idle-builder reminder.
 */
object Scheduler {
    private const val ACTION_DONE = "dev.mathias.clashtimers.TIMER_DONE"
    private const val ACTION_REFRESH = "dev.mathias.clashtimers.REFRESH"
    const val ACTION_REMINDER = "dev.mathias.clashtimers.REMINDER"
    const val EXTRA_TITLE = "title"
    const val EXTRA_TEXT = "text"
    const val EXTRA_CODE = "code"
    const val EXTRA_TAG = "tag"
    const val EXTRA_FREE_SINCE = "free_since"

    fun reschedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        for (stored in Store.alarmCodes(context)) {
            val code = stored.toIntOrNull() ?: continue
            existing(context, code)?.let {
                am.cancel(it)
                it.cancel()
            }
        }

        val now = System.currentTimeMillis()
        val villages = Store.loadAll(context)
        val many = villages.size > 1
        val codes = mutableSetOf<String>()

        fun schedule(code: Int, at: Long, intent: Intent) {
            if (at <= now) return
            val pi = PendingIntent.getBroadcast(
                context, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            try {
                if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } catch (e: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
            codes += code.toString()
        }

        for (v in villages) {
            val name = Store.name(context, v.tag)
            for (t in v.timers) {
                val code = "${v.tag}|${t.key}".hashCode()
                schedule(
                    code, t.endSec * 1000,
                    baseIntent(context, code, ACTION_DONE)
                        .putExtra(EXTRA_TITLE, t.doneTitle)
                        .putExtra(EXTRA_TEXT, if (many) "${t.doneText} · $name" else t.doneText)
                        .putExtra(EXTRA_CODE, code),
                )
            }

            // Refresh widgets when the export becomes a day old (they show a re-export hint).
            val staleCode = "stale|${v.tag}".hashCode()
            schedule(staleCode, v.timestamp * 1000 + Stale.AFTER_MS, baseIntent(context, staleCode, ACTION_REFRESH))

            // Idle-builder reminder.
            val minutes = Store.reminderMinutes(context)
            if (minutes > 0) {
                val since = Reminder.freeSinceSec(v, now, Store.builderOverride(context, v.tag))
                if (since != null) {
                    val code = "remind|${v.tag}".hashCode()
                    schedule(
                        code, since * 1000 + minutes * 60_000L,
                        baseIntent(context, code, ACTION_REMINDER)
                            .putExtra(EXTRA_TAG, v.tag)
                            .putExtra(EXTRA_FREE_SINCE, since)
                            .putExtra(EXTRA_CODE, code),
                    )
                }
            }
        }
        Store.setAlarmCodes(context, codes)
    }

    private fun baseIntent(context: Context, code: Int, action: String): Intent =
        Intent(context, CompletionReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("clashtimers://alarm/$code"))

    private fun existing(context: Context, code: Int): PendingIntent? {
        for (action in listOf(ACTION_DONE, ACTION_REFRESH, ACTION_REMINDER)) {
            PendingIntent.getBroadcast(
                context, code, baseIntent(context, code, action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
            )?.let { return it }
        }
        return null
    }
}

object Notifier {
    private const val CHANNEL = "done"
    private const val CHANNEL_REMINDER = "reminder"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 3, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Opens Clash of Clans, or this app if Clash isn't installed. */
    fun openClash(context: Context): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(CLASH)
            ?: return openApp(context)
        return PendingIntent.getActivity(
            context, 4, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private const val CLASH = "com.supercell.clashofclans"

    @SuppressLint("MissingPermission")
    fun notify(context: Context, id: Int, title: String, text: String, reminder: Boolean = false) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_done), NotificationManager.IMPORTANCE_DEFAULT),
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDER, context.getString(R.string.channel_reminder), NotificationManager.IMPORTANCE_DEFAULT),
        )
        if (!canNotify(context)) return

        val notification = Notification.Builder(context, if (reminder) CHANNEL_REMINDER else CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openClash(context))
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_stat),
                    context.getString(R.string.open_timers),
                    openApp(context),
                ).build(),
            )
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()
        nm.notify(id, notification)
    }
}

class CompletionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Scheduler.ACTION_REMINDER -> remind(context, intent)
            // Only notify for alarms the current schedule knows. Alarms left behind by older app
            // versions (or for timers no longer in any export) would otherwise notify twice.
            else -> intent.getStringExtra(Scheduler.EXTRA_TITLE)?.takeIf {
                intent.getIntExtra(Scheduler.EXTRA_CODE, 0).toString() in Store.alarmCodes(context)
            }?.let { title ->
                Notifier.notify(
                    context,
                    intent.getIntExtra(Scheduler.EXTRA_CODE, 0),
                    title,
                    intent.getStringExtra(Scheduler.EXTRA_TEXT) ?: "",
                )
            }
        }
        WidgetProvider.updateAll(context)
        LiveStatus.update(context)
    }

    /** Builder still idle since the time the reminder was set for? Then nudge (once per idle spell). */
    private fun remind(context: Context, intent: Intent) {
        val tag = intent.getStringExtra(Scheduler.EXTRA_TAG) ?: return
        val village = Store.load(context, tag) ?: return
        val now = System.currentTimeMillis()
        val s = Status.of(village, Slot.BUILDER, now, Store.builderOverride(context, tag)) ?: return
        if (s.free == 0) return
        if (Reminder.freeSinceSec(village, now, Store.builderOverride(context, tag)) !=
            intent.getLongExtra(Scheduler.EXTRA_FREE_SINCE, -1L)
        ) return
        val minutes = Store.reminderMinutes(context)
        val who = if (Store.tags(context).size > 1) " · ${Store.name(context, tag)}" else ""
        Notifier.notify(
            context,
            intent.getIntExtra(Scheduler.EXTRA_CODE, 0),
            if (s.free == 1) "A builder is idle" else "${s.free} builders are idle",
            "Free for $minutes+ min$who. Re-export after starting upgrades to keep this accurate.",
            reminder = true,
        )
    }
}

/** Alarms are wiped on reboot and app updates; restore them from the saved exports. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Refresh.all(context)
    }
}
