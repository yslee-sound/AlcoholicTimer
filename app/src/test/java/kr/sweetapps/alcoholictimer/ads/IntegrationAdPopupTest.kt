package kr.sweetapps.alcoholictimer.ads

import com.alcoholictimer.ad.AdManager
import com.alcoholictimer.ad.AdPolicyConfig
import com.alcoholictimer.ad.AdType
import com.alcoholictimer.ad.InMemoryPreferencesStore
import com.alcoholictimer.ad.TimeProvider
import org.junit.Assert.*
import org.junit.Test
import kr.sweetapps.alcoholictimer.ads.AdController

class IntegrationAdPopupTest {
    class TP(var now: Long) : TimeProvider { override fun nowMillis(): Long = now }

    @Test
    fun popupFlag_blocks_appOpen_and_interstitial() {
        // 초기 상태: 플래그 해제
        AdController.setFullScreenAdShowing(false)

        val base = 1_600_000_000_000L
        val tp = TP(base)
        val prefs = InMemoryPreferencesStore()
        val policy = AdPolicyConfig() // 기본값은 활성
        val manager = AdManager(policy, prefs, tp)

        // 기본적으로 노출 가능
        assertTrue(manager.canShowAd(AdType.APP_OPEN))
        assertTrue(manager.canShowAd(AdType.INTERSTITIAL))

        // 팝업 표시 중으로 설정 -> 광고는 불가
        AdController.setFullScreenAdShowing(true)
        assertFalse("팝업 활성 중에는 APP_OPEN 노출 불가", manager.canShowAd(AdType.APP_OPEN))
        assertFalse("팝업 활성 중에는 INTERSTITIAL 노출 불가", manager.canShowAd(AdType.INTERSTITIAL))

        // 팝업 해제 -> 다시 노출 가능
        AdController.setFullScreenAdShowing(false)
        assertTrue(manager.canShowAd(AdType.APP_OPEN))
        assertTrue(manager.canShowAd(AdType.INTERSTITIAL))
    }
}

