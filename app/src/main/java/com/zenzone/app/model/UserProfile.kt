package com.zenzone.app.model

import java.io.Serializable

data class UserProfile(
    val userName: String = "",
    val profileImageUri: String? = null,
    val totalFocusedMinutes: Long = 0L,
    val zenLevel: Int = 1,
    val zenXP: Int = 0,
    val badges: List<String> = emptyList(),
    val totalSessions: Int = 0,
    val currentChain: Int = 0,
    val longestEverChain: Int = 0,
    val rareSeeds: Int = 0,
    val zenPetType: String = "None",
    val zenPetLevel: Int = 1,
    val zenPetXp: Int = 0
) : Serializable
