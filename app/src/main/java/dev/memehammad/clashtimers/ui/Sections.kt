package dev.memehammad.clashtimers.ui

import androidx.compose.foundation.clickable
import dev.memehammad.clashtimers.R
import dev.memehammad.clashtimers.tr
import dev.memehammad.clashtimers.trq
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.memehammad.clashtimers.Format
import dev.memehammad.clashtimers.Planner
import dev.memehammad.clashtimers.Stale
import dev.memehammad.clashtimers.Village

/** Data for the extra sections, kept apart from the core screen parameters. */
data class ScreenExtras(
    /** (tag, display name) of every saved village. */
    val villages: List<Pair<String, String>> = emptyList(),
    val currentTag: String? = null,
    val reminderMinutes: Int = 0,
    /** Sections with "finished" notifications turned off. */
    val mutedSections: Set<String> = emptySet(),
    val liveUpdate: Boolean = false,
    val liveChip: Int = 0,
    val timeMode: Int = 0,
    val progressStyle: Int = 0,
    val staleHours: Int = 12,
    val updateCheck: Boolean = true,
    /** Newer version on GitHub, if any. */
    val update: String? = null,
    val version: String = "",
)

data class ScreenActions(
    val onSelectVillage: (String) -> Unit = {},
    val onRenameVillage: (String, String) -> Unit = { _, _ -> },
    val onRemoveVillage: (String) -> Unit = {},
    val onReminder: (Int) -> Unit = {},
    val onMuteSection: (String, Boolean) -> Unit = { _, _ -> },
    val onLiveUpdate: (Boolean) -> Unit = {},
    val onLiveChip: (Int) -> Unit = {},
    val onTimeMode: (Int) -> Unit = {},
    val onProgressStyle: (Int) -> Unit = {},
    val onStaleHours: (Int) -> Unit = {},
    val onUpdateCheck: (Boolean) -> Unit = {},
    val onTutorial: () -> Unit = {},
)

// --- Villages ------------------------------------------------------------------------------------

/** Chips to switch village; tapping the selected one opens rename/remove. */
@Composable
internal fun VillageBar(extras: ScreenExtras, actions: ScreenActions) {
    var manage by remember { mutableStateOf<String?>(null) }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for ((tag, name) in extras.villages) {
            val selected = tag == extras.currentTag
            FilterChip(
                selected = selected,
                onClick = { if (selected) manage = tag else actions.onSelectVillage(tag) },
                label = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                shape = CircleShape,
            )
        }
    }
    manage?.let { tag ->
        val current = extras.villages.firstOrNull { it.first == tag }?.second ?: tag
        var text by remember(tag) { mutableStateOf(if (current == tag) "" else current) }
        AlertDialog(
            onDismissRequest = { manage = null },
            title = { Text(tag) },
            text = {
                Column {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(tr(R.string.name)) },
                        placeholder = { Text(tr(R.string.name_hint)) },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { actions.onRemoveVillage(tag); manage = null }) {
                        Text(tr(R.string.remove_village), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { actions.onRenameVillage(tag, text); manage = null }) { Text(tr(R.string.t_save)) }
            },
            dismissButton = { TextButton(onClick = { manage = null }) { Text(tr(R.string.t_cancel)) } },
        )
    }
}

// --- Stale export --------------------------------------------------------------------------------

@Composable
internal fun StaleBanner(village: Village, now: Long, onPaste: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = scheme.errorContainer,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Stale.label(village, now), style = MaterialTheme.typography.titleMedium, color = scheme.onErrorContainer)
                Text(
                    tr(R.string.stale_help),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onErrorContainer.copy(alpha = 0.8f),
                )
            }
            TextButton(onClick = onPaste) { Text(tr(R.string.t_paste), color = scheme.onErrorContainer) }
        }
    }
}

// --- Heroes --------------------------------------------------------------------------------------

/**
 * Two per row; with an odd count the last item spans the full width, so 1-6 items (labs, heroes)
 * always fill the grid without gaps.
 */
