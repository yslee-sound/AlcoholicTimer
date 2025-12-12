@file:Suppress("UNUSED_PARAMETER")
package kr.sweetapps.alcoholictimer.consent

import android.app.Activity
import android.content.Context
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
     */
    fun gatherConsent(activity: Activity, onComplete: (Boolean) -> Unit) {
        if (isGathering.getAndSet(true)) {
            Log.w(TAG, "gatherConsent() ignored: already in progress")
            return
        }

        Log.d(TAG, "gatherConsent() start")

        val params = createConsentRequestParameters(activity)
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)

        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                Log.d(TAG, "requestConsentInfoUpdate success: status=${consentInfo.consentStatus}")

                // SDK에게 폼 표시 여부를 전적으로 위임
                formShowing = true
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError: FormError? ->
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
                }
            },
            { error: FormError? ->
                Log.e(TAG, "requestConsentInfoUpdate failed: ${error?.message}")
                formShowing = false
                isGathering.set(false)
                canRequestAds = false
                onComplete(false)
            }
        )
    }

    private fun createConsentRequestParameters(activity: Activity): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false)

        if (kr.sweetapps.alcoholictimer.BuildConfig.DEBUG) {
            val testId = kr.sweetapps.alcoholictimer.BuildConfig.UMP_TEST_DEVICE_HASH
            if (testId.isNotBlank()) {
                val debugSettings = ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .addTestDeviceHashedId(testId)
                    .build()
                builder.setConsentDebugSettings(debugSettings)
                Log.d(TAG, "DEBUG UMP device=$testId")
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
