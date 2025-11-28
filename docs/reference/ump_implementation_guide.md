# UMP (GDPR) 동의 관리 기능 구현 가이드

이 문서는 Google의 UMP(User Messaging Platform) SDK를 사용하여 GDPR 동의 관리 기능을 구현하는 표준적인 절차와 핵심 해결 과정을 안내합니다.

## 1. 의존성 추가 (`build.gradle.kts`)

프로젝트에 필요한 두 가지 라이브러리를 추가하고 Gradle 동기화를 진행합니다.

- **UMP SDK**: 구글의 동의 관리 플랫폼 라이브러리
- **Preference-KTX**: UMP가 저장하는 동의 상태 값을 직접 읽기 위한 라이브러리

```kotlin
dependencies {
    // UMP (User Messaging Platform) SDK for GDPR consent
    implementation("com.google.android.ump:user-messaging-platform:2.1.0")

    // AndroidX Preference for reading TCF strings directly
    implementation("androidx.preference:preference-ktx:1.2.1")
}
```

## 2. UmpConsentManager 구현

동의 관련 모든 로직을 책임지는 `UmpConsentManager` 클래스를 생성합니다. 이 클래스는 앱 전역에서 하나의 인스턴스만 유지되도록 `Application` 클래스에서 생성하여 관리합니다.

### 핵심 로직

- **정확한 동의 상태 확인 (`updateConsentStatus`)**: `canRequestAds()` 대신, `SharedPreferences`에 저장된 TCF v2.2 표준 문자열을 직접 읽어서 '개인 맞춤 광고' (Purpose 1) 동의 여부를 판단하는 것이 핵심입니다. **이것이 스위치 상태를 정확하게 반영하는 유일하고 확실한 방법입니다.**
  - **Key**: `"IABTCF_PurposeConsents"` (끝에 's'가 중요)
  - **Value**: 문자열의 첫 번째 문자(인덱스 0)가 '1'이면 동의, '0'이면 비동의.

- **상태 업데이트 시점**: 사용자의 선택이 끝나는 모든 시점(동의 양식, 개인정보 보호 옵션 창이 닫힐 때)에 `updateConsentStatus()`를 호출하여 UI가 바라보는 상태 값을 갱신해줍니다.

- **광고 SDK 초기화 시점**: 스위치 상태와 별개로, 광고 SDK 초기화 여부는 UMP SDK가 제공하는 `consentInformation.canRequestAds()` (어떤 종류든 광고를 하나라도 보여줄 수 있는지)의 결과를 따르는 것이 안전합니다.

- **테스트 용이성 확보**: `resetConsent()` 시 `SharedPreferences`를 직접 초기화하고, 디버그 빌드에서는 테스트 기기 ID와 지역을 강제하여 테스트를 쉽게 만듭니다.

### 전체 코드 (`UmpConsentManager.kt`)

```kotlin
package com.example.app.consent

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.example.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.Exception

class UmpConsentManager(private val context: Context) {

    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    private val _isPrivacyOptionsRequired = MutableStateFlow(false)
    val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    private val _isPersonalizedAdsAllowed = MutableStateFlow(false)
    val isPersonalizedAdsAllowed: StateFlow<Boolean> = _isPersonalizedAdsAllowed.asStateFlow()

    init {
        updateConsentStatus()
    }

    fun updateConsentStatus() {
        _isPrivacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val purposeConsents = prefs.getString("IABTCF_PurposeConsents", "") ?: ""
        val hasConsentedToPurpose1 = purposeConsents.isNotEmpty() && purposeConsents[0] == '1'
        
        _isPersonalizedAdsAllowed.value = hasConsentedToPurpose1
    }

    fun gatherConsent(activity: Activity, onConsentGathered: (canInitializeAds: Boolean) -> Unit) {
        // TODO: Logcat에서 "Use new ConsentDebugSettings.Builder().addTestDeviceHashedId" 메시지를 찾아
        // "YOUR_HASHED_DEVICE_ID"를 실제 기기의 해시 ID로 교체하세요.
        val testDeviceId = "YOUR_HASHED_DEVICE_ID"
        val debugSettings = ConsentDebugSettings.Builder(context)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId(testDeviceId)
            .build()

        val params = if (BuildConfig.DEBUG) {
            ConsentRequestParameters.Builder()
                .setConsentDebugSettings(debugSettings)
                .build()
        } else {
            ConsentRequestParameters.Builder().build()
        }

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            { 
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
                    updateConsentStatus()
                    if (loadAndShowError != null) {
                        Log.w(TAG, "Form load/show error: ${loadAndShowError.errorCode} - ${loadAndShowError.message}")
                        onConsentGathered(false)
                        return@loadAndShowConsentFormIfRequired
                    }
                    onConsentGathered(consentInformation.canRequestAds())
                }
            },
            { 
                requestConsentError ->
                Log.w(TAG, "Consent info update error: ${requestConsentError.errorCode} - ${requestConsentError.message}")
                updateConsentStatus()
                onConsentGathered(false)
            }
        )
    }

    fun showPrivacyOptionsForm(activity: Activity, onFormError: (Exception) -> Unit) {
        if (consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED) {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                updateConsentStatus()
                if (formError != null) {
                    onFormError(Exception("Privacy options form error: ${formError.message}"))
                }
            }
        } else {
            onFormError(Exception("Privacy options form not available."))
        }
    }

    fun resetConsent() {
        consentInformation.reset()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().apply()
        updateConsentStatus()
    }

    companion object {
        private const val TAG = "UmpConsentManager"
    }
}
```

