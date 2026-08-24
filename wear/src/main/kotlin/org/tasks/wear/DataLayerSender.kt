package org.tasks.wear

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Wear-side DataLayer gateway.
 *
 * - Sends complete/uncomplete messages and snapshot requests to the phone node.
 * - Listens for snapshot [DataClient] changes and forwards parsed tasks to observers.
 */
class DataLayerSender(context: Context) : DataClient.OnDataChangedListener, MessageClient.OnMessageReceivedListener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val dataClient: DataClient = Wearable.getDataClient(appContext)
    private val messageClient: MessageClient = Wearable.getMessageClient(appContext)
    private val capabilityClient: CapabilityClient = Wearable.getCapabilityClient(appContext)
    private val nodeClient = Wearable.getNodeClient(appContext)

    var onTasksUpdated: ((List<Task>) -> Unit)? = null
    var onConnectionChanged: ((Boolean) -> Unit)? = null

    fun start() {
        dataClient.addListener(this)
        messageClient.addListener(this)
        capabilityClient.addListener(
            { info: CapabilityInfo ->
                onConnectionChanged?.invoke(info.nodes.isNotEmpty())
            },
            CAPABILITY_PHONE_APP,
        )
        refreshConnectionState()
    }

    fun stop() {
        dataClient.removeListener(this)
        messageClient.removeListener(this)
        scope.cancel()
    }

    suspend fun connectedPhoneNode(): Node? =
        try {
            val info = capabilityClient.getCapability(CAPABILITY_PHONE_APP, CapabilityClient.FILTER_REACHABLE).await()
            info.nodes.firstOrNull()
        } catch (e: Exception) {
            null
        }

    private fun refreshConnectionState() = scope.launch {
        onConnectionChanged?.invoke(connectedPhoneNode() != null)
    }

    /** Ask the phone to push a fresh snapshot. */
    suspend fun requestSnapshot(): Boolean = send(PATH_REQUEST, null)

    /** Mark a task complete on the phone via the Astrid ContentResolver. */
    suspend fun complete(id: Long): Boolean = send(PATH_COMPLETE, Protocol.encodeId(id))

    /** Undo completion on the phone. */
    suspend fun uncomplete(id: Long): Boolean = send(PATH_UNCOMPLETE, Protocol.encodeId(id))

    private suspend fun send(path: String, payload: ByteArray?): Boolean {
        val node = connectedPhoneNode() ?: return false
        return try {
            messageClient.sendMessage(node.id, path, payload).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // ---- listeners -------------------------------------------------------

    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == Protocol.PATH_SNAPSHOT
            ) {
                val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                val bytes = map.getByteArray(KEY_TASKS) ?: return@forEach
                scope.launch { onTasksUpdated?.invoke(Protocol.decodeTasks(bytes)) }
            }
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == Protocol.PATH_SNAPSHOT && event.data != null) {
            scope.launch { onTasksUpdated?.invoke(Protocol.decodeTasks(event.data)) }
        }
    }

    companion object {
        const val KEY_TASKS = "tasks"
        const val CAPABILITY_PHONE_APP = "org.tasks.phone.bridge"
    }
}
