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
