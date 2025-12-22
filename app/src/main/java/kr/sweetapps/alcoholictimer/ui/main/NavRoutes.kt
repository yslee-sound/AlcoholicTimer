package kr.sweetapps.alcoholictimer.ui.main

/**
 * Sealed Class defining all screens in the app
 * Used as routes for Jetpack Compose Navigation
 *
 * [REFACTORING 2025-12-19] 탭 구조 재정의:
 * Tab 1: Timer (Start/Run)
 * Tab 2: Records + LevelDetail (하위)
 * Tab 3: Community (More) + Settings (About 하위)
 */
sealed class Screen(val route: String) {
    /**
     * Start screen (before starting sobriety)
     */
    data object Start : Screen("start")

    /**
     * Running screen (during sobriety)
     */
    data object Run : Screen("run")

    /**
     * [REFACTORED] Success screen (timer completed - goal achieved)
     */
    data object Success : Screen("success")

    /**
     * [REFACTORED] GiveUp screen (timer stopped - user gave up)
     */
    data object GiveUp : Screen("giveup")

    /**
     * Quit confirmation screen
     */
    data object Quit : Screen("quit")

    /**
     * Records list screen (recent 3 items)
     */
    data object Records : Screen("records")

    /**
     * All records screen
     */
    data object AllRecords : Screen("all_records")

    /** All diary entries (all sobriety journal entries) screen */
    data object AllDiary : Screen("all_diaries")

    /**
     * [REMOVED] Level screen - 이제 탭이 아닌 상세 페이지로만 접근
     * LevelDetail을 사용하세요
     */
    // data object Level : Screen("level")

    /**
     * [NEW] Level Detail screen (레벨 상세 페이지)
     * Tab 2의 레벨 요약 배너에서 진입하는 전체 화면
     */
    data object LevelDetail : Screen("level_detail")

    /**
     * [REFACTORED 2025-12-19] More screen → Community (커뮤니티) 메인 화면
     * "익명 응원 챌린지" 화면
     * 기존: Tab 4 (More)
     * 신규: Tab 3 (Community)
     * 라우트명 변경 없음 - 하위 호환성 100% 유지
     */
    data object More : Screen("more")

    /**
     * [REFACTORED 2025-12-19] About screen → Settings (설정) 진입점
     * 커뮤니티 화면의 설정 버튼으로 진입
     * 기존: Tab 5 (독립 탭)
     * 신규: Tab 3 하위 메뉴
     * 라우트명 변경 없음 - 하위 호환성 100% 유지
     */
    data object About : Screen("about")

    /**
     * Licenses screen
     */
    data object AboutLicenses : Screen("about_licenses")

    /**
     * Privacy policy (web view / external link)
     */
    data object Privacy : Screen("about_privacy")

    /**
     * Nickname edit screen
     */
    data object NicknameEdit : Screen("nickname_edit")

    /**
     * Currency settings screen
     */
    data object CurrencySettings : Screen("currency_settings")

    /**
     * [NEW] Habit settings screen (습관 설정)
     * 기존 Tab04의 습관 설정 기능을 Tab05의 하위 화면으로 이동
     */
    data object HabitSettings : Screen("habit_settings")

    /**
     * Debug screen for UMP consent verification
     */
    data object Debug : Screen("debug")

    /**
     * Record detail screen
     */
    data object Detail : Screen("detail/{startTime}/{endTime}/{targetDays}/{actualDays}/{isCompleted}") {
        fun createRoute(
            startTime: Long,
            endTime: Long,
            targetDays: Float,
            actualDays: Int,
            isCompleted: Boolean
        ) = "detail/$startTime/$endTime/$targetDays/$actualDays/$isCompleted"
    }

    /**
     * [NEW] Timer completion result screen (독립적인 결과 발표 화면)
     * 타이머 완료 직후에만 사용. UI는 DetailScreen과 동일하지만 네비게이션 경로가 독립적임.
     * 이렇게 분리하면 Tab 2의 Detail과 간섭하지 않아 네비게이션 스택 관리가 깔끔해짐.
     */
    data object Result : Screen("result/{startTime}/{endTime}/{targetDays}/{actualDays}/{isCompleted}") {
        fun createRoute(
            startTime: Long,
            endTime: Long,
            targetDays: Float,
            actualDays: Int,
            isCompleted: Boolean
        ) = "result/$startTime/$endTime/$targetDays/$actualDays/$isCompleted"
    }

    /** Add record screen (shown as subpage) */
    data object AddRecord : Screen("add_record")

    /** [NEW] Diary write screen */
    data object DiaryWrite : Screen("diary_write?selectedDate={selectedDate}") {
        fun createRoute(selectedDate: Long? = null): String {
            return if (selectedDate != null) {
                "diary_write?selectedDate=$selectedDate"
            } else {
                "diary_write"
            }
        }
    }

    /** [NEW] Diary detail/edit screen */
    data object DiaryDetail : Screen("diary_detail/{diaryId}") {
        fun createRoute(diaryId: String) = "diary_detail/$diaryId"
    }

    /** [NEW] Notification list screen */
    data object Notification : Screen("notification")
}

