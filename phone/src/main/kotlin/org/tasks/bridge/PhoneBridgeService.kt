package org.tasks.bridge

import android.content.Intent
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/**
 * Phone-side DataLayer endpoint. Answers wear requests by talking to the
 * Tasks.org provider through [AstridApiBridge], and can push snapshots.
 */
class PhoneBridgeService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bridge: AstridApiBridge by lazy { AstridApiBridge(this) }
    private val messageClient by lazy { com.google.android.gms.wearable.Wearable.getMessageClient(this) }

    override fun onMessageReceived(event: MessageEvent) {
        scope.launch {
            when (event.path) {
                Protocol.PATH_REQUEST -> pushSnapshot()
                Protocol.PATH_COMPLETE -> {
                    val id = idFrom(event.data) ?: return@launch
                    if (bridge.complete(id)) pushSnapshot()
                }
                Protocol.PATH_UNCOMPLETE -> {
                    val id = idFrom(event.data) ?: return@launch
                    if (bridge.uncomplete(id)) pushSnapshot()
                }
            }
        }
    }

    /** Push the current open-task list to all connected wear nodes. */
    fun pushSnapshot() {
        val tasks = bridge.openTasks().map { Task(it.id, it.title, it.due) }
        val payload = Protocol.encodeTasks(tasks)
        try {
            val nodes = com.google.android.gms.wearable.Wearable
                .getNodeClient(this).connectedNodes.await()
            nodes.forEach { node ->
                messageClient.sendMessage(node.id, Protocol.PATH_SNAPSHOT, payload).await()
            }
        } catch (_: Exception) {
            // No wear node connected; nothing to do.
        }
    }

    private fun idFrom(data: ByteArray?): Long? =
        data?.let { runCatching { JSONObject(String(it)).optLong(Protocol.KEY_ID, -1) }.getOrNull()?.takeIf { v -> v >= 0 } }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    data class Task(val id: Long, val title: String, val due: Long?)
}
