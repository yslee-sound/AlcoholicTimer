package kr.sweetapps.alcoholictimer.data.supabase.repository

import android.content.Context
import android.util.Log
import kr.sweetapps.alcoholictimer.data.supabase.model.PopupDecision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 팝업 정책 통합 관리자
 *
 * 여러 팝업 정책(긴급 공지, 업데이트, 일반 공지)을 우선순위에 따라 관리합니다.
 *
 * 우선순위:
 * 1. 긴급 공지 (EmergencyPolicy) - 최우선
 * 2. 업데이트 공지 (UpdatePolicy) - 강제 업데이트
 * 3. 일반 공지 (NoticePolicy) - 일반적인 공지사항
 * 4. 업데이트 공지 (UpdatePolicy) - 선택적 업데이트
 *
 * @property emergencyRepo 긴급 공지 저장소
 * @property updateRepo 업데이트 저장소
 * @property noticeRepo 일반 공지 저장소
 * @property context 앱 Context
 */
class PopupPolicyManager(
    private val emergencyRepo: EmergencyPolicyRepository,
    private val updateRepo: UpdatePolicyRepository,
    private val noticeRepo: NoticePolicyRepository,
    private val context: Context
) {
    /**
     * 표시할 팝업을 결정합니다.
     *
     * @param currentVersion 현재 앱 버전 (예: "1.0.0")
     * @return 표시할 팝업 결정
     */
    suspend fun decidePopup(currentVersion: String): PopupDecision = withContext(Dispatchers.IO) {
        try {
            Log.d("PopupPolicyManager", "decidePopup: currentVersion=$currentVersion")
             // 1. 긴급 공지 확인 (최우선)
             emergencyRepo.getActivePolicy()?.let { policy ->
                Log.d("PopupPolicyManager", "ShowEmergency chosen: ${policy.id}")
                 return@withContext PopupDecision.ShowEmergency(policy)
             }

            // 2. 강제 업데이트 / 선택적 업데이트 처리
            updateRepo.getActivePolicy()?.let { policy ->
                Log.d("PopupPolicyManager", "Update policy fetched: id=${policy.id} isForce=${policy.isForceUpdate} target=${policy.targetVersionCode}")
                 // If force update -> show immediately
                 if (policy.isForceUpdate) return@withContext PopupDecision.ShowUpdate(policy)

                // For optional update, check local stats: laterCount and cooldown
                val stats = updateRepo.getUpdateStats(policy.targetVersionCode)
                // check max later count
                if (policy.maxLaterCount > 0 && stats.currentLaterCount >= policy.maxLaterCount) {
                    // do not show
                } else {
                    // check cooldown if last dismissed exists
                    val lastMs = stats.lastDismissedAtMs
                    val now = System.currentTimeMillis()
                    val cooldownMs = ((policy.reshowIntervalHours ?: 0) * 3600L + (policy.reshowIntervalMinutes ?: 0) * 60L + (policy.reshowIntervalSeconds ?: 0)) * 1000L
                    if (lastMs <= 0 || now - lastMs >= cooldownMs) {
                        Log.d("PopupPolicyManager", "Update eligible by cooldown/never dismissed: lastMs=$lastMs cooldownMs=$cooldownMs")
                         // eligible
                         return@withContext PopupDecision.ShowUpdate(policy)
                     }
                 }
             }

            // 3. 일반 공지 확인
            noticeRepo.getActivePolicy(currentVersion)?.let { policy ->
                return@withContext PopupDecision.ShowNotice(policy)
            }

            // 표시할 팝업 없음
            Log.d("PopupPolicyManager", "No update/notice/emergency chosen")
            PopupDecision.None
        } catch (e: Exception) {
            e.printStackTrace()
            PopupDecision.None
        }
    }

    /**
     * 긴급 공지를 표시했음을 기록합니다.
     */
    fun markEmergencyShown(policyId: String) {
        emergencyRepo.markPolicyAsShown(policyId)
    }

    /**
     * 일반 공지를 표시했음을 기록합니다.
     */
    fun markNoticeShown(policyId: String) {
        noticeRepo.markPolicyAsShown(policyId)
    }

    /**
     * 업데이트를 나중에 하기로 선택했음을 기록합니다.
     */
    fun dismissUpdate(version: String) {
        updateRepo.dismissVersion(version)
    }

    /**
     * 버전코드 기반으로 '나중에' 선택 기록 (UpdatePolicyRepository.recordLaterClicked 사용)
     */
    fun dismissUpdate(versionCode: Long) {
        updateRepo.recordLaterClicked(versionCode)
    }

    /**
     * 모든 표시 기록을 초기화합니다. (디버그용)
     */
    fun clearAllRecords() {
        emergencyRepo.clearShownPolicies()
        noticeRepo.clearShownPolicies()
        updateRepo.clearDismissedVersion()
    }
}
