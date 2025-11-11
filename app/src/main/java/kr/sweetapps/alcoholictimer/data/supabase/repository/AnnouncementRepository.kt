package kr.sweetapps.alcoholictimer.data.supabase.repository

import kr.sweetapps.alcoholictimer.data.supabase.model.Announcement
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/**
 * 공지사항 Repository
 *
 * Supabase의 announcements 테이블과 통신하여
 * 공지사항 데이터를 조회하는 역할을 담당합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val repository = AnnouncementRepository(supabase, "alcoholictimer")
 * val announcements = repository.getAnnouncements()
 * ```
 */
class AnnouncementRepository(
    private val client: SupabaseClient,
    private val appId: String = "alcoholictimer"
) {
    /**
     * 모든 활성 공지사항 조회 (서버 필터 사용)
     *
     * @return 최신순으로 정렬된 공지사항 리스트
     */
    suspend fun getAnnouncements(): Result<List<Announcement>> = runCatching {
        client.from("announcements")
            .select()
            .decodeList<Announcement>()
            .filter { it.appId == appId && it.isActive }
            .sortedByDescending { it.createdAt }
    }

    /**
     * 최신 emergency 공지사항 1건 조회 (있으면 반환)
     *
     * @return 가장 최근 emergency 공지사항, 없으면 null
     */
    suspend fun getActiveEmergency(): Result<Announcement?> = runCatching {
        client.from("announcements")
            .select()
            .decodeList<Announcement>()
            .filter { it.appId == appId && it.isActive && it.isEmergency }
            .maxByOrNull { it.createdAt ?: "" }
    }

    /**
     * 최신 공지사항 1개 조회 (emergency 제외)
     *
     * @return 가장 최근 공지사항, 없으면 null
     */
    suspend fun getLatestAnnouncement(): Result<Announcement?> = runCatching {
        client.from("announcements")
            .select()
            .decodeList<Announcement>()
            .filter { it.appId == appId && it.isActive && !it.isEmergency }
            .maxByOrNull { it.createdAt ?: "" }
    }

    /**
     * 특정 ID의 공지사항 조회
     *
     * @param id 공지사항 ID
     * @return 해당 공지사항, 없으면 null
     */
    suspend fun getAnnouncementById(id: Long): Result<Announcement?> = runCatching {
        client.from("announcements")
            .select()
            .decodeList<Announcement>()
            .find { it.id == id && it.appId == appId }
    }

    /**
     * 특정 개수만큼 공지사항 조회 (emergency 제외)
     *
     * @param limit 조회할 개수
     * @return 최신순으로 정렬된 공지사항 리스트
     */
    suspend fun getRecentAnnouncements(limit: Int = 5): Result<List<Announcement>> = runCatching {
        client.from("announcements")
            .select()
            .decodeList<Announcement>()
            .filter { it.appId == appId && it.isActive && !it.isEmergency }
            .sortedByDescending { it.createdAt }
            .take(limit)
    }
}
