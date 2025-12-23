package kr.sweetapps.alcoholictimer.data.repository

import kotlinx.coroutines.flow.Flow
import kr.sweetapps.alcoholictimer.data.room.DiaryDao
import kr.sweetapps.alcoholictimer.data.room.DiaryEntity

/**
 * 일기 데이터 저장소 (Repository)
 * Room Database와 UI 사이의 중간 계층으로, 데이터 소스를 추상화합니다.
 */
class DiaryRepository(private val diaryDao: DiaryDao) {

    /**
     * 모든 일기를 최신순으로 실시간 관찰합니다.
     * Flow를 반환하므로 데이터 변경 시 자동으로 UI가 업데이트됩니다.
     */
    val diaryList: Flow<List<DiaryEntity>> = diaryDao.getAllDiaries()

    /**
     * 새로운 일기를 추가합니다.
     */
    suspend fun addDiary(entity: DiaryEntity) {
        diaryDao.insertDiary(entity)
    }

    /**
     * 기존 일기를 수정합니다.
     * ID가 같으면 업데이트됩니다 (OnConflictStrategy.REPLACE).
     */
    suspend fun updateDiary(entity: DiaryEntity) {
        diaryDao.insertDiary(entity)
    }

    /**
     * 일기를 삭제합니다.
     */
    suspend fun deleteDiary(entity: DiaryEntity) {
        diaryDao.deleteDiary(entity)
    }

    /**
     * 특정 ID로 일기를 조회합니다.
     */
    suspend fun getDiaryById(id: Long): DiaryEntity? {
        return diaryDao.getDiaryById(id)
    }

    /**
     * [NEW] 모든 일기 목록을 List로 가져옵니다 (Flow가 아닌 일회성 조회)
     * 테스트 데이터 삭제 시 사용 (2025-12-23)
     */
    suspend fun getAllDiaries(): List<DiaryEntity> {
        return diaryDao.getAllDiariesList()
    }

    /**
     * [테스트용] 모든 일기를 삭제합니다.
     */
    suspend fun deleteAllDiaries() {
        diaryDao.deleteAllDiaries()
    }
}
