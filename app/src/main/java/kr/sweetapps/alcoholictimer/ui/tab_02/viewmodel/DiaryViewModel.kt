package kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kr.sweetapps.alcoholictimer.data.repository.DiaryRepository
import kr.sweetapps.alcoholictimer.data.room.AppDatabase
import kr.sweetapps.alcoholictimer.data.room.DiaryEntity
import java.text.SimpleDateFormat
import java.util.*
import java.util.*

/**
 * 일기 화면 ViewModel
 * Room Database와 UI를 연결하는 비즈니스 로직을 담당합니다.
 */
class DiaryViewModel(application: Application) : AndroidViewModel(application) {

    // Database 인스턴스 가져오기
    private val database = AppDatabase.getDatabase(application)
    private val repository = DiaryRepository(database.diaryDao())

    /**
     * UI에서 관찰할 일기 목록 StateFlow
     * Flow를 StateFlow로 변환하여 초기값과 함께 노출합니다.
     */
    val uiState: StateFlow<List<DiaryEntity>> = repository.diaryList
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * 새로운 일기를 저장합니다.
     *
     * @param emoji 기분 이모티콘
     * @param content 일기 내용
     * @param cravingLevel 갈망 수치 (0~10)
     * @param timestamp 일기 작성 시간 (기본값: 현재 시간)
     * @param shareToChallenge 24시간 챌린지 익명 공유 여부 (Phase 3)
     */
    fun saveDiary(
        emoji: String,
        content: String,
        cravingLevel: Int,
        timestamp: Long = System.currentTimeMillis(), // [NEW] 파라미터로 받도록 수정
        shareToChallenge: Boolean = false // [NEW Phase 3] 챌린지 공유 플래그
    ) {
        viewModelScope.launch {
            val dateString = formatDate(timestamp)

            val diary = DiaryEntity(
                timestamp = timestamp,
                date = dateString,
                emoji = emoji,
                content = content,
                cravingLevel = cravingLevel
            )

            repository.addDiary(diary)

            // [NEW Phase 3] 챌린지 공유가 체크되어 있으면 Firestore posts에도 저장
            if (shareToChallenge) {
                saveToChallenge(emoji, content, timestamp)
            }
        }
    }

    /**
     * [NEW Phase 3] 일기를 24시간 챌린지(Firestore posts)에 익명으로 공유
     */
    private suspend fun saveToChallenge(
        emoji: String,
        content: String,
        timestamp: Long
    ) {
        try {
            val communityRepository = kr.sweetapps.alcoholictimer.data.repository.CommunityRepository()

            // 익명 닉네임 생성 (랜덤)
            val anonymousNicknames = listOf(
                "익명의 사자", "참는 중인 호랑이", "새벽의 독수리", "조용한 늑대",
                "밤하늘의 별", "아침의 햇살", "강한 곰", "자유로운 독수리",
                "평화로운 사슴", "용감한 여우"
            )
            val nickname = anonymousNicknames.random()

            // 타이머 지속 시간 계산 (현재 진행 중인 타이머 기준)
            val timerDuration = calculateTimerDuration()

            val post = kr.sweetapps.alcoholictimer.data.model.Post(
                nickname = nickname,
                timerDuration = timerDuration,
                content = "$emoji $content", // 이모지 + 내용
                imageUrl = null, // Phase 3: 이미지는 별도 처리 예정
                likeCount = 0,
                createdAt = com.google.firebase.Timestamp(timestamp / 1000, 0),
                deleteAt = com.google.firebase.Timestamp((timestamp / 1000) + 24 * 60 * 60, 0) // 24시간 후
            )

            // Firestore에 저장
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            firestore.collection("posts").add(post).await()

            android.util.Log.d("DiaryViewModel", "일기가 24시간 챌린지에 공유되었습니다: $nickname")
        } catch (e: Exception) {
            android.util.Log.e("DiaryViewModel", "챌린지 공유 실패", e)
        }
    }

    /**
     * [NEW Phase 3] 현재 진행 중인 타이머의 지속 시간 계산
     */
    private fun calculateTimerDuration(): String {
        val prefs = getApplication<Application>().getSharedPreferences("timer_prefs", 0)
        val startTime = prefs.getLong("start_time", 0L)

        if (startTime == 0L) {
            return "0시간"
        }

        val now = System.currentTimeMillis()
        val duration = now - startTime
        val hours = (duration / (1000 * 60 * 60)).toInt()

        return "${hours}시간"
    }

    /**
     * 기존 일기를 수정합니다.
     *
     * @param id 일기 ID
     * @param emoji 기분 이모티콘
     * @param content 일기 내용
     * @param cravingLevel 갈망 수치 (0~10)
     * @param timestamp 일기 작성 시간 (선택사항, 지정하지 않으면 기존 값 유지)
     */
    fun updateDiary(
        id: Long,
        emoji: String,
        content: String,
        cravingLevel: Int,
        timestamp: Long? = null // [NEW] 파라미터로 받도록 수정
    ) {
        viewModelScope.launch {
            // 기존 일기 데이터 가져오기
            val existingDiary = repository.getDiaryById(id)
            if (existingDiary != null) {
                // [NEW] timestamp가 변경되었으면 date도 업데이트
                val newTimestamp = timestamp ?: existingDiary.timestamp
                val newDate = formatDate(newTimestamp)

                val updatedDiary = existingDiary.copy(
                    timestamp = newTimestamp,
                    date = newDate,
                    emoji = emoji,
                    content = content,
                    cravingLevel = cravingLevel
                )
                repository.updateDiary(updatedDiary)
            }
        }
    }

    /**
     * 일기를 삭제합니다.
     *
     * @param id 일기 ID
     */
    fun deleteDiary(id: Long) {
        viewModelScope.launch {
            val diary = repository.getDiaryById(id)
            if (diary != null) {
                repository.deleteDiary(diary)
            }
        }
    }

    /**
     * 특정 ID의 일기를 조회합니다.
     *
     * @param id 일기 ID
     * @return DiaryEntity 또는 null
     */
    suspend fun getDiaryById(id: Long): DiaryEntity? {
        return repository.getDiaryById(id)
    }

    /**
     * timestamp를 날짜 문자열로 변환합니다.
     * 현재 시스템 Locale에 맞춰 날짜를 표시합니다.
     * 예: 한국어 - "2025년 12월 6일"
     *     일본어 - "2025年12月6日"
     *     영어 - "Dec 6, 2025"
     */
    private fun formatDate(timestamp: Long): String {
        // [다국어화] Locale.getDefault()를 사용하여 시스템 언어에 맞는 날짜 형식 사용
        val locale = Locale.getDefault()
        val sdf = when (locale.language) {
            "ko" -> SimpleDateFormat("yyyy년 M월 d일", locale)
            "ja" -> SimpleDateFormat("yyyy年M月d日", locale)
            "zh" -> SimpleDateFormat("yyyy年M月d日", locale)
            "es" -> SimpleDateFormat("d 'de' MMMM 'de' yyyy", locale)
            else -> SimpleDateFormat("MMM d, yyyy", locale) // 영어 및 기타 언어
        }
        return sdf.format(Date(timestamp))
    }
}

