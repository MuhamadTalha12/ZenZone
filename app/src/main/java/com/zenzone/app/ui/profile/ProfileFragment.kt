package com.zenzone.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.zenzone.app.R
import com.zenzone.app.viewmodel.ProfileViewModel

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle: TextView = view.findViewById(R.id.tv_zen_title)
        val tvSubtitle: TextView = view.findViewById(R.id.tv_zen_subtitle)
        val tvXpLabel: TextView = view.findViewById(R.id.tv_xp_label)
        val tvXpCaption: TextView = view.findViewById(R.id.tv_xp_caption)
        val pbXpProgress: ProgressBar = view.findViewById(R.id.pb_xp_progress)
        val gridBadges: GridLayout = view.findViewById(R.id.grid_badges)
        val tvAvatarInitial: TextView = view.findViewById(R.id.tv_avatar_initial)
        val tvBadgeCount: TextView = view.findViewById(R.id.tv_badge_count)

        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            val displayName = if (profile.userName.isNotBlank()) profile.userName else "Zen Practitioner"
            tvTitle.text = displayName
            tvAvatarInitial.text = displayName.first().uppercaseChar().toString()
            tvSubtitle.text = "Level ${profile.zenLevel}"
            tvXpLabel.text = "Zen Level ${profile.zenLevel}"
            
            val thresholds = listOf(100, 250, 500, 1000, 2000, 3500, 5500, 8000, 11000, 15000)
            val currentLevelIdx = profile.zenLevel - 1
            
            val neededForNext = if (currentLevelIdx < thresholds.size) thresholds[currentLevelIdx] else thresholds.last()
            val startOfLevel = if (currentLevelIdx > 0) thresholds[currentLevelIdx - 1] else 0
            
            val xpIntoLevel = profile.zenXP - startOfLevel
            val levelSize = neededForNext - startOfLevel
            
            pbXpProgress.max = maxOf(levelSize, 1)
            pbXpProgress.progress = maxOf(0, xpIntoLevel)
            
            tvXpCaption.text = "$xpIntoLevel / $levelSize XP"
        }

        viewModel.badges.observe(viewLifecycleOwner) { badges ->
            gridBadges.removeAllViews()
            val inflater = LayoutInflater.from(requireContext())
            val earnedCount = badges.count { it.isEarned }
            tvBadgeCount.text = "$earnedCount/${badges.size}"
            
            for (badge in badges) {
                val badgeView = inflater.inflate(R.layout.item_badge, gridBadges, false)
                val ivIcon = badgeView.findViewById<ImageView>(R.id.iv_badge_icon)
                val ivLock = badgeView.findViewById<ImageView>(R.id.iv_lock_overlay)
                val tvName = badgeView.findViewById<TextView>(R.id.tv_badge_name)
                
                tvName.text = badge.name
                
                // Load the correct icon
                val iconResId = when (badge.iconRes) {
                    "ic_badge_first_breath" -> R.drawable.ic_badge_first_breath
                    "ic_badge_chain" -> R.drawable.ic_badge_chain
                    "ic_badge_master" -> R.drawable.ic_badge_master
                    "ic_badge_time" -> R.drawable.ic_badge_time
                    else -> R.drawable.ic_lotus_logo
                }
                ivIcon.setImageResource(iconResId)
                
                if (badge.isEarned) {
                    ivIcon.alpha = 1.0f
                    tvName.alpha = 1.0f
                    ivLock.visibility = View.GONE
                } else {
                    ivIcon.alpha = 0.25f
                    tvName.alpha = 0.5f
                    ivLock.visibility = View.VISIBLE
                }
                
                gridBadges.addView(badgeView)
            }
        }

        viewModel.loadProfile()
    }
}
