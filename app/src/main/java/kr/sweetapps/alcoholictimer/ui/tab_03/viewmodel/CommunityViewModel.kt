package kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kr.sweetapps.alcoholictimer.data.model.Post
import kr.sweetapps.alcoholictimer.data.repository.CommunityRepository
import kr.sweetapps.alcoholictimer.data.repository.UserRepository
import java.util.UUID

/**
 * Phase 2: 커뮤니티 ViewModel
 * Firestore posts 데이터를 실시간으로 관리
 *
 * (v2.0) 아바타 시스템 추가:
 * - UserRepository를 통해 사용자의 아바타 인덱스 관리
 * (v2.1) WritePostTrigger 아바타 실시간 반영
 * (v2.2) 이미지 업로드 기능 추가 (2025-12-19)
 * (v3.0) 게시글 관리 기능 추가 (2025-12-20):
 * - authorId를 통한 소유권 식별
 * - 삭제, 숨기기, 신고 기능
 */
class CommunityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CommunityRepository(application.applicationContext) // [FIX] Context 전달
    private val userRepository = UserRepository(application.applicationContext) // 아바타 관리용

    // [NEW] Phase 3: 기기 고유 ID (Installation ID)
    private val deviceUserId: String by lazy {
        userRepository.getInstallationId()
    }

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // [NEW] Pull-to-Refresh 상태 (2025-12-20)
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // [NEW] 현재 사용자의 아바타 인덱스
    private val _currentUserAvatarIndex = MutableStateFlow(0)
    val currentUserAvatarIndex: StateFlow<Int> = _currentUserAvatarIndex.asStateFlow()

    // [NEW] 선택된 이미지 URI (미리보기용) (2025-12-19)
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    // [NEW] Phase 3: 숨긴 게시글 ID 목록 (메모리 저장)
    private val _hiddenPostIds = MutableStateFlow<Set<String>>(emptySet())

    // [NEW] 최근 숨긴 게시글 임시 저장 (Undo 지원용)
    private val _recentlyHiddenPosts = MutableStateFlow<Map<String, Post>>(emptyMap())

    init {
        loadPosts()
        loadCurrentUserAvatar() // [NEW] 사용자 아바타 로드
    }

    /**
     * [NEW] Phase 3: 이 게시글이 내 글인지 확인
     */
    fun isMyPost(post: Post): Boolean {
        return post.authorId == deviceUserId
    }

    /**
     * [NEW] 이미지 선택 시 호출 (2025-12-19)
     */
    fun onImageSelected(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    /**
     * [NEW] 현재 사용자의 아바타 인덱스 로드
     * SharedPreferences 변경 감지를 위한 polling (간단한 구현)
     */
    private fun loadCurrentUserAvatar() {
        viewModelScope.launch {
            // 초기 로드
            val avatarIndex = userRepository.getAvatarIndex()
            _currentUserAvatarIndex.value = avatarIndex

            // 주기적으로 체크 (1초마다)
            // SharedPreferences 변경 시 즉시 반영
            kotlinx.coroutines.delay(1000)
            while (true) {
                val newAvatarIndex = userRepository.getAvatarIndex()
                if (newAvatarIndex != _currentUserAvatarIndex.value) {
                    _currentUserAvatarIndex.value = newAvatarIndex
                    android.util.Log.d("CommunityViewModel", "Avatar updated: $newAvatarIndex")
                }
                kotlinx.coroutines.delay(1000) // 1초마다 체크
            }
        }
    }

    /**
     * Firestore에서 게시글 실시간 구독
     * [NEW] Phase 3: 숨긴 게시글 + 만료된 게시글 필터링
     */
    private fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getPosts().collect { postList ->
                val currentTime = System.currentTimeMillis()

                // [FIX] 숨긴 글 + 만료된 글(deleteAt 지남) 필터링 (2025-12-20)
                _posts.value = postList.filter { post ->
                    val isHidden = _hiddenPostIds.value.contains(post.id)
                    // deleteAt이 null이면 삭제 안 함(안전장치), 현재 시간보다 미래여야 살아남음
                    val isExpired = (post.deleteAt?.seconds ?: Long.MAX_VALUE) * 1000 <= currentTime

                    !isHidden && !isExpired
                }
                _isLoading.value = false
            }
        }
    }

    /**
     * [NEW] Pull-to-Refresh: 게시글 새로고침 (2025-12-20)
     */
    fun refreshPosts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // loadPosts는 이미 Flow로 실시간 구독 중이므로
                // 여기서는 간단히 로딩 상태만 표시하고 자동으로 업데이트됨
                kotlinx.coroutines.delay(500) // 최소 표시 시간 (UX)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * 좋아요 여부 확인 (내가 좋아요 했는지)
     */
    fun isLikedByMe(post: Post): Boolean {
        return post.likedBy.contains(deviceUserId)
    }

    /**
     * 좋아요 토글 (Optimistic UI update + Firestore arrayUnion/arrayRemove)
     */
    fun toggleLike(post: Post) {
        val isLiked = isLikedByMe(post)
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val postRef = firestore.collection("posts").document(post.id)

        // 1) 낙관적 업데이트
        val updatedList = if (isLiked) post.likedBy - deviceUserId else post.likedBy + deviceUserId
        val updatedCount = if (isLiked) post.likeCount - 1 else post.likeCount + 1

        _posts.value = _posts.value.map { p ->
            if (p.id == post.id) p.copy(likedBy = updatedList, likeCount = updatedCount) else p
        }

        // 2) 서버 업데이트 비동기
        viewModelScope.launch {
            try {
                if (isLiked) {
                    postRef.update(
                        "likeCount", com.google.firebase.firestore.FieldValue.increment(-1),
                        "likedBy", com.google.firebase.firestore.FieldValue.arrayRemove(deviceUserId)
                    ).await()
                } else {
                    postRef.update(
                        "likeCount", com.google.firebase.firestore.FieldValue.increment(1),
                        "likedBy", com.google.firebase.firestore.FieldValue.arrayUnion(deviceUserId)
                    ).await()
                }
            } catch (e: Exception) {
                android.util.Log.e("CommunityViewModel", "좋아요 업데이트 실패", e)
                // MVP: 롤백 생략 (선택적으로 재요청 또는 롤백 가능)
            }
        }
    }

    /**
     * 새 게시글 작성 (Tab 4에서 직접 작성)
     * (v2.0) 아바타 시스템: 사용자의 avatarIndex를 Post에 포함
     * (v2.2) 이미지 업로드 기능 추가 (2025-12-19)
     * @param content 게시글 내용
     * @param context Context (이미지 압축에 필요)
     */
    fun addPost(content: String, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                var imageUrl: String? = null

                // 1. 이미지가 있다면 압축 후 Firebase Storage 업로드
                val currentUri = _selectedImageUri.value
                if (currentUri != null) {
                    // 압축 작업 (IO 스레드)
                    val imageBytes = withContext(Dispatchers.IO) {
                        kr.sweetapps.alcoholictimer.util.ImageUtils.compressImage(context, currentUri)
                    }

                    if (imageBytes != null) {
                        // Firebase Storage 업로드
                        val storageRef = Firebase.storage.reference
                            .child("community_images/${UUID.randomUUID()}.jpg")

                        // 업로드
                        storageRef.putBytes(imageBytes).await()
                        // 다운로드 URL 획득
                        imageUrl = storageRef.downloadUrl.await().toString()
                        android.util.Log.d("CommunityViewModel", "이미지 업로드 완료: $imageUrl")
                    }
                }

                // 2. [Standard] 사용자 닉네임 가져오기
                val nickname = userRepository.getNickname() ?: "익명"
                android.util.Log.d("CommunityViewModel", "사용할 닉네임: $nickname")

                // 3. 사용자의 현재 아바타 인덱스 가져오기
                val avatarIndex = try {
                    userRepository.getAvatarIndex()
                } catch (e: Exception) {
                    android.util.Log.e("CommunityViewModel", "아바타 인덱스 가져오기 실패, 기본값 0 사용", e)
                    0
                }

                // 4. 타이머 지속 시간 계산 (랜덤 또는 실제 유저 타이머)
                val timerDuration = calculateTimerDuration()

                // 5. 현재 시간
                val now = System.currentTimeMillis()
                val createdAt = com.google.firebase.Timestamp(now / 1000, 0)

                // [TEST] 빠른 테스트를 위해 게시글 수명을 1분으로 단축 (배포 시 24 * 60 * 60으로 복구 필요)
                val deleteAt = com.google.firebase.Timestamp((now / 1000) + 60, 0) // 1분 후

                // 6. Post 객체 생성 (이미지 URL 포함)
                val post = Post(
                    nickname = nickname,
                    timerDuration = timerDuration,
                    content = content,
                    imageUrl = imageUrl, // 업로드된 URL 또는 null
                    likeCount = 0,
                    createdAt = createdAt,
                    deleteAt = deleteAt,
                    authorAvatarIndex = avatarIndex,
                    authorId = deviceUserId // [NEW] Phase 3: 작성자 기기 ID
                )

                // 7. Firestore에 저장
                repository.addPost(post)

                // 8. 성공 후 이미지 URI 초기화
                _selectedImageUri.value = null

                // [FIX] 게시글 목록 새로고침 (2025-12-19)
                loadPosts()

                android.util.Log.d("CommunityViewModel", "게시글 작성 완료: $nickname (avatar: $avatarIndex, image: ${imageUrl != null})")
            } catch (e: Exception) {
                android.util.Log.e("CommunityViewModel", "게시글 작성 실패", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 현재 진행 중인 타이머 지속 시간 계산 (랜덤 fallback)
     */
    private fun calculateTimerDuration(): String {
        // TODO: 실제 유저의 타이머 데이터 가져오기
        // 현재는 랜덤으로 생성
        val hours = (24..240).random()
        return "${hours}시간"
    }

    // ==================== Phase 3: 게시글 관리 기능 ====================

    /**
     * [NEW] 게시글 삭제 (내 글만 가능)
     */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // Firestore에서 삭제
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                firestore.collection("posts").document(postId).delete().await()

                android.util.Log.d("CommunityViewModel", "게시글 삭제 완료: $postId")

                // 목록 새로고침 (자동으로 제거됨)
                // loadPosts() 호출 불필요 (실시간 리스너가 자동 업데이트)
            } catch (e: Exception) {
                android.util.Log.e("CommunityViewModel", "게시글 삭제 실패", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * [NEW] 게시글 숨기기 (남의 글)
     */
    fun hidePost(postId: String) {
        // 1) 현재 화면 리스트에서 해당 Post 객체를 찾아 임시 저장
        val post = _posts.value.find { it.id == postId }
        if (post != null) {
            _recentlyHiddenPosts.value = _recentlyHiddenPosts.value + (postId to post)
        }

        // 2) 숨김 세트에 추가
        _hiddenPostIds.value = _hiddenPostIds.value + postId

        // 3) 목록에서 필터링 (UI 즉시 반영)
        _posts.value = _posts.value.filter { it.id != postId }
        android.util.Log.d("CommunityViewModel", "게시글 숨김 처리: $postId")
    }

    /**
     * [NEW] 숨김 취소(Undo)
     */
    fun undoHidePost(postId: String) {
        // 1) 숨김 세트에서 제거
        _hiddenPostIds.value = _hiddenPostIds.value - postId

        // 2) 임시 저장된 Post가 있으면 목록에 복구 (최상단에 삽입)
        val restoredPost = _recentlyHiddenPosts.value[postId]
        if (restoredPost != null) {
            _posts.value = listOf(restoredPost) + _posts.value
            // 임시 저장소에서 제거
            _recentlyHiddenPosts.value = _recentlyHiddenPosts.value - postId
            android.util.Log.d("CommunityViewModel", "숨김 복구(Undo) 처리: $postId")
        } else {
            // 만약 임시 저장이 없다면, repository의 실시간 흐름에서 곧 복구될 것임
            android.util.Log.d("CommunityViewModel", "숨김 복구 시도: 데이터가 없어 즉시 복구되지 않을 수 있음: $postId")
        }
    }

    /**
     * [NEW] 게시글 신고하기 (남의 글)
     * 중복 신고 방지 및 5회 누적 시 자동 삭제
     */
    fun reportPost(postId: String, context: Context) {
        viewModelScope.launch {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                // 1) 중복 신고 여부 확인
                val dupQuery = firestore.collection("reports")
                    .whereEqualTo("targetPostId", postId)
                    .whereEqualTo("reporterId", deviceUserId)
                    .get()
                    .await()

                if (!dupQuery.isEmpty) {
                    android.util.Log.w("CommunityViewModel", "중복 신고 차단: $postId")
                    android.widget.Toast.makeText(context, "이미 신고한 게시글입니다.", android.widget.Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val postRef = firestore.collection("posts").document(postId)

                // 2) 트랜잭션으로 신고 처리
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(postRef)

                    if (snapshot.exists()) {
                        val currentReports = snapshot.getLong("reportCount") ?: 0L
                        val newReportCount = currentReports + 1L

                        if (newReportCount >= 5L) {
                            transaction.delete(postRef)
                        } else {
                            transaction.update(postRef, "reportCount", newReportCount)
                        }

                        // 신고 내역 기록
                        val reportRef = firestore.collection("reports").document()
                        val reportData = hashMapOf<String, Any>(
                            "targetPostId" to postId,
                            "reason" to "부적절한 콘텐츠",
                            "reportedAt" to com.google.firebase.Timestamp.now(),
                            "reporterId" to deviceUserId
                        )
                        transaction.set(reportRef, reportData)
                    }

                    null
                }.await()

                android.util.Log.d("CommunityViewModel", "신고 및 카운트 증가 완료: $postId")
                android.widget.Toast.makeText(context, "신고가 접수되었습니다.", android.widget.Toast.LENGTH_SHORT).show()

                // 3) UI에서 즉시 숨김
                hidePost(postId)

            } catch (e: Exception) {
                android.util.Log.e("CommunityViewModel", "신고 실패", e)
                try { android.widget.Toast.makeText(context, "오류가 발생했습니다.", android.widget.Toast.LENGTH_SHORT).show() } catch (_: Throwable) {}
            }
        }
    }
}
