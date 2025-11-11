# 릴리즈 테스트 - Phase 5.1 (RLS 정책 수정 + 기본 테스트)

**버전**: v3.1 (is_active 근본 해결) | **최종 업데이트**: 2025-11-11 | **소요**: 약 15-20분

---
## 📋 목차
1. [개요](#1-개요)
2. [중요: RLS 정책 수정 (최초 1회)](#2-중요-rls-정책-수정-최초-1회)
3. [테스트 준비](#3-테스트-준비)
4. [is_active 전체 광고 제어 테스트](#4-is_active-전체-광고-제어-테스트)
5. [다음 단계](#5-다음-단계)
6. [문제 해결](#6-문제-해결)

---
## 1 개요
### 1.1 목적
RLS 수정으로 is_active 정상화 + is_active 테스트 + 3분 캐싱 이해.

**캐싱 구분**
```
ad_policy: 3분 (이 문서)
app_policy: 5분
```

### 1.2 테이블 구조
| 필드 | 기본값 | 설명 |
|------|--------|------|
| is_active | true | 전체 광고 ON/OFF |
| ad_app_open_enabled | true | App Open 광고 |
| ad_interstitial_enabled | true | Interstitial 광고 |
| ad_banner_enabled | true | Banner 광고 |
| ad_interstitial_max_per_hour | 2 | 시간당 제한 |
| ad_interstitial_max_per_day | 15 | 일일 제한 |

### 1.3 제어 방식 요약
- is_active=false → 전체 OFF, true → 개별 플래그 확인
- 정책 없음 → 기본 true (장애 대응)
- 앱 재시작 즉시 반영 / 실행 중 최대 3분 내 반영
- 3분: 대응 속도 + 배터리/네트워크 균형(1분 대비 요청 66%↓)

---
## 2 중요: RLS 정책 수정 (최초 1회)
### 2.1 필요성
과거 RLS가 is_active=false 행 숨김 → 기본값 활성화 역설. 공개 SELECT로 교체.
### 2.2 SQL
```sql
DROP POLICY IF EXISTS "ad_policy_select" ON ad_policy;
DROP POLICY IF EXISTS "ad_policy_select_all" ON ad_policy;
CREATE POLICY "ad_policy_select_all" ON ad_policy FOR SELECT USING (true);
SELECT app_id,is_active FROM ad_policy;
```
확인: 에러 없음 + false 행 조회 → 체크 완료 (1회만).

---
## 3 테스트 준비
- [ ] 섹션 2 수행 완료
- [ ] 기기/에뮬레이터 연결
- [ ] Logcat 필터: `tag:AdPolicyRepo | tag:InterstitialAdManager | tag:AppOpenAdManager | tag:MainActivity`
- 초기 상태 확인:
```sql
SELECT app_id,is_active,ad_app_open_enabled,ad_interstitial_enabled,ad_banner_enabled,
       ad_interstitial_max_per_hour,ad_interstitial_max_per_day
FROM ad_policy WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```
기대: 모든 광고 ON, per_hour=2, per_day=15.

---
## 4 is_active 전체 광고 제어 테스트
목적: is_active=false 시 전체 미표시.

1) OFF 설정
```sql
UPDATE ad_policy SET is_active=false
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```
2) 검증
- App Open: 백그라운드→포그라운드 복귀 시 미표시
- Interstitial: 패턴(예: 상세→홈 3회) + 60초 조건 충족해도 미표시
- Banner: 위치 확인 후 미표시
- 실행 중 최대 3분 내 배너 자동 반영 / 재시작 즉시 반영
- 로그 예시:
```
AdPolicyRepo: ✅ is_active: false
InterstitialAdManager: [정책] is_active = false - 모든 광고 비활성화
AppOpenAdManager: [정책] is_active = false - 모든 광고 비활성화
MainActivity: 🔄 배너 광고 정책 변경: 활성화 → 비활성화
```
3) 복구
```sql
UPDATE ad_policy SET is_active=true
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```
확인: 재시작 후 정상 표시

---
## 5 다음 단계
Phase 5.2: 개별 광고(App Open/Interstitial/Banner) 플래그 제어 및 전면 광고 표시 조건 검증.

---
## 6 문제 해결
### 6.1 is_active=false인데 광고 표시
원인: RLS 미적용. 조치:
```sql
SELECT schemaname,tablename,policyname,cmd,qual
FROM pg_policies WHERE tablename='ad_policy';
```
예상: policyname=ad_policy_select_all, qual=true → 후 재시작.

### 6.2 광고 미표시
순서:
1) RLS 수정 여부 확인
2) 설정 확인:
```sql
SELECT * FROM ad_policy WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```
3) 재시작 또는 3분 대기
4) Logcat:
```bash
adb logcat | findstr "AdPolicyRepo"
```

---
**문서 작성**: GitHub Copilot | **최종 업데이트**: 2025-11-11 | **버전**: v3.1 | **Phase 5.1 완료**: ⬜ PASS / ⬜ FAIL
