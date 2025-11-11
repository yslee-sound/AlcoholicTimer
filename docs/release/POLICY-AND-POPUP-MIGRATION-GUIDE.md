# Supabase 정책 팝업 + 광고정책 이식 가이드 (2025-11-11)

이 문서는 PocketChord 앱에 구현된 "3가지 Supabase 팝업(응급 / 업데이트 / 공지) + 광고정책(AdPolicy) + 광고 매니저(AppOpen/Interstitial)" 를 다른 앱에 **파일을 그대로 복사 → 같은 상대 경로 배치 → 단계별 연결** 방식으로 안전하게 이식하기 위한 전체 절차를 제공합니다.

---
## 0. 전체 개요
- 목표: 정책/광고/팝업 로직을 재사용 가능한 형태로 다른 Android 앱(Compose 기반 권장)에 적용
- 구성 요소:
  - Supabase 초기화(Application)
  - 데이터 모델 (UpdatePolicy, EmergencyPolicy, NoticePolicy, AdPolicy, UpdateInfo, Announcement)
  - Repository (각 정책별 4개 + 필요 시 AnnouncementRepository)
  - 광고 매니저 (AppOpenAdManager, InterstitialAdManager)
  - 팝업 UI (OptionalUpdateDialog, EmergencyRedirectDialog, AnnouncementDialog)
  - 오케스트레이션 로직 (우선순위: emergency > update(force/optional) > notice) – `HomeScreen.kt` 내부
  - BuildConfig 필드 및 환경 변수
  - SharedPreferences 상태 추적 키 세트
- 참고: 본 가이드는 Jetpack Compose 기반을 전제로 작성되었습니다. (비-Compose 앱은 DialogFragment 등으로 UI를 대체하거나 ComposeView 임베딩 필요)

---
## 1. 사전 점검 체크리스트
다른 앱(이하 "대상 앱")에서 아래 항목을 먼저 확인하세요.

| 항목 | 필요 여부 | 설명 |
|------|-----------|------|
| Jetpack Compose 사용 | 팝업 UI 그대로 쓰려면 필수 | 미사용 시 DialogFragment 로 재작성 필요 |
| Kotlin Serialization | 모델(@Serializable) 역직렬화에 필요 | Gradle plugin + dependency 추가 |
| Application 클래스 존재 | Supabase & Ads 초기화 삽입 | 기존 Application 확장/병합 |
| minSdk ≥ 26 | 원본과 동일 | 낮으면 일부 API 조정 필요 |
| Google Mobile Ads SDK | 광고 표시 필수 | 의존성/메타데이터 설정 |
| Supabase URL/Anon Key 확보 | 정책 조회 필수 | 환경변수 또는 secrets 저장 |

에이전트 입력 템플릿:
```
[사전 점검]
- Compose: (예/아니오)
- Serialization plugin: (예/추가 필요)
- Application 클래스: (존재/생성 예정 파일명)
- minSdk/targetSdk: (값)
- Ads SDK 유무: (예/추가 필요)
- Supabase 키 확보: (예/진행중)
```

---
## 2. 원본 소스 파일 목록 (복사 대상)
복사 대상은 아래 경로(상대) 기준입니다. 대상 앱 구조에 맞춰 동일/유사 위치로 배치하세요.

### 2.1 Application & 진입 화면
- `app/src/main/java/com/sweetapps/pocketchord/PocketChordApplication.kt`
- (팝업 호출 예시) `app/src/main/java/com/sweetapps/pocketchord/ui/screens/HomeScreen.kt` (오케스트레이션 블록만 추출 가능)

### 2.2 Supabase 모델
```
app/src/main/java/com/sweetapps/pocketchord/data/supabase/model/UpdatePolicy.kt
app/src/main/java/com/sweetapps/pocketchord/data/supabase/model/EmergencyPolicy.kt
app/src/main/java/com/sweetapps/pocketchord/data/supabase/model/NoticePolicy.kt
app/src/main/java/com/sweetapps/pocketchord/data/supabase/model/AdPolicy.kt
app/src/main/java/com/sweetapps/pocketchord/data/supabase/model/UpdateInfo.kt
app/src/main/java/com/sweetapps/pocketchord/data/supabase/model/Announcement.kt
```
(필요 시 `PopupDecision.kt` 참고용)

