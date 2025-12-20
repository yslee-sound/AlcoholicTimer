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
 *
 * (v3.0) 게시글 관리 기능:
 * - authorId: 작성자 기기 고유 ID (소유권 식별용)
 */
data class Post(
    @DocumentId
    val id: String = "",
    val nickname: String = "",
    val timerDuration: String = "", // "72시간" 형식
    val content: String = "",
    val imageUrl: String? = null, // 이미지 선택사항
    val likeCount: Int = 0,
    // [NEW] 좋아요한 사용자 ID 목록 (MVP: 기본값 빈 리스트)
    val likedBy: List<String> = emptyList(),
    // [NEW] 작성 시점의 현재 금주 일수 (예: 64일차)
    val currentDays: Int = 1,
    // [NEW] 작성 시점의 사용자 레벨(1~5)
    val userLevel: Int = 1,
    val createdAt: Timestamp = Timestamp.now(),
    val deleteAt: Timestamp = Timestamp.now(), // 24시간 후 삭제 예정 시간

    // [NEW] 아바타 시스템
    @PropertyName("authorAvatarIndex")
    val authorAvatarIndex: Int = 0, // 기본값 0 (avatar_00) - 하위 호환성 확보

    // [NEW] Phase 3: 게시글 관리 (소유권 식별)
    @PropertyName("authorId")
    val authorId: String = "", // 기본값 "" - 하위 호환성 확보

    // [NEW] 게시글 주제 태그 (예: "diary", "thanks", "reflect")
    @PropertyName("tagType")
    val tagType: String = ""
)
