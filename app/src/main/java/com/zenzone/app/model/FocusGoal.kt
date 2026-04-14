package com.zenzone.app.model

data class FocusGoal(
    val id: String,
    val name: String,
    val targetMinutes: Int,
    val frequency: String,
    val currentChain: Int,
    val longestChain: Int,
    val lastCompletedDate: String?,
    val totalMinutesFocused: Long,
    val createdAt: String,
    val colorTag: String
)
