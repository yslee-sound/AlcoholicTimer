# 릴리즈 빌드 테스트 광고 미표시 분석 보고서

**날짜**: 2025-12-17  
**문제**: 릴리즈 빌드에서 테스트 기기로 등록했는데도 "테스트" 마크가 표시되지 않음  
**원인**: 의도적인 보안 설정

---

## 🔍 문제 원인

### build.gradle.kts 분석 결과

**Line 64-65: 릴리즈 태스크 감지**
```kotlin
val isReleaseTaskRequested: Boolean = gradle.startParameter.taskNames.any { name ->
    val lower = name.lowercase()
    ("release" in lower && ("assemble" in lower || "bundle" in lower || "publish" in lower)) 
    || lower.endsWith("release")
}
```

**Line 67-70: 테스트 ID 강제 제거**
```kotlin
// 안전: 릴리즈 관련 태스크가 요청된 경우 디버그 전용 해시를 빈값으로 강제
val debugUmpTestDeviceHash = if (isReleaseTaskRequested) "" else umpTestDeviceHash
val debugAdMobTestDeviceId = if (isReleaseTaskRequested) "" else adMobTestDeviceId
```

**Line 178-179: Release 빌드에서 강제 초기화**
```kotlin
// [NEW] 테스트 기기 설정 오버라이드 (Release에서는 빈 문자열)
buildConfigField("String", "UMP_TEST_DEVICE_HASH", "\"\"")
buildConfigField("String", "ADMOB_TEST_DEVICE_ID", "\"\"")
```

---

## ⚠️ 왜 이렇게 설계되었나?

### 보안 이유

1. **실수 방지**
   - 릴리즈 APK/AAB에 테스트 기기 ID가 포함되면 안 됨
   - 디컴파일로 테스트 ID 노출 가능

2. **AdMob 정책 준수**
   - 릴리즈 빌드는 실제 광고를 표시해야 함
   - 테스트 광고는 개발 중에만 사용

3. **계정 보호**
   - 실수로 테스트 ID가 포함된 채로 배포되는 것 방지
   - 사용자들이 테스트 광고를 보는 상황 방지

---

## 🎯 현재 상황 정리

### 릴리즈 빌드 실행 시

```
1. 앱 빌드: bundleRelease / assembleRelease
   ↓
2. build.gradle.kts 감지
   ↓
3. debugUmpTestDeviceHash = "" (강제)
   debugAdMobTestDeviceId = "" (강제)
   ↓
4. BuildConfig.UMP_TEST_DEVICE_HASH = ""
   BuildConfig.ADMOB_TEST_DEVICE_ID = ""
   ↓
5. MainApplication.kt
   testDeviceIds = [] (빈 리스트)
   ↓
6. AdMob: 테스트 기기 설정 없음
   ↓
7. 결과: 실제 광고 표시 (테스트 마크 없음)
```

---

## ✅ 이것은 정상입니다!

### 릴리즈 빌드는 실제 광고를 표시해야 합니다

**AdMob 정책**:
- 릴리즈 빌드 = 실제 광고
- 디버그 빌드 = 테스트 광고

**현재 동작**:
- ✅ 릴리즈: 실제 광고 (테스트 마크 없음) ← **정상**
- ✅ 디버그: 테스트 광고 (테스트 마크 있음)

---

## 🧪 테스트 방법

### 방법 1: 디버그 빌드로 테스트 (권장)

```powershell
# 디버그 빌드
.\gradlew assembleDebug

# 또는 Android Studio에서
Run → Run 'app' (Shift+F10)
```

**결과**:
- ✅ 테스트 광고 표시
- ✅ "테스트" 마크 표시
- ✅ 클릭해도 수익 발생 안 함 (안전)

---

### 방법 2: 릴리즈 빌드에서 본인 광고 클릭하지 않기 (주의!)

**릴리즈 빌드 특징**:
```
- 실제 광고 표시됨
- 테스트 마크 없음
- 클릭 시 실제 수익 발생
- ⚠️ 본인 광고 클릭 시 계정 정지 위험!
```

**안전한 테스트 방법**:
1. 광고가 표시되는지 **시각적으로만** 확인
2. 광고를 **절대 클릭하지 않음**
3. 광고 영역만 스크린샷 촬영
4. 즉시 앱 종료

---

### 방법 3: 릴리즈 빌드에서도 테스트 광고 보려면 (비추천)

**build.gradle.kts 수정 (임시)**:

