# UMP 동의 표준 구성 가이드

-----

## 📌 UMP 동의 구현 가이드 (Agent 사용 시)

안녕하세요! UMP(User Messaging Platform) 동의 작업 때문에 어려움을 겪고 계시군요. 한국에 계셔서 유럽 사용자의 **동의 UI/흐름**이나 **검증 환경**을 구성하기 어려운 점을 충분히 이해합니다.

UMP 동의 구현은 복잡할 수 있지만, 필요한 구성요소와 흐름, 그리고 한국에서 검증할 수 있는 개발 방법을 명확히 정리해 드리겠습니다. 특히, 이전에 요청하신 것처럼 **앱 내비게이션 구성 요소**를 명시하고 **시스템 내비게이션 바** 관련 내용은 제외하여 가이드를 만들겠습니다.

-----

### 1. UMP 동의의 목적 및 핵심 구성요소

UMP는 주로 **GDPR(General Data Protection Regulation)** 및 **ePrivacy Directive**가 적용되는 **EEA(유럽 경제 지역)** 및 **영국** 사용자들에게 개인 데이터 처리(광고 목적 등)에 대한 **명확하고 자발적인 동의**를 얻기 위해 사용됩니다.

* **필수 요소:**
  * **동의 양식 (Consent Form):** 사용자에게 데이터 사용 목적과 방식, 그리고 동의 또는 거부 옵션을 제공하는 UI입니다.
  * **동의 상태 (Consent Status):** 사용자가 동의, 거부, 또는 동의하지 않음 중 어떤 상태인지 나타냅니다.
  * **동의 변경 옵션 (Privacy Settings):** 사용자가 언제든지 동의 상태를 검토하고 변경할 수 있도록 앱 내에 제공되는 UI 진입점입니다.

### 2. UMP 동의 흐름 및 UI 구성

UMP SDK를 통합하면, 사용자에게 동의가 필요한지 자동으로 확인하고 필요한 경우 동의 양식을 표시합니다.

#### 2.1. 동의 요청 흐름 (초기 실행)

1. **초기화 (Initialization):** 앱 실행 시 UMP SDK를 초기화하고 **최신 동의 정보**를 요청합니다.
   * `requestConsentInfoUpdate()`
2. **동의 상태 확인 (Check Status):** SDK는 사용자의 지역(EEA/UK 여부)과 기존 동의 상태를 확인합니다.
3. **양식 필요 여부 (Form Required):**
   * 양식이 필요함: (EEA/UK 사용자, 동의 상태가 불분명하거나 만료된 경우) → **동의 양식 표시**
   * 양식이 필요 없음: (한국 등 EEA/UK 외 지역 사용자, 이미 동의/거부한 상태 등) → **광고 로드 및 정상 동작**

#### 2.2. 동의 양식 UI (User Flow)

동의 양식은 UMP SDK에서 제공하며, 일반적으로 두 가지 화면으로 구성됩니다.

| 화면 | 앱 내비게이션 구성 요소 | 주요 내용 |
| :--- | :--- | :--- |
| **1단계: 동의 양식 (Consent Form)** | **모달 또는 전체 화면 팝업** | 개인 정보 처리 방식, 제휴사 목록, **'동의 및 계속'** 또는 **'옵션 관리'** 버튼 제공. |
| **2단계: 옵션 관리 (Manage Options)** | **모달 또는 전체 화면 뷰** | (사용자가 1단계에서 '옵션 관리' 선택 시) 목적별 데이터 사용 동의 여부 설정, **'확인 및 계속'** 버튼 제공. |

> ⚠️ **중요:** 이 양식은 SDK에서 표시하므로, 개발자가 직접 이 팝업의 UI를 만들 필요는 없습니다. 구현해야 할 것은 앱 실행 초기에 이 흐름을 트리거하고, 나중에 사용자가 동의를 변경할 수 있는 버튼을 앱에 배치하는 것입니다.

### 3. 구현해야 할 최소 범위

가장 쉽게 작업을 완료하기 위해 구현해야 할 핵심 단계는 다음과 같습니다.

