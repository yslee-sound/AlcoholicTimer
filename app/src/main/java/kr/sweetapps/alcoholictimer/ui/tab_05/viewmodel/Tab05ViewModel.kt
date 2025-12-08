package kr.sweetapps.alcoholictimer.ui.tab_05.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Tab05 설정 화면 UI 상태
 * @param isDarkMode 다크모드 활성화 여부
 * @param notificationsEnabled 알림 활성화 여부
 * @param nickname 사용자 닉네임
 * @param showCustomerFeedbackSheet 고객 문의 바텀시트 표시 여부
 */
data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val nickname: String = "",
    val showCustomerFeedbackSheet: Boolean = false
)

/**
 * Tab05(더보기/설정) 화면용 ViewModel
 * - 앱 설정 상태 관리
 * - 다크모드, 알림 등의 설정 토글
 * - 닉네임 관리
 * - 고객 문의 시트 제어
 */
class Tab05ViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var sharedPreferences: SharedPreferences? = null

    /**
     * ViewModel 초기화 - SharedPreferences 설정 및 닉네임 로드
     * @param context Android Context
     * @param defaultNickname 기본 닉네임 (리소스에서 가져온 값)
     */
    fun initialize(context: Context, defaultNickname: String) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
            loadNickname(defaultNickname)
        }
    }

    /**
     * SharedPreferences에서 닉네임 로드
     */
    private fun loadNickname(defaultNickname: String) {
        val savedNickname = sharedPreferences?.getString("nickname", defaultNickname) ?: defaultNickname
        _uiState.update { it.copy(nickname = savedNickname) }
    }

    /**
     * 닉네임 새로고침 (화면이 다시 보일 때 호출)
     */
    fun refreshNickname(defaultNickname: String) {
        loadNickname(defaultNickname)
    }

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

    /**
     * 고객 문의 바텀시트 표시/숨김
     * @param show 표시 여부
     */
    fun setShowCustomerFeedbackSheet(show: Boolean) {
        _uiState.update { it.copy(showCustomerFeedbackSheet = show) }
    }
}

