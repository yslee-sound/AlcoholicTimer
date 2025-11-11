package kr.sweetapps.alcoholictimer.data.supabase.repository

import kr.sweetapps.alcoholictimer.data.supabase.model.AdPolicy
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/**
 * AdPolicy Repository: 광고 정책 조회
 * - RLS 정책에 의해 is_active = TRUE인 정책만 조회 가능
 * - 3분 캐싱으로 네트워크 요청 최소화 (긴급 제어 가능)
 * - app_policy와 독립적으로 운영
 */
class AdPolicyRepository(
    private val client: SupabaseClient,
    private val appId: String = "alcoholictimer"
) {
    companion object {
        private const val TAG = "AdPolicyRepo"
        private const val CACHE_DURATION_MS = 3 * 60 * 1000L // 3분 (긴급 대응 가능 + 효율적)
    }

    private var cachedPolicy: AdPolicy? = null
    private var cacheTimestamp: Long = 0

    /**
     * 현재 활성화된 광고 정책 조회 (3분 캐싱)
     * @return 정책이 있으면 AdPolicy, 없으면 null
     */
    suspend fun getPolicy(): Result<AdPolicy?> = runCatching {
        val currentTime = System.currentTimeMillis()

        // 캐시가 유효하면 캐시 사용
        if (cachedPolicy != null && currentTime - cacheTimestamp < CACHE_DURATION_MS) {
            val remainingSeconds = (CACHE_DURATION_MS - (currentTime - cacheTimestamp)) / 1000
            android.util.Log.d(TAG, "📦 캐시된 광고 정책 사용 (유효 시간: ${remainingSeconds}초 남음)")
            return@runCatching cachedPolicy
        }

        // 캐시 만료 또는 없음 → Supabase에서 새로 가져오기
        android.util.Log.d(TAG, "===== Ad Policy Fetch Started =====")
        android.util.Log.d(TAG, "🔄 Supabase에서 광고 정책 새로 가져오기")
        android.util.Log.d(TAG, "Target app_id: $appId")

        // 전체 조회 후 클라이언트에서 필터링
        val allPolicies = client.from("ad_policy")
            .select()
            .decodeList<AdPolicy>()

        android.util.Log.d(TAG, "Total rows fetched: ${allPolicies.size}")

        // app_id로 정책 찾기 (is_active 상관없이)
        val policy = allPolicies.firstOrNull { it.appId == appId }

        if (policy != null) {
            android.util.Log.d(TAG, "✅ 광고 정책 발견!")
            android.util.Log.d(TAG, "  - is_active: ${policy.isActive}")
            android.util.Log.d(TAG, "  - App Open Ad: ${policy.adAppOpenEnabled}")
            android.util.Log.d(TAG, "  - Interstitial Ad: ${policy.adInterstitialEnabled}")
            android.util.Log.d(TAG, "  - Banner Ad: ${policy.adBannerEnabled}")
            android.util.Log.d(TAG, "  - Max Per Hour: ${policy.adInterstitialMaxPerHour}")
            android.util.Log.d(TAG, "  - Max Per Day: ${policy.adInterstitialMaxPerDay}")

            // is_active 체크는 사용하는 곳에서 수행
            // 캐시 갱신 (is_active 상관없이 저장)
            cachedPolicy = policy
            cacheTimestamp = currentTime
        } else {
            android.util.Log.d(TAG, "⚠️ 광고 정책 없음 (app_id: $appId)")
            android.util.Log.d(TAG, "⚠️ 기본값 사용됨")
            cachedPolicy = null
        }

        android.util.Log.d(TAG, "===== Ad Policy Fetch Completed =====")

        policy
    }

    /**
     * 캐시 초기화
     */
    fun clearCache() {
        cachedPolicy = null
        cacheTimestamp = 0
        android.util.Log.d(TAG, "🔄 광고 정책 캐시 초기화")
    }

    /**
     * 현재 캐시된 정책 반환 (동기)
     */
    fun getCachedPolicy(): AdPolicy? = cachedPolicy
}

