package kr.sweetapps.alcoholictimer.data.supabase.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 광고 정책 테이블 모델
 * - app_policy에서 분리된 광고 전용 정책 테이블
 * - RLS로 is_active = TRUE인 정책만 조회 가능
 *
 * 테이블: ad_policy
 * 주요 컬럼:
 *  - app_id (TEXT UNIQUE): 앱 식별자
 *  - is_active (BOOLEAN): 광고 정책 활성화 여부
 *  - ad_app_open_enabled (BOOLEAN): 앱 오픈 광고 활성화 여부
 *  - ad_interstitial_enabled (BOOLEAN): 전면 광고 활성화 여부
 *  - ad_banner_enabled (BOOLEAN): 배너 광고 활성화 여부
 *  - ad_interstitial_max_per_hour (INT): 전면 광고 시간당 최대 횟수
 *  - ad_interstitial_max_per_day (INT): 전면 광고 하루 최대 횟수
 *
 * 사용 예:
 * ```kotlin
 * val adPolicy = adPolicyRepository.getPolicy()
 * val bannerEnabled = adPolicy?.adBannerEnabled ?: true
 * val interstitialEnabled = adPolicy?.adInterstitialEnabled ?: true
 * val maxPerHour = adPolicy?.adInterstitialMaxPerHour ?: 2  // 보수적 기본값
 * ```
 *
 * 특징:
 * - app_policy와 완전히 독립적으로 제어 가능
 * - 팝업 상태와 무관하게 광고 ON/OFF 가능
 * - 광고 빈도 제한을 실시간으로 조정 가능
 */
@Serializable
data class AdPolicy(
    @SerialName("id")
    val id: Long? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("app_id")
    val appId: String,

    @SerialName("is_active")
    val isActive: Boolean = true,

    // ===== 광고 ON/OFF =====
    @SerialName("ad_app_open_enabled")
    val adAppOpenEnabled: Boolean = true,

    @SerialName("ad_interstitial_enabled")
    val adInterstitialEnabled: Boolean = true,

    @SerialName("ad_banner_enabled")
    val adBannerEnabled: Boolean = true,

    // ===== 광고 빈도 제어 =====
    @SerialName("ad_interstitial_max_per_hour")
    val adInterstitialMaxPerHour: Int = 2,

    @SerialName("ad_interstitial_max_per_day")
    val adInterstitialMaxPerDay: Int = 15,

    // App Open 광고 빈도 제한 (Supabase에 추가된 필드)
    @SerialName("app_open_max_per_hour")
    val appOpenMaxPerHour: Int = 2,

    @SerialName("app_open_max_per_day")
    val appOpenMaxPerDay: Int = 15
)
