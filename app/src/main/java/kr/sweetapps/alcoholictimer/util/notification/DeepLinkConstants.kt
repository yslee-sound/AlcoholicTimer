package kr.sweetapps.alcoholictimer.util.notification

/**
 * 알림 딥링크 상수
 *
 * 알림 클릭 시 이동할 화면 경로 및 Extra 키 정의
 *
 * @since 2025-12-31
 */
object DeepLinkConstants {

    // Intent Extra Keys
    const val EXTRA_SCREEN_ROUTE = "extra_screen_route"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_GROUP_TYPE = "extra_group_type"
    const val EXTRA_SHOW_BADGE_ANIMATION = "extra_show_badge_animation"

    // Screen Routes
    const val ROUTE_START = "start"
    const val ROUTE_RUN = "run"
    const val ROUTE_SUCCESS = "success"
    const val ROUTE_RECORDS = "records"

    /**
     * 알림 그룹별 목적지 화면 반환
     *
     * @param group 알림 그룹
     * @return 목적지 화면 경로
     */
    fun getTargetScreen(group: String): String {
        return when (group) {
            NotificationWorker.GROUP_NEW_USER -> ROUTE_START     // 그룹 A: START 화면
            NotificationWorker.GROUP_RESTING_USER -> ROUTE_START // 그룹 C: START 화면
            NotificationWorker.GROUP_ACTIVE_USER -> ROUTE_SUCCESS // 그룹 B: SUCCESS 화면
            else -> ROUTE_START
        }
    }

    /**
     * 배지 애니메이션 표시 여부 (그룹 B 마일스톤만)
     *
     * @param group 알림 그룹
     * @return true: 배지 애니메이션 표시
     */
    fun shouldShowBadgeAnimation(group: String): Boolean {
        return group == NotificationWorker.GROUP_ACTIVE_USER
    }
}

