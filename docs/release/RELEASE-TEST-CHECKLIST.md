# 🚀 릴리즈 테스트 체크리스트

**버전**: v3.0 | **최종 업데이트**: 2025-11-10 | **소요**: 약 40-50분

---

## 📋 목차
1. [사전 준비](#1-사전-준비)
2. [Phase 1: Emergency Policy](#2-phase-1-emergency-policy)
3. [Phase 2: Update Policy](#3-phase-2-update-policy)
4. [Phase 3: Notice Policy](#4-phase-3-notice-policy)
5. [Phase 4: 우선순위 테스트](#5-phase-4-우선순위-테스트)
6. [Phase 5: Ad Policy](#6-phase-5-ad-policy)
7. [최종 확인](#7-최종-확인)

---
## 1 사전 준비

### 테스트 환경 선택

| 빌드 타입 | app_id | 용도 |
|----------|--------|------|
| 릴리즈 | `com.sweetapps.pocketchord` | 실제 사용자 환경 검증 |
| 디버그 | `com.sweetapps.pocketchord.debug` | 빠른 테스트 |

### 준비 사항
- [ ] Supabase 접속, Android Studio/Logcat 준비(tag:HomeScreen | AppPolicyRepo)
- [ ] 테스트 기기/에뮬레이터 연결

### 초기 상태 확인(SQL, release+debug 병기)
```sql
SELECT 'emergency' t, app_id, is_active::text, LEFT(content,30) preview FROM emergency_policy WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug')
UNION ALL
SELECT 'update', app_id, is_active::text, CONCAT('target:',target_version_code,' force:',is_force_update) FROM update_policy WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug')
UNION ALL
SELECT 'notice', app_id, is_active::text, CONCAT('v',notice_version,': ',LEFT(COALESCE(title,''),20)) FROM notice_policy WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug')
UNION ALL
SELECT 'ad', app_id, is_active::text, CONCAT('open:',ad_app_open_enabled,' inter:',ad_interstitial_enabled,' banner:',ad_banner_enabled) FROM ad_policy WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug')
ORDER BY 1,2;
```
기록:
```
emergency: is_active=__
update: target=__ force=__
notice: version=__
ad: is_active=__ open=__ inter=__ banner=__
```

---
## 2 Phase 1: Emergency Policy

빠른 체크리스트

| 항목 | 확인 |
|------|------|
| X 버튼 있음 (is_dismissible=true) | ⬜ |
| X 버튼 없음 (is_dismissible=false) | ⬜ |
| 뒤로가기 불가(강제) | ⬜ |
| 재실행 시 다시 표시(추적 없음) | ⬜ |
| 정리(비활성화) | ⬜ |

---
## 3 Phase 2: Update Policy

빠른 체크리스트

| 항목 | 확인 |
|------|------|
| 강제 업데이트 (is_force_update=true) | ⬜ |
| 선택적 업데이트 (is_force_update=false) | ⬜ |
| "나중에" 클릭 후 추적 | ⬜ |
| SharedPreferences 초기화 | ⬜ |
| 정리 (target_version_code=1) | ⬜ |

---
## 4 Phase 3: Notice Policy

빠른 체크리스트

| 항목 | 확인 |
|------|------|
| 공지 활성화 및 표시 | ⬜ |
| 오타 수정(버전 유지) → 재표시 안 됨 | ⬜ |
| 새 공지(버전 증가) → 재표시됨 | ⬜ |
| 정리(비활성화) | ⬜ |

---
## 5 Phase 4: 우선순위 테스트

빠른 체크리스트

| 항목 | 확인 |
|------|------|
| Emergency + Update → Emergency만 표시 | ⬜ |
| Update + Notice → Update만 표시 | ⬜ |
| 모두 비활성화 → 팝업 없음 | ⬜ |

---
## 6 Phase 5: Ad Policy

### 6.1 RLS 정책 수정 (5.1, 최초 1회)

빠른 체크리스트

| 항목 | 확인 |
|------|------|
| ad_policy SELECT RLS가 USING(true)로 설정됨 | ⬜ |
| is_active=false 행도 조회됨(에러 없음) | ⬜ |
| 앱 재시작 후 정책 즉시 반영 확인 | ⬜ |

참고 쿼리:
```sql
SELECT schemaname, tablename, policyname, cmd, qual
FROM pg_policies WHERE tablename='ad_policy';
```
예상: policyname=ad_policy_select_all, qual=true

### 6.2 개별 광고 제어 (5.2)

빠른 체크리스트

| 항목 | 확인 |
|------|------|
| App Open OFF → 백그라운드→포그라운드 시 미표시 | ⬜ |
| Interstitial OFF → 조건 충족에도 미표시(60초, 패턴) | ⬜ |
| Banner OFF → 미표시(실행 중 최대 3분 내 반영) | ⬜ |
| 복구: App Open/Interstitial/Banner 모두 ON | ⬜ |

로그 필터: `tag:AdPolicyRepo | tag:AppOpenAdManager | tag:InterstitialAdManager | tag:MainActivity`

### 6.3 빈도 제한 및 최종 검증 (5.3)

빈도 제한(선택): 시간당=1, 일일=3으로 낮춰 1회 표시 후 차단 확인 → 기본값 복구(2/15).

빠른 체크리스트

| 항목 | 확인 |
|------|------|
| 시간당/일일 제한 낮추기 적용 | ⬜ |
| 1회 표시 후 시간당 제한으로 차단 확인 | ⬜ |
| 기본값 복구(ad_interstitial_max_per_hour=2, per_day=15) | ⬜ |
| 최종 설정: is_active=true, AppOpen/Interstitial/Banner 모두 ON | ⬜ |

최종 설정 확인(SQL, release+debug 병기)
```sql
SELECT app_id,is_active,ad_app_open_enabled,ad_interstitial_enabled,ad_banner_enabled,
       ad_interstitial_max_per_hour,ad_interstitial_max_per_day
FROM ad_policy
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug')
ORDER BY app_id;
```

---
## 7 최종 확인

### 핵심 동작 확인

| Policy | 핵심 확인 | 완료 |
|--------|----------|------|
| Emergency | 최우선 표시, X 제어, 추적 없음 | ⬜ |
| Update | 강제/선택, 버전 추적 | ⬜ |
| Notice | 버전 추적, 증가 시 재표시 | ⬜ |
| 우선순위 | emergency > update > notice | ⬜ |

### 테스트 결과 요약

| Phase | 항목 | 결과 |
|-------|------|------|
| 1 | Emergency (X 있음/없음) | ⬜ PASS / ⬜ FAIL |
| 2 | Update (강제/선택적) | ⬜ PASS / ⬜ FAIL |
| 3 | Notice (버전 관리) | ⬜ PASS / ⬜ FAIL |
| 4 | 우선순위 | ⬜ PASS / ⬜ FAIL |
| 5 | 종합 시나리오 | ⬜ PASS / ⬜ FAIL |

### 발견된 이슈
```
1. _______________________________
2. _______________________________
```

### 릴리즈 승인
- [ ] 모든 Phase PASS
- [ ] 이슈 0개 또는 모두 해결
- [ ] 프로덕션 상태 확인 완료
- [ ] 릴리즈 준비 완료 🚀

**테스트 완료 일시**: ____________  |  **테스터**: ____________

---
**문서 버전**: v3.0  |  **마지막 수정**: 2025-11-10

## 📝 부록 (간단 초기화)

```sql
-- 평상시 상태 초기화 (release+debug 병기)
UPDATE emergency_policy SET is_active=false WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
UPDATE update_policy SET target_version_code=1,is_force_update=false WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
UPDATE notice_policy SET is_active=true, title='환영합니다! 🎉', content='PocketChord를 이용해 주셔서 감사합니다!', notice_version=1 WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```
앱 데이터 초기화(디버그):
```bash
adb shell pm clear com.sweetapps.pocketchord.debug
```
