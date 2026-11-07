package dev.memehammad.clashtimers.ui

import androidx.compose.animation.core.LinearEasing
import dev.memehammad.clashtimers.tr
import dev.memehammad.clashtimers.trq
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.memehammad.clashtimers.AutoImport
import dev.memehammad.clashtimers.Format
import dev.memehammad.clashtimers.Icons
import dev.memehammad.clashtimers.ProgressStyle
import dev.memehammad.clashtimers.R
import dev.memehammad.clashtimers.Section
import dev.memehammad.clashtimers.Slot
import dev.memehammad.clashtimers.Stale
import dev.memehammad.clashtimers.SlotStatus
import dev.memehammad.clashtimers.Status
import dev.memehammad.clashtimers.Timer
import dev.memehammad.clashtimers.Village
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun MainScreen(
    village: Village?,
    builderOverride: Int,
    snackbar: SnackbarHostState,
    onPaste: () -> Unit,
    onSetBuilders: (Int) -> Unit,
    autoImport: AutoImport.State = AutoImport.State.SHIZUKU_MISSING,
    onAutoImport: () -> Unit = {},
    extras: ScreenExtras = ScreenExtras(),
    actions: ScreenActions = ScreenActions(),
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    var editBuilders by remember { mutableStateOf(false) }
    val boosts = village?.let { Status.activeBoosts(it, now) }.orEmpty()

    BoostTheme(active = boosts.isNotEmpty()) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = pad.calculateTopPadding() + 24.dp,
                bottom = pad.calculateBottomPadding() + 32.dp,
            ),
        ) {
            item(key = "header") { Header(village, now, onPaste, extras.villages.firstOrNull { it.first == village?.tag }?.second, actions.onSettings) }
            if (extras.villages.size > 1) {
                item(key = "villages") { VillageBar(extras, actions) }
            }
            if (village != null && Stale.isStale(village, now)) {
                item(key = "stale") { StaleBanner(village, now, onPaste) }
            }
            extras.update?.let { v -> item(key = "update") { UpdateBanner(v) } }
            if (boosts.isNotEmpty()) {
                item(key = "boost") { BoostBanner(boosts, now) }
            }

            val builders = village?.let { Status.of(it, Slot.BUILDER, now, builderOverride) }
            if (village != null && builders != null) {
                item(key = "status") {
                    BuilderCard(builders, now, builderOverride > 0) { editBuilders = true }
                }
                item(key = "chips") { Chips(village, now) }
                if (village.heroes.isNotEmpty()) {
                    item(key = "heroes") { HeroesCard(village, now) }
                }
                item(key = "planner") { PlannerCard(village, now, builderOverride) }
            }

            val timers = village?.timers.orEmpty()
            if (timers.isEmpty()) {
                item(key = "empty") { EmptyCard(hasVillage = village != null) }
            } else {
                timers.groupBy { it.section }.forEach { (section, list) ->
                    item(key = "s_${section.name}") { SectionLabel(section.title) }
                    itemsIndexed(list, key = { _, t -> t.key }) { i, t ->
                        TimerRow(t, now, rowShape(i, list.size))
                    }
                }
            }
            item(key = "settings") {
                Box(Modifier.fillMaxWidth().padding(top = 28.dp), contentAlignment = Alignment.Center) {
                    FilledTonalButton(onClick = actions.onSettings, shape = CircleShape) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Settings, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(tr(R.string.settings_t))
                    }
                }
            }
        }
    }

    if (editBuilders && village != null) {
        BuildersDialog(
            detected = village.capacity[Slot.BUILDER] ?: 0,
            current = builderOverride,
            onSet = onSetBuilders,
            onDismiss = { editBuilders = false },
        )
    }
    }
}

/** Pill banner listing running boosts, with a pulsing bolt in a spinning "sunny" shape. */
@Composable
private fun BoostBanner(boosts: List<Timer>, now: Long) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "boost")
    val spin by transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(8_000, easing = LinearEasing), RepeatMode.Restart), label = "spin",
    )
    val pulse by transition.animateFloat(
        0.85f, 1.1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse",
    )
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = scheme.tertiary,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        Row(Modifier.padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Image(
                    painterResource(R.drawable.shape_sunny),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).graphicsLayer { rotationZ = spin },
                )
                Image(
                    painterResource(R.drawable.ic_bolt),
                    contentDescription = null,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(scheme.onSecondaryContainer),
                    modifier = Modifier.size(22.dp).graphicsLayer { scaleX = pulse; scaleY = pulse },
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    Status.boostLabel(boosts),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    tr(R.string.timers_faster),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onTertiary.copy(alpha = 0.8f),
                )
            }
            Text(
                Format.countdown(boosts.first().remainingMs(now)),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = scheme.onTertiary,
            )
        }
    }
}

