# 릴리즈 테스트 - Phase 5.2 (개별 광고 제어 테스트)

**버전**: v3.1 | **최종 업데이트**: 2025-11-11 | **소요**: 약 20-25분

---
## 📋 목차
1. [개요](#1-개요)
2. [App Open 광고 제어](#2-app-open-광고-제어)
3. [Interstitial 광고 제어](#3-interstitial-광고-제어)
4. [Banner 광고 제어](#4-banner-광고-제어)
5. [긴급 광고 제어](#5-긴급-광고-제어)
6. [다음 단계](#6-다음-단계)

---
## 1 개요

목적(한 줄): 개별 광고(App Open/Interstitial/Banner)를 독립 제어하고 조건/로그를 확인.

선행 조건(요약):
- Phase 5.1 완료(RLS 수정, is_active 테스트)
- is_active = true (기본 ON)
- Debug 빌드에서 광고 표시 확인

---
## 2 App Open 광고 제어

1) OFF
```sql
UPDATE ad_policy SET ad_app_open_enabled=false
WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug');
```
2) 검증
- 앱 완전 종료 → 재실행
- 백그라운드 → 포그라운드 복귀 시 미표시
- Interstitial/Banner는 정상 표시
- Logcat 필터: `tag:AdPolicyRepo | tag:AppOpenAdManager`
  - 예: `AppOpenAdManager: [정책] 앱 오픈 광고 비활성화`
3) 복구
```sql
UPDATE ad_policy SET ad_app_open_enabled=true
WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug');
```

---
## 3 Interstitial 광고 제어

표시 조건(요약): 로드 완료 · 이전 표시 후 60초 · 화면 전환 3회(허용 패턴) · 시간당≤2 · 일일≤15 · 정책 ON.

1) OFF
```sql
UPDATE ad_policy SET ad_interstitial_enabled=false
WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug');
```
2) 검증
- 앱 재실행 후 패턴 충족(예: 상세→홈 3회, 60초 대기) 시도
- Interstitial 미표시, App Open/Banner 정상
- Logcat 필터: `tag:AdPolicyRepo | tag:InterstitialAdManager`
  - 예: `InterstitialAdManager: [정책] 전면 광고 비활성화`
3) 복구
```sql
UPDATE ad_policy SET ad_interstitial_enabled=true
WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug');
```

---
## 4 Banner 광고 제어

1) OFF
```sql
UPDATE ad_policy SET ad_banner_enabled=false
WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug');
```
2) 검증
- 앱 재실행, 배너 위치 확인 → 미표시
- Interstitial/App Open 정상
- (참고) 실행 중 최대 3분 내 정책 반영
- Logcat 예: `MainActivity: [정책] 배너 광고 비활성화`
3) 복구
```sql
UPDATE ad_policy SET ad_banner_enabled=true
WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug');
```

---
## 5 긴급 광고 제어

- is_active로 전체 차단(권장):
```sql
UPDATE ad_policy SET is_active=false
WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug');
```
- 개별 플래그로 특정 광고만 차단:
```sql
UPDATE ad_policy SET ad_banner_enabled=false
WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug');
```
반영 시간: 재시작 즉시 / 실행 중 최대 3분.

---
## 6 다음 단계

Phase 5.3: 빈도 제한(시간당/일일) 및 최종 검증, 배포 준비(내부 링크 생략).

---
**문서 작성**: GitHub Copilot  
**최종 업데이트**: 2025-11-11  
**버전**: v3.1  
**Phase 5.2 완료**: ⬜ PASS / ⬜ FAIL
