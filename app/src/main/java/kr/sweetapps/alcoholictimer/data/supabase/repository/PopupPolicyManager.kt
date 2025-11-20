package kr.sweetapps.alcoholictimer.data.supabase.repository

import android.content.Context
import kr.sweetapps.alcoholictimer.data.supabase.model.PopupDecision

/** Simplified PopupPolicyManager that uses stub repositories. */
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
    suspend fun decidePopup(currentVersion: String): PopupDecision = PopupDecision.Noop

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
