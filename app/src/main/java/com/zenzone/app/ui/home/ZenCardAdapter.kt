package com.zenzone.app.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.zenzone.app.R
import com.zenzone.app.model.FocusGoal

class ZenCardAdapter(
    private var goals: List<FocusGoal>,
    private val onCardClick: (FocusGoal) -> Unit,
    private val onEditClick: (FocusGoal) -> Unit
) : RecyclerView.Adapter<ZenCardAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvGoalName: TextView = view.findViewById(R.id.tv_goal_name)
        val tvGoalDetails: TextView = view.findViewById(R.id.tv_goal_details)
        val tvChainCount: TextView = view.findViewById(R.id.tv_chain_count)
        val pbProgress: ProgressBar = view.findViewById(R.id.pb_progress)
        val tvLongestChain: TextView = view.findViewById(R.id.tv_longest_chain)
        val vColorAccent: View = view.findViewById(R.id.v_color_accent)
        val btnEdit: ImageButton = view.findViewById(R.id.btn_edit_goal)
        val rootCard: MaterialCardView = view as MaterialCardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_zen_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val goal = goals[position]
        
        holder.tvGoalName.text = goal.name
        holder.tvGoalDetails.text = "${goal.targetMinutes} min · ${goal.frequency}"
        holder.tvChainCount.text = goal.currentChain.toString()
        holder.tvLongestChain.text = "Best: ${goal.longestChain}"
        
        val maxChain = maxOf(goal.longestChain, 10)
        holder.pbProgress.max = maxChain
        holder.pbProgress.progress = goal.currentChain
        
        try {
            val color = Color.parseColor(goal.colorTag)
            holder.vColorAccent.setBackgroundColor(color)
        } catch (e: Exception) {
            holder.vColorAccent.setBackgroundColor(
                holder.itemView.context.getColor(R.color.zen_teal_primary)
            )
        }

        holder.rootCard.setOnClickListener { onCardClick(goal) }
        holder.btnEdit.setOnClickListener { onEditClick(goal) }
    }

    override fun getItemCount() = goals.size

    fun updateData(newGoals: List<FocusGoal>) {
        val diffCallback = GoalDiffCallback(goals, newGoals)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        goals = newGoals
        diffResult.dispatchUpdatesTo(this)
    }
    
    private class GoalDiffCallback(
        private val oldList: List<FocusGoal>,
        private val newList: List<FocusGoal>
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
