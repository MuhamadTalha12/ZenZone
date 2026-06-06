package com.zenzone.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeManagerTest {

    @Test
    fun `checkAndUnlockBadges unlocks first_session on first session`() {
        val currentBadges = emptyList<String>()
        val (updatedBadges, newBadges) = BadgeManager.checkAndUnlockBadges(
            currentBadges = currentBadges,
            currentChain = 0,
            totalMinutes = 10,
            totalSessions = 1
        )
        assertTrue(updatedBadges.contains("first_session"))
        assertEquals(1, newBadges.size)
        assertEquals("first_session", newBadges[0].id)
    }

    @Test
    fun `checkAndUnlockBadges unlocks chain_7 Week Warrior badge`() {
        val currentBadges = emptyList<String>()
        val (updatedBadges, newBadges) = BadgeManager.checkAndUnlockBadges(
            currentBadges = currentBadges,
            currentChain = 7,
            totalMinutes = 100,
            totalSessions = 5
        )
        assertTrue(updatedBadges.contains("chain_7"))
        assertTrue(newBadges.any { it.id == "chain_7" })
    }

    @Test
    fun `checkAndUnlockBadges unlocks night_owl after 5 late sessions`() {
        val currentBadges = emptyList<String>()
        val (updatedBadges, newBadges) = BadgeManager.checkAndUnlockBadges(
            currentBadges = currentBadges,
            currentChain = 1,
            totalMinutes = 100,
            totalSessions = 5,
            lateSessions = 5
        )
        assertTrue(updatedBadges.contains("night_owl"))
        assertTrue(newBadges.any { it.id == "night_owl" })
    }

    @Test
    fun `checkAndUnlockBadges does not unlock night_owl if late sessions count is less than 5`() {
        val currentBadges = emptyList<String>()
        val (updatedBadges, newBadges) = BadgeManager.checkAndUnlockBadges(
            currentBadges = currentBadges,
            currentChain = 1,
            totalMinutes = 100,
            totalSessions = 4,
            lateSessions = 4
        )
        assertTrue(!updatedBadges.contains("night_owl"))
        assertTrue(newBadges.none { it.id == "night_owl" })
    }
}
