package dev.memehammad.clashtimers

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
        for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, build(context, id))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) Store.setWidgetTag(context, id, null)
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
            for (id in ids) mgr.updateAppWidget(id, build(context, id))
            Smartspace.notifyAll(context)
        }

        fun update(context: Context, widgetId: Int) {
            AppWidgetManager.getInstance(context).updateAppWidget(widgetId, build(context, widgetId))
        }

        private fun build(context: Context, widgetId: Int): RemoteViews = RemoteViews(layouts(context, widgetId))

        /** Every size's layout, smallest first (the launcher shows the largest that fits). */
        internal fun layouts(context: Context, widgetId: Int): Map<SizeF, RemoteViews> {
            val village = Store.loadForWidget(context, widgetId)
            val override = village?.let { Store.builderOverride(context, it.tag) } ?: 0
            val opts = Store.widgetOptions(context, widgetId)
            Format.configure(context)
            val now = System.currentTimeMillis()

            val pkg = context.packageName
            val tiny = RemoteViews(pkg, R.layout.widget_tiny)
            val small = RemoteViews(pkg, R.layout.widget_small)
            val square = RemoteViews(pkg, R.layout.widget_square)
            val narrow = RemoteViews(pkg, R.layout.widget_narrow)
            val compact = RemoteViews(pkg, R.layout.widget_compact)
            val full = RemoteViews(pkg, R.layout.widget)

            fillMini(context, tiny, village, override, now, framed = false, opts)
            fillMini(context, square, village, override, now, framed = false, opts)
            fillMini(context, small, village, override, now, framed = true, opts)
            fillMini(context, narrow, village, override, now, framed = true, opts)
            for (v in listOf(compact, full)) fillStatus(context, v, village, override, now, opts)
            fillList(context, narrow, village, now, narrowBars = true, opts)
            fillList(context, full, village, now, narrowBars = false, opts)

            // The launcher shows the largest layout that fits the widget's current size (in dp).
            // Thresholds assume typical phone cells of ~90-100 dp wide and ~100-120 dp tall.
            return mapOf(
                SizeF(40f, 40f) to tiny,       // 1x1, 1x2 and other single columns
                SizeF(150f, 40f) to small,     // 2x1 – 4x1
                SizeF(150f, 150f) to square,   // 2x2
                SizeF(260f, 150f) to compact,  // 3x2, 4x2
                SizeF(150f, 300f) to narrow,   // 2x3, 2x4
                SizeF(260f, 300f) to full,     // 3x3 and up
            )
        }

        /** Small layouts (1x1, 2x1, 2x2, narrow header): badge, short title, countdown, boost dot. */
        /** [framed] = false for 1x1/2x2, which show the shape on the wallpaper with no widget background. */
        private fun fillMini(
            context: Context, v: RemoteViews, village: Village?, override: Int, now: Long, framed: Boolean,
            opts: WidgetOptions,
        ) {
            val open = openApp(context)
            v.setOnClickPendingIntent(android.R.id.background, open)
            v.setOnClickPendingIntent(R.id.m_block, open)

            // The launcher re-applies updates onto the views it already shows, so every property
            // that any state changes is reset here first; otherwise old state lingers.
            if (framed) v.setInt(android.R.id.background, "setBackgroundResource", R.drawable.widget_bg)
            v.setViewVisibility(R.id.m_bolt, View.GONE)
            v.setViewVisibility(R.id.m_badge, View.VISIBLE)
            v.setImageViewResource(R.id.m_badge, R.drawable.cookie_busy)
            v.setViewVisibility(R.id.m_spin, View.GONE)
            v.setViewVisibility(R.id.m_spin_boost, View.GONE)
            v.setViewVisibility(R.id.m_chrono, View.GONE)
            for (id in intArrayOf(R.id.m_badge, R.id.m_spin, R.id.m_spin_boost, R.id.m_count)) v.setFloat(id, "setAlpha", 1f)
            v.setColor(R.id.m_count, "setTextColor", R.color.w_on_badge_busy)
            v.setInt(R.id.m_block, "setBackgroundResource", R.drawable.row_single)
            v.setColor(R.id.m_title, "setTextColor", R.color.w_text)
            v.setInt(R.id.m_pill, "setBackgroundResource", R.drawable.pill_bg)
            v.setColor(R.id.m_sub, "setTextColor", if (framed) R.color.w_text2 else R.color.w_text)

            val builders = village?.let { Status.of(it, Slot.BUILDER, now, override) }
            if (village == null || builders == null) {
                v.setTextViewText(R.id.m_count, "?")
                v.setTextViewText(R.id.m_title, context.getString(R.string.no_village))
                v.setTextViewText(R.id.m_sub, context.getString(R.string.tap_to_import_short))
                return
            }

            val boosts = Status.activeBoosts(village, now).takeIf { opts.has(WidgetOptions.BOOST) }.orEmpty()
            if (framed) {
                v.setInt(
                    android.R.id.background, "setBackgroundResource",
                    if (boosts.isEmpty()) R.drawable.widget_bg else R.drawable.widget_bg_boost,
                )
            }
            val boosted = boosts.isNotEmpty()
            v.setViewVisibility(R.id.m_bolt, if (boosted) View.VISIBLE else View.GONE)

            val free = builders.free > 0
            v.setTextViewText(R.id.m_count, builders.free.toString())
            // 1x1/2x2 show a boost by colouring the badge itself (they have no room for an extra dot).
            val boostBadge = boosted && !framed
            v.setViewVisibility(R.id.m_badge, if (free) View.GONE else View.VISIBLE)
            v.setImageViewResource(R.id.m_badge, if (boostBadge) R.drawable.cookie_boost else R.drawable.cookie_busy)
            v.setViewVisibility(R.id.m_spin, if (free && !boostBadge) View.VISIBLE else View.GONE)
            v.setViewVisibility(R.id.m_spin_boost, if (free && boostBadge) View.VISIBLE else View.GONE)
            v.setColor(
                R.id.m_count, "setTextColor",
                when {
                    boostBadge -> R.color.w_on_boost
                    free -> R.color.w_on_badge_free
                    else -> R.color.w_on_badge_busy
                },
            )
            v.setInt(R.id.m_block, "setBackgroundResource", if (free) R.drawable.block_free_bg else R.drawable.row_single)
            v.setColor(R.id.m_title, "setTextColor", if (free) R.color.w_on_free else R.color.w_text)

            // 2x2 status pill (absent in other layouts; RemoteViews skips missing views).
            v.setInt(R.id.m_pill, "setBackgroundResource", if (free) R.drawable.pill_free_bg else R.drawable.pill_bg)
            v.setColor(R.id.m_sub, "setTextColor", if (free && !framed) R.color.w_on_free else if (framed) R.color.w_text2 else R.color.w_text)

            if (free) {
                val label = trq(R.plurals.p_builders_free, builders.free)
                v.setTextViewText(R.id.m_title, label)
                v.setTextViewText(R.id.m_sub, if (framed) tr(R.string.n_of_m_working, builders.busy, builders.total) else label)
            } else {
                v.setTextViewText(R.id.m_title, tr(R.string.all_busy_short))
                val next = builders.nextFreeSec
                val left = (next ?: 0L) * 1000 - now
                when {
                    next == null -> v.setTextViewText(R.id.m_sub, "")
                    left < DAY_MS && !Format.clock -> {
                        v.setTextViewText(R.id.m_sub, tr(R.string.free_in_prefix))
                        showChrono(v, R.id.m_chrono, left)
                    }
                    framed -> v.setTextViewText(R.id.m_sub, tr(R.string.free_x, Format.phrase(left, now)))
                    else -> v.setTextViewText(R.id.m_sub, Format.left(left, now, short = true))
                }
            }

            // Day-old export: dim the badge and say so instead of the (likely wrong) countdown.
            if (Stale.isStale(village, now) && opts.has(WidgetOptions.STALE)) {
                for (id in intArrayOf(R.id.m_badge, R.id.m_spin, R.id.m_spin_boost, R.id.m_count)) {
                    v.setFloat(id, "setAlpha", 0.45f)
                }
                v.setViewVisibility(R.id.m_chrono, View.GONE)
                v.setTextViewText(R.id.m_sub, if (framed) Stale.label(village, now) else tr(R.string.re_export))
                v.setInt(R.id.m_pill, "setBackgroundResource", R.drawable.pill_stale_bg)
                v.setColor(R.id.m_sub, "setTextColor", if (framed) R.color.w_text2 else R.color.w_on_stale)
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

        /** The corner button: imports the export on the clipboard without opening the app. */
        private fun importNow(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context, 2, Intent(context, ImportActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun fillStatus(context: Context, v: RemoteViews, village: Village?, override: Int, now: Long, opts: WidgetOptions) {
            val open = openApp(context)
            v.setViewVisibility(R.id.s_import, if (opts.showImport(context)) View.VISIBLE else View.GONE)
            v.setOnClickPendingIntent(R.id.s_import, importNow(context))
            v.setOnClickPendingIntent(R.id.s_block, open)
            v.setOnClickPendingIntent(R.id.s_chips, open)
            v.setOnClickPendingIntent(R.id.s_boost, open)

            // Reset everything a state can change (see fillMini).
            v.setInt(android.R.id.background, "setBackgroundResource", R.drawable.widget_bg)
            v.setViewVisibility(R.id.s_boost, View.GONE)
            v.setViewVisibility(R.id.s_badge, View.VISIBLE)
            v.setViewVisibility(R.id.s_spin, View.GONE)
            v.setViewVisibility(R.id.s_chrono, View.GONE)
            v.setInt(R.id.s_block, "setBackgroundResource", R.drawable.row_single)
            v.setFloat(R.id.s_block, "setAlpha", 1f)
            v.setColor(R.id.s_count, "setTextColor", R.color.w_on_badge_busy)
            v.setColor(R.id.s_title, "setTextColor", R.color.w_text)
            v.setColor(R.id.s_sub, "setTextColor", R.color.w_text2)
            v.setViewVisibility(R.id.s_chips, View.GONE)
            for ((root, labelId, valueId, chronoId) in CHIPS) {
                v.setViewVisibility(root, View.GONE)
                v.setInt(root, "setBackgroundResource", R.drawable.chip_bg)
                v.setColor(labelId, "setTextColor", R.color.w_text2)
                v.setColor(valueId, "setTextColor", R.color.w_text)
                v.setViewVisibility(valueId, View.VISIBLE)
                v.setViewVisibility(chronoId, View.GONE)
            }
            v.setViewVisibility(R.id.s_heroes, View.GONE)
            v.setViewVisibility(R.id.s_stale, View.GONE)

            val builders = village?.let { Status.of(it, Slot.BUILDER, now, override) }
            if (village == null || builders == null) {
                v.setTextViewText(R.id.s_count, "?")
                v.setTextViewText(R.id.s_title, context.getString(R.string.no_village))
                v.setTextViewText(R.id.s_sub, context.getString(R.string.tap_to_import))
                return
            }

            // Boost theme: tinted background + pulsing banner while a potion/snack runs.
            // The boost's own alarm refreshes the widget when it ends.
            val boosts = Status.activeBoosts(village, now).takeIf { opts.has(WidgetOptions.BOOST) }.orEmpty()
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
                if (left < DAY_MS && !Format.clock) showChrono(v, R.id.s_chrono, left)
                else v.setTextViewText(R.id.s_sub, tr(R.string.next_free_x, Format.phrase(left, now)))
            }

            // Chips: Lab, Pets, Builder Base, Star Lab (only those unlocked).
            val chips = Status.chips.mapNotNull { (slot, _) ->
                Status.of(village, slot, now)?.let { Status.shortLabel(slot) to it }
            }.take(CHIPS.size).takeIf { opts.has(WidgetOptions.CHIPS) }.orEmpty()
            v.setViewVisibility(R.id.s_chips, if (chips.isEmpty()) View.GONE else View.VISIBLE)
            chips.forEachIndexed { i, (label, s) ->
                val (root, labelId, valueId, chronoId) = CHIPS[i]
                v.setViewVisibility(root, View.VISIBLE)
                v.setTextViewText(labelId, label)
                if (s.free > 0) {
                    v.setTextViewText(valueId, if (s.total > 1) trq(R.plurals.p_free, s.free) else tr(R.string.t_idle))
                    v.setInt(root, "setBackgroundResource", R.drawable.chip_free_bg)
                    v.setColor(labelId, "setTextColor", R.color.w_on_chip_free)
                    v.setColor(valueId, "setTextColor", R.color.w_on_chip_free)
                } else {
                    val left = (s.nextFreeSec ?: 0L) * 1000 - now
                    if (left in 1 until DAY_MS && !Format.clock) {
                        v.setViewVisibility(valueId, View.GONE)
                        showChrono(v, chronoId, left)
                    } else {
                        v.setTextViewText(valueId, Format.left(left, now, short = true))
                    }
                }
            }

            // Heroes: who can attack. Refreshed by the hero's own upgrade alarm.
            val heroes = HeroStatus.summary(village, now).takeIf { opts.has(WidgetOptions.HEROES) }
            v.setViewVisibility(R.id.s_heroes, if (heroes == null) View.GONE else View.VISIBLE)
            heroes?.let { v.setTextViewText(R.id.s_heroes, it) }

            // Day-old export: say so, and dim the builder block since its numbers are likely off.
            val stale = Stale.isStale(village, now) && opts.has(WidgetOptions.STALE)
            v.setViewVisibility(R.id.s_stale, if (stale) View.VISIBLE else View.GONE)
            if (stale) {
                v.setTextViewText(R.id.s_stale, tr(R.string.stale_copy, Stale.label(village, now)))
                v.setFloat(R.id.s_block, "setAlpha", 0.5f)
            }
        }

        private fun fillList(
            context: Context, root: RemoteViews, village: Village?, now: Long, narrowBars: Boolean, opts: WidgetOptions,
        ) {
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
                .setViewTypeCount(3 + WaveRows.COUNT)
            val sections = opts.sections
            village?.timers.orEmpty().filter { it.section in sections }.groupBy { it.section }.forEach { (section, list) ->
                items.addItem(Long.MIN_VALUE + section.ordinal, header(pkg, section))
                list.forEachIndexed { i, t ->
                    items.addItem(t.key.hashCode().toLong(), row(pkg, t, now, rowBackground(i, list.size), narrowBars, opts.has(WidgetOptions.PROGRESS)))
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

        private fun row(pkg: String, t: Timer, now: Long, background: Int, narrowBar: Boolean, progress: Boolean): RemoteViews {
            val left = t.remainingMs(now)
            val p = t.progress(now)
            // Rows with progress use a pre-generated layout whose wave ends at that progress.
            val style = ProgressStyle.current
            val showBar = progress && style != ProgressStyle.OFF && p != null && left > 0
            val layout = when {
                !showBar -> R.layout.widget_row
                style == ProgressStyle.STRAIGHT -> R.layout.widget_row_bar
                else -> WaveRows.forProgress(p!!, narrowBar)
            }
            val v = RemoteViews(pkg, layout)
            v.setInt(R.id.r_root, "setBackgroundResource", background)
            v.setTextViewText(R.id.r_name, t.name)
            v.setTextViewText(R.id.r_detail, t.detail)

            // Rows can be reused for other timers: reset which time view shows.
            for (id in intArrayOf(R.id.r_done, R.id.r_chrono, R.id.r_time)) v.setViewVisibility(id, View.GONE)
            when {
                left <= 0 -> v.setViewVisibility(R.id.r_done, View.VISIBLE)
                left < DAY_MS && !Format.clock -> showChrono(v, R.id.r_chrono, left)
                else -> {
                    v.setTextViewText(R.id.r_time, Format.left(left, now))
                    v.setViewVisibility(R.id.r_time, View.VISIBLE)
                }
            }

            if (showBar && style == ProgressStyle.STRAIGHT) {
                v.setProgressBar(R.id.r_progress, 1000, (p!! * 1000).toInt(), false)
            }
            val (shape, icon) = Icons.of(t.section)
            v.setImageViewResource(R.id.r_shape, shape)
            v.setImageViewResource(R.id.r_icon, icon)

            v.setOnClickFillInIntent(R.id.r_root, Intent())
            return v
        }
    }
}
