package kr.sweetapps.alcoholictimer.ui.tab_05.screens.debug

/**
 * 디버그용 데모 데이터를 관리하는 객체
 */
object DemoData {
    /**
     * 데모 모드에서 사용할 금주 진행 일수
     * 이 값을 변경하면 데모 화면의 수치가 변경됩니다.
     * 예: 97.345일
     */
    const val DEMO_ELAPSED_DAYS = 97.345

    /**
     * 데모 모드에서 사용할 목표일
     * 이 값을 변경하면 데모 화면의 목표일 및 진행률이 변경됩니다.
     * 예: 120일
     */
    const val DEMO_TARGET_DAYS = 100f

    /**
     * 데모 모드에서 사용할 레벨
     * 이 값을 변경하면 데모 화면의 레벨이 변경됩니다.
     */
    const val DEMO_LEVEL = 7
}
