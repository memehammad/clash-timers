package dev.memehammad.clashtimers

/** Per-widget display options (stored as a bit set; everything is on by default). */
data class WidgetOptions(val bits: Int = ALL) {
    fun has(flag: Int) = bits and flag != 0
    fun with(flag: Int, on: Boolean) = WidgetOptions(if (on) bits or flag else bits and flag.inv())

    val sections: Set<Section>
        get() = SECTION_FLAGS.filter { (_, f) -> has(f) }.map { it.first }.toSet()

    companion object {
        const val CHIPS = 1
        const val HEROES = 2
        const val BOOST = 4
        const val STALE = 8
        const val PROGRESS = 16
        const val LIST_BUILDERS = 32
        const val LIST_LAB = 64
        const val LIST_PETS = 128
        const val LIST_BUILDER_BASE = 256
        const val LIST_HELPERS = 512
        const val ALL = 0x3FF

        val SECTION_FLAGS = listOf(
            Section.BUILDERS to LIST_BUILDERS,
            Section.LAB to LIST_LAB,
            Section.PETS to LIST_PETS,
            Section.BUILDER_BASE to LIST_BUILDER_BASE,
            Section.HELPERS to LIST_HELPERS,
            Section.OTHER to LIST_BUILDERS,
        )

        /** (flag, label, description) shown in the widget settings. */
        val ITEMS = listOf(
            Triple(CHIPS, "Status chips", "Laboratory, Pet House, Builder Base, Star Lab"),
            Triple(HEROES, "Heroes", "How many heroes are ready"),
            Triple(BOOST, "Boost banner", "Running potions and snacks"),
            Triple(STALE, "Old export hint", "After a day without a new export"),
            Triple(PROGRESS, "Progress bars", "The squiggly lines in the list"),
            Triple(LIST_BUILDERS, "List: Builders", null),
            Triple(LIST_LAB, "List: Laboratory", null),
            Triple(LIST_PETS, "List: Pet House", null),
            Triple(LIST_BUILDER_BASE, "List: Builder Base", null),
            Triple(LIST_HELPERS, "List: Helpers & Boosts", null),
        )
    }
}
