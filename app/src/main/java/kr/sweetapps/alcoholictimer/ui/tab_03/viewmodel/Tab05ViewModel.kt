package kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.data.repository.UserRepository

/**
 * Tab05 설정 화면 UI 상태
 * @param isDarkMode 다크모드 활성화 여부
 * @param notificationsEnabled 알림 활성화 여부
 * @param nickname 사용자 닉네임
 * @param showCustomerFeedbackSheet 고객 문의 바텀시트 표시 여부
 * @param avatarIndex 현재 아바타 인덱스 (0~19)
 * @param showAvatarDialog 아바타 선택 다이얼로그 표시 여부
 */
data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val nickname: String = "",
    val showCustomerFeedbackSheet: Boolean = false,
    val avatarIndex: Int = 0,
    val showAvatarDialog: Boolean = false
)

/**
 * Tab05(더보기/설정) 화면용 ViewModel
 * - 앱 설정 상태 관리
 * - 다크모드, 알림 등의 설정 토글
 * - 닉네임 관리
 * - 고객 문의 시트 제어
 * - 아바타 관리
 */
class Tab05ViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var userRepository: UserRepository? = null

    fun initialize(context: Context, defaultNickname: String) {
        if (userRepository == null) {
            userRepository = UserRepository(context)
            loadNickname(defaultNickname)
            loadAvatarIndex()
        }
    }

    private fun loadNickname(defaultNickname: String) {
        val savedNickname = userRepository?.getNickname() ?: defaultNickname
        _uiState.update { it.copy(nickname = savedNickname) }
    }

    /**
     * 아바타 인덱스 로드
     */
    private fun loadAvatarIndex() {
        viewModelScope.launch {
            val avatarIndex = userRepository?.getAvatarIndex() ?: 0
            _uiState.update { it.copy(avatarIndex = avatarIndex) }
        }
    }

    /**
     * 닉네임 새로고침 (화면이 다시 보일 때 호출)
     * @param nickname 새로운 닉네임 또는 기본 닉네임
     */
    fun refreshNickname(nickname: String) {
        // 인자가 전달되면 직접 업데이트, 아니면 Repository에서 로드
        if (nickname.isNotBlank()) {
            _uiState.update { it.copy(nickname = nickname) }
            Log.d("Tab05ViewModel", "닉네임 갱신: $nickname")
        } else {
            loadNickname(nickname)
        }
    }

    // [NEW] 닉네임과 아바타를 강제로 새로고침 (ON_RESUME 시 사용) (2025-12-24)
    fun reloadUserData(defaultNickname: String) {
        loadNickname(defaultNickname)
        loadAvatarIndex()
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

    /**
     * 아바타 선택 다이얼로그 표시/숨김
     * @param show 표시 여부
     */
    fun setShowAvatarDialog(show: Boolean) {
        _uiState.update { it.copy(showAvatarDialog = show) }
    }

    /**
     * 아바타 업데이트
     * @param index 선택한 아바타 인덱스 (0~19)
     */
    fun updateAvatar(index: Int) {
        viewModelScope.launch {
            val success = userRepository?.updateAvatar(index) ?: false
            if (success) {
                _uiState.update { it.copy(avatarIndex = index) }
                Log.d("Tab05ViewModel", "아바타 업데이트 성공: $index")
            } else {
                Log.e("Tab05ViewModel", "아바타 업데이트 실패")
            }
        }
    }

    // [REMOVED] generateRandomMockData() - 디버그 메뉴에서 제거됨 (2025-12-26)
    // [REMOVED] clearAllRecords() - 앱 설정에 이미 존재하는 초기화 기능과 중복 (2025-12-26)
}

