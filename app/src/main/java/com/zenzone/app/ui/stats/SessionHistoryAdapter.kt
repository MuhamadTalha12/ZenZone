package com.zenzone.app.ui.stats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.zenzone.app.R
import com.zenzone.app.model.FocusSession
import java.text.SimpleDateFormat
import java.util.*

class SessionHistoryAdapter(
    private var sessions: List<FocusSession>
) : RecyclerView.Adapter<SessionHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cvIcon: CardView = view.findViewById(R.id.cv_icon)
        val ivSessionIcon: ImageView = view.findViewById(R.id.iv_session_icon)
        val tvGoalName: TextView = view.findViewById(R.id.tv_session_goal_name)
        val tvDate: TextView = view.findViewById(R.id.tv_session_date)
        val tvDuration: TextView = view.findViewById(R.id.tv_session_duration)
        val cvEfficiency: CardView = view.findViewById(R.id.cv_efficiency)
        val tvEfficiency: TextView = view.findViewById(R.id.tv_efficiency)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessions[position]
        val context = holder.itemView.context
        
        holder.tvGoalName.text = session.goalName
        holder.tvDuration.text = "${session.durationMinutes} min"
        
        // Format date
        try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            isoFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = isoFormat.parse(session.completedAt)
            
            if (date != null) {
                val calendar = Calendar.getInstance()
                calendar.time = date
                
                val now = Calendar.getInstance()
                val isToday = calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                
                val yesterday = Calendar.getInstance()
                yesterday.add(Calendar.DAY_OF_YEAR, -1)
                val isYesterday = calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                        calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
                
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val timeStr = timeFormat.format(date)
                
                val dateStr = when {
                    isToday -> "Today"
                    isYesterday -> "Yesterday"
                    else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
                }
                
                holder.tvDate.text = "$dateStr • $timeStr"
            } else {
                holder.tvDate.text = session.completedAt.substringBefore("T")
            }
        } catch (e: Exception) {
            holder.tvDate.text = session.completedAt.substringBefore("T")
        }
        
        // Calculate efficiency (random for now, could be based on actual data)
        val efficiency = (85..98).random()
        holder.tvEfficiency.text = "Efficiency\n$efficiency%"
        
        // Set efficiency badge color based on percentage
        val efficiencyColor = when {
            efficiency >= 90 -> context.getColor(R.color.zen_teal_light)
            efficiency >= 80 -> context.getColor(R.color.zen_accent_gold)
            else -> context.getColor(R.color.zen_danger_red)
        }
        val efficiencyTextColor = when {
            efficiency >= 90 -> context.getColor(R.color.zen_teal_dark)
            efficiency >= 80 -> context.getColor(R.color.zen_slate_dark)
            else -> context.getColor(R.color.zen_slate_surface)
        }
        holder.cvEfficiency.setCardBackgroundColor(efficiencyColor)
        holder.tvEfficiency.setTextColor(efficiencyTextColor)
        
        // Set icon background color based on position (variety)
        val colors = listOf(
            R.color.zen_slate_dark,
            R.color.zen_teal_primary,
            R.color.zen_blue_muted
        )
        holder.cvIcon.setCardBackgroundColor(context.getColor(colors[position % colors.size]))
    }

    override fun getItemCount() = sessions.size

    fun updateData(newSessions: List<FocusSession>) {
        val diffCallback = SessionDiffCallback(sessions, newSessions)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        sessions = newSessions
        diffResult.dispatchUpdatesTo(this)
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
            val oldSession = oldList[oldItemPosition]
            val newSession = newList[newItemPosition]
            return oldSession == newSession
        }
    }
}
