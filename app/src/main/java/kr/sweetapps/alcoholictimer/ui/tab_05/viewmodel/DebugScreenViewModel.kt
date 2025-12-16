package kr.sweetapps.alcoholictimer.ui.tab_05.viewmodel

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.ktx.performance
import com.google.firebase.perf.metrics.Trace
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kr.sweetapps.alcoholictimer.util.debug.DebugSettings
import kr.sweetapps.alcoholictimer.BuildConfig
import kr.sweetapps.alcoholictimer.MainApplication
import kr.sweetapps.alcoholictimer.data.repository.AdPolicyManager

data class DebugScreenUiState(
    val switch1: Boolean = false,
    val demoMode: Boolean = false,
    val umpForceEea: Boolean = false,
    val switch3: Boolean = false,
    val switch4: Boolean = false,
    val switch5: Boolean = false,
)

class DebugScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DebugScreenUiState())
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                demoMode = DebugSettings.isDemoModeEnabled(application),
                umpForceEea = DebugSettings.isUmpForceEeaEnabled(application)
            )
        }
    }

    fun setSwitch(switchIndex: Int, value: Boolean) {
        _uiState.update { currentState ->
            when (switchIndex) {
                1 -> currentState.copy(switch1 = value)
                2 -> {
                    DebugSettings.setDemoModeEnabled(getApplication(), value)
                    currentState.copy(demoMode = value)
                }
                3 -> currentState.copy(switch3 = value)
                4 -> currentState.copy(switch4 = value)
                5 -> currentState.copy(switch5 = value)
                6 -> {
                    // [REMOVED] UMP EEA 강제 설정 - 상용 배포에 불필요
                    currentState
                }
                else -> currentState
            }
        }
    }

    // [NEW] 디버그 모드 광고 쿨타임 설정
    fun setDebugAdCoolDown(context: Context, seconds: Long) {
        if (!BuildConfig.DEBUG) return
        try {
            AdPolicyManager.setDebugCoolDownSeconds(context, seconds)
            Log.d("DebugScreenVM", "광고 쿨타임 설정: $seconds 초")
        } catch (t: Throwable) {
            Log.e("DebugScreenVM", "광고 쿨타임 설정 실패", t)
        }
    }

    // [NEW] 디버그 모드 광고 쿨타임 가져오기
    fun getDebugAdCoolDown(context: Context): Long {
        return try {
            AdPolicyManager.getDebugCoolDownSeconds(context)
        } catch (t: Throwable) {
            Log.e("DebugScreenVM", "광고 쿨타임 로드 실패", t)
            -1L
        }
    }

    // [REMOVED] resetConsent - 상용 배포에 불필요 (유럽 지역 배포 제외)

    /**
     * Perform debug action for switches 3..5.
     * Actions are guarded to run only in debug builds.
     * - 3: Analytics test event
     * - 4: non-fatal Crashlytics report
     * - 5: Performance trace (start -> wait -> stop)
     */
    fun performAction(actionIndex: Int) {
        if (!BuildConfig.DEBUG) return

        when (actionIndex) {
            3 -> {
                // [수정] 로그를 명확하게 찍도록 변경
                Log.d("MY_TEST", "Analytics 이벤트 전송 시도 중...") // 시도 로그
                try {
                    val b = Bundle().apply { putString("source", "DebugScreen") }
                    Firebase.analytics.logEvent("debug_test_event", b)
                    Log.d("MY_TEST", "Analytics 이벤트 전송 명령 성공!") // 성공 로그
                } catch (e: Exception) {
                    Log.e("MY_TEST", "Analytics 전송 실패: ${e.message}") // 실패 로그 (에러 내용 확인)
                }
            }

            4 -> {
                // Crashlytics 테스트 (여기가 수정됨!)
                viewModelScope.launch {
                    Log.d("MY_TEST", "Crashlytics 비치명 보고 시도 중...")
                    try {
                        // 타임스탬프 생성
                        val timeStamp = System.currentTimeMillis()

                        // 1. 강제 활성화
                        Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)

                        // 2. 세션 로그 남기기
                        Firebase.crashlytics.log("Debug Session Start")

                        // 3. 에러 전송
                        Firebase.crashlytics.recordException(RuntimeException("테스트용 비치명 에러입니다! (시간: $timeStamp)"))

                        Log.d("MY_TEST", "Crashlytics 서버로 예외 전송 명령 성공! (시간: $timeStamp)")
                    } catch (e: Exception) {
                        Log.e("MY_TEST", "Crashlytics 전송 실패: ${e.message}")
                    }
                }
            }

            5 -> {
                // Performance 테스트
                viewModelScope.launch {
                    Log.d("MY_TEST", "Performance Trace 시작")
                    try {
                        val perf = Firebase.performance
                        val trace: Trace = perf.newTrace("debug_trace")
                        trace.start()
                        delay(1500)
                        trace.stop()
                        Log.d("MY_TEST", "Performance Trace 종료 및 전송")
                    } catch (e: Exception) {
                        Log.e("MY_TEST", "Performance 실패: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Phase 2: 커뮤니티 테스트 게시글 10개 생성
     */
    fun generateDummyCommunityPosts(context: Context) {
        viewModelScope.launch {
            try {
                val repository = kr.sweetapps.alcoholictimer.data.repository.CommunityRepository()
                val result = repository.generateDummyPosts()

                val message = if (result.isSuccess) {
                    "✅ 테스트 게시글 10개 생성 완료!\nTab 4에서 확인하세요."
                } else {
                    "❌ 게시글 생성 실패: ${result.exceptionOrNull()?.message}"
                }

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                }

                Log.d("DebugScreen", message)
            } catch (e: Exception) {
                Log.e("DebugScreen", "게시글 생성 중 에러", e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "에러: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Phase 2: 모든 커뮤니티 게시글 삭제
     */
    fun deleteAllCommunityPosts(context: Context) {
        viewModelScope.launch {
            try {
                val repository = kr.sweetapps.alcoholictimer.data.repository.CommunityRepository()
                val result = repository.deleteAllPosts()

                val message = if (result.isSuccess) {
                    "✅ 모든 게시글 삭제 완료!"
                } else {
                    "❌ 게시글 삭제 실패: ${result.exceptionOrNull()?.message}"
                }

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                }

                Log.d("DebugScreen", message)
            } catch (e: Exception) {
                Log.e("DebugScreen", "게시글 삭제 중 에러", e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "에러: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
