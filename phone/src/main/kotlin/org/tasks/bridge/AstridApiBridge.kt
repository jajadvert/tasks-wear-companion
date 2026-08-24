package org.tasks.bridge

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat

/**
 * Bridge to the Tasks.org (Astrid API) content provider.
 *
 * Tasks.org exposes its tasks through `org.tasks.content` (the historical
 * Astrid content provider API). This class wraps all provider access so the
 * rest of the app never touches raw ContentResolver calls.
 *
 * Required in the manifest:
 *   <uses-permission android:name="org.tasks.permission.READ_TASKS"/>
 *   <uses-permission android:name="org.tasks.permission.WRITE_TASKS"/>
 */
class AstridApiBridge(private val context: Context) {

    companion object {
        const val AUTHORITY = "org.tasks.content"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/tasks")

        const val COL_ID = "_id"
        const val COL_TITLE = "TITLE"
        const val COL_COMPLETED = "COMPLETED"
        const val COL_DUE = "DUE_DATE"
        const val COL_DELETED = "DELETED"

        private val PROJECTION = arrayOf(
            COL_ID, COL_TITLE, COL_COMPLETED, COL_DUE,
        )

        fun requiredPermissions(): Array<String> = arrayOf(
            "org.tasks.permission.READ_TASKS",
            "org.tasks.permission.WRITE_TASKS",
        )

        fun hasPermissions(context: Context): Boolean = requiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /** All open (not completed, not deleted) tasks. */
    fun openTasks(): List<TaskRow> {
        if (!hasPermissions(context)) return emptyList()
        val out = mutableListOf<TaskRow>()
        context.contentResolver.query(
            CONTENT_URI,
            PROJECTION,
            "${COL_COMPLETED} IS NULL OR ${COL_COMPLETED} <= 0",
            null,
            "$COL_DUE ASC",
        )?.use { c ->
            while (c.moveToNext()) {
                val due = if (c.isNull(3)) null else c.getLong(3)
            out.add(TaskRow(c.getLong(0), c.getString(1) ?: "", due))
            }
        }
        return out
    }

    /** Mark a task complete (sets COMPLETED to now). */
    fun complete(id: Long): Boolean = setCompleted(id, System.currentTimeMillis())

    /** Undo completion (clears COMPLETED). */
    fun uncomplete(id: Long): Boolean = setCompleted(id, 0L)

    private fun setCompleted(id: Long, whenMs: Long): Boolean {
        if (!hasPermissions(context)) return false
        val uri = ContentUris.withAppendedId(CONTENT_URI, id)
        val values = android.content.ContentValues().apply {
            put(COL_COMPLETED, whenMs)
        }
        return try {
            context.contentResolver.update(uri, values, null, null) > 0
        } catch (e: Exception) {
            false
        }
    }
}

data class TaskRow(val id: Long, val title: String, val due: Long?)
