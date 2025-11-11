# Supabase 팝업 정책 연동 가이드

이 문서는 AlcoholicTimer 앱에 Supabase 기반 팝업 정책 시스템을 연동한 내용을 설명합니다.

## 구현 개요

### 1. 데이터 모델 (Model)

`app/src/main/java/kr/sweetapps/alcoholictimer/data/supabase/model/`

- **EmergencyPolicy.kt**: 긴급 공지 정책
  - 앱 서비스 종료, 긴급 안내 등
  - 최우선 순위로 표시
  
- **NoticePolicy.kt**: 일반 공지사항 정책
  - 새 기능 안내, 이벤트 공지 등
  - 버전 범위, 만료일 지원
  
- **UpdatePolicy.kt**: 업데이트 정책
  - 강제/선택적 업데이트 지원
  - 버전 코드 기반 판단
  
- **PopupDecision.kt**: 팝업 표시 결정 결과
  - Sealed class로 표시할 팝업 타입 구분

### 2. 저장소 (Repository)

`app/src/main/java/kr/sweetapps/alcoholictimer/data/supabase/repository/`

- **EmergencyPolicyRepository.kt**: 긴급 공지 조회 및 표시 기록 관리
- **NoticePolicyRepository.kt**: 일반 공지 조회 및 버전 필터링
- **UpdatePolicyRepository.kt**: 업데이트 정책 조회 및 무시 기록 관리
- **PopupPolicyManager.kt**: 세 가지 정책을 통합 관리하는 매니저

### 3. Supabase 클라이언트

`app/src/main/java/kr/sweetapps/alcoholictimer/data/supabase/SupabaseProvider.kt`

- Singleton 패턴으로 Supabase 클라이언트 제공
- Postgrest 플러그인 설치
- JSON 직렬화 설정

### 4. ViewModel

`app/src/debug/java/kr/sweetapps/alcoholictimer/feature/debug/DebugViewModel.kt`

- 정책 로딩 및 상태 관리
- 팝업 결정 로직 실행
- 표시 기록 관리

### 5. UI (DebugActivity)

`app/src/debug/java/kr/sweetapps/alcoholictimer/feature/debug/DebugActivity.kt`

- Supabase 정책을 실시간으로 조회
- 각 정책별 팝업 미리보기
- 우선순위대로 팝업 자동 표시
- 표시 기록 초기화

## 우선순위 정책

팝업은 다음 순서로 표시됩니다:

1. **긴급 공지** (EmergencyPolicy)
   - 최우선 순위
   - 한 번 표시된 정책은 다시 표시되지 않음

2. **강제 업데이트** (UpdatePolicy - isForceUpdate=true)
   - 현재 버전보다 높은 버전만
   - 무시 기록 무시

3. **일반 공지** (NoticePolicy)
   - 우선순위(priority) 순
   - 버전 범위, 만료일 체크
   - showOnce=true인 경우 한 번만 표시

4. **선택적 업데이트** (UpdatePolicy - isForceUpdate=false)
   - "나중에" 버튼으로 무시 가능
   - 무시된 버전은 다시 표시되지 않음

## Supabase 테이블 구조

### emergency_policies
```sql
CREATE TABLE emergency_policies (
  id UUID PRIMARY KEY,
  is_active BOOLEAN,
  title TEXT,
  description TEXT,
  new_app_name TEXT,
  new_app_package TEXT,
  button_text TEXT,
  support_url TEXT,
  support_button_text TEXT,
  can_migrate_data BOOLEAN,
  is_dismissible BOOLEAN,
  badge_text TEXT,
  migration_message TEXT,
  priority INTEGER,
  created_at TIMESTAMP
);
```

### notice_policies
```sql
CREATE TABLE notice_policies (
  id UUID PRIMARY KEY,
  is_active BOOLEAN,
  title TEXT,
  description TEXT,
  button_text TEXT,
  action_url TEXT,
  priority INTEGER,
  show_once BOOLEAN,
  target_version_min TEXT,
  target_version_max TEXT,
  created_at TIMESTAMP,
  expires_at TIMESTAMP
);
```

### update_policies
```sql
CREATE TABLE update_policies (
  id UUID PRIMARY KEY,
  is_active BOOLEAN,
  version TEXT,
  version_code INTEGER,
  title TEXT,
  description TEXT,
  update_button_text TEXT,
  later_button_text TEXT,
  features TEXT[], -- PostgreSQL 배열
  is_force_update BOOLEAN,
  target_version_min TEXT,
  target_version_max TEXT,
  store_url TEXT,
  created_at TIMESTAMP
);
```

