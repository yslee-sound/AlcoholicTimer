# 릴리즈 테스트 SQL 스크립트 - Phase 2.3 시나리오 테스트 (섹션 4~6)

- **버전**: v3.1.2  
- **최종 업데이트**: 2025-11-10 03:15:00 KST  
- **이전 문서**: Phase 2.2 설정 가이드 (필드/초기값 완료 가정)

---
## 테스트 전략 요약

| 빌드 | 간격 설정 | 용도 | 권장 시나리오 |
|------|-----------|------|--------------|
| 디버그 (`.debug`) | seconds=60 | 빠른 반복검증 | S1~S6 전체 |
| 릴리즈 | hours=24 | 최종 배포 전 확인 | 핵심 S2,S4 (필요시 minutes=1 임시) |
> 일상 테스트는 디버그만, 배포 직전 릴리즈 최소 검증.

---
## 사전 조건
- Phase 2.2에서 필드 추가 및 초기값 설정 완료.
- 아래 순서대로 S1 → S6 진행.

---
## S1. DB 초기 설정 검증

### S1-1 테이블 구조 확인 (공통)
```sql
SELECT column_name, data_type, column_default, is_nullable
FROM information_schema.columns
WHERE table_name='update_policy'
  AND column_name IN ('reshow_interval_hours','reshow_interval_minutes','reshow_interval_seconds','max_later_count')
ORDER BY column_name;
```
기대:

| column | default | null | note |
|--------|---------|------|------|
| max_later_count | 3 | NO | 횟수 제한 |
| reshow_interval_hours | 24 | NO | 운영 기본 |
| reshow_interval_minutes | NULL | YES | 테스트용 |
| reshow_interval_seconds | NULL | YES | 초고속 |

### S1-2 값 확인 (release+debug)
```sql
SELECT app_id,is_active,target_version_code,is_force_update,
       reshow_interval_hours,reshow_interval_minutes,reshow_interval_seconds,max_later_count
FROM update_policy
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug')
ORDER BY app_id;
```
기대:

| app_id | target | force | active | hours | minutes | seconds | max |
|--------|--------|-------|--------|-------|---------|---------|-----|
| release | 10 | false | true | 24 | NULL | NULL | 3 |
| debug | 10 | false | true | 24 | NULL | 60 | 3 |

✅ release: 24시간 / debug: 60초.

---
## S2. 첫 "나중에" 클릭 (debug)
목적: 최초 클릭 후 추적 시작 & 시간 미경과 재시작 시 미표시.

단계 요약:

| 단계 | 실행 | 기대 |
|------|------|------|
| 1 | 앱 cold start | 선택적 업데이트 팝업 표시 |
| 2 | "나중에" 클릭 | 로그: dismissed + laterCount 0→1 |
| 3 | 즉시 재실행 (<60s) | 로그: skipped (dismissed=target) / 팝업 미표시 |
| 4 | 상태 기록 | prefs에 time, count=1 저장 |

필수 로그 패턴(`tag:UpdateLater`): `dismissed`, `Tracking: laterCount=0→1`, `skipped`.

릴리즈 환경 임시 테스트(옵션): minutes=1 설정 후 동일 검증 → 복원(hours=24, minutes=NULL, seconds=NULL).

---
## S3. 시간 경과 후 재표시 (debug)
전제: laterCount=1, 60초 경과.

단계:

| 단계 | 실행 | 기대 |
|------|------|------|
| 1 | 60초 대기 | interval elapsed 로그 |
| 2 | 재실행 | 팝업 재표시 / laterCount 1 유지 |
| 3 | "나중에" 클릭 | Tracking laterCount 1→2 |
| 4 | 확인 | prefs 업데이트 (count=2, time 갱신) |

반복하여 count=3 도달 시 S4 이동.

---
## S4. 최대 횟수 도달 후 강제 전환 (debug)
전제: laterCount=2.

60초 경과 재실행 → 로그: `Current later count: 3 / 3` + `forcing update mode` → 팝업 강제(나중에 버튼 제거, 뒤로가기 차단).

검증:

| 체크 | 기대 |
|------|------|
| 나중에 버튼 | 없음 |
| 뒤로가기 | 차단 |
| 다시 재실행 | 강제 팝업 유지 |

---
## S5. 앱 업데이트 후 초기화
목적: versionCode 증가 시 추적 데이터 자동 초기화.

단계:

| 단계 | 실행 | 기대 |
|------|------|------|
| 1 | Gradle에서 versionCode↑ 후 재빌드 설치 | 새 버전 실행 |
| 2 | 재실행 | 로그: `Clearing old update tracking data` |
| 3 | Supabase target 더 높게 설정 | 새 팝업 정상 표시 |

결과: laterCount/time 리셋.

---
## S6. 정책 변경 테스트

### S6-1 간격 변경
Release:
```sql
UPDATE update_policy
SET reshow_interval_hours=48, reshow_interval_minutes=NULL, reshow_interval_seconds=NULL
WHERE app_id='com.sweetapps.pocketchord';
```
Debug:
```sql
UPDATE update_policy
SET reshow_interval_seconds=120  -- 2분
WHERE app_id='com.sweetapps.pocketchord.debug';
```
테스트: 60초 경과시 미표시 / 120초 경과 후 재표시.

### S6-2 최대 횟수 변경
Release / Debug:
```sql
UPDATE update_policy SET max_later_count=1 WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```
테스트: 첫 "나중에" 후 재표시 → 즉시 강제.

### S6-3 즉시 강제 전환
Release / Debug:
```sql
UPDATE update_policy SET is_force_update=true WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```
테스트: 재실행 즉시 강제 팝업 (나중에 없음).

---
## 필수 로그 요약 (tag:UpdateLater)

| 상황 | 핵심 로그 |
|------|-----------|
| 최초 표시 | Current later count: 0 / 3 |
| 나중에 클릭 | dismissed / Tracking laterCount X→Y |
| 시간 경과 | Update interval elapsed (...) |
| 강제 전환 | forcing update mode |
| 버전 업데이트 | Clearing old update tracking data |
| 미표시(시간 미경과) | skipped (dismissed version: X, target: X) |

---
## 다음 단계
- Phase 2.4 (고급 시나리오/예외 케이스) 문서로 이동하여 에지 조건 재검증.

---
**문서 버전**: v3.1.2  
**마지막 수정**: 2025-11-10
