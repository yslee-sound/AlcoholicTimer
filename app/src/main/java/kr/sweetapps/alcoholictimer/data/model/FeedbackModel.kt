package kr.sweetapps.alcoholictimer.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * 고객 문의 데이터 모델
 * Firestore 컬렉션: "feedbacks"
 */
data class FeedbackModel(
    val category: String = "",
    val content: String = "",
    val email: String? = null,
    @ServerTimestamp // [NEW] Firestore 서버 타임스탬프로 저장 (Nullable)
    val createdAt: Date? = null,
    val appVersion: String = "",
    val deviceModel: String = android.os.Build.MODEL,
    val osVersion: String = "Android ${android.os.Build.VERSION.RELEASE}"
)
