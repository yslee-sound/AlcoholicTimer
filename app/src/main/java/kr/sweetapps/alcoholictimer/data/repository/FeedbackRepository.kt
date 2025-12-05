package kr.sweetapps.alcoholictimer.data.repository

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kr.sweetapps.alcoholictimer.data.model.FeedbackModel

/**
 * 고객 피드백 Repository
 * Firebase Firestore의 feedbacks 컬렉션에 데이터 저장
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
     * @param feedback 저장할 피드백 데이터
     * @param onSuccess 성공 시 콜백
     * @param onFailure 실패 시 콜백 (에러 메시지 전달)
     */
    fun submitFeedback(
        feedback: FeedbackModel,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection(collectionName)
            .add(feedback.toMap())
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Feedback submitted successfully: ${documentReference.id}")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error submitting feedback", exception)
                onFailure(exception.message ?: "알 수 없는 오류가 발생했습니다.")
            }
    }

    /**
     * 피드백 전송 (간단 버전 - 카테고리, 내용, 이메일만 받음)
     */
    fun submitFeedback(
        category: String,
        content: String,
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val feedback = FeedbackModel(
            category = category,
            content = content,
            email = email
        )
        submitFeedback(feedback, onSuccess, onFailure)
    }
}

