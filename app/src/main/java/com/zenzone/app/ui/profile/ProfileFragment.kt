package com.zenzone.app.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.zenzone.app.R
import com.zenzone.app.ui.main.MainActivity
import com.zenzone.app.utils.Constants
import com.zenzone.app.utils.FirebaseSyncManager
import com.zenzone.app.utils.ImageUtils
import com.zenzone.app.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var badgeAdapter: BadgeAdapter
    private var isGridView = true

    // Image Picker Launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            showAdjustImageDialog(it)
        }
    }

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
            val cvStreakCard: CardView = view.findViewById(R.id.cv_streak_card)
            val tvJourneyText: TextView = view.findViewById(R.id.tv_journey_text)
            val btnStartFocusing: MaterialButton = view.findViewById(R.id.btn_start_focusing)
            val btnSettings: View = view.findViewById(R.id.btn_settings)
            val btnSwitchView: ImageButton = view.findViewById(R.id.btn_switch_view)
            val rvBadges: RecyclerView = view.findViewById(R.id.rv_badges)
            val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)

            // Firebase Sync UI Bindings
            val tvSyncStatus: TextView = view.findViewById(R.id.tv_sync_status)
            val tvSyncDesc: TextView = view.findViewById(R.id.tv_sync_desc)
            val btnSyncAuth: MaterialButton = view.findViewById(R.id.btn_sync_auth)

            fun updateSyncCard() {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    tvSyncStatus.text = "Cloud Sync: Active (Compulsory)"
                    tvSyncDesc.text = "Logged in as ${user.email}. Sync is compulsory for all practitioners."
                    btnSyncAuth.visibility = View.GONE
                } else {
                    tvSyncStatus.text = "Cloud Sync: Required"
                    tvSyncDesc.text = "Connecting to the cloud is compulsory. Please sign in or sign up."
                    btnSyncAuth.text = "SIGN IN / SIGN UP"
                    btnSyncAuth.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.zen_teal_primary))
                    btnSyncAuth.visibility = View.VISIBLE
                }
            }

            updateSyncCard()

            btnSyncAuth.setOnClickListener {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    FirebaseAuth.getInstance().signOut()
                    lifecycleScope.launch {
                        FirebaseSyncManager.clearAllLocalData(requireContext())
                        Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show()
                        updateSyncCard()
                        viewModel.loadProfile()
                    }
                } else {
                    showAuthDialog {
                        updateSyncCard()
                        viewModel.loadProfile()
                    }
                }
            }

            // Setup Badge RecyclerView
            badgeAdapter = BadgeAdapter(emptyList(), isGridView)
            rvBadges.layoutManager = if (isGridView) GridLayoutManager(requireContext(), 3) else LinearLayoutManager(requireContext())
            rvBadges.adapter = badgeAdapter

            viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }

            viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    viewModel.clearErrorMessage()
                }
            }

            viewModel.profile.observe(viewLifecycleOwner) { profile ->
                profile?.let {
                    val displayName = profile.userName.ifBlank { "ZenZone" }
                    view.findViewById<TextView>(R.id.tv_app_logo_name)?.text = "🧘 $displayName"

                    val displayUserName = if (it.userName.isNotEmpty()) it.userName else "Zen Practitioner"
                    tvUserName.text = displayUserName

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
                    
                    val totalHours = it.totalFocusedMinutes / 60.0
                    tvTotalHours.text = String.format("%.1f", totalHours)

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

                    // Render Profile Avatar (Base64 vs URI vs Initial)
                    if (!it.profileImageUri.isNullOrBlank()) {
                        if (ImageUtils.isBase64Image(it.profileImageUri)) {
                            val bitmap = ImageUtils.base64ToBitmap(it.profileImageUri)
                            if (bitmap != null) {
                                ivProfileImage.setImageBitmap(bitmap)
                                ivProfileImage.visibility = View.VISIBLE
                                tvAvatarInitial.visibility = View.GONE
                            } else {
                                ivProfileImage.visibility = View.GONE
                                tvAvatarInitial.visibility = View.VISIBLE
                                tvAvatarInitial.text = displayUserName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "Z"
                            }
                        } else {
                            try {
                                val uri = android.net.Uri.parse(it.profileImageUri)
                                ivProfileImage.setImageURI(uri)
                                ivProfileImage.visibility = View.VISIBLE
                                tvAvatarInitial.visibility = View.GONE
                            } catch (e: Exception) {
                                ivProfileImage.visibility = View.GONE
                                tvAvatarInitial.visibility = View.VISIBLE
                                tvAvatarInitial.text = displayUserName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "Z"
                            }
                        }
                    } else {
                        ivProfileImage.visibility = View.GONE
                        tvAvatarInitial.visibility = View.VISIBLE
                        tvAvatarInitial.text = displayUserName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "Z"
                    }

                    tvDailyStreak.text = it.currentChain.toString()
                    cvStreakCard.setOnClickListener { _ ->
                        com.zenzone.app.utils.ShareCardHelper.shareStreakCard(requireContext(), it)
                    }

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
                btnSwitchView.setImageResource(if (isGridView) R.drawable.ic_stats else R.drawable.ic_home)
            }

            // Click avatar container to select picture
            cvAvatar.setOnClickListener {
                if (FirebaseAuth.getInstance().currentUser != null) {
                    pickImageLauncher.launch("image/*")
                } else {
                    Toast.makeText(requireContext(), "Please sign in to upload a profile picture.", Toast.LENGTH_SHORT).show()
                }
            }

            view.findViewById<View>(R.id.iv_common_menu)?.setOnClickListener {
                (activity as? com.zenzone.app.ui.main.MainActivity)?.openDrawer()
            }
            view.findViewById<View>(R.id.iv_common_agent)?.setOnClickListener {
                com.zenzone.app.ui.social.ZenAgentDialog.show(requireContext(), parentFragmentManager, activity as? com.zenzone.app.ui.main.MainActivity)
            }

            btnStartFocusing.setOnClickListener {
                (activity as? com.zenzone.app.ui.main.MainActivity)?.navigateToMenuItem(R.id.nav_focus)
            }

            btnSettings.setOnClickListener {
                showSettingsDialog()
            }

            viewModel.loadProfile()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error loading profile: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showSettingsDialog() {
        val currentProfile = viewModel.profile.value ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        dialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.zen_slate_dark)
        )

        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_settings_name)
        val tilName = dialogView.findViewById<TextInputLayout>(R.id.til_settings_name)
        val btnSaveName = dialogView.findViewById<MaterialButton>(R.id.btn_settings_save_name)
        val btnChangePassword = dialogView.findViewById<MaterialButton>(R.id.btn_settings_change_password)
        val btnCustomBlocklist = dialogView.findViewById<MaterialButton>(R.id.btn_settings_custom_blocklist)
        val btnDeleteAccount = dialogView.findViewById<MaterialButton>(R.id.btn_settings_delete_account)
        val btnLogout = dialogView.findViewById<MaterialButton>(R.id.btn_settings_logout)

        btnCustomBlocklist.setOnClickListener {
            com.zenzone.app.ui.settings.BlockedAppsDialogFragment().show(
                parentFragmentManager, "BlockedAppsDialogFragment"
            )
            dialog.dismiss()
        }

        val ivPreview = dialogView.findViewById<ImageView>(R.id.iv_settings_profile_preview)
        val tvInitial = dialogView.findViewById<TextView>(R.id.tv_settings_avatar_initial)
        val btnSelectImage = dialogView.findViewById<MaterialButton>(R.id.btn_settings_select_image)
        val btnRemoveImage = dialogView.findViewById<MaterialButton>(R.id.btn_settings_remove_image)

        // Set current name
        etName.setText(currentProfile.userName)

        // Bind image preview
        val displayUserName = if (currentProfile.userName.isNotEmpty()) currentProfile.userName else "Zen Practitioner"
        tvInitial.text = displayUserName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "Z"
        if (!currentProfile.profileImageUri.isNullOrBlank()) {
            if (ImageUtils.isBase64Image(currentProfile.profileImageUri)) {
                val bitmap = ImageUtils.base64ToBitmap(currentProfile.profileImageUri)
                if (bitmap != null) {
                    ivPreview.setImageBitmap(bitmap)
                    ivPreview.visibility = View.VISIBLE
                    tvInitial.visibility = View.GONE
                } else {
                    ivPreview.visibility = View.GONE
                    tvInitial.visibility = View.VISIBLE
                }
            } else {
                try {
                    val uri = android.net.Uri.parse(currentProfile.profileImageUri)
                    ivPreview.setImageURI(uri)
                    ivPreview.visibility = View.VISIBLE
                    tvInitial.visibility = View.GONE
                } catch (e: Exception) {
                    ivPreview.visibility = View.GONE
                    tvInitial.visibility = View.VISIBLE
                }
            }
        } else {
            ivPreview.visibility = View.GONE
            tvInitial.visibility = View.VISIBLE
        }

        btnSelectImage.setOnClickListener {
            if (FirebaseAuth.getInstance().currentUser != null) {
                pickImageLauncher.launch("image/*")
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please sign in to upload a profile picture.", Toast.LENGTH_SHORT).show()
            }
        }

        btnRemoveImage.setOnClickListener {
            viewModel.updateProfile(currentProfile.userName, null)
            Toast.makeText(requireContext(), "Profile picture deleted!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        // Save Name
        btnSaveName.setOnClickListener {
            val newName = etName.text?.toString()?.trim() ?: ""
            if (newName.isBlank()) {
                tilName.error = "Name cannot be empty"
                return@setOnClickListener
            }
            if (newName.length > Constants.MAX_USER_NAME_LENGTH) {
                tilName.error = "Name cannot exceed ${Constants.MAX_USER_NAME_LENGTH} characters"
                return@setOnClickListener
            }
            tilName.error = null

            // Save to local shared preferences
            val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(Constants.PREF_USER_NAME, newName).apply()

            // Save to ViewModel & Firestore
            viewModel.updateProfile(newName, currentProfile.profileImageUri)
            com.zenzone.app.ui.widget.FocusChainWidgetProvider.triggerWidgetUpdate(requireContext())
            Toast.makeText(requireContext(), "Display name updated!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        // Change Password
        btnChangePassword.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(requireContext(), "Please sign in to change password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showChangePasswordDialog()
            dialog.dismiss()
        }

        // Logout
        btnLogout.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(requireContext(), "You are not signed in.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Your data will be synced to the cloud before logging out. Continue?")
                .setPositiveButton("LOGOUT") { _, _ ->
                    btnLogout.isEnabled = false
                    btnLogout.text = "Syncing & Logging out..."
                    
                    lifecycleScope.launch {
                        try {
                            // Sync all local data to Firestore before signing out (max 10s)
                            kotlinx.coroutines.withTimeoutOrNull(10_000L) {
                                FirebaseSyncManager.syncLocalToFirestore(requireContext())
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // Sign out from Firebase Auth
                        FirebaseAuth.getInstance().signOut()

                        // Clear onboarding status so the user sees registration screen
                        val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                        prefs.edit().putBoolean(Constants.PREF_ONBOARDING_COMPLETE, false).apply()

                        // Clear local data
                        try {
                            FirebaseSyncManager.clearAllLocalData(requireContext())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        Toast.makeText(requireContext(), "Logged out successfully!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()

                        // Restart MainActivity to show registration/onboarding screen
                        val intent = Intent(requireContext(), MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Delete Account
        btnDeleteAccount.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(requireContext(), "Please sign in to delete account", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your ZenZone account? This will wipe all your records and cannot be undone.")
                .setPositiveButton("DELETE") { _, _ ->
                    user.delete()
                        .addOnSuccessListener {
                            // Clear onboarding status
                            val prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                            prefs.edit().putBoolean(Constants.PREF_ONBOARDING_COMPLETE, false).apply()
                            
                            Toast.makeText(requireContext(), "Account deleted successfully.", Toast.LENGTH_LONG).show()
                            dialog.dismiss()
                            
                            // Restart MainActivity to launch onboarding slider
                            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to delete: ${e.message}. Please log in again to confirm credentials.", Toast.LENGTH_LONG).show()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.show()
    }

    private fun showChangePasswordDialog() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        dialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.zen_slate_dark)
        )

        val etPass = dialogView.findViewById<TextInputEditText>(R.id.et_settings_password)
        val tilPass = dialogView.findViewById<TextInputLayout>(R.id.til_settings_password)
        val etConfirm = dialogView.findViewById<TextInputEditText>(R.id.et_settings_confirm_password)
        val tilConfirm = dialogView.findViewById<TextInputLayout>(R.id.til_settings_confirm_password)
        val btnUpdate = dialogView.findViewById<MaterialButton>(R.id.btn_settings_update_password)

        btnUpdate.setOnClickListener {
            val newPass = etPass.text?.toString()?.trim() ?: ""
            val confirmPass = etConfirm.text?.toString()?.trim() ?: ""

            var isValid = true

            if (newPass.length < 6) {
                tilPass.error = "Password must be at least 6 characters"
                isValid = false
            } else {
                tilPass.error = null
            }

            if (confirmPass != newPass) {
                tilConfirm.error = "Passwords do not match"
                isValid = false
            } else {
                tilConfirm.error = null
            }

            if (!isValid) return@setOnClickListener

            btnUpdate.isEnabled = false
            btnUpdate.text = "Updating..."

            user.updatePassword(newPass)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    btnUpdate.isEnabled = true
                    btnUpdate.text = "UPDATE PASSWORD"
                    Toast.makeText(requireContext(), "Failed: ${e.message}. You may need to log in again.", Toast.LENGTH_LONG).show()
                }
        }

        dialog.show()
    }

    private fun showAuthDialog(onSuccess: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_auth, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)
        
        dialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.zen_slate_dark)
        )
        
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_auth_title)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tv_auth_subtitle)
        val tilEmail = dialogView.findViewById<TextInputLayout>(R.id.til_email)
        val etEmail = dialogView.findViewById<TextInputEditText>(R.id.et_email)
        val tilPassword = dialogView.findViewById<TextInputLayout>(R.id.til_password)
        val etPassword = dialogView.findViewById<TextInputEditText>(R.id.et_password)
        val btnAction = dialogView.findViewById<MaterialButton>(R.id.btn_action)
        val tvToggleMode = dialogView.findViewById<TextView>(R.id.tv_toggle_mode)
        
        var isLoginMode = true
        
        tvToggleMode.setOnClickListener {
            isLoginMode = !isLoginMode
            if (isLoginMode) {
                tvTitle.text = "Sign In to ZenZone"
                tvSubtitle.text = "Save your focus metrics, badges, and chains securely in the cloud."
                btnAction.text = "SIGN IN"
                tvToggleMode.text = "New to ZenZone? Create an account"
            } else {
                tvTitle.text = "Create ZenZone Account"
                tvSubtitle.text = "Start backing up your deep work progress in the cloud."
                btnAction.text = "CREATE ACCOUNT"
                tvToggleMode.text = "Already have an account? Sign In"
            }
        }
        
        btnAction.setOnClickListener {
            val email = etEmail.text?.toString()?.trim() ?: ""
            val password = etPassword.text?.toString()?.trim() ?: ""
            
            if (email.isBlank()) {
                tilEmail.error = "Please enter an email"
                return@setOnClickListener
            }
            if (password.length < 6) {
                tilPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            tilEmail.error = null
            tilPassword.error = null
            
            val auth = FirebaseAuth.getInstance()
            
            if (isLoginMode) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        lifecycleScope.launch {
                            FirebaseSyncManager.clearAllLocalData(requireContext())
                            val success = kotlinx.coroutines.withTimeoutOrNull(15_000L) {
                                try {
                                    FirebaseSyncManager.syncFirestoreToLocal(requireContext())
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    false
                                }
                            } ?: false

                            if (success) {
                                Toast.makeText(requireContext(), "Signed in successfully!", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                                onSuccess()
                                com.zenzone.app.utils.SyncWorker.enqueuePeriodicSync(requireContext())
                            } else {
                                try {
                                    auth.signOut()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                Toast.makeText(requireContext(), "Failed to sync profile from cloud. Please check connection and try again.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        lifecycleScope.launch {
                            FirebaseSyncManager.syncLocalToFirestore(requireContext())
                            Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            onSuccess()
                            com.zenzone.app.utils.SyncWorker.enqueuePeriodicSync(requireContext())
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }
        
        dialog.show()
    }

    private fun showAdjustImageDialog(uri: android.net.Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (originalBitmap == null) {
                Toast.makeText(requireContext(), "Failed to load image.", Toast.LENGTH_SHORT).show()
                return
            }

            val dialogView = layoutInflater.inflate(R.layout.dialog_adjust_image, null)
            val dialog = BottomSheetDialog(requireContext())
            dialog.setContentView(dialogView)

            dialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.zen_slate_dark)
            )

            val ivPreview = dialogView.findViewById<ImageView>(R.id.iv_adjust_preview)
            val btnRotate = dialogView.findViewById<View>(R.id.btn_adjust_rotate)
            val sbZoom = dialogView.findViewById<SeekBar>(R.id.sb_adjust_zoom)
            val sbPanX = dialogView.findViewById<SeekBar>(R.id.sb_adjust_pan_x)
            val sbPanY = dialogView.findViewById<SeekBar>(R.id.sb_adjust_pan_y)
            val btnCancel = dialogView.findViewById<View>(R.id.btn_adjust_cancel)
            val btnSave = dialogView.findViewById<View>(R.id.btn_adjust_save)

            ivPreview.setImageBitmap(originalBitmap)

            var rotationDegrees = 0
            var zoomPercent = 0
            var panXOffset = 200
            var panYOffset = 200

            fun updateMatrix() {
                val matrix = android.graphics.Matrix()
                val bmpW = originalBitmap.width
                val bmpH = originalBitmap.height
                val viewW = 200f * resources.displayMetrics.density
                val viewH = 200f * resources.displayMetrics.density

                matrix.postTranslate(-bmpW / 2f, -bmpH / 2f)
                matrix.postRotate(rotationDegrees.toFloat())
                matrix.postTranslate(viewW / 2f, viewH / 2f)

                val isSwapped = (rotationDegrees / 90) % 2 != 0
                val activeW = if (isSwapped) bmpH else bmpW
                val activeH = if (isSwapped) bmpW else bmpH

                val scaleX = viewW / activeW
                val scaleY = viewH / activeH
                val baseScale = Math.max(scaleX, scaleY)

                val zoomFactor = 1.0f + (zoomPercent / 100f)
                matrix.postScale(baseScale * zoomFactor, baseScale * zoomFactor, viewW / 2f, viewH / 2f)

                val translationX = (panXOffset - 200) * (viewW / 200f)
                val translationY = (panYOffset - 200) * (viewH / 200f)
                matrix.postTranslate(translationX, translationY)

                ivPreview.imageMatrix = matrix
            }

            btnRotate.setOnClickListener {
                rotationDegrees = (rotationDegrees + 90) % 360
                updateMatrix()
            }

            val seekListener = object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    when (seekBar?.id) {
                        R.id.sb_adjust_zoom -> zoomPercent = progress
                        R.id.sb_adjust_pan_x -> panXOffset = progress
                        R.id.sb_adjust_pan_y -> panYOffset = progress
                    }
                    updateMatrix()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }

            sbZoom.setOnSeekBarChangeListener(seekListener)
            sbPanX.setOnSeekBarChangeListener(seekListener)
            sbPanY.setOnSeekBarChangeListener(seekListener)

            ivPreview.post {
                updateMatrix()
            }

            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            btnSave.setOnClickListener {
                try {
                    val croppedBitmap = android.graphics.Bitmap.createBitmap(400, 400, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(croppedBitmap)
                    val saveMatrix = android.graphics.Matrix()

                    val bmpW = originalBitmap.width
                    val bmpH = originalBitmap.height
                    val viewW = 200f * resources.displayMetrics.density
                    val viewH = 200f * resources.displayMetrics.density

                    saveMatrix.postTranslate(-bmpW / 2f, -bmpH / 2f)
                    saveMatrix.postRotate(rotationDegrees.toFloat())
                    saveMatrix.postTranslate(viewW / 2f, viewH / 2f)

                    val isSwapped = (rotationDegrees / 90) % 2 != 0
                    val activeW = if (isSwapped) bmpH else bmpW
                    val activeH = if (isSwapped) bmpW else bmpH

                    val scaleX = viewW / activeW
                    val scaleY = viewH / activeH
                    val baseScale = Math.max(scaleX, scaleY)

                    val zoomFactor = 1.0f + (zoomPercent / 100f)
                    saveMatrix.postScale(baseScale * zoomFactor, baseScale * zoomFactor, viewW / 2f, viewH / 2f)

                    val translationX = (panXOffset - 200) * (viewW / 200f)
                    val translationY = (panYOffset - 200) * (viewH / 200f)
                    saveMatrix.postTranslate(translationX, translationY)

                    val finalScale = 400f / viewW
                    saveMatrix.postScale(finalScale, finalScale)

                    canvas.drawBitmap(originalBitmap, saveMatrix, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG))

                    val base64 = ImageUtils.bitmapToBase64(croppedBitmap)

                    val currentProfile = viewModel.profile.value
                    if (currentProfile != null && base64 != null) {
                        viewModel.updateProfile(currentProfile.userName, base64)
                        Toast.makeText(requireContext(), "Profile picture updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Failed to save profile picture: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error adjusting image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
