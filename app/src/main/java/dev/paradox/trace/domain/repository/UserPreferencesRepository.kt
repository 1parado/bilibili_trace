package dev.paradox.trace.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * User preferences: local-only, replaceable via settings UI.
 * Daily goal is a target, never a moral judgment (AGENTS.md §5).
 */
interface UserPreferencesRepository {

    /** Daily viewing goal in minutes; 0 means "no goal set". */
    val dailyGoalMinutes: Flow<Int>

    suspend fun setDailyGoalMinutes(minutes: Int)
}

/** Domain helper for interpreting the goal against observed time. */
object DailyGoal {

    /** Returns remaining minutes toward the goal; negative means exceeded. */
    fun remainingMinutes(goalMinutes: Int, observedMs: Long): Int? {
        if (goalMinutes <= 0) return null
        return goalMinutes - (observedMs / 60_000L).toInt()
    }
}
