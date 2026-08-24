package org.tasks.bridge

import org.json.JSONArray
import org.json.JSONObject

/** Shared DataLayer protocol constants and (de)serialization for the phone side. */
object Protocol {
    const val PATH_SNAPSHOT = "/tasks/snapshot"
    const val PATH_REQUEST = "/tasks/request"
    const val PATH_COMPLETE = "/tasks/complete"
    const val PATH_UNCOMPLETE = "/tasks/uncomplete"

    const val KEY_ID = "id"

    fun encodeTasks(tasks: List<TaskRow>): ByteArray {
        val arr = JSONArray()
        tasks.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                if (t.due != null) put("due", t.due)
            })
        }
        return arr.toString().toByteArray(Charsets.UTF_8)
    }
}