### 2.3 Repositories
```
app/src/main/java/com/sweetapps/pocketchord/data/supabase/repository/UpdatePolicyRepository.kt
app/src/main/java/com/sweetapps/pocketchord/data/supabase/repository/EmergencyPolicyRepository.kt
app/src/main/java/com/sweetapps/pocketchord/data/supabase/repository/NoticePolicyRepository.kt
app/src/main/java/com/sweetapps/pocketchord/data/supabase/repository/AdPolicyRepository.kt
app/src/main/java/com/sweetapps/pocketchord/data/supabase/repository/AnnouncementRepository.kt (선택)
```

### 2.4 광고 매니저
```
app/src/main/java/com/sweetapps/pocketchord/ads/AppOpenAdManager.kt
app/src/main/java/com/sweetapps/pocketchord/ads/InterstitialAdManager.kt
```

### 2.5 팝업 UI
```
app/src/main/java/com/sweetapps/pocketchord/ui/dialogs/OptionalUpdateDialog.kt
app/src/main/java/com/sweetapps/pocketchord/ui/dialogs/EmergencyRedirectDialog.kt
app/src/main/java/com/sweetapps/pocketchord/ui/dialogs/NoticeDialog.kt  (AnnouncementDialog 포함)
```

### 2.6 리소스 (drawable)
```
app/src/main/res/drawable/update_sample.xml
app/src/main/res/drawable/emergency_notice.xml
```
필요 시 이름 충돌 방지를 위해 `policy_update_sample.xml` 등으로 변경하고 코드 수정.

### 2.7 Manifest 설정
- `<application android:name=".PocketChordApplication" ...>` 부분
- AdMob APP ID meta-data

에이전트 입력 템플릿 (복사 진행 상황 기록):
```
[파일 복사 체크]
- Application.kt: (완료/보류)
- Models: (n/6 완료 목록)
- Repositories: (n/5 완료 목록)
- Ads Managers: (n/2 완료)
- Dialogs: (n/3 완료)
- Drawables: (완료/보류)
- Manifest 수정: (완료/보류)
```

### 2.8 파일 복사 순서 권장 (컴파일 오류 최소화)
1) 모델(model) → 2) 리포지터리(repository) → 3) 팝업 UI(dialogs) → 4) 광고 매니저(ads) → 5) Application 수정 → 6) Manifest 수정 → 7) 오케스트레이션(Home/Main 화면)
- 이유: 상위 계층이 하위 계층을 참조하므로(예: Repo가 Model 참조) 하위부터 순서대로 복사 시 미해결 심볼 발생을 줄입니다.
- 복사 직후 각 파일의 package 경로를 대상 앱 네임스페이스로 조정하세요.

---
## 3. Gradle 설정 이식
### 3.1 plugins 블럭
```
plugins {
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.serialization") // 없으면 추가
}
```
### 3.2 dependencies (핵심)
```
implementation(platform("io.github.jan-supabase:bom:<버전>"))
implementation("io.github.jan-supabase:postgrest-kt:<버전>")
implementation("io.ktor:ktor-client-android:<버전>")
implementation("com.google.android.gms:play-services-ads:22.6.0")
implementation("androidx.lifecycle:lifecycle-process:2.6.2")
// Compose 이미지 필요 시
implementation("io.coil-kt:coil-compose:2.6.0")
```
### 3.3 BuildConfig 필드
원본 예시를 참고해 대상 앱 `defaultConfig`와 `buildTypes`에 아래 추가:
```
buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl}\"")
buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supabaseAnonKey}\"")
buildConfigField("String", "SUPABASE_APP_ID", "\"com.example.targetapp\"")
// 광고 단위 ID
buildConfigField("String", "BANNER_AD_UNIT_ID", "\"<bannerId>\"")
buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"<interstitialId>\"")
buildConfigField("String", "APP_OPEN_AD_UNIT_ID", "\"<appOpenId>\"")
```
에이전트 입력 템플릿:
```
[Gradle 적용]
- Serialization plugin 추가 여부: (예/아니오)
- Supabase BOM/Deps 추가: (예/아니오)
- Ads SDK 추가: (예/아니오)
- BuildConfig 필드 삽입: (예/목록)
```

