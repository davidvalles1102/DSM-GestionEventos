package com.foro_2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentsAdapter(private var comments: List<Comment>) :
    RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewName: TextView = itemView.findViewById(R.id.textViewUserName)
        val textViewComment: TextView = itemView.findViewById(R.id.textViewComment)
        val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBarComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.textViewName.text = comment.userName
        holder.textViewComment.text = comment.text
        holder.ratingBar.rating = comment.rating.toFloat()
        holder.textViewDate.text = dateFormat.format(Date(comment.timestamp))
    }

    override fun getItemCount(): Int = comments.size

    fun updateComments(newComments: List<Comment>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = comments.size
            override fun getNewListSize() = newComments.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                comments[oldPos].id == newComments[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                comments[oldPos] == newComments[newPos]
        })
        comments = newComments
        diff.dispatchUpdatesTo(this)
    }
}
