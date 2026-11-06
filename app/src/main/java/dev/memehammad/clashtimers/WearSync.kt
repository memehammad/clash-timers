package dev.memehammad.clashtimers

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import org.json.JSONArray
import org.json.JSONObject

/** Sends a summary of the current village to the Wear OS app (experimental). */
object WearSync {
    private const val PATH = "/village"

    private val labels = listOf(
        Slot.BUILDER to "Builders",
        Slot.LAB to "Lab",
        Slot.PET to "Pets",
        Slot.STAR_LAB to "Star Lab",
        Slot.BB_BUILDER to "Builder Base",
    )

    fun push(context: Context) {
        val v = Store.load(context) ?: return
        val override = Store.builderOverride(context, v.tag)
        val slots = JSONArray()
        for ((slot, label) in labels) {
            val total = if (slot == Slot.BUILDER && override > 0) override else v.capacity[slot] ?: continue
            if (total <= 0) continue
            val ends = JSONArray()
            v.timers.filter { (it.slot == slot && !it.extra) || it.blocks == slot }.forEach { ends.put(it.endSec * 1000) }
            slots.put(JSONObject().put("key", slot.name).put("label", label).put("total", total).put("ends", ends))
        }
        val jobs = JSONArray()
        v.timers.filter { it.slot != Slot.NONE || it.isBoost }.sortedBy { it.endSec }.take(60).forEach { t ->
            jobs.put(
                JSONObject().put("name", t.name).put("detail", t.detail).put("end", t.endSec * 1000)
                    .put("section", t.section.name).put("extra", t.extra),
            )
        }
        val json = JSONObject()
            .put("tag", v.tag).put("name", Store.name(context, v.tag)).put("exported", v.timestamp * 1000)
            .put("slots", slots).put("jobs", jobs)
            .toString()
        try {
            val req = PutDataMapRequest.create(PATH).apply {
                dataMap.putString("json", json)
                dataMap.putLong("sent", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(req).addOnFailureListener { }
        } catch (e: Throwable) {
            // No Google Play services: nothing to sync to.
        }
    }
}
