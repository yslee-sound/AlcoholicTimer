@file:Suppress("UNUSED_PARAMETER")
package kr.sweetapps.alcoholictimer.consent

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * UMP 통합 구현체 (Window Focus 문제 완벽 해결 버전)
 */
class UmpConsentManager(private val context: Context) {
    private val TAG = "UmpConsentManager"

    // 동시 실행 방지 플래그
    private val isGathering = AtomicBoolean(false)

    @Volatile
    var canRequestAds: Boolean = false
        private set

    /** 현재 UMP 폼이 표시 중인지 여부 */
    @Volatile
    private var formShowing: Boolean = false

    fun isFormShowing(): Boolean = formShowing

    /**
     * UMP 동의 수집
     * [FIX] Handler 대신 decorView.post 사용으로 '터치해야 넘어가는 문제' 해결
     */
    fun gatherConsent(activity: Activity, onComplete: (Boolean) -> Unit) {
        if (isGathering.getAndSet(true)) {
            Log.w(TAG, "gatherConsent() ignored: already in progress")
            return
        }

        Log.d(TAG, "🚀 gatherConsent() start")

        val isFinished = AtomicBoolean(false)
        val mainHandler = Handler(Looper.getMainLooper())

        // [FIX v5] 딜레이 제거 - runOnUiThread로 UI 스레드 확실히 보장
        val proceedToApp = {
            if (isFinished.compareAndSet(false, true)) {
                Log.d(TAG, "✅ Consent flow finished. Proceeding to app...")
                formShowing = false
                isGathering.set(false)

                // [핵심] runOnUiThread로 UI 스레드에서 즉시 실행
                activity.runOnUiThread {
                    Log.d(TAG, "🎯 Calling onComplete (UI Thread)")
                    onComplete(canRequestAds)
                }
            }
        }

        // 1. [안전장치] 4초 타임아웃
        val timeoutRunnable = Runnable {
            Log.e(TAG, "⏰ FORCE TIMEOUT (4s)! Skipping to app.")
            canRequestAds = false
            proceedToApp()
        }
        mainHandler.postDelayed(timeoutRunnable, 4000L)

        // 2. UMP 파라미터 생성
        val params = createConsentRequestParameters(activity)
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)

        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            { // [성공 시]
                Log.d(TAG, "📋 Consent Info Available")

                // 타이머 해제
                mainHandler.removeCallbacks(timeoutRunnable)

                // [FIX v6] loadAndShowConsentFormIfRequired는 폼이 필요 없을 때 콜백을 호출하지 않음!
                // 해결: 수동으로 상태를 체크하고 처리
                val finalStatus = consentInfo.consentStatus
                canRequestAds = finalStatus == ConsentInformation.ConsentStatus.OBTAINED ||
                               finalStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED

                Log.d(TAG, "✅ Consent status: $finalStatus, canRequestAds=$canRequestAds")

                // 무조건 진행 (폼 표시 여부와 무관)
                proceedToApp()
            },
            { error: FormError? -> // [실패 시]
                Log.w(TAG, "❌ Consent Info Update Failed: ${error?.message}")
                mainHandler.removeCallbacks(timeoutRunnable)
                canRequestAds = false
                proceedToApp()
            }
        )
    }

    private fun createConsentRequestParameters(activity: Activity): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false)

        if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
            val testHash = kr.sweetapps.alcoholictimer.BuildConfig.UMP_TEST_DEVICE_HASH
            if (testHash.isNotBlank()) {
                val debugSettingsBuilder = ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)

                val testDeviceHashes = testHash.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                testDeviceHashes.forEach { hash ->
                    debugSettingsBuilder.addTestDeviceHashedId(hash)
                }
                builder.setConsentDebugSettings(debugSettingsBuilder.build())
            }
        }
        return builder.build()
    }

    // ... (resetConsent, showPrivacyOptionsForm 등 나머지는 기존 유지) ...
    fun resetConsent(context: Context) {
        if (!kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) return
        try {
            UserMessagingPlatform.getConsentInformation(context).reset()
            isGathering.set(false)
            canRequestAds = false
        } catch (t: Throwable) {}
    }

    fun showPrivacyOptionsForm(activity: Activity, onClosed: (FormError?) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onClosed)
    }

    fun isPrivacyOptionsRequired(): Boolean {
        return try {
            val consentInfo = UserMessagingPlatform.getConsentInformation(context)
            consentInfo.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        } catch (t: Throwable) { false }
    }
}