package org.tasks.wear

import org.json.JSONArray
import org.json.JSONObject

/** Single source of truth for DataLayer paths and payload encoding. */
object Protocol {
    const val PATH_SNAPSHOT = "/tasks/snapshot"   // phone -> wear
    const val PATH_REQUEST = "/tasks/request"     // wear -> phone
    const val PATH_COMPLETE = "/tasks/complete"   // wear -> phone
    const val PATH_UNCOMPLETE = "/tasks/uncomplete" // wear -> phone

    const val KEY_ID = "id"

    fun encodeTasks(tasks: List<Task>): ByteArray {
        val arr = JSONArray()
        tasks.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                if (t.due != null) put("due", t.due)
                put("completed", t.completed)
            })
        }
        return arr.toString().toByteArray(Charsets.UTF_8)
    }

    fun decodeTasks(bytes: ByteArray): List<Task> {
        val arr = JSONArray(String(bytes, Charsets.UTF_8))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Task(
                id = o.getLong("id"),
                title = o.getString("title"),
                due = if (o.has("due")) o.getLong("due") else null,
                completed = o.optBoolean("completed", false),
            )
        }
    }

    fun encodeId(id: Long): ByteArray =
        JSONObject().put(KEY_ID, id).toString().toByteArray(Charsets.UTF_8)

    fun decodeId(bytes: ByteArray?): Long? =
        bytes?.let { JSONObject(String(it, Charsets.UTF_8)).optLong(KEY_ID) }
}

data class Task(
    val id: Long,
    val title: String,
    val due: Long?,
    val completed: Boolean = false,
)
