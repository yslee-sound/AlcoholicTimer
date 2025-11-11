package kr.sweetapps.alcoholictimer.data.supabase.repository

import kr.sweetapps.alcoholictimer.data.supabase.model.UpdateInfo
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlin.collections.filter
import kotlin.collections.find
import kotlin.collections.maxByOrNull
import kotlin.collections.sortedByDescending

/**
 * 앱 버전 정보 Repository
 *
 * Supabase의 update_info 테이블과 통신하여
 * 앱 업데이트 관련 정보를 조회하는 역할을 담당합니다.
 */
class UpdateInfoRepository(
    private val client: SupabaseClient,
    private val appId: String = "alcoholictimer"
) {
    private fun List<UpdateInfo>.activeOnly(): List<UpdateInfo> = this.filter { it.isActive }

    /**
     * 최신 버전 정보 조회 (해당 appId 한정)
     */
    suspend fun getLatestVersion(): Result<UpdateInfo?> = runCatching {
        client.from("update_info")
            .select()
            .decodeList<UpdateInfo>()
            .filter { it.appId == appId }
            .activeOnly()
            .maxByOrNull { it.versionCode }
    }

    /**
     * 최신 버전 정보 조회 (명시적 이름) - 내부 재사용 가능
     */
    suspend fun getLatestForApp(): Result<UpdateInfo?> = getLatestVersion()

    /**
     * 업데이트 필요 여부 확인 (현재 버전보다 높은 최신 버전이 있으면 그 객체 반환)
     */
    suspend fun checkUpdateRequired(currentVersionCode: Int): Result<UpdateInfo?> = runCatching {
        val list = client.from("update_info").select().decodeList<UpdateInfo>()
            .filter { it.appId == appId }
            .activeOnly()
        val latest = list.maxByOrNull { it.versionCode }
        if (latest != null && latest.versionCode > currentVersionCode) latest else null
    }

    /**
     * 강제 업데이트 필요 여부 확인 (버전 상승 + isForce)
     */
    suspend fun isForceUpdateRequired(currentVersionCode: Int): Result<Boolean> = runCatching {
        val latest = client.from("update_info").select().decodeList<UpdateInfo>()
            .filter { it.appId == appId }
            .activeOnly()
            .maxByOrNull { it.versionCode }
        latest != null && latest.versionCode > currentVersionCode && latest.isForce
    }

    /**
     * 특정 버전 코드 정보 조회
     */
    suspend fun getVersionByCode(versionCode: Int): Result<UpdateInfo?> = runCatching {
        client.from("update_info").select().decodeList<UpdateInfo>()
            .filter { it.appId == appId }
            .activeOnly()
            .find { it.versionCode == versionCode }
    }

    /**
     * 버전 히스토리 조회 (최신순)
     */
    suspend fun getVersionHistory(limit: Int = 10): Result<List<UpdateInfo>> = runCatching {
        client.from("update_info").select().decodeList<UpdateInfo>()
            .filter { it.appId == appId }
            .activeOnly()
            .sortedByDescending { it.versionCode }
            .take(limit)
    }
}
