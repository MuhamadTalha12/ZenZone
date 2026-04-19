package com.zenzone.app.utils

object Constants {
    const val PREFS_NAME = "prefs_zenzone"
    const val PREF_LAST_OPENED_DATE = "last_opened_date"
    const val PREF_ONBOARDING_COMPLETE = "onboarding_complete"
    const val PREF_USER_NAME = "user_name"
    
    // Validation constants
    const val MAX_GOAL_NAME_LENGTH = 50
    const val MIN_TARGET_MINUTES = 1
    const val MAX_TARGET_MINUTES = 480 // 8 hours
    const val MAX_USER_NAME_LENGTH = 30
    
    // XP and Level constants
    const val XP_PER_MINUTE = 2
    const val MAX_CHAIN_BONUS_XP = 100
    const val CHAIN_BONUS_XP_PER_DAY = 5
    val ZEN_LEVEL_THRESHOLDS = listOf(100, 250, 500, 1000, 2000, 3500, 5500, 8000, 11000, 15000)
    const val MAX_ZEN_LEVEL = 10
    
    // Timer constants
    const val TIMER_TICK_INTERVAL_MS = 1000L
    
    // Badge thresholds
    const val BADGE_CHAIN_3 = 3
    const val BADGE_CHAIN_7 = 7
    const val BADGE_CHAIN_14 = 14
    const val BADGE_CHAIN_30 = 30
    const val BADGE_CHAIN_60 = 60
    const val BADGE_CHAIN_100 = 100
    const val BADGE_HOURS_10_MINUTES = 600 // 10 hours in minutes
    
    // Splash screen delay
    const val SPLASH_DELAY_MS = 2000L
}
