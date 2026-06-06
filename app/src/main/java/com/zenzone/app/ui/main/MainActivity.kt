package com.zenzone.app.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.zenzone.app.R
import com.zenzone.app.model.UserProfile
import com.zenzone.app.repository.UserRepository
import com.zenzone.app.ui.focus.FocusFragment
import com.zenzone.app.ui.home.HomeFragment
import com.zenzone.app.ui.profile.ProfileFragment
import com.zenzone.app.ui.stats.StatsFragment
import com.zenzone.app.utils.Constants
import com.zenzone.app.utils.FirebaseSyncManager
import kotlinx.coroutines.launch
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.zenzone.app.ui.challenge.DailyChallengeFragment
import com.zenzone.app.utils.ReminderWorker

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: androidx.drawerlayout.widget.DrawerLayout
    private lateinit var navView: com.google.android.material.navigation.NavigationView
    private lateinit var layoutOnboarding: View
    private lateinit var vpOnboarding: ViewPager2
    private lateinit var btnGetStarted: MaterialButton
    private lateinit var tilNameInput: TextInputLayout
    private lateinit var etUserName: TextInputEditText
    private lateinit var llDots: LinearLayout
    
    var isFocusLockActive = false
        private set

    private val slides = listOf(
        OnboardingSlide(R.drawable.ic_lotus_logo, "Welcome to ZenZone", "Build focus chains and find your flow."),
        OnboardingSlide(R.drawable.ic_focus, "Deep Work Goals", "Set sessions, stay distraction-free, and own your time."),
        OnboardingSlide(R.drawable.ic_stats, "Track Your Progress", "Watch your chain grow. Level up your Zen."),
        OnboardingSlide(R.drawable.ic_lotus_logo, "What's your name?", "We'll personalise your ZenZone experience.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            window.statusBarColor = ContextCompat.getColor(this, R.color.zen_teal_dark)
            setContentView(R.layout.activity_main)

            drawerLayout = findViewById(R.id.drawer_layout)
            navView = findViewById(R.id.nav_view)
            layoutOnboarding = findViewById(R.id.layout_onboarding)
            vpOnboarding = layoutOnboarding.findViewById(R.id.vp_onboarding)
            btnGetStarted = layoutOnboarding.findViewById(R.id.btn_get_started)
            tilNameInput = layoutOnboarding.findViewById(R.id.til_name_input)
            etUserName = layoutOnboarding.findViewById(R.id.et_user_name)
            llDots = layoutOnboarding.findViewById(R.id.ll_dots)

            val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val isOnboardingComplete = prefs.getBoolean(Constants.PREF_ONBOARDING_COMPLETE, false)

            if (!isOnboardingComplete) {
                showOnboarding()
                findViewById<View>(R.id.fab_zen_agent)?.visibility = View.GONE
            } else {
                if (savedInstanceState == null) {
                    setupNavigation()
                } else {
                    setupNavigationListeners()
                }
            }
            findViewById<View>(R.id.fab_zen_agent)?.setOnClickListener {
                com.zenzone.app.ui.social.ZenAgentDialog.show(this, supportFragmentManager, this)
            }
            scheduleDailyReminder()
        } catch (e: Exception) {
            e.printStackTrace()
            if (savedInstanceState == null) {
                setupNavigation()
            }
        }
    }

    private fun showOnboarding() {
        try {
            layoutOnboarding.visibility = View.VISIBLE
            setupDots(0)

            val regContainer = layoutOnboarding.findViewById<View>(R.id.sv_registration_container)
            val tilEmail = layoutOnboarding.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_email_input)
            val etEmail = layoutOnboarding.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_email)
            val tilPassword = layoutOnboarding.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_password_input)
            val etPassword = layoutOnboarding.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_password)
            val tilConfirmPassword = layoutOnboarding.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_confirm_password_input)
            val etConfirmPassword = layoutOnboarding.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_confirm_password)
            val tvToggleAuthMode = layoutOnboarding.findViewById<TextView>(R.id.tv_toggle_auth_mode)

            var isSignInMode = false

            tvToggleAuthMode.setOnClickListener {
                isSignInMode = !isSignInMode
                if (isSignInMode) {
                    tilNameInput.visibility = View.GONE
                    tilConfirmPassword.visibility = View.GONE
                    btnGetStarted.text = "Sign In"
                    tvToggleAuthMode.text = "New here? Register instead"
                } else {
                    tilNameInput.visibility = View.VISIBLE
                    tilConfirmPassword.visibility = View.VISIBLE
                    btnGetStarted.text = "Get Started & Register"
                    tvToggleAuthMode.text = "Already have an account? Sign In"
                }
            }

            val adapter = OnboardingAdapter(slides)
            vpOnboarding.adapter = adapter

            vpOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    try {
                        updateDots(position)
                        val isLastSlide = position == slides.size - 1
                        regContainer.isVisible = isLastSlide
                        tvToggleAuthMode.isVisible = isLastSlide
                        btnGetStarted.visibility = View.VISIBLE
                        if (isLastSlide) {
                            btnGetStarted.text = if (isSignInMode) "Sign In" else "Get Started & Register"
                        } else {
                            btnGetStarted.text = "Next"
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })

            btnGetStarted.setOnClickListener {
                try {
                    val currentPos = vpOnboarding.currentItem
                    if (currentPos < slides.size - 1) {
                        vpOnboarding.currentItem = currentPos + 1
                        return@setOnClickListener
                    }

                    val name = etUserName.text?.toString()?.trim() ?: ""
                    val email = etEmail.text?.toString()?.trim() ?: ""
                    val password = etPassword.text?.toString()?.trim() ?: ""
                    val confirmPassword = etConfirmPassword.text?.toString()?.trim() ?: ""

                    var isValid = true

                    if (!isSignInMode) {
                        if (name.isBlank()) {
                            tilNameInput.error = "Please enter your name"
                            isValid = false
                        } else if (name.length > Constants.MAX_USER_NAME_LENGTH) {
                            tilNameInput.error = "Name cannot exceed ${Constants.MAX_USER_NAME_LENGTH} characters"
                            isValid = false
                        } else {
                            tilNameInput.error = null
                        }
                    }

                    if (email.isBlank()) {
                        tilEmail.error = "Please enter your email"
                        isValid = false
                    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        tilEmail.error = "Please enter a valid email address"
                        isValid = false
                    } else {
                        tilEmail.error = null
                    }

                    if (password.length < 6) {
                        tilPassword.error = "Password must be at least 6 characters"
                        isValid = false
                    } else {
                        tilPassword.error = null
                    }

                    if (!isSignInMode) {
                        if (confirmPassword != password) {
                            tilConfirmPassword.error = "Passwords do not match"
                            isValid = false
                        } else {
                            tilConfirmPassword.error = null
                        }
                    }

                    if (!isValid) return@setOnClickListener

                    btnGetStarted.isEnabled = false

                    if (isSignInMode) {
                        btnGetStarted.text = "Signing In..."
                        com.google.firebase.auth.FirebaseAuth.getInstance()
                            .signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener {
                                val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                                lifecycleScope.launch {
                                    // 1. Clear old local data to prepare for the new account
                                    try {
                                        FirebaseSyncManager.clearAllLocalData(this@MainActivity)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                    // 2. Try to sync from Firestore with a 15-second timeout
                                    btnGetStarted.text = "Syncing data..."
                                    val syncSuccess = kotlinx.coroutines.withTimeoutOrNull(15_000L) {
                                        try {
                                            FirebaseSyncManager.syncFirestoreToLocal(this@MainActivity)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            false
                                        }
                                    } ?: false

                                    // 3. If sync failed, create a default profile so the app works
                                    if (!syncSuccess) {
                                        try {
                                            val repo = UserRepository(this@MainActivity)
                                            val displayName = if (name.isNotBlank()) name else email.substringBefore("@")
                                            val defaultProfile = UserProfile(
                                                userName = displayName,
                                                totalFocusedMinutes = 0L,
                                                zenLevel = 1,
                                                zenXP = 0,
                                                badges = emptyList(),
                                                totalSessions = 0,
                                                currentChain = 0,
                                                longestEverChain = 0
                                            )
                                            repo.saveProfile(defaultProfile)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }

                                    // 4. Navigate into the app
                                    prefs.edit().putBoolean(Constants.PREF_ONBOARDING_COMPLETE, true).apply()
                                    layoutOnboarding.visibility = View.GONE
                                    setupNavigation()
                                }
                            }
                            .addOnFailureListener { e ->
                                btnGetStarted.isEnabled = true
                                btnGetStarted.text = "Sign In"
                                android.widget.Toast.makeText(this@MainActivity, "Sign In failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                    } else {
                        btnGetStarted.text = "Registering..."
                        com.google.firebase.auth.FirebaseAuth.getInstance()
                            .createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener {
                                val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                                prefs.edit()
                                    .putBoolean(Constants.PREF_ONBOARDING_COMPLETE, true)
                                    .putString(Constants.PREF_USER_NAME, name)
                                    .apply()

                                lifecycleScope.launch {
                                    try {
                                        val repo = UserRepository(this@MainActivity)
                                        val defaultProfile = UserProfile(
                                            userName = name,
                                            totalFocusedMinutes = 0L,
                                            zenLevel = 1,
                                            zenXP = 0,
                                            badges = emptyList(),
                                            totalSessions = 0,
                                            currentChain = 0,
                                            longestEverChain = 0
                                        )
                                        repo.saveProfile(defaultProfile)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                    layoutOnboarding.visibility = View.GONE
                                    setupNavigation()
                                }
                            }
                            .addOnFailureListener { e ->
                                btnGetStarted.isEnabled = true
                                btnGetStarted.text = "Get Started & Register"
                                android.widget.Toast.makeText(this@MainActivity, "Registration failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            setupNavigation()
        }
    }

    private fun setupDots(selected: Int) {
        llDots.removeAllViews()
        for (i in slides.indices) {
            val dot = TextView(this).apply {
                text = if (i == selected) "●" else "○"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.zen_slate_surface))
                setPadding(6, 0, 6, 0)
            }
            llDots.addView(dot)
        }
    }

    private fun updateDots(selected: Int) {
        for (i in 0 until llDots.childCount) {
            val dot = llDots.getChildAt(i) as? TextView ?: continue
            dot.text = if (i == selected) "●" else "○"
            dot.alpha = if (i == selected) 1f else 0.5f
        }
    }

    private fun setupNavigationListeners() {
        navView.setNavigationItemSelectedListener { item ->
            try {
                val fragment: Fragment = when (item.itemId) {
                    R.id.nav_home -> HomeFragment()
                    R.id.nav_focus -> FocusFragment()
                    R.id.nav_challenges -> DailyChallengeFragment()
                    R.id.nav_stats -> StatsFragment()
                    R.id.nav_profile -> ProfileFragment()
                    else -> HomeFragment()
                }
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .commit()
                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun setupNavigation() {
        try {
            updateDrawerHeader()
            setupNavigationListeners()
            
            val navigateTo = intent?.getStringExtra("NAVIGATE_TO")
            val selectedId = if (navigateTo == "focus") R.id.nav_focus else R.id.nav_home
            navView.setCheckedItem(selectedId)
            
            val initialFragment: Fragment = when (selectedId) {
                R.id.nav_focus -> FocusFragment()
                else -> HomeFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, initialFragment)
                .commit()
            findViewById<View>(R.id.fab_zen_agent)?.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateDrawerHeader() {
        lifecycleScope.launch {
            try {
                val repo = UserRepository(this@MainActivity)
                val profile = repo.loadProfile()
                if (navView.headerCount > 0) {
                    val headerView = navView.getHeaderView(0)
                    val tvUsername = headerView.findViewById<TextView>(R.id.tv_drawer_username)
                    val tvSubtitle = headerView.findViewById<TextView>(R.id.tv_drawer_subtitle)
                    tvUsername.text = profile.userName.ifBlank { "Zen Mind" }
                    tvSubtitle.text = "Level ${profile.zenLevel} Practitioner"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun navigateToMenuItem(menuItemId: Int) {
        try {
            navView.setCheckedItem(menuItemId)
            val fragment: Fragment = when (menuItemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_focus -> FocusFragment()
                R.id.nav_challenges -> DailyChallengeFragment()
                R.id.nav_stats -> StatsFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> HomeFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val navigateTo = intent?.getStringExtra("NAVIGATE_TO")
        if (navigateTo == "focus") {
            navView.setCheckedItem(R.id.nav_focus)
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, FocusFragment())
                .commit()
        }
    }
    
    fun openDrawer() {
        if (!isFocusLockActive) {
            drawerLayout.openDrawer(androidx.core.view.GravityCompat.START)
        }
    }
    
    fun setDrawerLocked(locked: Boolean) {
        val mode = if (locked) {
            androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        } else {
            androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED
        }
        drawerLayout.setDrawerLockMode(mode)
    }
    
    fun enableFocusLock() {
        isFocusLockActive = true
        drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        com.zenzone.app.utils.FocusLockHelper.enableFocusLock(this)
        findViewById<View>(R.id.fab_zen_agent)?.visibility = View.GONE
    }
    
    fun disableFocusLock() {
        isFocusLockActive = false
        drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)
        com.zenzone.app.utils.FocusLockHelper.disableFocusLock(this)
        findViewById<View>(R.id.fab_zen_agent)?.visibility = View.VISIBLE
    }
    
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (isFocusLockActive) {
            android.widget.Toast.makeText(this, "Focus session in progress. Stop the timer to exit.", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            if (drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
            } else {
                super.onBackPressed()
            }
        }
    }

    private fun scheduleDailyReminder() {
        try {
            val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
                24, TimeUnit.HOURS
            )
                .setInitialDelay(calculateInitialDelayForEvening(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_focus_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateInitialDelayForEvening(): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 20) // 8:00 PM
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }
        val currentTime = System.currentTimeMillis()
        if (calendar.timeInMillis <= currentTime) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis - currentTime
    }
}
