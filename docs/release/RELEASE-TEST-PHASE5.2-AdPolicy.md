<style>
body {
    color: black;
}
</style>

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

> 변경 요지: App Open 광고는 더 이상 일반적인 포그라운드 복귀마다 자동으로 노출되지 않습니다. App Open은 "앱 시작(콜드 스타트)" 시점에만 트리거되도록 정책이 변경되었고, Supabase의 `ad_app_open_enabled` 및 빈도 제한 필드(`app_open_max_per_hour`, `app_open_max_per_day`)로 제어됩니다. 따라서 테스트/검증 시에는 "앱 완전 종료 → 재실행"(cold start)을 사용하여 App Open 동작을 확인해야 합니다.

1) OFF (Supabase)
```sql
UPDATE ad_policy SET ad_app_open_enabled=false
WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug');
```

2) 검증 (중요: 콜드 스타트 기준)
- "앱 완전 종료 → 재실행"(앱 프로세스 종료 후 런처에서 다시 시작) 시 App Open 노출 여부 확인.
- 일반적인 백그라운드 → 포그라운드 복귀에서는 App Open이 표시되지 않아야 함(정책 변경 의도).
- Interstitial/Other ads와의 충돌 방지:
  - 앱 시작 시 App Open이 이미 표시되는 경우에는 동일 시점에 Interstitial을 동시에 트리거하지 않도록 앱 로직/광고 매니저가 우선순위를 관리해야 합니다.
  - Interstitial이 먼저 표시되거나 다른 광고가 활성화된 상태에서는 App Open 노출을 억제하도록 클라이언트가 추가 검사를 수행합니다. (로그에서 확인 가능)
- Logcat 필터: `tag:AdPolicyRepo | tag:AppOpenAdManager | tag:InterstitialAdManager`
  - 예: `AppOpenAdManager: [정책] 앱 오픈 광고 비활성화` 또는 `AppOpenAdManager: skipping show() due to interstitial showing` (클라이언트 로그 형식)

3) 복구
```sql
UPDATE ad_policy SET ad_app_open_enabled=true
WHERE app_id IN ('kr.sweetapps.alcoholictimer','kr.sweetapps.alcoholictimer.debug');
```

4) App Open 빈도 제한 테스트 (Supabase 제어, 콜드 스타트 기준)
 - Supabase에서 `app_open_max_per_hour` / `app_open_max_per_day` 값을 조정하여 클라이언트의 로컬 제한 동작을 확인합니다.
 - 주의: App Open 정책은 콜드 스타트 시에만 적용되므로, 테스트는 앱을 완전히 종료한 뒤 재시작(cold start)을 반복해서 수행해야 합니다.
 - 예: `app_open_max_per_hour=1`, `app_open_max_per_day=3`로 설정하고 앱을 재실행하여 AppOpen을 여러 번(콜드 스타트) 트리거한 뒤 로그에서 제한 초과 메시지(`showIfAvailable abort: AppOpen limit reached by policy`)를 확인합니다.
 - 로그 위치: `AppOpenAdManager`, `AdController`.
 - 참고: 현재 구현은 클라이언트 로컬 기록(SharedPreferences) 기반입니다. 여러 기기에서의 전역 집계는 서버 API 필요.

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
