package com.zenzone.app.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.zenzone.app.R
import com.zenzone.app.model.ZenBadge

class BadgeAdapter(
    private var badges: List<ZenBadge>,
    private var isGridView: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class GridViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivBadgeIcon: ImageView = view.findViewById(R.id.iv_badge_icon)
        val ivLockOverlay: ImageView = view.findViewById(R.id.iv_badge_lock)
        val tvBadgeName: TextView = view.findViewById(R.id.tv_badge_name)
    }

    inner class ListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivBadgeIcon: ImageView = view.findViewById(R.id.iv_badge_list_icon)
        val ivLockOverlay: ImageView = view.findViewById(R.id.iv_badge_list_lock)
        val tvBadgeName: TextView = view.findViewById(R.id.tv_badge_list_name)
        val tvBadgeDesc: TextView = view.findViewById(R.id.tv_badge_list_desc)
        val tvBadgeRequirement: TextView = view.findViewById(R.id.tv_badge_list_requirement)
    }

    override fun getItemViewType(position: Int): Int {
        return if (isGridView) VIEW_TYPE_GRID else VIEW_TYPE_LIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_GRID) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_badge, parent, false)
            GridViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_badge_list, parent, false)
            ListViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val badge = badges[position]
        
        if (holder is GridViewHolder) {
            bindGridView(holder, badge)
        } else if (holder is ListViewHolder) {
            bindListView(holder, badge)
        }
    }

    private fun bindGridView(holder: GridViewHolder, badge: ZenBadge) {
        // Set badge name
        holder.tvBadgeName.text = badge.name
        
        val iconResId = when (badge.iconRes) {
            "ic_badge_first_breath" -> R.drawable.ic_badge_first_breath
            "ic_badge_chain" -> R.drawable.ic_badge_chain
            "ic_badge_master" -> R.drawable.ic_badge_master
            "ic_badge_time" -> R.drawable.ic_badge_time
            else -> R.drawable.ic_lotus_logo
        }
        holder.ivBadgeIcon.setImageResource(iconResId)
        
        if (badge.isEarned) {
            holder.ivBadgeIcon.alpha = 1.0f
            holder.tvBadgeName.alpha = 1.0f
            holder.ivLockOverlay.visibility = View.GONE
        } else {
            holder.ivBadgeIcon.alpha = 0.3f
            holder.tvBadgeName.alpha = 0.5f
            holder.ivLockOverlay.visibility = View.VISIBLE
        }
    }

    private fun bindListView(holder: ListViewHolder, badge: ZenBadge) {
        holder.tvBadgeName.text = badge.name
        holder.tvBadgeDesc.text = badge.description
        
        val requirementText = when {
            badge.id == "first_session" -> "Complete your first session"
            badge.id == "hours_10" -> "Accumulate 10 hours of focus time"
            badge.requiredChain > 0 -> "Reach a ${badge.requiredChain}-day chain"
            else -> "Special achievement"
        }
        holder.tvBadgeRequirement.text = requirementText
        
        val iconResId = when (badge.iconRes) {
            "ic_badge_first_breath" -> R.drawable.ic_badge_first_breath
            "ic_badge_chain" -> R.drawable.ic_badge_chain
            "ic_badge_master" -> R.drawable.ic_badge_master
            "ic_badge_time" -> R.drawable.ic_badge_time
            else -> R.drawable.ic_lotus_logo
        }
        holder.ivBadgeIcon.setImageResource(iconResId)
        
        if (badge.isEarned) {
            holder.ivBadgeIcon.alpha = 1.0f
            holder.tvBadgeName.alpha = 1.0f
            holder.tvBadgeDesc.alpha = 1.0f
            holder.tvBadgeRequirement.alpha = 0.7f
            holder.ivLockOverlay.visibility = View.GONE
        } else {
            holder.ivBadgeIcon.alpha = 0.3f
            holder.tvBadgeName.alpha = 0.5f
            holder.tvBadgeDesc.alpha = 0.5f
            holder.tvBadgeRequirement.alpha = 0.5f
            holder.ivLockOverlay.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = badges.size

    fun setViewMode(isGrid: Boolean) {
        if (isGridView != isGrid) {
            isGridView = isGrid
            notifyDataSetChanged()
        }
    }

    fun updateData(newBadges: List<ZenBadge>) {
        val diffCallback = BadgeDiffCallback(badges, newBadges)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        badges = newBadges
        diffResult.dispatchUpdatesTo(this)
    }
    
    private class BadgeDiffCallback(
        private val oldList: List<ZenBadge>,
        private val newList: List<ZenBadge>
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

    companion object {
        private const val VIEW_TYPE_GRID = 0
        private const val VIEW_TYPE_LIST = 1
    }
}
