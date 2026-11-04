package dev.memehammad.clashtimers

/** Live worker status for one slot (builders, lab, ...). */
data class SlotStatus(val slot: Slot, val total: Int, val busy: Int, val nextFreeSec: Long?) {
    val free: Int get() = (total - busy).coerceAtLeast(0)
}

object Status {
    /** Short labels for the status chips. */
    val chips = listOf(
        Slot.LAB to "Lab",
        Slot.STAR_LAB to "Star Lab",
        Slot.PET to "Pets",
        Slot.BB_BUILDER to "Builder Base",
    )

    /**
     * @param builderOverride user-set number of home builders, 0 = auto-detect.
     * @return null if that slot isn't unlocked.
     */
    fun of(village: Village, slot: Slot, nowMs: Long, builderOverride: Int = 0): SlotStatus? {
        val total = if (slot == Slot.BUILDER && builderOverride > 0) builderOverride
        else village.capacity[slot] ?: return null
        if (total <= 0) return null

        val running = village.timers
            .filter { ((it.slot == slot && !it.extra) || it.blocks == slot) && it.remainingMs(nowMs) > 0 }
            .sortedBy { it.endSec }
        val busy = running.size.coerceAtMost(total)
        // With every worker busy, the next one frees up when the (n - total + 1)-th earliest job ends.
        val next = if (running.size >= total) running[running.size - total].endSec else null
        return SlotStatus(slot, total, busy, next)
    }

    fun shortLabel(slot: Slot): String = when (slot) {
        Slot.BB_BUILDER -> "BB"
        Slot.STAR_LAB -> "Star Lab"
        Slot.PET -> "Pets"
        else -> "Lab"
    }

    fun builderTitle(s: SlotStatus): String = when {
        s.free == 0 -> "All builders busy"
        s.free == 1 -> "1 builder free"
        else -> "${s.free} builders free"
    }

    fun builderSubtitle(s: SlotStatus): String =
        if (s.free == 0) "Next free in" else "${s.busy} of ${s.total} working"

    /** Running potions/snacks/Clock Tower, soonest ending first. */
    fun activeBoosts(village: Village, nowMs: Long): List<Timer> =
        village.timers.filter { it.isBoost && it.remainingMs(nowMs) > 0 }.sortedBy { it.endSec }

    /** One-line label, e.g. "Builder Potion 10x" or "Builder Potion + 1 more". */
    fun boostLabel(active: List<Timer>): String {
        val first = active.first()
        val mult = first.detail.substringAfter("· ", "").trim()
        val base = if (mult.isNotEmpty()) "${first.name} $mult" else first.name
        return if (active.size > 1) "$base + ${active.size - 1} more" else base
    }
}
