package com.zenzone.app.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.zenzone.app.R
import com.zenzone.app.utils.Constants

class EditProfileDialogFragment(
    private val currentName: String,
    private val currentImageUri: String?,
    private val onUpdate: (String, String?) -> Unit
) : BottomSheetDialogFragment() {

    private var selectedImageUri: Uri? = null
    private lateinit var ivProfilePreview: ImageView

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                ivProfilePreview.setImageURI(uri)
                ivProfilePreview.alpha = 1.0f
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Override the bottom sheet container background
        (dialog as? com.google.android.material.bottomsheet.BottomSheetDialog)?.let { bsd ->
            val sheet = bsd.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.setBackgroundColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.zen_slate_dark)
            )
        }

        val etName = view.findViewById<TextInputEditText>(R.id.et_profile_name)
        ivProfilePreview = view.findViewById(R.id.iv_profile_preview)
        val btnSelectImage = view.findViewById<MaterialButton>(R.id.btn_select_image)
        val btnRemoveImage = view.findViewById<MaterialButton>(R.id.btn_remove_image)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_profile)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel)

        // Pre-fill with current data
        etName.setText(currentName)
        
        // Load current image if exists
        if (!currentImageUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(currentImageUri)
                ivProfilePreview.setImageURI(uri)
                ivProfilePreview.alpha = 1.0f
                selectedImageUri = uri
            } catch (e: Exception) {
                // If URI is invalid, show placeholder
                ivProfilePreview.setImageResource(R.drawable.ic_profile)
                ivProfilePreview.alpha = 0.5f
            }
        } else {
            ivProfilePreview.setImageResource(R.drawable.ic_profile)
            ivProfilePreview.alpha = 0.5f
        }

        btnSelectImage.setOnClickListener {
            openImagePicker()
        }

        btnRemoveImage.setOnClickListener {
            selectedImageUri = null
            ivProfilePreview.setImageResource(R.drawable.ic_profile)
            ivProfilePreview.alpha = 0.5f
            Toast.makeText(requireContext(), "Profile image removed", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()

            // Validation
            if (name.isBlank()) {
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.length > Constants.MAX_USER_NAME_LENGTH) {
                Toast.makeText(
                    requireContext(),
                    "Name cannot exceed ${Constants.MAX_USER_NAME_LENGTH} characters",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val imageUriString = selectedImageUri?.toString()
            onUpdate(name, imageUriString)
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }
}
