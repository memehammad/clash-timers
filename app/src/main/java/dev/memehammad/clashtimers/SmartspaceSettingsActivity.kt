package dev.memehammad.clashtimers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kieronquinn.app.smartspacer.sdk.SmartspacerConstants
import dev.memehammad.clashtimers.ui.AppTheme

/** Settings for one Smartspacer card or chip, opened from Smartspacer. */
class SmartspaceSettingsActivity : ComponentActivity() {
    companion object {
        const val EXTRA_KIND = "kind"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val id = intent.getStringExtra(SmartspacerConstants.EXTRA_SMARTSPACER_ID)
        if (id == null) {
            finish()
            return
        }
        setResult(RESULT_OK)
        val kind = intent.getStringExtra(EXTRA_KIND) ?: "target"
        val isCard = kind == "target"
        val default = SmartOptions(what = setOf(kind.substringAfter("chip:", "builder").ifEmpty { "builder" }))
        val tags = Store.tags(this)

        setContent {
            AppTheme {
                var o by remember { mutableStateOf(Smartspace.options(this, id, default)) }
                fun save(n: SmartOptions) {
                    o = n
                    Smartspace.setOptions(this, id, n)
                }
                val now = System.currentTimeMillis()
                val preview = Smartspace.content(this, o, now)

                Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        Modifier.systemBarsPadding(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp),
                    ) {
                        item {
                            Text(
                                if (isCard) "At a Glance card" else "At a Glance chip",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
                            )
                        }
                        // What it shows right now.
                        item {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            ) {
                                androidx.compose.foundation.layout.Column(Modifier.padding(20.dp)) {
                                    if (preview == null) {
                                        Text("Hidden right now", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Nothing matches these settings at the moment; it appears when something does.",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    } else if (isCard) {
                                        Text(preview.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            if (o.text == "count" && preview.items.size > 1) preview.items.joinToString(" · ") { it.count } else preview.subtitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    } else {
                                        Text(preview.items.joinToString("    ") { it.chip }, style = MaterialTheme.typography.titleLarge)
                                    }
                                }
                            }
                        }

                        item { Label("Show (pick any)") }
                        for ((k, label) in SmartOptions.WHAT) {
                            item {
                                Toggle(label, null, k in o.what) { on ->
                                    val n = if (on) o.what + k else o.what - k
                                    if (n.isNotEmpty()) save(o.copy(what = n))
                                }
                            }
                        }

                        item { Label("When") }
                        item {
                            Toggle("Always", "Ignore the conditions below", o.show.isEmpty()) { on ->
                                save(o.copy(show = if (on) emptySet() else setOf("free")))
                            }
                        }
                        if (o.show.isNotEmpty()) {
                            item {
                                Text(
                                    "Shows when any of these is true:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                                )
                            }
                            for ((k, label) in SmartOptions.SHOW) {
                                item {
                                    Toggle(
                                        label,
                                        if (k == "soon") "Within the time below" else null,
                                        k in o.show,
                                    ) { on ->
                                        val n = if (on) o.show + k else o.show - k
                                        save(o.copy(show = n))
                                    }
                                }
                            }
                            if ("soon" in o.show) {
                                item { Label("How long before") }
                                for (m in SmartOptions.SOON) {
                                    item {
                                        Option(if (m < 60) "$m minutes" else if (m == 60) "1 hour" else "${m / 60} hours", o.soonMinutes == m) {
                                            save(o.copy(soonMinutes = m))
                                        }
                                    }
                                }
                            }
                        }

                        item { Label(if (isCard) "Second line" else "Chip text") }
                        item {
                            Option(
                                if (isCard) "What finishes next, and when" else "Free count, or time until the next one",
                                o.text == "next",
                            ) { save(o.copy(text = "next")) }
                        }
                        item {
                            Option(if (isCard) "How many are working" else "Working / total (e.g. 4/6)", o.text == "count") {
                                save(o.copy(text = "count"))
                            }
                        }

                        if (isCard) {
                            item { Label("Look") }
                            item {
                                Option("Plain: like other At a Glance cards", o.style == "plain") { save(o.copy(style = "plain")) }
                            }
                            item { Option("Chips: coloured chips with a dot per builder (Android 16)", o.style == "playful") { save(o.copy(style = "playful")) } }
                            item {
                                Text(
                                    "The playful look needs Android 16 (or the Smartspacer widget); elsewhere you get the plain card.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp, end = 8.dp),
                                )
                            }
                            item { Label("Extras") }
                            if ("boost" !in o.what) {
                                item {
                                    Toggle("Mention boosts", "Add a running potion or Clock Tower to the title", o.boostInTitle) {
                                        save(o.copy(boostInTitle = it))
                                    }
                                }
                            }
                            item {
                                Toggle("Lock screen", "Also show on the lock screen", o.lockScreen) {
                                    save(o.copy(lockScreen = it))
                                }
                            }
                        }

                        if (tags.size > 1) {
                            item { Label("Village") }
                            item { Option("Follow the app's selection", o.village.isEmpty()) { save(o.copy(village = "")) } }
                            for (t in tags) {
                                item {
                                    val name = Store.name(this@SmartspaceSettingsActivity, t)
                                    Option(if (name == t) t else "$name  ·  $t", o.village == t) { save(o.copy(village = t)) }
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = { finish() },
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(52.dp),
                            ) { Text("Done") }
                        }
                    }
                }
            }
        }
    }
}
