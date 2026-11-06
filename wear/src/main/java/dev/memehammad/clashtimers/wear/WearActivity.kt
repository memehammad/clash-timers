package dev.memehammad.clashtimers.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

class WearActivity : ComponentActivity() {
    private val summary = mutableStateOf<Summary?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        summary.value = Summary.load(this)
        setContent {
            MaterialTheme {
                LaunchedEffect(Unit) { pull() }
                Screen(summary.value)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        summary.value = Summary.load(this)
    }

    /** In case an update arrived while the listener wasn't running. */
    private suspend fun pull() {
        try {
            val items = Wearable.getDataClient(this).dataItems.await()
            for (item in items) {
                if (item.uri.path != DataListener.PATH) continue
                DataMapItem.fromDataItem(item).dataMap.getString("json")?.let {
                    Summary.save(this, it)
                    summary.value = Summary.load(this)
                    DataListener.refresh(this)
                }
            }
            items.release()
        } catch (e: Exception) {
            // No Play services or no phone: keep what we have.
        }
    }
}

@Composable
private fun Screen(s: Summary?) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val state = rememberScalingLazyListState(initialCenterItemIndex = 0)
    val scheme = MaterialTheme.colorScheme
    AppScaffold {
        ScreenScaffold(scrollState = state) {
            ScalingLazyColumn(state = state, modifier = Modifier.fillMaxWidth()) {
                if (s == null) {
                    item { Hero(null, 0, null, now) }
                    item {
                        Text(
                            "Copy your village export on your phone and open Clash Timers there. It shows up here by itself.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                    return@ScalingLazyColumn
                }
                val b = s.builders
                item { Hero(b?.free(now), b?.total ?: 0, b?.nextFree(now), now) }
                val others = s.slots.filter { it.key != "BUILDER" }
                items(others.size) { i ->
                    val o = others[i]
                    val next = o.nextFree(now)
                    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                        Text(o.label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                        Text(
                            if (next == null) "${o.free(now)} free" else shortTime(next - now),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (next == null) scheme.primary else scheme.onSurface,
                        )
                    }
                }
                val jobs = s.jobs.filter { it.endMs > now }.sortedBy { it.endMs }.take(25)
                if (jobs.isNotEmpty()) item { ListHeader { Text("Up next") } }
                items(jobs.size) { i ->
                    val j = jobs[i]
                    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                        Text(j.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row {
                            Text(
                                j.detail, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(shortTime(j.endMs - now), style = MaterialTheme.typography.bodySmall, color = scheme.primary)
                        }
                    }
                }
                item {
                    Text(
                        "Exported " + shortTime(now - s.exportedMs) + " ago",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

/** A slowly turning cookie shape with the free-builder count. */
@Composable
private fun Hero(free: Int?, total: Int, next: Long?, now: Long) {
    val scheme = MaterialTheme.colorScheme
    val spin = androidx.compose.animation.core.rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(
        0f, 360f,
        androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(30_000, easing = androidx.compose.animation.core.LinearEasing),
        ),
        label = "angle",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(R.drawable.shape_cookie), null,
                tint = if (free != null && free > 0) scheme.primaryContainer else scheme.surfaceContainerHigh,
                modifier = Modifier.size(96.dp).rotate(angle),
            )
            if (free == null) {
                Icon(painterResource(R.drawable.ic_hammer), null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(40.dp))
            } else {
                Text("$free", style = MaterialTheme.typography.displayMedium, color = scheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                free == null -> "No village yet"
                free == 1 -> "1 builder free"
                free > 0 -> "$free builders free"
                next != null -> "Next in ${shortTime(next - now)}"
                else -> "All $total busy"
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}
