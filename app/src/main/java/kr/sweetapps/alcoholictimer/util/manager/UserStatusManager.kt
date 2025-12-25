package kr.sweetapps.alcoholictimer.util.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kr.sweetapps.alcoholictimer.ui.tab_02.components.LevelDefinitions
import kr.sweetapps.alcoholictimer.util.constants.Constants

/**
 * [NEW] 사용자 상태 중앙 관리자 (2025-12-25)
 *
 * **목적:**
 * - 앱 전체에서 사용자의 레벨/일수를 일관되게 제공
 * - TimerTimeManager의 경과 시간을 구독하여 자동으로 UserStatus 계산
 * - 모든 화면(Run, Diary, Community 등)에서 동일한 데이터 사용 보장
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
     * @param level 레벨 번호 (1부터 시작, 1-indexed)
     * @param days 경과 일수 (0부터 시작, floor 연산)
     */
    data class UserStatus(
        val level: Int,
        val days: Int
    ) {
        companion object {
            val DEFAULT = UserStatus(level = 1, days = 0)
        }
    }

    /**
     * 전역 코루틴 스코프 (앱 생명주기와 독립적)
     * SupervisorJob 사용으로 자식 코루틴 실패 시에도 스코프 유지
     */
    private val managerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * 사용자 상태 StateFlow (읽기 전용)
     *
     * **계산 로직:**
     * 1. TimerTimeManager.elapsedMillis 구독
     * 2. millis → days 변환 (floor 연산)
     * 3. days → level 계산 (LevelDefinitions 사용)
     * 4. 값이 변경될 때만 방출 (distinctUntilChanged)
     *
     * **특징:**
     * - Eagerly 시작: 앱 시작 즉시 구독 시작
     * - 자동 업데이트: 타이머 변경 시 자동 반영
     * - 성능 최적화: 동일한 값은 재방출하지 않음
     */
    val userStatus: StateFlow<UserStatus> = TimerTimeManager.elapsedMillis
        .map { millis ->
            calculateUserStatus(millis)
        }
        .distinctUntilChanged()
        .stateIn(
            scope = managerScope,
            started = SharingStarted.Eagerly,
            initialValue = UserStatus.DEFAULT
        )

    /**
     * 경과 시간(밀리초)을 UserStatus로 변환
     *
     * @param millis 경과 시간 (밀리초)
     * @return UserStatus 객체
     */
    private fun calculateUserStatus(millis: Long): UserStatus {
        // [CASE 1] 타이머가 시작되지 않았거나 음수일 때
        if (millis <= 0L) {
            return UserStatus(level = 1, days = 0)
        }

        // [CASE 2] 정상 경과 시간
        // days: floor 연산 (꽉 채운 일수만 카운트)
        val days = (millis / Constants.DAY_IN_MILLIS).toInt()

        // level: LevelDefinitions 기준 계산 (0-indexed → 1-indexed 변환)
        val levelNumber = LevelDefinitions.getLevelNumber(days)
        val level = if (levelNumber >= 0) levelNumber + 1 else 1

        return UserStatus(level = level, days = days)
    }
}

