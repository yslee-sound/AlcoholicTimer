package kr.sweetapps.alcoholictimer.ui.tab_03.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kr.sweetapps.alcoholictimer.data.model.Post
import kr.sweetapps.alcoholictimer.data.repository.CommunityRepository
import kr.sweetapps.alcoholictimer.data.repository.UserRepository
import java.util.Locale
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

    // [NEW] 현재 사용자의 닉네임 (2025-12-22)
    private val _currentNickname = MutableStateFlow("")
    val currentNickname: StateFlow<String> = _currentNickname.asStateFlow()

    // [NEW] 선택된 이미지 URI (미리보기용) (2025-12-19)
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    // [NEW] Phase 3: 숨긴 게시글 ID 목록 (메모리 저장)
    private val _hiddenPostIds = MutableStateFlow<Set<String>>(emptySet())

    // [NEW] 최근 숨긴 게시글 임시 저장 (Undo 지원용)
    private val _recentlyHiddenPosts = MutableStateFlow<Map<String, Post>>(emptyMap())

    // [NEW] 원본 데이터 캐시 (DB에서 온 것 그대로 보관)
    private var _cachedPostList: List<Post> = emptyList()

    // [NEW] 외부(일기)에서 공유된 초안 내용
    private val _sharedDraftContent = MutableStateFlow<String?>(null)
    val sharedDraftContent: StateFlow<String?> = _sharedDraftContent.asStateFlow()

    init {
        // Load posts with device language filter by default
        val deviceLang = normalizeLanguage(Locale.getDefault().language)
        setLanguageFilter(deviceLang)

        // [MODIFIED] 통합된 동기화 함수 호출 (2025-12-22)
        startUserInfoSync()
    }

    // Helper to normalize language codes (handle old 'in' -> 'id')
    private fun normalizeLanguage(lang: String?): String {
        if (lang.isNullOrBlank()) return "en"
        return when (lang.lowercase().trim()) {
            "in" -> "id"
            else -> lang.lowercase().trim()
        }
    }

    private var postsJob: Job? = null

    // Public API: set language filter for posts. If lang == null -> all languages
    fun setLanguageFilter(lang: String?) {
        postsJob?.cancel()
        val normalized = lang?.let { normalizeLanguage(it) }
        postsJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getPosts(normalized, includeEnglishFallback = true).collect { postList ->
                    _cachedPostList = postList
                    executeFiltering()
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                android.util.Log.e("CommunityViewModel", "setLanguageFilter error", e)
                _isLoading.value = false
            }
        }
    }

    /**
     * NEW Phase 3: 이 게시글이 내 글인지 확인
     */
    fun isMyPost(post: Post): Boolean {
        return post.authorId == deviceUserId
    }

    /**
     * NEW 이미지 선택 시 호출 (2025-12-19)
     */
    fun onImageSelected(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    /**
     * [NEW] 선택된 이미지 초기화 (2025-12-23)
     */
    fun clearSelectedImage() {
        _selectedImageUri.value = null
    }

    /**
     * [NEW] 통합 동기화 함수 (2025-12-22)
     * - 아바타와 닉네임을 주기적으로(1초마다) 동기화하여 변경사항 실시간 반영
     * - 기존 loadCurrentUserAvatar()와 loadUserInfo() 통합
     */
    private fun startUserInfoSync() {
        viewModelScope.launch {
            // 초기 실행 (딜레이 없이 즉시 로드)
            syncUserData()

            // 주기적 폴링 (1초마다 변경사항 감지)
            while (true) {
                kotlinx.coroutines.delay(1000)
                syncUserData()
            }
        }
    }

    /**
     * [NEW] 실제 데이터 확인 및 업데이트 로직 (2025-12-22)
     * - 아바타 인덱스와 닉네임을 UserRepository에서 읽어와 변경사항이 있으면 상태 업데이트
     */
    private suspend fun syncUserData() {
        try {
            // 1. 아바타 동기화
            val newAvatarIndex = try {
                userRepository.getAvatarIndex()
            } catch (e: Exception) {
                0
            }

            if (newAvatarIndex != _currentUserAvatarIndex.value) {
                _currentUserAvatarIndex.value = newAvatarIndex
                android.util.Log.d("CommunityViewModel", "아바타 변경 감지 및 업데이트: $newAvatarIndex")
            }

            // 2. 닉네임 동기화
            var nickname = userRepository.getNickname()

            // 닉네임이 없으면 생성 (신규 유저 케이스)
            if (nickname.isNullOrBlank()) {
                val newNickname = try {
                    kr.sweetapps.alcoholictimer.util.NicknameGenerator.generateRandomNickname()
                } catch (e: Exception) {
                    "Unknown User"
                }
                userRepository.saveNickname(newNickname)
                nickname = newNickname
                android.util.Log.d("CommunityViewModel", "신규 닉네임 생성: $nickname")
            }

            // [핵심] 변경 사항이 있을 때만 StateFlow 업데이트
            if (nickname != _currentNickname.value) {
                _currentNickname.value = nickname
                android.util.Log.d("CommunityViewModel", "닉네임 변경 감지 및 업데이트: $nickname")
            }

        } catch (e: Exception) {
            android.util.Log.e("CommunityViewModel", "UserData Sync Failed", e)
        }
    }

    /**
     * Firestore에서 게시글 실시간 구독
     * NEW Phase 3: 숨긴 게시글 + 만료된 게시글 필터링
     */
    private fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getPosts().collect { postList ->
                // 1) 원본 데이터 최신화 (캐시)
                _cachedPostList = postList

                // 2) 필터링 실행 (로그 포함)
                executeFiltering()

                _isLoading.value = false
            }
        }
    }

    /**
     * CORE 필터링 로직 분리
     * DB 변경 시에도 불리고, 당겨서 새로고침(시간 경과) 시에도 불립니다.
     */
    private fun executeFiltering() {
        val currentTimeMillis = System.currentTimeMillis()
        android.util.Log.d("PostFilterDebug", "=== 필터링 실행! 기준 시간: $currentTimeMillis ===")

        val filteredList = _cachedPostList.filter { post ->
            val isHidden = _hiddenPostIds.value.contains(post.id)
            val deleteTimeMillis = (post.deleteAt?.seconds ?: Long.MAX_VALUE) * 1000
            val isExpired = deleteTimeMillis <= currentTimeMillis

            try {
                android.util.Log.d(
                    "PostFilterDebug",
                    "글ID: ${post.id.take(5)}... | 만료시간: $deleteTimeMillis | 현재시간: $currentTimeMillis | 만료됨?: $isExpired | 숨김됨?: $isHidden -> 최종결과: ${if (!isHidden && !isExpired) "보여줌(O)" else "숨김(X)"}"
                )
            } catch (e: Exception) {
                android.util.Log.e("PostFilterDebug", "로그 출력 중 오류: ${e.message}")
            }

            !isHidden && !isExpired
        }

        android.util.Log.d("PostFilterDebug", "=== 결과: ${_cachedPostList.size}개 -> ${filteredList.size}개 ===")
        _posts.value = filteredList
    }

    /**
     * NEW Pull-to-Refresh: 게시글 새로고침 (2025-12-20)
     */
    fun refreshPosts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // 시간 기반 필터링을 강제로 재실행하여 만료된 글을 제거
                executeFiltering()
                kotlinx.coroutines.delay(1000) // UX용 최소 표시 시간 (1초로 늘림)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // [NEW] 테스트/디버그용: 더미 게시글 생성 트리거
    // View에서 호출할 수 있도록 public 함수로 노출합니다.
    fun generateMockData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val deviceLang = normalizeLanguage(Locale.getDefault().language)
                val result = repository.generateDummyPosts(targetLanguage = deviceLang)
                if (result.isFailure) {
                    android.util.Log.e("CommunityViewModel", "generateMockData failed: ${result.exceptionOrNull()?.message}")
                } else {
                    // 새로 생성된 더미 데이터를 반영하도록 필터링 실행
                    executeFiltering()
                }
            } catch (e: Exception) {
                android.util.Log.e("CommunityViewModel", "generateMockData exception", e)
            } finally {
                _isLoading.value = false
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
    fun addPost(content: String, context: Context, tagType: String = "", thirstLevel: Int? = null, onSuccess: () -> Unit = {}) {
        // Immediately mark loading so UI can reflect spinner synchronously
        _isLoading.value = true

        // [MODIFIED] 현재 선택된 URI를 로컬 변수에 캡처만 하고 즉시 지우지 않음 (2025-12-22)
        // 업로드가 끝난 후 finally 블록에서 초기화하여 로딩 중에도 미리보기 유지
        val uriToUpload = _selectedImageUri.value

        viewModelScope.launch {
            try {
                var imageUrl: String? = null

                // 1. 이미지가 있다면 압축 후 Firebase Storage 업로드 (use copied uri)
                val currentUri = uriToUpload
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

                // 6. 일차 및 레벨 계산: timer_prefs에서 실제 타이머 시작 시간 읽기
                val timerPrefs = context.getSharedPreferences("timer_prefs", android.content.Context.MODE_PRIVATE)
                val startTime = timerPrefs.getLong("start_time", 0L) // 저장된 시작 시간이 없으면 0
                val diffMillis = if (startTime == 0L) 0L else now - startTime
                val days = if (startTime == 0L) 1 else (diffMillis / (1000L * 60L * 60L * 24L)).toInt() + 1

                // 레벨 공식: (일수 / 10) + 1  (예: 64일 -> 6 + 1 = Lv.7)
                val level = (days / 10) + 1

                // 7. Post 객체 생성 (이미지 URL 포함, 일차/레벨 포함)
                val deviceLang = normalizeLanguage(Locale.getDefault().language)
                val post = Post(
                    nickname = nickname,
                    timerDuration = timerDuration.toString(), // [FIX] Long -> String 변환
                    content = content,
                    imageUrl = imageUrl, // 업로드된 URL 또는 null
                    likeCount = 0,
                    likedBy = emptyList(),
                    currentDays = days,
                    userLevel = level,
                    createdAt = createdAt,
                    deleteAt = deleteAt,
                    authorAvatarIndex = avatarIndex,
                    authorId = deviceUserId, // [NEW] Phase 3: 작성자 기기 ID
                    tagType = tagType,
                    thirstLevel = thirstLevel
                    ,languageCode = deviceLang
                )

                // 8. Firestore에 게시글 추가
                repository.addPost(post)

                // 9. 성공 콜백 호출 (UI 쪽에서 창 닫기 등 후속 처리 담당)
                try {
                    onSuccess()
                } catch (e: Exception) {
                    android.util.Log.w("CommunityViewModel", "onSuccess callback failed: ${e.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("CommunityViewModel", "게시글 작성 실패", e)
            } finally {
                // [MODIFIED] 모든 작업이 끝난 후에 이미지 초기화 (성공/실패 무관) (2025-12-22)
                _selectedImageUri.value = null
                _isLoading.value = false
            }
        }
    }

    /**
     * [NEW] 게시글 수정 (2025-12-22)
     * @param postId 수정할 게시글 ID
     * @param newContent 새로운 내용
     * @param newTagType 새로운 태그 타입
     * @param newThirstLevel 새로운 갈증 수치
     * @param onSuccess 성공 콜백
     */
    fun updatePost(
        postId: String,
        newContent: String,
        newTagType: String = "",
        newThirstLevel: Int? = null,
        onSuccess: () -> Unit = {}
    ) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Firestore 업데이트
                val updates = mutableMapOf<String, Any?>(
                    "content" to newContent,
                    "tagType" to newTagType,
                    "thirstLevel" to newThirstLevel
                )

                repository.updatePost(postId, updates)

                android.util.Log.d("CommunityViewModel", "게시글 수정 완료: $postId")

                // 성공 콜백 호출
                try {
                    onSuccess()
                } catch (e: Exception) {
                    android.util.Log.w("CommunityViewModel", "onSuccess callback failed: ${e.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("CommunityViewModel", "게시글 수정 실패", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * NEW 타이머 지속 시간 계산 (랜덤 또는 실제 유저 타이머)
     * - Phase 3: 실제 유저 타이머 기반으로 변경 (2025-12-20)
     */
    private fun calculateTimerDuration(): Long {
        // 1. SharedPreferences에서 실제 타이머 값 읽기
        val prefs = getApplication<Application>().getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        val timerValue = prefs.getLong("timer_value", 24 * 60 * 60 * 1000) // 기본값: 24시간

        // 2. 최대 2배까지 랜덤 범위 설정 (예: 12시간 ~ 48시간)
        val minValue = (timerValue * 0.5).toLong()
        val maxValue = (timerValue * 2).toLong()

        // 3. 랜덤 지속 시간 생성
        return (minValue..maxValue).random()
    }

    /**
     * NEW 게시글 숨기기 (Phase 3 추가 기능)
     * - 로컬 상태 업데이트 후, Firestore에서도 숨김 처리
     */
    fun hidePost(post: Post) {
        viewModelScope.launch {
            // 1) 로컬 상태 업데이트
            val updatedHiddenIds = _hiddenPostIds.value + post.id
            _hiddenPostIds.value = updatedHiddenIds

            // 2) Firestore에서 해당 게시글 숨기기 (비동기)
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val postRef = firestore.collection("posts").document(post.id)

            try {
                // 숨김 처리
                postRef.update("deleteAt", com.google.firebase.Timestamp.now()).await()

                // 최근 숨긴 게시글 목록에 추가 (Undo 지원)
                _recentlyHiddenPosts.value = _recentlyHiddenPosts.value + (post.id to post)
            } catch (e: Exception) {
                android.util.Log.e("CommunityViewModel", "게시글 숨기기 실패", e)
                // 롤백: 로컬 상태에서만 제거 (서버 오류 시에도 사용자가 볼 수 있도록)
                _hiddenPostIds.value = _hiddenPostIds.value - post.id
            }
        }
    }

    // [NEW] 편의 오버로드: postId로 호출 가능하게 함 (기존 UI가 String id를 전달하는 곳이 있어 호환성 유지)
    fun hidePost(postId: String) {
        // 1) 캐시된 목록에서 Post 객체를 찾아 사용
        val post = _cachedPostList.find { it.id == postId }
        if (post != null) {
            hidePost(post)
        } else {
            // 캐시에 없는 경우 안전하게 Firestore에서 직접 deleteAt를 설정하도록 처리
            viewModelScope.launch {
                try {
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val postRef = firestore.collection("posts").document(postId)
                    postRef.update("deleteAt", com.google.firebase.Timestamp.now()).await()
                    // 로컬 상태에도 반영
                    _hiddenPostIds.value = _hiddenPostIds.value + postId
                } catch (e: Exception) {
                    android.util.Log.e("CommunityViewModel", "hidePost(postId) 실패", e)
                }
            }
        }
    }

    /**
     * NEW 숨기기 취소 (Undo) 기능
     * - 최근에 숨긴 게시글 목록에서 복원
     */
    fun undoHidePost(postId: String) {
        viewModelScope.launch {
            // 1) 최근 숨긴 게시글 목록에서 제거
            val post = _recentlyHiddenPosts.value[postId] ?: return@launch
            _recentlyHiddenPosts.value = _recentlyHiddenPosts.value - postId

            // 2) 로컬 숨김 ID 목록에서 제거
            _hiddenPostIds.value = _hiddenPostIds.value - postId

            // 3) Firestore에서 해당 게시글 복원 (deleteAt 필드 제거)
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val postRef = firestore.collection("posts").document(postId)

            try {
                // 복원 처리
                postRef.update("deleteAt", null).await()
            } catch (e: Exception) {
                android.util.Log.e("CommunityViewModel", "게시글 복원 실패", e)
                // 실패 시 롤백: 로컬 상태에서만 복원
                _hiddenPostIds.value = _hiddenPostIds.value + postId
            }
        }
    }

    // [NEW] 게시글 삭제(편의): postId로 호출 가능하도록 구현
    fun deletePost(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                firestore.collection("posts").document(postId).delete().await()

                // 로컬 캐시 및 표시 리스트 갱신
                _cachedPostList = _cachedPostList.filter { it.id != postId }
                executeFiltering()

                android.util.Log.d("CommunityViewModel", "게시글 삭제 완료: $postId")
            } catch (e: Exception) {
                android.util.Log.e("CommunityViewModel", "게시글 삭제 실패", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // [NEW] 게시글 신고(편의): 간단히 reports 컬렉션에 신고 기록을 남깁니다.
    fun reportPost(postId: String, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val report = hashMapOf(
                    "postId" to postId,
                    "reporterId" to deviceUserId,
                    "reporterAvatarIndex" to try { userRepository.getAvatarIndex() } catch (_: Exception) { 0 },
                    "createdAt" to com.google.firebase.Timestamp.now()
                )

                firestore.collection("reports").add(report).await()
                android.util.Log.d("CommunityViewModel", "게시글 신고 완료: $postId")
            } catch (e: Exception) {
                android.util.Log.e("CommunityViewModel", "게시글 신고 실패", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * NEW 외부에서 공유된 초안 수신 (일기 앱 등에서)
     * - 초안 내용이 변경될 때만 업데이트
     */
    fun receiveSharedDraft(content: String) {
        if (content != _sharedDraftContent.value) {
            _sharedDraftContent.value = content
            android.util.Log.d("CommunityViewModel", "공유된 초안 업데이트: $content")
        }
    }

    // [NEW] 외부에서 Draft 내용을 설정하는 편의 메서드 (MainActivity 등에서 사용)
    fun setDraftContent(content: String) {
        _sharedDraftContent.value = content
        android.util.Log.d("CommunityViewModel", "setDraftContent called: $content")
    }

    /**
     * NEW Firestore에 저장된 사용자 아바타 URL 가져오기
     * - 비동기로 Firestore에서 아바타 URL을 가져와 상태 업데이트
     */
    fun fetchUserAvatarUrl() {
        viewModelScope.launch {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val userId = deviceUserId

            try {
                // Firestore에서 사용자 문서 가져오기
                val userDoc = firestore.collection("users").document(userId).get().await()

                if (userDoc.exists()) {
                    // 아바타 URL 필드가 있으면 상태 업데이트
                    val avatarUrl = userDoc.getString("avatarUrl")
                    if (avatarUrl != null) {
                        // [FIX] 안전하게 SharedPreferences에 직접 저장 (userRepository의 메서드 대신)
                        try {
                            val prefs = getApplication<Application>().getSharedPreferences("user_settings", Context.MODE_PRIVATE)
                            prefs.edit().putString("avatar_url", avatarUrl).apply()
                            android.util.Log.d("CommunityViewModel", "아바타 URL 로컬 저장: $avatarUrl")
                        } catch (e: Exception) {
                            android.util.Log.e("CommunityViewModel", "아바타 URL 저장 실패", e)
                        }
                     }
                 }
            } catch (e: Exception) {
                android.util.Log.e("CommunityViewModel", "사용자 아바타 URL 가져오기 실패", e)
            }
        }
    }
}
