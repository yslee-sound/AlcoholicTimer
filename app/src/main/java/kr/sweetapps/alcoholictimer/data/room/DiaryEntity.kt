package kr.sweetapps.alcoholictimer.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 일기 데이터 엔티티
 * Room Database의 diary_table에 저장됩니다.
 *
 * [ROBUST] 모든 필드에 기본값 설정으로 마이그레이션 안전성 확보
 * [NEW] imageUrl 필드 추가 (2025-12-22)
 */
@Entity(tableName = "diary_table")
data class DiaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 타임스탬프 (밀리초) - 정렬 및 날짜 계산용 */
    val timestamp: Long = System.currentTimeMillis(),

    /** 화면 표시용 날짜 문자열 (예: "2023년 12월 25일") */
    val date: String = "",

    /** 기분 이모티콘 */
    val emoji: String = "😐",

    /** 일기 내용 */
    val content: String = "",

    /** 갈망 수치 (0~10) */
    val cravingLevel: Int = 0,

    /** [NEW] 사진 URL (2025-12-22) */
    val imageUrl: String = "",

    /** [NEW] 태그 타입 (diary, thanks, reflect 등) (2025-12-23) */
    val tagType: String = "diary"
)

