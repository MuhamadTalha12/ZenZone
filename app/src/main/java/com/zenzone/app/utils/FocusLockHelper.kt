package com.zenzone.app.utils

import android.app.Activity
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object FocusLockHelper {
    
    /**
     * Enables focus lock mode:
     * - Hides system UI (status bar, navigation bar)
     * - Prevents screen from turning off
     * - Makes the activity immersive
     */
    fun enableFocusLock(activity: Activity) {
        // Keep screen on during focus session
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Hide system bars for immersive experience
        val windowInsetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        windowInsetsController.apply {
            // Hide both status bar and navigation bar
            hide(WindowInsetsCompat.Type.systemBars())
            // Make it sticky so bars don't reappear on swipe
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        // Prevent user from leaving the app (requires user permission on Android 5.0+)
        // This makes back button disabled during focus session
        activity.window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }
    
    /**
     * Disables focus lock mode and restores normal UI
     */
    fun disableFocusLock(activity: Activity) {
        // Allow screen to turn off normally
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Show system bars again
        val windowInsetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        
        // Restore normal system UI
        activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }
    
    /**
     * Check if focus lock is currently active
     */
    fun isFocusLockActive(activity: Activity): Boolean {
        return (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
    }
}
