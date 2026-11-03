package dev.memehammad.clashtimers

/** Expressive shape + icon per section, shared by the widget and the app. */
object Icons {
    fun of(section: Section): Pair<Int, Int> = when (section) {
        Section.BUILDERS -> R.drawable.shape_cookie to R.drawable.ic_hammer
        Section.LAB -> R.drawable.shape_clover to R.drawable.ic_flask
        Section.PETS -> R.drawable.shape_flower to R.drawable.ic_paw
        Section.BUILDER_BASE -> R.drawable.shape_sunny to R.drawable.ic_star
        Section.HELPERS, Section.OTHER -> R.drawable.shape_circle to R.drawable.ic_hourglass
    }
}
