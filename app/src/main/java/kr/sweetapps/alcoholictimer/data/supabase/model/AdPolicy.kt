// 릴리스 노트: releaseVersionName이 "1.1.7"일 때 Supabase 키(anon 및 service_role)를 즉시 회전해야 합니다.
// - 콘솔에서 기존 키 폐기 및 재발급
// - local.properties, CI/CD 시크릿, 배포 아티팩트에서 새 키로 교체
// - 키 회전 전후로 광고/정책 동작을 모니터링할 것
//
// 추가 안내(중요): 키 회전 후의 예상 흐름 및 권장 롤아웃
// 1) 릴리즈 배포(이번 변경: 릴리즈 폴백 정책 포함) -> 앱이 원격 정책을 못 받아도 폴백으로 광고 동작 유지
// 2) Supabase 콘솔에서 새 키를 발급하거나 새 키를 추가(가능하면 기존 키와 병행 적용)
// 3) 새 키를 local.properties, CI 시크릿, 배포 스크립트 등에 반영하여 다음 클라이언트 업데이트에 포함
// 4) 충분한 사용자(내부 기준, 예: 설치 비율 또는 1~2주)에게 업데이트가 도달하면 기존 키를 폐기
//
// 영향 요약:
// - 기존 사용자가 업데이트 이전에 기존(혹은 새) 키가 폐기되면 Supabase와의 통신(공지/원격설정 등)은 동작하지 않습니다.
// - 광고 관련 폴백은 릴리즈 빌드에서 설정되어 있으므로 광고 노출은 폴백 규칙에 따라 유지됩니다.
// - 따라서 즉시 기존 키 폐기는 피하고, "병행 키 적용 → 충분 기간 대기 → 기존 키 폐기" 절차를 권장합니다.
//
// 모니터링 권장 항목:
// - Supabase 인증 실패 비율 및 에러 로그
// - 광고 로드/노출 로그(폴백 작동 여부)
// - 릴리즈 노트 및 내부 공지로 회전 일정을 공유

package kr.sweetapps.alcoholictimer.data.supabase.model

/** Minimal AdPolicy model mapping Supabase ad_policy fields used by AdController. */
data class AdPolicy(
    val id: Long = 0L,
    val appId: String = "",
    val isActive: Boolean = false,
    val adAppOpenEnabled: Boolean = true,
    val adInterstitialEnabled: Boolean = false,
    val adBannerEnabled: Boolean = false,
    val appOpenMaxPerHour: Int = 2,
    val appOpenMaxPerDay: Int = 15,
    val adInterstitialMaxPerHour: Int = 3,
    val adInterstitialMaxPerDay: Int = 20,
    // cooldown between app-open ads in seconds (remote-controlled)
    val appOpenCooldownSeconds: Int = 60,
    // minimum gap (seconds) between any two full-screen ads (server-controlled)
    val minFullscreenGapSeconds: Int = 30
) {
    companion object {
        // Default fallback policy used when remote policy cannot be fetched or parsed.
        // Values chosen to preserve revenue while being conservative about frequency.
        // [REMOVED] 배너 광고 제거: adBannerEnabled = false로 설정 (2025-12-01)
        val DEFAULT_FALLBACK = AdPolicy(
            id = 0L,
            appId = "",
            isActive = true,
            adAppOpenEnabled = true,
            adInterstitialEnabled = true,
            adBannerEnabled = false, // [REMOVED] 배너 광고 제거
            appOpenMaxPerHour = 2,
            appOpenMaxPerDay = 15,
            adInterstitialMaxPerHour = 2,
            adInterstitialMaxPerDay = 15,
            appOpenCooldownSeconds = 60,
            minFullscreenGapSeconds = 30
        )
    }
}
