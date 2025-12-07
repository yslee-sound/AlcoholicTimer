package kr.sweetapps.alcoholictimer.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kr.sweetapps.alcoholictimer.BuildConfig

/**
 * 고객 피드백 Repository
 * Firebase Firestore의 "feedbacks" 컬렉션에 데이터 저장
 */
class FeedbackRepository {

    private val firestore = Firebase.firestore
    private val collectionName = "feedbacks"

    companion object {
        private const val TAG = "FeedbackRepository"
    }

    /**
     * 피드백을 Firestore에 저장
     *
     * @param category 문의 유형 (예: "기능 제안", "버그 신고", "기타 문의")
     * @param content 문의 내용
     * @param email 답변 받을 이메일 (선택 사항)
     * @param onSuccess 성공 시 콜백
     * @param onFailure 실패 시 콜백 (에러 메시지 전달)
     */
    fun submitFeedback(
        category: String,
        content: String,
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val feedbackMap = hashMapOf(
            "category" to category,
            "content" to content,
            "email" to email.ifEmpty { null },
            // [NEW] 서버에서 생성된 Timestamp를 사용하도록 토큰 전달
            "createdAt" to FieldValue.serverTimestamp(),
            "appVersion" to BuildConfig.VERSION_NAME,
            "deviceModel" to android.os.Build.MODEL,
            "osVersion" to "Android ${android.os.Build.VERSION.RELEASE}"
        )

        firestore.collection(collectionName)
            .add(feedbackMap)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Feedback submitted successfully: ${documentReference.id}")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error submitting feedback", exception)
                onFailure(exception.message ?: "알 수 없는 오류가 발생했습니다.")
            }
    }
}
