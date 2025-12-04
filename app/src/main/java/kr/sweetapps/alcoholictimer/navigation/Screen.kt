package kr.sweetapps.alcoholictimer.navigation

/**
 * Sealed Class defining all screens in the app
 * Used as routes for Jetpack Compose Navigation
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
     * Finished screen (timer expired)
     */
    data object Finished : Screen("finished")

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
     * Level screen
     */
    data object Level : Screen("level")

    /**
     * Settings screen
     */
    data object More : Screen("more")

    /**
     * About screen
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
     * Debug: Ad verifier screen (development only)
     */
    data object DebugAds : Screen("debug_ads")

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

    /** Add record screen (shown as subpage) */
    data object AddRecord : Screen("add_record")

    /** [NEW] Diary write screen */
    data object DiaryWrite : Screen("diary_write")

    /** [NEW] Diary detail/edit screen */
    data object DiaryDetail : Screen("diary_detail/{diaryId}") {
        fun createRoute(diaryId: String) = "diary_detail/$diaryId"
    }
}

/**
 * Extension function to return screen title resource ID
 */
fun Screen.getTitleResId(): Int? = when (this) {
    is Screen.Start -> kr.sweetapps.alcoholictimer.R.string.start_screen_title
    is Screen.Run -> kr.sweetapps.alcoholictimer.R.string.run_title
    is Screen.Quit -> kr.sweetapps.alcoholictimer.R.string.quit_title
    is Screen.Records -> null // kr.sweetapps.alcoholictimer.R.string.records_title
    is Screen.AllRecords -> kr.sweetapps.alcoholictimer.R.string.all_records_title
    is Screen.Level -> kr.sweetapps.alcoholictimer.R.string.level_title
    is Screen.More -> kr.sweetapps.alcoholictimer.R.string.more_title
    is Screen.About -> kr.sweetapps.alcoholictimer.R.string.about_title
    is Screen.AboutLicenses -> null // No resource
    is Screen.Privacy -> null
    is Screen.NicknameEdit -> null // Uses custom title
    is Screen.CurrencySettings -> kr.sweetapps.alcoholictimer.R.string.settings_currency
    is Screen.Detail -> null // Uses custom title
    is Screen.AddRecord -> kr.sweetapps.alcoholictimer.R.string.add_record_title
    else -> null
}
