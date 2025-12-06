package kr.sweetapps.alcoholictimer.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 일기 데이터 접근 객체 (DAO)
 * Room Database에 접근하는 모든 쿼리를 정의합니다.
 */
@Dao
interface DiaryDao {

    /**
     * 모든 일기를 최신순으로 가져옵니다.
     * Flow를 사용하여 실시간으로 데이터 변경사항을 감지합니다.
     */
    @Query("SELECT * FROM diary_table ORDER BY timestamp DESC")
    fun getAllDiaries(): Flow<List<DiaryEntity>>

    /**
     * 일기를 추가하거나 수정합니다.
     * 같은 ID가 있으면 교체(REPLACE)합니다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiary(diary: DiaryEntity)

    /**
     * 일기를 삭제합니다.
     */
    @Delete
    suspend fun deleteDiary(diary: DiaryEntity)

    /**
     * 특정 ID로 일기를 조회합니다.
     */
    @Query("SELECT * FROM diary_table WHERE id = :id")
    suspend fun getDiaryById(id: Long): DiaryEntity?

    /**
     * [추가] 모든 일기를 삭제합니다 (테스트/디버그용).
     */
    @Query("DELETE FROM diary_table")
    suspend fun deleteAllDiaries()
}

