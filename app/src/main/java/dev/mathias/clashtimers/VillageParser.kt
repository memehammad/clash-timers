package dev.mathias.clashtimers

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class Section(val title: String) {
    BUILDERS("Builders"),
    LAB("Laboratory"),
    PETS("Pet House"),
    BUILDER_BASE("Builder Base"),
    HELPERS("Helpers & Boosts"),
    OTHER("Other"),
}

/** Which worker a timer occupies; used to work out free builders, idle lab, etc. */
enum class Slot(val freeText: String) {
    BUILDER("Builder free"),
    LAB("Laboratory free"),
    PET("Pet House free"),
    BB_BUILDER("Builder Base builder free"),
    STAR_LAB("Star Laboratory free"),
    NONE(""),
}

data class Timer(
    val key: String,
    val section: Section,
    val slot: Slot,
    /** Runs on a temporary extra worker (e.g. Goblin Builder), so it doesn't block a regular one. */
    val extra: Boolean,
    val name: String,
    val detail: String,
    val endSec: Long,
    val totalSec: Long?,
    val doneTitle: String,
    val doneText: String,
    /** Upgrading the Laboratory etc. also blocks research there until it's done. */
    val blocks: Slot? = null,
    /** Potions, snacks and helpers speeding this timer up. */
    val boosts: List<Boost> = emptyList(),
    /** This row is an active potion/snack/Clock Tower (drives the boost theme). */
    val isBoost: Boolean = false,
) {
    fun remainingMs(nowMs: Long): Long = endSec * 1000 - nowMs

    fun progress(nowMs: Long): Float? {
        val total = totalSec ?: return null
        if (total <= 0L) return null
        val left = Speed.workLeft(nowMs / 1000.0, endSec, boosts)
        return (1f - left.toFloat() / total).coerceIn(0f, 1f)
    }
}

data class Village(
    val tag: String,
    val timestamp: Long,
    val timers: List<Timer>,
    /** Number of workers per slot; missing = not unlocked. */
    val capacity: Map<Slot, Int>,
    /** Home-village heroes; [Hero.backSec] is set while one is upgrading (and can't attack). */
    val heroes: List<Hero> = emptyList(),
)

data class Hero(val id: Long, val name: String, val level: Int, val backSec: Long?) {
    fun ready(nowMs: Long): Boolean = backSec == null || backSec * 1000 <= nowMs

    /** "Barbarian King" -> "BK", "Minion Prince" -> "MP". */
    val short: String
        get() = name.split(' ').filter { it.isNotEmpty() }.joinToString("") { it.first().uppercase() }.take(3)
}

/** Parses the in-game "Data Export" JSON into running timers and worker counts. */
object VillageParser {
    private const val BUILDERS_HUT = 1000015L
    private const val BOB_HUT = 1000064L
    private const val LABORATORY = 1000007L
    private const val PET_HOUSE = 1000068L
    private const val BUILDER_HALL = 1000034L
    private const val OTTO_OUTPOST = 1000078L
    private const val STAR_LAB = 1000046L

    fun looksLikeVillage(text: CharSequence?): Boolean {
        if (text == null) return false
        val t = text.trim()
        return t.startsWith("{") && t.endsWith("}") &&
            t.contains("\"tag\"") && t.contains("\"timestamp\"")
    }

    fun parse(context: Context, text: String): Village? =
        try {
            parseOrThrow(context, text)
        } catch (e: Exception) {
            null
        }

    private fun parseOrThrow(context: Context, text: String): Village? {
        val root = JSONObject(text.trim())
        val tag = root.optString("tag")
        val timestamp = root.optLong("timestamp", 0L)
        if (tag.isEmpty() || timestamp <= 0L) return null
        if (!root.has("buildings") && !root.has("heroes") && !root.has("units")) return null

        val collector = Collector(
            GameData.get(context), timestamp,
            BoostReader.read(root.optJSONObject("boosts"), timestamp),
            helperLevels(root.optJSONArray("helpers")),
        )
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val section = sectionFor(key) ?: continue
            val array = root.optJSONArray(key) ?: continue
            collector.scanArray(array, key, section, slotFor(key), null)
        }
        root.optJSONArray("helpers")?.let { collector.helpers(it) }
        root.optJSONObject("boosts")?.let { collector.boosts(it) }

