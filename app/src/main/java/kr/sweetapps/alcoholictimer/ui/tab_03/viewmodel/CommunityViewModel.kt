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
     * [NEW] Phase 3: 숨긴 게시글 필터링
     */
    private fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getPosts().collect { postList ->
                // [NEW] 숨긴 게시글 제외
                _posts.value = postList.filter { post ->
                    !_hiddenPostIds.value.contains(post.id)
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
     * 좋아요 토글 (Phase 2: UI만, 실제 저장은 Phase 3)
     */
    fun toggleLike(postId: String) {
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) {
                // Phase 2: 로컬 상태만 변경 (Phase 3에서 Firestore 업데이트 추가)
                post.copy(likeCount = post.likeCount + 1)
            } else {
                post
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
        _hiddenPostIds.value = _hiddenPostIds.value + postId
        // 목록에서 필터링 (loadPosts의 filter 로직이 자동 적용)
        _posts.value = _posts.value.filter { it.id != postId }
        android.util.Log.d("CommunityViewModel", "게시글 숨김 처리: $postId")
    }

    /**
     * [NEW] 게시글 신고하기 (남의 글)
     */
    fun reportPost(postId: String) {
        viewModelScope.launch {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val reportData = hashMapOf(
                    "targetPostId" to postId,
                    "reason" to "부적절한 콘텐츠",
                    "reportedAt" to com.google.firebase.Timestamp.now(),
                    "reporterId" to deviceUserId
                )

                firestore.collection("reports").add(reportData).await()
                android.util.Log.d("CommunityViewModel", "신고 접수 완료: $postId")

                // 신고 후 자동으로 숨기기
                hidePost(postId)
            } catch (e: Exception) {
                android.util.Log.e("CommunityViewModel", "신고 실패", e)
            }
        }
    }
}
