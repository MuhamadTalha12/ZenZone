package com.zenzone.app.ui.social

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.zenzone.app.R
import com.zenzone.app.model.FocusGoal
import com.zenzone.app.repository.FocusRepository
import com.zenzone.app.repository.UserRepository
import com.zenzone.app.ui.main.MainActivity
import com.zenzone.app.utils.AgentActionManager
import com.zenzone.app.utils.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import com.zenzone.app.ui.home.HomeFragment
import com.zenzone.app.ui.focus.FocusFragment
import com.zenzone.app.ui.challenge.DailyChallengeFragment
import com.zenzone.app.ui.stats.StatsFragment
import com.zenzone.app.ui.profile.ProfileFragment
import com.zenzone.app.ui.garden.ZenGardenFragment
import androidx.lifecycle.ViewModelProvider
import com.zenzone.app.viewmodel.HomeViewModel
import com.zenzone.app.viewmodel.HomeViewModelFactory

class ZenAgentDialog : BottomSheetDialogFragment() {

    private lateinit var llChatContainer: LinearLayout
    private lateinit var svChatScroll: NestedScrollView
    private lateinit var etAgentInput: EditText
    private lateinit var btnSendAgent: ImageButton
    private var mainActivity: MainActivity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_zen_agent, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        llChatContainer = view.findViewById(R.id.ll_chat_container)
        svChatScroll = view.findViewById(R.id.sv_chat_scroll)
        etAgentInput = view.findViewById(R.id.et_agent_input)
        btnSendAgent = view.findViewById(R.id.btn_send_agent)

        view.findViewById<View>(R.id.btn_close_agent)?.setOnClickListener {
            dismiss()
        }

        // Bind quick suggestion chips
        view.findViewById<Chip>(R.id.chip_add_coding)?.setOnClickListener {
            etAgentInput.setText("create coding for 45 min")
            btnSendAgent.performClick()
        }
        view.findViewById<Chip>(R.id.chip_run_coding)?.setOnClickListener {
            etAgentInput.setText("run coding")
            btnSendAgent.performClick()
        }
        view.findViewById<Chip>(R.id.chip_stats)?.setOnClickListener {
            etAgentInput.setText("show stats")
            btnSendAgent.performClick()
        }
        view.findViewById<Chip>(R.id.chip_set_mode)?.setOnClickListener {
            etAgentInput.setText("set mode to pomodoro")
            btnSendAgent.performClick()
        }

        btnSendAgent.setOnClickListener {
            val cmd = etAgentInput.text.toString()
            if (cmd.isNotBlank()) {
                parseCommand(cmd)
            }
        }

