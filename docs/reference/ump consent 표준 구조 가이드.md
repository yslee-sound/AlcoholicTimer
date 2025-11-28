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

UMP 동의 구현과 관련된 특정 오류 메시지가 있으신가요? 오류 메시지를 알려주시면 문제 해결 가이드를 추가로 제공해 드리겠습니다。
