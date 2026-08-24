package org.tasks.wear.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import kotlinx.coroutines.launch
import org.tasks.wear.DataLayerSender
import org.tasks.wear.R
import org.tasks.wear.SnapshotStore
import org.tasks.wear.Task
import org.tasks.wear.databinding.ActivityTaskListBinding

/**
 * Main wear screen: scrollable list of open tasks with tap-to-complete.
 * Data comes from the phone over the DataLayer; taps are sent back the same way.
 */
class TaskListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskListBinding
    private lateinit var sender: DataLayerSender
    private val adapter = TaskAdapter(::onTaskTap)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.list.apply {
            layoutManager = WearableLinearLayoutManager(this@TaskListActivity)
            adapter = this@TaskListActivity.adapter
            isEdgeItemsCenteringEnabled = true
        }

        sender = DataLayerSender(this)
        sender.onTasksUpdated = { tasks ->
            runOnUiThread {
                adapter.submit(tasks)
                binding.empty.visibility =
                    if (tasks.isEmpty()) View.VISIBLE else View.GONE
            }
            SnapshotStore.save(applicationContext, tasks)
        }
        sender.onConnectionChanged = { connected ->
            if (!connected) runOnUiThread {
                binding.status.text = getString(R.string.disconnected)
            }
        }
        sender.start()

        // Hydrate from cache immediately, then ask for fresh data.
        adapter.submit(SnapshotStore.load(applicationContext))
        lifecycleScope.launch { sender.requestSnapshot() }
    }

    private fun onTaskTap(task: Task) {
        lifecycleScope.launch {
            val ok = sender.complete(task.id)
            Toast.makeText(
                this@TaskListActivity,
                getString(if (ok) R.string.completing else R.string.phone_unreachable, task.title),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sender.stop()
    }
}
