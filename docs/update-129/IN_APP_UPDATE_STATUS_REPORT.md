# 📊 인앱 업데이트 구현 상태 보고서

**작성일**: 2026-01-06  
**검토자**: AI Assistant  
**상태**: ⚠️ 부분 구현 (Google Play In-App Update API 미적용)

---

## 🎯 요약

AlcoholicTimer 앱은 **Supabase 기반 커스텀 업데이트 팝업 시스템**을 사용하고 있으며, **Google Play In-App Updates API는 미적용** 상태입니다.

### 현재 적용된 방식
- ✅ **Supabase `update_policy` 테이블 기반 업데이트 알림**
- ✅ **커스텀 다이얼로그로 업데이트 안내**
- ✅ **Play Store 링크로 사용자 유도**

### 미적용 상태
- ❌ **Google Play In-App Updates API** (`com.google.android.play:app-update-ktx`)
- ❌ **FLEXIBLE/IMMEDIATE 업데이트 타입**
- ❌ **백그라운드 다운로드 및 자동 설치**

---

## 📂 현재 구현 구조

### 1. 업데이트 정책 관리 (Supabase 기반)

#### 📁 Repository Layer
```
app/src/main/java/kr/sweetapps/alcoholictimer/data/supabase/repository/
├── UpdatePolicyRepository.kt        [현재 비활성화 상태]
├── PopupPolicyManager.kt            [팝업 결정 로직]
├── EmergencyPolicyRepository.kt     [긴급 공지용]
└── NoticePolicyRepository.kt        [일반 공지용]
```

**UpdatePolicyRepository.kt 상태**:
```kotlin
suspend fun getActivePolicy(): UpdatePolicy? = withContext(Dispatchers.IO) {
    // [DISABLED] Supabase 팝업 기능 비활성화 - Firebase로 이전 예정
    // 앱 크래시 방지를 위해 항상 null 반환 (표시할 팝업 없음)
    android.util.Log.d("UpdatePolicyRepo", "Update popup disabled - returning null")
    return@withContext null
}
```

→ **현재 업데이트 팝업이 완전히 비활성화되어 있습니다!**

#### 📁 Model Layer
```
app/src/main/java/kr/sweetapps/alcoholictimer/data/supabase/model/
├── UpdatePolicy.kt          [업데이트 정책 모델]
├── UpdateInfo.kt            [Stub - 실제 미사용]
├── PopupDecision.kt         [팝업 표시 결정]
└── EmergencyPolicy.kt       [긴급 공지 모델]
```

#### 📁 UI Layer
```
app/src/main/java/kr/sweetapps/alcoholictimer/ui/main/
└── MainActivity.kt
    ├── PopupPolicyManager 호출
    ├── OptionalUpdateDialog 표시
    └── Play Store 이동 처리
```

---

### 2. Supabase 테이블 스키마

#### `update_policy` 테이블

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `id` | bigint | Primary Key |
| `app_id` | text | 앱 패키지명 (예: `kr.sweetapps.alcoholictimer.debug`) |
| `is_active` | boolean | 활성화 여부 |
| `target_version_code` | integer | 타겟 버전 코드 |
| `is_force_update` | boolean | 강제 업데이트 여부 |
| `release_notes` | text | 업데이트 설명 |
| `download_url` | text | Play Store URL |
| `reshow_interval_hours` | integer | 재표시 간격(시간) |
| `reshow_interval_seconds` | integer | 재표시 간격(초, 디버그용) |
| `max_later_count` | integer | 최대 연기 횟수 (기본 3회) |

#### 동작 로직

1. **앱 시작 시 PopupPolicyManager.decidePopup() 호출**
2. **우선순위**: Emergency > Update > Notice
3. **Update 표시 조건**:
   - `is_active = true`
   - `target_version_code >= currentVersionCode` (현재 버전 이상)
   - 강제 업데이트 OR (연기 횟수 초과 OR 재표시 간격 경과)

4. **OptionalUpdateDialog 표시**:
   - 강제 모드: "나중에" 버튼 없음, 뒤로가기 차단
   - 선택 모드: "나중에" 버튼 표시 (최대 3회)

5. **사용자 액션**:
   - "업데이트" → Play Store 이동 (`Intent.ACTION_VIEW`)
   - "나중에" → 연기 횟수 +1, SharedPreferences 저장

---

### 3. 단위 테스트 구조

#### 📁 Test Code
```
app/src/test/java/kr/sweetapps/alcoholictimer/popup/
├── PopupManager.kt              [테스트용 매니저]
├── PopupManagerTest.kt          [단위 테스트]
├── TestModels.kt                [테스트 모델]
└── MockRepositories.kt          [Mock 객체]
```

