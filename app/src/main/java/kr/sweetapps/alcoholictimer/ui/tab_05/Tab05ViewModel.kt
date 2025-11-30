package kr.sweetapps.alcoholictimer.ui.tab_05

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true
)

class Tab05ViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleDarkMode(isDark: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isDarkMode = isDark)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(notificationsEnabled = enabled)
        }
    }
}

