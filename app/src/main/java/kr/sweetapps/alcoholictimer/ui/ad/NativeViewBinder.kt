package kr.sweetapps.alcoholictimer.ui.ad

import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import kr.sweetapps.alcoholictimer.R

object NativeViewBinder {

    fun bind(nativeAdView: NativeAdView, nativeAd: NativeAd) {
        try {
            nativeAdView.findViewById<TextView>(R.id.ad_headline)?.let {
                it.text = nativeAd.headline
                nativeAdView.headlineView = it
            }

            nativeAdView.findViewById<TextView>(R.id.ad_body)?.let {
                it.text = nativeAd.body
                nativeAdView.bodyView = it
            }

            nativeAdView.findViewById<Button>(R.id.ad_call_to_action)?.let {
                it.text = nativeAd.callToAction ?: "?�세??보기"
                nativeAdView.callToActionView = it
            }

            nativeAdView.findViewById<ImageView>(R.id.ad_icon)?.let { iconView ->
                nativeAd.icon?.let { icon ->
                    iconView.setImageDrawable(icon.drawable)
                    nativeAdView.iconView = iconView
                }
            }

            nativeAdView.findViewById<TextView>(R.id.ad_advertiser)?.let {
                it.text = nativeAd.advertiser
                nativeAdView.advertiserView = it
            }

            nativeAdView.findViewById<com.google.android.gms.ads.nativead.MediaView>(R.id.ad_media)?.let {
                nativeAdView.mediaView = it
            }

            nativeAdView.setNativeAd(nativeAd)
        } catch (t: Throwable) {
            // swallow to avoid crashing if ad SDK classes mismatch
        }
    }
}

