package kr.sweetapps.alcoholictimer.ui.tab_04.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.util.constants.Constants

/**
 * [NEW] Tab04(설정 화면) 상태 관리 ViewModel
 * - 음주 비용, 빈도, 기간 설정값 관리
 * - SharedPreferences 읽기/쓰기 처리
 */
class Tab04ViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPref = application.getSharedPreferences(
        Constants.USER_SETTINGS_PREFS,
        android.content.Context.MODE_PRIVATE
    )

    // [NEW] 음주 비용 설정 상태
    private val _selectedCost = MutableStateFlow(Constants.KEY_COST_MEDIUM)
    val selectedCost: StateFlow<String> = _selectedCost.asStateFlow()

    // [NEW] 음주 빈도 설정 상태
    private val _selectedFrequency = MutableStateFlow(Constants.KEY_FREQUENCY_MEDIUM)
    val selectedFrequency: StateFlow<String> = _selectedFrequency.asStateFlow()

    // [NEW] 음주 기간 설정 상태
    private val _selectedDuration = MutableStateFlow(Constants.KEY_DURATION_MEDIUM)
    val selectedDuration: StateFlow<String> = _selectedDuration.asStateFlow()

    init {
        // [NEW] 초기 설정값 로드
        loadUserSettings()
    }

    /**
     * [NEW] SharedPreferences에서 사용자 설정 불러오기
     */
    private fun loadUserSettings() {
        val (cost, frequency, duration) = Constants.getUserSettings(getApplication())
        _selectedCost.value = cost
        _selectedFrequency.value = frequency
        _selectedDuration.value = duration
    }

    /**
     * [NEW] 음주 비용 설정 변경
     */
    fun updateCost(newCost: String) {
        viewModelScope.launch {
            _selectedCost.value = newCost
            sharedPref.edit {
                putString(Constants.PREF_SELECTED_COST, newCost)
            }
        }
    }

    /**
     * [NEW] 음주 빈도 설정 변경
     */
    fun updateFrequency(newFrequency: String) {
        viewModelScope.launch {
            _selectedFrequency.value = newFrequency
            sharedPref.edit {
                putString(Constants.PREF_SELECTED_FREQUENCY, newFrequency)
            }
        }
    }

    /**
     * [NEW] 음주 기간 설정 변경
     */
    fun updateDuration(newDuration: String) {
        viewModelScope.launch {
            _selectedDuration.value = newDuration
            sharedPref.edit {
                putString(Constants.PREF_SELECTED_DURATION, newDuration)
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
    fun generateRandomMockData() {
        viewModelScope.launch {
            try {
                val records = org.json.JSONArray()
                val calendar = java.util.Calendar.getInstance()
                val currentYear = calendar.get(java.util.Calendar.YEAR)
                val random = java.util.Random()

                android.util.Log.d("Tab04ViewModel", "랜덤 데이터 생성 시작: ${currentYear - 4} ~ ${currentYear - 1}")

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

                        android.util.Log.d("Tab04ViewModel",
                            "생성: ${targetYear}년 ${calendar.get(java.util.Calendar.MONTH) + 1}월 - ${durationDays}일 (${if (isCompleted) "성공" else "실패"})")
                    }
                }

                // 3. 저장
                sharedPref.edit {
                    putString(Constants.PREF_SOBRIETY_RECORDS, records.toString())
                }

                android.util.Log.d("Tab04ViewModel", "랜덤 데이터 생성 완료: 총 ${records.length()}개 기록")
            } catch (e: Exception) {
                android.util.Log.e("Tab04ViewModel", "랜덤 데이터 생성 실패", e)
            }
        }
    }

    /**
     * [DEBUG] 모든 기록 삭제 (테스트용)
     */
    fun clearAllRecords() {
        viewModelScope.launch {
            try {
                sharedPref.edit {
                    remove(Constants.PREF_SOBRIETY_RECORDS)
                    remove(Constants.PREF_START_TIME)
                    putBoolean(Constants.PREF_TIMER_COMPLETED, false)
                }
                android.util.Log.d("Tab04ViewModel", "모든 기록 삭제 완료")
            } catch (e: Exception) {
                android.util.Log.e("Tab04ViewModel", "기록 삭제 실패", e)
            }
        }
    }
}