        // Welcome message
        addMessage("Hi 🙏. I am your Zen Agent. How can I help you focus today?", isUser = false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            mainActivity = context
        }
    }

    private fun addMessage(text: String, isUser: Boolean) {
        val context = context ?: return
        val messageView = LayoutInflater.from(context).inflate(
            if (isUser) R.layout.item_chat_user else R.layout.item_chat_agent,
            llChatContainer,
            false
        )
        messageView.findViewById<TextView>(R.id.tv_message_text).text = text
        llChatContainer.addView(messageView)
        svChatScroll.post {
            svChatScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun parseCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return

        addMessage(trimmed, isUser = true)
        etAgentInput.setText("")

        // Clean command: lowercase and remove trailing punctuation (. ! ?)
        val cleaned = trimmed.lowercase()
            .trim { it <= ' ' || it == '.' || it == '!' || it == '?' }

        lifecycleScope.launch {
            try {
                val focusRepo = FocusRepository(requireContext())
                val userRepo = UserRepository(requireContext())
                val factory = HomeViewModelFactory(focusRepo, userRepo)
                val homeViewModel = ViewModelProvider(requireActivity(), factory)[HomeViewModel::class.java]

                // Regex pattern definitions
                val addGoalWithTimeRegex = Regex("""(?:create|add|new|setup|make)\s+(?:goal\s+|focus\s+|a\s+goal\s+|a\s+focus\s+|new\s+goal\s+|a\s+new\s+goal\s+|focus\s+goal\s+|a\s+focus\s+goal\s+)?([a-zA-Z0-9\s\-]+?)\s+(?:for\s+)?(\d+)\s*(?:min|minutes|m)?""")
                val addGoalOnlyRegex = Regex("""(?:create|add|new|setup|make)\s+(?:goal\s+|focus\s+|a\s+goal\s+|a\s+focus\s+|new\s+goal\s+|a\s+new\s+goal\s+|focus\s+goal\s+|a\s+focus\s+goal\s+)?([a-zA-Z0-9\s\-]+)""")

                val deleteGoalRegex = Regex("""(?:delete|remove|clear|erase|drop)\s+(?:goal\s+|focus\s+|the\s+goal\s+|the\s+focus\s+|focus\s+goal\s+|the\s+focus\s+goal\s+)?([a-zA-Z0-9\s\-]+)""")

                val changeTimeRegex = Regex("""(?:change|set|update|modify|change\s+time\s+of)\s+(?:time\s+of\s+|target\s+of\s+|duration\s+of\s+|target\s+time\s+of\s+|target\s+time\s+|time\s+|target\s+|duration\s+)?([a-zA-Z0-9\s\-]+?)\s+(?:from\s+(\d+)\s*(?:min|minutes|m)?\s+)?to\s+(\d+)\s*(?:min|minutes|m)?""")

                val navigateRegex = Regex("""(?:navigate|go|open|show)\s+(?:to\s+)?(home|focus|challenge|challenges|daily\s+challenges?|garden|zen\s+garden|stats|statistics|profile)""")
                val navigateAskRegex = Regex("""(?:navigate|go|open|show|go\s+to)(?:\s+(?:any\s*page|anypage|a\s+page|page))?""")

                val runGoalRegex = Regex("""(?:run|start|focus\s+on|do)\s+([a-zA-Z0-9\s\-]+)""")
                val setModeRegex = Regex("""(?:set|change)\s+(?:mode|focus\s+mode)\s+(?:to\s+)?(pomodoro|deep\s+work|study\s+sprint|custom)""")
                val showStatsRegex = Regex("""(?:show\s+stats|my\s+level|what\s+is\s+my\s+level|my\s+progress|get\s+stats)""")
                val helpRegex = Regex("""(?:help|what\s+can\s+you\s+do|commands)""")

                when {
                    helpRegex.find(cleaned) != null -> {
                        showHelp()
                    }
                    showStatsRegex.find(cleaned) != null -> {
                        val userRepo = UserRepository(requireContext())
                        val profile = userRepo.loadProfile()
                        val msg = "🧘 Here is your Zen progress:\n\n" +
                                "• Level: ${profile.zenLevel} Practitioner\n" +
                                "• XP: ${profile.zenXP} XP\n" +
                                "• Total Completed Sessions: ${profile.totalSessions}\n" +
                                "• Total Focused Time: ${profile.totalFocusedMinutes} minutes\n" +
                                "• Longest Streak: ${profile.longestEverChain} days 🔥\n\nKeep focusing to grow your Zen Garden! 🌸"
                        addMessage(msg, isUser = false)
                    }
                    navigateRegex.find(cleaned) != null -> {
                        val match = navigateRegex.find(cleaned)!!
                        val pageStr = match.groupValues[1].trim().lowercase()

                        val checkedId = when (pageStr) {
                            "home" -> R.id.nav_home
                            "focus" -> R.id.nav_focus
                            "challenge", "challenges", "daily challenge", "daily challenges" -> R.id.nav_challenges
                            "stats", "statistics" -> R.id.nav_stats
                            "profile" -> R.id.nav_profile
                            else -> -1
                        }

                        if (checkedId != -1) {
                            addMessage("🧘 Opening **${pageStr.uppercase()}** page...", isUser = false)
                            delay(1000)
                            dismiss()
                            mainActivity?.navigateToMenuItem(checkedId)
                        } else if (pageStr == "garden" || pageStr == "zen garden") {
                            addMessage("🧘 Opening **ZEN GARDEN** page...", isUser = false)
                            delay(1000)
                            dismiss()
                            mainActivity?.let { activity ->
                                activity.supportFragmentManager.beginTransaction()
                                    .replace(R.id.nav_host_fragment, ZenGardenFragment())
                                    .commit()
                            }
                        } else {
                            addMessage("❌ I couldn't find a page matching '$pageStr'. You can go to Home, Focus, Daily Challenges, Stats, Profile, or Zen Garden.", isUser = false)
                        }
                    }
                    navigateAskRegex.find(cleaned) != null -> {
                        addMessage("🧘 Which page would you like to navigate to?\n\nYou can choose from:\n• Home\n• Focus\n• Daily Challenges\n• Stats\n• Profile\n• Zen Garden", isUser = false)
                    }
                    changeTimeRegex.find(cleaned) != null -> {
                        val match = changeTimeRegex.find(cleaned)!!
                        val name = match.groupValues[1].trim()
                        val oldTimeStr = match.groupValues[2]
                        val newTimeStr = match.groupValues[3]
                        val minutes = newTimeStr.toInt()

                        val existing = focusRepo.loadGoals()
                        val goalToUpdate = existing.find { 
                            it.name.equals(name, ignoreCase = true) || 
                            it.name.contains(name, ignoreCase = true) ||
                            name.contains(it.name, ignoreCase = true)
                        }

                        if (goalToUpdate != null) {
                            val updated = goalToUpdate.copy(targetMinutes = minutes)
                            homeViewModel.updateGoal(updated)
                            if (oldTimeStr.isNotEmpty()) {
                                addMessage("🧘 I have updated the focus target for **${goalToUpdate.name}** from **$oldTimeStr minutes** to **$minutes minutes**! ⏱️", isUser = false)
                            } else {
                                addMessage("🧘 I have updated the focus target for **${goalToUpdate.name}** to **$minutes minutes**! ⏱️", isUser = false)
                            }
                        } else {
                            addMessage("❌ I couldn't find a focus goal matching '$name' to update.", isUser = false)
                        }
                    }
                    addGoalWithTimeRegex.find(cleaned) != null -> {
                        val match = addGoalWithTimeRegex.find(cleaned)!!
                        val name = match.groupValues[1].trim().replaceFirstChar { it.uppercase() }
                        val minutes = match.groupValues[2].toInt()

                        val existing = focusRepo.loadGoals()
                        val isUpdate = existing.any { it.name.equals(name, ignoreCase = true) }

                        val newGoal = FocusGoal(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            targetMinutes = minutes,
                            frequency = "Daily",
                            currentChain = 0,
                            longestChain = 0,
                            lastCompletedDate = null,
                            totalMinutesFocused = 0L,
                            createdAt = DateUtils.getIsoTimestamp(),
                            colorTag = "#2A9D8F",
                            isSynced = false
                        )
                        homeViewModel.addGoal(newGoal)

                        val msg = if (isUpdate) {
                            "🧘 I have updated the focus goal: **$name** with a new target of **$minutes minutes**! ⏱️"
                        } else {
                            "🧘 I have created a new focus goal: **$name** with a daily target of **$minutes minutes**! 🎯"
                        }
                        addMessage(msg, isUser = false)
                    }
                    addGoalOnlyRegex.find(cleaned) != null -> {
                        val match = addGoalOnlyRegex.find(cleaned)!!
                        val name = match.groupValues[1].trim().replaceFirstChar { it.uppercase() }
                        val minutes = 25 // Default fallback duration

                        val existing = focusRepo.loadGoals()
                        val isUpdate = existing.any { it.name.equals(name, ignoreCase = true) }

                        val newGoal = FocusGoal(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            targetMinutes = minutes,
                            frequency = "Daily",
                            currentChain = 0,
                            longestChain = 0,
                            lastCompletedDate = null,
                            totalMinutesFocused = 0L,
                            createdAt = DateUtils.getIsoTimestamp(),
                            colorTag = "#2A9D8F",
                            isSynced = false
                        )
                        homeViewModel.addGoal(newGoal)

                        val msg = if (isUpdate) {
                            "🧘 I have updated the focus goal: **$name** to a target of **$minutes minutes**! ⏱️"
                        } else {
                            "🧘 I have created a new focus goal: **$name** (defaulting to **$minutes minutes**)! 🎯"
                        }
                        addMessage(msg, isUser = false)
                    }
                    deleteGoalRegex.find(cleaned) != null -> {
                        val match = deleteGoalRegex.find(cleaned)!!
                        val name = match.groupValues[1].trim()

                        val existing = focusRepo.loadGoals()
                        val goalToDelete = existing.find { 
                            it.name.equals(name, ignoreCase = true) || 
                            it.name.contains(name, ignoreCase = true) ||
                            name.contains(it.name, ignoreCase = true)
                        }

                        if (goalToDelete != null) {
                            homeViewModel.deleteGoal(goalToDelete.id)
                            addMessage("🧘 I have removed the focus goal: **${goalToDelete.name}**! 🗑️", isUser = false)
                        } else {
                            addMessage("❌ I couldn't find a focus goal matching '$name' to delete.", isUser = false)
                        }
                    }
                    runGoalRegex.find(cleaned) != null -> {
                        val match = runGoalRegex.find(cleaned)!!
                        val name = match.groupValues[1].trim()

                        val focusRepo = FocusRepository(requireContext())
                        val goals = focusRepo.loadGoals()
                        val matchedGoal = goals.find { 
                            it.name.equals(name, ignoreCase = true) || 
                            it.name.contains(name, ignoreCase = true) ||
                            name.contains(it.name, ignoreCase = true)
                        }

                        if (matchedGoal != null) {
                            AgentActionManager.pendingGoalToStart = matchedGoal.id
                            addMessage("🧘 Starting the focus session for **${matchedGoal.name}**! Navigating to Focus...", isUser = false)
                            
                            delay(1000)
                            dismiss()
                            mainActivity?.navigateToMenuItem(R.id.nav_focus)
                        } else {
                            addMessage("❌ I couldn't find a focus goal matching '$name'. Ask me to 'create goal $name 25 min' first!", isUser = false)
                        }
                    }
                    setModeRegex.find(cleaned) != null -> {
                        val match = setModeRegex.find(cleaned)!!
                        val modeStr = match.groupValues[1].trim().lowercase()
                        
                        val modeName = when (modeStr) {
                            "pomodoro" -> "POMODORO"
                            "deep work", "deepwork" -> "DEEP_WORK"
                            "study sprint", "studysprint" -> "STUDY_SPRINT"
                            else -> "CUSTOM"
                        }
                        
                        AgentActionManager.pendingModeToSet = modeName
                        addMessage("🧘 Setting focus mode to **${modeStr.uppercase()}**! Navigating to Focus...", isUser = false)
                        
                        delay(1000)
                        dismiss()
                        mainActivity?.navigateToMenuItem(R.id.nav_focus)
                    }
                    else -> {
                        // Substring match fallback for starting timer
                        val focusRepo = FocusRepository(requireContext())
                        val goals = focusRepo.loadGoals()
                        val matchedGoal = goals.find { cleaned.contains(it.name.lowercase()) }
                        
                        if (matchedGoal != null && (cleaned.contains("run") || cleaned.contains("start") || cleaned.contains("focus") || cleaned.contains("do"))) {
                            AgentActionManager.pendingGoalToStart = matchedGoal.id
                            addMessage("🧘 Substring match found: Starting focus session for **${matchedGoal.name}**! Navigating to Focus...", isUser = false)
                            delay(1000)
                            dismiss()
                            mainActivity?.navigateToMenuItem(R.id.nav_focus)
                        } else {
                            showHelp()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                addMessage("❌ Error executing command: ${e.message}", isUser = false)
            }
        }
    }

    private fun showHelp() {
        addMessage("🤔 I'm here to help you focus. Try asking me to:\n\n" +
                "• \"create goal Exercise for 30 min\"\n" +
                "• \"add goal Reading\" (defaults to 25 min)\n" +
                "• \"delete goal Exercise\"\n" +
                "• \"change target time of Exercise from 30 min to 45 min\"\n" +
                "• \"change Exercise to 45 min\"\n" +
                "• \"run Exercise\"\n" +
                "• \"set mode to Deep Work\"\n" +
                "• \"go to Daily Challenges\"\n" +
                "• \"navigate\" (prompts for options)\n" +
                "• \"show stats\"", isUser = false)
    }

    companion object {
        fun show(context: Context, fragmentManager: androidx.fragment.app.FragmentManager, activity: MainActivity?) {
            val dialog = ZenAgentDialog()
            dialog.show(fragmentManager, "zen_agent_dialog")
        }
    }
}
