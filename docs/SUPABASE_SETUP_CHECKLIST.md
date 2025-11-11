# Supabase 연동 체크리스트

## ✅ 완료된 작업

- [x] EmergencyPolicy 모델 생성
- [x] NoticePolicy 모델 생성
- [x] UpdatePolicy 모델 생성
- [x] PopupDecision sealed class 생성
- [x] EmergencyPolicyRepository 구현
- [x] NoticePolicyRepository 구현
- [x] UpdatePolicyRepository 구현
- [x] PopupPolicyManager 통합 매니저 구현
- [x] SupabaseProvider 싱글톤 구현
- [x] DebugViewModel 생성
- [x] DebugActivity 수정 및 연동
- [x] Supabase 의존성 추가 (gradle)
- [x] Kotlin Serialization 플러그인 추가
- [x] 문서화 (통합 가이드 및 요약)
- [x] README 업데이트
- [x] **Gradle 의존성 문제 해결 (Supabase 3.0.2, Ktor 3.0.1 업데이트)**

## ⚠️ 설정 필요 사항

### 0. Gradle 의존성 문제 해결 (완료)

Supabase Kotlin SDK 버전이 업데이트되어 다음과 같이 수정되었습니다:

**gradle/libs.versions.toml**:
```toml
[versions]
supabase = "3.0.2"  # 2.7.2에서 3.0.2로 업데이트
ktor = "3.0.1"      # 2.3.12에서 3.0.1로 업데이트

[libraries]
# postgrest-kt-jvm 아티팩트 사용 (Android 호환)
supabase-postgrest = { group = "io.github.jan-tennert.supabase", name = "postgrest-kt-jvm", version.ref = "supabase" }
ktor-client-android = { group = "io.ktor", name = "ktor-client-android", version.ref = "ktor" }
ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version = "1.7.3" }
```

**app/build.gradle.kts**:
```kotlin
dependencies {
    // Supabase
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)
}
```

**SupabaseProvider.kt** - KotlinXSerializer 제거 (기본 설정 사용)

### 1. Supabase 프로젝트 생성

1. https://supabase.com 에서 새 프로젝트 생성
2. 다음 3개 테이블 생성:
   - `emergency_policies`
   - `notice_policies`
   - `update_policies`
3. 테이블 스키마는 `docs/SUPABASE_POPUP_INTEGRATION_GUIDE.md` 참조

### 2. BuildConfig 설정 (✅ 완료)

**이미 `app/build.gradle.kts`에 설정되어 있습니다!**

```kotlin
android {
    defaultConfig {
        // ... 기존 설정
        
        // Supabase 설정 (환경변수 또는 기본값)
        val supabaseUrl = System.getenv("SUPABASE_URL") ?: "https://your-project.supabase.co"
        val supabaseKey = System.getenv("SUPABASE_KEY") ?: "your-anon-key"
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
    }
}
```

#### 환경변수 설정 방법

**Windows (PowerShell)**:
```powershell
# 현재 세션에서만 유효
$env:SUPABASE_URL = "https://your-project-id.supabase.co"
$env:SUPABASE_KEY = "your-anon-key-here"

# 또는 영구 설정 (시스템 환경변수)
[System.Environment]::SetEnvironmentVariable("SUPABASE_URL", "https://your-project-id.supabase.co", "User")
[System.Environment]::SetEnvironmentVariable("SUPABASE_KEY", "your-anon-key-here", "User")
```

**Windows (CMD)**:
```cmd
SET SUPABASE_URL=https://your-project-id.supabase.co
SET SUPABASE_KEY=your-anon-key-here
```

**로컬 properties 파일 방식 (권장)**:

프로젝트 루트에 `local.properties` 파일 생성 또는 수정:
```properties
# Supabase 설정
supabase.url=https://your-project-id.supabase.co
supabase.key=your-anon-key-here
```

그 다음 `app/build.gradle.kts`를 다음과 같이 수정:
```kotlin
// local.properties 읽기
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    defaultConfig {
        // Supabase 설정 (local.properties → 환경변수 → 기본값 순)
        val supabaseUrl = localProperties.getProperty("supabase.url") 
            ?: System.getenv("SUPABASE_URL") 
            ?: "https://your-project.supabase.co"
        val supabaseKey = localProperties.getProperty("supabase.key") 
            ?: System.getenv("SUPABASE_KEY") 
            ?: "your-anon-key"
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
    }
}
```

**⚠️ 주의**: `local.properties`는 `.gitignore`에 포함되어 있어 Git에 커밋되지 않습니다.

### 3. SupabaseProvider.kt 수정 (✅ 완료)

**이미 `BuildConfig`를 사용하도록 수정되었습니다!**

`app/src/main/java/kr/sweetapps/alcoholictimer/data/supabase/SupabaseProvider.kt`:

```kotlin
import kr.sweetapps.alcoholictimer.BuildConfig

private fun createClient(context: Context): SupabaseClient {
    return createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Postgrest)
    }
}
```

### 4. Supabase 프로젝트에서 URL과 Key 가져오기

1. https://supabase.com 로그인
2. 프로젝트 선택 또는 새로 생성
3. **Settings** → **API** 메뉴로 이동
4. 다음 정보 복사:
   - **Project URL**: `https://xxxxx.supabase.co` 형식
   - **anon public key**: `eyJhbGc...` 로 시작하는 긴 문자열

