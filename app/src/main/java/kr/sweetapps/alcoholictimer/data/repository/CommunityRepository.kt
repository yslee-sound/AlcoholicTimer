package kr.sweetapps.alcoholictimer.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kr.sweetapps.alcoholictimer.data.model.Post
import java.util.Calendar

/**
 * Phase 2: 커뮤니티 게시글 Repository
 * Firestore posts 컬렉션 관리
 */
class CommunityRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val postsCollection = firestore.collection("posts")

    companion object {
        private const val TAG = "CommunityRepository"
    }

    /**
     * 실시간 게시글 목록 가져오기
     * createdAt 내림차순 정렬 (최신글이 위로)
     */
    fun getPosts(): Flow<List<Post>> = callbackFlow {
        val listener = postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to posts", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Post::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing post: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                trySend(posts)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Phase 2: 테스트용 더미 게시글 10개 생성
     * Tab 5 디버그 메뉴에서 호출
     */
    suspend fun generateDummyPosts(): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val now = Timestamp.now()
            val deleteAt = Timestamp(now.seconds + 24 * 60 * 60, 0) // 24시간 후

            val nicknames = listOf(
                "익명 1", "참는 중인 사자", "새벽의 독수리", "조용한 늑대",
                "밤하늘의 별", "아침의 햇살", "익명의 호랑이", "강한 곰",
                "자유로운 독수리", "평화로운 사슴"
            )

            val contents = listOf(
                "오늘도 술 없이 하루를 보냈습니다. 처음엔 힘들었지만 점점 익숙해지고 있어요. 여러분도 할 수 있습니다!",
                "3일차인데 생각보다 괜찮네요. 아침에 일어나는 게 훨씬 가벼워요 😊",
                "친구들이 술 마시자고 할 때가 제일 힘들지만 거절하는 연습을 하고 있어요.",
                "술 없이 보낸 주말이 이렇게 길게 느껴질 줄은 몰랐어요. 그래도 뿌듯합니다!",
                "일주일을 채웠습니다! 🎉 건강검진 결과가 좋아졌어요. 계속 이어갈게요!",
                "하루만 해보자는 마음으로 시작했는데 여기까지 왔네요. 작은 성공이 큰 힘이 됩니다.",
                "8일째! 숙면을 취하니까 피부도 좋아지고 기분도 상쾌해요. 앞으로도 화이팅!",
                "처음 3일이 가장 힘들었어요. 지금은 습관이 된 것 같습니다.",
                "술 끊고 나니 저축한 돈이 눈에 보이네요. 경제적으로도 좋은 선택이었어요!",
                "가족들이 제 변화를 알아봐 주셔서 더 힘이 납니다. 감사합니다 💪"
            )

            for (i in 0 until 10) {
                val postRef = postsCollection.document()
                val hasImage = i % 3 == 0 // 3개 중 1개만 이미지 포함

                val post = Post(
                    id = postRef.id,
                    nickname = nicknames[i],
                    timerDuration = "${(i + 1) * 24}시간", // 24, 48, 72... 240시간
                    content = contents[i],
                    imageUrl = if (hasImage) "https://picsum.photos/seed/${i}/400/300" else null,
                    likeCount = (0..50).random(),
                    createdAt = Timestamp(now.seconds - i * 3600, 0), // 1시간씩 간격
                    deleteAt = deleteAt
                )

                batch.set(postRef, post)
            }

            batch.commit().await()
            Log.d(TAG, "Successfully generated 10 dummy posts")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating dummy posts", e)
            Result.failure(e)
        }
    }

    /**
     * Phase 2: 모든 게시글 삭제 (테스트용)
     */
    suspend fun deleteAllPosts(): Result<Unit> {
        return try {
            val snapshot = postsCollection.get().await()
            val batch = firestore.batch()

            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            batch.commit().await()
            Log.d(TAG, "Successfully deleted all posts")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting posts", e)
            Result.failure(e)
        }
    }

    /**
     * [NEW] 새 게시글 추가
     */
    suspend fun addPost(post: Post): Result<Unit> {
        return try {
            val postRef = postsCollection.document()
            val postWithId = post.copy(id = postRef.id)
            postRef.set(postWithId).await()

            Log.d(TAG, "Successfully added post: ${postRef.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding post", e)
            Result.failure(e)
        }
    }
}
