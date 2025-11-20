package kr.sweetapps.alcoholictimer.data.supabase.repository

import kr.sweetapps.alcoholictimer.data.supabase.model.NoticePolicy

/** Stub NoticePolicyRepository - no network access, returns null/empty items. */
class NoticePolicyRepository {
    suspend fun getActivePolicy(currentVersion: String): NoticePolicy? = null
    fun markPolicyAsShown(policyId: String) {}
    fun clearShownPolicies() {}
}
