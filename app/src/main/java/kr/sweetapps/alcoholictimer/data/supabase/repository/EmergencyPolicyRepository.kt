package kr.sweetapps.alcoholictimer.data.supabase.repository

import kr.sweetapps.alcoholictimer.data.supabase.model.EmergencyPolicy

/**
 * 긴급 공지 정책 저장소 (스텁 구현)
 *
 * Supabase 클라이언트 없이 작동하며,
 * 모든 메서드는 기본값(null 또는 빈 동작)을 가집니다.
 */
class EmergencyPolicyRepository {
    /**
     * 활성화된 긴급 공지 정책을 가져옵니다.
     * 현재는 항상 null을 반환합니다.
     *
     * @return 표시할 긴급 공지 정책 또는 null
     */
    suspend fun getActivePolicy(): EmergencyPolicy? = null

    /**
     * 정책을 표시했음을 기록합니다.
     *
     * @param policyId 표시된 정책 ID
     */
    fun markPolicyAsShown(policyId: String) {
        // 빈 동작
    }

    /**
     * 표시 기록을 초기화합니다. (디버그용)
     */
    fun clearShownPolicies() {
        // 빈 동작
    }
}
