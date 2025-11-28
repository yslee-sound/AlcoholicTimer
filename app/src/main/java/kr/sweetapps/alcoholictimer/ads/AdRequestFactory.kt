package kr.sweetapps.alcoholictimer.ads

import android.content.Context
import android.os.Bundle
import com.google.android.gms.ads.AdRequest
import com.google.ads.mediation.admob.AdMobAdapter

object AdRequestFactory {
    fun create(context: Context): AdRequest {
        return try {
            val usePersonalized = try { UmpConsentManager.consentChecked && UmpConsentManager.lastCanRequestAds } catch (_: Throwable) { false }
            if (usePersonalized) {
                AdRequest.Builder().build()
            } else {
                // Request non-personalized ads as conservative default until consent resolved
                val extras = Bundle()
                extras.putString("npa", "1")
                AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
            }
        } catch (_: Throwable) {
            // Fallback to default request
            AdRequest.Builder().build()
        }
    }
}

