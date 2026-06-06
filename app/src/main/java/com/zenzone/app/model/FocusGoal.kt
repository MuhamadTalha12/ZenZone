package com.zenzone.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "focus_goals")
data class FocusGoal(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val targetMinutes: Int = 0,
    val frequency: String = "",
    val currentChain: Int = 0,
    val longestChain: Int = 0,
    val lastCompletedDate: String? = null,
    val totalMinutesFocused: Long = 0L,
    val createdAt: String = "",
    val colorTag: String = "",
    val isSynced: Boolean = false
) : Serializable
