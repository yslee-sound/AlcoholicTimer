@file:Suppress("UNUSED_PARAMETER")
package kr.sweetapps.alcoholictimer.consent

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * UMP 통합 구현체 (중복 방지 + 표준 플로우 전용 단일 버전)
 */
class UmpConsentManager(private val context: Context) {
    private val TAG = "UmpConsentManager"

    // 동시 실행 방지 플래그
    private val isGathering = AtomicBoolean(false)

    @Volatile
    var canRequestAds: Boolean = false
        private set

    /** 현재 UMP 폼이 표시 중인지 여부 (간단 버전: gatherConsent 안에서만 관리) */
    @Volatile
    private var formShowing: Boolean = false

    fun isFormShowing(): Boolean = formShowing

    /**
     * 표준 UMP 플로우 (RequestInfoUpdate -> loadAndShowConsentFormIfRequired)
     * - 중복 호출 시 무시
     * - [NEW] 5초 타임아웃 안전장치 추가 (2026-01-02)
     */
    fun gatherConsent(activity: Activity, onComplete: (Boolean) -> Unit) {
        if (isGathering.getAndSet(true)) {
            Log.w(TAG, "gatherConsent() ignored: already in progress")
            return
        }

        Log.d(TAG, "gatherConsent() start")

        // [NEW] Race Condition: UMP 응답 vs 타임아웃 중 먼저 완료되는 쪽이 실행 (2026-01-02)
        val isCompleted = AtomicBoolean(false)
        val handler = Handler(Looper.getMainLooper())

        // [안전장치] 5초 타임아웃 - UMP 서버 응답 없을 시 강제 진행
        val timeoutRunnable = Runnable {
            if (isCompleted.compareAndSet(false, true)) {
                Log.w(TAG, "⏱️ TIMEOUT (5s): UMP 서버 응답 없음 - 강제 진행")
                formShowing = false
                isGathering.set(false)
                canRequestAds = false
                onComplete(false) // 동의 없이 진행 (광고 없음)
            }
        }
        handler.postDelayed(timeoutRunnable, 5000L) // 5초

        val params = createConsentRequestParameters(activity)
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)

        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                // UMP 요청 성공 - 타임아웃보다 먼저 완료됨
                if (isCompleted.get()) {
                    Log.d(TAG, "requestConsentInfoUpdate success but already timed out - ignoring")
                    return@requestConsentInfoUpdate
                }

                Log.d(TAG, "requestConsentInfoUpdate success: status=${consentInfo.consentStatus}")

                // SDK에게 폼 표시 여부를 전적으로 위임
                formShowing = true
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError: FormError? ->
                    handler.removeCallbacks(timeoutRunnable) // 타임아웃 취소

                    if (isCompleted.compareAndSet(false, true)) {
                        formShowing = false
                        isGathering.set(false)

                        if (formError != null) {
                            Log.e(TAG, "Consent form error: ${formError.message}")
                            canRequestAds = false
                            onComplete(false)
                            return@loadAndShowConsentFormIfRequired
                        }

                        val finalStatus = consentInfo.consentStatus
                        val allowed = finalStatus == ConsentInformation.ConsentStatus.OBTAINED ||
                                      finalStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED

                        Log.d(TAG, "Consent finished: status=$finalStatus canRequestAds=$allowed")
                        canRequestAds = allowed
                        onComplete(allowed)
                    } else {
                        Log.d(TAG, "Form completed but already handled by timeout")
                    }
                }
            },
            { error: FormError? ->
                handler.removeCallbacks(timeoutRunnable) // 타임아웃 취소

                if (isCompleted.compareAndSet(false, true)) {
                    Log.e(TAG, "requestConsentInfoUpdate failed: ${error?.message}")
                    formShowing = false
                    isGathering.set(false)
                    canRequestAds = false
                    onComplete(false)
                } else {
                    Log.d(TAG, "Request failed but already handled by timeout")
                }
            }
        )
    }

    private fun createConsentRequestParameters(activity: Activity): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false)

        if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
            // [UPDATED] local.properties의 UMP_TEST_DEVICE_HASH 사용 (쉼표로 구분된 여러 기기 지원)
            val testHash = kr.sweetapps.alcoholictimer.BuildConfig.UMP_TEST_DEVICE_HASH
            if (testHash.isNotBlank()) {
                val debugSettingsBuilder = ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)

                // 쉼표로 구분된 여러 테스트 기기 해시 지원
                val testDeviceHashes = testHash.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                testDeviceHashes.forEach { hash ->
                    debugSettingsBuilder.addTestDeviceHashedId(hash)
                    Log.d(TAG, "UMP Debug 기기 추가: $hash")
                }

                val debugSettings = debugSettingsBuilder.build()
                builder.setConsentDebugSettings(debugSettings)
                Log.d(TAG, "UMP 디버그 모드 활성화 (${testDeviceHashes.size}개 기기)")
            } else {
                Log.d(TAG, "UMP_TEST_DEVICE_HASH가 비어있음 - 일반 모드로 실행")
            }
        }

        return builder.build()
    }

    /** Debug 전용: 동의 상태 강제 리셋 */
    fun resetConsent(context: Context) {
        if (!kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) return
        try {
            UserMessagingPlatform.getConsentInformation(context).reset()
            isGathering.set(false)
            canRequestAds = false
            Log.d(TAG, "Consent reset (DEBUG)")
        } catch (t: Throwable) {
            Log.e(TAG, "resetConsent failed: ${t.message}")
        }
    }

    /** Privacy Options 폼 직접 표시 (About 화면 등에서 사용) */
    fun showPrivacyOptionsForm(activity: Activity, onClosed: (FormError?) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            onClosed(error)
        }
    }

    /**
     * [EU 감지] 사용자의 지역 및 설정에 따라 'Privacy Options' 메뉴 표시 여부를 반환합니다.
     * - REQUIRED: EU/EEA 지역 사용자 (반드시 표시)
     * - NOT_REQUIRED: 한국/미국 등 (표시 안 함)
     */
    fun isPrivacyOptionsRequired(): Boolean {
        return try {
            val consentInfo = UserMessagingPlatform.getConsentInformation(context)
            val status = consentInfo.privacyOptionsRequirementStatus
            status == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        } catch (t: Throwable) {
            Log.e(TAG, "isPrivacyOptionsRequired check failed: ${t.message}")
            false
        }
    }
}