1. **SDK 초기화 및 동의 정보 업데이트 요청:** 앱의 메인 활동/프래그먼트에서 가장 먼저 수행해야 합니다.
2. **동의 양식 로드 및 표시:** 동의가 필요한 경우 양식을 로드하고 표시하는 로직입니다.
3. **동의 변경 버튼 구현 (Privacy Settings):** 사용자가 언제든 동의를 변경할 수 있도록 앱 내비게이션 구성 요소(예: 설정 화면, 햄버거 메뉴 등) 내에 "개인 정보 보호 및 동의 설정"과 같은 항목을 만들고, 해당 버튼을 탭하면 동의 양식이 다시 표시되도록 연결해야 합니다.

---

```kotlin
// 예시: 동의 양식 다시 표시 로직 (동의 변경 버튼 연결)
// SDK는 현재 동의 상태에 따라 양식 표시 여부를 내부적으로 결정합니다.
ConsentInformation.getInstance(context).requestConsentInfoUpdate(
    activity,
    params,
    // 업데이트 성공 시 처리
)
```

-----

### 4. 한국에서 UMP 동의 검증 방법 🌍

한국에 계셔더라도 UMP의 동작을 검증할 수 있는 개발 방법이 있습니다. 이는 **테스트 지역(Geography)**을 강제로 설정하는 방식입니다.

#### 4.1. 개발/테스트 모드 활성화

앱의 디버그 빌드에서 `ConsentDebugSettings`를 사용하여 UMP를 테스트할 수 있습니다.

1. **테스트 기기 추가:**
   * 앱을 실행하고 로그캣에서 `UMP` 태그를 검색하여 자신의 해시된 기기 ID를 확인합니다.
   * `ConsentDebugSettings`에 이 ID를 추가하여 기기를 테스트 기기로 등록합니다.
2. **테스트 지역 설정:**
   * `ConsentDebugSettings.setDebugGeography()` 메서드를 사용하여 SDK가 현재 사용자를 특정 지역에 있는 것으로 인식하도록 강제합니다。

```kotlin
val debugSettings = ConsentDebugSettings.Builder(context)
    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
    .addTestDeviceHashedId("YOUR_TEST_DEVICE_HASHED_ID")
    .build()

val params = ConsentRequestParameters.Builder()
    .setConsentDebugSettings(debugSettings)
    .build()

ConsentInformation.getInstance(context).requestConsentInfoUpdate(
    activity,
    params,
    // ...
)
```

#### 4.2. 검증 단계

1. **GDPR 테스트 (EEA/UK로 설정):**
   * `DEBUG_GEOGRAPHY_EEA`로 설정하고 앱을 실행합니다。
   * 기대 결과: 앱 시작 시 동의 양식 팝업이 즉시 표시되어야 합니다。
   * 검증: 동의(Accept), 거부(Reject), 옵션 관리(Manage Options) 시 동의 상태가 올바르게 바뀌는지 확인합니다。
2. **비-GDPR 테스트 (기타 지역으로 설정):**
   * `DEBUG_GEOGRAPHY_NOT_EEA` 또는 설정을 제거하고 앱을 실행합니다。
   * 기대 결과: 앱 시작 시 동의 양식 팝업이 표시되지 않아야 합니다。
   * 검증: 광고 로드 시 문제가 없는지 확인합니다。

-----

### 5. 앱 내 추가 기능(개발자가 구현한 항목) 검토 — 요청 반영

요청하신 내용(디버그 메뉴 및 탭05 스위치)을 문서에 반영합니다. 아래는 안전 권장사항과 동작 정의입니다.

1) 탭05의 "Personalized Ads" 스위치

- 기능 설명: 앱의 UI 탭(사용자께서 명시한 `tab05`)에 "Personalized Ads"라는 영어 명의 스위치를 추가하였고, 이 스위치를 토글하면 UMP 동의 양식을 다시 열어 사용자가 동의/거부를 직접 선택할 수 있게 되어 있습니다.
- 권장 동작:
  - 이 기능은 사용자가 언제든지 동의를 변경할 수 있도록 하는 공식적이고 유효한 구현 방식입니다.
  - 스위치를 누르면 다음 흐름을 트리거하세요:
    1. `ConsentInformation.requestConsentInfoUpdate()`를 호출하여 최신 상태를 가져옵니다.
    2. 필요 시 `UserMessagingPlatform.loadConsentForm()`으로 양식을 로드한 뒤 `form.show()`로 표시합니다.
  - UI 노출 조건: 릴리스 빌드에서도 노출 가능(사용자 설정용)이지만, 스위치 자체가 디버그 전용 정보를 노출하지 않도록 주의하세요.

