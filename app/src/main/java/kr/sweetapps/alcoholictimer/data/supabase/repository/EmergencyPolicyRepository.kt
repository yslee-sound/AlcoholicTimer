package kr.sweetapps.alcoholictimer.data.supabase.repository

import android.R.attr.order
import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kr.sweetapps.alcoholictimer.data.supabase.model.EmergencyPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 긴급 공지 정책 저장소
 *
 * Supabase의 emergency_policies 테이블과 연동하여
 * 활성화된 긴급 공지 정책을 가져옵니다.
 *
 * 우선순위:
 * 1. is_active = true인 정책만 조회
 * 2. priority가 높은 순서로 정렬
 * 3. 가장 최근에 생성된 정책을 우선
 *
 * @property client Supabase 클라이언트
 * @property context 앱 Context (SharedPreferences 등에 사용)
 */
class EmergencyPolicyRepository(
    private val client: SupabaseClient,
    private val context: Context
) {
    companion object {
        private const val TABLE_NAME = "emergency_policies"
        private const val PREF_NAME = "emergency_policy_prefs"
        private const val KEY_SHOWN_POLICY_IDS = "shown_policy_ids"
    }

    /**
     * 활성화된 긴급 공지 정책을 가져옵니다.
     * 우선순위가 높고 아직 표시되지 않은 정책을 반환합니다.
     *
     * @return 표시할 긴급 공지 정책 또는 null
     */
    suspend fun getActivePolicy(): EmergencyPolicy? = withContext(Dispatchers.IO) {
        try {
            val policies = client.from(TABLE_NAME)
                .select {
                    filter {
                        eq("is_active", true)
                    }
                }
                .decodeList<EmergencyPolicy>()
                .sortedWith(compareByDescending<EmergencyPolicy> { it.priority }.thenByDescending { it.createdAt })

            // 아직 표시되지 않은 정책 찾기
            val shownIds = getShownPolicyIds()
            policies.firstOrNull { it.id !in shownIds }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 정책을 표시했음을 기록합니다.
     *
     * @param policyId 표시된 정책 ID
     */
    fun markPolicyAsShown(policyId: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val shownIds = getShownPolicyIds().toMutableSet()
        shownIds.add(policyId)
        prefs.edit().putStringSet(KEY_SHOWN_POLICY_IDS, shownIds).apply()
    }

    /**
     * 이미 표시된 정책 ID 목록을 가져옵니다.
     *
     * @return 표시된 정책 ID Set
     */
    private fun getShownPolicyIds(): Set<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_SHOWN_POLICY_IDS, emptySet()) ?: emptySet()
    }

    /**
     * 표시 기록을 초기화합니다. (디버그용)
     */
    fun clearShownPolicies() {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SHOWN_POLICY_IDS).apply()
    }
}

