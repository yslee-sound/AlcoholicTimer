package kr.sweetapps.alcoholictimer.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Phase 2: 커뮤니티 게시글 데이터 모델
 * Firestore 컬렉션: posts
 */
data class Post(
    @DocumentId
    val id: String = "",
    val nickname: String = "",
    val timerDuration: String = "", // "72시간" 형식
    val content: String = "",
    val imageUrl: String? = null, // 이미지 선택사항
    val likeCount: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val deleteAt: Timestamp = Timestamp.now() // 24시간 후 삭제 예정 시간
)

