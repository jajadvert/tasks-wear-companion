package org.tasks.wear

import android.content.Context
import org.json.JSONArray

/** Tiny on-disk cache of the last received snapshot (JSON in a private file). */
object SnapshotStore {
    private const val FILE = "snapshot.json"

    fun save(context: Context, tasks: List<Task>) {
        context.openFileOutput(FILE, Context.MODE_PRIVATE).use { out ->
            out.write(Protocol.encodeTasks(tasks))
        }
    }

    fun load(context: Context): List<Task> = runCatching {
        val bytes = context.openFileInput(FILE).use { it.readBytes() }
        Protocol.decodeTasks(bytes)
    }.getOrDefault(emptyList())
}
