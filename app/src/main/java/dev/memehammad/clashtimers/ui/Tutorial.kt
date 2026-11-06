package dev.memehammad.clashtimers.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.memehammad.clashtimers.R
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons as MaterialIcons

private class Page(
    val shape: Int,
    val icon: Int? = null,
    val vector: ImageVector? = null,
    val title: String,
    val body: String,
    val openClash: Boolean = false,
)

private val pages = listOf(
    Page(
        R.drawable.shape_cookie, icon = R.drawable.ic_hammer,
        title = "Never miss a free builder",
        body = "Clash Timers shows when your builders, lab and pets are free, right on your home screen.",
    ),
    Page(
        R.drawable.shape_clover, vector = MaterialIcons.Filled.Settings,
        title = "Copy your export",
        body = "In Clash of Clans, open Settings, then More Settings, then Data Export, and tap Copy.",
        openClash = true,
    ),
    Page(
        R.drawable.shape_flower, vector = MaterialIcons.Filled.Check,
        title = "Come back here",
        body = "Clash Timers picks up the export by itself. Copy a new one after you start upgrades or use a potion.",
    ),
    Page(
        R.drawable.shape_sunny, vector = MaterialIcons.Filled.Home,
        title = "Add the widget",
        body = "Long-press your home screen, tap Widgets and find Clash Timers. You'll get a notification when something finishes.",
    ),
)

/** A short swipeable intro, shown on first launch and from "How it works". */
@Composable
fun Tutorial(onDone: () -> Unit) {
    val state = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val last = state.currentPage == pages.lastIndex
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "angle")
    val breathe by spin.animateFloat(0.94f, 1.04f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "breathe")

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.systemBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDone, enabled = !last) { Text(if (last) "" else "Skip") }
            }
            HorizontalPager(state, Modifier.weight(1f)) { i ->
                val p = pages[i]
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.size(220.dp).scale(breathe), contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(p.shape), null,
                            tint = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(220.dp).rotate(angle),
                        )
                        val tint = MaterialTheme.colorScheme.onPrimaryContainer
                        if (p.icon != null) Icon(painterResource(p.icon), null, tint = tint, modifier = Modifier.size(88.dp))
                        if (p.vector != null) Icon(p.vector, null, tint = tint, modifier = Modifier.size(88.dp))
                    }
                    Spacer(Modifier.height(40.dp))
                    Text(p.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        p.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(320.dp),
                    )
                    if (p.openClash) {
                        val launch = context.packageManager.getLaunchIntentForPackage("com.supercell.clashofclans")
                        if (launch != null) {
                            Spacer(Modifier.height(16.dp))
                            TextButton(onClick = { context.startActivity(launch) }) { Text("Open Clash of Clans") }
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(pages.size) { i ->
                    val on = i == state.currentPage
                    val w by animateDpAsState(if (on) 24.dp else 8.dp, label = "dot")
                    val c by animateColorAsState(
                        if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "dotc",
                    )
                    Box(Modifier.padding(horizontal = 4.dp).height(8.dp).width(w).clip(CircleShape).background(c))
                }
            }
            Button(
                onClick = { if (last) onDone() else scope.launch { state.animateScrollToPage(state.currentPage + 1) } },
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text(if (last) "Get started" else "Next", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

/** First-run setup: notifications, idle reminder and optional auto-import. Followed by [Tutorial]. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun Setup(
    notificationsOn: Boolean,
    onNotifications: () -> Unit,
    reminderMinutes: Int,
    onReminder: (Int) -> Unit,
    autoImport: dev.memehammad.clashtimers.AutoImport.State,
    onAutoImport: () -> Unit,
    onDone: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(color = scheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.systemBarsPadding().padding(horizontal = 16.dp, vertical = 16.dp)) {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f)) {
                item {
                    Column(Modifier.padding(start = 8.dp, top = 24.dp, bottom = 20.dp)) {
                        Text("Quick setup", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "A few choices, then a short tour. You can change all of this later.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    SetupCard(R.drawable.shape_cookie, R.drawable.ic_hammer, "Notifications", "Get told the moment a builder, the lab or a pet is free.") {
                        if (notificationsOn) {
                            Text("On", style = MaterialTheme.typography.titleMedium, color = scheme.primary)
                        } else {
                            Button(onClick = onNotifications, shape = CircleShape) { Text("Allow") }
                        }
                    }
                }
                item {
                    SetupCard(R.drawable.shape_clover, R.drawable.ic_hourglass, "Idle builder reminder", "Nudge me again if a builder sits free for a while.") {
                        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (m in listOf(0, 30, 60, 120)) {
                                androidx.compose.material3.FilterChip(
                                    selected = reminderMinutes == m,
                                    onClick = { onReminder(m) },
                                    label = { Text(if (m == 0) "Off" else if (m < 60) "$m min" else "${m / 60} h") },
                                    shape = CircleShape,
                                )
                            }
                        }
                    }
                }
                item {
                    val (label, text) = when (autoImport) {
                        dev.memehammad.clashtimers.AutoImport.State.SHIZUKU_MISSING -> "Get Shizuku" to "Optional. With the free Shizuku app, exports import by themselves when you tap Copy in Clash."
                        dev.memehammad.clashtimers.AutoImport.State.NEEDS_PERMISSION -> "Allow" to "Shizuku is installed. Allow it so exports import by themselves when you tap Copy in Clash."
                        dev.memehammad.clashtimers.AutoImport.State.OFF -> "Turn on" to "Exports import by themselves when you tap Copy in Clash."
                        dev.memehammad.clashtimers.AutoImport.State.ON -> "" to "On. Exports import by themselves when you tap Copy in Clash."
                    }
                    SetupCard(R.drawable.shape_flower, R.drawable.ic_bolt, "Auto-import", text) {
                        if (autoImport == dev.memehammad.clashtimers.AutoImport.State.ON) {
                            Text("On", style = MaterialTheme.typography.titleMedium, color = scheme.primary)
                        } else {
                            androidx.compose.material3.FilledTonalButton(onClick = onAutoImport, shape = CircleShape) { Text(label) }
                        }
                    }
                }
            }
            Button(
                onClick = onDone,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(56.dp),
            ) { Text("Continue", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
private fun SetupCard(shape: Int, icon: Int, title: String, text: String, action: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        color = scheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(painterResource(shape), null, tint = scheme.primaryContainer, modifier = Modifier.size(48.dp))
                Icon(painterResource(icon), null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                action()
            }
        }
    }
}
