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
    val adInterstitialMaxPerDay: Int = 20
)
