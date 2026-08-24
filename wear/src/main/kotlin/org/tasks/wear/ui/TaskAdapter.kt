package org.tasks.wear.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.tasks.wear.Task
import org.tasks.wear.databinding.ItemTaskBinding
import java.text.DateFormat
import java.util.Date

class TaskAdapter(private val onTap: (Task) -> Unit) :
    RecyclerView.Adapter<TaskAdapter.ViewHolder>() {

    private val items = mutableListOf<Task>()

    fun submit(tasks: List<Task>) {
        items.clear()
        items.addAll(tasks.filterNot { it.completed })
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = items[position]
        holder.binding.title.text = task.title
        holder.binding.due.text = task.due?.let {
            DateFormat.getDateInstance(DateFormat.SHORT).format(Date(it))
        } ?: ""
        holder.binding.root.setOnClickListener { onTap(task) }
    }

    override fun getItemCount(): Int = items.size
}
