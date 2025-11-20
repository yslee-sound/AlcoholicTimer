package kr.sweetapps.alcoholictimer.data.supabase.model

/** Minimal AdPolicy stub used while Supabase code is removed/replaced. */
data class AdPolicy(
    val appId: String = "",
    val isActive: Boolean = false,
    val adAppOpenEnabled: Boolean = false,
    val adInterstitialEnabled: Boolean = false,
    val adBannerEnabled: Boolean = false,
    val adInterstitialMaxPerHour: Int = 0,
    val adInterstitialMaxPerDay: Int = 0
)
