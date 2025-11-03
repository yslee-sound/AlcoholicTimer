# 릴리즈 빌드 광고 검증 가이드

## 🎯 목적

릴리즈 빌드에서 디버그 전용 기능(예: 배너 광고 숨기기)이 실수로 활성화되어 출시되는 것을 방지합니다.

---

## 🛡️ 다층 방어 시스템

### 1단계: 자동 Gradle 검증 ✅

릴리즈 빌드 실행 시 자동으로 다음을 검증합니다:

```bash
# 모든 릴리즈 빌드 명령에서 자동 실행됨
./gradlew assembleRelease    # ← verifyReleaseAdConfig 자동 실행
./gradlew bundleRelease      # ← verifyReleaseAdConfig 자동 실행
```

**검증 항목:**
- ✅ `DebugAdHelper.kt`에 `BuildConfig.DEBUG` 체크 존재
- ✅ `BaseActivity.kt`에 `BuildConfig.DEBUG` 체크 존재  
- ✅ `StandardScreen.kt`에 `BuildConfig.DEBUG` 체크 존재
- ✅ `DetailActivity.kt`에 `BuildConfig.DEBUG` 체크 존재
- ✅ 릴리즈 빌드에 실제 광고 유닛 ID 설정

**실패 시:**
```
❌ ERROR: BaseActivity.kt의 shouldHideBanner 로직에 BuildConfig.DEBUG 체크가 없습니다!
```
→ 빌드가 **자동으로 중단**됩니다.

---

### 2단계: 빌드 스크립트 체크리스트 ✅

`build_release.ps1` 실행 시 수동 체크리스트를 제공합니다:

```powershell
.\build_release.ps1
```

**확인 항목:**
1. VersionCode와 VersionName 업데이트 확인
2. CHANGELOG.md 업데이트 확인
3. **디버그 기능 비활성화 확인** ← 광고 관련
4. 광고 유닛 ID 확인
5. 로그/디버그 메시지 제거 확인

사용자가 `Y`를 입력해야만 빌드가 진행됩니다.

---

### 3단계: Unit Test 검증 ✅

릴리즈 테스트 실행:

```bash
./gradlew testReleaseUnitTest
```

**테스트 파일:** `app/src/test/java/kr/sweetapps/alcoholictimer/ReleaseAdConfigTest.kt`

**검증 내용:**
- BuildConfig.DEBUG가 false인지 확인
- 광고 유닛 ID가 테스트 ID가 아닌지 확인
- 모든 광고 타입의 ID가 설정되어 있는지 확인

---

## 🔧 수동 검증 방법

### Quick Check

릴리즈 빌드 전에 다음 명령으로 빠르게 검증:

```bash
./gradlew verifyReleaseAdConfig
```

### 소스 코드 검증

다음 파일들을 열어서 `BuildConfig.DEBUG` 체크가 있는지 확인:

#### ✅ BaseActivity.kt
```kotlin
// 이 패턴이 있어야 함
var shouldHideBanner by remember { 
    mutableStateOf(
        if (BuildConfig.DEBUG) DebugAdHelper.bannerHiddenFlow.value else false
    ) 
}

if (BuildConfig.DEBUG) {
    LaunchedEffect(Unit) { ... }
}
```

#### ✅ StandardScreen.kt
```kotlin
// 동일한 패턴
var shouldHideBanner by remember { 
    mutableStateOf(
        if (BuildConfig.DEBUG) DebugAdHelper.bannerHiddenFlow.value else false
    ) 
}

if (BuildConfig.DEBUG) {
    LaunchedEffect(Unit) { ... }
}
```

#### ✅ DetailActivity.kt
```kotlin
// 동일한 패턴
var shouldHideBanner by remember { 
    mutableStateOf(
        if (BuildConfig.DEBUG) DebugAdHelper.bannerHiddenFlow.value else false
    ) 
}

if (BuildConfig.DEBUG) {
    androidx.compose.runtime.LaunchedEffect(Unit) { ... }
}
```

---

## ⚠️ 일반적인 실수

### ❌ 잘못된 코드
```kotlin
// BuildConfig.DEBUG 체크 없음 - 릴리즈에서도 광고가 숨겨질 수 있음!
var shouldHideBanner by remember { 
    mutableStateOf(DebugAdHelper.bannerHiddenFlow.value) 
}

LaunchedEffect(Unit) {
    DebugAdHelper.bannerHiddenFlow.collect { hidden ->
        shouldHideBanner = hidden
    }
}
```

### ✅ 올바른 코드
```kotlin
// 릴리즈에서는 항상 false
var shouldHideBanner by remember { 
    mutableStateOf(
        if (BuildConfig.DEBUG) DebugAdHelper.bannerHiddenFlow.value else false
    ) 
}

// Flow 구독도 디버그에서만
if (BuildConfig.DEBUG) {
    LaunchedEffect(Unit) {
        DebugAdHelper.bannerHiddenFlow.collect { hidden ->
            shouldHideBanner = hidden
        }
    }
}
```

---

## 📋 릴리즈 체크리스트

릴리즈 빌드 전에 다음을 확인하세요:

- [ ] `./gradlew verifyReleaseAdConfig` 실행 성공
- [ ] `./gradlew testReleaseUnitTest` 실행 성공 (가능하다면)
- [ ] 소스 코드에서 `BuildConfig.DEBUG` 체크 육안 확인
- [ ] 테스트 기기에서 광고가 표시되는지 확인
- [ ] `build_release.ps1` 체크리스트 모두 확인

---

## 🚨 문제 발생 시

### 릴리즈에서 광고가 표시되지 않는 경우

1. **Gradle 검증 실행:**
   ```bash
   ./gradlew verifyReleaseAdConfig
   ```

2. **로그 확인:**
   ```bash
   adb logcat | grep -E "(AdmobBanner|DebugAdHelper|shouldHideBanner)"
   ```

3. **BuildConfig 확인:**
   릴리즈 APK/AAB 디컴파일하여 `BuildConfig.DEBUG`가 `false`인지 확인

4. **코드 검토:**
   위의 "수동 검증 방법" 섹션 참조

---

## 🔄 CI/CD 통합

GitHub Actions 또는 다른 CI에서 사용:

```yaml
- name: Verify Release Ad Configuration
  run: ./gradlew verifyReleaseAdConfig

- name: Run Release Unit Tests
  run: ./gradlew testReleaseUnitTest

- name: Build Release AAB
  run: ./gradlew bundleRelease
```

---

## 📚 관련 문서

- `docs/a_AD_POLICY_COMPLIANCE_REVIEW.md` - 광고 정책 검토
- `docs/a_PRODUCTION_RELEASE_VALIDATION.md` - 프로덕션 릴리즈 검증
- `docs/a_RELEASE_READY_SUMMARY.md` - 릴리즈 준비 요약

---

**마지막 업데이트:** 2025-11-04
**버전:** 1.0