2) 디버그 메뉴의 "맞춤형 광고 재설정(Reset Personalized Ads)" 버튼

- 기능 설명: 디버그 메뉴에 "맞춤형 광고 재설정" 버튼을 두어, UMP 상태를 초기화(앱이 마치 한 번도 동의 팝업을 띄운 적이 없는 상태)하는 기능을 구현해 두셨습니다.
- 권장 동작 및 안전 규칙:
  - 이 버튼은 반드시 디버그 전용으로 노출하세요 (예: `BuildConfig.DEBUG` 확인). 릴리스 빌드에 포함되면 안 됩니다.
  - 동작 구현 예시:
    - 로컬 저장소(SharedPreferences 등)와 SDK 내부 저장 상태를 모두 초기화합니다.
    - 가능한 경우 광고 SDK의 관련 초기화 플래그도 재설정한 뒤 앱을 재시작하거나 UMP 초기화 로직을 다시 실행하세요.
  - 사용자 데이터 유출 위험: 이 버튼 자체는 민감정보를 외부로 전송하지 않으므로 유출 위험은 낮지만, 실수로 릴리스에 포함되면 사용자 경험과 규정 준수에 문제를 일으킬 수 있습니다.

3) 해시된 테스트 기기 ID 관리(보안 권고)

- 해시된 기기 ID 자체는 일반 텍스트 API 키처럼 민감한 값은 아니지만, 기기 식별자와 연관되므로 공개 저장소에 하드코딩하지 마세요.
- 권장 방식:
  - `local.properties`에 `UMP_TEST_DEVICE_HASH=` 항목으로 추가하고 절대 커밋하지 마십시오.
  - Gradle에서 이 값을 읽어 `BuildConfig`의 디버그 전용 필드로 주입한 뒤 앱에서 사용하세요.
- 예시 (요약):
  - `local.properties`에 다음을 추가합니다:

    UMP_TEST_DEVICE_HASH=YOUR_TEST_DEVICE_HASHED_ID

  - `build.gradle.kts`에서 `buildConfigField`로 주입하여 앱 코드에서는 `BuildConfig.UMP_TEST_DEVICE_HASH`로 읽습니다.

-----

### 6. 코드 예시(안전한 테스트 기기 해시 주입 및 탭05 / 디버그 버튼 사용 흐름)

- Gradle(KTS) 예시 (local.properties를 읽어 `BuildConfig`에 주입):

```kotlin
// app/build.gradle.kts (요약 예시)
val localProps = java.util.Properties().apply {
    file(rootProject.file("local.properties")).inputStream().use { load(it) }
}

android {
    // ...existing code...
    defaultConfig {
        // ...existing code...
        buildConfigField("String", "UMP_TEST_DEVICE_HASH", "\"${localProps.getProperty("UMP_TEST_DEVICE_HASH") ?: ""}\"")
    }
}
```

- Kotlin: Debug 전용 UMP 테스트 설정 및 탭05 스위치/디버그 리셋 흐름(요약)

```kotlin
// ...existing code...
if (BuildConfig.DEBUG) {
    val testHash = BuildConfig.UMP_TEST_DEVICE_HASH
    if (testHash.isNotEmpty()) {
        val debugSettings = ConsentDebugSettings.Builder(context)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId(testHash)
            .build()

        val params = ConsentRequestParameters.Builder()
            .setConsentDebugSettings(debugSettings)
            .build()

        ConsentInformation.getInstance(context).requestConsentInfoUpdate(
            activity,
            params,
            { // 성공: 필요 시 양식 로드
                // UserMessagingPlatform.loadConsentForm(...) 후 show()
            },
            { formError ->
                // 오류 처리: 로그/대체 로직
            }
        )
    }
}

// 탭05에 있는 Personalized Ads 스위치 핸들러 요약
fun onPersonalizedAdsToggleClicked() {
    // 최신 상태 요청
    ConsentInformation.getInstance(context).requestConsentInfoUpdate(
        activity,
        params,
        { // 성공: 양식 로드 후 표시
            // UserMessagingPlatform.loadConsentForm(...)
            // form.show(activity)
        },
        { /* 오류 처리 */ }
    )
}

// 디버그 메뉴: 맞춤형 광고 재설정 버튼(디버그 전용)
fun resetPersonalizedAdsDebug() {
    if (!BuildConfig.DEBUG) return
    // SharedPreferences 등에서 동의 상태 키 삭제
    // ConsentInformation.getInstance(context).reset() // SDK에 reset API가 없을 수 있음 -> 로컬 저장소 초기화 + 앱 재시작 권장
}
// ...existing code...
```