        val sorted = collector.out.sortedWith(compareBy<Timer>({ it.section.ordinal }, { it.endSec }))
        val heroes = heroes(root.optJSONArray("heroes"), sorted, GameData.get(context))
        return Village(tag, timestamp, sorted, capacity(root, sorted), heroes)
    }

    /** All home heroes; an upgrading hero is back when its (boost-aware) timer ends. */
    private fun heroes(array: JSONArray?, timers: List<Timer>, data: Map<Long, GameInfo>): List<Hero> {
        if (array == null) return emptyList()
        val out = mutableListOf<Hero>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optLong("data", -1L)
            val level = obj.optInt("lvl", 0)
            val name = data[id]?.name ?: continue
            val back = if (obj.optLong("timer", 0L) > 0L) {
                timers.firstOrNull { it.key.startsWith("heroes:$id:") }?.endSec
            } else null
            out += Hero(id, name, level, back)
        }
        return out
    }

    private const val BUILDERS_APPRENTICE = 93000000L
    private const val LAB_ASSISTANT = 93000001L

    /** Helper level by the slot it works on: level N adds N hours of work per hour, i.e. (N+1)x. */
    private fun helperLevels(array: JSONArray?): Map<Slot, Int> {
        val out = HashMap<Slot, Int>()
        if (array == null) return out
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val level = obj.optInt("lvl", 0)
            when (obj.optLong("data", -1L)) {
                BUILDERS_APPRENTICE -> out[Slot.BUILDER] = level
                LAB_ASSISTANT -> out[Slot.LAB] = level
            }
        }
        return out
    }

    private fun capacity(root: JSONObject, timers: List<Timer>): Map<Slot, Int> {
        val home = counts(root.optJSONArray("buildings"))
        val builder = counts(root.optJSONArray("buildings2"))
        val detected = mapOf(
            Slot.BUILDER to (home[BUILDERS_HUT] ?: 0) + (if (BOB_HUT in home) 1 else 0),
            Slot.LAB to if (LABORATORY in home) 1 else 0,
            Slot.PET to if (PET_HOUSE in home) 1 else 0,
            Slot.BB_BUILDER to if (BUILDER_HALL in builder) 1 + (if (OTTO_OUTPOST in builder) 1 else 0) else 0,
            Slot.STAR_LAB to if (STAR_LAB in builder) 1 else 0,
        )
        // At export time every regular timer had its own worker, so there are at least that many.
        val result = HashMap<Slot, Int>()
        for ((slot, n) in detected) {
            val busy = timers.count { it.slot == slot && !it.extra }
            val total = maxOf(n, busy)
            if (total > 0) result[slot] = total
        }
        return result
    }

    private fun counts(array: JSONArray?): Map<Long, Int> {
        val map = HashMap<Long, Int>()
        if (array == null) return map
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optLong("data", -1L)
            map[id] = (map[id] ?: 0) + obj.optInt("cnt", 1)
        }
        return map
    }

    private fun sectionFor(key: String): Section? = when (key) {
        "buildings", "traps", "heroes" -> Section.BUILDERS
        "units", "spells", "siege_machines" -> Section.LAB
        "pets" -> Section.PETS
        "buildings2", "traps2", "units2", "heroes2" -> Section.BUILDER_BASE
        "helpers", "boosts", "house_parts", "skins", "skins2", "sceneries", "sceneries2" -> null
        else -> if (key.startsWith("decos") || key.startsWith("obstacles")) null else Section.OTHER
    }

    private fun slotFor(key: String): Slot = when (key) {
        "buildings", "traps", "heroes" -> Slot.BUILDER
        "units", "spells", "siege_machines" -> Slot.LAB
        "pets" -> Slot.PET
        "buildings2", "traps2", "heroes2" -> Slot.BB_BUILDER
        "units2" -> Slot.STAR_LAB
        else -> Slot.NONE
    }

    private class Collector(
        val data: Map<Long, GameInfo>,
        val timestamp: Long,
        val slotBoosts: Map<Slot, List<Boost>>,
        val helperLevels: Map<Slot, Int>,
    ) {
        val out = mutableListOf<Timer>()
        private val seen = HashMap<String, Int>()

        private fun uniqueKey(base: String): String {
            val n = seen[base] ?: 0
            seen[base] = n + 1
            return if (n == 0) base else "$base#$n"
        }

        fun scanArray(array: JSONArray, key: String, section: Section, slot: Slot, parentName: String?) {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                scanObject(obj, key, section, slot, parentName)
            }
        }

        private fun scanObject(obj: JSONObject, key: String, section: Section, slot: Slot, parentName: String?) {
            val id = obj.optLong("data", -1L)
            val baseName = data[id]?.name ?: "Unknown #$id"
            val name = if (parentName != null) "$parentName · $baseName" else baseName
            if (obj.has("timer")) addUpgrade(obj, key, section, slot, id, name)

            // Nested entries, e.g. Crafting Station -> types -> modules.
            val fields = obj.keys()
            while (fields.hasNext()) {
                val nested = obj.optJSONArray(fields.next()) ?: continue
                scanArray(nested, key, section, slot, if (id >= 0) baseName else parentName)
            }
        }

        private fun addUpgrade(obj: JSONObject, key: String, homeSection: Section, homeSlot: Slot, id: Long, name: String) {
            val timer = obj.optLong("timer", 0L)
            if (timer <= 0L) return
            val level = obj.optInt("lvl", -1)
            val gearUp = obj.has("gear_up") && obj.optInt("gear_up", 1) == 0
            // Gear Ups in the home village are done by a Builder Base builder (Master Builder / O.T.T.O).
            val slot = if (gearUp) Slot.BB_BUILDER else homeSlot
            val section = if (gearUp) Section.BUILDER_BASE else homeSection
            val extra = obj.optBoolean("extra", false)
            val target = level + 1
            // Gear Ups are done by the Master Builder, so home builder boosts don't apply.
            val boosts = mutableListOf<Boost>()
            if (!gearUp) boosts += slotBoosts[homeSlot].orEmpty()
            if (gearUp) boosts += slotBoosts[Slot.BB_BUILDER].orEmpty()
            // Builder's Apprentice / Lab Assistant currently working on this upgrade.
            val helperLeft = obj.optLong("helper_timer", 0L)
            val helperLevel = helperLevels[homeSlot] ?: 0
            if (helperLeft > 0L && helperLevel > 0) boosts += Boost(timestamp + helperLeft, helperLevel)
            val end = timestamp + Speed.realSeconds(timestamp, timer, boosts)
            val speedNow = 1 + boosts.sumOf { it.extra }

            val detail = when {
                gearUp -> "Gear Up · home village"
                level <= 0 -> "Construction"
                else -> "Level $level → $target"
            }
            // Only trust the table duration if it fits the remaining time (boosts can shorten it).
            val total = if (gearUp || level < 0) null
            else data[id]?.times?.get(target)?.takeIf { it >= timer }

            val isResearch = slot == Slot.LAB || slot == Slot.STAR_LAB
            val title = when {
                gearUp -> "$name geared up"
                isResearch -> "$name research complete"
                else -> "$name upgrade complete"
            }
            val suffix = if (extra || slot == Slot.NONE) section.title else slot.freeText
            val text = if (gearUp || level < 0) suffix else "Level $target · $suffix"

            out += Timer(
                uniqueKey("$key:$id:$level:$end"), section, slot, extra,
                name,
                detail + (if (extra) " · extra" else "") + (if (speedNow > 1) " · ${speedNow}x" else ""),
                end, total, title, text,
                blocks = when (id) {
                    LABORATORY -> Slot.LAB
                    PET_HOUSE -> Slot.PET
                    STAR_LAB -> Slot.STAR_LAB
                    else -> null
                },
                boosts = boosts,
            )
        }

        fun helpers(array: JSONArray) {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val cooldown = obj.optLong("helper_cooldown", 0L)
                if (cooldown <= 0L) continue
                val id = obj.optLong("data", -1L)
                val name = data[id]?.name ?: "Helper"
                val end = timestamp + cooldown
                out += Timer(
                    uniqueKey("helper:$id:$end"), Section.HELPERS, Slot.NONE, false, name, "Cooldown",
                    end, null, "$name is ready", "Helper cooldown finished",
                )
            }
        }

        fun boosts(obj: JSONObject) {
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val seconds = obj.optLong(key, 0L)
                if (seconds <= 0L) continue
                val info = BoostReader.identify(key)
                val name = info?.name ?: boostName(key)
                val end = timestamp + seconds
                val active = !key.endsWith("_cooldown")
                out += Timer(
                    uniqueKey("boost:$key:$end"), Section.HELPERS, Slot.NONE, false, name,
                    if (active) (if (info != null) "Active · ${info.mult}x" else "Active") else "Cooldown",
                    end, info?.durationSec?.takeIf { it >= seconds },
                    if (active) "$name ended" else "$name is ready",
                    if (active) "Boost finished" else "Cooldown finished",
                    isBoost = active,
                )
            }
        }

        private fun boostName(key: String): String = when (key) {
            "clocktower_cooldown" -> "Clock Tower boost"
            else -> key.removeSuffix("_cooldown").removeSuffix("_boost").split('_')
                .joinToString(" ") { word -> word.replaceFirstChar { c -> c.uppercase() } } +
                if (key.endsWith("_boost")) " boost" else ""
        }
    }
}
