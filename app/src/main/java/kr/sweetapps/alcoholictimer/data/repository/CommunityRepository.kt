package kr.sweetapps.alcoholictimer.data.repository

import android.content.Context
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
import java.util.UUID

/**
 * Phase 2: 커뮤니티 게시글 Repository
 * Firestore posts 컬렉션 관리
 *
 * (v3.0) 테스트 데이터 생성 시 내 글/남의 글 구분
 */
class CommunityRepository(private val context: Context? = null) {
    private val firestore = FirebaseFirestore.getInstance()
    private val postsCollection = firestore.collection("posts")

    // [NEW] UserRepository (테스트 데이터 생성용)
    private val userRepository: UserRepository? = context?.let { UserRepository(it) }

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
     *
     * [UPDATED] Phase 3: 내 글 3개 + 남의 글 7개로 생성
     * - 첫 3개: 내 글 (authorId = 현재 사용자 ID)
     * - 나머지 7개: 남의 글 (authorId = 랜덤 UUID)
     */
    suspend fun generateDummyPosts(targetLanguage: String = "en"): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val now = Timestamp.now()
            val deleteAt = Timestamp(now.seconds + 24 * 60 * 60, 0) // 24시간 후

            // [NEW] 내 사용자 ID 가져오기
            val myUserId = userRepository?.getInstallationId() ?: UUID.randomUUID().toString()

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

                // [NEW] Phase 3: 첫 3개는 내 글, 나머지는 남의 글
                val authorId = if (i < 3) {
                    myUserId // 내 글 (삭제 테스트용)
                } else {
                    UUID.randomUUID().toString() // 남의 글 (숨기기/신고 테스트용)
                }

                val post = Post(
                    id = postRef.id,
                    nickname = nicknames[i],
                    timerDuration = "${(i + 1) * 24}시간", // 24, 48, 72... 240시간
                    content = contents[i],
                    imageUrl = if (hasImage) "https://picsum.photos/seed/${i}/400/300" else null,
                    likeCount = (0..50).random(),
                    createdAt = Timestamp(now.seconds - i * 3600, 0), // 1시간씩 간격
                    deleteAt = deleteAt,
                    authorAvatarIndex = (0..19).random(), // [NEW] 랜덤 아바타 (0~19)
                    authorId = authorId // [NEW] Phase 3: 작성자 ID
                    ,languageCode = targetLanguage
                )

                batch.set(postRef, post)
            }

            batch.commit().await()
            Log.d(TAG, "Successfully generated 10 dummy posts (3 mine + 7 others)")
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

    /**
     * 실시간 게시글 목록 가져오기
     * - languageCode: ISO 639-1 코드로 필터. null이면 모든 언어를 반환
     * - includeEnglishFallback: true일 경우 primary 언어 포스트가 적으면 영어("en") 포스트를 함께 병합하여 반환
     */
    fun getPosts(languageCode: String? = null, includeEnglishFallback: Boolean = false): Flow<List<Post>> = callbackFlow {
        if (languageCode == null) {
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
        } else if (!includeEnglishFallback) {
            val q = postsCollection
                .whereEqualTo("languageCode", languageCode)
                .orderBy("createdAt", Query.Direction.DESCENDING)

            val listener = q.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to posts(language=$languageCode)", error)
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
        } else {
            // includeEnglishFallback == true: set up two listeners and merge results
            val primaryQuery = postsCollection
                .whereEqualTo("languageCode", languageCode)
                .orderBy("createdAt", Query.Direction.DESCENDING)

            val fallbackQuery = postsCollection
                .whereEqualTo("languageCode", "en")
                .orderBy("createdAt", Query.Direction.DESCENDING)

            var primaryList: List<Post> = emptyList()
            var fallbackList: List<Post> = emptyList()

            val primaryListener = primaryQuery.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening primary language posts ($languageCode)", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                primaryList = snapshot?.documents?.mapNotNull { doc ->
                    try { doc.toObject(Post::class.java) } catch (e: Exception) { null }
                } ?: emptyList()

                // merge and send
                val merged = mutableListOf<Post>()
                val ids = mutableSetOf<String>()
                primaryList.forEach { if (ids.add(it.id)) merged.add(it) }
                if (merged.size < 10) {
                    fallbackList.forEach { if (ids.add(it.id)) merged.add(it) }
                }
                trySend(merged)
            }

            val fallbackListener = fallbackQuery.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening fallback English posts", error)
                    return@addSnapshotListener
                }

                fallbackList = snapshot?.documents?.mapNotNull { doc ->
                    try { doc.toObject(Post::class.java) } catch (e: Exception) { null }
                } ?: emptyList()

                // merge and send (use latest primaryList)
                val merged = mutableListOf<Post>()
                val ids = mutableSetOf<String>()
                primaryList.forEach { if (ids.add(it.id)) merged.add(it) }
                if (merged.size < 10) {
                    fallbackList.forEach { if (ids.add(it.id)) merged.add(it) }
                }
                trySend(merged)
            }

            awaitClose {
                primaryListener.remove()
                fallbackListener.remove()
            }
        }
    }
}
