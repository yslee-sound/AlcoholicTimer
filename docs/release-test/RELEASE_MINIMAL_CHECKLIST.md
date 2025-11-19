요약: 빠른 릴리즈용 최소 검증 체크리스트

목표: 다음 항목만 빠르게 확인 후 릴리즈
1) 배너 광고 정상작동
2) 앱 오프닝 광고 정상작동
3) 전면광고는 이번 단계에서 사용 안함(비활성)
4) 1~2번의 Supabase 연동 확인
5) 업데이트 팝업 정상작동
6) 업데이트 팝업 Supabase 연동 확인

-- 시작 가이드 (간단한 순서)
1. 코드/설정 점검 (로컬)
   - `app/build.gradle.kts`에서 아래 BuildConfig 값 확인
     - `ADMOB_BANNER_UNIT_ID` (debug: 테스트 ID, release: 실 ID)
     - `ADMOB_APP_OPEN_UNIT_ID`
     - `ADMOB_INTERSTITIAL_UNIT_ID` (이번 릴리즈: 빈값 또는 비활성)
   - 파일 위치 참고:
     - 배너 구현: `app/src/main/java/.../core/ui/AdBanner.kt`
     - App Open: `app/src/main/java/.../core/ads/AppOpenAdManager.kt`
     - Interstitial: `core/ads/InterstitialAdManager.kt` (사용 안함)
     - Popup 매니저: `data/supabase/repository/PopupPolicyManager.kt`
     - Update policy repo: `data/supabase/repository/UpdatePolicyRepository.kt`

2. Supabase 상태 확인 (SQL)
   - Ads 정책(예시 테이블명: `ad_policy`) 확인
     ```sql
     SELECT * FROM ad_policy WHERE app_id = 'kr.sweetapps.alcoholictimer' AND is_active = true;
     ```
   - Emergency/Notice/Update 정책 확인
     ```sql
     SELECT * FROM emergency_policy WHERE app_id = 'kr.sweetapps.alcoholictimer' AND is_active = true;
     SELECT * FROM notice_policies WHERE app_id = 'kr.sweetapps.alcoholictimer' AND is_active = true;
     SELECT * FROM update_policies WHERE app_id = 'kr.sweetapps.alcoholictimer' AND is_active = true;
     ```
   - Update 팝업용 레코드가 있으면 `version`, `is_force_update`, `message` 필드 확인

3. 디버그 빌드로 앱 실행
   - build + install (cmd.exe)
     ```bash
     cd G:\Workspace\AlcoholicTimer
     ./gradlew.bat assembleDebug installDebug
     ```

4. 로그로 배너 정상 확인
   - 앱 실행 후 로그에서 다음 태그 확인
     - `AdmobBanner` (배너 로드/disabled 메시지)
     - `BaseScaffold` (배너 렌더 호출)
   - adb log 명령
     ```bash
     adb logcat | grep -E "(AdmobBanner|BaseScaffold|AdController)"
     ```
   - 정상 시 로그 예시
     - `AdmobBanner: Banner enabled: true` 또는 `Banner disabled by policy`
     - `AdmobBanner: onAdLoaded` 등

5. App Open 광고 확인
   - 동작 시점: 앱 백그라운드 -> 포그라운드 전환 (앱 재실행 또는 홈후 복귀)
   - 로그 태그: `AppOpenAdManager`, `AdController`
     ```bash
     adb logcat | grep -E "(AppOpenAdManager|AdController)"
     ```
   - 정상 시: `AppOpenAdManager: AppOpen ad shown` 또는 `ad loaded` 로그
   - 실패 시: `ad failed to load: ...` 메시지 확인

6. Interstitial (이번 단계 비활성화)
   - `InterstitialAdManager` 설정에서 show 호출이 없도록 확인
   - `BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID`를 빈 문자열로 두거나, `AdController` 정책에서 interstitial 비활성화
   - Supabase에서 전면 광고 관련 flag도 비활성화 (ad_interstitial_enabled = false)

7. Supabase 연동 검사 (실패 상황 원인 찾기)
   - 앱 로그에서 정책 로드 관련 태그 확인
     - `AdController` 또는 `EmergencyPolicyRepo` 등 (문서에 정의된 태그)
   - TTL/캐시 이슈: 정책 변경 후 즉시 반영되지 않으면 캐시 만료 시간(예: 3분) 확인
   - app_id 일치 여부: `context.packageName`과 Supabase 레코드의 `app_id` 확인
   - RLS 권한 문제: anon key로 조회 가능한지 확인

8. 업데이트 팝업 동작 확인
   - DebugView (없으면 DebugActivity의 PolicyDecisionButton 사용)에서 ‘우선순위대로 팝업 표시’ 실행
   - 또는 Supabase에 UpdatePolicy 레코드 추가 후 앱에서 `policyManager.decidePopup()` 호출
   - 로그 태그: `PopupPolicyManager`, `UpdatePolicyRepository`
   - 정상 시: update 팝업(강제/선택) 다이얼로그 표시

9. 업데이트 팝업 Supabase 연동 확인
   - SQL로 레코드 존재 확인 (위 2번)
   - 앱 로그에서 정책 파싱/매핑 로그 확인
   - 표시 후 `markNoticeShown` 또는 dismiss 기록이 로컬 저장소에 기록되는지 확인

10. 최종 확인 및 릴리즈 준비
    - Debug에서 정상 동작 확인되면 Release 빌드 생성
      ```bash
      ./gradlew.bat assembleRelease
      ```
    - 릴리즈 빌드에서 광고 유닛 ID(실제) 적용 확인
    - 내부 테스트 트랙으로 배포하여 실제 광고 동작 확인

-- 추가 팁 및 빠른 문제해결
- 정책이 반영되지 않을 때
  - `adb logcat | grep -i EmergencyPolicyRepo` 로 정책 fetch 로그 확인
  - Debug에서 표시 기록 초기화: `policyManager.clearAllRecords()` 또는 디버그 화면의 ‘표시 기록 초기화’ 버튼 사용
- 앱에서 app_id가 다른 경우 (디버그 vs release) Supabase 레코드 확인 후 `app_id` 두 개 생성
- 긴급 팝업이 뜨지 않을 경우
  - Supabase row의 `is_active` true 확인
  - `app_id` 일치 확인
  - 표시 이력(이미 표시됨) 여부 확인(SharedPreferences 기준)

문서 경로: `docs/release-test/RELEASE_MINIMAL_CHECKLIST.md`
작성 완료.
