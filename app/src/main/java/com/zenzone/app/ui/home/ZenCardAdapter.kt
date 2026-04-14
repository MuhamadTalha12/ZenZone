package com.zenzone.app.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.zenzone.app.R
import com.zenzone.app.model.FocusGoal

class ZenCardAdapter(
    private var goals: List<FocusGoal>,
    private val onClick: (FocusGoal) -> Unit
) : RecyclerView.Adapter<ZenCardAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val vColorAccent: View = view.findViewById(R.id.v_color_accent)
        val tvGoalName: TextView = view.findViewById(R.id.tv_goal_name)
        val tvChainCount: TextView = view.findViewById(R.id.tv_chain_count)
        val tvGoalDetails: TextView = view.findViewById(R.id.tv_goal_details)
        val pbProgress: ProgressBar = view.findViewById(R.id.pb_progress)
        val tvProgressCaption: TextView = view.findViewById(R.id.tv_progress_caption)
        val tvLongestChain: TextView = view.findViewById(R.id.tv_longest_chain)
        val rootCard: MaterialCardView = view as MaterialCardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_zen_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val goal = goals[position]
        holder.tvGoalName.text = goal.name
        holder.tvChainCount.text = goal.currentChain.toString()
        holder.tvGoalDetails.text = "${goal.targetMinutes} min · ${goal.frequency} · ${goal.totalMinutesFocused} min total"
        holder.tvLongestChain.text = "Best: ${goal.longestChain}"

        try {
            holder.vColorAccent.setBackgroundColor(Color.parseColor(goal.colorTag))
        } catch (e: Exception) {}

        val maxChain = maxOf(goal.longestChain, 10).coerceAtLeast(goal.currentChain + 5)
        holder.pbProgress.max = maxChain
        holder.pbProgress.progress = goal.currentChain
        holder.tvProgressCaption.text = "Chain: ${goal.currentChain} / $maxChain"

        holder.rootCard.setOnClickListener { onClick(goal) }
    }

    override fun getItemCount() = goals.size

    fun updateData(newGoals: List<FocusGoal>) {
        goals = newGoals
        notifyDataSetChanged()
    }
}
