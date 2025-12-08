package kr.sweetapps.alcoholictimer.ui.tab_05.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Tab05 설정 화면 UI 상태
 * @param isDarkMode 다크모드 활성화 여부
 * @param notificationsEnabled 알림 활성화 여부
 */
data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true
)

/**
 * Tab05(더보기/설정) 화면용 ViewModel
 * - 앱 설정 상태 관리
 * - 다크모드, 알림 등의 설정 토글
 */
class Tab05ViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * 다크모드 설정 변경
     * @param isDark 다크모드 활성화 여부
     */
    fun toggleDarkMode(isDark: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isDarkMode = isDark)
        }
    }

    /**
     * 알림 설정 변경
     * @param enabled 알림 활성화 여부
     */
    fun toggleNotifications(enabled: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(notificationsEnabled = enabled)
        }
    }
}

