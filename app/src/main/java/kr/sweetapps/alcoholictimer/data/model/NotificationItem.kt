package kr.sweetapps.alcoholictimer.data.model

/**
 * 앱 공지사항/알림 데이터 모델
 * Firestore 컬렉션: "app_notices"
 */
data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false, // 로컬 상태 관리용
    val type: String = "NOTICE" // NOTICE, EVENT, UPDATE 등
)

