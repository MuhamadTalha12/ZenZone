package com.zenzone.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.zenzone.app.R
import com.zenzone.app.model.FocusGoal
import com.zenzone.app.utils.DateUtils
import java.util.UUID

class AddFocusDialogFragment(private val onAdd: (FocusGoal) -> Unit) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_add_focus, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Override the bottom sheet container background so it's not white
        (dialog as? com.google.android.material.bottomsheet.BottomSheetDialog)?.let { bsd ->
            val sheet = bsd.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.setBackgroundColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.zen_slate_dark)
            )
        }

        val etName = view.findViewById<TextInputEditText>(R.id.et_focus_name)
        val etMinutes = view.findViewById<TextInputEditText>(R.id.et_target_minutes)
        val spFreq = view.findViewById<Spinner>(R.id.spinner_frequency)
        val rgColors = view.findViewById<RadioGroup>(R.id.rg_colors)
        val btnCreate = view.findViewById<MaterialButton>(R.id.btn_create_goal)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("Daily", "Weekly", "Flexible"))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spFreq.adapter = adapter
        spFreq.setPopupBackgroundResource(R.color.zen_slate_dark)

        // Pre-select first color swatch
        rgColors.check(R.id.rb_color1)
        updateSwatchSelection(rgColors)

        rgColors.setOnCheckedChangeListener { group, _ -> updateSwatchSelection(group) }

        btnCreate.setOnClickListener {
            val name = etName.text.toString().trim()
            val minsStr = etMinutes.text.toString().trim()
            val freq = spFreq.selectedItem.toString()

            val checkedColorId = rgColors.checkedRadioButtonId
            val colorTag = if (checkedColorId != -1) {
                view.findViewById<RadioButton>(checkedColorId).tag?.toString() ?: "#2A9D8F"
            } else "#2A9D8F"

            // Validation
            if (name.isBlank()) {
                Toast.makeText(requireContext(), "Goal name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (name.length > com.zenzone.app.utils.Constants.MAX_GOAL_NAME_LENGTH) {
                Toast.makeText(requireContext(), "Goal name cannot exceed ${com.zenzone.app.utils.Constants.MAX_GOAL_NAME_LENGTH} characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (minsStr.isBlank()) {
                Toast.makeText(requireContext(), "Target minutes cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val targetMinutes = minsStr.toIntOrNull()
            if (targetMinutes == null || targetMinutes <= 0) {
                Toast.makeText(requireContext(), "Target minutes must be a positive number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (targetMinutes > com.zenzone.app.utils.Constants.MAX_TARGET_MINUTES) {
                Toast.makeText(requireContext(), "Target minutes cannot exceed ${com.zenzone.app.utils.Constants.MAX_TARGET_MINUTES} minutes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val goal = FocusGoal(
                id = UUID.randomUUID().toString(),
                name = name,
                targetMinutes = targetMinutes,
                frequency = freq,
                currentChain = 0,
                longestChain = 0,
                lastCompletedDate = null,
                totalMinutesFocused = 0,
                createdAt = DateUtils.getIsoTimestamp(),
                colorTag = colorTag
            )
            onAdd(goal)
            dismiss()
        }
    }

    private fun updateSwatchSelection(group: RadioGroup) {
        for (i in 0 until group.childCount) {
            val rb = group.getChildAt(i) as? RadioButton ?: continue
            rb.scaleX = if (rb.isChecked) 1.25f else 1.0f
            rb.scaleY = if (rb.isChecked) 1.25f else 1.0f
            rb.alpha  = if (rb.isChecked) 1.0f else 0.55f
        }
    }
}
