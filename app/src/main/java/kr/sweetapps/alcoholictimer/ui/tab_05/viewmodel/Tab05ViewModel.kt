package kr.sweetapps.alcoholictimer.ui.tab_05.viewmodel

import android.content.Context
import android.content.SharedPreferences
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

    private var sharedPreferences: SharedPreferences? = null
    private var userRepository: UserRepository? = null

    /**
     * ViewModel 초기화 - SharedPreferences 설정 및 닉네임 로드
     * @param context Android Context
     * @param defaultNickname 기본 닉네임 (리소스에서 가져온 값)
     */
    fun initialize(context: Context, defaultNickname: String) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
            userRepository = UserRepository(context)
            loadNickname(defaultNickname)
            loadAvatarIndex()
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
                android.util.Log.d("Tab05ViewModel", "아바타 업데이트 성공: $index")
            } else {
                android.util.Log.e("Tab05ViewModel", "아바타 업데이트 실패")
            }
        }
    }

    /**
     * [DEBUG] 4년치 랜덤 과거 데이터 생성 (테스트용)
     * - 4년 전 ~ 1년 전까지의 무작위 금주 기록 생성
     * - 각 연도당 2~3개의 기록 생성
     * - 지속 기간: 3~50일 랜덤
     * - 성공/실패 여부: 랜덤
     */
    fun generateRandomMockData(context: Context) {
        try {
            val prefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
            val records = org.json.JSONArray()
            val calendar = java.util.Calendar.getInstance()
            val currentYear = calendar.get(java.util.Calendar.YEAR)
            val random = java.util.Random()

            android.util.Log.d("Tab05ViewModel", "랜덤 데이터 생성 시작: ${currentYear - 4} ~ ${currentYear - 1}")

            // 1. 4년 전 ~ 1년 전까지 루프
            for (yearOffset in 4 downTo 1) {
                val targetYear = currentYear - yearOffset

                // 2. 연간 2~3개 기록 생성
                val recordCount = 2 + random.nextInt(2) // 2 or 3

                for (i in 0 until recordCount) {
                    // 날짜 랜덤 설정
                    calendar.set(java.util.Calendar.YEAR, targetYear)
                    calendar.set(java.util.Calendar.MONTH, random.nextInt(12)) // 0~11월
                    calendar.set(java.util.Calendar.DAY_OF_MONTH, 1 + random.nextInt(25)) // 1~25일

                    // 시간 랜덤 (오전/오후)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, random.nextInt(24))
                    calendar.set(java.util.Calendar.MINUTE, random.nextInt(60))
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)

                    val startTime = calendar.timeInMillis

                    // 지속 기간 (3일 ~ 50일 랜덤)
                    val durationDays = 3 + random.nextInt(48)
                    val durationMillis = durationDays * 24L * 60 * 60 * 1000
                    val endTime = startTime + durationMillis

                    // 목표 일수 (실제 기간과 비슷하거나 조금 더 길게)
                    val targetDays = (durationDays + random.nextInt(5)).toFloat()
                    val isCompleted = random.nextFloat() > 0.3f // 70% 성공률

                    // JSON 생성
                    val obj = org.json.JSONObject().apply {
                        put("id", "${startTime}_mock")
                        put("startTime", startTime)
                        put("endTime", endTime)
                        put("targetDays", targetDays)
                        put("actualDays", if (isCompleted) durationDays else durationDays / 2) // 실패 시 절반만
                        put("isCompleted", isCompleted)
                        put("status", if (isCompleted) "completed" else "failed")
                    }
                    records.put(obj)

                    android.util.Log.d("Tab05ViewModel",
                        "생성: ${targetYear}년 ${calendar.get(java.util.Calendar.MONTH) + 1}월 - ${durationDays}일 (${if (isCompleted) "성공" else "실패"})")
                }
            }

            // 3. 저장
            prefs.edit().apply {
                putString("sobriety_records", records.toString())
                apply()
            }

            android.util.Log.d("Tab05ViewModel", "랜덤 데이터 생성 완료: 총 ${records.length()}개 기록")
        } catch (e: Exception) {
            android.util.Log.e("Tab05ViewModel", "랜덤 데이터 생성 실패", e)
        }
    }

    /**
     * [DEBUG] 모든 기록 삭제 (테스트용)
     */
    fun clearAllRecords(context: Context) {
        try {
            val prefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
            prefs.edit().apply {
                remove("sobriety_records")
                remove("start_time")
                putBoolean("timer_completed", false)
                apply()
            }
            android.util.Log.d("Tab05ViewModel", "모든 기록 삭제 완료")
        } catch (e: Exception) {
            android.util.Log.e("Tab05ViewModel", "기록 삭제 실패", e)
        }
    }
}

