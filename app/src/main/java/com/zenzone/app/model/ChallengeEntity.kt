package com.zenzone.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "daily_challenges")
data class ChallengeEntity(
    @PrimaryKey
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "", // FOCUS_DURATION, SESSION_COUNT, STREAK_MAINTAIN, GOAL_COMPLETE
    val targetValue: Int = 0,
    val currentProgress: Int = 0,
    val xpReward: Int = 0,
    val seedReward: Int = 0,
    val isCompleted: Boolean = false,
    val date: String = "" // yyyy-MM-dd
) : Serializable
