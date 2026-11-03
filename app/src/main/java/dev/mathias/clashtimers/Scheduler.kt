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

/** One alarm per running timer; fires CompletionReceiver at the finish time. */
object Scheduler {
    private const val ACTION_DONE = "dev.mathias.clashtimers.TIMER_DONE"
    const val EXTRA_TITLE = "title"
    const val EXTRA_TEXT = "text"
    const val EXTRA_CODE = "code"

    fun reschedule(context: Context, village: Village?) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return

        for (stored in Store.alarmCodes(context)) {
            val code = stored.toIntOrNull() ?: continue
            existing(context, code)?.let {
                am.cancel(it)
                it.cancel()
            }
        }

        val now = System.currentTimeMillis()
        val codes = mutableSetOf<String>()
        village?.timers?.forEach { t ->
            val at = t.endSec * 1000
            if (at <= now) return@forEach
            val code = t.key.hashCode()
            val pi = create(context, code, t)
            try {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                }
            } catch (e: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
            codes += code.toString()
        }
        Store.setAlarmCodes(context, codes)
    }

    private fun baseIntent(context: Context, code: Int): Intent =
        Intent(context, CompletionReceiver::class.java)
            .setAction(ACTION_DONE)
            .setData(Uri.parse("clashtimers://timer/$code"))

    private fun existing(context: Context, code: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context, code, baseIntent(context, code),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )

    private fun create(context: Context, code: Int, t: Timer): PendingIntent {
        val intent = baseIntent(context, code)
            .putExtra(EXTRA_TITLE, t.doneTitle)
            .putExtra(EXTRA_TEXT, t.doneText)
            .putExtra(EXTRA_CODE, code)
        return PendingIntent.getBroadcast(
            context, code, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

object Notifier {
    private const val CHANNEL = "done"

    @SuppressLint("MissingPermission")
    fun notify(context: Context, id: Int, title: String, text: String) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_done), NotificationManager.IMPORTANCE_DEFAULT),
        )
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val open = PendingIntent.getActivity(
            context, 3, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()
        nm.notify(id, notification)
    }
}

class CompletionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(Scheduler.EXTRA_TITLE)
        if (title != null) {
            Notifier.notify(
                context,
                intent.getIntExtra(Scheduler.EXTRA_CODE, 0),
                title,
                intent.getStringExtra(Scheduler.EXTRA_TEXT) ?: "",
            )
        }
        WidgetProvider.updateAll(context)
    }
}

/** Alarms are wiped on reboot and app updates; restore them from the saved export. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Scheduler.reschedule(context, Store.load(context))
        WidgetProvider.updateAll(context)
    }
}
