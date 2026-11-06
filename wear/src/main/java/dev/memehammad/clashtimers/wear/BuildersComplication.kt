package dev.memehammad.clashtimers.wear

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.CountDownTimeReference
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import androidx.wear.watchface.complications.datasource.ComplicationDataTimeline
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingTimelineComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.TimeInterval
import androidx.wear.watchface.complications.datasource.TimelineEntry
import java.time.Instant

/** Free builders on the watch face: a number, or a countdown when all are busy. */
class BuildersComplication : SuspendingTimelineComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = data(type, 2, 6, null)

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationDataTimeline? {
        val type = request.complicationType
        val b = Summary.load(this)?.builders ?: return ComplicationDataTimeline(data(type, null, 0, null) ?: return null, emptyList())
        val now = System.currentTimeMillis()
        fun at(t: Long) = data(type, b.free(t), b.total, b.nextFree(t))
        val first = at(now) ?: return null
        // One entry per moment the count changes, so the face stays right without the phone.
        val points = listOf(now) + b.changes(now).take(12)
        val entries = points.zipWithNext().mapNotNull { (a, z) ->
            at(a + 1)?.let { TimelineEntry(TimeInterval(Instant.ofEpochMilli(a), Instant.ofEpochMilli(z)), it) }
        } + listOfNotNull(points.last().let { last ->
            at(last + 1)?.let { TimelineEntry(TimeInterval(Instant.ofEpochMilli(last), Instant.ofEpochMilli(last + 30L * 86_400_000)), it) }
        })
        return ComplicationDataTimeline(first, entries)
    }

    private fun data(type: ComplicationType, free: Int?, total: Int, next: Long?): ComplicationData? {
        val icon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_hammer)).build()
        val desc = PlainComplicationText.Builder(
            when {
                free == null -> "No village yet"
                free > 0 -> "$free builders free"
                else -> "All builders busy"
            },
        ).build()
        val text = when {
            free == null -> PlainComplicationText.Builder("–").build()
            free == 0 && next != null -> TimeDifferenceComplicationText.Builder(
                TimeDifferenceStyle.SHORT_SINGLE_UNIT, CountDownTimeReference(Instant.ofEpochMilli(next)),
            ).build()
            else -> PlainComplicationText.Builder("$free").build()
        }
        val title = PlainComplicationText.Builder(if (free != null && free > 0) "free" else "next").build()
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(text, desc)
                .setMonochromaticImage(icon)
                .setTitle(title)
                .build()
            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = (total - (free ?: total)).toFloat(), min = 0f, max = total.coerceAtLeast(1).toFloat(), contentDescription = desc,
            ).setText(text).setMonochromaticImage(icon).build()
            else -> null
        }
    }
}
