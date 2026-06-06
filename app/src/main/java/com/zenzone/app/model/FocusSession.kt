package com.zenzone.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey
    val id: String = "",
    val goalId: String = "",
    val goalName: String = "",
    val durationMinutes: Int = 0,
    val completedAt: String = "",
    val wasChainSaved: Boolean = false,
    val isSynced: Boolean = false,
    val sessionNotes: String? = null
) : Serializable
