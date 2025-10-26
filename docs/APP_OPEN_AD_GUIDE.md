# 앱 오프닝 광고 (App Open Ad) 구현 가이드

## 개요
구글 애드몹의 앱 오프닝 광고가 성공적으로 적용되었습니다. 앱이 백그라운드에서 포그라운드로 전환될 때 전면 광고를 표시합니다.

## 구현된 기능

### 1. AppOpenAdManager 클래스
- **위치**: `core/ads/AppOpenAdManager.kt`
- **역할**: 앱 오프닝 광고의 로드, 표시, 정책 관리

### 2. 주요 특징

#### 콜드 스타트 보호
- 앱 프로세스 시작 직후(콜드 스타트)에는 광고를 표시하지 않음
- 스플래시 화면과의 충돌 방지
- `resetColdStart()` 메서드로 플래그 초기화

#### 광고 정책
- **일일 노출 제한**: 최대 5회/일
- **쿨다운**: 광고 간 최소 5분 간격
- **광고 유효 시간**: 로드 후 4시간
- **디버그 모드**: 정책 우회 (테스트 용이)

#### 자동 관리
- `ProcessLifecycleOwner`를 통한 앱 생명주기 추적
- 포그라운드 전환 시 자동으로 광고 표시 시도
- 광고 표시 후 자동으로 다음 광고 프리로드

### 3. Build Configuration

#### Debug 빌드
```kotlin
buildConfigField("String", "ADMOB_APP_OPEN_UNIT_ID", "\"ca-app-pub-3940256099942544/9257395921\"")
```
- Google 테스트 앱 오프닝 광고 ID 사용

#### Release 빌드
```kotlin
buildConfigField("String", "ADMOB_APP_OPEN_UNIT_ID", "\"ca-app-pub-8420908105703273/REPLACE_WITH_REAL_APP_OPEN\"")
```
- **실제 배포 전**: `REPLACE_WITH_REAL_APP_OPEN` 부분을 실제 앱 오프닝 광고 유닛 ID로 교체

### 4. MainApplication 초기화

```kotlin
class MainApplication : Application() {
    private lateinit var appOpenAdManager: AppOpenAdManager
    
    override fun onCreate() {
        super.onCreate()
        
        // MobileAds 초기화
        MobileAds.initialize(this) { initStatus ->
            // 초기화 완료 후 앱 오프닝 광고 로드
            appOpenAdManager.loadAd()
        }
        
        // 앱 오프닝 광고 매니저 초기화
        appOpenAdManager = AppOpenAdManager(this)
        appOpenAdManager.resetColdStart()
    }
}
```

### 5. 의존성 추가

```gradle
implementation("androidx.lifecycle:lifecycle-process:2.9.4")
```

## 동작 흐름

1. **앱 시작**
   - `MainApplication.onCreate()`에서 `AppOpenAdManager` 초기화
   - `resetColdStart()` 호출로 콜드 스타트 플래그 설정
   - MobileAds 초기화 완료 후 첫 광고 로드

2. **백그라운드 → 포그라운드**
   - `ProcessLifecycleOwner`가 `onStart()` 이벤트 감지
   - `showAdIfAvailable()` 호출
   - 정책 체크 (콜드 스타트, 일일 제한, 쿨다운)
   - 조건 충족 시 광고 표시

3. **광고 표시 후**
   - 노출 기록 저장 (일일 카운트, 마지막 표시 시간)
   - 광고 객체 정리
   - 다음 광고 자동 프리로드

## 광고 정책 커스터마이징

`AppOpenAdManager.kt`에서 상수 값을 수정하여 정책 조정 가능:

```kotlin
companion object {
    private const val DEFAULT_DAILY_CAP = 5              // 일일 최대 노출 횟수
    private const val DEFAULT_COOLDOWN_MS = 5 * 60 * 1000L  // 쿨다운 시간 (밀리초)
    private const val AD_TIMEOUT_MS = 4 * 60 * 60 * 1000L   // 광고 유효 시간
}
```

## 로그 확인

Logcat에서 다음 태그로 필터링:
```
AppOpenAdManager
```

주요 로그 메시지:
- `Loading app open ad with unitId=...` - 광고 로드 시작
- `App open ad loaded successfully` - 광고 로드 성공
- `App open ad failed to load: ...` - 광고 로드 실패
- `Skipping ad on cold start` - 콜드 스타트로 인한 스킵
- `Blocked by policy: dailycap` - 일일 제한 도달
- `Blocked by policy: cooldown` - 쿨다운 기간 중
- `App open ad showed full screen content` - 광고 표시 성공
- `Policy bypassed (debug): showing ad` - 디버그 모드 (정책 우회)