## 사용 방법

### 1. Supabase 설정

`app/build.gradle.kts`에 다음을 추가해야 합니다:

```kotlin
android {
    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"https://your-project.supabase.co\"")
        buildConfigField("String", "SUPABASE_KEY", "\"your-anon-key\"")
    }
}
```

그리고 `SupabaseProvider.kt`를 수정:

```kotlin
val supabaseUrl = BuildConfig.SUPABASE_URL
val supabaseKey = BuildConfig.SUPABASE_KEY
```

### 2. 정책 로딩

```kotlin
val viewModel: DebugViewModel by viewModels()

// 정책 로딩
viewModel.loadPolicies()

// 상태 관찰
val emergencyPolicy by viewModel.emergencyPolicy.collectAsState()
val noticePolicy by viewModel.noticePolicy.collectAsState()
val updatePolicy by viewModel.updatePolicy.collectAsState()
```

### 3. 팝업 결정 및 표시

```kotlin
viewModel.decidePopup { decision ->
    when (decision) {
        is PopupDecision.ShowEmergency -> {
            // 긴급 공지 다이얼로그 표시
            showEmergencyDialog = true
        }
        is PopupDecision.ShowNotice -> {
            // 일반 공지 다이얼로그 표시
            showNoticeDialog = true
        }
        is PopupDecision.ShowUpdate -> {
            // 업데이트 다이얼로그 표시
            showUpdateDialog = true
        }
        is PopupDecision.None -> {
            // 표시할 팝업 없음
        }
    }
}
```

### 4. 앱 시작 시 자동 팝업 (MainActivity 등에서)

```kotlin
class MainActivity : BaseActivity() {
    private val policyManager by lazy {
        val client = SupabaseProvider.getClient(this)
        PopupPolicyManager(
            EmergencyPolicyRepository(client, this),
            UpdatePolicyRepository(client, this),
            NoticePolicyRepository(client, this),
            this
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            val currentVersion = packageManager
                .getPackageInfo(packageName, 0)
                .versionName
            
            when (val decision = policyManager.decidePopup(currentVersion)) {
                is PopupDecision.ShowEmergency -> {
                    // 긴급 공지 표시
                    policyManager.markEmergencyShown(decision.policy.id)
                }
                is PopupDecision.ShowNotice -> {
                    // 공지 표시
                    policyManager.markNoticeShown(decision.policy.id)
                }
                is PopupDecision.ShowUpdate -> {
                    // 업데이트 표시
                }
                else -> {}
            }
        }
    }
}
```

## 디버그 기능

DebugActivity에서 다음 기능을 제공합니다:

1. **정책 로딩**: Supabase에서 현재 활성 정책 조회
2. **개별 팝업 토글**: 각 정책별로 팝업 미리보기
3. **우선순위 실행**: 자동으로 우선순위에 따라 팝업 표시
4. **기록 초기화**: 모든 표시 기록 삭제 (테스트용)

## 의존성

`gradle/libs.versions.toml`:
```toml
[versions]
supabase = "2.7.2"
ktor = "2.3.12"

[libraries]
supabase-postgrest = { group = "io.github.jan-tennert.supabase", name = "postgrest-kt", version.ref = "supabase" }
supabase-serializer = { group = "io.github.jan-tennert.supabase", name = "serializer-kotlinx", version.ref = "supabase" }
ktor-client-android = { group = "io.ktor", name = "ktor-client-android", version.ref = "ktor" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version = "1.7.3" }

[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

## 주의사항

1. **Supabase URL과 Key**: 실제 프로덕션 환경에서는 BuildConfig로 관리
2. **SharedPreferences**: 표시 기록은 로컬에 저장됨
3. **버전 비교**: Semantic Versioning (x.y.z) 형식 사용
4. **네트워크 에러**: 정책 로딩 실패 시 빈 결과 반환
5. **테스트**: 디버그 모드에서 충분히 테스트 후 프로덕션 적용

## 확장 가능성

- **A/B 테스팅**: 사용자 그룹별 다른 정책 적용
- **지역화**: 다국어 정책 지원
- **분석**: 팝업 표시/클릭 이벤트 추적
- **동적 업데이트**: 앱 재시작 없이 정책 갱신

