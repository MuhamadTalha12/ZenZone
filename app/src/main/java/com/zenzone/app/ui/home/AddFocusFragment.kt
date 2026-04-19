package com.zenzone.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.zenzone.app.R
import com.zenzone.app.model.FocusGoal
import com.zenzone.app.repository.FocusRepository
import com.zenzone.app.repository.UserRepository
import com.zenzone.app.utils.Constants
import com.zenzone.app.utils.DateUtils
import com.zenzone.app.viewmodel.HomeViewModel
import com.zenzone.app.viewmodel.HomeViewModelFactory
import java.util.UUID

class AddFocusFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private var selectedFrequency = "Daily"

    private val motivations = listOf(
        "\"The secret of getting ahead is getting started.\"" to "MARK TWAIN",
        "\"Concentrate all your thoughts upon the work at hand. The sun's rays do not burn until brought to a focus.\"" to "ALEXANDER GRAHAM BELL",
        "\"Focus is a matter of deciding what things you're not going to do.\"" to "STEVE JOBS",
        "\"It is during our darkest moments that we must focus to see the light.\"" to "ARISTOTLE",
        "\"Your life is the fruit of where you focus your energy.\"" to "ZEN WISDOM",
        "\"He who has a why to live can bear almost any how.\"" to "FRIEDRICH NIETZSCHE",
        "\"Deep work is the superpower of the 21st century.\"" to "CAL NEWPORT"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_focus, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide bottom navigation
        (activity as? com.zenzone.app.ui.main.MainActivity)?.findViewById<View>(R.id.bottom_nav)?.visibility = View.GONE

        // Initialize ViewModel
        val focusRepo = FocusRepository(requireContext())
        val userRepo = UserRepository(requireContext())
        val factory = HomeViewModelFactory(focusRepo, userRepo)
        viewModel = ViewModelProvider(requireActivity(), factory)[HomeViewModel::class.java]

        setupViews(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show bottom navigation again
        (activity as? com.zenzone.app.ui.main.MainActivity)?.findViewById<View>(R.id.bottom_nav)?.visibility = View.VISIBLE
    }

    private fun setupViews(view: View) {
        val btnBack = view.findViewById<View>(R.id.btn_back)
        val tvTitle = view.findViewById<TextView>(R.id.tv_title)
        val etFocusName = view.findViewById<TextInputEditText>(R.id.et_focus_name)
        val etTargetMinutes = view.findViewById<EditText>(R.id.et_target_minutes)
        val toggleFrequency = view.findViewById<MaterialButtonToggleGroup>(R.id.toggle_frequency)
        val btnDaily = view.findViewById<MaterialButton>(R.id.btn_daily)
        val btnWeekly = view.findViewById<MaterialButton>(R.id.btn_weekly)
        val btnCreateGoal = view.findViewById<MaterialButton>(R.id.btn_create_goal)
        val tvQuote = view.findViewById<TextView>(R.id.tv_quote)
        val tvQuoteAuthor = view.findViewById<TextView>(R.id.tv_quote_author)

        // Set random motivation
        val randomMotivation = motivations.random()
        tvQuote.text = randomMotivation.first
        tvQuoteAuthor.text = randomMotivation.second

        // Set title with colored "Focus" word
        val titleText = "Define Your Focus"
        val spannableTitle = android.text.SpannableString(titleText)
        val focusStart = titleText.indexOf("Focus")
        if (focusStart != -1) {
            spannableTitle.setSpan(
                android.text.style.ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.zen_teal_primary)),
                focusStart,
                focusStart + 5,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        tvTitle.text = spannableTitle

        // Back button
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Frequency toggle listener
        toggleFrequency.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                // Update styling
                when (checkedId) {
                    R.id.btn_daily -> {
                        selectedFrequency = "Daily"
                        btnDaily.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.zen_teal_primary)
                        btnDaily.strokeWidth = 4
                        btnDaily.setTextColor(ContextCompat.getColor(requireContext(), R.color.zen_teal_primary))
                        
                        btnWeekly.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.zen_gray_text)
                        btnWeekly.strokeWidth = 2
                        btnWeekly.setTextColor(ContextCompat.getColor(requireContext(), R.color.zen_gray_text))
                    }
                    R.id.btn_weekly -> {
                        selectedFrequency = "Weekly"
                        btnWeekly.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.zen_teal_primary)
                        btnWeekly.strokeWidth = 4
                        btnWeekly.setTextColor(ContextCompat.getColor(requireContext(), R.color.zen_teal_primary))
                        
                        btnDaily.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color.zen_gray_text)
                        btnDaily.strokeWidth = 2
                        btnDaily.setTextColor(ContextCompat.getColor(requireContext(), R.color.zen_gray_text))
                    }
                }
            }
        }

        // Create Goal button
        btnCreateGoal.setOnClickListener {
            createGoal(etFocusName, etTargetMinutes)
        }
    }

    private fun createGoal(etFocusName: TextInputEditText, etTargetMinutes: EditText) {
        val name = etFocusName.text.toString().trim()
        val minutesStr = etTargetMinutes.text.toString().trim()

        if (name.isBlank()) {
            Toast.makeText(requireContext(), "Goal name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (name.length > Constants.MAX_GOAL_NAME_LENGTH) {
            Toast.makeText(requireContext(), "Goal name too long", Toast.LENGTH_SHORT).show()
            return
        }

        val targetMinutes = minutesStr.toIntOrNull()
        if (targetMinutes == null || targetMinutes <= 0) {
            Toast.makeText(requireContext(), "Enter valid minutes", Toast.LENGTH_SHORT).show()
            return
        }

        val goal = FocusGoal(
            id = UUID.randomUUID().toString(),
            name = name,
            targetMinutes = targetMinutes,
            frequency = selectedFrequency,
            currentChain = 0,
            longestChain = 0,
            lastCompletedDate = null,
            totalMinutesFocused = 0,
            createdAt = DateUtils.getIsoTimestamp(),
            colorTag = "#2A9D8F"
        )

        viewModel.addGoal(goal)
        Toast.makeText(requireContext(), "Goal created!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }
}
