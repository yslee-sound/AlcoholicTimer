package kr.sweetapps.alcoholictimer.data.supabase.model

/**
 * 팝업 표시 결정 결과
 */
sealed class PopupDecision {
    /**
     * 긴급 공지 팝업을 표시해야 함
     */
    data class ShowEmergency(val policy: EmergencyPolicy) : PopupDecision()

    /**
     * 업데이트 팝업을 표시해야 함
     */
    data class ShowUpdate(val policy: UpdatePolicy) : PopupDecision()

    /**
     * 일반 공지 팝업을 표시해야 함
     */
    data class ShowNotice(val policy: NoticePolicy) : PopupDecision()

    /**
     * 표시할 팝업이 없음
     */
    data object None : PopupDecision()
}

