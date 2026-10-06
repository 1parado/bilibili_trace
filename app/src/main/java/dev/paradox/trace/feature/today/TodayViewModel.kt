package dev.paradox.trace.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.paradox.trace.domain.model.ContentSession
import dev.paradox.trace.domain.repository.ManualSessionCommand
import dev.paradox.trace.domain.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodayViewModel(private val repository: SessionRepository) : ViewModel() {

    val sessions: StateFlow<List<ContentSession>> = repository.observeAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun addSession(command: ManualSessionCommand, onResult: (Result<ContentSession>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.addManualSession(command))
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch {
            repository.deleteSession(id)
        }
    }

    companion object {
        fun factory(repository: SessionRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { TodayViewModel(repository) }
        }
    }
}
