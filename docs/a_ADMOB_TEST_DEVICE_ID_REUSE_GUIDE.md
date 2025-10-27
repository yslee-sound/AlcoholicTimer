# 여러 앱에서 동일한 AdMob 테스트 기기 ID 재사용 가이드

작성일: 2025-10-27  
중요도: 🔴 매우 중요 (계정 안전/테스트 생산성)

---

## 목적
- 하나의(또는 여러 대의) 테스트 기기 ID를 여러 Android 앱(다른 패키지)에도 일관되게 적용하는 절차를 정리합니다.
- 본인/가족 기기를 모두 테스트 기기로 등록해 실수 클릭으로 인한 계정 정지 리스크를 제거합니다.

관련 문서
- 테스트 기기 ID 확인: [a_HOW_TO_GET_DEVICE_ID.md](./a_HOW_TO_GET_DEVICE_ID.md)
- 광고 안전 가이드(프로덕션에서 안전 사용): [a_INTERNAL_TEST_AD_GUIDE.md](./a_INTERNAL_TEST_AD_GUIDE.md)

---

## 빠른 체크리스트
1) 기기 ID를 Logcat으로 확인한다.  
2) 각 앱의 Application(onCreate)에서 MobileAds RequestConfiguration에 `setTestDeviceIds()`로 등록한다.  
3) 릴리즈 빌드에도 동일하게 등록한다.  
4) 여러 앱에서 공통으로 쓰려면 중앙 관리(옵션 B 또는 C)로 중복을 없앤다.  
5) 빌드/설치 후 배너 상단에 "Test Ad" 표기를 확인한다.

---

## 방법 A: 각 앱 코드에 직접 추가 (가장 단순)
- 각 앱의 Application 클래스에서 MobileAds 초기화 전에 RequestConfiguration으로 테스트 기기를 등록합니다.

예시 (Kotlin):
```kotlin
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val testDeviceIds = if (BuildConfig.DEBUG) {
            listOf(
                "79DB2DA46501DFD953D9222E13384F99" // 본인 기기
                // "ANOTHER_DEVICE_ID" // 가족/태블릿 등 추가 시
            )
        } else {
            // 프로덕션에서도 본인/가족 기기는 반드시 테스트 기기로 등록 (권장)
            listOf(
                "79DB2DA46501DFD953D9222E13384F99"
            )
        }

        val config = RequestConfiguration.Builder()
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
            .apply {
                if (testDeviceIds.isNotEmpty()) setTestDeviceIds(testDeviceIds)
            }
            .build()
        MobileAds.setRequestConfiguration(config)

        MobileAds.initialize(this) { /* ... */ }
    }
}
```
장단점  
- 장점: 가장 간단, 바로 적용 가능  
- 단점: 여러 앱에서 동일 코드/ID를 중복 편집해야 함

---

## 방법 B: 중앙 관리(권장) – local.properties로 공유
여러 앱에서 동일한 ID 목록을 공유하려면, 루트의 `local.properties`에 정의하고 각 앱 `build.gradle.kts`에서 읽어 `BuildConfig`로 넘기거나 런타임에 직접 사용합니다.

1) 루트 `local.properties`에 추가(예시):
```
admob.testDeviceIds=79DB2DA46501DFD953D9222E13384F99
# 여러 대 등록 시: 콤마로 구분
# admob.testDeviceIds=79DB2DA46501DFD953D9222E13384F99,ANOTHER_DEVICE_ID
```
- 주의: `local.properties`는 보통 VCS에 커밋되지 않아 개인화된 값 관리에 적합합니다.

2) 각 앱 `app/build.gradle.kts`에서 BuildConfig로 주입(예시):
```kotlin
import java.util.Properties

val props = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val admobTestIdsCsv = props.getProperty("admob.testDeviceIds", "")

android {
    buildTypes {
        debug {
            buildConfigField("String", "ADMOB_TEST_DEVICE_IDS_CSV", "\"$admobTestIdsCsv\"")
        }
        release {
            buildConfigField("String", "ADMOB_TEST_DEVICE_IDS_CSV", "\"$admobTestIdsCsv\"")
            // 프로덕션에서도 본인 기기 등록 유지 권장
        }
    }
}
```

3) Application 코드에서 사용(예시):
```kotlin
val testDeviceIds = BuildConfig.ADMOB_TEST_DEVICE_IDS_CSV
    .split(',')
    .map { it.trim() }
    .filter { it.isNotEmpty() }

val config = RequestConfiguration.Builder()
    .apply { if (testDeviceIds.isNotEmpty()) setTestDeviceIds(testDeviceIds) }
    .build()
MobileAds.setRequestConfiguration(config)
```
장단점  
- 장점: 여러 앱이 동일한 소스에서 값을 가져와 일관성/재사용성 최고  
- 단점: 각 앱 그레이들 파일에 1회 보일러플레이트 필요

---

## 방법 C: 공통 헬퍼/모듈로 캡슐화
여러 앱이 같은 멀티-모듈 리포에서 개발 중이라면, `ads-core` 같은 공통 모듈을 만들고 아래 유틸로 통일합니다.

```kotlin
object AdsTestDeviceConfig {
    fun currentIds(): List<String> = listOf(
        // 중앙 1곳만 수정
        "79DB2DA46501DFD953D9222E13384F99"
        // ,"ANOTHER_DEVICE_ID"
    )
}
```
Application에서:
```kotlin
val config = RequestConfiguration.Builder()
    .apply { setTestDeviceIds(AdsTestDeviceConfig.currentIds()) }
    .build()
MobileAds.setRequestConfiguration(config)
```
- 장점: 각 앱에서는 호출만, 소스 1곳에서 관리  
- 단점: 별도 모듈 배포/버전 동기화가 필요할 수 있음

---

## 검증 방법
1) 앱 설치/실행  
2) 배너 상단에 "Test Ad" 표시 확인  
3) Logcat에서 다음 메시지 확인(선택):
```
I/Ads: This request is sent from a test device
```

Windows 빌드 예시:
```cmd
G:\Workspace\AlcoholicTimer> gradlew.bat :app:assembleRelease
```

---

## FAQ
- Q: Google 계정을 바꾸면 테스트 기기 ID도 바뀌나요?  
  A: 아닙니다. 기기(하드웨어) 기준으로 동일합니다. ([a_HOW_TO_GET_DEVICE_ID.md](./a_HOW_TO_GET_DEVICE_ID.md) 참고)

- Q: 릴리즈에도 등록해야 하나요?  
  A: 네. 본인/가족 기기는 릴리즈에도 등록해 실수 클릭 리스크를 제거하세요. (권장)

- Q: 여러 프로젝트(서로 다른 레포)에도 같은 ID를 쓰고 싶어요.  
  A: local.properties 방식이 가장 편리합니다. 각 레포의 루트 `local.properties`에 동일 키/값을 넣고, 동일한 Gradle 스니펫을 복사해 사용하세요.

---

## 요약
- 하나의 테스트 기기 ID는 여러 앱에 그대로 재사용 가능합니다.
- 가장 쉬운 방법은 각 앱 Application에 직접 추가(방법 A).  
- 팀/다앱 운영이면 local.properties 중앙 관리(방법 B) 또는 공통 모듈(방법 C)이 유지보수에 최적.