**주요 테스트 케이스**:
- ✅ Emergency 우선순위 (최우선)
- ✅ Update 버전 체크 (`target > current`)
- ✅ 강제 업데이트 우선순위
- ✅ 연기 횟수 체크 (`laterCount >= max_later_count`)
- ✅ 재표시 간격 체크
- ✅ Notice 버전 체크

---

## ❌ Google Play In-App Update API 미적용 상태

### 문서는 있지만 코드는 없음

#### 📄 존재하는 문서
```
docs/update-118/
├── IN_APP_UPDATE_IMPLEMENTATION.md   [구현 완료 문서]
└── IN_APP_UPDATE_DONE.md             [빈 파일]

docs/backup-docs/
└── APP_UPDATE_POLICY.md              [정책 문서]
```

**IN_APP_UPDATE_IMPLEMENTATION.md 내용**:
- ✅ Material 라이브러리 추가 필요
- ✅ 다국어 문자열 추가 (4개 언어)
- ✅ MainActivity 변수 추가 (`IS_URGENT_UPDATE`, `appUpdateManager`)
- ✅ 함수 구현 (`initInAppUpdate()`, `registerDownloadListener()` 등)
- ⚠️ **주의**: "MainActivity 파일 복원 필요", "구현 중 파일 손상"

### 실제 코드 상태

#### ❌ 라이브러리 미추가
```kotlin
// gradle/libs.versions.toml
appUpdate = "2.1.0"  // ✅ 버전 정의됨

app-update-ktx = { 
    group = "com.google.android.play", 
    name = "app-update-ktx", 
    version.ref = "appUpdate" 
}  // ✅ 라이브러리 정의됨
```

```kotlin
// app/build.gradle.kts dependencies
// ❌ app-update-ktx 미추가
// implementation(libs.app.update.ktx)  // 이 줄이 없음!
```

#### ❌ MainActivity에 관련 코드 없음
- `IS_URGENT_UPDATE` 변수 없음
- `appUpdateManager` 없음
- `initInAppUpdate()` 함수 없음
- `registerDownloadListener()` 함수 없음
- `checkUpdateDownloadStatus()` 함수 없음

#### ❌ 관련 Import 없음
```kotlin
// MainActivity.kt에 없는 Import
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
```

---

## 📊 비교표: Supabase vs Google Play In-App Updates

| 항목 | Supabase 방식 (현재) | Google Play API (미적용) |
|------|---------------------|-------------------------|
| **업데이트 확인** | 서버(Supabase) 폴링 | Play Store 자동 확인 |
| **다운로드** | 사용자가 Play Store로 이동 | 앱 내 백그라운드 다운로드 |
| **설치** | Play Store에서 수동 설치 | 앱 재시작 시 자동 설치 |
| **사용자 경험** | 외부 이동 (UX 단절) | 앱 내 완결 (UX 우수) |
| **구현 복잡도** | 낮음 (커스텀 다이얼로그) | 중간 (Google API 연동) |
| **서버 의존성** | 높음 (Supabase 필수) | 낮음 (Play Store 연동) |
| **오프라인 대응** | 불가 | 부분 가능 (캐시) |
| **정책 유연성** | 높음 (서버 제어) | 낮음 (Play Store 정책) |
| **Google 권장** | ❌ | ✅ |

---

## 🚨 현재 문제점

### 1. ⚠️ 업데이트 팝업 완전 비활성화

**UpdatePolicyRepository.kt**:
```kotlin
suspend fun getActivePolicy(): UpdatePolicy? = withContext(Dispatchers.IO) {
    // [DISABLED] Supabase 팝업 기능 비활성화 - Firebase로 이전 예정
    android.util.Log.d("UpdatePolicyRepo", "Update popup disabled - returning null")
    return@withContext null
}
```

→ **현재 사용자에게 업데이트 알림이 전혀 표시되지 않습니다!**

### 2. 📄 문서와 코드 불일치

- 문서: "✅ 인앱 업데이트 기능 구현 완료"
- 실제: Google Play API 관련 코드 전혀 없음
- 문서 작성일: 2026-01-02 (4일 전)

### 3. 🔧 라이브러리 정의만 있고 미사용

- `libs.versions.toml`: `app-update-ktx` 정의 ✅
- `build.gradle.kts`: 의존성 미추가 ❌

---

## ✅ 권장사항

### 옵션 A: Supabase 방식 유지 (빠른 해결)

**장점**:
- 이미 구현된 코드 활용
- 서버에서 유연한 제어 가능
- 빠른 배포 가능

**작업 내용**:
1. **UpdatePolicyRepository.kt 활성화**:
   ```kotlin
   suspend fun getActivePolicy(): UpdatePolicy? = withContext(Dispatchers.IO) {
       // [ENABLED] 주석 처리된 원본 코드 복원
       // 실제 Supabase 조회 로직 활성화
   }
   ```

2. **Supabase 테이블 데이터 확인**:
   ```sql
   SELECT * FROM update_policy 
   WHERE app_id IN (
       'kr.sweetapps.alcoholictimer',
       'kr.sweetapps.alcoholictimer.debug'
   );
   ```