```kotlin
// Line 178-179 주석 처리
// buildConfigField("String", "UMP_TEST_DEVICE_HASH", "\"\"")
// buildConfigField("String", "ADMOB_TEST_DEVICE_ID", "\"\"")

// 또는 조건 추가
buildConfigField("String", "UMP_TEST_DEVICE_HASH", 
    if (project.hasProperty("allowTestInRelease")) "\"$umpTestDeviceHash\"" else "\"\"")
```

**빌드 시**:
```powershell
.\gradlew assembleRelease -PallowTestInRelease
```

**⚠️ 주의**:
- 절대 이 상태로 Play Store 배포 금지!
- 테스트 후 즉시 원래대로 되돌리기
- Git에 커밋하지 않기

---

## 📊 빌드 타입별 비교

| 항목 | Debug | Release |
|------|-------|---------|
| **Application ID** | kr.sweetapps.alcoholictimer.debug | kr.sweetapps.alcoholictimer |
| **테스트 기기 ID** | local.properties에서 로드 | **빈 문자열 (강제)** |
| **광고 종류** | 테스트 광고 | 실제 광고 |
| **테스트 마크** | ✅ 표시됨 | ❌ 표시 안 됨 |
| **클릭 안전** | ✅ 안전 (수익 없음) | ⚠️ 위험 (계정 정지) |
| **용도** | 개발/테스트 | 배포/사용자 |

---

## 🎯 권장 사항

### 1. 일반적인 개발/테스트

**디버그 빌드 사용**:
```powershell
# Android Studio
Run → Run 'app'

# 또는 Gradle
.\gradlew installDebug
```

**장점**:
- ✅ 테스트 광고 표시
- ✅ 안전하게 클릭 가능
- ✅ 빠른 빌드
- ✅ 디버깅 가능

---

### 2. 릴리즈 테스트 (Play Store 배포 전)

**시각적 확인만**:
```
1. assembleRelease 빌드
2. 앱 설치
3. 광고 영역만 확인 (시각적으로)
4. 광고 절대 클릭하지 않기!
5. 레이아웃/크기/위치만 확인
6. 즉시 종료
```

**실제 동작 테스트**:
- Internal Testing 트랙 사용
- 테스터 계정으로 테스트
- 본인 계정으로는 클릭 금지

---

### 3. Play Store 배포 후

**실제 사용자 테스트**:
- 테스터 계정으로만 확인
- AdMob 대시보드에서 노출 확인
- 본인은 광고 클릭 절대 금지

---

## ❓ FAQ

### Q1: 왜 릴리즈 빌드에서 테스트 광고를 볼 수 없나요?

**A**: 보안과 AdMob 정책 준수를 위한 의도적인 설계입니다.
- 실수로 테스트 ID가 배포되는 것 방지
- 릴리즈 = 실제 광고가 원칙

---

### Q2: 릴리즈 빌드를 테스트하고 싶어요

**A**: 디버그 빌드를 사용하거나, 시각적 확인만 하세요.
- **권장**: 디버그 빌드로 기능 테스트
- **릴리즈**: 레이아웃만 시각적으로 확인, 클릭 금지

---

### Q3: 실수로 릴리즈 빌드에서 광고를 클릭했어요

**A**: 1-2회는 괜찮지만, 반복되면 위험합니다.
- 즉시 중단
- 이후 디버그 빌드만 사용
- AdMob 대시보드에서 "무효 클릭" 확인
- 반복 시 계정 정지 가능

---

### Q4: Play Store 배포 전에 릴리즈 빌드를 완전히 테스트하고 싶어요

**A**: Internal Testing 트랙을 사용하세요.
```
1. Play Console → Internal Testing
2. AAB 업로드
3. 테스터 이메일 추가
4. 테스터 계정으로 테스트
```

---

## 🎉 결론

### 현재 상황은 정상입니다!

**릴리즈 빌드에서 테스트 마크가 없는 것은**:
- ✅ **의도된 동작**입니다
- ✅ **보안을 위한 설계**입니다
- ✅ **AdMob 정책에 부합**합니다

### 개발 중에는

**디버그 빌드를 사용하세요**:
```powershell
.\gradlew installDebug
```

**또는 Android Studio**:
```
Run → Run 'app' (Shift+F10)
```

### 배포 전에는

**Internal Testing으로 확인하세요**:
- Play Console 업로드
- 테스터 계정 사용
- 본인 광고 클릭 절대 금지

---

**작성일**: 2025-12-17  
**상태**: ✅ 정상 동작 확인  
**권장**: 디버그 빌드 사용

