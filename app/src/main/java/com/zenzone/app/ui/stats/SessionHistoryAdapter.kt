package com.zenzone.app.ui.stats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zenzone.app.R
import com.zenzone.app.model.FocusSession

class SessionHistoryAdapter(
    private var sessions: List<FocusSession>
) : RecyclerView.Adapter<SessionHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvGoalName: TextView = view.findViewById(R.id.tv_session_goal_name)
        val tvDate: TextView = view.findViewById(R.id.tv_session_date)
        val tvDuration: TextView = view.findViewById(R.id.tv_session_duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessions[position]
        holder.tvGoalName.text = session.goalName
        val datePart = session.completedAt.substringBefore("T")
        val timePart = session.completedAt.substringAfter("T").substringBefore("Z")
        holder.tvDate.text = "$datePart $timePart"
        holder.tvDuration.text = "${session.durationMinutes} min"
    }

    override fun getItemCount() = sessions.size

    fun updateData(newSessions: List<FocusSession>) {
        sessions = newSessions
        notifyDataSetChanged()
    }
}
