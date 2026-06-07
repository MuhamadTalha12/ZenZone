package com.zenzone.app.ui.focus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.zenzone.app.R
import com.zenzone.app.model.UserProfile
import com.zenzone.app.utils.DndHelper
import com.zenzone.app.utils.FocusTimerService
import com.zenzone.app.ui.main.MainActivity
import com.zenzone.app.ui.home.AddFocusFragment
import com.zenzone.app.viewmodel.FocusEvent
import com.zenzone.app.viewmodel.FocusViewModel

class FocusFragment : Fragment(R.layout.fragment_focus) {

    private val viewModel: FocusViewModel by viewModels()
    private var hasGoals = false
    private var selectedMode: com.zenzone.app.model.FocusMode? = null
    private var customMinutes = 25

    private var cvCommonProfile: View? = null
    private var ivCommonInfo: View? = null
    private var ivCommonMenu: View? = null
    private var ivCommonAgent: View? = null

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            checkDndAndStartSession()
        } else {
            Toast.makeText(requireContext(), "Notification permission is required to show the timer.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            val spinnerGoals: Spinner = view.findViewById(R.id.spinner_goal_selector)
            val timerView: CircularTimerView = view.findViewById(R.id.timer_view)
            val btnStart: MaterialButton = view.findViewById(R.id.btn_start)
            val btnComplete: MaterialButton = view.findViewById(R.id.btn_complete)
            val btnPauseContainer: LinearLayout = view.findViewById(R.id.btn_pause_container)
            // Fix: Avoid dangerous cast by checking if findView returns an ImageView
            val btnPauseIcon: ImageView? = view.findViewById(R.id.iv_pause_icon)
            val btnCancelContainer: LinearLayout = view.findViewById(R.id.btn_cancel_container)
            val llControlButtons: LinearLayout = view.findViewById(R.id.ll_control_buttons)
            val tvDndBadge: TextView = view.findViewById(R.id.tv_dnd_badge)
            val tvFocusLockBadge: TextView = view.findViewById(R.id.tv_focus_lock_badge)
            val tvGoalName: TextView = view.findViewById(R.id.tv_goal_name)
            val tvGoalSubtitle: TextView = view.findViewById(R.id.tv_goal_subtitle)
            val cvGoalSelector: MaterialCardView = view.findViewById(R.id.cv_goal_selector)
            val cvActiveBadge: MaterialCardView = view.findViewById(R.id.cv_active_badge)
            ivCommonInfo = view.findViewById(R.id.iv_common_info)
            cvCommonProfile = view.findViewById(R.id.cv_common_profile_mini)
            ivCommonMenu = view.findViewById(R.id.iv_common_menu)
            ivCommonAgent = view.findViewById(R.id.iv_common_agent)
            val tvCommonInitial = view.findViewById<TextView>(R.id.tv_common_profile_initial_mini)
            val ivCommonProfileImage = view.findViewById<ImageView>(R.id.iv_common_profile_image_mini)
            ivCommonMenu?.setOnClickListener {
                (activity as? MainActivity)?.openDrawer()
            }
            ivCommonAgent?.setOnClickListener {
                showAgentInstructionsDialog()
            }
            val progressBar = view.findViewById<android.widget.ProgressBar>(R.id.progress_bar)

            val cvModeSelector: MaterialCardView = view.findViewById(R.id.cv_mode_selector)
            val modePomodoro = view.findViewById<MaterialCardView>(R.id.mode_pomodoro)
            val modeDeepWork = view.findViewById<MaterialCardView>(R.id.mode_deep_work)
            val modeStudySprint = view.findViewById<MaterialCardView>(R.id.mode_study_sprint)
            val modeCustom = view.findViewById<MaterialCardView>(R.id.mode_custom)

            val cvSmartRecommendation: MaterialCardView = view.findViewById(R.id.cv_smart_recommendation)
            val tvSmartRecommendationText: TextView = view.findViewById(R.id.tv_smart_recommendation_text)
            val btnApplyRecommendation: MaterialButton = view.findViewById(R.id.btn_apply_recommendation)

            view.findViewById<View>(R.id.btn_add_goal)?.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, AddFocusFragment())
                    .addToBackStack(null)
                    .commit()
            }

            modePomodoro.setOnClickListener {
                val nextMode = if (selectedMode == com.zenzone.app.model.FocusMode.POMODORO) null else com.zenzone.app.model.FocusMode.POMODORO
                selectFocusMode(nextMode, modePomodoro, modeDeepWork, modeStudySprint, modeCustom, timerView)
            }
            modeDeepWork.setOnClickListener {
                val nextMode = if (selectedMode == com.zenzone.app.model.FocusMode.DEEP_WORK) null else com.zenzone.app.model.FocusMode.DEEP_WORK
                selectFocusMode(nextMode, modePomodoro, modeDeepWork, modeStudySprint, modeCustom, timerView)
            }
            modeStudySprint.setOnClickListener {
                val nextMode = if (selectedMode == com.zenzone.app.model.FocusMode.STUDY_SPRINT) null else com.zenzone.app.model.FocusMode.STUDY_SPRINT
                selectFocusMode(nextMode, modePomodoro, modeDeepWork, modeStudySprint, modeCustom, timerView)
            }
            modeCustom.setOnClickListener {
                if (selectedMode == com.zenzone.app.model.FocusMode.CUSTOM) {
                    selectFocusMode(null, modePomodoro, modeDeepWork, modeStudySprint, modeCustom, timerView)
                } else {
                    val input = android.widget.EditText(requireContext()).apply {
                        inputType = android.text.InputType.TYPE_CLASS_NUMBER
                        setText(customMinutes.toString())
                        setSelection(text.length)
                    }
                    val padding = (24 * resources.displayMetrics.density).toInt()
                    val container = android.widget.FrameLayout(requireContext()).apply {
                        addView(input, android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            leftMargin = padding
                            rightMargin = padding
                            topMargin = padding / 2
                            bottomMargin = padding / 2
                        })
                    }

                    AlertDialog.Builder(requireContext())
                        .setTitle("Custom Focus Duration")
                        .setMessage("Enter focus duration in minutes (1 - 180):")
                        .setView(container)
                        .setPositiveButton("Set") { _, _ ->
                            val entered = input.text.toString().toIntOrNull()
                            if (entered != null && entered in 1..180) {
                                customMinutes = entered
                                view.findViewById<TextView>(R.id.tv_custom_mode_duration)?.text = "$customMinutes min"
                                selectFocusMode(com.zenzone.app.model.FocusMode.CUSTOM, modePomodoro, modeDeepWork, modeStudySprint, modeCustom, timerView)
                            } else {
                                Toast.makeText(requireContext(), "Please enter a valid duration between 1 and 180 minutes.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }

            selectFocusMode(null, modePomodoro, modeDeepWork, modeStudySprint, modeCustom, timerView)

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
                }
            }
        
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
                    
                    val currentSelected = viewModel.selectedGoal.value
                    val targetPosition = if (currentSelected != null) {
                        goals.indexOfFirst { it.id == currentSelected.id }
                    } else {
                        -1
                    }

                    if (targetPosition != -1) {
                        spinnerGoals.setSelection(targetPosition)
                        viewModel.selectGoal(goals[targetPosition])
                    } else {
                        spinnerGoals.setSelection(0)
                        viewModel.selectGoal(goals[0])
                    }
                    
                    spinnerGoals.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                             viewModel.selectGoal(goals[position])
                             updateTimerDisplay(timerView)
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }

                    // Check if there is a pending mode to apply from the Zen Agent
                    val pendingMode = com.zenzone.app.utils.AgentActionManager.pendingModeToSet
                    if (pendingMode != null) {
                        com.zenzone.app.utils.AgentActionManager.pendingModeToSet = null
                        try {
                            selectedMode = com.zenzone.app.model.FocusMode.valueOf(pendingMode)
                            selectFocusMode(selectedMode, modePomodoro, modeDeepWork, modeStudySprint, modeCustom, timerView)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    // Check if there is a pending goal to auto-start from the Zen Agent
                    val pendingGoalId = com.zenzone.app.utils.AgentActionManager.pendingGoalToStart
                    if (pendingGoalId != null) {
                        com.zenzone.app.utils.AgentActionManager.pendingGoalToStart = null
                        val goal = goals.find { it.id == pendingGoalId || it.name.equals(pendingGoalId, ignoreCase = true) }
                        if (goal != null) {
                            viewModel.selectGoal(goal)
                            
                            // Auto start the timer session
                            view.postDelayed({
                                checkDndAndStartSession()
                            }, 500)
                        }
                    }
                } else {
                    viewModel.selectGoal(null)
                    tvGoalName.text = "No Goals Yet"
                    tvGoalSubtitle.text = "Add a focus goal in the Home tab to get started"
                    timerView.update(0L, 0L)
                    
                    tvGoalName.setOnClickListener {
                        (activity as? MainActivity)?.navigateToMenuItem(R.id.nav_home)
                    }
                }
            }

            viewModel.selectedGoal.observe(viewLifecycleOwner) { goal ->
                goal?.let {
                    tvGoalName.text = it.name
                    tvGoalSubtitle.text = "Target: ${it.targetMinutes} min · Chain: ${it.currentChain} 🔥"
                    updateTimerDisplay(timerView)
                }
            }

            viewModel.remainingTimeMs.observe(viewLifecycleOwner) { _ ->
                updateTimerDisplay(timerView)
            }

            viewModel.totalDurationMs.observe(viewLifecycleOwner) { _ ->
                updateTimerDisplay(timerView)
            }

            viewModel.isRunning.observe(viewLifecycleOwner) { isRunning ->
                btnStart.visibility = if (isRunning) View.GONE else View.VISIBLE
                btnComplete.visibility = if (isRunning) View.VISIBLE else View.GONE
                llControlButtons.visibility = if (isRunning) View.VISIBLE else View.GONE
                cvGoalSelector.visibility = if (isRunning) View.GONE else View.VISIBLE
                cvModeSelector.visibility = if (isRunning) View.GONE else View.VISIBLE
                cvActiveBadge.visibility = if (isRunning) View.VISIBLE else View.GONE
                cvSmartRecommendation.visibility = if (isRunning) View.GONE else (if (viewModel.smartRecommendation.value != null) View.VISIBLE else View.GONE)
                
                spinnerGoals.isEnabled = !isRunning
                tvFocusLockBadge.visibility = if (isRunning) View.VISIBLE else View.GONE
                
                val mainActivity = activity as? com.zenzone.app.ui.main.MainActivity
                if (isRunning) {
                    mainActivity?.enableFocusLock()
                } else {
                    mainActivity?.disableFocusLock()
                }

                // Disable non-essential views during active focus session
                cvCommonProfile?.isEnabled = !isRunning
                ivCommonInfo?.isEnabled = !isRunning
                ivCommonMenu?.isEnabled = !isRunning
                ivCommonAgent?.isEnabled = !isRunning

                val alphaVal = if (isRunning) 0.4f else 1.0f
                cvCommonProfile?.alpha = alphaVal
                ivCommonInfo?.alpha = alphaVal
                ivCommonMenu?.alpha = alphaVal
                ivCommonAgent?.alpha = alphaVal

                updateTimerDisplay(timerView)
            }

            viewModel.isPaused.observe(viewLifecycleOwner) { isPaused ->
                btnPauseIcon?.setImageResource(
                    if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
                )
            }

            viewModel.isDndActive.observe(viewLifecycleOwner) { dnd ->
                tvDndBadge.visibility = if (dnd) View.VISIBLE else View.GONE
            }

            viewModel.completedPendingLog.observe(viewLifecycleOwner) { pending ->
                pending?.let { (goal, minutes) ->
                    val dialogView = layoutInflater.inflate(R.layout.dialog_session_complete, null)
                    val dialog = AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .setCancelable(false)
                        .create()

                    // Setup Lottie firework celebration
                    val lottieCelebration = dialogView.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottie_celebration)
                    lottieCelebration.setFailureListener { throwable ->
                        throwable.printStackTrace()
                    }
                    lottieCelebration.setAnimationFromUrl("https://assets2.lottiefiles.com/packages/lf20_81x92z.json")

                    val etNotes = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_session_notes)

                    dialogView.findViewById<View>(R.id.btn_dialog_log).setOnClickListener {
                        val notes = etNotes?.text?.toString()?.trim()
                        viewModel.logSession(goal, minutes, if (notes.isNullOrBlank()) null else notes)
                        FocusTimerService.clearPendingLog()
                        dialog.dismiss()

                        if (selectedMode == com.zenzone.app.model.FocusMode.POMODORO) {
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.nav_host_fragment, com.zenzone.app.ui.wellness.BreathingFragment())
                                .addToBackStack(null)
                                .commit()
                        }
                    }

                    dialogView.findViewById<View>(R.id.tv_dialog_discard).setOnClickListener {
                        FocusTimerService.clearPendingLog()
                        dialog.dismiss()
                    }

                    dialog.show()
                }
            }

            viewModel.focusEvents.observe(viewLifecycleOwner) { event ->
                event?.let {
                    when (it) {
                        is FocusEvent.SessionComplete -> {
                            val msg = "🎉 Session Complete!\n+${it.xpGained} XP · Chain: ${it.oldChain} → ${it.newChain} 🔥"
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                            if (it.newlyUnlockedBadges.isNotEmpty()) {
                                val badgeNames = it.newlyUnlockedBadges.joinToString(", ") { badge -> badge.name }
                                Toast.makeText(requireContext(), "🏆 New Badge Unlocked: $badgeNames!", Toast.LENGTH_LONG).show()
                            }
                            if (it.completedChallenges.isNotEmpty()) {
                                it.completedChallenges.forEach { challengeTitle ->
                                    Toast.makeText(requireContext(), "🏆 Challenge Completed: $challengeTitle!", Toast.LENGTH_LONG).show()
                                }
                            }
                            viewModel.clearEvent()
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    checkDndAndStartSession()
                }
            }

            btnComplete.setOnClickListener {
                viewModel.selectedGoal.value?.let { goal ->
                    AlertDialog.Builder(requireContext())
                        .setTitle("Complete Session")
                        .setMessage("Log this session for ${goal.name}?")
                        .setPositiveButton("Yes") { _, _ ->
                            viewModel.logSession(goal, goal.targetMinutes)
                            viewModel.stopTimer()
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            }

            btnPauseContainer.setOnClickListener {
                if (viewModel.isPaused.value == false) {
                    viewModel.pauseTimer()
                    Toast.makeText(requireContext(), "Session paused", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.resumeTimer()
                    Toast.makeText(requireContext(), "Session resumed", Toast.LENGTH_SHORT).show()
                }
            }

            btnCancelContainer.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Cancel Session")
                    .setMessage("Are you sure you want to cancel this session? Progress will not be saved.")
                    .setPositiveButton("Yes, Cancel") { _, _ ->
                        viewModel.stopTimer()
                        Toast.makeText(requireContext(), "Session cancelled", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("No", null)
                    .show()
            }

            ivCommonInfo?.setOnClickListener {
                showXpInfoDialog()
            }

            cvCommonProfile?.setOnClickListener {
                (activity as? MainActivity)?.navigateToMenuItem(R.id.nav_profile)
            }

            viewModel.smartRecommendation.observe(viewLifecycleOwner) { recommendation ->
                if (recommendation != null && viewModel.isRunning.value != true) {
                    tvSmartRecommendationText.text = recommendation.displayMessage
                    cvSmartRecommendation.visibility = View.VISIBLE
                } else {
                    cvSmartRecommendation.visibility = View.GONE
                }
            }

            btnApplyRecommendation.setOnClickListener {
                viewModel.smartRecommendation.value?.let { rec ->
                    val matchedMode = when (rec.recommendedDuration) {
                        25 -> com.zenzone.app.model.FocusMode.POMODORO
                        45 -> com.zenzone.app.model.FocusMode.DEEP_WORK
                        50 -> com.zenzone.app.model.FocusMode.STUDY_SPRINT
                        else -> {
                            customMinutes = rec.recommendedDuration
                            view.findViewById<TextView>(R.id.tv_custom_mode_duration)?.text = "${rec.recommendedDuration} min"
                            com.zenzone.app.model.FocusMode.CUSTOM
                        }
                    }
                    selectFocusMode(
                        matchedMode,
                        modePomodoro,
                        modeDeepWork,
                        modeStudySprint,
                        modeCustom,
                        timerView
                    )
                    Toast.makeText(requireContext(), "Focus settings applied: ${rec.recommendedDuration} min!", Toast.LENGTH_SHORT).show()
                }
            }

            viewModel.loadGoals()
            viewModel.loadSmartRecommendation()
            viewModel.loadUserProfile()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error loading focus screen: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserProfile()
        viewModel.loadSmartRecommendation()
        viewModel.loadGoals()
    }

    private var selectedSoundscape = com.zenzone.app.utils.SoundscapePlayer.SOUNDSCAPE_NONE

    private fun checkDndAndStartSession() {
        if (!DndHelper.hasDndPermission(requireContext())) {
            AlertDialog.Builder(requireContext())
                .setTitle("DND Permission Required")
                .setMessage("To silence notifications during your zen session, please grant ZenZone Do Not Disturb access.")
                .setPositiveButton("Grant Access") { _, _ ->
                     DndHelper.requestDndPermission(requireContext())
                }
                .setNegativeButton("Skip") { _, _ -> 
                     checkUsageStatsAndStartSession(false)
                }
                .show()
        } else {
            checkUsageStatsAndStartSession(true)
        }
    }

    private fun checkUsageStatsAndStartSession(useDnd: Boolean) {
        if (!com.zenzone.app.utils.UsageStatsHelper.hasUsageStatsPermission(requireContext())) {
            AlertDialog.Builder(requireContext())
                .setTitle("Zen Mode: App Guard")
                .setMessage("To detect and nudge you away from distracting apps (like Instagram & TikTok) during your focus session, please grant ZenZone Usage Stats access.")
                .setPositiveButton("Grant Access") { _, _ ->
                     com.zenzone.app.utils.UsageStatsHelper.requestUsageStatsPermission(requireContext())
                }
                .setNegativeButton("Skip") { _, _ -> 
                     viewModel.selectedGoal.value?.let { goal ->
                          val mode = selectedMode
                          val duration = if (mode == null) goal.targetMinutes else (if (mode != com.zenzone.app.model.FocusMode.CUSTOM) mode.durationMinutes else customMinutes)
                          val sound = if (mode == null) "None" else (if (mode != com.zenzone.app.model.FocusMode.CUSTOM) mode.defaultSoundscape else selectedSoundscape)
                          val sensitivity = if (mode == null) "GENTLE" else mode.distractionSensitivity
                          viewModel.startTimer(goal, useDnd, sound, duration, sensitivity)
                     }
                }
                .show()
        } else {
            viewModel.selectedGoal.value?.let { goal ->
                  val mode = selectedMode
                  val duration = if (mode == null) goal.targetMinutes else (if (mode != com.zenzone.app.model.FocusMode.CUSTOM) mode.durationMinutes else customMinutes)
                  val sound = if (mode == null) "None" else (if (mode != com.zenzone.app.model.FocusMode.CUSTOM) mode.defaultSoundscape else selectedSoundscape)
                  val sensitivity = if (mode == null) "GENTLE" else mode.distractionSensitivity
                  viewModel.startTimer(goal, useDnd, sound, duration, sensitivity)
            }
        }
    }

    private fun selectFocusMode(
        mode: com.zenzone.app.model.FocusMode?,
        modePomodoro: MaterialCardView,
        modeDeepWork: MaterialCardView,
        modeStudySprint: MaterialCardView,
        modeCustom: MaterialCardView,
        timerView: CircularTimerView
    ) {
        selectedMode = mode
        
        val inactiveBg = ContextCompat.getColor(requireContext(), R.color.zen_slate_surface)
        val activeBg = ContextCompat.getColor(requireContext(), R.color.zen_teal_light)
        val activeStroke = ContextCompat.getColor(requireContext(), R.color.zen_teal_primary)
        val inactiveStroke = android.graphics.Color.TRANSPARENT

        val cards = listOf(modePomodoro, modeDeepWork, modeStudySprint, modeCustom)
        cards.forEach { card ->
            card.setCardBackgroundColor(inactiveBg)
            card.strokeColor = inactiveStroke
            card.strokeWidth = 0
        }

        if (mode != null) {
            val activeCard = when (mode) {
                com.zenzone.app.model.FocusMode.POMODORO -> modePomodoro
                com.zenzone.app.model.FocusMode.DEEP_WORK -> modeDeepWork
                com.zenzone.app.model.FocusMode.STUDY_SPRINT -> modeStudySprint
                com.zenzone.app.model.FocusMode.CUSTOM -> modeCustom
            }
            activeCard.setCardBackgroundColor(activeBg)
            activeCard.strokeColor = activeStroke
            activeCard.strokeWidth = 4
        }

        updateTimerDisplay(timerView)
    }

    private fun updateTimerDisplay(timerView: CircularTimerView) {
        val isRunning = viewModel.isRunning.value == true
        if (isRunning) {
            val remainMs = viewModel.remainingTimeMs.value ?: 0L
            val totalMs = viewModel.totalDurationMs.value ?: (25 * 60 * 1000L)
            timerView.update(remainMs, totalMs)
        } else {
            val goal = viewModel.selectedGoal.value
            val durationMins = if (selectedMode == null) {
                goal?.targetMinutes ?: 25
            } else if (selectedMode != com.zenzone.app.model.FocusMode.CUSTOM) {
                selectedMode!!.durationMinutes
            } else {
                customMinutes
            }
            val totalMs = durationMins * 60 * 1000L
            timerView.update(totalMs, totalMs)
        }
    }

    private fun showXpInfoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_xp_info, null)
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)
        
        dialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.zen_slate_dark)
        )
        
        dialogView.findViewById<MaterialButton>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun showAgentInstructionsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_agent_instructions, null)
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        dialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.zen_slate_dark)
        )

        dialogView.findViewById<View>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
