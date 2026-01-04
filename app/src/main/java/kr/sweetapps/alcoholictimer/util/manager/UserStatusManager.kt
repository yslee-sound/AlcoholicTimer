package kr.sweetapps.alcoholictimer.util.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions
import kr.sweetapps.alcoholictimer.util.constants.Constants
import kr.sweetapps.alcoholictimer.analytics.AnalyticsManager // [NEW] Analytics 이벤트 전송용 (2025-12-31)

/**
 * [NEW] 사용자 상태 중앙 관리자 (2025-12-25)
 *
 * **목적:**
 * - 앱 전체에서 사용자의 레벨/일수를 일관되게 제공
 * - TimerTimeManager의 경과 시간 + 과거 기록(DB)을 합산하여 Total Days 계산
 * - 모든 화면(Run, Diary, Community 등)에서 동일한 데이터 사용 보장
 *
 * **[UPDATED] 과거 기록 통합 (2025-12-25):**
 * - 과거 기록(DB) + 현재 타이머를 합산하여 정확한 누적 일수 제공
 * - Tab02ViewModel에서 DB 기록을 로드하여 updateHistoryDays()로 주입
 *
 * **사용 예시:**
 * ```kotlin
 * val userStatus by UserStatusManager.userStatus.collectAsState()
 * Text("Lv.${userStatus.level} · Day ${userStatus.days}")
 * ```
 */
object UserStatusManager {

    /**
     * 사용자 상태 데이터 클래스
     *
     * @param level 레벨 번호 (1부터 시작, 1-indexed)
     * @param days 경과 일수 (누적, 정수, 과거 기록 + 현재 타이머)
     * @param totalDaysPrecise 정밀한 경과 일수 (누적, 소수점 포함, Float)
     */
    data class UserStatus(
        val level: Int,
        val days: Int,
        val totalDaysPrecise: Float
    ) {
        companion object {
            val DEFAULT = UserStatus(level = 1, days = 0, totalDaysPrecise = 0f)
        }
    }

    /**
     * 전역 코루틴 스코프 (앱 생명주기와 독립적)
     * SupervisorJob 사용으로 자식 코루틴 실패 시에도 스코프 유지
     */
    private val managerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * [NEW] 과거 기록 일수 저장용 (Float 정밀도) (2025-12-26)
     * Tab02ViewModel에서 DB 로드 후 updateHistoryDays()로 업데이트
     */
    private val _historyDays = MutableStateFlow(0f)

    /**
     * [NEW] 이전 레벨 추적 (레벨업 감지용) (2025-12-31)
     */
    private var previousLevel: Int = 1

    /**
     * [UPDATED] 외부에서 과거 기록 업데이트 (Float 지원) (2025-12-26)
     * [FIX] 타이머 상태 보호 강화 (2026-01-04)
     * @param days 과거 기록의 총 금주 일수 (Float, 소수점 포함)
     *
     * **중요**: 이 메서드는 과거 기록만 업데이트하며, 현재 타이머 상태에는 영향을 주지 않습니다.
     * 현재 타이머의 경과 시간은 TimerTimeManager.elapsedMillis를 통해 독립적으로 관리됩니다.
     */
    fun updateHistoryDays(days: Float) {
        _historyDays.value = days
        android.util.Log.d("UserStatusManager", "📚 History updated: $days days (precise) - Timer state NOT affected")
    }

    /**
     * 사용자 상태 StateFlow (읽기 전용)
     *
     * **[UPDATED] 계산 로직 (2025-12-25):**
     * 1. TimerTimeManager.elapsedMillis + _historyDays를 combine
     * 2. currentTimerDays: millis → days 변환 (floor 연산)
     * 3. totalDays: historyDays + currentTimerDays (★핵심: 과거 + 현재 합산)
     * 4. level: LevelDefinitions 기준 계산
     * 5. 값이 변경될 때만 방출 (distinctUntilChanged)
     *
     * **특징:**
     * - Eagerly 시작: 앱 시작 즉시 구독 시작
     * - 자동 업데이트: 타이머 또는 DB 기록 변경 시 자동 반영
     * - 성능 최적화: 동일한 값은 재방출하지 않음
     */
    val userStatus: StateFlow<UserStatus> = combine(
        TimerTimeManager.elapsedMillis,
        _historyDays
    ) { millis, historyDays ->
        calculateUserStatus(millis, historyDays)
    }
        .distinctUntilChanged()
        .stateIn(
            scope = managerScope,
            started = SharingStarted.Eagerly,
            initialValue = UserStatus.DEFAULT
        )

    /**
     * 경과 시간(밀리초) + 과거 기록(일수)을 UserStatus로 변환
     * [FIX] 타이머 상태 보호 강화 (2026-01-04)
     *
     * @param millis 현재 타이머 경과 시간 (밀리초, TimerTimeManager로부터)
     * @param historyDays 과거 기록의 총 금주 일수 (Float, Tab02ViewModel로부터)
     * @return UserStatus 객체
     *
     * **중요**:
     * - millis는 TimerTimeManager에서 독립적으로 관리되며, 기록 삭제에 영향받지 않음
     * - historyDays는 과거 완료된 기록만 포함하며, 현재 타이머와 완전히 분리됨
     * - 두 값을 합산하여 totalDaysPrecise를 계산 (과거 + 현재)
     */
    private fun calculateUserStatus(millis: Long, historyDays: Float): UserStatus {
        // 1. 현재 타이머의 경과 일수 계산 (Float 정밀도)
        val currentTimerDaysFloat = if (millis > 0L) {
            (millis.toDouble() / Constants.DAY_IN_MILLIS.toDouble()).toFloat()
        } else {
            0f
        }

        // 2. ★핵심: 과거 기록 + 현재 타이머 합산 (독립적으로 관리됨)
        val totalDaysPrecise = historyDays + currentTimerDaysFloat

        // 3. 정수형 일수 (기존 호환성 유지)
        val totalDays = totalDaysPrecise.toInt()

        // [DEBUG] 타이머 상태 추적 로그 (2026-01-04)
        if (millis > 0L) {
            android.util.Log.d("UserStatusManager", "⏱️ Timer Active: current=${currentTimerDaysFloat}d, history=${historyDays}d, total=${totalDaysPrecise}d")
        } else {
            android.util.Log.d("UserStatusManager", "📊 Timer Idle: history=${historyDays}d, total=${totalDaysPrecise}d")
        }

        // 4. 레벨 계산 (0-indexed → 1-indexed 변환)
        val levelNumber = LevelDefinitions.getLevelNumber(totalDays)
        val level = if (levelNumber >= 0) levelNumber + 1 else 1

        // [NEW] 레벨업 감지 및 Analytics 전송 (2026-01-02)
        if (level > previousLevel && previousLevel > 0) {
            try {
                val levelInfo = LevelDefinitions.getLevelInfo(totalDays)
                // [FIX] levelName은 Context 필요하므로 toString() 대신 레벨 번호 사용 (2026-01-02)
                AnalyticsManager.logLevelUp(
                    oldLevel = previousLevel,
                    newLevel = level,
                    totalDays = totalDays,
                    levelName = "Level $level" // Context 없이도 사용 가능한 기본 이름
                )
                android.util.Log.d("UserStatusManager", "Analytics: level_up event sent (${previousLevel} → ${level})")
            } catch (e: Exception) {
                android.util.Log.e("UserStatusManager", "Failed to log level_up", e)
            }
        }
        previousLevel = level

        return UserStatus(
            level = level,
            days = totalDays,
            totalDaysPrecise = totalDaysPrecise
        )
    }
}

