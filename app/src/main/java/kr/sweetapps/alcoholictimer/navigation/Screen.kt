package kr.sweetapps.alcoholictimer.navigation

/**
 * 앱의 모든 화면을 정의하는 Sealed Class
 * Jetpack Compose Navigation의 route로 사용됨
 */
sealed class Screen(val route: String) {
    /**
     * 금주 시작 화면 (금주 진행 전)
     */
    data object Start : Screen("start")

    /**
     * 금주 진행 화면 (금주 진행 중)
     */
    data object Run : Screen("run")

    /**
     * 금주 완료 화면 (타이머 만료)
     */
    data object Finished : Screen("finished")

    /**
     * 종료 확인 화면
     */
    data object Quit : Screen("quit")

    /**
     * 기록 목록 화면 (최근 3개)
     */
    data object Records : Screen("records")

    /**
     * 전체 기록 화면
     */
    data object AllRecords : Screen("all_records")

    /**
     * 레벨 화면
     */
    data object Level : Screen("level")

    /**
     * 설정 화면
     */
    data object Settings : Screen("settings")

    /**
     * 정보 화면
     */
    data object About : Screen("about")

    /**
     * 라이선스 화면
     */
    data object AboutLicenses : Screen("about_licenses")

    /**
     * 개인정보 처리방침 (웹 뷰 / 외부 링크)
     */
    data object Privacy : Screen("about_privacy")

    /**
     * 닉네임 편집 화면
     */
    data object NicknameEdit : Screen("nickname_edit")

    /**
     * 통화 설정 화면
     */
    data object CurrencySettings : Screen("currency_settings")

    /**
     * Debug: Ad verifier screen (development only)
     */
    data object DebugAds : Screen("debug_ads")

    /**
     * Debug screen for UMP consent verification
     */
    data object Debug : Screen("debug")

    /**
     * 기록 상세 화면
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

    /** 기록 추가 화면 (하위 페이지로 표시) */
    data object AddRecord : Screen("add_record")

    /** [NEW] 일기 작성 화면 */
    data object DiaryWrite : Screen("diary_write")
}

/**
 * 화면 제목을 반환하는 확장 함수
 */
fun Screen.getTitleResId(): Int? = when (this) {
    is Screen.Start -> kr.sweetapps.alcoholictimer.R.string.start_screen_title
    is Screen.Run -> kr.sweetapps.alcoholictimer.R.string.run_title
    is Screen.Quit -> kr.sweetapps.alcoholictimer.R.string.quit_title
    is Screen.Records -> null // kr.sweetapps.alcoholictimer.R.string.records_title
    is Screen.AllRecords -> kr.sweetapps.alcoholictimer.R.string.all_records_title
    is Screen.Level -> kr.sweetapps.alcoholictimer.R.string.level_title
    is Screen.Settings -> kr.sweetapps.alcoholictimer.R.string.settings_title
    is Screen.About -> kr.sweetapps.alcoholictimer.R.string.about_title
    is Screen.AboutLicenses -> null // 리소스 없음
    is Screen.Privacy -> null
    is Screen.NicknameEdit -> null // 커스텀 제목 사용
    is Screen.CurrencySettings -> kr.sweetapps.alcoholictimer.R.string.settings_currency
    is Screen.Detail -> null // 커스텀 제목 사용
    is Screen.AddRecord -> kr.sweetapps.alcoholictimer.R.string.add_record_title
    else -> null
}
