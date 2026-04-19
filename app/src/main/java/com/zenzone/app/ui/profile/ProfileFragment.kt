package com.zenzone.app.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.zenzone.app.R
import com.zenzone.app.utils.Constants
import com.zenzone.app.viewmodel.ProfileViewModel

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var badgeAdapter: BadgeAdapter
    private var isGridView = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            val tvUserName: TextView = view.findViewById(R.id.tv_user_name)
            val tvZenLevel: TextView = view.findViewById(R.id.tv_zen_level)
            val tvAvatarInitial: TextView = view.findViewById(R.id.tv_avatar_initial)
            val ivProfileImage: ImageView = view.findViewById(R.id.iv_profile_image)
            val cvAvatar: View = view.findViewById(R.id.cv_avatar)
            val cvPremiumBadge: CardView = view.findViewById(R.id.cv_premium_badge)
            val tvTotalHours: TextView = view.findViewById(R.id.tv_total_hours)
            val tvDailyStreak: TextView = view.findViewById(R.id.tv_daily_streak)
            val tvJourneyText: TextView = view.findViewById(R.id.tv_journey_text)
            val btnStartFocusing: MaterialButton = view.findViewById(R.id.btn_start_focusing)
            val btnSettings: View = view.findViewById(R.id.btn_settings)
            val btnSwitchView: ImageButton = view.findViewById(R.id.btn_switch_view)
            val rvBadges: RecyclerView = view.findViewById(R.id.rv_badges)
            val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)

            // Setup Badge RecyclerView
            badgeAdapter = BadgeAdapter(emptyList(), isGridView)
            rvBadges.layoutManager = if (isGridView) GridLayoutManager(requireContext(), 3) else LinearLayoutManager(requireContext())
            rvBadges.adapter = badgeAdapter

            viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }

            viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
                error?.let {
                    android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_LONG).show()
                    viewModel.clearErrorMessage()
                }
            }

            viewModel.profile.observe(viewLifecycleOwner) { profile ->
                profile?.let {
                    // Get name from SharedPreferences
                    val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                    val savedName = prefs.getString(Constants.PREF_USER_NAME, "Zen Practitioner")
                    
                    tvUserName.text = savedName

                    // Milestone thresholds in hours (matching StatsFragment)
                    val milestones = listOf(0, 5, 15, 40, 100, 250, 500)
                    val milestoneNames = listOf(
                        "Novice Monk",
                        "Calm Keeper", 
                        "Focused Warrior",
                        "Seasoned Practitioner",
                        "Deep Diver",
                        "Zen Master",
                        "Enlightened One"
                    )
                    
                    // Calculate and display total hours
                    val totalHours = it.totalFocusedMinutes / 60.0
                    tvTotalHours.text = String.format("%.1f", totalHours)

                    // Find current level based on totalHours
                    var level = 1
                    for (i in 1 until milestones.size) {
                        if (totalHours >= milestones[i]) {
                            level = i + 1
                        } else {
                            break
                        }
                    }
                    
                    val levelName = milestoneNames[level - 1]
                    tvZenLevel.text = "Zen Level: $levelName"

                    // Show avatar initial (no image upload feature)
                    ivProfileImage.visibility = View.GONE
                    tvAvatarInitial.visibility = View.VISIBLE
                    tvAvatarInitial.text = savedName?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "Z"

                    // Show daily streak
                    tvDailyStreak.text = it.currentChain.toString()

                    if (level < milestones.size) {
                        val hoursToNext = milestones[level].toDouble() - totalHours
                        val nextLevelName = milestoneNames[level]
                        tvJourneyText.text = "You are only ${String.format("%.1f", hoursToNext)} hours away from becoming a '$nextLevelName'."
                    } else {
                        tvJourneyText.text = "Congratulations! You've reached the highest level: ${milestoneNames.last()}!"
                    }

                    cvPremiumBadge.visibility = View.VISIBLE
                }
            }

            viewModel.badges.observe(viewLifecycleOwner) { allBadges ->
                allBadges?.let {
                    badgeAdapter.updateData(it)
                }
            }

            btnSwitchView.setOnClickListener {
                isGridView = !isGridView
                rvBadges.layoutManager = if (isGridView) GridLayoutManager(requireContext(), 3) else LinearLayoutManager(requireContext())
                badgeAdapter.setViewMode(isGridView)
                // Update icon - grid view shows list icon, list view shows grid icon
                btnSwitchView.setImageResource(if (isGridView) R.drawable.ic_stats else R.drawable.ic_home)
            }

            // Remove click listener from avatar (no upload feature)
            cvAvatar.setOnClickListener(null)

            btnStartFocusing.setOnClickListener {
                val bottomNav = activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav)
                bottomNav?.selectedItemId = R.id.nav_focus
            }

            btnSettings.setOnClickListener {
                android.widget.Toast.makeText(requireContext(), "Settings - Coming soon!", android.widget.Toast.LENGTH_SHORT).show()
            }

            viewModel.loadProfile()
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(requireContext(), "Error loading profile: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
