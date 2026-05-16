package com.foro_2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class HistoryEventAdapter(
    private var events: List<Event>,
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<HistoryEventAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageViewEvent)
        val titleView: TextView = itemView.findViewById(R.id.textViewTitle)
        val dateView: TextView = itemView.findViewById(R.id.textViewDate)
        val statusView: TextView = itemView.findViewById(R.id.textViewStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        holder.titleView.text = event.title
        holder.dateView.text = "${event.date} ${event.time}"
        holder.statusView.text = "Asistí"
        holder.statusView.setTextColor(holder.itemView.context.getColor(R.color.success_green))

        if (event.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(event.imageUrl)
                .placeholder(R.drawable.ic_event_placeholder)
                .into(holder.imageView)
        } else {
            holder.imageView.setImageResource(R.drawable.ic_event_placeholder)
        }

        holder.itemView.setOnClickListener { onEventClick(event) }
    }

    override fun getItemCount(): Int = events.size

    fun updateEvents(newEvents: List<Event>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = events.size
            override fun getNewListSize() = newEvents.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                events[oldPos].id == newEvents[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                events[oldPos] == newEvents[newPos]
        })
        events = newEvents
        diff.dispatchUpdatesTo(this)
    }
}
