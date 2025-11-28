# AdMob 테스트 기기 등록 FAQ

**문서 버전**: 1.0.0  
**최초 작성**: 2025-10-26  
**최종 수정**: 2025-10-26  
**작성자**: AlcoholicTimer 개발팀  
**대상 독자**: Android 개발자, AdMob 사용자

---

## 📋 문서 이력

| 버전 | 날짜 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 1.0.0 | 2025-10-26 | 최초 작성 - Q1, Q2 FAQ 추가 | 개발팀 |

---

## 📖 목차

1. [Q1: 플레이 스토어에서 다운받아도 테스트 기기인가요?](#q1-플레이-스토어에서-다운받아도-테스트-기기인가요)
2. [Q2: 다른 앱에도 매번 하드코딩 해야 하나요?](#q2-다른-앱에도-매번-하드코딩-해야-하나요)
3. [부록: 테스트 기기 ID 찾는 방법](#부록-테스트-기기-id-찾는-방법)

---

## Q1: 플레이 스토어에서 다운받아도 테스트 기기인가요?

### ✅ 답변: 네, 100% 맞습니다!

테스트 기기로 등록된 폰은 **앱을 어떤 방법으로 설치해도 항상 테스트 기기**로 인식됩니다.

---

### 🔍 테스트 기기 작동 원리

#### 1. 테스트 기기 판별 기준

AdMob은 **기기의 고유 ID**를 기준으로 테스트 기기를 판별합니다.  
앱의 설치 방법이나 출처는 전혀 관계없습니다.

```kotlin
// MainApplication.kt 예시
val testDeviceIds = listOf(
    "79DB2DA46501DFD953D9222E13384F99"   // 이 기기 ID는 항상 테스트 기기
)

MobileAds.setRequestConfiguration(
    RequestConfiguration.Builder()
        .setTestDeviceIds(testDeviceIds)  // 기기 ID 기반 판별
        .build()
)
```

#### 2. 설치 방법별 테스트 기기 인식

| 설치 방법 | 테스트 기기 인식 | 광고 종류 | 수익 발생 |
|---------|--------------|----------|----------|
| **Play 스토어 다운로드** | ✅ 테스트 기기 | 테스트 광고 | ❌ 없음 |
| **APK 직접 설치** | ✅ 테스트 기기 | 테스트 광고 | ❌ 없음 |
| **Android Studio 실행** | ✅ 테스트 기기 | 테스트 광고 | ❌ 없음 |
| **내부 테스트 트랙** | ✅ 테스트 기기 | 테스트 광고 | ❌ 없음 |
| **프로덕션 릴리스** | ✅ 테스트 기기 | 테스트 광고 | ❌ 없음 |

**핵심**: 앱이 어디서 왔는지가 아니라, **폰의 기기 ID**만 중요합니다.

---

### 📊 테스트 광고 vs 실제 광고

| 구분 | 테스트 광고 (등록된 기기) | 실제 광고 (일반 사용자) |
|------|----------------------|-------------------|
| **광고 내용** | "Test Ad" 문구 표시 | 실제 광고주 제품 |
| **클릭 시 수익** | ❌ 0원 | ✅ 수익 발생 |
| **계정 정지 위험** | ❌ 없음 (안전) | ⚠️ 본인 클릭 시 정지 |
| **표시 빈도** | 항상 표시 가능 | 정책에 따라 제한 |
| **AdMob 집계** | 제외됨 | 포함됨 |

---

### 🎯 실제 시나리오 예시

#### AlcoholicTimer 앱의 경우

```kotlin
// MainApplication.kt - 릴리즈 빌드 설정
val testDeviceIds = listOf(
    "79DB2DA46501DFD953D9222E13384F99"   // 개발자 본인 폰
)
```

**개발자(나)의 폰에서**:
- Play 스토어에서 설치 → "Test Ad" 표시 ✅
- 광고 클릭 → 수익 0원, 안전함 ✅
- AdMob 대시보드 → 노출/클릭 집계 제외됨 ✅

**다른 사용자의 폰에서**:
- Play 스토어에서 설치 → 실제 광고 표시 ✅
- 광고 클릭 → 수익 발생 ✅
- AdMob 대시보드 → 노출/클릭 집계됨 ✅

---

### 🔬 실제 광고 확인 방법

테스트 기기에서는 실제 광고를 볼 수 없습니다. 확인하려면:

#### 방법 1: 다른 사람의 폰 사용 (권장 ⭐⭐⭐⭐⭐)

```
1. 가족/친구의 폰에서 Play 스토어로 앱 설치
2. 광고가 어떻게 보이는지 확인
3. ⚠️ 주의: 절대 광고를 클릭하지 말라고 당부!
```

#### 방법 2: AdMob 대시보드 확인 (안전 ⭐⭐⭐⭐⭐)

```
1. AdMob 콘솔 접속 (https://apps.admob.com)
2. 앱 선택 → 광고 유닛 선택
3. "광고 미리보기" 기능으로 실제 광고 샘플 확인
```

#### 방법 3: 테스트 기기 ID 제거 (⚠️ 위험! 비추천)

```kotlin
// 주의: 이렇게 하면 본인 폰도 실제 광고를 받게 됩니다!
val testDeviceIds = listOf(
    // "79DB2DA46501DFD953D9222E13384F99"  // 주석 처리 (위험!)
)
```

**⚠️ 경고**: 
- 실수로 광고 클릭 시 AdMob 계정 영구 정지 가능
- 테스트 완료 후 반드시 다시 추가해야 함
- **절대 추천하지 않습니다!**

---

### ✅ 테스트 기기 정상 작동 확인 방법

#### 1. Logcat 확인

```
D/Ads: This request is sent from a test device.
```
이 메시지가 보이면 → 테스트 기기로 정상 인식됨 ✅

#### 2. 광고 화면 확인

앱 실행 후 광고를 보세요:
- **"Test Ad"** 문구 표시 → 테스트 광고 (정상) ✅
- **Google 샘플 광고** → 테스트 광고 (정상) ✅
- 실제 제품 광고 → 실제 광고 (테스트 기기 아님) ⚠️

#### 3. AdMob 대시보드 확인

며칠 후 AdMob 대시보드를 확인하세요:

```
총 노출수: 1000회
├─ 본인의 폰: 50회 (집계 제외) → 수익: 0원
└─ 다른 사용자: 950회 (집계됨) → 수익: 발생함
```

---

### 🛡️ 테스트 기기 등록의 장점

| 장점 | 설명 |
|------|------|
| **안전성** | 실수로 광고 클릭해도 계정 정지 걱정 없음 |
| **편의성** | 자유롭게 앱 테스트 가능 (광고 클릭 OK) |
| **정확성** | AdMob 통계에서 본인 사용 데이터 제외 |
| **규정 준수** | Google AdMob 정책 완벽 준수 |

### ⚠️ 단점

| 단점 | 해결 방법 |
|------|----------|
| 실제 광고 미리보기 불가 | 다른 기기 사용 또는 AdMob 대시보드 활용 |
| 수익 테스트 불가 | 다른 사용자 통계로 확인 |

---

### 🔄 테스트 기기 등록 해제 (권장하지 않음)

#### 언제 해제해야 하나?

**❌ 절대 해제하지 마세요!**

- 계정 정지 위험이 너무 큽니다
- 실제 광고 확인은 다른 방법으로 하세요 (위 참조)

#### 정말로 해제하려면 (긴급 상황만)

```kotlin
// MainApplication.kt
val testDeviceIds = if (BuildConfig.DEBUG) {
    listOf("79DB2DA46501DFD953D9222E13384F99")
} else {
    // 릴리즈 빌드에서 제거 (⚠️ 매우 위험!)
    emptyList()  // 또는 listOf()
}
```

**⚠️ 다시 한번 경고**: 
1. 본인 광고 클릭 시 AdMob 계정 **영구 정지**
2. 기존 수익 **몰수** 가능
3. Google Play 개발자 계정 **제재** 가능

---

### 📝 Q1 요약

| 질문 | 답변 |
|------|------|
| Play 스토어에서 설치해도 테스트 기기? | ✅ 네, 항상 테스트 기기입니다 |
| APK 직접 설치는? | ✅ 네, 테스트 기기입니다 |
| 실제 광고를 볼 수 있나요? | ❌ 아니요, 항상 테스트 광고만 |
| 광고를 클릭해도 되나요? | ✅ 네, 안전합니다 (수익 없음) |
| 수익이 발생하나요? | ❌ 아니요, 0원입니다 |
| 다른 사용자는 실제 광고를 보나요? | ✅ 네, 정상적으로 실제 광고 표시 |

**결론: 걱정 마시고 자유롭게 앱을 사용하세요! 당신의 폰은 영구적으로 안전 모드입니다.** 🎉

---

## Q2: 다른 앱에도 매번 하드코딩 해야 하나요?

### ✅ 답변: 기본적으로 각 앱마다 설정해야 하지만, 더 똑똑한 방법이 있습니다!

각 앱은 독립적인 AdMob 설정을 가지므로 기기 ID를 지정해야 합니다.  
하지만 **하드코딩 없이 자동화**할 수 있습니다.

---

### 🎯 방법 비교

| 방법 | 장점 | 단점 | 추천도 | 적용 시간 |
|-----|------|------|--------|----------|
| **1. 하드코딩** | 간단함 | 매번 복사/붙여넣기 필요 | ⭐⭐ | 30초 |
| **2. local.properties** | Git 제외, 안전 | 각 앱마다 설정 | ⭐⭐⭐⭐ | 2분 |
| **3. 환경변수** | 한 번만 설정 | 초기 설정 필요 | ⭐⭐⭐⭐⭐ | 최초 5분, 이후 1분 |
| **4. Gradle 플러그인** | 완전 자동화 | 복잡함 | ⭐⭐⭐ | 10분 |

---

### 🏆 방법 1: 환경변수 사용 (최고 추천!)

**한 번만 설정하면 모든 프로젝트에서 자동으로 사용됩니다.**

#### 1단계: Windows 환경변수 설정 (최초 1회만)

##### 방법 A: GUI로 설정

```
1. Win + R → "sysdm.cpl" 입력 후 엔터
2. "고급" 탭 클릭
3. "환경 변수" 버튼 클릭
4. "사용자 변수" 영역에서 "새로 만들기" 클릭
5. 변수 정보 입력:
   - 변수 이름: ADMOB_TEST_DEVICE_ID
   - 변수 값: 79DB2DA46501DFD953D9222E13384F99
6. "확인" 클릭하여 모든 창 닫기
7. Android Studio 재시작
```

##### 방법 B: CMD로 설정

```cmd
REM 관리자 권한 CMD 실행 후
setx ADMOB_TEST_DEVICE_ID "79DB2DA46501DFD953D9222E13384F99"

REM PC 재시작 또는 Android Studio 재시작
```

##### 방법 C: PowerShell로 설정

```powershell
# 관리자 권한 PowerShell 실행 후
[System.Environment]::SetEnvironmentVariable(
    "ADMOB_TEST_DEVICE_ID", 
    "79DB2DA46501DFD953D9222E13384F99", 
    [System.EnvironmentVariableTarget]::User
)

# Android Studio 재시작
```

#### 2단계: build.gradle.kts 템플릿 (모든 프로젝트에 복사)

```kotlin
// app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.yourapp.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yourapp.example"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        // ✅ 환경변수에서 테스트 기기 ID 자동 로드
        val testDeviceId = System.getenv("ADMOB_TEST_DEVICE_ID") ?: ""
        buildConfigField("String", "TEST_DEVICE_ID", "\"$testDeviceId\"")
    }

    buildFeatures {
        buildConfig = true  // BuildConfig 생성 활성화
    }

    // ...existing config...
}

dependencies {
    implementation("com.google.android.gms:play-services-ads:23.4.0")
    // ...existing dependencies...
}
```

#### 3단계: MainApplication.kt 템플릿 (모든 프로젝트에 복사)

```kotlin
// MainApplication.kt

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // ✅ 테스트 기기 ID 자동 로드 (환경변수에서)
        val testDeviceIds = mutableListOf<String>()
        
        if (BuildConfig.TEST_DEVICE_ID.isNotBlank()) {
            testDeviceIds.add(BuildConfig.TEST_DEVICE_ID)
            Log.d(TAG, "✅ Test device registered: ${BuildConfig.TEST_DEVICE_ID}")
        } else {
            Log.w(TAG, "⚠️ No test device ID configured - 광고 클릭 주의!")
        }

        // AdMob 설정
        val config = RequestConfiguration.Builder()
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
            .apply {
                if (testDeviceIds.isNotEmpty()) {
                    setTestDeviceIds(testDeviceIds)
                }
            }
            .build()
        
        MobileAds.setRequestConfiguration(config)
        MobileAds.initialize(this) { initStatus ->
            Log.d(TAG, "AdMob initialized: $initStatus")
        }
    }
    
    companion object {
        private const val TAG = "MainApplication"
    }
}
```

#### 장점

- ✅ **한 번만 설정**하면 PC의 모든 프로젝트에 자동 적용
- ✅ 새 프로젝트 시작 시 템플릿만 복사하면 끝
- ✅ Git에 절대 노출 안 됨 (보안)
- ✅ 팀원 각자 자기 환경변수 설정 가능

---

### 🥈 방법 2: local.properties 사용 (차선책)

**프로젝트별로 다른 설정이 필요할 때 유용합니다.**

#### 1단계: local.properties 설정 (각 프로젝트마다)

```properties
# 프로젝트루트/local.properties

sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk

# ✅ 테스트 기기 ID 추가
test.device.id=79DB2DA46501DFD953D9222E13384F99
```

#### 2단계: build.gradle.kts 설정

```kotlin
// app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// ✅ local.properties 읽기
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}

android {
    namespace = "com.yourapp.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yourapp.example"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        // ✅ local.properties에서 테스트 기기 ID 로드
        val testDeviceId = localProperties.getProperty("test.device.id", "")
        buildConfigField("String", "TEST_DEVICE_ID", "\"$testDeviceId\"")
    }

    buildFeatures {
        buildConfig = true
    }

    // ...existing config...
}

dependencies {
    implementation("com.google.android.gms:play-services-ads:23.4.0")
    // ...existing dependencies...
}
```

#### 3단계: MainApplication.kt (방법 1과 동일)

```kotlin
// MainApplication.kt - 방법 1의 코드와 완전히 동일
val testDeviceIds = mutableListOf<String>()

if (BuildConfig.TEST_DEVICE_ID.isNotBlank()) {
    testDeviceIds.add(BuildConfig.TEST_DEVICE_ID)
}

MobileAds.setRequestConfiguration(
    RequestConfiguration.Builder()
        .setTestDeviceIds(testDeviceIds)
        .build()
)
MobileAds.initialize(this)
```

#### 4단계: .gitignore 확인 (중요!)

```gitignore
# .gitignore

# ✅ local.properties는 Git에 포함하지 않음
local.properties

# 키스토어 파일도 제외
*.jks
*.keystore
keystore.properties
```

#### 장점

- ✅ Git에 노출 안 됨 (보안)
- ✅ 프로젝트별로 다른 ID 사용 가능
- ✅ 팀 협업에 유리 (각자 local.properties 설정)

#### 단점

- ⚠️ 새 프로젝트마다 local.properties에 추가 필요
- ⚠️ 환경변수보다 한 단계 더 필요

---

### 🥉 방법 3: 환경변수 + local.properties 하이브리드 (최강)

**두 가지 장점을 모두 활용!**

#### build.gradle.kts 설정

```kotlin
// app/build.gradle.kts

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    defaultConfig {
        // ✅ 우선순위: 1) 환경변수 2) local.properties 3) 빈 문자열
        val testDeviceId = System.getenv("ADMOB_TEST_DEVICE_ID") 
            ?: localProperties.getProperty("test.device.id", "")
        
        buildConfigField("String", "TEST_DEVICE_ID", "\"$testDeviceId\"")
        
        // 로그로 어디서 로드되었는지 확인 (선택사항)
        if (System.getenv("ADMOB_TEST_DEVICE_ID") != null) {
            println("✅ Test device ID loaded from environment variable")
        } else if (localProperties.getProperty("test.device.id") != null) {
            println("✅ Test device ID loaded from local.properties")
        } else {
            println("⚠️ No test device ID configured")
        }
    }
}
```

#### 장점

- ✅ 개인 PC: 환경변수로 자동 적용
- ✅ 팀 프로젝트: local.properties로 각자 설정
- ✅ CI/CD: 환경변수로 통합
- ✅ 최대 유연성

---

### 🔢 여러 기기 관리하기

**개발용 폰이 여러 대라면?**

#### 환경변수 방식 (쉼표 구분)

```cmd
REM Windows 환경변수 설정
setx ADMOB_TEST_DEVICE_IDS "79DB2DA46501DFD953D9222E13384F99,ABC123DEF456,XYZ789GHI012"
```

#### build.gradle.kts

```kotlin
android {
    defaultConfig {
        val testDeviceIdsString = System.getenv("ADMOB_TEST_DEVICE_IDS") ?: ""
        buildConfigField("String", "TEST_DEVICE_IDS", "\"$testDeviceIdsString\"")
    }
}
```

#### MainApplication.kt

```kotlin
val testDeviceIds = BuildConfig.TEST_DEVICE_IDS
    .split(",")
    .map { it.trim() }
    .filter { it.isNotBlank() }

Log.d(TAG, "✅ Test devices registered: ${testDeviceIds.size}")

MobileAds.setRequestConfiguration(
    RequestConfiguration.Builder()
        .setTestDeviceIds(testDeviceIds)
        .build()
)
```

---

### 🎓 실전 워크플로우

#### 시나리오 1: 혼자 개발 (환경변수 추천)

```
[최초 1회만]
1. Windows 환경변수 ADMOB_TEST_DEVICE_ID 설정
2. Android Studio 재시작

[새 프로젝트마다]
1. build.gradle.kts 템플릿 복사 (30초)
2. MainApplication.kt 템플릿 복사 (30초)
3. 끝! 자동으로 테스트 기기 인식됨
```

#### 시나리오 2: 팀 협업 (local.properties 추천)

```
[각 개발자마다 최초 1회]
1. 프로젝트 클론
2. local.properties에 test.device.id 추가
3. 끝!

[장점]
- 개발자 A: A의 폰만 테스트 기기
- 개발자 B: B의 폰만 테스트 기기
- Git 충돌 없음 (local.properties는 .gitignore)
```

#### 시나리오 3: CI/CD 환경 (환경변수 필수)

```yaml
# GitHub Actions 예시
env:
  ADMOB_TEST_DEVICE_ID: ${{ secrets.ADMOB_TEST_DEVICE_ID }}

steps:
  - name: Build
    run: ./gradlew assembleRelease
```

---

### 📦 재사용 가능한 완전한 템플릿

#### build.gradle.kts 최종 템플릿

```kotlin
// app/build.gradle.kts
// ========================================
// AdMob 테스트 기기 자동 로드 템플릿
// 환경변수 또는 local.properties 사용
// ========================================

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// local.properties 로드 (존재하면)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}

android {
    namespace = "com.yourapp.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yourapp.example"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        // ========================================
        // 테스트 기기 ID 자동 로드
        // 우선순위: 환경변수 > local.properties > 빈 문자열
        // ========================================
        val testDeviceId = System.getenv("ADMOB_TEST_DEVICE_ID") 
            ?: localProperties.getProperty("test.device.id", "")
        
        buildConfigField("String", "TEST_DEVICE_ID", "\"$testDeviceId\"")
        
        // 빌드 로그 (선택사항)
        if (testDeviceId.isNotBlank()) {
            val source = when {
                System.getenv("ADMOB_TEST_DEVICE_ID") != null -> "environment variable"
                localProperties.getProperty("test.device.id") != null -> "local.properties"
                else -> "unknown"
            }
            println("✅ Test device ID loaded from $source")
        } else {
            println("⚠️ No test device ID configured")
        }
    }

    buildFeatures {
        buildConfig = true  // BuildConfig 생성 필수
        compose = true      // Compose 사용 시
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // ...existing config...
}

dependencies {
    implementation("com.google.android.gms:play-services-ads:23.4.0")
    implementation("com.google.android.ump:user-messaging-platform:2.2.0")
    // ...existing dependencies...
}
```

#### MainApplication.kt 최종 템플릿

```kotlin
// MainApplication.kt
// ========================================
// AdMob 초기화 및 테스트 기기 등록 템플릿
// ========================================

package com.yourapp.example

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

class MainApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()

        // ========================================
        // 테스트 기기 ID 자동 로드 및 등록
        // ========================================
        val testDeviceIds = mutableListOf<String>()
        
        // BuildConfig에서 로드 (환경변수 또는 local.properties)
        if (BuildConfig.TEST_DEVICE_ID.isNotBlank()) {
            testDeviceIds.add(BuildConfig.TEST_DEVICE_ID)
            Log.d(TAG, "✅ Test device registered: ${BuildConfig.TEST_DEVICE_ID}")
        } else {
            Log.w(TAG, "⚠️ No test device ID configured")
            Log.w(TAG, "   광고 클릭 시 계정 정지 위험!")
        }

        // AdMob RequestConfiguration 설정
        val config = RequestConfiguration.Builder()
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
            .apply {
                if (testDeviceIds.isNotEmpty()) {
                    setTestDeviceIds(testDeviceIds)
                    Log.d(TAG, "   Total test devices: ${testDeviceIds.size}")
                }
            }
            .build()
        
        MobileAds.setRequestConfiguration(config)
        
        // AdMob SDK 초기화
        MobileAds.initialize(this) { initStatus ->
            Log.d(TAG, "✅ AdMob initialized")
            Log.d(TAG, "   Adapter status: $initStatus")
        }
    }
    
    companion object {
        private const val TAG = "MainApplication"
    }
}
```

#### local.properties 템플릿 (선택사항)

```properties
# local.properties
# ⚠️ 이 파일은 Git에 커밋하지 마세요! (.gitignore에 포함)

sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk

# ========================================
# AdMob 테스트 기기 ID
# ========================================
# 본인 폰의 기기 ID를 입력하세요
# 찾는 방법: 앱 실행 후 Logcat에서 "Use RequestConfiguration.Builder()" 검색
test.device.id=79DB2DA46501DFD953D9222E13384F99

# 여러 기기 (쉼표 구분)
# test.device.ids=ID1,ID2,ID3
```

#### .gitignore 필수 설정

```gitignore
# .gitignore

# ========================================
# 테스트 기기 ID 보안
# ========================================
# local.properties는 절대 커밋하지 않음
local.properties

# 키스토어 파일도 제외
*.jks
*.keystore
keystore.properties

# 환경 설정 파일
*.env
.env.*

# Android Studio
.idea/
.gradle/
local.properties
```

---

### ⚠️ 주의사항

#### 절대 하면 안 되는 것

❌ **Git에 테스트 기기 ID 커밋**
```kotlin
// BAD: 하드코딩된 ID가 GitHub에 공개됨
val testDeviceIds = listOf("79DB2DA46501DFD953D9222E13384F99")
```

❌ **릴리즈 빌드에서 테스트 기기 제거**
```kotlin
// BAD: 본인 광고 클릭 시 계정 정지!
val testDeviceIds = if (BuildConfig.DEBUG) {
    listOf("79DB2DA46501DFD953D9222E13384F99")
} else {
    emptyList()  // 위험!
}
```

#### ✅ 올바른 방법

```kotlin
// GOOD: 디버그/릴리즈 모두 동일하게 등록
val testDeviceIds = listOfNotNull(
    BuildConfig.TEST_DEVICE_ID.takeIf { it.isNotBlank() }
)

// GOOD: 환경변수나 local.properties 사용
```

---

### 🔍 문제 해결

#### 환경변수가 인식되지 않을 때

```
1. Android Studio 완전 종료
2. Gradle 캐시 삭제:
   - 프로젝트 루트에서: gradlew.bat clean
3. Android Studio 재시작
4. File → Invalidate Caches / Restart
```

#### BuildConfig.TEST_DEVICE_ID를 찾을 수 없다는 오류

```kotlin
// build.gradle.kts에서 확인
buildFeatures {
    buildConfig = true  // 이게 있어야 함!
}
```

#### 테스트 광고가 표시되지 않을 때

```
1. Logcat 확인:
   - "Test device registered" 로그 있는지 확인
   - "This request is sent from a test device" 있는지 확인

2. 기기 ID가 정확한지 재확인:
   - Logcat에서 "Use RequestConfiguration" 검색
   - 제시된 ID와 설정한 ID 비교

3. AdMob 앱 등록 확인:
   - AdMob 콘솔에 앱이 등록되어 있는지
   - 광고 유닛 ID가 올바른지
```

---

### 📝 Q2 요약

| 방법 | 설정 시간 | 새 프로젝트마다 | Git 안전 | 추천도 |
|------|----------|--------------|---------|--------|
| **환경변수** | 최초 5분 | 템플릿만 복사 (1분) | ✅ | ⭐⭐⭐⭐⭐ |
| **local.properties** | 각 2분 | 2분 | ✅ | ⭐⭐⭐⭐ |
| **하드코딩** | 30초 | 30초 | ❌ | ⭐⭐ |

**최종 추천**: 
1. **개인 개발**: 환경변수 사용 (한 번만 설정)
2. **팀 협업**: local.properties 사용 (각자 설정)
3. **최강 조합**: 환경변수 + local.properties 하이브리드

**결론: 더 이상 매번 하드코딩할 필요 없습니다!** 🎉

---

## 부록: 테스트 기기 ID 찾는 방법

### 방법 1: Logcat으로 찾기 (가장 쉬움)

```
1. 앱을 실행합니다 (광고 SDK 초기화 필요)
2. Android Studio → Logcat 열기
3. 검색창에 입력: "Use RequestConfiguration"
4. 로그 예시:
   I/Ads: Use RequestConfiguration.Builder()
         .setTestDeviceIds(Arrays.asList("79DB2DA46501DFD953D9222E13384F99"))
5. 큰따옴표 안의 ID 복사
```

### 방법 2: 코드로 출력하기

```kotlin
// MainApplication.kt - onCreate()
override fun onCreate() {
    super.onCreate()
    
    // ✅ 기기 ID 출력 (개발 중에만 사용)
    val deviceId = MobileAds.getRequestConfiguration().testDeviceIds
    Log.d("TEST_DEVICE", "Current device ID: $deviceId")
    
    // 또는 직접 계산
    val adRequest = AdRequest.Builder().build()
    Log.d("TEST_DEVICE", "AdRequest: $adRequest")
}
```

### 방법 3: AdMob 대시보드

```
1. AdMob 콘솔 접속
2. 앱 실행 (광고 요청 발생)
3. AdMob에서 "Invalid traffic" 경고 확인
4. 경고에 기기 ID 표시됨
```

---

## 📚 관련 문서

- [AdMob 정책 준수 가이드](A_AD_POLICY_COMPLIANCE_SUMMARY.md)
- [본인 광고 클릭 경고](a_AD_SELF_CLICK_WARNING.md)
- [AdMob 통합 가이드](https://developers.google.com/admob/android/quick-start)

---

## 🔄 문서 업데이트 계획

| 예정 버전 | 예정 날짜 | 추가 내용 |
|----------|----------|----------|
| 1.1.0 | TBD | Q3: 테스트 광고와 실제 광고 구분법 |
| 1.2.0 | TBD | Q4: AdMob 수익 분석 방법 |
| 1.3.0 | TBD | Q5: 여러 광고 유닛 관리 |

---

## 💬 피드백 및 문의

문서 개선 제안이나 추가 질문이 있으시면:
- GitHub Issues 등록
- 개발팀 이메일 문의
- 문서 내 오타/오류 발견 시 Pull Request 환영

---

**문서 끝** - AlcoholicTimer 개발팀 © 2025