3. **테스트**:
   - 디버그 빌드로 업데이트 팝업 표시 확인
   - 강제/선택 모드 동작 검증

### 옵션 B: Google Play In-App Updates 적용 (권장)

**장점**:
- ✅ Google 공식 권장 방식
- ✅ 사용자 경험 우수 (앱 내 완결)
- ✅ 백그라운드 다운로드/자동 설치
- ✅ Play Store 정책 준수

**작업 내용**:

#### 1단계: 라이브러리 추가

```kotlin
// app/build.gradle.kts dependencies
implementation(libs.app.update.ktx)
implementation("com.google.android.material:material:1.11.0")
```

#### 2단계: MainActivity 변수 추가

```kotlin
class MainActivity : BaseActivity() {
    // [NEW] 인앱 업데이트 설정
    private val IS_URGENT_UPDATE = false  // false: FLEXIBLE, true: IMMEDIATE
    
    // [NEW] 인앱 업데이트 매니저
    private lateinit var appUpdateManager: AppUpdateManager
    private val updateRequestCode = 1001
}
```

#### 3단계: 함수 구현

**initInAppUpdate()**:
```kotlin
private fun initInAppUpdate() {
    appUpdateManager = AppUpdateManagerFactory.create(this)
    
    appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
        if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
            val updateType = if (IS_URGENT_UPDATE) {
                AppUpdateType.IMMEDIATE
            } else {
                AppUpdateType.FLEXIBLE
            }
            
            if (info.isUpdateTypeAllowed(updateType)) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    updateType,
                    this,
                    updateRequestCode
                )
                
                if (updateType == AppUpdateType.FLEXIBLE) {
                    registerDownloadListener()
                }
            }
        }
    }
}
```

**registerDownloadListener()**:
```kotlin
private fun registerDownloadListener() {
    val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showUpdateDownloadedSnackbar()
        }
    }
    appUpdateManager.registerListener(listener)
}
```

**showUpdateDownloadedSnackbar()**:
```kotlin
private fun showUpdateDownloadedSnackbar() {
    Snackbar.make(
        findViewById(android.R.id.content),
        getString(R.string.update_downloaded),
        Snackbar.LENGTH_INDEFINITE
    ).setAction(getString(R.string.restart_to_install)) {
        appUpdateManager.completeUpdate()
    }.show()
}
```

**checkUpdateDownloadStatus()** (onResume에서 호출):
```kotlin
override fun onResume() {
    super.onResume()
    appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
        if (info.installStatus() == InstallStatus.DOWNLOADED) {
            showUpdateDownloadedSnackbar()
        }
    }
}
```

#### 4단계: 다국어 문자열 추가

**res/values/strings.xml** (영어):
```xml
<string name="update_downloaded">Update downloaded</string>
<string name="restart_to_install">Restart</string>
```

**res/values-ko/strings.xml** (한국어):
```xml
<string name="update_downloaded">업데이트 다운로드 완료</string>
<string name="restart_to_install">재실행</string>
```

**res/values-ja/strings.xml** (일본어):
```xml
<string name="update_downloaded">アップデートのダウンロード完了</string>
<string name="restart_to_install">再起動</string>
```

**res/values-in/strings.xml** (인도네시아어):
```xml
<string name="update_downloaded">Pembaruan berhasil diunduh</string>
<string name="restart_to_install">Mulai Ulang</string>
```

#### 5단계: 테스트

**로컬 테스트 방법**:
1. 내부 테스트 트랙에 신규 버전 업로드 (versionCode +1)
2. 이전 버전 설치 후 앱 실행
3. 업데이트 다이얼로그 확인

---

## 🎯 결론

### 현재 상태
- ❌ Google Play In-App Updates **미적용**
- ⚠️ Supabase 업데이트 팝업 **비활성화 상태**
- 📄 문서만 존재, 실제 코드 없음

### 다음 단계 선택

| 선택지 | 소요 시간 | 사용자 경험 | 권장도 |
|--------|----------|------------|--------|
| **A. Supabase 활성화** | 1-2시간 | ⭐⭐⭐ | 임시 조치 |
| **B. Google Play API 적용** | 4-6시간 | ⭐⭐⭐⭐⭐ | ✅ 권장 |

### 최종 권장: **옵션 B (Google Play In-App Updates)**

**이유**:
1. ✅ Google 공식 권장 방식
2. ✅ 사용자 경험 우수 (앱 내 완결)
3. ✅ Play Store 정책 완벽 준수
4. ✅ 라이브러리 정의 이미 완료 (추가만 하면 됨)
5. ✅ 문서화 이미 완료 (구현만 남음)

---

**보고서 작성**: 2026-01-06  
**검토 완료**: ✅  
**다음 단계**: 개발자 결정 필요

