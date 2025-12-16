package kr.sweetapps.alcoholictimer.ui.tab_04.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.data.model.Post
import kr.sweetapps.alcoholictimer.data.repository.CommunityRepository

/**
 * Phase 2: 커뮤니티 ViewModel
 * Firestore posts 데이터를 실시간으로 관리
 */
class CommunityViewModel : ViewModel() {
    private val repository = CommunityRepository()

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
}