---
## 4. Supabase 테이블 DDL & 정책
### 4.1 최소 DDL (Postgres)
```sql
-- update_policy
CREATE TABLE public.update_policy (
  id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  app_id TEXT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  target_version_code INT NOT NULL,
  is_force_update BOOLEAN NOT NULL,
  release_notes TEXT,
  download_url TEXT,
  reshow_interval_hours INT,
  reshow_interval_minutes INT,
  reshow_interval_seconds INT,
  max_later_count INT
);
CREATE INDEX ON public.update_policy(app_id);

-- emergency_policy
CREATE TABLE public.emergency_policy (
  id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  app_id TEXT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  content TEXT NOT NULL,
  redirect_url TEXT,
  button_text TEXT,
  is_dismissible BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX ON public.emergency_policy(app_id);

-- notice_policy
CREATE TABLE public.notice_policy (
  id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  app_id TEXT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  title TEXT,
  content TEXT NOT NULL,
  notice_version INT NOT NULL DEFAULT 1,
  image_url TEXT,
  action_url TEXT
);
CREATE INDEX ON public.notice_policy(app_id);

-- ad_policy
CREATE TABLE public.ad_policy (
  id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  app_id TEXT NOT NULL UNIQUE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  ad_app_open_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  ad_interstitial_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  ad_banner_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  ad_interstitial_max_per_hour INT NOT NULL DEFAULT 2,
  ad_interstitial_max_per_day INT NOT NULL DEFAULT 15
);
CREATE INDEX ON public.ad_policy(app_id);
```
### 4.2 초기 데이터 (예시)
```sql
INSERT INTO ad_policy(app_id) VALUES ('com.example.targetapp');
INSERT INTO update_policy(app_id, target_version_code, is_force_update, is_active) VALUES ('com.example.targetapp', 5, FALSE, TRUE);
INSERT INTO notice_policy(app_id, content, notice_version, is_active) VALUES ('com.example.targetapp', '신규 기능 안내', 1, TRUE);
-- emergency_policy는 필요 시 추가
```
### 4.3 RLS/권한 (선택)
- 운영 중 앱 다수 관리 시 app_id 기반 필터 정책 추가
- 단순 공개 조회였다면 익명 키로 read 허용 (정책 데이터만) → write 제한

에이전트 입력 템플릿:
```
[Supabase DDL 적용]
- 테이블 생성 완료 여부: (update/emergency/notice/ad)
- 인덱스 생성: (예/아니오)
- 초기 정책 행 삽입: (목록)
- RLS 설정: (적용/미적용/추후)
```

---
## 5. Application 통합
1) 대상 앱 `Application` 클래스 열기
2) Supabase 초기화 블록 삽입 (URL/KEY BuildConfig 반영)
3) AdMob 초기화(MobileAds.initialize)
4) AppOpenAdManager 인스턴스 생성 (필요 시 프로퍼티 추가)
5) `isSupabaseConfigured` 로그 처리 (빈 문자열 대응)

에이전트 입력 템플릿:
```
[Application 수정]
- Supabase URL/Key 읽기: (코드 삽입 완료)
- Postgrest install: (예/아니오)
- AdMob 초기화: (예/아니오)
- AppOpenAdManager 생성: (예/아니오)
- isSupabaseConfigured 플래그: (예/아니오)
```

---
## 6. 팝업 오케스트레이션 로직 통합
권장: 대상 앱 메인 화면(Compose) 또는 Splash 이후 첫 화면에서 아래 순서 실행.
- Phase 1: EmergencyPolicy 조회 → 있으면 즉시 Dialog 표시 & 종료
- Phase 2: UpdatePolicy 조회 → 강제/선택 로직 + 시간/횟수 기반(Phase 2.5) 재표시 결정
- Phase 3: NoticePolicy 조회 → notice_version 기반 최초표시/재표시 제어
- finally: hasCheckedPopups = true 로 중복 실행 방지

