package kr.sweetapps.alcoholictimer

import org.junit.Test
import org.junit.Assert.*

class ReleaseAdConfigTest {

    @Test
    fun buildConfigShouldBeFalseInRelease() {
        if (BuildConfig.DEBUG) {
            println("DEBUG build -> skip release assertion")
            return
        }
        assertFalse("In release build BuildConfig.DEBUG must be false", BuildConfig.DEBUG)
    }

    @Test
    fun bannerUnitIdConfigured() {
        val bannerId: String = BuildConfig.ADMOB_BANNER_UNIT_ID
        assertTrue("Banner id must not be blank", bannerId.isNotBlank())
        if (!BuildConfig.DEBUG) {
            assertFalse("Release build must not use test banner id", bannerId.contains("3940256099942544"))
        }
    }

    @Test
    fun interstitialUnitIdConfigured() {
        val id: String = BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID
        assertTrue("Interstitial id must not be blank", id.isNotBlank())
        if (!BuildConfig.DEBUG) {
            assertFalse("Release build must not use test interstitial id", id.contains("3940256099942544"))
        }
    }
}
