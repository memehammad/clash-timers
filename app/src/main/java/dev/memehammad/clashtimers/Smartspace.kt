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
    /** Any of builder, lab, pet, bb, starlab, boost. */
    val what: Set<String> = setOf("builder"),
    /** Any of free, busy, soon; empty = always. Something shows when any of them is true. */
    val show: Set<String> = emptySet(),
    /** "soon" means finishing within this many minutes. */
    val soonMinutes: Int = 60,
    /** Card subtitle / chip text: next (what finishes next, when) or count (working / total). */
    val text: String = "next",
    /** Card: mention a running potion or Clock Tower in the title. */
    val boostInTitle: Boolean = true,
    /** Card: also show on the lock screen. */
    val lockScreen: Boolean = true,
) {
    fun toJson(): String = JSONObject().put("village", village)
        .put("what", org.json.JSONArray(what.toList())).put("show", org.json.JSONArray(show.toList()))
        .put("soonMinutes", soonMinutes).put("text", text)
        .put("boostInTitle", boostInTitle).put("lockScreen", lockScreen).toString()

    companion object {
        val WHAT = listOf(
            "builder" to "Builders", "lab" to "Laboratory", "pet" to "Pet House",
            "bb" to "Builder Base builders", "starlab" to "Star Laboratory", "boost" to "Active boost",
        )
        val SHOW = listOf(
            "free" to "When one is free",
            "busy" to "While all are busy",
            "soon" to "Before something finishes",
        )
        val SOON = listOf(15, 30, 60, 120, 180, 360)

        private fun set(o: JSONObject, key: String, default: Set<String>): Set<String> {
            o.optJSONArray(key)?.let { a -> return (0 until a.length()).map { a.getString(it) }.toSet() }
            // Older single-value settings.
            val v = o.optString(key, "")
            return when {
                v.isEmpty() -> default
                v == "always" -> emptySet()
                else -> setOf(v)
            }
        }

        fun fromJson(s: String?, default: SmartOptions): SmartOptions {
            val o = s?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return default
            return SmartOptions(
                o.optString("village", default.village), set(o, "what", default.what).ifEmpty { default.what },
                set(o, "show", default.show), o.optInt("soonMinutes", default.soonMinutes),
                o.optString("text", default.text),
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

    /** One item (builders, lab, ...) as it looks right now. */
    class Item(
        val title: String,
        val subtitle: String,
        val chip: String,
        val icon: Int,
        /** When the next thing finishes (ms from now), for picking the most urgent item. */
        val nextMs: Long?,
        val count: String,
    )

    /** What a card or chip says right now; empty when nothing should show. */
    class Content(val items: List<Item>) {
        val title: String get() = items.joinToString(" · ") { it.title }
        val subtitle: String get() = (items.filter { it.nextMs != null }.minByOrNull { it.nextMs!! } ?: items.first()).subtitle
        val icon: Int get() = (items.filter { it.nextMs != null }.minByOrNull { it.nextMs!! } ?: items.first()).icon
    }

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
        val soonMs = o.soonMinutes * 60_000L
        val items = SmartOptions.WHAT.map { it.first }.filter { it in o.what }.mapNotNull { item(c, v, it, o, now, soonMs) }
        if (items.isEmpty()) return null
        // With several items, titles get short so they fit side by side.
        return Content(items)
    }

    private fun item(c: Context, v: Village, what: String, o: SmartOptions, now: Long, soonMs: Long): Item? {
        if (what == "boost") {
            val active = Status.activeBoosts(v, now)
            val b = active.firstOrNull() ?: return null
            val left = b.remainingMs(now)
            if (o.show.isNotEmpty() && !("soon" in o.show && left <= soonMs)) {
                // A running boost counts as "busy"; "free" never applies to it.
                if ("busy" !in o.show) return null
            }
            return Item(Status.boostLabel(active), "${b.name} ends ${Format.phrase(left, now)}", compact(left), iconOf(what), left, "")
        }

        val slot = slotOf(what)
        val s = Status.of(v, slot, now, if (slot == Slot.BUILDER) Store.builderOverride(c, v.tag) else 0) ?: return null
        val running = v.timers.filter { (it.slot == slot && !it.extra || it.blocks == slot) && it.remainingMs(now) > 0 }
            .sortedBy { it.endSec }
        val first = running.firstOrNull()
        val freeAt = if (s.free == 0) s.nextFreeSec?.let { it * 1000 - now } else null
        if (o.show.isNotEmpty()) {
            val ok = ("free" in o.show && s.free > 0) ||
                ("busy" in o.show && s.free == 0) ||
                ("soon" in o.show && first != null && first.remainingMs(now) <= soonMs)
            if (!ok) return null
        }
        val many = o.what.size > 1
        val title = when (slot) {
            Slot.BUILDER -> if (many) (if (s.free > 0) "${s.free} builder${if (s.free > 1) "s" else ""} free" else "Builders busy") else Status.builderTitle(s)
            Slot.BB_BUILDER -> if (s.free > 0) "${s.free} BB builder${if (s.free > 1) "s" else ""} free" else "Builder Base busy"
            Slot.LAB -> if (s.free > 0) "Lab free" else if (many) "Lab busy" else "Researching"
            Slot.STAR_LAB -> if (s.free > 0) "Star Lab free" else "Star Lab busy"
            Slot.PET -> if (s.free > 0) "Pets free" else "Pet House busy"
            else -> ""
        } + if (!many && o.boostInTitle) Status.activeBoosts(v, now).takeIf { it.isNotEmpty() }?.let { " · ${Status.boostLabel(it)}" } ?: "" else ""
        val label = when (slot) {
            Slot.BUILDER -> "Builders"
            Slot.BB_BUILDER -> "BB"
            Slot.LAB -> "Lab"
            Slot.STAR_LAB -> "Star Lab"
            else -> "Pets"
        }
        val subtitle = when {
            o.text == "count" -> "${s.busy} of ${s.total} working"
            freeAt != null -> "$label: next free ${Format.phrase(freeAt, now)}"
            first != null -> "${first.name} ${Format.phrase(first.remainingMs(now), now)}"
            else -> if (slot == Slot.LAB || slot == Slot.STAR_LAB) "Time to research something" else "Time to build something"
        }
        val chip = when {
            o.text == "count" -> "${s.busy}/${s.total}"
            s.free > 0 -> if (s.total > 1) "${s.free} free" else "free"
            freeAt != null -> compact(freeAt)
            else -> "busy"
        }
        return Item(title, subtitle, chip, iconOf(what), first?.remainingMs(now), "$label ${s.busy}/${s.total}")
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
                subtitle = Text(if (o.text == "count" && x.items.size > 1) x.items.joinToString(" · ") { it.count } else x.subtitle),
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
        // One chip per item (builders, lab, ...).
        return x.items.mapIndexed { i, it ->
            ComplicationTemplate.Basic(
                id = "clash_${smartspacerId}_$i",
                icon = Smartspace.icon(c, it.icon),
                content = Text(it.chip),
                onClick = Smartspace.tap(c),
            ).create()
        }
    }

    override fun getConfig(smartspacerId: String?) = Config(
        label = label,
        description = description,
        icon = AndroidIcon.createWithResource(provideContext(), when (default.what.first()) {
            "lab" -> R.drawable.ic_flask
            "boost" -> R.drawable.ic_bolt
            else -> R.drawable.ic_hammer
        }),
        allowAddingMoreThanOnce = true,
        configActivity = Smartspace.settingsIntent(provideContext(), "chip:${default.what.first()}"),
        refreshPeriodMinutes = 1,
    )

    override fun onProviderRemoved(smartspacerId: String) = Smartspace.remove(provideContext(), smartspacerId)
}

class BuildersComplication : ClashComplication(
    SmartOptions(what = setOf("builder")), "Clash builders", "Free builders, or time until the next one (configurable)",
)

class LabComplication : ClashComplication(
    SmartOptions(what = setOf("lab")), "Clash laboratory", "Lab free, or time left on the research (configurable)",
)

class BoostComplication : ClashComplication(
    SmartOptions(what = setOf("boost")), "Clash boost", "Time left on a running potion, snack or Clock Tower",
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