5. 위에서 복사한 정보를 환경변수 또는 `local.properties`에 설정

## 🧪 테스트 절차

### 1. DebugActivity 테스트

```
1. 앱 실행 (디버그 모드)
2. 디버그 메뉴 진입
3. "공지사항 (Supabase 연동)" 카드 확인
4. 정책 로딩 상태 확인
5. 각 정책별 팝업 토글 테스트
6. "우선순위대로 팝업 표시" 버튼 테스트
7. "표시 기록 초기화" 버튼 테스트
```

### 2. Supabase 테이블에 테스트 데이터 추가

#### emergency_policies 테스트 데이터
```sql
INSERT INTO emergency_policies (
  id, is_active, title, description, button_text, 
  priority, created_at, is_dismissible
) VALUES (
  gen_random_uuid(),
  true,
  '긴급 안내',
  '테스트 긴급 공지입니다.',
  '확인',
  100,
  now(),
  true
);
```

#### notice_policies 테스트 데이터
```sql
INSERT INTO notice_policies (
  id, is_active, title, description, button_text,
  priority, show_once, created_at
) VALUES (
  gen_random_uuid(),
  true,
  '새 기능 안내',
  '테스트 일반 공지입니다.',
  '확인했습니다',
  50,
  false,
  now()
);
```

#### update_policies 테스트 데이터
```sql
INSERT INTO update_policies (
  id, is_active, version, version_code, title, description,
  update_button_text, later_button_text, is_force_update, created_at
) VALUES (
  gen_random_uuid(),
  true,
  '2.0.0',
  2000000,
  '새 버전 사용 가능',
  '테스트 업데이트 공지입니다.',
  '지금 업데이트',
  '나중에',
  false,
  now()
);
```

### 3. 프로덕션 적용 전 확인

- [ ] Supabase 프로젝트 생성 완료
- [ ] 테이블 스키마 생성 완료
- [ ] BuildConfig 설정 완료
- [ ] SupabaseProvider.kt TODO 제거 완료
- [ ] 디버그 모드에서 정책 로딩 성공 확인
- [ ] 각 팝업 타입별 표시 테스트 완료
- [ ] 우선순위 로직 테스트 완료
- [ ] 표시 기록 저장/로드 테스트 완료

## 📁 생성된 파일 목록

### 모델 (4개)
- `app/src/main/java/kr/sweetapps/alcoholictimer/data/supabase/model/EmergencyPolicy.kt`
- `app/src/main/java/kr/sweetapps/alcoholictimer/data/supabase/model/NoticePolicy.kt`
- `app/src/main/java/kr/sweetapps/alcoholictimer/data/supabase/model/UpdatePolicy.kt`
- `app/src/main/java/kr/sweetapps/alcoholictimer/data/supabase/model/PopupDecision.kt`

### 저장소 (4개)
- `app/src/main/java/kr/sweetapps/alcoholictimer/data/supabase/repository/EmergencyPolicyRepository.kt`
- `app/src/main/java/kr/sweetapps/alcoholictimer/data/supabase/repository/NoticePolicyRepository.kt`
- `app/src/main/java/kr/sweetapps/alcoholictimer/data/supabase/repository/UpdatePolicyRepository.kt`
- `app/src/main/java/kr/sweetapps/alcoholictimer/data/supabase/repository/PopupPolicyManager.kt`

### 인프라 (2개)
- `app/src/main/java/kr/sweetapps/alcoholictimer/data/supabase/SupabaseProvider.kt`
- `app/src/debug/java/kr/sweetapps/alcoholictimer/feature/debug/DebugViewModel.kt`

### 문서 (3개)
- `docs/SUPABASE_POPUP_INTEGRATION_GUIDE.md` - 전체 통합 가이드
- `docs/SUPABASE_IMPLEMENTATION_SUMMARY.md` - 구현 요약
- `docs/SUPABASE_SETUP_CHECKLIST.md` - 이 파일

### 수정된 파일 (3개)
- `app/src/debug/java/kr/sweetapps/alcoholictimer/feature/debug/DebugActivity.kt`
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `README.md`

## 🔗 참고 링크

- **Supabase 공식 문서**: https://supabase.com/docs
- **Supabase Kotlin SDK**: https://github.com/supabase-community/supabase-kt
- **프로젝트 통합 가이드**: `docs/SUPABASE_POPUP_INTEGRATION_GUIDE.md`

## 💡 다음 단계

1. Supabase 프로젝트 생성
2. BuildConfig 설정
3. 테스트 데이터 추가
4. DebugActivity에서 동작 확인
5. MainActivity에 팝업 로직 추가
6. 프로덕션 배포

## 🆘 문제 해결

### 빌드 오류
- Gradle Sync 실행: `gradlew.bat --refresh-dependencies`
- Clean Build: `gradlew.bat clean :app:assembleDebug`

### 네트워크 오류
- Supabase URL/Key 확인
- 인터넷 연결 확인
- RLS (Row Level Security) 정책 확인 (익명 읽기 허용 필요)

### 정책이 로딩되지 않음
- Supabase 대시보드에서 데이터 확인
- `is_active = true` 확인
- 로그 확인 (`e.printStackTrace()` 출력)

