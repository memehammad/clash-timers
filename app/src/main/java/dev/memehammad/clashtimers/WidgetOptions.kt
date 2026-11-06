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
        const val IMPORT = 1024
        const val ALL = 0x7FF

        /** Marks options saved by a version that knows about [IMPORT]. */
        const val SAVED_V2 = 1 shl 30

        val SECTION_FLAGS = listOf(
            Section.BUILDERS to LIST_BUILDERS,
            Section.LAB to LIST_LAB,
            Section.PETS to LIST_PETS,
            Section.BUILDER_BASE to LIST_BUILDER_BASE,
            Section.HELPERS to LIST_HELPERS,
            Section.OTHER to LIST_BUILDERS,
        )

        /** (flag, label, description) shown in the widget settings. */
        val ITEMS get() = listOf(
            Triple(IMPORT, tr(R.string.wo_import), tr(R.string.wo_import_d)),
            Triple(CHIPS, tr(R.string.wo_chips), tr(R.string.wo_chips_d)),
            Triple(HEROES, tr(R.string.t_heroes), tr(R.string.wo_heroes_d)),
            Triple(BOOST, tr(R.string.wo_boost), tr(R.string.wo_boost_d)),
            Triple(STALE, tr(R.string.wo_stale), tr(R.string.wo_stale_d)),
            Triple(PROGRESS, tr(R.string.wo_progress), tr(R.string.wo_progress_d)),
            Triple(LIST_BUILDERS, tr(R.string.wo_list, tr(R.string.t_builders)), null),
            Triple(LIST_LAB, tr(R.string.wo_list, tr(R.string.t_lab)), null),
            Triple(LIST_PETS, tr(R.string.wo_list, tr(R.string.t_pet_house)), null),
            Triple(LIST_BUILDER_BASE, tr(R.string.wo_list, tr(R.string.t_bb)), null),
            Triple(LIST_HELPERS, tr(R.string.wo_list, tr(R.string.t_helpers)), null),
        )
    }
}