@Composable
private fun Header(village: Village?, now: Long, onPaste: () -> Unit, name: String? = null, onSettings: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Clash Timers", style = MaterialTheme.typography.headlineMedium)
            if (village != null) {
                Text(
                    "${name ?: village.tag} · ${Format.ago(now - village.timestamp * 1000)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FilledTonalButton(onClick = onPaste, shape = CircleShape) { Text(tr(R.string.t_paste)) }
        Spacer(Modifier.width(4.dp))
        androidx.compose.material3.IconButton(onClick = onSettings) {
            Icon(androidx.compose.material.icons.Icons.Filled.Settings, tr(R.string.settings_t))
        }
    }
}

/** Nine-sided "cookie" shape from the Material 3 Expressive shape set. */
private object CookieShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = min(cx, cy).toDouble()
        val steps = 180
        for (i in 0..steps) {
            val a = 2.0 * PI * i / steps
            val rr = r * (0.93 + 0.07 * cos(9 * a))
            val x = cx + (rr * cos(a - PI / 2)).toFloat()
            val y = cy + (rr * sin(a - PI / 2)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

@Composable
private fun BuilderCard(s: SlotStatus, now: Long, manual: Boolean, onEdit: () -> Unit) {
    val free = s.free > 0
    val scheme = MaterialTheme.colorScheme
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = if (free) scheme.primaryContainer else scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(92.dp)
                        .graphicsLayer { rotationZ = if (free) spin else 0f }
                        .clip(CookieShape)
                        // Free: badge in the card's text colour, number in the card colour, so contrast
                        // holds in every palette (including the boost theme).
                        .background(if (free) scheme.onPrimaryContainer else scheme.surfaceContainerHighest),
                )
                Text(
                    s.free.toString(),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                    color = if (free) scheme.primaryContainer else scheme.onSurface,
                )
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                val onColor = if (free) scheme.onPrimaryContainer else scheme.onSurface
                Text(Status.builderTitle(s), style = MaterialTheme.typography.headlineSmall, color = onColor)
                val next = s.nextFreeSec
                Text(
                    if (!free && next != null) tr(R.string.next_free_x, Format.phrase(next * 1000 - now, now))
                    else Status.builderSubtitle(s),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                    color = onColor.copy(alpha = 0.8f),
                )
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                ) {
                    Text(
                        tr(R.string.n_builders, s.total) + tr(if (manual) R.string.set_suffix else R.string.edit_suffix),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (free) scheme.onPrimaryContainer.copy(alpha = 0.85f) else scheme.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun Chips(village: Village, now: Long) {
    val labels = mapOf(
        Slot.LAB to tr(R.string.t_lab),
        Slot.PET to tr(R.string.t_pet_house),
        Slot.BB_BUILDER to tr(R.string.t_bb),
        Slot.STAR_LAB to tr(R.string.t_star_lab_short),
    )
    val chips = Status.chips.mapNotNull { (slot, _) -> Status.of(village, slot, now)?.let { slot to it } }
    if (chips.isEmpty()) return
    BalancedGrid(chips, Modifier.padding(top = 6.dp)) { (slot, s), mod -> Chip(labels[slot] ?: "", s, now, mod) }
}

@Composable
private fun Chip(label: String, s: SlotStatus, now: Long, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val free = s.free > 0
    val onColor = if (free) scheme.onTertiaryContainer else scheme.onSurface
    Surface(
        // Shape changes with state: pill when idle, squarer when busy.
        shape = if (free) RoundedCornerShape(percent = 50) else RoundedCornerShape(12.dp),
        color = if (free) scheme.tertiaryContainer else scheme.surfaceContainer,
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = onColor.copy(alpha = 0.75f))
            val next = s.nextFreeSec
            Text(
                when {
                    free && s.total > 1 -> trq(R.plurals.p_free, s.free)
                    free -> tr(R.string.t_idle)
                    next != null -> Format.left(next * 1000 - now, now, short = true)
                    else -> tr(R.string.t_busy)
                },
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                color = onColor,
            )
        }
    }
}

@Composable
private fun BuildersDialog(detected: Int, current: Int, onSet: (Int) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(if (current > 0) current else detected.coerceAtLeast(1)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr(R.string.home_builders)) },
        text = {
            Column {
                Text(
                    tr(R.string.detected_builders, detected),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalButton(onClick = { if (value > 1) value-- }, shape = CircleShape) { Text("−") }
                    Text(
                        value.toString(),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 28.dp),
                    )
                    FilledTonalButton(onClick = { if (value < 9) value++ }, shape = CircleShape) { Text("+") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSet(value); onDismiss() }) { Text(tr(R.string.t_save)) }
        },
        dismissButton = {
            TextButton(onClick = { onSet(0); onDismiss() }) { Text(tr(R.string.auto)) }
        },
    )
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 28.dp, bottom = 8.dp),
    )
}