@Composable
internal fun <T> BalancedGrid(items: List<T>, modifier: Modifier = Modifier, cell: @Composable (T, Modifier) -> Unit) {
    if (items.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in items.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (item in row) cell(item, Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun HeroesCard(village: Village, now: Long) {
    val scheme = MaterialTheme.colorScheme
    BalancedGrid(village.heroes, Modifier.padding(top = 6.dp)) { h, mod ->
        val ready = h.ready(now)
        Surface(
            // Same shape language as the status chips: pill when available, squarer when busy.
            shape = if (ready) RoundedCornerShape(percent = 50) else RoundedCornerShape(12.dp),
            color = if (ready) scheme.secondaryContainer else scheme.surfaceContainer,
            modifier = mod,
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                Text(
                    h.name,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (ready) scheme.onSecondaryContainer.copy(alpha = 0.75f) else scheme.onSurfaceVariant,
                )
                Text(
                    if (ready) tr(R.string.ready_lvl, h.level) else Format.left(h.backSec!! * 1000 - now, now, short = true),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    color = if (ready) scheme.onSecondaryContainer else scheme.onSurface,
                )
            }
        }
    }
}

// --- Boost planner -------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlannerCard(village: Village, now: Long, builderOverride: Int) {
    val scheme = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    var item by remember { mutableStateOf(Planner.Item.BUILDER_POTION) }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = scheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { open = !open },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(tr(R.string.boost_planner), style = MaterialTheme.typography.titleMedium)
                    Text(
                        tr(R.string.boost_planner_d),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Text(tr(if (open) R.string.hide else R.string.show), style = MaterialTheme.typography.labelLarge, color = scheme.primary)
            }
            if (open) {

                Spacer(Modifier.height(14.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (opt in Planner.Item.entries) {
                        FilterChip(
                            selected = opt == item,
                            onClick = { item = opt },
                            label = { Text(opt.label) },
                            shape = CircleShape,
                        )
                    }
                }

                val plan = Planner.plan(village, item, now, builderOverride)
                Spacer(Modifier.height(10.dp))
                when {
                    plan.note != null -> Text(plan.note, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    plan.rows.isEmpty() -> Text(
                        tr(R.string.nothing_to_speed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    else -> {
                        val finishing = plan.rows.count { it.duringBoost }
                        val nextRes = when (item) {
                            Planner.Item.BUILDER_POTION, Planner.Item.BUILDER_BITE -> R.string.next_builder_free
                            Planner.Item.PET_POTION -> R.string.next_pet_free
                            else -> R.string.next_lab_free
                        }
                        val before = plan.nextFreeBefore
                        val after = plan.nextFreeAfter
                        Text(
                            if (finishing == 0) tr(R.string.nothing_finishes)
                            else trq(R.plurals.p_finishes, finishing),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (before != null && after != null) {
                            Text(
                                tr(nextRes, Format.phrase(after * 1000 - now, now), Format.phrase(before * 1000 - now, now)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        for (r in plan.rows) {
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    r.timer.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    Format.left(r.oldEndSec * 1000 - now, now, short = true),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                    color = scheme.onSurfaceVariant,
                                )
                                Text("  →  ", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                                Text(
                                    if (r.duringBoost) tr(R.string.done_lower) else Format.left(r.newEndSec * 1000 - now, now, short = true),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                                    color = if (r.duringBoost) scheme.primary else scheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Updates & about -----------------------------------------------------------------------------

@Composable
internal fun UpdateBanner(version: String) {
    val scheme = MaterialTheme.colorScheme
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = scheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr(R.string.update_available), style = MaterialTheme.typography.titleMedium, color = scheme.onSecondaryContainer)
                Text(
                    tr(R.string.update_text, version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSecondaryContainer.copy(alpha = 0.8f),
                )
            }
            TextButton(onClick = { uri.openUri(dev.memehammad.clashtimers.Updates.RELEASES_URL) }) {
                Text(tr(R.string.get_it), color = scheme.onSecondaryContainer)
            }
        }
    }
}

/** Footer: version and a link to the GitHub repo. */
@Composable
internal fun AboutFooter(version: String, onTutorial: () -> Unit = {}) {
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = onTutorial) {
            Text(tr(R.string.how_it_works), style = MaterialTheme.typography.labelLarge)
        }
        TextButton(onClick = { uri.openUri(dev.memehammad.clashtimers.Updates.REPO_URL) }) {
            Text(tr(R.string.on_github), style = MaterialTheme.typography.labelLarge)
        }
        Text(
            tr(R.string.version_line, version),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            tr(R.string.not_affiliated),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// --- Settings ------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsCard(extras: ScreenExtras, actions: ScreenActions) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = scheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(tr(R.string.show_times_as), style = MaterialTheme.typography.titleMedium)
            Text(
                tr(R.string.show_times_d),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((mode, label) in listOf(0 to tr(R.string.t_countdown), 1 to tr(R.string.t_finish_time))) {
                    FilterChip(
                        selected = extras.timeMode == mode,
                        onClick = { actions.onTimeMode(mode) },
                        label = { Text(label) },
                        shape = CircleShape,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(tr(R.string.wo_progress), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((style, label) in listOf(0 to tr(R.string.wavy), 1 to tr(R.string.straight), 2 to tr(R.string.t_off))) {
                    FilterChip(
                        selected = extras.progressStyle == style,
                        onClick = { actions.onProgressStyle(style) },
                        label = { Text(label) },
                        shape = CircleShape,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(tr(R.string.wo_stale), style = MaterialTheme.typography.titleMedium)
            Text(
                tr(R.string.stale_hint_d),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (h in listOf(6, 12, 24, 48, 0)) {
                    FilterChip(
                        selected = extras.staleHours == h,
                        onClick = { actions.onStaleHours(h) },
                        label = { Text(if (h == 0) tr(R.string.never) else tr(R.string.t_hours, h)) },
                        shape = CircleShape,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(tr(R.string.notify_about), style = MaterialTheme.typography.titleMedium)
            Text(
                tr(R.string.notify_about_d),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((key, label) in listOf(
                    "BUILDERS" to R.string.t_builders, "LAB" to R.string.t_lab, "PETS" to R.string.t_pet_house,
                    "BUILDER_BASE" to R.string.t_bb, "HELPERS" to R.string.t_helpers,
                )) {
                    val on = key !in extras.mutedSections
                    FilterChip(
                        selected = on,
                        onClick = { actions.onMuteSection(key, on) },
                        label = { Text(tr(label)) },
                        shape = CircleShape,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(tr(R.string.idle_reminder), style = MaterialTheme.typography.titleMedium)
            Text(
                tr(R.string.idle_reminder_d),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (m in listOf(0, 15, 30, 60, 120)) {
                    FilterChip(
                        selected = extras.reminderMinutes == m,
                        onClick = { actions.onReminder(m) },
                        label = { Text(if (m == 0) tr(R.string.t_off) else if (m < 60) tr(R.string.t_minutes, m) else tr(R.string.t_hours, m / 60)) },
                        shape = CircleShape,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr(R.string.live_status), style = MaterialTheme.typography.titleMedium)
                    Text(
                        tr(R.string.live_status_d),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = extras.liveUpdate, onCheckedChange = actions.onLiveUpdate)
            }
            if (extras.liveUpdate) {
                Spacer(Modifier.height(10.dp))
                Text(tr(R.string.chip_shows), style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((mode, label) in listOf(0 to tr(R.string.t_countdown), 1 to tr(R.string.t_finish_time), 2 to tr(R.string.free_builders))) {
                        FilterChip(
                            selected = extras.liveChip == mode,
                            onClick = { actions.onLiveChip(mode) },
                            label = { Text(label) },
                            shape = CircleShape,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr(R.string.check_updates), style = MaterialTheme.typography.titleMedium)
                    Text(
                        tr(R.string.check_updates_d),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = extras.updateCheck, onCheckedChange = actions.onUpdateCheck)
            }

            Spacer(Modifier.height(20.dp))
            Text(tr(R.string.widget), style = MaterialTheme.typography.titleMedium)
            Text(
                tr(R.string.widget_help),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}
