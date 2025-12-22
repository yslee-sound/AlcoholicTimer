package kr.sweetapps.alcoholictimer.ui.tab_02.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kr.sweetapps.alcoholictimer.data.repository.DiaryRepository
import kr.sweetapps.alcoholictimer.data.room.AppDatabase
import kr.sweetapps.alcoholictimer.data.room.DiaryEntity
import java.text.SimpleDateFormat
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
     */
    fun saveDiary(
        emoji: String,
        content: String,
        cravingLevel: Int,
        timestamp: Long = System.currentTimeMillis()
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
        }
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
        timestamp: Long? = null
    ) {
        viewModelScope.launch {
            val existingDiary = repository.getDiaryById(id)
            if (existingDiary != null) {
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
     * [NEW] 새로운 일기를 저장합니다 (DiaryEntity 직접 받기)
     * WritePostScreenContent와의 통합을 위해 추가 (2025-12-22)
     */
    fun insertDiary(diary: DiaryEntity) {
        viewModelScope.launch {
            repository.addDiary(diary)
        }
    }

    /**
     * [NEW] 기존 일기를 수정합니다 (DiaryEntity 직접 받기)
     * WritePostScreenContent와의 통합을 위해 추가 (2025-12-22)
     */
    fun updateDiary(diary: DiaryEntity) {
        viewModelScope.launch {
            repository.updateDiary(diary)
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
     * [NEW] 특정 날짜의 일기를 조회합니다 (캘린더용) (2025-12-22)
     * @param date LocalDate 객체
     * @return 해당 날짜의 DiaryEntity 또는 null
     */
    suspend fun getDiaryByDate(date: java.time.LocalDate): DiaryEntity? {
        val startOfDay = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        return uiState.value.firstOrNull { diary ->
            diary.timestamp in startOfDay until endOfDay
        }
    }

    /**
     * [NEW] 일기 데이터를 날짜별 Map으로 변환 (캘린더용) (2025-12-22)
     * @return Map<String, DiaryEntity> (Key: "yyyy-MM-dd")
     */
    fun getDiaryMapByDate(): Map<String, DiaryEntity> {
        return uiState.value.associateBy { diary ->
            val date = java.time.Instant.ofEpochMilli(diary.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        }
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

    /**
     * [NEW] 테스트용 랜덤 일기 데이터 생성 (사진 포함) (2025-12-22)
     * - 약 40%의 확률로 사진 URL 포함
     * - 다양한 갈증 수치와 내용으로 UI 테스트 용이
     */
    fun generateMockDiaries() {
        viewModelScope.launch {
            val random = java.util.Random()
            val contents = listOf(
                "오늘 날씨가 너무 좋아서 사진 한 장 찍어봤어요! ☀️",
                "술 대신 맛있는 안주만 먹고 왔습니다. 사진 보니까 또 먹고 싶네요. 🍜",
                "운동 끝나고 오니 개운하네요. 금주 5일차! 💪",
                "사진은 없지만 오늘 정말 보람찬 하루였습니다.",
                "조금 힘들었지만 잘 참아낸 나 자신, 칭찬해! 👏",
                "친구들과 즐거운 시간을 보냈어요. 술 없어도 재밌네요! 🎉",
                "오늘은 좀 갈증이 심했지만 버텨냈습니다.",
                "맛있는 저녁 먹고 산책했어요. 기분 좋은 하루! 🌙",
                "일기 쓰는 습관이 들어가고 있어요. 뿌듯해요!",
                "오늘도 무사히 하루를 마무리합니다. 감사합니다. 🙏"
            )

            val emojis = listOf("📝", "✅", "🌟", "💧", "💪", "😊", "🎯", "🔥", "✨", "🌈")

            repeat(10) { index ->
                // 0~364일 전의 랜덤 날짜 생성
                val randomDaysAgo = random.nextInt(365)
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -randomDaysAgo)
                val timestamp = cal.timeInMillis

                // 40% 확률로 사진 포함, 나머지는 빈 문자열
                val mockImageUrl = if (random.nextFloat() < 0.4f) {
                    // Picsum Photos API를 사용하여 랜덤 테스트 이미지 생성
                    "https://picsum.photos/seed/${random.nextInt(1000)}/400/300"
                } else {
                    ""
                }

                // 10% 확률로 갈증 수치 0 (미입력), 나머지는 1~10
                val cravingLevel = if (random.nextFloat() < 0.1f) {
                    0
                } else {
                    random.nextInt(10) + 1
                }

                val mockDiary = DiaryEntity(
                    timestamp = timestamp,
                    date = formatDate(timestamp),
                    emoji = emojis.random(),
                    content = contents.random(),
                    cravingLevel = cravingLevel,
                    imageUrl = mockImageUrl // [NEW] 랜덤 사진 URL 포함
                )

                repository.addDiary(mockDiary)
            }

            android.util.Log.d("DiaryViewModel", "✅ 테스트용 일기 10개 생성 완료 (사진 포함 비율: 40%)")
        }
    }
}
