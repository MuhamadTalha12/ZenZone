package com.zenzone.app.ui.main

import android.content.Context
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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
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
        window.statusBarColor = ContextCompat.getColor(this, R.color.zen_teal_dark)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)
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
        } else {
            setupNavigation()
        }
    }

    private fun showOnboarding() {
        layoutOnboarding.visibility = View.VISIBLE
        setupDots(0)

        val adapter = OnboardingAdapter(slides)
        vpOnboarding.adapter = adapter

        vpOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                val isLastSlide = position == slides.size - 1
                tilNameInput.isVisible = isLastSlide
                btnGetStarted.isVisible = isLastSlide
            }
        })

        btnGetStarted.setOnClickListener {
            val name = etUserName.text?.toString()?.trim() ?: ""
            if (name.isEmpty()) {
                tilNameInput.error = "Please enter your name"
                return@setOnClickListener
            }
            tilNameInput.error = null

            val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(Constants.PREF_ONBOARDING_COMPLETE, true)
                .putString(Constants.PREF_USER_NAME, name)
                .apply()

            // Save name into profile
            lifecycleScope.launch {
                val repo = UserRepository(this@MainActivity)
                val existing = repo.loadProfile()
                repo.saveProfile(existing.copy(userName = name))
            }

            layoutOnboarding.visibility = View.GONE
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

    private fun setupNavigation() {
        bottomNav.visibility = View.VISIBLE
        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_focus -> FocusFragment()
                R.id.nav_stats -> StatsFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> HomeFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit()
            true
        }
        bottomNav.selectedItemId = R.id.nav_home
    }
    
    fun enableFocusLock() {
        isFocusLockActive = true
        bottomNav.visibility = View.GONE
        com.zenzone.app.utils.FocusLockHelper.enableFocusLock(this)
    }
    
    fun disableFocusLock() {
        isFocusLockActive = false
        bottomNav.visibility = View.VISIBLE
        com.zenzone.app.utils.FocusLockHelper.disableFocusLock(this)
    }
    
    override fun onBackPressed() {
        if (isFocusLockActive) {
            // Prevent back button during focus session
            android.widget.Toast.makeText(this, "Focus session in progress. Stop the timer to exit.", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            super.onBackPressed()
        }
    }
}
