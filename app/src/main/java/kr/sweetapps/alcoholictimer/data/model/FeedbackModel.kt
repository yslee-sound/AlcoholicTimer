package kr.sweetapps.alcoholictimer.data.model

import android.os.Build
import kr.sweetapps.alcoholictimer.BuildConfig

/**
 * 고객 문의/피드백 데이터 모델
 * Firebase Firestore의 feedbacks 컬렉션에 저장됨
 */
data class FeedbackModel(
    val category: String = "", // 기능 제안, 버그 신고, 기타 문의
    val content: String = "", // 문의 내용
    val email: String = "", // 이메일 주소 (선택 사항)
    val createdAt: Long = System.currentTimeMillis(), // 생성 시간
    val appVersion: String = BuildConfig.VERSION_NAME, // 앱 버전
    val deviceModel: String = Build.MODEL, // 기기 모델
    val osVersion: String = "Android ${Build.VERSION.RELEASE}" // OS 버전
) {
    // Firestore용 기본 생성자 (필수)
    constructor() : this("", "", "", 0L, "", "", "")

    /**
     * Firestore에 저장할 Map 형태로 변환
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "category" to category,
            "content" to content,
            "email" to email.ifEmpty { "" },
            "createdAt" to createdAt,
            "appVersion" to appVersion,
            "deviceModel" to deviceModel,
            "osVersion" to osVersion
        )
    }
}

