package com.zenzone.app.utils

import com.zenzone.app.model.FocusGoal

object ChainCalculator {

    fun shouldIncrementChain(goal: FocusGoal, todayDate: String): Boolean {
        if (goal.lastCompletedDate == null) return true
        val days = DateUtils.daysBetween(goal.lastCompletedDate, todayDate)
        return days == 1
    }

    fun isChainBroken(goal: FocusGoal, todayDate: String): Boolean {
        if (goal.lastCompletedDate == null) return false
        val days = DateUtils.daysBetween(goal.lastCompletedDate, todayDate)
        return days > 1
    }

    fun applyChainResult(goal: FocusGoal, sessionMinutes: Int, today: String): FocusGoal {
        var newChain = goal.currentChain
        var newLongest = goal.longestChain

        if (goal.lastCompletedDate == null) {
            newChain = 1
            newLongest = maxOf(newChain, newLongest)
        } else if (isChainBroken(goal, today)) {
            newChain = 1
        } else if (shouldIncrementChain(goal, today)) {
            newChain += 1
            newLongest = maxOf(newChain, newLongest)
        }

        return goal.copy(
            currentChain = newChain,
            longestChain = newLongest,
            lastCompletedDate = today,
            totalMinutesFocused = goal.totalMinutesFocused + sessionMinutes.toLong()
        )
    }

    fun calculateXPGain(sessionMinutes: Int, chainLength: Int): Int {
        val baseXP = sessionMinutes * Constants.XP_PER_MINUTE
        val chainBonus = minOf(chainLength * Constants.CHAIN_BONUS_XP_PER_DAY, Constants.MAX_CHAIN_BONUS_XP)
        return baseXP + chainBonus
    }

    fun calculateZenLevel(totalXP: Int): Pair<Int, Int> {
        val thresholds = Constants.ZEN_LEVEL_THRESHOLDS
        var level = 1
        var currentLevelStartXP = 0

        for (i in thresholds.indices) {
            if (totalXP >= thresholds[i]) {
                level = i + 2
                currentLevelStartXP = thresholds[i]
            } else {
                break
            }
        }
        val xpIntoLevel = totalXP - currentLevelStartXP
        return Pair(minOf(level, Constants.MAX_ZEN_LEVEL), xpIntoLevel)
    }
}