## 3. Application 클래스 연동

`Application` 클래스에서 `UmpConsentManager`의 인스턴스를 생성하여 앱 전역에서 공유합니다.

```kotlin
class MainApplication : Application() {
    lateinit var umpConsentManager: UmpConsentManager
        private set

    override fun onCreate() {
        super.onCreate()
        umpConsentManager = UmpConsentManager(this)
    }
}
```

## 4. MainActivity 연동

앱의 메인 `Activity`에서 동의 절차를 시작하고, 그 결과에 따라 광고 SDK를 초기화합니다.

```kotlin
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val umpConsentManager = (application as MainApplication).umpConsentManager

        umpConsentManager.gatherConsent(this) { canInitializeAds ->
            if (canInitializeAds) {
                // 광고 SDK 초기화 코드
                MobileAds.initialize(this) {}
            }
        }
        
        // ... setContent { ... }
    }
}
```

## 5. 설정 화면 UI 구현 (Jetpack Compose)

설정 화면에 '개인 맞춤 광고' 스위치를 표시하고, `UmpConsentManager`와 연동합니다.

### 핵심 로직

- **상태 구독**: `UmpConsentManager`가 제공하는 `isPersonalizedAdsAllowed` `StateFlow`를 구독하여 스위치의 `checked` 상태와 직접 연결합니다.
- **역할 분담**: 스위치 자체는 눌리지 않도록(`onCheckedChange = null`) 하고, 메뉴 아이템 전체(`Row`)를 클릭했을 때 `showPrivacyOptionsForm`을 호출하도록 만듭니다. 이것이 UI 상태 충돌을 막는 가장 안정적인 방법입니다.

### 전체 코드 (예시: `SettingsScreen.kt`)

```kotlin
@Composable
fun SettingsList() {
    val context = LocalContext.current
    val application = context.applicationContext as MainApplication
    val umpConsentManager = application.umpConsentManager

    val isPrivacyOptionsRequired by umpConsentManager.isPrivacyOptionsRequired.collectAsState()
    val isPersonalizedAdsAllowed by umpConsentManager.isPersonalizedAdsAllowed.collectAsState()

    Column {
        // ... 다른 설정 메뉴들 ...

        if (isPrivacyOptionsRequired) {
            SettingsMenuWithSwitch(
                title = "Personalized Ads",
                checked = isPersonalizedAdsAllowed,
                onClick = {
                    val activity = context as? Activity
                    if (activity != null) {
                        umpConsentManager.showPrivacyOptionsForm(activity) { exception ->
                            // 오류 처리 (예: Toast 메시지)
                        }
                    }
                }
            )
        }

        // 디버그 모드에서만 동의 상태 재설정 메뉴 표시
        if (BuildConfig.DEBUG) {
            SettingsListItem(
                title = "Personalized Ads 재설정 (Debug)",
                onClick = {
                    umpConsentManager.resetConsent()
                    // 사용자에게 앱을 다시 시작하라는 안내 메시지 표시
                }
            )
        }
    }
}

@Composable
fun SettingsMenuWithSwitch(
    title: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // Row 전체를 클릭 가능하게 만듭니다.
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title)
        Switch(
            checked = checked,
            onCheckedChange = null // 스위치 자체는 상태를 직접 변경하지 않습니다.
        )
    }
}
```