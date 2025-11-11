package kr.sweetapps.alcoholictimer.data.supabase.repository

import android.R.attr.order
import android.content.Context
import android.service.autofill.Validators.or
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kr.sweetapps.alcoholictimer.data.supabase.model.NoticePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Objects.isNull

/**
 * 일반 공지사항 정책 저장소
 *
 * Supabase의 notice_policies 테이블과 연동하여
 * 활성화된 공지사항 정책을 가져옵니다.
 *
 * 우선순위:
 * 1. is_active = true이고 만료되지 않은 정책만 조회
 * 2. 현재 앱 버전에 해당하는 정책만 조회
 * 3. priority가 높은 순서로 정렬
 * 4. show_once가 true인 경우 한 번만 표시
 *
 * @property client Supabase 클라이언트
 * @property context 앱 Context (SharedPreferences 등에 사용)
 */
class NoticePolicyRepository(
    private val client: SupabaseClient,
    private val context: Context
) {
    companion object {
        private const val TABLE_NAME = "notice_policies"
        private const val PREF_NAME = "notice_policy_prefs"
        private const val KEY_SHOWN_POLICY_IDS = "shown_policy_ids"
    }

    /**
     * 활성화된 공지사항 정책을 가져옵니다.
     *
     * @param currentVersion 현재 앱 버전 (예: "1.0.0")
     * @return 표시할 공지사항 정책 또는 null
     */
    suspend fun getActivePolicy(currentVersion: String): NoticePolicy? = withContext(Dispatchers.IO) {
        try {
            val now = Instant.now().toString()

            val policies = client.from(TABLE_NAME)
                .select {
                    filter {
                        eq("is_active", true)
                        or {
                            isNull("expires_at")
                            gt("expires_at", now)
                        }
                    }
                }
                .decodeList<NoticePolicy>()
                .sortedWith(compareByDescending<NoticePolicy> { it.priority }.thenByDescending { it.createdAt })

            // 버전 필터링 및 한 번만 표시 로직 적용
            val shownIds = getShownPolicyIds()
            policies.firstOrNull { policy ->
                isVersionInRange(currentVersion, policy.targetVersionMin, policy.targetVersionMax) &&
                (!policy.showOnce || policy.id !in shownIds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 현재 버전이 대상 버전 범위 내에 있는지 확인합니다.
     */
    private fun isVersionInRange(current: String, min: String?, max: String?): Boolean {
        if (min == null && max == null) return true

        try {
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }

            if (min != null) {
                val minParts = min.split(".").map { it.toIntOrNull() ?: 0 }
                if (compareVersions(currentParts, minParts) < 0) return false
            }

            if (max != null) {
                val maxParts = max.split(".").map { it.toIntOrNull() ?: 0 }
                if (compareVersions(currentParts, maxParts) > 0) return false
            }

            return true
        } catch (e: Exception) {
            return true // 버전 비교 실패 시 일단 표시
        }
    }

    /**
     * 두 버전을 비교합니다.
     * @return v1 < v2이면 음수, v1 == v2이면 0, v1 > v2이면 양수
     */
    private fun compareVersions(v1: List<Int>, v2: List<Int>): Int {
        val maxLength = maxOf(v1.size, v2.size)
        for (i in 0 until maxLength) {
            val part1 = v1.getOrNull(i) ?: 0
            val part2 = v2.getOrNull(i) ?: 0
            if (part1 != part2) return part1 - part2
        }
        return 0
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

