package kr.sweetapps.alcoholictimer.ui.common

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import kr.sweetapps.alcoholictimer.util.constants.Constants
import kr.sweetapps.alcoholictimer.util.manager.CurrencyManager

/**
 * 사용자 설정 데이터 클래스
 * SharedPreferences의 값들을 하나로 묶어서 관리
 */
data class UserSettings(
    val cost: String,
    val frequency: String,
    val duration: String,
    val currencyCode: String,
    val currencySymbol: String
)

/**
 * SharedPreferences의 변경사항을 실시간으로 감지하는 Composable
 *
 * 탭4에서 설정을 변경하면, 이 Hook을 사용하는 모든 화면이 자동으로 갱신됨
 *
 * @param context Android Context
 * @return State<UserSettings> 설정 변경 시 자동으로 리컴포지션 트리거
 */
@Composable
fun rememberUserSettingsState(context: Context): State<UserSettings> {
    val userPrefs = remember {
        context.getSharedPreferences(Constants.USER_SETTINGS_PREFS, Context.MODE_PRIVATE)
    }
    val settingsPrefs = remember {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    // [NEW] 현재 설정값을 State로 관리
    val currentSettings = remember {
        mutableStateOf(readCurrentSettings(context, userPrefs, settingsPrefs))
    }

    // [CRITICAL] SharedPreferences 변경 감지 리스너 등록
    DisposableEffect(context) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            // 설정이 변경되면 즉시 최신 값을 읽어서 State 업데이트
            currentSettings.value = readCurrentSettings(context, userPrefs, settingsPrefs)
        }

        // 두 개의 SharedPreferences 모두 감시
        userPrefs.registerOnSharedPreferenceChangeListener(listener)
        settingsPrefs.registerOnSharedPreferenceChangeListener(listener)

        // Composable이 화면에서 사라질 때 리스너 해제 (메모리 누수 방지)
        onDispose {
            userPrefs.unregisterOnSharedPreferenceChangeListener(listener)
            settingsPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    return currentSettings
}

/**
 * 현재 저장된 설정값을 읽어오는 헬퍼 함수
 *
 * @return UserSettings 최신 설정 데이터
 */
private fun readCurrentSettings(
    context: Context,
    userPrefs: SharedPreferences,
    settingsPrefs: SharedPreferences
): UserSettings {
    // [1] 음주 비용/빈도/시간 설정 읽기
    val cost = userPrefs.getString(Constants.PREF_SELECTED_COST, Constants.DEFAULT_COST)
        ?: Constants.DEFAULT_COST
    val frequency = userPrefs.getString(Constants.PREF_SELECTED_FREQUENCY, Constants.DEFAULT_FREQUENCY)
        ?: Constants.DEFAULT_FREQUENCY
    val duration = userPrefs.getString(Constants.PREF_SELECTED_DURATION, Constants.DEFAULT_DURATION)
        ?: Constants.DEFAULT_DURATION

    // [2] 통화 설정 읽기 (CurrencyManager 로직과 동일)
    val isExplicit = settingsPrefs.getBoolean("currency_explicit", false)
    val currencyCode = if (isExplicit) {
        settingsPrefs.getString("selected_currency", "AUTO") ?: "AUTO"
    } else {
        "AUTO"
    }

    // [3] 통화 기호 계산 (CurrencyManager.getSelectedCurrency 사용)
    val selectedCurrency = CurrencyManager.getSelectedCurrency(context)
    val currencySymbol = selectedCurrency.symbol

    return UserSettings(
        cost = cost,
        frequency = frequency,
        duration = duration,
        currencyCode = currencyCode,
        currencySymbol = currencySymbol
    )
}

