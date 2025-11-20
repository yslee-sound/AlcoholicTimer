package kr.sweetapps.alcoholictimer.data.supabase.repository

import kr.sweetapps.alcoholictimer.data.supabase.model.UpdateInfo

/** Stub UpdateInfoRepository: placeholder methods to avoid Supabase dependency */
class UpdateInfoRepository(
    private val appId: String = "alcoholictimer"
) {
    suspend fun getLatestVersion(): Result<UpdateInfo?> = runCatching { null }
    suspend fun getLatestForApp(): Result<UpdateInfo?> = getLatestVersion()
    suspend fun checkUpdateRequired(currentVersionCode: Int): Result<UpdateInfo?> = runCatching { null }
    suspend fun isForceUpdateRequired(currentVersionCode: Int): Result<Boolean> = runCatching { false }
    suspend fun getVersionByCode(versionCode: Int): Result<UpdateInfo?> = runCatching { null }
    suspend fun getVersionHistory(limit: Int = 10): Result<List<UpdateInfo>> = runCatching { emptyList() }
}
