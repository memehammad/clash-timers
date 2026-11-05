package dev.memehammad.clashtimers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.UiSurface
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerComplicationUpdateReceiver
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerTargetUpdateReceiver
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import org.json.JSONObject
import android.graphics.drawable.Icon as AndroidIcon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon as SmartIcon

/** What one Smartspacer card or chip shows. Stored per Smartspacer ID. */
data class SmartOptions(
    /** Village tag, or "" to follow the app's selection. */
    val village: String = "",
    /** builder, lab, pet, bb, starlab or boost. */
    val what: String = "builder",
    /** always, free (only when a worker is free), busy (only while all are busy), soon (something finishes within the hour). */
    val show: String = "always",
    /** Card subtitle / chip text: next (what finishes next, when) or count (free/busy count). */
    val text: String = "next",
    /** Card: mention a running potion or Clock Tower in the title. */
    val boostInTitle: Boolean = true,
    /** Card: also show on the lock screen. */
    val lockScreen: Boolean = true,
) {
    fun toJson(): String = JSONObject().put("village", village).put("what", what).put("show", show).put("text", text)
        .put("boostInTitle", boostInTitle).put("lockScreen", lockScreen).toString()

    companion object {
        val WHAT = listOf(
            "builder" to "Builders", "lab" to "Laboratory", "pet" to "Pet House",
            "bb" to "Builder Base builders", "starlab" to "Star Laboratory", "boost" to "Active boost",
        )
        val SHOW = listOf(
            "always" to "Always", "free" to "Only when one is free", "busy" to "Only while all are busy",
            "soon" to "Only when something finishes within the hour",
        )

        fun fromJson(s: String?, default: SmartOptions): SmartOptions {
            val o = s?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return default
            return SmartOptions(
                o.optString("village", default.village), o.optString("what", default.what),
                o.optString("show", default.show), o.optString("text", default.text),
                o.optBoolean("boostInTitle", default.boostInTitle), o.optBoolean("lockScreen", default.lockScreen),
            )
        }
    }
}

/**
 * Smartspacer (At a Glance) integration: a card plus small chips, each configurable in
 * Smartspacer (what to show, which village, when to show it).
 */
object Smartspace {
    private fun prefs(c: Context) = c.getSharedPreferences("smartspace", Context.MODE_PRIVATE)

    fun options(c: Context, id: String, default: SmartOptions): SmartOptions =
        SmartOptions.fromJson(prefs(c).getString(id, null), default)

    fun setOptions(c: Context, id: String, o: SmartOptions) {
        prefs(c).edit().putString(id, o.toJson()).apply()
        notifyAll(c)
    }

    fun remove(c: Context, id: String) = prefs(c).edit().remove(id).apply()

    /** Tells Smartspacer to ask for fresh data (after an import, a finished timer, a setting change). */
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

    fun tap(context: Context) = TapAction(pendingIntent = Notifier.openClash(context))

    fun settingsIntent(c: Context, kind: String) =
        Intent(c, SmartspaceSettingsActivity::class.java).putExtra(SmartspaceSettingsActivity.EXTRA_KIND, kind)

    /** What a card/chip says right now, or null when it should be hidden. */
    class Content(val title: String, val subtitle: String, val chip: String, val icon: Int)

    private fun slotOf(what: String) = when (what) {
        "lab" -> Slot.LAB
        "pet" -> Slot.PET
        "bb" -> Slot.BB_BUILDER
        "starlab" -> Slot.STAR_LAB
        else -> Slot.BUILDER
    }

    private fun iconOf(what: String) = when (what) {
        "lab" -> R.drawable.ic_flask
        "pet" -> R.drawable.ic_paw
        "starlab" -> R.drawable.ic_star
        "boost" -> R.drawable.ic_bolt
        else -> R.drawable.ic_hammer
    }

