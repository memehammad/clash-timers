package dev.memehammad.clashtimers

/** Live worker status for one slot (builders, lab, ...). */
data class SlotStatus(val slot: Slot, val total: Int, val busy: Int, val nextFreeSec: Long?) {
    val free: Int get() = (total - busy).coerceAtLeast(0)
}

object Status {
    /** Short labels for the status chips. */
    val chips get() = listOf(
        Slot.LAB to tr(R.string.t_lab_short),
        Slot.STAR_LAB to tr(R.string.t_star_lab_short),
        Slot.PET to tr(R.string.t_pets),
        Slot.BB_BUILDER to tr(R.string.t_bb),
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
        Slot.BB_BUILDER -> tr(R.string.t_bb_short)
        Slot.STAR_LAB -> tr(R.string.t_star_lab_short)
        Slot.PET -> tr(R.string.t_pets)
        else -> tr(R.string.t_lab_short)
    }

    fun builderTitle(s: SlotStatus): String = when {
        s.free == 0 -> tr(R.string.all_busy)
        else -> trq(R.plurals.p_builders_free, s.free)
    }

    fun builderSubtitle(s: SlotStatus): String =
        if (s.free == 0) tr(R.string.next_free_in) else tr(R.string.n_of_m_working, s.busy, s.total)

    /** Running potions/snacks/Clock Tower, soonest ending first. */
    fun activeBoosts(village: Village, nowMs: Long): List<Timer> =
        village.timers.filter { it.isBoost && it.remainingMs(nowMs) > 0 }.sortedBy { it.endSec }

    /** One-line label, e.g. "Builder Potion 10x" or "Builder Potion + 1 more". */
    fun boostLabel(active: List<Timer>): String {
        val first = active.first()
        val mult = first.detail.substringAfter("· ", "").trim()
        val base = if (mult.isNotEmpty()) "${first.name} $mult" else first.name
        return if (active.size > 1) tr(R.string.plus_more, base, active.size - 1) else base
    }
}
