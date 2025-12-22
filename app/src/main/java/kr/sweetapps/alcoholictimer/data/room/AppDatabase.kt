package kr.sweetapps.alcoholictimer.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room Database 인스턴스
 * Singleton 패턴을 사용하여 앱 전체에서 하나의 DB 인스턴스만 사용합니다.
 *
 * [NEW] Version 2: imageUrl 필드 추가 (2025-12-22)
 */
@Database(
    entities = [DiaryEntity::class],
    version = 2, // [UPDATED] 1 -> 2 (imageUrl 필드 추가)
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * DiaryDao 인스턴스를 반환합니다.
     */
    abstract fun diaryDao(): DiaryDao

    companion object {
        // Singleton 방지: volatile로 선언하여 멀티스레드 환경에서 안전성 확보
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * [NEW] Migration 1 -> 2: imageUrl 컬럼 추가 (2025-12-22)
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // imageUrl 컬럼 추가 (기본값 빈 문자열)
                database.execSQL("ALTER TABLE diary_table ADD COLUMN imageUrl TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Database 인스턴스를 가져옵니다.
         * 없으면 새로 생성하고, 있으면 기존 인스턴스를 반환합니다.
         */
        fun getDatabase(context: Context): AppDatabase {
            // INSTANCE가 null이 아니면 반환, null이면 synchronized 블록 실행
            return INSTANCE ?: synchronized(this) {
                // synchronized 블록 내에서 다시 한 번 null 체크 (Double-Checked Locking)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "alcoholic_timer_database"
                )
                    // [NEW] Migration 추가 (2025-12-22)
                    .addMigrations(MIGRATION_1_2)

                    // [옵션] 메인 스레드에서 쿼리 허용 (개발 중에만 사용, 프로덕션에서는 제거 권장)
                    // .allowMainThreadQueries()

                    // [옵션] 마이그레이션 없이 데이터베이스 재생성 (개발 중에만 사용)
                    // .fallbackToDestructiveMigration()

                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * [테스트/디버그용] Database 인스턴스를 초기화합니다.
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}