필수 SharedPreferences 키 (원본):
- update 강제/선택: `force_required_version`, `force_update_info`, `update_dismissed_time`, `update_later_count`, `dismissedVersionCode`, `dismissed_version_code`
- notice: `viewed_notices`
- 광고: `ad_count_hourly`, `ad_count_daily`, `last_hour_reset`, `last_day_reset`, `last_ad_show_time`
(다른 앱과 충돌 방지 위해 prefix 가능: `pc_` 등)

에이전트 입력 템플릿:
```
[팝업 오케스트레이션]
- Emergency 호출 위치 구현: (예/라인 번호)
- Update 정책 처리 및 강제/선택 분기: (예/라인 번호)
- Phase 2.5 시간/횟수 로직: (예/라인 번호)
- Notice 버전 체크: (예/라인 번호)
- hasCheckedPopups 플래그: (예/라인 번호)
```

---
## 7. 광고 매니저 연결
- `AppOpenAdManager`: Application onCreate에서 초기화 + ActivityLifecycleCallbacks 등록
- `InterstitialAdManager`: 화면 전환 시 `recordScreenTransition()` 호출 후 조건 충족 시 `showAd()` 또는 `tryShowAd()` 호출
- Supabase 비활성 또는 실패 시 기본 활성 로직 유지 (정책 없는 경우 true 처리)

에이전트 입력 템플릿:
```
[광고 매니저]
- AppOpenAdManager 초기화: (예/아니오)
- InterstitialAdManager 생성 위치: (클래스/라인)
- 화면 전환 기록 호출: (예/메서드명)
- Supabase 정책 fallback 로그 확인: (예/LogTag)
```

---
## 8. 리소스 / 문자열 국제화 (선택 개선)
하드코딩된 한국어 문자열을 `res/values/strings.xml` 로 이전 권장:
- "앱 업데이트", "지금 업데이트", "나중에", "긴급", "공지" 등
국제화 준비 시 다국어 폴더(`values-en`, `values-ja`) 추가.

에이전트 입력 템플릿:
```
[i18n 개선]
- 문자열 추출 수: (n)
- strings.xml 추가 여부: (예/아니오)
- 다국어 준비: (언어 목록)
```

---
## 9. 테스트 전략
### 9.1 단위 테스트 포인트
- UpdatePolicy.requiresForceUpdate / recommendsOptionalUpdate
- 재표시 간격 계산(초/분/시) 우선순위
- laterCount >= maxLaterCount → 강제 전환
- Emergency 우선순위 상 모든 다른 팝업 skip
- Notice version 증가 시 재표시
- 광고 빈도 제한 (시간/일별 카운트 + 정책 최대치)
### 9.2 통합 테스트
- Supabase 비구성 상태(isSupabaseConfigured=false)에서 강제 업데이트 복원 동작
- 앱 첫 실행에서 expected Dialog sequence
- 뒤로가기 차단(강제 업데이트) 정상
### 9.3 에러/오프라인
- 네트워크 끊김 → emergency 캐시 없으면 아무 팝업 없이 진행 (개선 시 emergency 캐시 고려)

에이전트 입력 템플릿:
```
[테스트]
- 단위 테스트 클래스 생성: (목록)
- 핵심 케이스 통과: (예/실패 항목)
- 통합 테스트 시나리오 실행: (예/실패)
- 오프라인 시나리오 결과: (동작 설명)
```

---
## 10. 품질 & 릴리즈 전 점검

| 항목 | 설명 | 확인 |
|------|------|------|
| BuildConfig 필드 누락 시 경고 | isSupabaseConfigured=false 로그 |  |
| Proguard/R8 유지 | @Serializable 모델 유지 |  |
| 로그 레벨 | DEBUG 빌드만 상세 로그 |  |
| 팝업 중복 표시 방지 | hasCheckedPopups 플래그 정상 |  |
| 광고 정책 캐시 | 3분 캐시 만료 후 재조회 |  |
| Force Update BackHandler | 뒤로가기 차단 확인 |  |

