package kr.sweetapps.alcoholictimer.core.ui

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.core.content.edit
import kr.sweetapps.alcoholictimer.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object DebugAdHelper {
    private const val TAG = "DebugAdHelper"
    private const val PREF_NAME = "debug_settings"
    private const val KEY_HIDE_BANNER = "hide_banner_ad"

    // 전역 상태 Flow - 모든 화면이 이를 구독
    private val _bannerHiddenFlow = MutableStateFlow(false)
    val bannerHiddenFlow: StateFlow<Boolean> = _bannerHiddenFlow

    fun initialize(context: Context) {
        Log.e(TAG, "initialize called, BuildConfig.DEBUG=${BuildConfig.DEBUG}")
        if (!BuildConfig.DEBUG) {
            _bannerHiddenFlow.value = false
            Log.e(TAG, "initialize: DEBUG is false, setting flow to false")
            return
        }
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val hidden = sharedPref.getBoolean(KEY_HIDE_BANNER, false)
        _bannerHiddenFlow.value = hidden
        Log.e(TAG, "initialize: hidden=$hidden, flow updated")
    }

    fun isBannerHidden(context: Context): Boolean {
        if (!BuildConfig.DEBUG) return false
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val hidden = sharedPref.getBoolean(KEY_HIDE_BANNER, false)
        Log.e(TAG, "isBannerHidden: $hidden")
        return hidden
    }

    fun setBannerHidden(context: Context, hidden: Boolean) {
        if (!BuildConfig.DEBUG) return
        Log.e(TAG, "setBannerHidden: $hidden")
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPref.edit {
            putBoolean(KEY_HIDE_BANNER, hidden)
        }
        // 전역 상태 즉시 업데이트 - 이것이 핵심!
        _bannerHiddenFlow.value = hidden
        Log.e(TAG, "setBannerHidden: flow updated to $hidden")
    }

    fun toggleBannerHidden(context: Context): Boolean {
        if (!BuildConfig.DEBUG) return false
        val currentState = _bannerHiddenFlow.value
        val newState = !currentState
        Log.e(TAG, "toggleBannerHidden: currentState=$currentState, newState=$newState")
        setBannerHidden(context, newState)
        return newState
    }

    @Composable
    fun rememberBannerHiddenState(context: Context): State<Boolean> {
        if (!BuildConfig.DEBUG) {
            Log.e(TAG, "rememberBannerHiddenState: DEBUG is false, returning false")
            return androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        }

        // 전역 Flow를 구독하여 모든 화면에서 동시에 업데이트
        val state = bannerHiddenFlow.collectAsState()
        Log.e(TAG, "rememberBannerHiddenState: returning state with value=${state.value}")
        return state
    }
}

