package dev.memehammad.clashtimers

/** Export age: after a day the timers are likely out of date (new upgrades, potions, ...). */
/**
 * When the export is probably out of date. Timers can only go wrong once you've played since
 * exporting, and the sign of that is a builder/lab/pet freeing up (you'd start the next upgrade).
 * So it turns stale a set time (setting) after the first upgrade or research finishes after the
 * export. While everything is still running it stays valid, however old the export is.
 */
object Stale {
    /** Hours after the first finish; 0 = never. Set by [Format.configure]. */
    @Volatile
    var hours = 12

    fun ageMs(village: Village, nowMs: Long): Long = nowMs - village.timestamp * 1000

    /** When the export turns stale (ms), or null if it won't. */
    fun staleAtMs(village: Village): Long? {
        if (hours <= 0) return null
        return village.timers
            .filter { it.slot != Slot.NONE && it.endSec > village.timestamp }
            .minOfOrNull { it.endSec }
            ?.let { it * 1000 + hours * 3600_000L }
    }

    fun isStale(village: Village, nowMs: Long): Boolean = staleAtMs(village)?.let { nowMs >= it } ?: false

    /** "Export 1 d old" */
    fun label(village: Village, nowMs: Long): String = "Export ${Format.ago(ageMs(village, nowMs)).removeSuffix(" ago")} old"
}

object HeroStatus {
    /** "Heroes 3/4 ready · BK back in 16h" or null if no heroes. */
    fun summary(village: Village, nowMs: Long): String? {
        val heroes = village.heroes
        if (heroes.isEmpty()) return null
        val ready = heroes.count { it.ready(nowMs) }
        val next = heroes.filter { !it.ready(nowMs) }.minByOrNull { it.backSec ?: Long.MAX_VALUE }
        return if (next == null) "All ${heroes.size} heroes ready"
        else "Heroes $ready/${heroes.size} ready · ${next.short} back ${Format.phrase(next.backSec!! * 1000 - nowMs, nowMs)}"
    }
}

/** When to remind about an idle builder. */
object Reminder {
    /**
     * Time (s) a builder became, or will become, free from now on; null if never within the data.
     * If one is free already, it's the latest builder finish before now (or the export time).
     */
    fun freeSinceSec(village: Village, nowMs: Long, override: Int): Long? {
        val s = Status.of(village, Slot.BUILDER, nowMs, override) ?: return null
        if (s.free > 0) {
            val nowSec = nowMs / 1000
            val lastDone = village.timers
                .filter { it.slot == Slot.BUILDER && !it.extra && it.endSec <= nowSec }
                .maxOfOrNull { it.endSec }
            return maxOf(lastDone ?: village.timestamp, village.timestamp)
        }
        return s.nextFreeSec
    }
}

/** "What if I use this boost now?" */
object Planner {
    enum class Item(val label: String, val slots: List<Slot>, val extra: Int) {
        BUILDER_POTION("Builder Potion", listOf(Slot.BUILDER), 9),
        BUILDER_BITE("Builder Bite", listOf(Slot.BUILDER), 1),
        RESEARCH_POTION("Research Potion", listOf(Slot.LAB), 23),
        STUDY_SOUP("Study Soup", listOf(Slot.LAB), 3),
        PET_POTION("Pet Potion", listOf(Slot.PET), 23),
    }

    data class Row(val timer: Timer, val oldEndSec: Long, val newEndSec: Long, val duringBoost: Boolean)

    data class Plan(
        val item: Item,
        val rows: List<Row>,
        /** Next free worker of the main slot, before and after (s); null = one is free already. */
        val nextFreeBefore: Long?,
        val nextFreeAfter: Long?,
        /** Why nothing changes, e.g. a Builder Bite during a Builder Potion. */
        val note: String?,
    )

    private const val HOUR = 3600L

    fun plan(village: Village, item: Item, nowMs: Long, override: Int): Plan {
        val now = nowMs / 1000
        val boostEnd = now + HOUR
        val running = village.timers.filter { it.slot in item.slots && it.endSec > now }

        val potionRunning = item == Item.BUILDER_BITE && running.any { t ->
            t.boosts.any { it.extra >= 8 && it.endSec > now }
        }
        val rows = running.map { t ->
            val work = Speed.workLeft(now.toDouble(), t.endSec, t.boosts).toLong()
            val active = t.boosts.filter { it.endSec > now }
            val sameKind = active.firstOrNull { it.extra == item.extra }
            val boosts = when {
                potionRunning -> active
                // Same potion again: it extends the running one by an hour.
                sameKind != null -> active - sameKind + Boost(sameKind.endSec + HOUR, item.extra)
                else -> active + Boost(boostEnd, item.extra)
            }
            val newEnd = now + Speed.realSeconds(now, work, boosts)
            Row(t, t.endSec, newEnd, newEnd <= (sameKind?.endSec?.plus(HOUR) ?: boostEnd))
        }.sortedBy { it.newEndSec }

        val slot = item.slots.first()
        val before = Status.of(village, slot, nowMs, if (slot == Slot.BUILDER) override else 0)?.nextFreeSec
        val total = Status.of(village, slot, nowMs, if (slot == Slot.BUILDER) override else 0)?.total ?: 0
        val ends = (rows.filter { !it.timer.extra }.map { it.newEndSec } +
            village.timers.filter { it.blocks == slot && it.endSec > now }.map { it.endSec }).sorted()
        val after = if (total > 0 && ends.size >= total) ends[ends.size - total] else null

        return Plan(
            item, rows, before, after,
            if (potionRunning) "A Builder Bite has no effect while a Builder Potion is running." else null,
        )
    }
}
