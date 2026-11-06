package dev.memehammad.clashtimers.wear

import android.content.ComponentName
import android.content.Context
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/** Receives the village summary from the phone and refreshes the tile and complication. */
class DataListener : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        for (e in events) {
            if (e.type != DataEvent.TYPE_CHANGED || e.dataItem.uri.path != PATH) continue
            val json = DataMapItem.fromDataItem(e.dataItem).dataMap.getString("json") ?: continue
            Summary.save(this, json)
            refresh(this)
        }
    }

    companion object {
        const val PATH = "/village"

        fun refresh(context: Context) {
            TileService.getUpdater(context).requestUpdate(BuildersTile::class.java)
            ComplicationDataSourceUpdateRequester
                .create(context, ComponentName(context, BuildersComplication::class.java))
                .requestUpdateAll()
        }
    }
}
