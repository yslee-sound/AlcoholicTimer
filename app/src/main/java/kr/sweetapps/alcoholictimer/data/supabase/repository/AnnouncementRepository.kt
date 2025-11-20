package kr.sweetapps.alcoholictimer.data.supabase.repository

import kr.sweetapps.alcoholictimer.data.supabase.model.Announcement

/** Stub AnnouncementRepository - returns empty lists/nulls while Supabase is removed */
class AnnouncementRepository {
    /**
     * 모든 활성 공지사항 조회 (서버 필터 사용)
     *
     * @return 최신순으로 정렬된 공지사항 리스트
     */
    suspend fun getAnnouncements(): Result<List<Announcement>> = runCatching { emptyList<Announcement>() }

    /**
     * 최신 emergency 공지사항 1건 조회 (있으면 반환)
     *
     * @return 가장 최근 emergency 공지사항, 없으면 null
     */
    suspend fun getActiveEmergency(): Result<Announcement?> = runCatching { null }

    /**
     * 최신 공지사항 1개 조회 (emergency 제외)
     *
     * @return 가장 최근 공지사항, 없으면 null
     */
    suspend fun getLatestAnnouncement(): Result<Announcement?> = runCatching { null }

    /**
     * 특정 ID의 공지사항 조회
     *
     * @param id 공지사항 ID
     * @return 해당 공지사항, 없으면 null
     */
    suspend fun getAnnouncementById(id: Long): Result<Announcement?> = runCatching { null }

    /**
     * 특정 개수만큼 공지사항 조회 (emergency 제외)
     *
     * @param limit 조회할 개수
     * @return 최신순으로 정렬된 공지사항 리스트
     */
    suspend fun getRecentAnnouncements(limit: Int = 5): Result<List<Announcement>> = runCatching { emptyList<Announcement>() }
}