-----

### 7. 문서에 반영된 변경 요약 (요구사항 대비)

- 탭05 "Personalized Ads" 스위치의 동작과 권장 흐름을 문서에 추가했습니다. (Done)
- 디버그 메뉴의 "맞춤형 광고 재설정(Reset Personalized Ads)" 버튼 설명과 안전 규칙을 추가했습니다. (Done)
- 테스트 기기 해시의 보안 권고(`local.properties` 사용 및 BuildConfig 주입)와 예시 코드를 추가했습니다. (Done)

---

궁금한 점이나 추가로 문서에 넣을 샘플 코드(실제 파일 경로에 맞춘 예시)가 필요하면 알려주세요. 파일도 바로 수정해 드립니다.

-----

### 8. 테스트 기기 해시 찾는 방법 (로그에서 해시 추출)

UMP 테스트 기기 해시는 보통 디버그 로그(logcat)에 다음과 같은 형태로 출력됩니다:

- 로그 예시 라인 (확인할 문자열):
  - "Use new ConsentDebugSettings.Builder().addTestDeviceHashedId(\"<해시>\")" 

절차 (중학생 수준으로 간단하게):

1) 기기(또는 에뮬레이터)를 연결하고 디버그 빌드로 앱을 실행하세요.

2) PowerShell에서 로그를 실시간으로 확인하세요. (예시: 에뮬레이터 ID를 명시)

adb -s emulator-5554 logcat -v time | findstr UMP


3) 로그에서 위 예시 문자열을 찾으면 따옴표 안의 값이 해시된 기기 ID입니다. 이 값을 복사합니다.

4) `local.properties` 파일을 열고 `UMP_TEST_DEVICE_HASH` 항목의 값으로 붙여넣습니다. 예:

UMP_TEST_DEVICE_HASH=ab12cd34ef56...  # 실제 해시를 여기에 넣으세요 (절대 커밋 금지)


보안 주의사항:

- 해시 값은 기기 식별자와 연관되므로 공개 저장소에 절대 커밋하지 마세요.
- `local.properties`는 로컬 전용입니다. 저장소 추적에서 제외되어 있는지(`.gitignore`에 포함) 반드시 확인하세요.


### 9. 탭05("Personalized Ads") 및 디버그 재설정 기능 요약

- 탭05의 "Personalized Ads" 스위치:
  - 동작: 사용자가 이 스위치를 탭하면 UMP의 Privacy Options(또는 전체 동의 폼)를 다시 열어 동의/거부를 선택할 수 있도록 합니다.
  - 구현 요약: `ConsentInformation.requestConsentInfoUpdate()` 호출 후 필요하면 `UserMessagingPlatform.loadConsentForm()` 또는 `showPrivacyOptionsForm()`을 사용하여 폼을 표시합니다.

- 디버그 메뉴의 "Reset Personalized Ads" 버튼:
  - 동작: 디버그 전용으로 UMP 관련 로컬 저장값(IAB TC 문자열, 앱의 UMP prefs 등)을 초기화하여 앱이 동의 팝업을 처음 보는 상태처럼 만듭니다.
  - 구현 권장:
    - `BuildConfig.DEBUG` 체크로 디버그 전용으로만 노출하세요.
    - SharedPreferences(`IABTCF_*`, `ump_prefs` 등)를 초기화하고 필요 시 앱을 재시작하거나 초기화 로직을 재실행하세요.


### 10. 문서 반영 요약

- 로그에서 해시 추출 방법을 추가했습니다. (Done)
- `local.properties`에 해시 적용 예시와 보안 주의문구를 명시했습니다. (Done)
- 탭05("Personalized Ads") 및 디버그 리셋 흐름을 문서에 명시했습니다. (Done)
