// [NEW] 클린 아키텍처 리팩토링: AdRequestFactory를 data/source/remote로 이동
package kr.sweetapps.alcoholictimer.data.source.remote

import android.content.Context
import android.os.Bundle
import com.google.android.gms.ads.AdRequest
import com.google.ads.mediation.admob.AdMobAdapter
import kr.sweetapps.alcoholictimer.MainApplication

object AdRequestFactory {
    fun create(context: Context): AdRequest {
        return try {
            // [수정] MainApplication에서 UmpConsentManager 인스턴스 가져오기
            val umpManager = try {
                (context.applicationContext as? MainApplication)?.umpConsentManager
            } catch (_: Throwable) { null }

            // [FIX] UmpConsentManager의 새 구조에 맞게 필드 사용 변경
            val usePersonalized = try {
                umpManager?.canRequestAds == true
            } catch (_: Throwable) { false }

            if (usePersonalized) {
                AdRequest.Builder().build()
            } else {
                // Request non-personalized ads as conservative default until consent resolved
                val extras = Bundle().apply {
                    putString("npa", "1")
                }
                AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                    .build()
            }
        } catch (_: Throwable) {
            // Fallback to default request
            AdRequest.Builder().build()
        }
    }
}
