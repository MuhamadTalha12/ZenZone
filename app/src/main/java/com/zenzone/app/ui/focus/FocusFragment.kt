package com.zenzone.app.ui.focus

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.zenzone.app.R
import com.zenzone.app.utils.DndHelper
import com.zenzone.app.viewmodel.FocusEvent
import com.zenzone.app.viewmodel.FocusViewModel

class FocusFragment : Fragment(R.layout.fragment_focus) {

    private val viewModel: FocusViewModel by viewModels()
    private var isPaused = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            val spinnerGoals: Spinner = view.findViewById(R.id.spinner_goal_selector)
            val timerView: CircularTimerView = view.findViewById(R.id.timer_view)
            val btnStart: MaterialButton = view.findViewById(R.id.btn_start)
            val btnComplete: MaterialButton = view.findViewById(R.id.btn_complete)
            val btnPauseContainer: LinearLayout = view.findViewById(R.id.btn_pause_container)
            val btnCancelContainer: LinearLayout = view.findViewById(R.id.btn_cancel_container)
            val llControlButtons: LinearLayout = view.findViewById(R.id.ll_control_buttons)
            val tvDndBadge: TextView = view.findViewById(R.id.tv_dnd_badge)
            val tvFocusLockBadge: TextView = view.findViewById(R.id.tv_focus_lock_badge)
            val tvGoalName: TextView = view.findViewById(R.id.tv_goal_name)
            val tvGoalSubtitle: TextView = view.findViewById(R.id.tv_goal_subtitle)
            val cvGoalSelector: MaterialCardView = view.findViewById(R.id.cv_goal_selector)
            val cvActiveBadge: MaterialCardView = view.findViewById(R.id.cv_active_badge)
            val ivCommonInfo: ImageView = view.findViewById(R.id.iv_common_info)
            val cvCommonProfile: View = view.findViewById(R.id.cv_common_profile_mini)
            val progressBar = view.findViewById<android.widget.ProgressBar>(R.id.progress_bar)

