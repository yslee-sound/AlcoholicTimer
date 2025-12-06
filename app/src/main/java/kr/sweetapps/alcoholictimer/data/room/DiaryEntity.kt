package kr.sweetapps.alcoholictimer.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 일기 데이터 엔티티
 * Room Database의 diary_table에 저장됩니다.
 */
@Entity(tableName = "diary_table")
data class DiaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 타임스탬프 (밀리초) - 정렬 및 날짜 계산용 */
    val timestamp: Long,

    /** 화면 표시용 날짜 문자열 (예: "2023년 12월 25일") */
    val date: String,

    /** 기분 이모티콘 */
    val emoji: String,

    /** 일기 내용 */
    val content: String,

    /** 갈망 수치 (0~10) */
    val cravingLevel: Int
)

