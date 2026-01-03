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

                // [FIX v8] UMP 동의 폼을 정상적으로 표시 (2026-01-03)
                // loadAndShowConsentFormIfRequired를 호출하여 필요 시 동의 창 표시
                formShowing = true
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAdError: FormError? ->
                    formShowing = false

                    // 타이머 해제
                    mainHandler.removeCallbacks(timeoutRunnable)

                    if (loadAdError != null) {
                        Log.w(TAG, "⚠️ Form load error: ${loadAdError.message}")
                    }

                    // 동의 상태 확인하여 canRequestAds 갱신
                    val finalStatus = consentInfo.consentStatus
                    canRequestAds = finalStatus == ConsentInformation.ConsentStatus.OBTAINED ||
                                   finalStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED

                    Log.d(TAG, "✅ Consent status: $finalStatus, canRequestAds=$canRequestAds")

                    // 모든 처리 완료 후 메인으로 진행
                    proceedToApp()
                }
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

        // [FIX v9] Debug 모드면 무조건 EEA 지역 설정 (2026-01-03)
        if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
            Log.d(TAG, "🇪🇺 Debug 모드 - 강제 EEA 지역 설정")

            val debugSettingsBuilder = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA) // 🇪🇺 강제 유럽

            // BuildConfig에 등록된 테스트 기기 해시가 있으면 추가
            val testHash = try {
                kr.sweetapps.alcoholictimer.BuildConfig.UMP_TEST_DEVICE_HASH
            } catch (_: Exception) {
                ""
            }

            if (testHash.isNotBlank()) {
                val testDeviceHashes = testHash.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                testDeviceHashes.forEach { hash ->
                    debugSettingsBuilder.addTestDeviceHashedId(hash)
                    Log.d(TAG, "   ✓ 테스트 기기 해시 추가: $hash")
                }
            } else {
                Log.d(TAG, "   ℹ️ UMP_TEST_DEVICE_HASH 없음 - EEA 설정만 적용")
            }

            // ★ 중요: 실제 기기에서 테스트 중이라면, Logcat에 뜨는 본인의 기기 ID를 여기에 추가하세요
            // 예: debugSettingsBuilder.addTestDeviceHashedId("YOUR_DEVICE_HASH_FROM_LOGCAT")

            builder.setConsentDebugSettings(debugSettingsBuilder.build())
            Log.d(TAG, "   ✅ Debug 설정 완료: EEA 지역 강제 적용")
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