/** Grouped list: outer corners large, inner corners small. */
private fun rowShape(index: Int, count: Int): RoundedCornerShape {
    val big = 28.dp
    val small = 6.dp
    return when {
        count == 1 -> RoundedCornerShape(big)
        index == 0 -> RoundedCornerShape(topStart = big, topEnd = big, bottomStart = small, bottomEnd = small)
        index == count - 1 -> RoundedCornerShape(topStart = small, topEnd = small, bottomStart = big, bottomEnd = big)
        else -> RoundedCornerShape(small)
    }
}

@Composable
private fun TimerRow(t: Timer, now: Long, shape: RoundedCornerShape) {
    val left = t.remainingMs(now)
    val done = left <= 0
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = shape,
        color = if (done) scheme.secondaryContainer else scheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 20.dp, top = 14.dp, bottom = 14.dp)) {
          SectionIcon(t.section, Modifier.padding(top = 2.dp))
          Spacer(Modifier.width(14.dp))
          Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        t.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        t.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    if (done) tr(R.string.t_done) else Format.left(left, now),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    color = if (done) scheme.onSecondaryContainer else scheme.onSurface,
                )
            }
            val p = t.progress(now)
            if (p != null && !done && ProgressStyle.current != ProgressStyle.OFF) {
                Spacer(Modifier.height(12.dp))
                WavyProgress(
                    wavy = ProgressStyle.current == ProgressStyle.WAVY,
                    progress = p,
                    modifier = Modifier.fillMaxWidth(),
                    color = scheme.primary,
                    trackColor = scheme.surfaceContainerHighest,
                )
            }
          }
        }
    }
}

@Composable
private fun SectionIcon(section: Section, modifier: Modifier = Modifier) {
    val (shape, icon) = Icons.of(section)
    Box(modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Image(painterResource(shape), contentDescription = null, modifier = Modifier.size(40.dp))
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun EmptyCard(hasVillage: Boolean) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                tr(if (hasVillage) R.string.nothing_running_t else R.string.no_village_t),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                tr(R.string.empty_help),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Material 3 Expressive style wavy progress: an animated wave for the done part,
 * a straight track after a small gap, and a stop dot at the end.
 */
@Composable
private fun WavyProgress(progress: Float, modifier: Modifier, color: Color, trackColor: Color, wavy: Boolean = true) {
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Canvas(modifier.height(12.dp)) {
        val stroke = 3.dp.toPx()
        val amplitude = if (wavy) 2.5.dp.toPx() else 0f
        val wavelength = 22.dp.toPx()
        val gap = 6.dp.toPx()
        val taper = wavelength * 0.75f
        val cy = size.height / 2f
        val start = stroke / 2f
        val endX = size.width - stroke / 2f
        val waveEnd = (size.width * progress.coerceIn(0f, 1f)).coerceIn(start, endX)

        if (waveEnd > start + 1f) {
            val path = Path()
            var x = start
            // Wave flattens towards the head, like the media player's squiggle.
            fun y(px: Float): Float {
                val fade = ((waveEnd - px) / taper).coerceIn(0f, 1f)
                return cy + amplitude * fade * sin(((px / wavelength) - phase) * 2f * PI.toFloat())
            }
            path.moveTo(x, y(x))
            while (x < waveEnd) {
                x = minOf(x + 1.5f, waveEnd)
                path.lineTo(x, y(x))
            }
            drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }

        val trackStart = waveEnd + gap + stroke
        if (trackStart < endX) {
            drawLine(trackColor, Offset(trackStart, cy), Offset(endX, cy), strokeWidth = stroke, cap = StrokeCap.Round)
        }
        drawCircle(color, radius = stroke / 2f, center = Offset(endX, cy))
    }
}

/** Optional auto-import via Shizuku: tapping Copy in Clash is enough, no need to open the app. */
@Composable
private fun AutoImportCard(state: AutoImport.State, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val (title, body, action) = when (state) {
        AutoImport.State.SHIZUKU_MISSING -> Triple(tr(R.string.ai_title), tr(R.string.ai_missing), tr(R.string.get_shizuku))
        AutoImport.State.NEEDS_PERMISSION -> Triple(tr(R.string.ai_title), tr(R.string.ai_perm), tr(R.string.t_allow))
        AutoImport.State.OFF -> Triple(tr(R.string.ai_off_title), tr(R.string.ai_off), tr(R.string.turn_on))
        AutoImport.State.ON -> Triple(tr(R.string.ai_on_title), tr(R.string.ai_on), tr(R.string.turn_off))
    }
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = if (state == AutoImport.State.ON) scheme.secondaryContainer else scheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(onClick = onClick, shape = CircleShape) { Text(action) }
        }
    }
}
