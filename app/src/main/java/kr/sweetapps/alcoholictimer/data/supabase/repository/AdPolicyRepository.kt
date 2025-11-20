package kr.sweetapps.alcoholictimer.data.supabase.repository

import kr.sweetapps.alcoholictimer.data.supabase.model.AdPolicy

/** Stub AdPolicyRepository: no network, returns null/empty results. */
class AdPolicyRepository(private val appId: String = "alcoholictimer") {
    suspend fun getPolicy(): Result<AdPolicy?> = runCatching { null }
    fun clearCache() {}
    fun getCachedPolicy(): AdPolicy? = null
}
