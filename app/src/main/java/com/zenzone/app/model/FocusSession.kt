package com.zenzone.app.model

data class FocusSession(
    val id: String,
    val goalId: String,
    val goalName: String,
    val durationMinutes: Int,
    val completedAt: String,
    val wasChainSaved: Boolean
)
