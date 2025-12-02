package kr.sweetapps.alcoholictimer.ui.ad

import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import kr.sweetapps.alcoholictimer.R

/**
 * ?§Ïù¥?∞Î∏å Í¥ëÍ≥† Î∑?Î∞îÏù∏???†Ìã∏Î¶¨Ìã∞
 */
object NativeViewBinder {
    /**
     * NativeAdView??NativeAd ?∞Ïù¥?∞Î? Î∞îÏù∏??
     */
    fun bind(nativeAdView: NativeAdView, nativeAd: NativeAd) {
        try {
            // ?§Îìú?ºÏù∏
            nativeAdView.findViewById<TextView>(R.id.ad_headline)?.let {
                it.text = nativeAd.headline
                nativeAdView.headlineView = it
            }

            // Î≥∏Î¨∏
            nativeAdView.findViewById<TextView>(R.id.ad_body)?.let {
                it.text = nativeAd.body
                nativeAdView.bodyView = it
            }

            // CTA Î≤ÑÌäº
            nativeAdView.findViewById<Button>(R.id.ad_call_to_action)?.let {
                it.text = nativeAd.callToAction ?: "?êÏÑ∏??Î≥¥Í∏∞"
                nativeAdView.callToActionView = it
            }

            // ?ÑÏù¥ÏΩ?
            nativeAdView.findViewById<ImageView>(R.id.ad_icon)?.let { iconView ->
                nativeAd.icon?.let { icon ->
                    iconView.setImageDrawable(icon.drawable)
                    nativeAdView.iconView = iconView
                }
            }

            // Í¥ëÍ≥†Ï£?
            nativeAdView.findViewById<TextView>(R.id.ad_advertiser)?.let {
                it.text = nativeAd.advertiser
                nativeAdView.advertiserView = it
            }

            // ÎØ∏Îîî??Î∑?
            nativeAdView.findViewById<com.google.android.gms.ads.nativead.MediaView>(R.id.ad_media)?.let {
                nativeAdView.mediaView = it
            }

            // NativeAdView??NativeAd Í∞ùÏ≤¥ ?§Ï†ï
            nativeAdView.setNativeAd(nativeAd)
        } catch (t: Throwable) {
            // swallow to avoid crashing if ad SDK classes mismatch
        }
    }
}

