package kr.sweetapps.alcoholictimer.ads

import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import kr.sweetapps.alcoholictimer.R

/**
 * 네이티브 광고 뷰 바인딩 유틸리티
 */
object NativeViewBinder {
    /**
     * NativeAdView에 NativeAd 데이터를 바인딩
     */
    fun bind(nativeAdView: NativeAdView, nativeAd: NativeAd) {
        try {
            // 헤드라인
            nativeAdView.findViewById<TextView>(R.id.ad_headline)?.let {
                it.text = nativeAd.headline
                nativeAdView.headlineView = it
            }

            // 본문
            nativeAdView.findViewById<TextView>(R.id.ad_body)?.let {
                it.text = nativeAd.body
                nativeAdView.bodyView = it
            }

            // CTA 버튼
            nativeAdView.findViewById<Button>(R.id.ad_call_to_action)?.let {
                it.text = nativeAd.callToAction ?: "자세히 보기"
                nativeAdView.callToActionView = it
            }

            // 아이콘
            nativeAdView.findViewById<ImageView>(R.id.ad_icon)?.let { iconView ->
                nativeAd.icon?.let { icon ->
                    iconView.setImageDrawable(icon.drawable)
                    nativeAdView.iconView = iconView
                }
            }

            // 광고주
            nativeAdView.findViewById<TextView>(R.id.ad_advertiser)?.let {
                it.text = nativeAd.advertiser
                nativeAdView.advertiserView = it
            }

            // 미디어 뷰
            nativeAdView.findViewById<com.google.android.gms.ads.nativead.MediaView>(R.id.ad_media)?.let {
                nativeAdView.mediaView = it
            }

            // NativeAdView에 NativeAd 객체 설정
            nativeAdView.setNativeAd(nativeAd)
        } catch (t: Throwable) {
            // swallow to avoid crashing if ad SDK classes mismatch
        }
    }
}