## 테스트 방법

### 1. 기본 동작 테스트
1. 앱을 빌드하여 실행
2. 홈 버튼을 눌러 백그라운드로 이동
3. 최근 앱 목록에서 앱을 다시 선택
4. 앱 오프닝 광고가 표시되는지 확인

### 2. 콜드 스타트 테스트
1. 앱을 완전히 종료 (최근 앱 목록에서 제거)
2. 앱 아이콘을 탭하여 재시작
3. 광고가 **표시되지 않아야** 함 (콜드 스타트 보호)
4. 백그라운드 → 포그라운드 전환 시 광고 표시 확인

### 3. 정책 테스트 (Release 빌드)
- **일일 제한**: 5회 노출 후 더 이상 표시되지 않음
- **쿨다운**: 광고 표시 후 5분 이내에는 재표시되지 않음

### 4. 디버그 모드 (Debug 빌드)
- 정책이 우회되어 매번 광고 표시 시도
- 테스트 광고 ID 사용

## AdMob 콘솔 설정

### 1. 앱 오프닝 광고 유닛 생성
1. [AdMob 콘솔](https://apps.admob.com/) 접속
2. **앱 선택** → **광고 단위** → **광고 단위 추가**
3. **앱 오프닝 광고** 선택
4. 광고 단위 이름 입력 (예: "Main App Open")
5. **광고 단위 생성** 클릭

### 2. 광고 유닛 ID 적용
1. 생성된 광고 유닛 ID 복사 (예: `ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY`)
2. `app/build.gradle.kts` 파일 열기
3. Release 빌드타입에서 ID 교체:
   ```kotlin
   buildConfigField("String", "ADMOB_APP_OPEN_UNIT_ID", "\"ca-app-pub-8420908105703273/실제광고유닛ID\"")
   ```

## 주의사항

### 1. 사용자 경험
- 앱 오프닝 광고는 사용자 흐름을 방해할 수 있음
- 정책 값을 적절히 조정하여 과도한 노출 방지
- 콜드 스타트 보호는 필수적으로 유지

### 2. 다른 전면 광고와의 충돌
- Interstitial 광고와 동시에 표시되지 않도록 `isShowingAd` 플래그로 제어
- 필요 시 `InterstitialAdManager`와의 조율 로직 추가 고려

### 3. Activity 상태
- 앱이 백그라운드에 있는 동안 광고를 로드하지만 표시하지는 않음
- Activity가 finishing 또는 destroyed 상태일 때는 광고를 표시하지 않음

### 4. 메모리 관리
- 광고는 4시간 후 자동으로 만료
- 만료된 광고는 자동으로 새로 로드

## 문제 해결

### 광고가 표시되지 않는 경우
1. **Logcat 확인**: `AppOpenAdManager` 태그로 필터링
2. **광고 로드 실패**: 네트워크 연결, AdMob 계정 상태 확인
3. **정책 블록**: 일일 제한 또는 쿨다운 확인
4. **콜드 스타트**: 백그라운드 → 포그라운드 전환으로 테스트

### Gradle 동기화 오류
1. Android Studio에서 **File** → **Sync Project with Gradle Files**
2. 또는 터미널에서:
   ```bash
   ./gradlew --stop
   ./gradlew clean
   ```

### ProcessLifecycleOwner 오류
- `lifecycle-process` 의존성이 추가되었는지 확인
- Gradle 동기화 실행
- Android Studio 재시작

## 배포 체크리스트

- [ ] Release 빌드에 실제 앱 오프닝 광고 유닛 ID 설정
- [ ] AdMob 콘솔에서 앱 오프닝 광고 유닛 생성 및 활성화
- [ ] 정책 값 검토 및 조정 (일일 제한, 쿨다운)
- [ ] 테스트 기기에서 동작 확인
- [ ] 콜드 스타트 보호 동작 확인
- [ ] 다른 광고 형식과의 충돌 테스트
- [ ] AdMob 정책 준수 확인

## 참고 자료
- [Google AdMob - App Open Ads](https://developers.google.com/admob/android/app-open)
- [ProcessLifecycleOwner Documentation](https://developer.android.com/reference/androidx/lifecycle/ProcessLifecycleOwner)

