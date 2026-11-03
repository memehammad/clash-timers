package dev.mathias.clashtimers

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * Optional ongoing notification: free builders or a countdown to the next one, for the village
 * selected in the app. Requests promotion to a Live Update, so Android 16 shows it as a status bar
 * chip and prominently on the lock screen. Refreshed on import and by every timer alarm.
 */
object LiveStatus {
    private const val CHANNEL = "live"
    private const val ID = 1
    private const val EXTRA_PROMOTED = "android.requestPromotedOngoing"

    @SuppressLint("MissingPermission")
    fun update(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val village = Store.load(context)
        if (!Store.liveUpdate(context) || village == null || !Notifier.canNotify(context)) {
            nm.cancel(ID)
            return
        }
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_live), NotificationManager.IMPORTANCE_LOW),
        )

        val now = System.currentTimeMillis()
        val s = Status.of(village, Slot.BUILDER, now, Store.builderOverride(context, village.tag))
        if (s == null) {
            nm.cancel(ID)
            return
        }
        val boosts = Status.activeBoosts(village, now)
        val stale = Stale.isStale(village, now)

        val b = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setContentIntent(Notifier.openApp(context))

        val next = s.nextFreeSec
        // Status bar chip: short text when free; when busy Android shows the countdown itself.
        val chip: String?
        if (s.free > 0) {
            b.setContentTitle(Status.builderTitle(s))
            b.setContentText("${s.busy} of ${s.total} working")
            chip = "${s.free} free"
        } else if (next != null) {
            b.setContentTitle("All builders busy")
            b.setContentText("Next builder free in")
            b.setWhen(next * 1000)
            b.setShowWhen(true)
            b.setUsesChronometer(true)
            b.setChronometerCountDown(true)
            chip = null
        } else {
            nm.cancel(ID)
            return
        }
        val sub = listOfNotNull(
            boosts.firstOrNull()?.let { Status.boostLabel(boosts) },
            if (stale) Stale.label(village, now) else null,
            if (Store.tags(context).size > 1) Store.name(context, village.tag) else null,
        )
        if (sub.isNotEmpty()) b.setSubText(sub.joinToString(" · "))

        b.extras.putBoolean(EXTRA_PROMOTED, true)
        // Android 16 status bar chip text (API 36), set by reflection to keep building with SDK 35.
        if (chip != null) {
            try {
                Notification.Builder::class.java.getMethod("setShortCriticalText", String::class.java).invoke(b, chip)
            } catch (_: Throwable) {
            }
        }
        nm.notify(ID, b.build())
    }
}
