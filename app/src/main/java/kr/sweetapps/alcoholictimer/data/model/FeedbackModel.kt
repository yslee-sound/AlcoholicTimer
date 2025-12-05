package kr.sweetapps.alcoholictimer.data.model

/**
 * 고객 문의 데이터 모델
 * Firestore 컬렉션: "feedbacks"
 */
data class FeedbackModel(
    val category: String = "",
    val content: String = "",
    val email: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val appVersion: String = "",
    val deviceModel: String = android.os.Build.MODEL,
    val osVersion: String = "Android ${android.os.Build.VERSION.RELEASE}"
)

