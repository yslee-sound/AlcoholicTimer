package kr.sweetapps.alcoholictimer.data.model

import java.util.Date

/**
 * 앱 공지사항/알림 데이터 모델
 * Firestore 컬렉션: "app_notices"
 */
data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    // [NEW] Firestore ServerTimestamp 사용을 위해 Date? 타입으로 변경 (nullable, default null)
    val timestamp: Date? = null,
    val isRead: Boolean = false, // 로컬 상태 관리용
    val type: String = "NOTICE" // NOTICE, EVENT, UPDATE 등
)