에이전트 입력 템플릿:
```
[릴리즈 체크]
- Supabase 구성 여부: (True/False)
- 팝업 우선순위 실제 동작: (Emergency > Update > Notice) 확인됨?
- 광고 정책 캐시 만료 테스트: (예/시간)
- BackHandler 강제 업데이트: (예/아니오)
```

---
## 11. 안전한 롤백 전략

| 상황 | 조치 |
|------|------|
| 잘못된 강제 업데이트 | update_policy.is_active=false 또는 target_version_code 낮춤 |
| 응급 정책 오남용 | emergency_policy.is_active=false 후 앱 재시작 유도 |
| 광고 과도표시 | ad_policy 수정 (enabled=false) 또는 max_per_hour/day 낮춤 |
| Supabase 장애 | isSupabaseConfigured=false 상태 graceful skip (이미 구현) |

에이전트 입력 템플릿:
```
[롤백 시나리오]
- 발생 상황: (설명)
- 조치 명령/SQL: (기록)
- 적용 후 재테스트 결과: (성공/실패)
```

---
## 12. 선택적 리팩터링 (추후)
1. Decision Engine 추상화로 화면 의존 최소화
2. Supabase 쿼리: 전체 select → 조건 필터(Postgrest where)로 네트워크 절감
3. 정책 병렬 fetch + emergency 우선 취소(cancelChildren)
4. SharedPreferences 키 prefix 일괄 적용 (`pc_`)
5. 모듈 분리: `feature-policies`, `feature-ads`, `ui-policy`

에이전트 입력 템플릿:
```
[리팩터링 후보]
- 적용 대상: (#1~#5 중 선택)
- 구현 상태: (예/보류)
- 추가 가치: (설명)
```

---
## 13. 단계별 실행 요약 (실무용 스크립트 순서)
1) 브랜치 생성: `git checkout -b feature/policy-migration`
2) Gradle plugin & dependency 추가 → Sync
3) BuildConfig 필드 추가 → 빌드 성공 확인
4) Supabase DDL 실행 → 초기 데이터 삽입
5) Application 수정 & Supabase/AdMob 초기화
6) 모델 / Repository / 광고 매니저 / Dialog 파일 복사
7) 오케스트레이션 로직 대상 화면에 삽입
8) 리소스(drawable) 복사 및 이름 충돌 확인
9) SharedPreferences prefix 재검토
10) 테스트 코드 작성 & 실행
11) 로그/성능 최종 점검
12) 문서(본 파일) 갱신 & 코드 리뷰
13) 릴리즈 준비 → 정책 실서버 값 교체

에이전트 입력 템플릿:
```
[실행 진척]
- 1 브랜치: (완료)
- 2 Gradle: (완료)
- 3 BuildConfig: (완료)
- 4 DDL: (완료)
- 5 Application: (완료)
- 6 복사: (완료)
- 7 오케스트레이션: (완료)
- 8 리소스: (완료)
- 9 Pref 키: (완료)
- 10 테스트: (완료)
- 11 로그 점검: (완료)
- 12 리뷰: (완료)
- 13 릴리즈: (예정 날짜)
```

---
## 14. Windows (cmd) 예시 명령 (대상 앱 루트 = D:\TargetApp )
```
REM 예: PocketChord에서 모델 디렉터리 복사
xcopy /E /I /Y "G:\Workspace\PocketChord\app\src\main\java\com\sweetapps\pocketchord\data\supabase\model" "D:\TargetApp\app\src\main\java\com\example\targetapp\data\supabase\model"

REM 개별 파일 복사 예시
copy "G:\Workspace\PocketChord\app\src\main\java\com\sweetapps\pocketchord\ads\AppOpenAdManager.kt" "D:\TargetApp\app\src\main\java\com\example\targetapp\ads\AppOpenAdManager.kt"
```
(패키지명 변경 시 파일 상단 package 라인 수정 필요)

