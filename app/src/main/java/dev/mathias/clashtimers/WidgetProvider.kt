package dev.mathias.clashtimers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = build(context)
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, views)
    }

    companion object {
        private const val DAY_MS = 86_400_000L

        /** root, label, value, chronometer for each status chip. */
        private val CHIPS = arrayOf(
            intArrayOf(R.id.c1, R.id.c1_label, R.id.c1_value, R.id.c1_chrono),
            intArrayOf(R.id.c2, R.id.c2_label, R.id.c2_value, R.id.c2_chrono),
            intArrayOf(R.id.c3, R.id.c3_label, R.id.c3_value, R.id.c3_chrono),
            intArrayOf(R.id.c4, R.id.c4_label, R.id.c4_value, R.id.c4_chrono),
        )

        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, WidgetProvider::class.java))
            if (ids.isEmpty()) return
            val views = build(context)
            for (id in ids) mgr.updateAppWidget(id, views)
        }

        private fun build(context: Context): RemoteViews {
            val village = Store.load(context)
            val override = Store.builderOverride(context)
            val now = System.currentTimeMillis()

            val pkg = context.packageName
            val tiny = RemoteViews(pkg, R.layout.widget_tiny)
            val small = RemoteViews(pkg, R.layout.widget_small)
            val square = RemoteViews(pkg, R.layout.widget_square)
            val narrow = RemoteViews(pkg, R.layout.widget_narrow)
            val compact = RemoteViews(pkg, R.layout.widget_compact)
            val full = RemoteViews(pkg, R.layout.widget)

            for (v in listOf(tiny, small, square, narrow)) fillMini(context, v, village, override, now)
            for (v in listOf(compact, full)) fillStatus(context, v, village, override, now)
            fillList(context, narrow, village, now)
            fillList(context, full, village, now)

            // The launcher shows the largest layout that fits the widget's current size (in dp).
            return RemoteViews(
                mapOf(
                    SizeF(40f, 40f) to tiny,       // 1x1
                    SizeF(100f, 40f) to small,     // 2x1 – 4x1
                    SizeF(100f, 100f) to square,   // 2x2
                    SizeF(180f, 100f) to compact,  // 3x2, 4x2
                    SizeF(100f, 200f) to narrow,   // 2x3, 2x4
                    SizeF(180f, 200f) to full,     // 3x3 and up
                ),
            )
        }

        /** Small layouts (1x1, 2x1, 2x2, narrow header): badge, short title, countdown, boost dot. */
        private fun fillMini(context: Context, v: RemoteViews, village: Village?, override: Int, now: Long) {
            val open = openApp(context)
            v.setOnClickPendingIntent(android.R.id.background, open)
            v.setOnClickPendingIntent(R.id.m_block, open)

            val builders = village?.let { Status.of(it, Slot.BUILDER, now, override) }
            if (village == null || builders == null) {
                v.setTextViewText(R.id.m_count, "?")
                v.setTextViewText(R.id.m_title, context.getString(R.string.no_village))
                v.setTextViewText(R.id.m_sub, context.getString(R.string.tap_to_import_short))
                return
            }

            val boosts = Status.activeBoosts(village, now)
            v.setInt(
                android.R.id.background, "setBackgroundResource",
                if (boosts.isEmpty()) R.drawable.widget_bg else R.drawable.widget_bg_boost,
            )
            v.setViewVisibility(R.id.m_bolt, if (boosts.isEmpty()) View.GONE else View.VISIBLE)

            val free = builders.free > 0
            v.setTextViewText(R.id.m_count, builders.free.toString())
            v.setViewVisibility(R.id.m_badge, if (free) View.GONE else View.VISIBLE)
            v.setViewVisibility(R.id.m_spin, if (free) View.VISIBLE else View.GONE)
            v.setColor(R.id.m_count, "setTextColor", if (free) R.color.w_on_badge_free else R.color.w_on_badge_busy)
            v.setInt(R.id.m_block, "setBackgroundResource", if (free) R.drawable.block_free_bg else R.drawable.row_single)
            v.setColor(R.id.m_title, "setTextColor", if (free) R.color.w_on_free else R.color.w_text)

            if (free) {
                v.setTextViewText(R.id.m_title, if (builders.free == 1) "1 builder free" else "${builders.free} builders free")
                v.setTextViewText(R.id.m_sub, "${builders.busy} of ${builders.total} working")
            } else {
                v.setTextViewText(R.id.m_title, "All busy")
                val next = builders.nextFreeSec
                val left = (next ?: 0L) * 1000 - now
                when {
                    next == null -> v.setTextViewText(R.id.m_sub, "")
                    left < DAY_MS -> {
                        v.setTextViewText(R.id.m_sub, "Free in ")
                        showChrono(v, R.id.m_chrono, left)
                    }
                    else -> v.setTextViewText(R.id.m_sub, "Free in ${Format.countdown(left)}")
                }
            }
        }

        private fun openApp(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context, 1, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun showChrono(v: RemoteViews, id: Int, leftMs: Long) {
            v.setChronometer(id, SystemClock.elapsedRealtime() + leftMs, null, true)
            v.setChronometerCountDown(id, true)
            v.setViewVisibility(id, View.VISIBLE)
        }

        private fun fillStatus(context: Context, v: RemoteViews, village: Village?, override: Int, now: Long) {
            val open = openApp(context)
            v.setOnClickPendingIntent(R.id.s_block, open)
            v.setOnClickPendingIntent(R.id.s_chips, open)
            v.setOnClickPendingIntent(R.id.s_boost, open)

            val builders = village?.let { Status.of(it, Slot.BUILDER, now, override) }
            if (village == null || builders == null) {
                v.setTextViewText(R.id.s_count, "?")
                v.setTextViewText(R.id.s_title, context.getString(R.string.no_village))
                v.setTextViewText(R.id.s_sub, context.getString(R.string.tap_to_import))
                return
            }

            // Boost theme: tinted background + pulsing banner while a potion/snack runs.
            // The boost's own alarm refreshes the widget when it ends.
            val boosts = Status.activeBoosts(village, now)
            v.setInt(
                android.R.id.background, "setBackgroundResource",
                if (boosts.isEmpty()) R.drawable.widget_bg else R.drawable.widget_bg_boost,
            )
            if (boosts.isNotEmpty()) {
                v.setViewVisibility(R.id.s_boost, View.VISIBLE)
                v.setTextViewText(R.id.s_boost_text, Status.boostLabel(boosts))
                v.setChronometer(R.id.s_boost_chrono, SystemClock.elapsedRealtime() + boosts.first().remainingMs(now), null, true)
                v.setChronometerCountDown(R.id.s_boost_chrono, true)
            } else {
                v.setViewVisibility(R.id.s_boost, View.GONE)
            }

            // Builder block: tinted and highlighted whenever a builder is free.
            val free = builders.free > 0
            v.setTextViewText(R.id.s_count, builders.free.toString())
            v.setTextViewText(R.id.s_title, Status.builderTitle(builders))
            v.setTextViewText(R.id.s_sub, Status.builderSubtitle(builders))
            v.setViewVisibility(R.id.s_badge, if (free) View.GONE else View.VISIBLE)
            v.setViewVisibility(R.id.s_spin, if (free) View.VISIBLE else View.GONE)
            v.setInt(R.id.s_block, "setBackgroundResource", if (free) R.drawable.block_free_bg else R.drawable.row_single)
            v.setColor(R.id.s_count, "setTextColor", if (free) R.color.w_on_badge_free else R.color.w_on_badge_busy)
            v.setColor(R.id.s_title, "setTextColor", if (free) R.color.w_on_free else R.color.w_text)
            v.setColor(R.id.s_sub, "setTextColor", if (free) R.color.w_on_free2 else R.color.w_text2)
            val next = builders.nextFreeSec
            if (!free && next != null) {
                val left = next * 1000 - now
                if (left < DAY_MS) showChrono(v, R.id.s_chrono, left)
                else v.setTextViewText(R.id.s_sub, "Next free in ${Format.countdown(left)}")
            }

            // Chips: Lab, Pets, Builder Base, Star Lab (only those unlocked).
            val chips = Status.chips.mapNotNull { (slot, _) ->
                Status.of(village, slot, now)?.let { Status.shortLabel(slot) to it }
            }.take(CHIPS.size)
            v.setViewVisibility(R.id.s_chips, if (chips.isEmpty()) View.GONE else View.VISIBLE)
            chips.forEachIndexed { i, (label, s) ->
                val (root, labelId, valueId, chronoId) = CHIPS[i]
                v.setViewVisibility(root, View.VISIBLE)
                v.setTextViewText(labelId, label)
                if (s.free > 0) {
                    v.setTextViewText(valueId, if (s.total > 1) "${s.free} free" else "Idle")
                    v.setInt(root, "setBackgroundResource", R.drawable.chip_free_bg)
                    v.setColor(labelId, "setTextColor", R.color.w_on_chip_free)
                    v.setColor(valueId, "setTextColor", R.color.w_on_chip_free)
                } else {
                    val left = (s.nextFreeSec ?: 0L) * 1000 - now
                    if (left in 1 until DAY_MS) {
                        v.setViewVisibility(valueId, View.GONE)
                        showChrono(v, chronoId, left)
                    } else {
                        v.setTextViewText(valueId, Format.countdown(left))
                    }
                }
            }
        }

        private fun fillList(context: Context, root: RemoteViews, village: Village?, now: Long) {
            val pkg = context.packageName
            root.setOnClickPendingIntent(R.id.w_empty, openApp(context))
            root.setPendingIntentTemplate(
                R.id.w_list,
                PendingIntent.getActivity(
                    context, 2, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            root.setTextViewText(
                R.id.w_empty,
                context.getString(if (village == null) R.string.widget_empty else R.string.nothing_running),
            )

            val items = RemoteViews.RemoteCollectionItems.Builder()
                .setHasStableIds(true)
                .setViewTypeCount(2 + WaveRows.layouts.size)
            village?.timers.orEmpty().groupBy { it.section }.forEach { (section, list) ->
                items.addItem(Long.MIN_VALUE + section.ordinal, header(pkg, section))
                list.forEachIndexed { i, t ->
                    items.addItem(t.key.hashCode().toLong(), row(pkg, t, now, rowBackground(i, list.size)))
                }
            }
            root.setRemoteAdapter(R.id.w_list, items.build())
            root.setEmptyView(R.id.w_list, R.id.w_empty)
        }

        /** Grouped list: large outer corners, small inner corners. */
        private fun rowBackground(index: Int, count: Int): Int = when {
            count == 1 -> R.drawable.row_single
            index == 0 -> R.drawable.row_top
            index == count - 1 -> R.drawable.row_bottom
            else -> R.drawable.row_mid
        }

        private fun header(pkg: String, section: Section): RemoteViews =
            RemoteViews(pkg, R.layout.widget_header).apply {
                setTextViewText(R.id.h_title, section.title)
                setOnClickFillInIntent(R.id.h_title, Intent())
            }

        private fun row(pkg: String, t: Timer, now: Long, background: Int): RemoteViews {
            val left = t.remainingMs(now)
            val p = t.progress(now)
            // Rows with progress use a pre-generated layout whose wave ends at that progress.
            val layout = if (p != null && left > 0) WaveRows.forProgress(p) else R.layout.widget_row
            val v = RemoteViews(pkg, layout)
            v.setInt(R.id.r_root, "setBackgroundResource", background)
            v.setTextViewText(R.id.r_name, t.name)
            v.setTextViewText(R.id.r_detail, t.detail)

            when {
                left <= 0 -> v.setViewVisibility(R.id.r_done, View.VISIBLE)
                left < DAY_MS -> showChrono(v, R.id.r_chrono, left)
                else -> {
                    v.setTextViewText(R.id.r_time, Format.countdown(left))
                    v.setViewVisibility(R.id.r_time, View.VISIBLE)
                }
            }

            val (shape, icon) = Icons.of(t.section)
            v.setImageViewResource(R.id.r_shape, shape)
            v.setImageViewResource(R.id.r_icon, icon)

            v.setOnClickFillInIntent(R.id.r_root, Intent())
            return v
        }
    }
}
