# Supabase 팝업 정책 시스템 구현 완료

## 📋 구현된 기능

### ✅ 완료된 작업

1. **데이터 모델 구현** (4개 파일)
   - `EmergencyPolicy.kt` - 긴급 공지 정책
   - `NoticePolicy.kt` - 일반 공지사항 정책
   - `UpdatePolicy.kt` - 업데이트 정책
   - `PopupDecision.kt` - 팝업 결정 sealed class

2. **저장소 계층 구현** (4개 파일)
   - `EmergencyPolicyRepository.kt` - 긴급 공지 관리
   - `NoticePolicyRepository.kt` - 일반 공지 관리
   - `UpdatePolicyRepository.kt` - 업데이트 정책 관리
   - `PopupPolicyManager.kt` - 통합 정책 매니저

3. **인프라 구현** (2개 파일)
   - `SupabaseProvider.kt` - Supabase 클라이언트 싱글톤
   - `DebugViewModel.kt` - 디버그 화면용 ViewModel

4. **UI 연동**
   - `DebugActivity.kt` 수정
     - Supabase 정책 실시간 조회
     - 각 정책별 팝업 미리보기
     - 우선순위 자동 실행
     - 표시 기록 초기화

5. **의존성 추가**
   - Supabase Kotlin SDK (v3.0.2)
   - Ktor Android 클라이언트 (v3.0.1)
   - Kotlinx Serialization

6. **BuildConfig 설정 (✅ 완료)**
   - `app/build.gradle.kts`에 SUPABASE_URL, SUPABASE_KEY 추가
   - 환경변수 또는 local.properties 방식 지원
   - `SupabaseProvider.kt`에서 BuildConfig 사용

7. **문서화**
   - 📘 `SUPABASE_POPUP_INTEGRATION_GUIDE.md` - 전체 통합 가이드
   - 📋 `SUPABASE_IMPLEMENTATION_SUMMARY.md` - 구현 요약
   - ✅ `SUPABASE_SETUP_CHECKLIST.md` - 설정 체크리스트
   - 🔑 `SUPABASE_API_SETUP.md` - **API 설정 가이드 (신규)**

## 🎯 팝업 우선순위

```
1. 긴급 공지 (최우선)
   ↓
2. 강제 업데이트
   ↓
3. 일반 공지
   ↓
4. 선택적 업데이트
```

## 📁 파일 구조

```
app/src/main/java/kr/sweetapps/alcoholictimer/data/supabase/
├── SupabaseProvider.kt
├── model/
│   ├── EmergencyPolicy.kt
│   ├── NoticePolicy.kt
│   ├── UpdatePolicy.kt
│   └── PopupDecision.kt
└── repository/
    ├── EmergencyPolicyRepository.kt
    ├── NoticePolicyRepository.kt
    ├── UpdatePolicyRepository.kt
    └── PopupPolicyManager.kt

app/src/debug/java/kr/sweetapps/alcoholictimer/feature/debug/
├── DebugActivity.kt (수정됨)
└── DebugViewModel.kt (신규)
```

## 🔧 설정 필요 사항

### 1. Supabase 프로젝트 설정

Supabase 대시보드에서 다음 테이블을 생성해야 합니다:
- `emergency_policies`
- `notice_policies`
- `update_policies`

자세한 스키마는 `SUPABASE_POPUP_INTEGRATION_GUIDE.md` 참조

### 2. BuildConfig 설정 (✅ 완료)

**이미 설정되어 있습니다!** `app/build.gradle.kts`에 다음이 추가되어 있습니다:

```kotlin
android {
    defaultConfig {
        // Supabase 설정 (환경변수 또는 기본값)
        val supabaseUrl = System.getenv("SUPABASE_URL") ?: "https://your-project.supabase.co"
        val supabaseKey = System.getenv("SUPABASE_KEY") ?: "your-anon-key"
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
    }
}
```

**API 키 설정 방법**: `docs/SUPABASE_API_SETUP.md` 참조

간단하게는 프로젝트 루트의 `local.properties`에 추가:
```properties
supabase.url=https://your-project-id.supabase.co
supabase.key=your-anon-key-here
```

### 3. SupabaseProvider 수정 (✅ 완료)

`SupabaseProvider.kt`가 이미 BuildConfig를 사용하도록 수정되었습니다:

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

## 🧪 테스트 방법

### DebugActivity에서 테스트

1. 앱 실행
2. 디버그 메뉴 진입
3. "공지사항 (Supabase 연동)" 카드 확인
4. 각 기능 테스트:
   - 정책 로딩 (자동 실행)
   - 개별 팝업 토글
   - 우선순위 실행 버튼
   - 기록 초기화 버튼

### 프로덕션 적용

`MainActivity` 또는 앱 시작점에서:

```kotlin
class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 팝업 정책 확인 및 표시
        checkAndShowPolicies()
    }
    
    private fun checkAndShowPolicies() {
        lifecycleScope.launch {
            val client = SupabaseProvider.getClient(this@MainActivity)
            val policyManager = PopupPolicyManager(
                EmergencyPolicyRepository(client, this@MainActivity),
                UpdatePolicyRepository(client, this@MainActivity),
                NoticePolicyRepository(client, this@MainActivity),
                this@MainActivity
            )
            
            val currentVersion = packageManager
                .getPackageInfo(packageName, 0)
                .versionName
            
            when (val decision = policyManager.decidePopup(currentVersion)) {
                is PopupDecision.ShowEmergency -> {
                    showEmergencyDialog(decision.policy)
                    policyManager.markEmergencyShown(decision.policy.id)
                }
                is PopupDecision.ShowNotice -> {
                    showNoticeDialog(decision.policy)
                    policyManager.markNoticeShown(decision.policy.id)
                }
                is PopupDecision.ShowUpdate -> {
                    showUpdateDialog(decision.policy)
                }
                is PopupDecision.None -> {
                    // 표시할 팝업 없음
                }
            }
        }
    }
}
```

## 📊 로컬 저장소

정책 표시 기록은 SharedPreferences에 저장됩니다:

- **emergency_policy_prefs** - 긴급 공지 표시 기록
- **notice_policy_prefs** - 일반 공지 표시 기록
- **update_policy_prefs** - 업데이트 무시 기록

## 🔍 디버깅

### 로그 확인

각 Repository에서 예외 발생 시 `e.printStackTrace()` 호출

### 네트워크 확인

Supabase 접근이 안 되는 경우:
1. 인터넷 연결 확인
2. Supabase URL/Key 확인
3. 테이블 존재 여부 확인
4. RLS (Row Level Security) 정책 확인

## 📚 참고 문서

- `SUPABASE_POPUP_INTEGRATION_GUIDE.md` - 전체 가이드
- Supabase Kotlin SDK: https://github.com/supabase-community/supabase-kt

## ⚠️ 주의사항

1. **현재 상태**: Supabase URL/Key가 더미 값으로 설정되어 있음
2. **다음 단계**: 실제 Supabase 프로젝트 생성 후 설정 필요
3. **테스트**: 디버그 모드에서 충분히 테스트 후 프로덕션 적용
4. **보안**: Supabase Key는 환경변수로 관리 권장

## ✨ 추가 기능 제안

- [ ] 팝업 표시 분석 이벤트 추가
- [ ] 다국어 정책 지원
- [ ] A/B 테스팅 기능
- [ ] 캐싱 전략 구현
- [ ] 오프라인 모드 지원

