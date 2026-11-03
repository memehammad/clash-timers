package dev.mathias.clashtimers

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mathias.clashtimers.ui.AppTheme

/**
 * Widget settings (long-press the widget → settings, or shown when adding one if the launcher
 * asks): which village it shows and what's on it. Changes apply live.
 */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        // Adding the widget still succeeds if this screen is just closed.
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))

        val tags = Store.tags(this)
        setContent {
            AppTheme {
                var tag by remember { mutableStateOf(Store.widgetTag(this, widgetId)) }
                var opts by remember { mutableStateOf(Store.widgetOptions(this, widgetId)) }

                fun saveTag(t: String?) {
                    tag = t
                    Store.setWidgetTag(this, widgetId, t)
                    WidgetProvider.update(this, widgetId)
                }

                fun saveOpts(o: WidgetOptions) {
                    opts = o
                    Store.setWidgetOptions(this, widgetId, o)
                    WidgetProvider.update(this, widgetId)
                }

                Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        Modifier.systemBarsPadding(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp),
                    ) {
                        item {
                            Text(
                                "Widget settings",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
                            )
                        }

                        if (tags.size > 1) {
                            item { Label("Village") }
                            item { Option("Follow the app's selection", tag == null) { saveTag(null) } }
                            for (t in tags) {
                                item {
                                    val name = Store.name(this@WidgetConfigActivity, t)
                                    Option(if (name == t) t else "$name  ·  $t", tag == t) { saveTag(t) }
                                }
                            }
                        }

                        item { Label("Show on the widget") }
                        for ((flag, label, desc) in WidgetOptions.ITEMS) {
                            item {
                                Toggle(label, desc, opts.has(flag)) { saveOpts(opts.with(flag, it)) }
                            }
                        }
                        item {
                            Text(
                                "Small sizes only show the builder badge, so most of these apply from 3x2 up.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
                            )
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

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 8.dp),
    )
}

@Composable
private fun Option(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun Toggle(label: String, desc: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp).clickable { onChange(!checked) },
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                if (desc != null) {
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}
