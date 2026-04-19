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
import com.zenzone.app.utils.Constants

class EditFocusDialogFragment(
    private val goal: FocusGoal,
    private val onUpdate: (FocusGoal) -> Unit,
    private val onDelete: (String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_edit_focus, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Override the bottom sheet container background
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
        val btnUpdate = view.findViewById<MaterialButton>(R.id.btn_update_goal)
        val btnDelete = view.findViewById<MaterialButton>(R.id.btn_delete_goal)

        // Pre-fill with existing data
        etName.setText(goal.name)
        etMinutes.setText(goal.targetMinutes.toString())

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("Daily", "Weekly", "Flexible"))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spFreq.adapter = adapter
        spFreq.setPopupBackgroundResource(R.color.zen_slate_dark)
        
        // Set current frequency
        val freqPosition = when (goal.frequency) {
            "Daily" -> 0
            "Weekly" -> 1
            "Flexible" -> 2
            else -> 0
        }
        spFreq.setSelection(freqPosition)

        // Set current color
        val colorMap = mapOf(
            "#2A9D8F" to R.id.rb_color1,
            "#E76F51" to R.id.rb_color2,
            "#F4A261" to R.id.rb_color3,
            "#E9C46A" to R.id.rb_color4,
            "#264653" to R.id.rb_color5,
            "#8338EC" to R.id.rb_color6
        )
        val colorId = colorMap[goal.colorTag] ?: R.id.rb_color1
        rgColors.check(colorId)
        updateSwatchSelection(rgColors)

        rgColors.setOnCheckedChangeListener { group, _ -> updateSwatchSelection(group) }

        btnUpdate.setOnClickListener {
            val name = etName.text.toString().trim()
            val minsStr = etMinutes.text.toString().trim()
            val freq = spFreq.selectedItem.toString()

            val checkedColorId = rgColors.checkedRadioButtonId
            val colorTag = if (checkedColorId != -1) {
                view.findViewById<RadioButton>(checkedColorId).tag?.toString() ?: goal.colorTag
            } else goal.colorTag

            // Validation
            if (name.isBlank()) {
                Toast.makeText(requireContext(), "Goal name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (name.length > Constants.MAX_GOAL_NAME_LENGTH) {
                Toast.makeText(requireContext(), "Goal name cannot exceed ${Constants.MAX_GOAL_NAME_LENGTH} characters", Toast.LENGTH_SHORT).show()
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
            
            if (targetMinutes > Constants.MAX_TARGET_MINUTES) {
                Toast.makeText(requireContext(), "Target minutes cannot exceed ${Constants.MAX_TARGET_MINUTES} minutes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedGoal = goal.copy(
                name = name,
                targetMinutes = targetMinutes,
                frequency = freq,
                colorTag = colorTag
            )
            onUpdate(updatedGoal)
            dismiss()
        }

        btnDelete.setOnClickListener {
            // Show confirmation dialog
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Goal")
                .setMessage("Are you sure you want to delete \"${goal.name}\"? This will also delete all associated session history.")
                .setPositiveButton("Delete") { _, _ ->
                    onDelete(goal.id)
                    dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
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
