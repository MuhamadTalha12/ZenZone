package com.zenzone.app.ui.focus

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.zenzone.app.R
import com.zenzone.app.utils.DndHelper
import com.zenzone.app.viewmodel.FocusEvent
import com.zenzone.app.viewmodel.FocusViewModel

class FocusFragment : Fragment(R.layout.fragment_focus) {

    private val viewModel: FocusViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spinnerGoals: Spinner = view.findViewById(R.id.spinner_goal_selector)
        val timerView: CircularTimerView = view.findViewById(R.id.timer_view)
        val btnStart: MaterialButton = view.findViewById(R.id.btn_start)
        val btnStop: MaterialButton = view.findViewById(R.id.btn_stop)
        val btnLogManual: MaterialButton = view.findViewById(R.id.btn_log_manual)
        val tvDndBadge: TextView = view.findViewById(R.id.tv_dnd_badge)
        val tvFocusLockBadge: TextView = view.findViewById(R.id.tv_focus_lock_badge)
        val tvGoalTitle: TextView = view.findViewById(R.id.tv_goal_info_title)
        val tvGoalDesc: TextView = view.findViewById(R.id.tv_goal_info_desc)
        val fabInfo: com.google.android.material.floatingactionbutton.FloatingActionButton = view.findViewById(R.id.fab_info)

        var hasGoals = false

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
                tvGoalTitle.text = "No goals available."
                tvGoalDesc.text = "Please add a goal in the Home Tab first."
                timerView.update(0L, 0L)
            }
        }

        viewModel.selectedGoal.observe(viewLifecycleOwner) { goal ->
            goal?.let {
                tvGoalTitle.text = "Current Goal: ${it.name}"
                tvGoalDesc.text = "Target: ${it.targetMinutes} min · ${it.frequency} · Chain: ${it.currentChain} 🔥"
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
            btnStart.isEnabled = !isRunning
            btnStop.isEnabled = isRunning
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
                                    val goal = viewModel.selectedGoal.value!!
                                    viewModel.logSession(goal, goal.targetMinutes)
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
                         viewModel.startTimer(viewModel.selectedGoal.value!!, false) 
                    }
                    .show()
            } else {
                viewModel.startTimer(viewModel.selectedGoal.value!!, true)
            }
        }

        btnStop.setOnClickListener {
            viewModel.stopTimer()
        }

        btnLogManual.setOnClickListener {
            if (!hasGoals) return@setOnClickListener
            val goal = viewModel.selectedGoal.value!!
            AlertDialog.Builder(requireContext())
                .setTitle("Log Manual Session")
                .setMessage("Manually log ${goal.targetMinutes} minutes for ${goal.name}?")
                .setPositiveButton("Yes") { _, _ ->
                    viewModel.logSession(goal, goal.targetMinutes)
                }
                .setNegativeButton("No", null)
                .show()
        }

        fabInfo.setOnClickListener {
            showXpInfoDialog()
        }

        viewModel.loadGoals()
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
