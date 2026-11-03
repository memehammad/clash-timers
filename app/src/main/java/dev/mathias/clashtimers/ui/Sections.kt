package dev.mathias.clashtimers.ui

import androidx.compose.foundation.clickable
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
import dev.mathias.clashtimers.Format
import dev.mathias.clashtimers.Planner
import dev.mathias.clashtimers.Stale
import dev.mathias.clashtimers.Village

/** Data for the extra sections, kept apart from the core screen parameters. */
data class ScreenExtras(
    /** (tag, display name) of every saved village. */
    val villages: List<Pair<String, String>> = emptyList(),
    val currentTag: String? = null,
    val reminderMinutes: Int = 0,
    val liveUpdate: Boolean = false,
)

data class ScreenActions(
    val onSelectVillage: (String) -> Unit = {},
    val onRenameVillage: (String, String) -> Unit = { _, _ -> },
    val onRemoveVillage: (String) -> Unit = {},
    val onReminder: (Int) -> Unit = {},
    val onLiveUpdate: (Boolean) -> Unit = {},
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
                        label = { Text("Name") },
                        placeholder = { Text("e.g. Main, Alt") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { actions.onRemoveVillage(tag); manage = null }) {
                        Text("Remove this village", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { actions.onRenameVillage(tag, text); manage = null }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { manage = null }) { Text("Cancel") } },
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
                    "Timers may be out of date. Copy a new export in Clash.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onErrorContainer.copy(alpha = 0.8f),
                )
            }
            TextButton(onClick = onPaste) { Text("Paste", color = scheme.onErrorContainer) }
        }
    }
}

// --- Heroes --------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HeroesCard(village: Village, now: Long) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.padding(top = 6.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (h in village.heroes) {
                val ready = h.ready(now)
                Surface(
                    shape = if (ready) CircleShape else RoundedCornerShape(12.dp),
                    color = if (ready) scheme.secondaryContainer else scheme.surfaceContainer,
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            "${h.name} ${h.level}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (ready) scheme.onSecondaryContainer else scheme.onSurface,
                        )
                        Text(
                            if (ready) "Ready" else "Back in ${Format.countdown(h.backSec!! * 1000 - now)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = (if (ready) scheme.onSecondaryContainer else scheme.onSurfaceVariant),
                        )
                    }
                }
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
                    Text("Boost planner", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "What a potion or snack would do right now",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Text(if (open) "Hide" else "Show", style = MaterialTheme.typography.labelLarge, color = scheme.primary)
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
                        "Nothing running that it would speed up.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    else -> {
                        val finishing = plan.rows.count { it.duringBoost }
                        val worker = when (item) {
                            Planner.Item.BUILDER_POTION, Planner.Item.BUILDER_BITE -> "builder"
                            Planner.Item.PET_POTION -> "Pet House"
                            else -> "Laboratory"
                        }
                        val before = plan.nextFreeBefore
                        val after = plan.nextFreeAfter
                        Text(
                            if (finishing == 0) "Nothing finishes during the boost."
                            else "$finishing finish${if (finishing == 1) "es" else ""} during the boost.",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (before != null && after != null) {
                            Text(
                                "Next $worker free in ${Format.countdown(after * 1000 - now)} " +
                                    "instead of ${Format.countdown(before * 1000 - now)}",
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
                                    Format.countdown(r.oldEndSec * 1000 - now),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                    color = scheme.onSurfaceVariant,
                                )
                                Text("  →  ", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                                Text(
                                    if (r.duringBoost) "done" else Format.countdown(r.newEndSec * 1000 - now),
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
            Text("Idle builder reminder", style = MaterialTheme.typography.titleMedium)
            Text(
                "Notify again if a builder stays free for a while.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (m in listOf(0, 15, 30, 60, 120)) {
                    FilterChip(
                        selected = extras.reminderMinutes == m,
                        onClick = { actions.onReminder(m) },
                        label = { Text(if (m == 0) "Off" else if (m < 60) "$m min" else "${m / 60} h") },
                        shape = CircleShape,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Live builder status", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Ongoing notification counting down to the next free builder. " +
                            "On Android 16 it shows as a Live Update chip in the status bar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = extras.liveUpdate, onCheckedChange = actions.onLiveUpdate)
            }
        }
    }
}
