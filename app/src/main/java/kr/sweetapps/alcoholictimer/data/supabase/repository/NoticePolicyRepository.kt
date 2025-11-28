package kr.sweetapps.alcoholictimer.data.supabase.repository

import android.content.Context
import kr.sweetapps.alcoholictimer.data.supabase.model.Announcement

/**
 * NoticePolicyRepository: notice_policy 관련 간단 래퍼.
 */
class NoticePolicyRepository(private val context: Context) {
    private val announcementRepository = AnnouncementRepository(context)

    suspend fun getLatestActiveAnnouncement(): Announcement? {
        return try {
            announcementRepository.getLatestAnnouncement().getOrNull()
        } catch (_: Exception) {
            null
        }
    }
}
