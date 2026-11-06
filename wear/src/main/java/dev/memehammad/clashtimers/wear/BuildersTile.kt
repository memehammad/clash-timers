package dev.memehammad.clashtimers.wear

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.degrees
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Arc
import androidx.wear.protolayout.LayoutElementBuilders.ArcLine
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.text.DateFormat
import java.util.Date

/** Tile: a gold ring of busy builders around the free count, plus lab and pets. */
class BuildersTile : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val s = Summary.load(this)
        val now = System.currentTimeMillis()
        val timeline = TimelineBuilders.Timeline.Builder()
        if (s == null) {
            timeline.addTimelineEntry(entry(empty(), null, null))
        } else {
            // A layout per stretch between changes, so the tile stays right on its own.
            val points = listOf(now) + s.changes(now).take(12)
            points.forEachIndexed { i, start ->
                val end = points.getOrNull(i + 1)
                timeline.addTimelineEntry(entry(layout(s, start + 1), if (i == 0) null else start, end))
            }
        }
        return Futures.immediateFuture(
            TileBuilders.Tile.Builder()
                .setResourcesVersion("1")
                .setFreshnessIntervalMillis(30 * 60_000L)
                .setTileTimeline(timeline.build())
                .build(),
        )
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(ResourceBuilders.Resources.Builder().setVersion("1").build())

    private fun entry(root: LayoutElementBuilders.LayoutElement, start: Long?, end: Long?): TimelineBuilders.TimelineEntry {
        val b = TimelineBuilders.TimelineEntry.Builder()
            .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(root).build())
        if (start != null || end != null) {
            val v = TimelineBuilders.TimeInterval.Builder()
            start?.let { v.setStartMillis(it) }
            end?.let { v.setEndMillis(it) }
            b.setValidity(v.build())
        }
        return b.build()
    }

    private fun text(s: String, size: Float, color: Int, bold: Boolean = false): Text =
        Text.Builder().setText(s).setMaxLines(1).setFontStyle(
            FontStyle.Builder().setSize(sp(size)).setColor(argb(color))
                .setWeight(if (bold) LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_NORMAL)
                .build(),
        ).build()

    private fun openApp(): ModifiersBuilders.Modifiers = ModifiersBuilders.Modifiers.Builder()
        .setClickable(
            ModifiersBuilders.Clickable.Builder().setId("open").setOnClick(
                ActionBuilders.LaunchAction.Builder().setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(packageName)
                        .setClassName(WearActivity::class.java.name)
                        .build(),
                ).build(),
            ).build(),
        ).build()

    private fun empty(): LayoutElementBuilders.LayoutElement =
        Box.Builder().setWidth(expand()).setHeight(expand()).setModifiers(openApp())
            .addContent(
                Column.Builder()
                    .addContent(text("Clash Timers", 16f, GOLD, bold = true))
                    .addContent(Spacer.Builder().setHeight(dp(6f)).build())
                    .addContent(text("Copy an export", 14f, ON))
                    .addContent(text("on your phone", 14f, ON))
                    .build(),
            ).build()

    private fun layout(s: Summary, t: Long): LayoutElementBuilders.LayoutElement {
        val b = s.builders
        val total = b?.total ?: 0
        val busy = b?.busy(t) ?: 0
        val free = b?.free(t) ?: 0
        val next = b?.nextFree(t)
        val ring = Arc.Builder()
            .setAnchorAngle(degrees(0f))
            .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
            .addContent(ArcLine.Builder().setLength(degrees(360f)).setThickness(dp(8f)).setColor(argb(TRACK)).build())
            .build()
        val fill = Arc.Builder()
            .setAnchorAngle(degrees(0f))
            .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
            .addContent(
                ArcLine.Builder()
                    .setLength(degrees(if (total > 0) 360f * busy / total else 0f))
                    .setThickness(dp(8f)).setColor(argb(GOLD)).build(),
            ).build()

        val col = Column.Builder()
            .addContent(text("$free", 54f, ON, bold = true))
            .addContent(text(if (free == 1) "builder free" else "builders free", 14f, MUTED))
        if (free == 0 && next != null) {
            col.addContent(Spacer.Builder().setHeight(dp(4f)).build())
            col.addContent(text("next " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(next)), 14f, GOLD, bold = true))
        }
        // Lab and pets in one line.
        val others = s.slots.filter { it.key == "LAB" || it.key == "PET" }.joinToString("  ·  ") { o ->
            val n = o.nextFree(t)
            o.label + " " + if (n == null) "free" else DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(n))
        }
        if (others.isNotEmpty()) {
            col.addContent(Spacer.Builder().setHeight(dp(8f)).build())
            col.addContent(text(others, 12f, MUTED))
        }
        return Box.Builder().setWidth(expand()).setHeight(expand()).setModifiers(openApp())
            .addContent(ring).addContent(fill).addContent(col.build())
            .build()
    }

    companion object {
        private const val GOLD = 0xFFFFC23D.toInt()
        private const val TRACK = 0xFF4A3A9E.toInt()
        private const val ON = 0xFFFFFFFF.toInt()
        private const val MUTED = 0xFFCAC4F0.toInt()
    }
}
