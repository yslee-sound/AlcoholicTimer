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
     */
    fun saveDiary(emoji: String, content: String, cravingLevel: Int) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
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
     */
    fun updateDiary(id: Long, emoji: String, content: String, cravingLevel: Int) {
        viewModelScope.launch {
            // 기존 일기 데이터 가져오기
            val existingDiary = repository.getDiaryById(id)
            if (existingDiary != null) {
                // timestamp와 date는 유지하고 내용만 수정
                val updatedDiary = existingDiary.copy(
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
     * 예: "2025년 12월 6일"
     */
    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN)
        return sdf.format(Date(timestamp))
    }
}

