package dev.paradox.trace.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.paradox.trace.domain.model.ContentSession
import dev.paradox.trace.domain.repository.ManualSessionCommand
import dev.paradox.trace.domain.repository.SessionRepository
import dev.paradox.trace.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodayViewModel(
    sessionRepository: SessionRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val sessions: StateFlow<List<ContentSession>> = sessionRepository.observeAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val dailyGoalMinutes: StateFlow<Int> = userPreferencesRepository.dailyGoalMinutes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    fun addSession(command: ManualSessionCommand, onResult: (Result<ContentSession>) -> Unit) {
        viewModelScope.launch {
            onResult(sessionRepository.addManualSession(command))
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch {
            sessionRepository.deleteSession(id)
        }
    }

    companion object {
        fun factory(
            sessionRepository: SessionRepository,
            userPreferencesRepository: UserPreferencesRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { TodayViewModel(sessionRepository, userPreferencesRepository) }
        }
    }
}
