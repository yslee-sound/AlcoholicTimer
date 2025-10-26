# 앱 오프닝 광고 적용 완료

## 적용된 변경사항

### 1. 새로 생성된 파일
- **`app/src/main/java/com/example/alcoholictimer/core/ads/AppOpenAdManager.kt`**
  - 앱 오프닝 광고 관리 클래스
  - 생명주기 기반 자동 광고 표시
  - 일일 제한, 쿨다운 정책 적용
  - 콜드 스타트 보호 기능

- **`docs/APP_OPEN_AD_GUIDE.md`**
  - 상세한 구현 가이드
  - 테스트 방법
  - 문제 해결 가이드

### 2. 수정된 파일

#### `app/build.gradle.kts`
```kotlin
// 의존성 추가
implementation("androidx.lifecycle:lifecycle-process:2.9.4")

// Debug 빌드
buildConfigField("String", "ADMOB_APP_OPEN_UNIT_ID", "\"ca-app-pub-3940256099942544/9257395921\"")

// Release 빌드 (실제 ID로 교체 필요)
buildConfigField("String", "ADMOB_APP_OPEN_UNIT_ID", "\"ca-app-pub-8420908105703273/REPLACE_WITH_REAL_APP_OPEN\"")
```

#### `app/src/main/java/com/example/alcoholictimer/MainApplication.kt`
```kotlin
class MainApplication : Application() {
    private lateinit var appOpenAdManager: AppOpenAdManager
    
    override fun onCreate() {
        super.onCreate()
        
        // ... MobileAds 초기화 ...
        MobileAds.initialize(this) { initStatus ->
            // 초기화 완료 후 앱 오프닝 광고 로드
            appOpenAdManager.loadAd()
        }
        
        // 앱 오프닝 광고 매니저 초기화
        appOpenAdManager = AppOpenAdManager(this)
        appOpenAdManager.resetColdStart()
        
        // ... 기존 코드 ...
    }
}
```

## 주요 기능

### 자동 광고 표시
- 앱이 백그라운드에서 포그라운드로 전환될 때 자동으로 광고 표시
- `ProcessLifecycleOwner`를 통한 앱 생명주기 추적

### 콜드 스타트 보호
- 앱 최초 실행 시에는 광고를 표시하지 않음
- 스플래시 화면과의 충돌 방지

### 스마트 정책
- **일일 제한**: 최대 5회/일
- **쿨다운**: 광고 간 최소 5분 간격
- **광고 유효 시간**: 4시간
- **디버그 모드**: 정책 우회로 테스트 용이

### 자동 관리
- 광고 표시 후 자동으로 다음 광고 프리로드
- 만료된 광고 자동 교체

## 다음 단계

### 1. Gradle 동기화
Android Studio에서:
```
File → Sync Project with Gradle Files
```

### 2. 테스트
1. Debug 빌드로 앱 실행
2. 홈 버튼으로 백그라운드 이동
3. 최근 앱 목록에서 앱 재진입
4. 테스트 광고 확인

### 3. Release 배포 전
1. AdMob 콘솔에서 앱 오프닝 광고 유닛 생성
2. `app/build.gradle.kts`의 Release 빌드에 실제 광고 유닛 ID 설정
3. 실제 기기에서 테스트

## 로그 확인
```bash
adb logcat -s AppOpenAdManager
```

주요 로그:
- `Loading app open ad` - 광고 로드 중
- `App open ad loaded successfully` - 로드 성공
- `Skipping ad on cold start` - 콜드 스타트 스킵
- `Blocked by policy: dailycap` - 일일 제한
- `Blocked by policy: cooldown` - 쿨다운
- `App open ad showed` - 광고 표시

## 정책 커스터마이징
`AppOpenAdManager.kt`에서 상수 수정:
```kotlin
private const val DEFAULT_DAILY_CAP = 5              // 일일 최대 횟수
private const val DEFAULT_COOLDOWN_MS = 5 * 60 * 1000L  // 쿨다운 (밀리초)
private const val AD_TIMEOUT_MS = 4 * 60 * 60 * 1000L   // 유효 시간
```

## 참고 문서
- `docs/APP_OPEN_AD_GUIDE.md` - 상세 가이드
- [Google AdMob - App Open Ads](https://developers.google.com/admob/android/app-open)

