package dev.paradox.trace.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.paradox.trace.data.accessibility.AccessibilityServiceChecker
import dev.paradox.trace.data.export.ExportFormats
import dev.paradox.trace.data.usage.UsageAccessChecker
import dev.paradox.trace.domain.collection.UsageSessionSyncer
import dev.paradox.trace.domain.repository.SessionRepository
import dev.paradox.trace.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferences: UserPreferencesRepository,
    private val sessionRepository: SessionRepository,
    private val usageAccessChecker: UsageAccessChecker,
    private val usageSessionSyncer: UsageSessionSyncer,
    private val accessibilityServiceChecker: AccessibilityServiceChecker,
) : ViewModel() {
    val dailyGoalMinutes: StateFlow<Int> = userPreferences.dailyGoalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** null = not yet checked; the grant can change outside the app at any time. */
    private val _usageAccessGranted = MutableStateFlow<Boolean?>(null)
    val usageAccessGranted: StateFlow<Boolean?> = _usageAccessGranted.asStateFlow()

    /** null = not yet checked; accessibility can be revoked at any moment. */
    private val _accessibilityEnabled = MutableStateFlow<Boolean?>(null)
    val accessibilityEnabled: StateFlow<Boolean?> = _accessibilityEnabled.asStateFlow()

    /** Re-reads the special grant; call when the screen becomes visible again. */
    fun refreshUsageAccess() {
        _usageAccessGranted.value = usageAccessChecker.isGranted()
    }

    /** Re-reads the accessibility service state; call when the screen becomes visible. */
    fun refreshAccessibilityState() {
        _accessibilityEnabled.value = accessibilityServiceChecker.isEnabled()
    }

    /** One explicit import pass, then re-reads the grant. */
    fun syncUsageNow(onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            refreshUsageAccess()
            onResult(usageSessionSyncer.sync())
        }
    }

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
            usageAccessChecker: UsageAccessChecker,
            usageSessionSyncer: UsageSessionSyncer,
            accessibilityServiceChecker: AccessibilityServiceChecker,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    userPreferences,
                    sessionRepository,
                    usageAccessChecker,
                    usageSessionSyncer,
                    accessibilityServiceChecker,
                )
            }
        }
    }
}
