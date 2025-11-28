# 릴리즈 테스트 - Phase 5.3 (최종 검증 + 배포)

**버전**: v3.1 | **최종 업데이트**: 2025-11-11 | **소요**: 약 30-40분

---
## 📋 목차
1. [개요](#1-개요)
2. [빈도 제한 테스트(선택)](#2-빈도-제한-테스트선택)
3. [최종 검증](#3-최종-검증)
4. [완료 체크리스트](#4-완료-체크리스트)
5. [문제 해결](#5-문제-해결)

---
## 1 개요
목표(한 줄): 빈도 제한(시간당/일일) 확인 → 기본값 복구 → 최종 검증/배포 체크.
선행 조건: Phase 5.1·5.2 통과, Debug에서 광고 정상 표시.

---
## 2 빈도 제한 테스트(선택)

1) 제한 낮추기(SQL)
```sql
UPDATE ad_policy
SET ad_interstitial_max_per_hour = 1,
    ad_interstitial_max_per_day = 3
WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug');
```
2) 테스트
- 재실행(캐시 초기화) → 전면광고 표시 조건 충족(상세→홈 3회, 필요 시 60초 대기) → 1회 표시 ✅
- 다시 시도(동일 절차 + 60초 대기) → 시간당 제한으로 미표시 ❌
- 로그(요약)
- 필터: `tag:InterstitialAdManager | tag:AdPolicyRepo`
- 성공: `✅ 빈도 제한 통과: 시간당 0/1, 일일 0/3` → 표시/닫힘 로그
- 차단: `⚠️ 시간당 빈도 제한 초과: 1/1` → 미표시
- 복구: `UPDATE ad_policy SET ad_interstitial_max_per_hour = 2, ad_interstitial_max_per_day = 15`
3) App Open 빈도 제한 테스트(추가, 콜드 스타트 기준)
 - Supabase에서 `app_open_max_per_hour`와 `app_open_max_per_day`를 낮춰 테스트합니다 (예: 1/3).
 - 주의: App Open은 이제 "앱 시작(콜드 스타트)" 시에만 트리거됩니다. 따라서 App Open 빈도 제한 테스트는 반드시 앱 프로세스를 완전 종료한 뒤 재실행(cold start)을 반복하여 수행하세요.
 - 앱을 재실행한 후 App Open을 여러 번 트리거(콜드 스타트 반복)하여 초과 시 `AppOpenAdManager` 로그에서 `showIfAvailable abort: AppOpen limit reached by policy`를 확인합니다.
 - 성공 로그: `📝 AppOpen shown recorded (total: N)` (AdController)
 - 차단 로그: `showIfAvailable abort: AppOpen limit reached by policy` (AppOpenAdManager)
 - 복구: Supabase에서 값을 원래대로 변경하거나 기본값 2/15로 복원

---
## 3 최종 검증

### 3.1 출시 전 기본값 일괄 설정(SQL)
```sql
-- 1) ad_policy: 광고 활성화
UPDATE ad_policy 
SET is_active = true,
    ad_app_open_enabled = true,
    ad_interstitial_enabled = true,
    ad_banner_enabled = true,
    ad_interstitial_max_per_hour = 2,
    ad_interstitial_max_per_day = 15
WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug');

-- 2) update_policy: 비활성화
UPDATE update_policy SET is_active = false
WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug');

-- 3) emergency_policy: 비활성화
UPDATE emergency_policy SET is_active = false
WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug');

-- 4) notice_policy: 비활성화
UPDATE notice_policy SET is_active = false
WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug');

-- 5) 확인
SELECT 'ad_policy' as t, app_id, is_active FROM ad_policy WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug')
UNION ALL
SELECT 'update_policy', app_id, is_active FROM update_policy WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug')
UNION ALL
SELECT 'emergency_policy', app_id, is_active FROM emergency_policy WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug')
UNION ALL
SELECT 'notice_policy', app_id, is_active FROM notice_policy WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug')
ORDER BY 1, 2;
```
기대: ad_policy=true, 나머지=false (release/debug 각 1행).

### 3.2 광고 설정 확인(SQL)
```sql
SELECT app_id,is_active,ad_app_open_enabled,ad_interstitial_enabled,ad_banner_enabled,
       ad_interstitial_max_per_hour,ad_interstitial_max_per_day
FROM ad_policy
WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug')
ORDER BY app_id;
```
기대: 두 행 모두 true/true/true/true, per_hour=2, per_day=15.

### 3.3 동작 최종 확인
- 앱 완전 재시작 후 App Open/Banner/Interstitial 정상 표시(조건 충족)
- Logcat 에러 없음 확인
- 필터: `tag:AdPolicyRepo | tag:InterstitialAdManager | tag:AppOpenAdManager | tag:MainActivity`

---
## 4 완료 체크리스트
| Phase | 시나리오 | 결과 |
|-------|----------|------|
| 5.1 | RLS 정책 수정 | ⬜ PASS / ⬜ FAIL |
| 5.1 | is_active 전체 제어 | ⬜ PASS / ⬜ FAIL |
| 5.2 | App Open 제어 | ⬜ PASS / ⬜ FAIL |
| 5.2 | Interstitial 제어 | ⬜ PASS / ⬜ FAIL |
| 5.2 | Banner 제어 | ⬜ PASS / ⬜ FAIL |
| 5.3 | 빈도 제한(선택) | ⬜ PASS / ⬜ FAIL / ⬜ SKIP |
| 5.3 | 최종 검증 | ⬜ PASS / ⬜ FAIL |

---
## 5 문제 해결

- 표시 안 됨
```sql
SELECT * FROM ad_policy WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug');
```
조치: 재시작(즉시 반영) 또는 3분 대기 → 필요 시 Debug 앱 데이터 초기화
```bash
adb shell pm clear kr.sweetapps.alcoholictimer.debug
```
- 긴급 전체 차단
```sql
UPDATE ad_policy SET is_active=false
WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug');
```
반영: 재시작 즉시 또는 실행 중 최대 3분.

---
**문서 작성**: GitHub Copilot | **최종 업데이트**: 2025-11-11 | **버전**: v3.1  
**Phase 5.3 완료**: ⬜ PASS / ⬜ FAIL | **배포 준비**: ⬜ 완료 / ⬜ 미완료
