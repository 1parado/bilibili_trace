package dev.paradox.trace.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.paradox.trace.data.export.ExportFormats
import dev.paradox.trace.domain.repository.SessionRepository
import dev.paradox.trace.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferences: UserPreferencesRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    val dailyGoalMinutes: StateFlow<Int> = userPreferences.dailyGoalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setDailyGoalMinutes(minutes: Int) {
        viewModelScope.launch { userPreferences.setDailyGoalMinutes(minutes) }
    }

    /** Deletes every local record; irreversible by design and always user-initiated. */
    fun deleteAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            sessionRepository.deleteAllData()
            onDone()
        }
    }

    suspend fun buildCsv(): String =
        ExportFormats.csv(sessionRepository.observeAllSessions().first())

    suspend fun buildJson(): String =
        ExportFormats.json(sessionRepository.observeAllSessions().first(), System.currentTimeMillis())

    companion object {
        fun factory(
            userPreferences: UserPreferencesRepository,
            sessionRepository: SessionRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(userPreferences, sessionRepository)
            }
        }
    }
}
