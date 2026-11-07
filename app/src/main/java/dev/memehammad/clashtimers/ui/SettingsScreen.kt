package dev.memehammad.clashtimers.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.memehammad.clashtimers.AutoImport
import dev.memehammad.clashtimers.R
import dev.memehammad.clashtimers.tr
import androidx.compose.material.icons.Icons as MaterialIcons

/** A row's icon: a drawable from the app or a Material icon. */
private class RowIcon(val res: Int? = null, val vector: ImageVector? = null)

/** Settings in their own screen, grouped like Android's own settings (Material 3 Expressive). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    extras: ScreenExtras,
    actions: ScreenActions,
    autoImport: AutoImport.State,
    onAutoImport: () -> Unit,
    onBack: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    Surface(color = scheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.systemBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        ) {
            item {
                Row(Modifier.padding(top = 8.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(MaterialIcons.AutoMirrored.Filled.ArrowBack, tr(R.string.back))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(tr(R.string.settings_t), style = MaterialTheme.typography.headlineLarge)
                }
            }

            // --- Notifications ---
            item { GroupLabel(tr(R.string.notifications)) }
            item {
                SettingRow(RowIcon(vector = MaterialIcons.Filled.Notifications), tr(R.string.notify_about), tr(R.string.notify_about_d), 0, 4) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for ((key, label) in listOf(
                            "BUILDERS" to R.string.t_builders, "LAB" to R.string.t_lab, "PETS" to R.string.t_pet_house,
                            "BUILDER_BASE" to R.string.t_bb, "HELPERS" to R.string.t_helpers,
                        )) {
                            val on = key !in extras.mutedSections
                            FilterChip(selected = on, onClick = { actions.onMuteSection(key, on) }, label = { Text(tr(label)) }, shape = CircleShape)
                        }
                    }
                }
            }
            item {
                SettingRow(RowIcon(res = R.drawable.ic_hourglass), tr(R.string.idle_reminder), tr(R.string.idle_reminder_d), 1, 4) {
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
                }
            }
            item {
                SettingRow(
                    RowIcon(res = R.drawable.ic_hammer), tr(R.string.live_status), tr(R.string.live_status_d), 2, 4,
                    trailing = { Switch(checked = extras.liveUpdate, onCheckedChange = actions.onLiveUpdate) },
                ) {
                    if (extras.liveUpdate) {
                        Text(tr(R.string.chip_shows), style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for ((mode, label) in listOf(0 to tr(R.string.t_countdown), 1 to tr(R.string.t_finish_time), 2 to tr(R.string.free_builders))) {
                                FilterChip(selected = extras.liveChip == mode, onClick = { actions.onLiveChip(mode) }, label = { Text(label) }, shape = CircleShape)
                            }
                        }
                    }
                }
            }
            item {
                SettingRow(
                    RowIcon(res = R.drawable.ic_battery), tr(R.string.battery_t), tr(R.string.battery_d), 3, 4,
                    trailing = {
                        if (extras.batteryOk) {
                            Text(tr(R.string.battery_ok), style = MaterialTheme.typography.labelLarge, color = scheme.primary)
                        } else {
                            FilledTonalButton(onClick = actions.onBattery, shape = CircleShape) { Text(tr(R.string.t_allow)) }
                        }
                    },
                )
            }

            // --- Display ---
            item { GroupLabel(tr(R.string.g_display)) }
            item {
                SettingRow(RowIcon(vector = MaterialIcons.Filled.DateRange), tr(R.string.show_times_as), tr(R.string.show_times_d), 0, 3) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for ((mode, label) in listOf(0 to tr(R.string.t_countdown), 1 to tr(R.string.t_finish_time))) {
                            FilterChip(selected = extras.timeMode == mode, onClick = { actions.onTimeMode(mode) }, label = { Text(label) }, shape = CircleShape)
                        }
                    }
                }
            }
            item {
                SettingRow(RowIcon(vector = MaterialIcons.Filled.Edit), tr(R.string.wo_progress), null, 1, 3) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for ((style, label) in listOf(0 to tr(R.string.wavy), 1 to tr(R.string.straight), 2 to tr(R.string.t_off))) {
                            FilterChip(selected = extras.progressStyle == style, onClick = { actions.onProgressStyle(style) }, label = { Text(label) }, shape = CircleShape)
                        }
                    }
                }
            }
            item {
                SettingRow(RowIcon(vector = MaterialIcons.Filled.Warning), tr(R.string.wo_stale), tr(R.string.stale_hint_d), 2, 3) {
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
                }
            }

            // --- Import & widget ---
            item { GroupLabel(tr(R.string.g_import)) }
            item {
                val (title, body, action) = when (autoImport) {
                    AutoImport.State.SHIZUKU_MISSING -> Triple(tr(R.string.ai_title), tr(R.string.ai_missing), tr(R.string.get_shizuku))
                    AutoImport.State.NEEDS_PERMISSION -> Triple(tr(R.string.ai_title), tr(R.string.ai_perm), tr(R.string.t_allow))
                    AutoImport.State.OFF -> Triple(tr(R.string.ai_off_title), tr(R.string.ai_off), tr(R.string.turn_on))
                    AutoImport.State.ON -> Triple(tr(R.string.ai_on_title), tr(R.string.ai_on), tr(R.string.turn_off))
                }
                SettingRow(RowIcon(res = R.drawable.ic_bolt), title, body, 0, 2) {
                    FilledTonalButton(onClick = onAutoImport, shape = CircleShape) { Text(action) }
                }
            }
            item {
                SettingRow(RowIcon(vector = MaterialIcons.Filled.Home), tr(R.string.widget), tr(R.string.widget_help), 1, 2)
            }

            // --- About ---
            item { GroupLabel(tr(R.string.g_about)) }
            item {
                SettingRow(
                    RowIcon(vector = MaterialIcons.Filled.Refresh), tr(R.string.check_updates), tr(R.string.check_updates_d), 0, 3,
                    trailing = { Switch(checked = extras.updateCheck, onCheckedChange = actions.onUpdateCheck) },
                )
            }
            item {
                SettingRow(RowIcon(vector = MaterialIcons.Filled.Info), tr(R.string.how_it_works), null, 1, 3, onClick = actions.onTutorial)
            }
            item {
                SettingRow(
                    RowIcon(vector = MaterialIcons.Filled.Share), tr(R.string.on_github), tr(R.string.version_line, extras.version), 2, 3,
                    onClick = { uri.openUri(dev.memehammad.clashtimers.Updates.REPO_URL) },
                )
            }
            item {
                Text(
                    tr(R.string.not_affiliated),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp, start = 8.dp, end = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 20.dp, bottom = 8.dp),
    )
}

/**
 * One setting in a group. Rows of a group touch with small corners between them and big ones at
 * the group's ends, like Android 16's settings.
 */
@Composable
private fun SettingRow(
    icon: RowIcon,
    title: String,
    subtitle: String?,
    index: Int,
    count: Int,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    below: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val big = 24.dp
    val small = 4.dp
    val shape = RoundedCornerShape(
        topStart = if (index == 0) big else small, topEnd = if (index == 0) big else small,
        bottomStart = if (index == count - 1) big else small, bottomEnd = if (index == count - 1) big else small,
    )
    Surface(
        shape = shape,
        color = scheme.surfaceBright,
        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
            .let { if (onClick != null) it.clip(shape).clickable(onClick = onClick) else it },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.shape_cookie), null, tint = scheme.primaryContainer, modifier = Modifier.size(40.dp))
                    val tint = scheme.onPrimaryContainer
                    icon.res?.let { Icon(painterResource(it), null, tint = tint, modifier = Modifier.size(20.dp)) }
                    icon.vector?.let { Icon(it, null, tint = tint, modifier = Modifier.size(20.dp)) }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (subtitle != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    }
                }
                if (trailing != null) {
                    Spacer(Modifier.width(12.dp))
                    trailing()
                }
            }
            if (below != null) {
                Column(Modifier.padding(start = 56.dp, top = 12.dp)) { below() }
            }
        }
    }
}
