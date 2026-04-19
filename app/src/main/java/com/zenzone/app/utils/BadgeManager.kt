package com.zenzone.app.utils

import com.zenzone.app.model.UserProfile
import com.zenzone.app.model.ZenBadge

object BadgeManager {
    
    /**
     * Returns all available badges with their earned status based on the user profile
     */
    fun getAllBadges(profile: UserProfile): List<ZenBadge> {
        return listOf(
            ZenBadge(
                "first_session",
                "First Breath",
                "Complete your very first focus session.",
                "ic_badge_first_breath",
                0,
                profile.badges.contains("first_session")
            ),
            ZenBadge(
                "chain_3",
                "Novice Monk",
                "Reach a 3-day chain.",
                "ic_badge_chain",
                Constants.BADGE_CHAIN_3,
                profile.badges.contains("chain_3")
            ),
            ZenBadge(
                "chain_7",
                "Week Warrior",
                "Reach a 7-day chain.",
                "ic_badge_chain",
                Constants.BADGE_CHAIN_7,
                profile.badges.contains("chain_7")
            ),
            ZenBadge(
                "chain_14",
                "Zen Apprentice",
                "Reach a 14-day chain.",
                "ic_badge_chain",
                Constants.BADGE_CHAIN_14,
                profile.badges.contains("chain_14")
            ),
            ZenBadge(
                "chain_30",
                "Digital Monk",
                "Reach a 30-day chain.",
                "ic_badge_master",
                Constants.BADGE_CHAIN_30,
                profile.badges.contains("chain_30")
            ),
            ZenBadge(
                "chain_60",
                "Focus Master",
                "Reach a 60-day chain.",
                "ic_badge_master",
                Constants.BADGE_CHAIN_60,
                profile.badges.contains("chain_60")
            ),
            ZenBadge(
                "chain_100",
                "Digital Ninja",
                "Reach a 100-day chain.",
                "ic_badge_master",
                Constants.BADGE_CHAIN_100,
                profile.badges.contains("chain_100")
            ),
            ZenBadge(
                "hours_10",
                "Time Bender",
                "Accumulate 10 hours of total focus time.",
                "ic_badge_time",
                0,
                profile.badges.contains("hours_10")
            )
        )
    }
    
    /**
     * Checks which new badges should be unlocked based on current session data
     * Returns a pair of: (updated badge list, newly unlocked badges)
     */
    fun checkAndUnlockBadges(
        currentBadges: List<String>,
        currentChain: Int,
        totalMinutes: Long,
        totalSessions: Int
    ): Pair<List<String>, List<ZenBadge>> {
        val earnedBadges = currentBadges.toMutableList()
        val newlyUnlockedBadges = mutableListOf<ZenBadge>()
        
        val checkBadge = { id: String, name: String, chainReq: Int, condition: Boolean ->
            if (condition && !earnedBadges.contains(id)) {
                earnedBadges.add(id)
                newlyUnlockedBadges.add(ZenBadge(id, name, "", "ic_lotus_logo", chainReq, true))
            }
        }
        
        checkBadge("first_session", "First Breath", 0, totalSessions == 1)
        checkBadge("chain_3", "Novice Monk", Constants.BADGE_CHAIN_3, currentChain >= Constants.BADGE_CHAIN_3)
        checkBadge("chain_7", "Week Warrior", Constants.BADGE_CHAIN_7, currentChain >= Constants.BADGE_CHAIN_7)
        checkBadge("chain_14", "Zen Apprentice", Constants.BADGE_CHAIN_14, currentChain >= Constants.BADGE_CHAIN_14)
        checkBadge("chain_30", "Digital Monk", Constants.BADGE_CHAIN_30, currentChain >= Constants.BADGE_CHAIN_30)
        checkBadge("chain_60", "Focus Master", Constants.BADGE_CHAIN_60, currentChain >= Constants.BADGE_CHAIN_60)
        checkBadge("chain_100", "Digital Ninja", Constants.BADGE_CHAIN_100, currentChain >= Constants.BADGE_CHAIN_100)
        checkBadge("hours_10", "Time Bender", 0, totalMinutes >= Constants.BADGE_HOURS_10_MINUTES)
        
        return Pair(earnedBadges, newlyUnlockedBadges)
    }
}
