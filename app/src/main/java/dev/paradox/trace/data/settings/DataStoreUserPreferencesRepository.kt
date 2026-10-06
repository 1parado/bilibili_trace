package dev.paradox.trace.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.paradox.trace.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "trace_prefs")

/**
 * DataStore-backed preferences. Goal is capped defensively: the settings UI
 * validates, this layer clamps.
 */
class DataStoreUserPreferencesRepository(
    private val context: Context,
) : UserPreferencesRepository {

    override val dailyGoalMinutes: Flow<Int> =
        context.dataStore.data.map { prefs ->
            (prefs[KEY_GOAL_MINUTES] ?: DEFAULT_GOAL_MINUTES).coerceIn(0, MAX_GOAL_MINUTES)
        }

    override suspend fun setDailyGoalMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(0, MAX_GOAL_MINUTES)
        context.dataStore.edit { prefs ->
            prefs[KEY_GOAL_MINUTES] = clamped
        }
    }

    companion object {
        private val KEY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        const val DEFAULT_GOAL_MINUTES: Int = 0
        const val MAX_GOAL_MINUTES: Int = 24 * 60
    }
}
