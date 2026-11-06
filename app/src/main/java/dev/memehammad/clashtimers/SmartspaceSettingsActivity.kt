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
                                tr(if (isCard) R.string.ss_card else R.string.ss_chip),
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
                                        Text(tr(R.string.ss_hidden), style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            tr(R.string.ss_hidden_d),
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

                        item { Label(tr(R.string.ss_show_any)) }
                        for ((k, label) in SmartOptions.WHAT) {
                            item {
                                Toggle(label, null, k in o.what) { on ->
                                    val n = if (on) o.what + k else o.what - k
                                    if (n.isNotEmpty()) save(o.copy(what = n))
                                }
                            }
                        }

                        item { Label(tr(R.string.ss_when)) }
                        item {
                            Toggle(tr(R.string.ss_always), tr(R.string.ss_always_d), o.show.isEmpty()) { on ->
                                save(o.copy(show = if (on) emptySet() else setOf("free")))
                            }
                        }
                        if (o.show.isNotEmpty()) {
                            item {
                                Text(
                                    tr(R.string.ss_any_true),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                                )
                            }
                            for ((k, label) in SmartOptions.SHOW) {
                                item {
                                    Toggle(
                                        label,
                                        if (k == "soon") tr(R.string.ss_within) else null,
                                        k in o.show,
                                    ) { on ->
                                        val n = if (on) o.show + k else o.show - k
                                        save(o.copy(show = n))
                                    }
                                }
                            }
                            if ("soon" in o.show) {
                                item { Label(tr(R.string.ss_how_long)) }
                                for (m in SmartOptions.SOON) {
                                    item {
                                        Option(if (m < 60) trq(R.plurals.p_minutes, m) else trq(R.plurals.p_hours, m / 60), o.soonMinutes == m) {
                                            save(o.copy(soonMinutes = m))
                                        }
                                    }
                                }
                            }
                        }

                        item { Label(tr(if (isCard) R.string.ss_second_line else R.string.ss_chip_text)) }
                        item {
                            Option(
                                tr(if (isCard) R.string.ss_next_when else R.string.ss_free_or_next),
                                o.text == "next",
                            ) { save(o.copy(text = "next")) }
                        }
                        item {
                            Option(tr(if (isCard) R.string.ss_how_many else R.string.ss_working_total), o.text == "count") {
                                save(o.copy(text = "count"))
                            }
                        }

                        if (isCard) {
                            item { Label(tr(R.string.ss_look)) }
                            item {
                                Option(tr(R.string.ss_plain), o.style == "plain") { save(o.copy(style = "plain")) }
                            }
                            item { Option(tr(R.string.ss_chips_look), o.style == "playful") { save(o.copy(style = "playful")) } }
                            item {
                                Text(
                                    tr(R.string.ss_playful_note),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp, end = 8.dp),
                                )
                            }
                            item { Label(tr(R.string.ss_extras)) }
                            if ("boost" !in o.what) {
                                item {
                                    Toggle(tr(R.string.ss_mention_boosts), tr(R.string.ss_mention_boosts_d), o.boostInTitle) {
                                        save(o.copy(boostInTitle = it))
                                    }
                                }
                            }
                            item {
                                Toggle(tr(R.string.ss_lock), tr(R.string.ss_lock_d), o.lockScreen) {
                                    save(o.copy(lockScreen = it))
                                }
                            }
                        }

                        if (tags.size > 1) {
                            item { Label(tr(R.string.t_village)) }
                            item { Option(tr(R.string.t_follow_app), o.village.isEmpty()) { save(o.copy(village = "")) } }
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
                            ) { Text(tr(R.string.t_done)) }
                        }
                    }
                }
            }
        }
    }
}