    fun content(c: Context, o: SmartOptions, now: Long): Content? {
        Format.configure(c)
        val v = (if (o.village.isNotEmpty()) Store.load(c, o.village) else null) ?: Store.load(c) ?: return null

        if (o.what == "boost") {
            val active = Status.activeBoosts(v, now)
            val b = active.firstOrNull() ?: return null
            val left = b.remainingMs(now)
            if (o.show == "soon" && left > 3_600_000L) return null
            return Content(Status.boostLabel(active), "Ends ${Format.phrase(left, now)}", compact(left), iconOf("boost"))
        }

        val slot = slotOf(o.what)
        val s = Status.of(v, slot, now, if (slot == Slot.BUILDER) Store.builderOverride(c, v.tag) else 0) ?: return null
        val running = v.timers.filter { (it.slot == slot && !it.extra || it.blocks == slot) && it.remainingMs(now) > 0 }
            .sortedBy { it.endSec }
        val first = running.firstOrNull()
        when (o.show) {
            "free" -> if (s.free == 0) return null
            "busy" -> if (s.free > 0) return null
            "soon" -> if (first == null || first.remainingMs(now) > 3_600_000L) return null
        }

        val name = when (slot) {
            Slot.BUILDER -> "builder"
            Slot.BB_BUILDER -> "Builder Base builder"
            Slot.LAB -> "Laboratory"
            Slot.STAR_LAB -> "Star Laboratory"
            Slot.PET -> "Pet House"
            else -> ""
        }
        val title = when (slot) {
            Slot.BUILDER -> Status.builderTitle(s)
            Slot.BB_BUILDER -> if (s.free == 0) "Builder Base busy" else if (s.free == 1) "1 $name free" else "${s.free} ${name}s free"
            else -> if (s.free > 0) "$name free" else "$name busy"
        } + if (o.boostInTitle) Status.activeBoosts(v, now).takeIf { it.isNotEmpty() }?.let { " · ${Status.boostLabel(it)}" } ?: "" else ""

        val subtitle = when {
            o.text == "count" -> "${s.busy} of ${s.total} working"
            s.free == 0 && s.nextFreeSec != null -> "Next free ${Format.phrase(s.nextFreeSec * 1000 - now, now)}"
            first != null -> "${first.name} ${Format.phrase(first.remainingMs(now), now)}"
            else -> if (slot == Slot.LAB || slot == Slot.STAR_LAB) "Time to research something" else "Time to build something"
        }
        val chip = when {
            o.text == "count" -> "${s.busy}/${s.total}"
            s.free > 0 -> if (s.total > 1) "${s.free} free" else "free"
            s.nextFreeSec != null -> compact(s.nextFreeSec * 1000 - now)
            else -> "busy"
        }
        return Content(title, subtitle, chip, iconOf(o.what))
    }
}

/** At a Glance card. Can be added several times (e.g. builders and lab), each with its own settings. */
class BuildersTarget : SmartspacerTargetProvider() {
    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val c = provideContext()
        val o = Smartspace.options(c, smartspacerId, SmartOptions())
        val x = Smartspace.content(c, o, System.currentTimeMillis()) ?: return emptyList()
        return listOf(
            TargetTemplate.Basic(
                id = "clash_$smartspacerId",
                componentName = ComponentName(c, BuildersTarget::class.java),
                title = Text(x.title),
                subtitle = Text(x.subtitle),
                icon = Smartspace.icon(c, x.icon),
                onClick = Smartspace.tap(c),
            ).create().apply {
                canBeDismissed = false
                if (!o.lockScreen) limitToSurfaces = setOf(UiSurface.HOMESCREEN)
            },
        )
    }

    override fun getConfig(smartspacerId: String?) = Config(
        label = "Clash Timers",
        description = "Builders, lab, pets or boosts: what's free and what finishes next",
        icon = AndroidIcon.createWithResource(provideContext(), R.drawable.ic_hammer),
        allowAddingMoreThanOnce = true,
        configActivity = Smartspace.settingsIntent(provideContext(), "target"),
        refreshPeriodMinutes = 1,
    )

    override fun onDismiss(smartspacerId: String, targetId: String) = false

    override fun onProviderRemoved(smartspacerId: String) = Smartspace.remove(provideContext(), smartspacerId)
}

/** Base for the chips; the subclasses only differ in what they show by default. */
abstract class ClashComplication(
    private val default: SmartOptions,
    private val label: String,
    private val description: String,
) : SmartspacerComplicationProvider() {
    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        val c = provideContext()
        val o = Smartspace.options(c, smartspacerId, default)
        val x = Smartspace.content(c, o, System.currentTimeMillis()) ?: return emptyList()
        return listOf(
            ComplicationTemplate.Basic(
                id = "clash_$smartspacerId",
                icon = Smartspace.icon(c, x.icon),
                content = Text(x.chip),
                onClick = Smartspace.tap(c),
            ).create(),
        )
    }

    override fun getConfig(smartspacerId: String?) = Config(
        label = label,
        description = description,
        icon = AndroidIcon.createWithResource(provideContext(), when (default.what) {
            "lab" -> R.drawable.ic_flask
            "boost" -> R.drawable.ic_bolt
            else -> R.drawable.ic_hammer
        }),
        allowAddingMoreThanOnce = true,
        configActivity = Smartspace.settingsIntent(provideContext(), "chip:${default.what}"),
        refreshPeriodMinutes = 1,
    )

    override fun onProviderRemoved(smartspacerId: String) = Smartspace.remove(provideContext(), smartspacerId)
}

class BuildersComplication : ClashComplication(
    SmartOptions(what = "builder"), "Clash builders", "Free builders, or time until the next one (configurable)",
)

class LabComplication : ClashComplication(
    SmartOptions(what = "lab"), "Clash laboratory", "Lab free, or time left on the research (configurable)",
)

class BoostComplication : ClashComplication(
    SmartOptions(what = "boost"), "Clash boost", "Time left on a running potion, snack or Clock Tower",
)

/** Smartspacer's periodic refresh: just ask it to re-read our providers (data is local). */
class SmartspaceTargetRefresh : SmartspacerTargetUpdateReceiver() {
    override fun onRequestSmartspaceTargetUpdate(context: Context, requestTargets: List<RequestTarget>) =
        Smartspace.notifyAll(context)
}

class SmartspaceComplicationRefresh : SmartspacerComplicationUpdateReceiver() {
    override fun onRequestSmartspaceComplicationUpdate(context: Context, requestComplications: List<RequestComplication>) =
        Smartspace.notifyAll(context)
}
