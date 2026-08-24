package org.tasks.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives DataLayer messages targeted at the wear app even when no activity is
 * running. Snapshot pushes arrive either here (message channel) or through the
 * [com.google.android.gms.wearable.DataClient] listener inside [DataLayerSender].
 */
class WearListenerService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != Protocol.PATH_SNAPSHOT || event.data == null) return
        // Persist the latest snapshot so the list activity can hydrate instantly.
        val tasks = runCatching { Protocol.decodeTasks(event.data) }.getOrNull() ?: return
        SnapshotStore.save(applicationContext, tasks)
    }
}