에이전트 입력 템플릿:
```
[명령 실행 로그]
- 복사 명령: (입력한 실제 명령)
- 결과: (성공/오류 메시지)
- 후속 작업: (패키지 수정 여부)
```

---
## 15. 최종 검증 시나리오 (실행 후 수동 확인)
1. Supabase URL/Key 미설정 → 경고 로그 출력 & 팝업 네트워크 fetch skip
2. 강제 업데이트 정책(target_version_code > 현재 versionCode, is_force_update=true) → 뒤로가기 차단
3. 선택 업데이트 정책(laterCount 증가) → maxLaterCount 도달 후 강제 전환
4. Notice 정책 notice_version 증가 → 새 공지 팝업 재표시
5. Emergency 정책 is_active=true → 다른 팝업 무시
6. 광고 정책 is_active=false → 모든 광고 비활성
7. 광고 interstitial 빈도 초과 → 로그 경고 후 표시 차단

에이전트 입력 템플릿:
```
[최종 검증]
- 미설정 Supabase 동작: (로그 캡처)
- 강제 업데이트 동작: (팝업/뒤로가기 차단 여부)
- Optional laterCount 전환: (카운트/강제 시점)
- Notice 버전 증가 반응: (표시/skip 여부)
- Emergency 최우선: (표시 순서)
- 광고 is_active 제어: (ON/OFF 결과)
- Interstitial 빈도 제한: (카운트/차단 로그)
```

---
## 16. 자주 발생하는 실수 & 예방

| 실수 | 증상 | 예방 |
|------|------|------|
| BuildConfig 필드 누락 | Null/빈 문자열로 Supabase 미구성 | Gradle Sync 후 로그 확인 |
| 패키지명 수정 누락 | Import/참조 오류 | 복사 직후 package 라인 점검 |
| 의존성 순서 문제 | Serialization decode 실패 | plugin 적용 후 모델 컴파일 우선 테스트 |
| SharedPreferences 키 충돌 | 다른 기능 데이터 덮어씀 | prefix 정책 문서화 |
| 광고 Unit ID 미교체 | 실제 릴리즈에 테스트 광고 | release 빌드 직전 체크리스트 활용 |

---
## 17. 다음 단계 (선택)
- 모듈화: 정책/광고 분리 후 Maven Local 배포로 재사용
- Remote Command: Supabase 함수로 광고 캐시 강제 무효화
- Analytics: 팝업 노출/업데이트 버튼 클릭/나중에 클릭 횟수 로깅

에이전트 입력 템플릿:
```
[추가 개선 선택]
- 모듈화 여부: (예/보류)
- 캐시 무효화 전략: (구현/미구현)
- 분석 로깅 추가: (설계/미설계)
```

---
## 부록 A. 간단 Decision 결과 모델 (참고)
```kotlin
sealed class PopupDecisionResult {
  data object None: PopupDecisionResult()
  data class Emergency(val policy: EmergencyPolicy): PopupDecisionResult()
  data class ForceUpdate(val info: UpdateInfo): PopupDecisionResult()
  data class OptionalUpdate(val info: UpdateInfo): PopupDecisionResult()
  data class Notice(val announcement: Announcement, val version: Int): PopupDecisionResult()
}
```

---
## 부록 B. 빠른 점검 명령 (cmd)
```
REM Gradle 빌드
gradlew.bat assembleDebug

REM 특정 로그 필터 (PowerShell 사용 예)
powershell -Command "Get-Content .\app\build\outputs\logcat.txt | Select-String 'Supabase configured'"
```

---
## 문서 활용 방법
1. 각 섹션을 순서대로 진행하며 에이전트에게 템플릿 블록을 그대로 입력
2. 에이전트는 해당 단계 작업만 수행 후 결과 회신
3. 완료 체크 후 다음 단계 이동 – 실패 시 재시도 또는 수정 지시
4. 모든 단계 완료 후 최종 검증 템플릿으로 회귀 테스트

행운을 빕니다. 필요한 추가 개선이나 스크립트 자동화가 필요하면 별도 섹션 확장 가능합니다.
