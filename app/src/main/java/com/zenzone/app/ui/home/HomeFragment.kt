package com.zenzone.app.ui.home

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.zenzone.app.R
import com.zenzone.app.repository.FocusRepository
import com.zenzone.app.repository.UserRepository
import com.zenzone.app.viewmodel.HomeViewModel
import com.zenzone.app.viewmodel.HomeViewModelFactory
import com.zenzone.app.ui.main.MainActivity

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
            FocusRepository(requireContext()),
            UserRepository(requireContext())
        )
    }
    private lateinit var adapter: ZenCardAdapter
    private lateinit var recentSessionAdapter: RecentSessionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            // Common Navbar Views
            val ivCommonInfo = view.findViewById<ImageView>(R.id.iv_common_info)
            val cvCommonProfile = view.findViewById<View>(R.id.cv_common_profile_mini)
            val tvCommonInitial = view.findViewById<TextView>(R.id.tv_common_profile_initial_mini)
            val ivCommonProfileImage = view.findViewById<ImageView>(R.id.iv_common_profile_image_mini)
            view.findViewById<View>(R.id.iv_common_menu)?.setOnClickListener {
                (activity as? MainActivity)?.openDrawer()
            }
            view.findViewById<View>(R.id.iv_common_agent)?.setOnClickListener {
                com.zenzone.app.ui.social.ZenAgentDialog.show(requireContext(), parentFragmentManager, activity as? MainActivity)
            }

            val rv = view.findViewById<RecyclerView>(R.id.rv_zen_cards)
            val rvRecentSessions = view.findViewById<RecyclerView>(R.id.rv_recent_sessions)
            val fab = view.findViewById<FloatingActionButton>(R.id.fab_add_goal)
            val emptyState = view.findViewById<View>(R.id.empty_state_layout)
            val progressBar = view.findViewById<android.widget.ProgressBar>(R.id.progress_bar)
            val tvTotalStreak = view.findViewById<TextView>(R.id.tv_total_streak)
            val tvFocusTime = view.findViewById<TextView>(R.id.tv_focus_time)
            val btnEditGoals = view.findViewById<MaterialButton>(R.id.btn_edit_goals)

            // Zen Garden bindings
            val cvZenGarden = view.findViewById<View>(R.id.cv_zen_garden)
            val tvGardenPeek = view.findViewById<TextView>(R.id.tv_garden_peek)
            val tvGardenStatusEmoji = view.findViewById<TextView>(R.id.tv_garden_status_emoji)

            cvZenGarden.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, com.zenzone.app.ui.garden.ZenGardenFragment())
                    .addToBackStack(null)
                    .commit()
            }

            // Setup goals adapter
            adapter = ZenCardAdapter(
                goals = emptyList(),
                onCardClick = { goal ->
                    (activity as? MainActivity)?.navigateToMenuItem(R.id.nav_focus)
                },
                onEditClick = { goal ->
                    EditFocusDialogFragment(
                        goal = goal,
                        onUpdate = { updatedGoal ->
                            viewModel.updateGoal(updatedGoal)
                        },
                        onDelete = { goalId ->
                            viewModel.deleteGoal(goalId)
                        }
                    ).show(childFragmentManager, "EditGoal")
                }
            )
            
            rv.layoutManager = LinearLayoutManager(requireContext())
            rv.adapter = adapter
            
            // Setup recent sessions adapter
            recentSessionAdapter = RecentSessionAdapter(emptyList()) {
                (activity as? MainActivity)?.navigateToMenuItem(R.id.nav_stats)
            }
            rvRecentSessions.layoutManager = LinearLayoutManager(requireContext())
            rvRecentSessions.adapter = recentSessionAdapter
            
            // Observe loading state
            viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }

            // Observe goals
            viewModel.goals.observe(viewLifecycleOwner) { goals ->
                adapter.updateData(goals)
                if (goals.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    rv.visibility = View.GONE
                } else {
                    emptyState.visibility = View.GONE
                    rv.visibility = View.VISIBLE
                }
            }
            
            // Observe total streak
            viewModel.totalStreak.observe(viewLifecycleOwner) { streak ->
                tvTotalStreak.text = if (streak == 1) "$streak Day" else "$streak Days"
            }
            
            // Observe focus time
            viewModel.focusTime.observe(viewLifecycleOwner) { time ->
                tvFocusTime.text = time
            }
            
             // Observe user profile for common navbar
             viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
                 profile?.let {
                     val displayName = it.userName.ifBlank { "ZenZone" }
                     view.findViewById<TextView>(R.id.tv_app_logo_name)?.text = "🧘 $displayName"

                     val initial = if (it.userName.isNotEmpty()) {
                         it.userName.first().uppercaseChar().toString()
                     } else {
                         "Z"
                     }
                     tvCommonInitial.text = initial
                     
                     if (!it.profileImageUri.isNullOrBlank()) {
                         if (com.zenzone.app.utils.ImageUtils.isBase64Image(it.profileImageUri)) {
                             val bitmap = com.zenzone.app.utils.ImageUtils.base64ToBitmap(it.profileImageUri)
                             if (bitmap != null) {
                                 ivCommonProfileImage.setImageBitmap(bitmap)
                                 ivCommonProfileImage.visibility = View.VISIBLE
                                 tvCommonInitial.visibility = View.GONE
                             } else {
                                 ivCommonProfileImage.visibility = View.GONE
                                 tvCommonInitial.visibility = View.VISIBLE
                             }
                         } else {
                             try {
                                 val uri = android.net.Uri.parse(it.profileImageUri)
                                 ivCommonProfileImage.setImageURI(uri)
                                 ivCommonProfileImage.visibility = View.VISIBLE
                                 tvCommonInitial.visibility = View.GONE
                             } catch (e: Exception) {
                                 ivCommonProfileImage.visibility = View.GONE
                                 tvCommonInitial.visibility = View.VISIBLE
                             }
                         }
                     } else {
                         ivCommonProfileImage.visibility = View.GONE
                         tvCommonInitial.visibility = View.VISIBLE
                     }

                     // Update garden peek layout
                     val plantCount = it.zenXP / 100
                     val isWithered = (it.currentChain == 0)
                     tvGardenStatusEmoji.text = if (isWithered && plantCount > 0) "🥀" else if (plantCount == 0) "🕳️" else "🌸"
                     tvGardenPeek.text = if (isWithered && plantCount > 0) {
                         "Streak broken! Revive your withered plants by starting a focus session."
                     } else if (plantCount == 0) {
                         "Your garden is currently empty. Complete focus sessions and earn 100 XP to start planting!"
                     } else {
                         "Thriving garden with $plantCount plant(s) grown from ${it.zenXP} XP."
                     }
                 }
             }
            
            // Profile icon click
            cvCommonProfile.setOnClickListener {
                (activity as? MainActivity)?.navigateToMenuItem(R.id.nav_profile)
            }

            // Instruction icon click
            ivCommonInfo.setOnClickListener {
                showInstructionsDialog()
            }
            
            // Observe recent sessions
            viewModel.recentSessions.observe(viewLifecycleOwner) { sessions ->
                recentSessionAdapter.updateData(sessions)
            }
            
            // FAB click listener
            fab.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, AddFocusFragment())
                    .addToBackStack(null)
                    .commit()
            }
            
            btnEditGoals.setOnClickListener {
                android.widget.Toast.makeText(
                    requireContext(), 
                    "Use the edit icon on any card to modify it",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }

            // Load all data
            viewModel.loadGoals()
            viewModel.loadUserProfile()
            viewModel.loadRecentSessions()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserProfile()
        viewModel.loadGoals()
    }

    private fun showInstructionsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_home_instructions, null)
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        dialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundColor(
            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.zen_slate_dark)
        )

        dialogView.findViewById<View>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
