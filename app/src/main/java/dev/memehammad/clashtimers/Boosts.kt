package dev.memehammad.clashtimers

import org.json.JSONObject

/**
 * A speed-up running until [endSec]: adds [extra] to the normal speed of 1.
 * (Builder Potion 10x -> extra 9, Lab Assistant level 4 = 5x -> extra 4.)
 *
 * The export stores remaining work at normal speed, so real finish times are worked out here.
 */
data class Boost(val endSec: Long, val extra: Int)

object Speed {
    /** Speed at [t] (seconds): 1 + extras of the boosts still running. */
    private fun at(boosts: List<Boost>, t: Double): Double =
        1.0 + boosts.sumOf { if (t < it.endSec) it.extra.toDouble() else 0.0 }

    /** Real seconds needed from [startSec] to finish [work] normal-speed seconds. */
    fun realSeconds(startSec: Long, work: Long, boosts: List<Boost>): Long {
        if (boosts.isEmpty()) return work
        var t = startSec.toDouble()
        var left = work.toDouble()
        for (end in boosts.map { it.endSec.toDouble() }.filter { it > t }.sorted().distinct()) {
            val speed = at(boosts, t)
            val canDo = (end - t) * speed
            if (left <= canDo) return kotlin.math.ceil(t + left / speed - startSec).toLong()
            left -= canDo
            t = end
        }
        return kotlin.math.ceil(t + left - startSec).toLong()
    }

    /** Normal-speed work left at [nowSec] for a timer that finishes at [endSec]. */
    fun workLeft(nowSec: Double, endSec: Long, boosts: List<Boost>): Double {
        if (nowSec >= endSec) return 0.0
        if (boosts.isEmpty()) return endSec - nowSec
        val points = (boosts.map { it.endSec.toDouble() }.filter { it > nowSec && it < endSec } + endSec.toDouble())
            .sorted().distinct()
        var t = nowSec
        var work = 0.0
        for (p in points) {
            work += (p - t) * at(boosts, t)
            t = p
        }
        return work
    }
}

/**
 * Reads potions and snacks from the export's "boosts" object.
 *
 * Confirmed: "builder_boost" (Builder Potion, 10x, verified against two exports).
 * From other export readers: "lab_boost" (Research Potion) and "pet_boost" (Pet Potion), both 24x.
 * Snack key names aren't documented yet, so they're matched loosely:
 * builder snack -> Builder Bite (2x, doesn't stack with the potion),
 * lab/study snack -> Study Soup (4x, stacks with the Research Potion: 24x + 4x = 27x).
 * Any other "*clocktower*" key that isn't a cooldown -> Clock Tower, 10x on the Builder Base.
 */
object BoostReader {
    data class Info(val name: String, val slots: List<Slot>, val mult: Int, val durationSec: Long?)

    fun identify(key: String): Info? {
        val k = key.lowercase()
        if (k.endsWith("cooldown")) return null
        return when {
            k == "builder_boost" -> Info(tr(R.string.t_builder_potion), listOf(Slot.BUILDER), 10, 3600)
            k == "lab_boost" || k == "research_boost" -> Info(tr(R.string.t_research_potion), listOf(Slot.LAB), 24, 3600)
            k == "pet_boost" -> Info(tr(R.string.t_pet_potion), listOf(Slot.PET), 24, 3600)
            "clocktower" in k || "clock_tower" in k -> Info(tr(R.string.t_clock_tower), listOf(Slot.BB_BUILDER, Slot.STAR_LAB), 10, null)
            "bite" in k || ("builder" in k && ("snack" in k || "food" in k)) -> Info(tr(R.string.t_builder_bite), listOf(Slot.BUILDER), 2, 3600)
            "soup" in k || "study" in k || ("lab" in k && ("snack" in k || "food" in k)) -> Info(tr(R.string.t_study_soup), listOf(Slot.LAB), 4, 3600)
            else -> null
        }
    }

    /** Boosts per slot, as absolute end times. */
    fun read(obj: JSONObject?, timestamp: Long): Map<Slot, List<Boost>> {
        if (obj == null) return emptyMap()
        val bySlot = HashMap<Slot, MutableList<Pair<Info, Long>>>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val left = obj.optLong(key, 0L)
            if (left <= 0L) continue
            val info = identify(key) ?: continue
            for (slot in info.slots) bySlot.getOrPut(slot) { mutableListOf() } += info to (timestamp + left)
        }
        return bySlot.mapValues { (slot, list) ->
            if (slot == Slot.BUILDER) builderBoosts(list) else list.map { (info, end) -> Boost(end, info.mult - 1) }
        }
    }

    /** Builder Potion and Builder Bite don't stack: the Bite only counts once the Potion ends. */
    private fun builderBoosts(list: List<Pair<Info, Long>>): List<Boost> {
        val potionEnd = list.filter { it.first.mult >= 10 }.maxOfOrNull { it.second }
        val out = mutableListOf<Boost>()
        if (potionEnd != null) out += Boost(potionEnd, 9)
        for ((info, end) in list) {
            if (info.mult >= 10) continue
            if (potionEnd == null) {
                out += Boost(end, info.mult - 1)
            } else if (end > potionEnd) {
                // Runs at 10x until the potion ends, then at 2x: model as +1 until bite end, +8 more until potion end.
                out[0] = Boost(potionEnd, 8)
                out += Boost(end, info.mult - 1)
            }
        }
        return out
    }
}
