package kr.sweetapps.alcoholictimer.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Phase 2: 커뮤니티 게시글 데이터 모델
 * Firestore 컬렉션: posts
 *
 * (v2.0) 아바타 시스템 추가:
 * - authorAvatarIndex: 작성자의 아바타 인덱스 (0~19)
 * - 기존 Firestore 데이터와의 하위 호환성 유지
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
    val deleteAt: Timestamp = Timestamp.now(), // 24시간 후 삭제 예정 시간

    // [NEW] 아바타 시스템
    @PropertyName("authorAvatarIndex")
    val authorAvatarIndex: Int = 0 // 기본값 0 (avatar_00) - 하위 호환성 확보
)