            var hasGoals = false
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }

        viewModel.goals.observe(viewLifecycleOwner) { goals ->
            hasGoals = goals.isNotEmpty()
            if (goals.isNotEmpty()) {
                val names = goals.map { it.name }
                val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_goal, names)
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_goal)
                spinnerGoals.adapter = adapter
                
                spinnerGoals.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                         viewModel.selectGoal(goals[position])
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
                if (viewModel.selectedGoal.value == null) {
                    viewModel.selectGoal(goals[0])
                }
            } else {
                tvGoalName.text = "No Goals Yet"
                tvGoalSubtitle.text = "Add a focus goal in the Home tab to get started"
                timerView.update(0L, 0L)
                
                // Make the goal name clickable to navigate to Home
                tvGoalName.setOnClickListener {
                    val bottomNav = activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav)
                    bottomNav?.selectedItemId = R.id.nav_home
                }
            }
        }

        viewModel.selectedGoal.observe(viewLifecycleOwner) { goal ->
            goal?.let {
                tvGoalName.text = it.name
                tvGoalSubtitle.text = "Target: ${it.targetMinutes} min · Chain: ${it.currentChain} 🔥"
                val totalMs = it.targetMinutes * 60 * 1000L
                val remainMs = viewModel.remainingTimeMs.value ?: totalMs
                timerView.update(remainMs, totalMs)
            }
        }

        viewModel.remainingTimeMs.observe(viewLifecycleOwner) { remainMs ->
            val totalMs = viewModel.selectedGoal.value?.targetMinutes?.times(60)?.times(1000L) ?: 0L
            timerView.update(remainMs, totalMs)
        }

        viewModel.isRunning.observe(viewLifecycleOwner) { isRunning ->
            // Show/hide UI elements based on running state
            btnStart.visibility = if (isRunning) View.GONE else View.VISIBLE
            btnComplete.visibility = if (isRunning) View.VISIBLE else View.GONE
            llControlButtons.visibility = if (isRunning) View.VISIBLE else View.GONE
            cvGoalSelector.visibility = if (isRunning) View.GONE else View.VISIBLE
            cvActiveBadge.visibility = if (isRunning) View.VISIBLE else View.GONE
            
            spinnerGoals.isEnabled = !isRunning
            tvFocusLockBadge.visibility = if (isRunning) View.VISIBLE else View.GONE
            
            // Enable/disable focus lock mode
            val mainActivity = activity as? com.zenzone.app.ui.main.MainActivity
            if (isRunning) {
                mainActivity?.enableFocusLock()
            } else {
                mainActivity?.disableFocusLock()
            }
        }

        viewModel.isDndActive.observe(viewLifecycleOwner) { dnd ->
            tvDndBadge.visibility = if (dnd) View.VISIBLE else View.GONE
        }

        viewModel.focusEvents.observe(viewLifecycleOwner) { event ->
            event?.let {
                when (it) {
                    is FocusEvent.SessionComplete -> {
                        if (it.xpGained == 0) {
                             AlertDialog.Builder(requireContext())
                                .setTitle("Session Complete!")
                                .setMessage("Great job focusing. Ready to log your session?")
                                .setPositiveButton("Log It!") { _, _ ->
                                    viewModel.selectedGoal.value?.let { goal ->
                                        viewModel.logSession(goal, goal.targetMinutes)
                                    }
                                }
                                .setNegativeButton("Discard", null)
                                .setCancelable(false)
                                .show()
                             viewModel.clearEvent()
                        } else {
                            val msg = "🎉 Session Complete!\n+${it.xpGained} XP · Chain: ${it.oldChain} → ${it.newChain} 🔥"
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                            if (it.newlyUnlockedBadges.isNotEmpty()) {
                                val badgeNames = it.newlyUnlockedBadges.joinToString(", ") { badge -> badge.name }
                                Toast.makeText(requireContext(), "🏆 New Badge Unlocked: $badgeNames!", Toast.LENGTH_LONG).show()
                            }
                            viewModel.clearEvent()
                        }
                    }
                    is FocusEvent.Error -> {
                        Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                        viewModel.clearEvent()
                    }
                }
            }
        }

        // Start button click
        btnStart.setOnClickListener {
            if (!hasGoals) {
                Toast.makeText(requireContext(), "No goals selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!DndHelper.hasDndPermission(requireContext())) {
                AlertDialog.Builder(requireContext())
                    .setTitle("DND Permission Required")
                    .setMessage("To silence notifications during your zen session, please grant ZenZone Do Not Disturb access.")
                    .setPositiveButton("Grant Access") { _, _ ->
                         DndHelper.requestDndPermission(requireContext())
                    }
                    .setNegativeButton("Skip") { _, _ -> 
                         viewModel.selectedGoal.value?.let { goal ->
                             viewModel.startTimer(goal, false)
                         }
                    }
                    .show()
            } else {
                viewModel.selectedGoal.value?.let { goal ->
                    viewModel.startTimer(goal, true)
                }
            }
        }

        // Complete button click
        btnComplete.setOnClickListener {
            viewModel.stopTimer()
            viewModel.selectedGoal.value?.let { goal ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Complete Session")
                    .setMessage("Log this session for ${goal.name}?")
                    .setPositiveButton("Yes") { _, _ ->
                        viewModel.logSession(goal, goal.targetMinutes)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }

        // Pause button click
        btnPauseContainer.setOnClickListener {
            if (!isPaused) {
                viewModel.stopTimer()
                isPaused = true
                Toast.makeText(requireContext(), "Session paused", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.selectedGoal.value?.let { goal ->
                    viewModel.startTimer(goal, viewModel.isDndActive.value == true)
                }
                isPaused = false
                Toast.makeText(requireContext(), "Session resumed", Toast.LENGTH_SHORT).show()
            }
        }

        // Cancel button click
        btnCancelContainer.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Cancel Session")
                .setMessage("Are you sure you want to cancel this session? Progress will not be saved.")
                .setPositiveButton("Yes, Cancel") { _, _ ->
                    viewModel.stopTimer()
                    isPaused = false
                    Toast.makeText(requireContext(), "Session cancelled", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .show()
        }

        // Info icon click - show info
        ivCommonInfo.setOnClickListener {
            showXpInfoDialog()
        }

        // Profile icon click - navigate to profile
        cvCommonProfile.setOnClickListener {
            val bottomNav = activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav)
            bottomNav?.selectedItemId = R.id.nav_profile
        }

        viewModel.loadGoals()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error loading focus screen: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showXpInfoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_xp_info, null)
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)
        
        // Style the bottom sheet background
        dialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundColor(
            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.zen_slate_dark)
        )
        
        dialogView.findViewById<MaterialButton>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
}
