package kr.sweetapps.alcoholictimer.ui.tab_04

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
}
