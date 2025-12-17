package kr.sweetapps.alcoholictimer.ui.tab_04.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.data.model.Post
import kr.sweetapps.alcoholictimer.data.repository.CommunityRepository
import kr.sweetapps.alcoholictimer.data.repository.UserRepository

/**
 * Phase 2: 커뮤니티 ViewModel
 * Firestore posts 데이터를 실시간으로 관리
 *
 * (v2.0) 아바타 시스템 추가:
 * - UserRepository를 통해 사용자의 아바타 인덱스 관리
 */
class CommunityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CommunityRepository()
    private val userRepository = UserRepository(application.applicationContext) // 아바타 관리용

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPosts()
    }

    /**
     * Firestore에서 게시글 실시간 구독
     */
    private fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getPosts().collect { postList ->
                _posts.value = postList
                _isLoading.value = false
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
     * @param content 게시글 내용
     */
    fun addPost(content: String) {
        viewModelScope.launch {
            try {
                // 익명 닉네임 랜덤 생성
                val anonymousNicknames = listOf(
                    "익명의 사자", "참는 중인 호랑이", "새벽의 독수리", "조용한 늑대",
                    "밤하늘의 별", "아침의 햇살", "강한 곰", "자유로운 독수리",
                    "평화로운 사슴", "용감한 여우"
                )
                val nickname = anonymousNicknames.random()

                // [NEW] 사용자의 현재 아바타 인덱스 가져오기
                val avatarIndex = try {
                    userRepository.getAvatarIndex()
                } catch (e: Exception) {
                    android.util.Log.e("CommunityViewModel", "아바타 인덱스 가져오기 실패, 기본값 0 사용", e)
                    0
                }

                // 타이머 지속 시간 계산 (랜덤 또는 실제 유저 타이머)
                val timerDuration = calculateTimerDuration()

                // 현재 시간
                val now = System.currentTimeMillis()
                val createdAt = com.google.firebase.Timestamp(now / 1000, 0)
                val deleteAt = com.google.firebase.Timestamp((now / 1000) + 24 * 60 * 60, 0) // 24시간 후

                // Post 객체 생성 (authorAvatarIndex 포함)
                val post = Post(
                    nickname = nickname,
                    timerDuration = timerDuration,
                    content = content,
                    imageUrl = null, // 이미지는 향후 추가
                    likeCount = 0,
                    createdAt = createdAt,
                    deleteAt = deleteAt,
                    authorAvatarIndex = avatarIndex // [NEW] 아바타 인덱스 포함
                )

                // Firestore에 저장
                repository.addPost(post)

                android.util.Log.d("CommunityViewModel", "게시글 작성 완료: $nickname (avatar: $avatarIndex)")
            } catch (e: Exception) {
                android.util.Log.e("CommunityViewModel", "게시글 작성 실패", e)
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
}
