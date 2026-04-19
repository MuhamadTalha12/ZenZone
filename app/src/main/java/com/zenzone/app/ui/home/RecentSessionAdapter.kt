package com.zenzone.app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.zenzone.app.R
import com.zenzone.app.model.FocusSession
import com.zenzone.app.utils.DateUtils

class RecentSessionAdapter(
    private var sessions: List<FocusSession>,
    private val onSessionClick: (() -> Unit)? = null
) : RecyclerView.Adapter<RecentSessionAdapter.SessionViewHolder>() {

    class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val goalName: TextView = view.findViewById(R.id.tv_session_goal_name)
        val date: TextView = view.findViewById(R.id.tv_session_date)
        val duration: TextView = view.findViewById(R.id.tv_session_duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]
        holder.goalName.text = session.goalName
        holder.date.text = formatSessionDate(session.completedAt)
        holder.duration.text = "${session.durationMinutes}m"
        
        // Add click listener to navigate to stats
        holder.itemView.setOnClickListener {
            onSessionClick?.invoke()
        }
    }

    override fun getItemCount(): Int = sessions.size

    fun updateData(newSessions: List<FocusSession>) {
        val diffCallback = SessionDiffCallback(sessions, newSessions)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        sessions = newSessions
        diffResult.dispatchUpdatesTo(this)
    }

    private fun formatSessionDate(dateString: String): String {
        val today = DateUtils.getTodayString()
        return if (dateString == today) {
            "Today"
        } else {
            // Format as "Jan 15" or similar
            dateString
        }
    }

    private class SessionDiffCallback(
        private val oldList: List<FocusSession>,
        private val newList: List<FocusSession>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
