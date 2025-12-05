package kr.sweetapps.alcoholictimer.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kr.sweetapps.alcoholictimer.data.model.NotificationItem

/**
 * 앱 공지사항/알림 레포지토리
 */
class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("app_notices")

    /**
     * Firestore에서 알림 목록 가져오기 (최신순 정렬)
     */
    suspend fun fetchNotifications(): Result<List<NotificationItem>> {
        return try {
            val snapshot = collection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val notifications = snapshot.documents.mapNotNull { doc ->
                doc.toObject(NotificationItem::class.java)?.copy(id = doc.id)
            }

            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

