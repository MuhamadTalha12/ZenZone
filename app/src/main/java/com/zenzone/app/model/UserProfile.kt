package com.zenzone.app.model

data class UserProfile(
    val userName: String = "",
    val totalFocusedMinutes: Long,
    val zenLevel: Int,
    val zenXP: Int,
    val badges: List<String>,
    val totalSessions: Int,
    val longestEverChain: Int
)
