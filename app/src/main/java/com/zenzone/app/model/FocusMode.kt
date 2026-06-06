package com.zenzone.app.model

import java.io.Serializable

enum class FocusMode(
    val displayName: String,
    val durationMinutes: Int,
    val defaultSoundscape: String,
    val distractionSensitivity: String // "STRICT" (1.5s check), "GENTLE" (5s check), "OFF" (no checks)
) : Serializable {
    DEEP_WORK("Deep Work", 45, "Lo-fi Beats", "STRICT"),
    POMODORO("Pomodoro", 25, "Rain", "GENTLE"),
    STUDY_SPRINT("Study Sprint", 50, "Forest Wind", "GENTLE"),
    CUSTOM("Custom", 25, "None", "OFF")
}
