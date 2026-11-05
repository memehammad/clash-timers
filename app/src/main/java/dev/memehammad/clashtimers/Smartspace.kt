package dev.memehammad.clashtimers

import android.content.ComponentName
import android.content.Context
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerComplicationUpdateReceiver
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerTargetUpdateReceiver
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import android.graphics.drawable.Icon as AndroidIcon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon as SmartIcon

/**
 * Smartspacer (At a Glance) integration: a builders card plus small chips for builders, the lab
 * and running boosts. Data comes from the currently selected village, like the main screen.
 */
object Smartspace {
    /** Tells Smartspacer to ask for fresh data (after an import, a finished timer, ...). */
    fun notifyAll(context: Context) {
        runCatching {
            SmartspacerTargetProvider.notifyChange(context, BuildersTarget::class.java)
            SmartspacerComplicationProvider.notifyChange(context, BuildersComplication::class.java)
            SmartspacerComplicationProvider.notifyChange(context, LabComplication::class.java)
            SmartspacerComplicationProvider.notifyChange(context, BoostComplication::class.java)
        }
    }

    /** Fits Smartspace's 6-character chips: "2h15", "45m", "3d4h". */
    fun compact(ms: Long): String {
        val s = (ms + 59_999) / 1000
        val d = s / 86400
        val h = (s % 86400) / 3600
        val m = (s % 3600) / 60
        return when {
            d > 0 -> "${d}d${h}h"
            h > 0 -> "${h}h${m.toString().padStart(2, '0')}"
            else -> "${m.coerceAtLeast(1)}m"
        }
    }

    fun icon(context: Context, res: Int) = SmartIcon(AndroidIcon.createWithResource(context, res))

    fun tap(context: Context) = TapAction(pendingIntent = Scheduler.openClash(context))

    /** The current village and its builder status, or null if nothing is imported yet. */
    fun builders(context: Context, now: Long): Pair<Village, SlotStatus>? {
        val v = Store.load(context) ?: return null
        val s = Status.of(v, Slot.BUILDER, now, Store.builderOverride(context, v.tag)) ?: return null
        return v to s
    }
}

/** At a Glance card: "2 builders free" / "All builders busy · next free 18:45". */
class BuildersTarget : SmartspacerTargetProvider() {
    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val c = provideContext()
        Format.configure(c)
        val now = System.currentTimeMillis()
        val (village, s) = Smartspace.builders(c, now) ?: return emptyList()
        val running = village.timers.filter { it.slot == Slot.BUILDER && !it.extra && it.remainingMs(now) > 0 }
            .sortedBy { it.endSec }
        val subtitle = when {
            s.free == 0 && s.nextFreeSec != null -> "Next free ${Format.phrase(s.nextFreeSec * 1000 - now, now)}"
            running.isNotEmpty() -> "${running.first().name} ${Format.phrase(running.first().remainingMs(now), now)}"
            else -> "Time to build something"
        }
        val boosts = Status.activeBoosts(village, now)
        val title = Status.builderTitle(s) + if (boosts.isNotEmpty()) " · ${Status.boostLabel(boosts)}" else ""
        return listOf(
            TargetTemplate.Basic(
                id = "builders_$smartspacerId",
                componentName = ComponentName(c, BuildersTarget::class.java),
                title = Text(title),
                subtitle = Text(subtitle),
                icon = Smartspace.icon(c, R.drawable.ic_hammer),
                onClick = Smartspace.tap(c),
            ).create().apply { canBeDismissed = false },
        )
    }

    override fun getConfig(smartspacerId: String?) = Config(
        label = "Clash builders",
        description = "Free builders and when the next one is done",
        icon = AndroidIcon.createWithResource(provideContext(), R.drawable.ic_hammer),
        refreshPeriodMinutes = 1,
    )

    override fun onDismiss(smartspacerId: String, targetId: String) = false
}

/** Small chip: "3 free" or time until the next builder is free. */
class BuildersComplication : SmartspacerComplicationProvider() {
    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        val c = provideContext()
        val now = System.currentTimeMillis()
        val (_, s) = Smartspace.builders(c, now) ?: return emptyList()
        val text = if (s.free > 0) "${s.free} free" else s.nextFreeSec?.let { Smartspace.compact(it * 1000 - now) } ?: "busy"
        return listOf(
            ComplicationTemplate.Basic(
                id = "builders_$smartspacerId",
                icon = Smartspace.icon(c, R.drawable.ic_hammer),
                content = Text(text),
                onClick = Smartspace.tap(c),
            ).create(),
        )
    }

    override fun getConfig(smartspacerId: String?) = Config(
        label = "Clash builders",
        description = "Free builders, or time until the next one",
        icon = AndroidIcon.createWithResource(provideContext(), R.drawable.ic_hammer),
        refreshPeriodMinutes = 1,
    )
}

/** Small chip: lab "free" or time left on the current research. */
class LabComplication : SmartspacerComplicationProvider() {
    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        val c = provideContext()
        val now = System.currentTimeMillis()
        val v = Store.load(c) ?: return emptyList()
        val s = Status.of(v, Slot.LAB, now) ?: return emptyList()
        val text = if (s.free > 0) "free" else s.nextFreeSec?.let { Smartspace.compact(it * 1000 - now) } ?: "busy"
        return listOf(
            ComplicationTemplate.Basic(
                id = "lab_$smartspacerId",
                icon = Smartspace.icon(c, R.drawable.ic_flask),
                content = Text(text),
                onClick = Smartspace.tap(c),
            ).create(),
        )
    }

    override fun getConfig(smartspacerId: String?) = Config(
        label = "Clash laboratory",
        description = "Lab free, or time left on the research",
        icon = AndroidIcon.createWithResource(provideContext(), R.drawable.ic_flask),
        refreshPeriodMinutes = 1,
    )
}

/** Small chip, only while a potion, snack or Clock Tower boost is running: its time left. */
class BoostComplication : SmartspacerComplicationProvider() {
    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        val c = provideContext()
        val now = System.currentTimeMillis()
        val v = Store.load(c) ?: return emptyList()
        val boost = Status.activeBoosts(v, now).firstOrNull() ?: return emptyList()
        return listOf(
            ComplicationTemplate.Basic(
                id = "boost_$smartspacerId",
                icon = Smartspace.icon(c, R.drawable.ic_bolt),
                content = Text(Smartspace.compact(boost.remainingMs(now))),
                onClick = Smartspace.tap(c),
            ).create(),
        )
    }

    override fun getConfig(smartspacerId: String?) = Config(
        label = "Clash boost",
        description = "Time left on a running potion, snack or Clock Tower (hidden otherwise)",
        icon = AndroidIcon.createWithResource(provideContext(), R.drawable.ic_bolt),
        refreshPeriodMinutes = 1,
    )
}

/** Smartspacer's periodic refresh: just ask it to re-read our providers (data is local). */
class SmartspaceTargetRefresh : SmartspacerTargetUpdateReceiver() {
    override fun onRequestSmartspaceTargetUpdate(context: Context, requestTargets: List<RequestTarget>) =
        Smartspace.notifyAll(context)
}

class SmartspaceComplicationRefresh : SmartspacerComplicationUpdateReceiver() {
    override fun onRequestSmartspaceComplicationUpdate(context: Context, requestComplications: List<RequestComplication>) =
        Smartspace.notifyAll(context)
}
