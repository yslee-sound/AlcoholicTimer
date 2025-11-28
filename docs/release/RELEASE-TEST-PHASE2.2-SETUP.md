# 릴리즈 테스트 SQL 스크립트 - Phase 2.2 설정 가이드 (섹션 0~3)

- **버전**: v3.1.2  
- **최종 업데이트**: 2025-11-10 03:15:00 KST

---
## 0. 목적

**Phase 2.2**: 선택적 업데이트에서 "나중에" 클릭 후 **시간 기반 재표시** 구현 (초/분 단위 디버그, 시간 단위 운영)

| 구분 | 항목 | 설명 | 운영(릴리즈) | 우선순위 |
|------|------|------|-------------|----------|
| **DB 필드** | `reshow_interval_hours` | 재표시 간격(시간) | 24 (NOT NULL, DEFAULT) | 3 |
| | `reshow_interval_minutes` | 재표시 간격(분, 테스트) | NULL | 2 |
| | `reshow_interval_seconds` | 재표시 간격(초, 초고속 테스트) | NULL | 1 |
| | `max_later_count` | 최대 "나중에" 횟수 | 3 | - |
| **추적 데이터** | SharedPreferences | `update_dismissed_time`, `update_later_count` | 24시간 기준 | - |
| **디버그** | 설정 가능 | seconds / minutes 사용 | 60초 예시 | - |

> 운영: hours=24 단일 유지, 디버그: seconds 또는 minutes 설정 후 해제 시 자동 24시간 복귀.

---
## 🚀 빠른 테스트 시작

- 처음 설정 필요 → [3. DB 스키마 변경 SQL](#3-db-스키마-변경-sql)
- 이미 필드/초기값 완료 → Phase 2.4 시나리오 문서로 진행

---
## 1. 테스트 시나리오 요약

| 시나리오 | 목적 | 기대 결과 |
|----------|------|-----------|
| S1 스키마/초기값 | 새 필드 추가 + 기본값 확인 | 조회 시 새 필드 보임 |
| S2 첫 "나중에" | 시간/카운트 추적 시작 | 닫힘, 시간 미경과 시 재표시 없음 |
| S3 시간 경과 재표시 | 간격 경과 후 재표시 | 재표시 + count 증가 |
| S4 최대 횟수 강제 | max_later_count 도달 | "나중에" 숨김, 강제 전환 |
| S5 업데이트 후 초기화 | 새 버전 이동 | 카운트/시간 리셋 |
| S6 값 변경 반영 | 간격/횟수 변경 테스트 | 변경값 즉시 적용 |

---
## 2. Logcat 필터 & 예상 로그

필터: `tag:UpdateLater`

| 패턴 | 의미 | 시나리오 |
|------|------|----------|
| `Current later count: X / Y` | 현재 횟수 | S2~S4 |
| `Update interval elapsed (>= Xs)` | 설정 간격 경과 → 재표시 허용 | S3 |
| `Tracking: laterCount=X→Y` | "나중에" 클릭 기록 | S2, S3 |
| `Later count (3) >= max (3)` | 강제 전환 조건 충족 | S4 |
| `Update dialog dismissed for code=X` | "나중에" 클릭 완료 | S2, S3 |
| `Update dialog skipped (dismissed version: X, target: X)` | 시간 미경과로 스킵 | S2 재시작 |
| `Clearing old update tracking data (version updated)` | 업데이트 완료 초기화 | S5 |

---
## 3. DB 스키마 변경 SQL

### 3.1 설계 요약 (NOT NULL + DEFAULT 24)

- hours 필드: 항상 24 (최소 간격 안전장치)
- seconds/minutes 사용 시 우선순위: seconds > minutes > hours (상위만 적용, 조합 없음)
- 모든 interval 필드가 NULL인 상황 불가능 → 코드 단순화

**간단 예시**:
```sql
-- 60초 테스트 시작
override update_policy set reshow_interval_seconds = 60 where app_id='com.sweetapps.pocketchord.debug';
-- 해제 (24시간 복귀)
override update_policy set reshow_interval_seconds = NULL where app_id='com.sweetapps.pocketchord.debug';
```
(실 사용 시 override 제거: UPDATE로 실행)

### 3.2 필드 추가 및 제약 적용

```sql
-- 존재 여부 확인
override SELECT column_name, is_nullable, column_default
FROM information_schema.columns
WHERE table_name='update_policy' AND column_name LIKE 'reshow_interval%';

-- NOT NULL + DEFAULT 적용 (필요시)
ALTER TABLE public.update_policy
ALTER COLUMN reshow_interval_hours SET DEFAULT 24,
ALTER COLUMN reshow_interval_hours SET NOT NULL;
UPDATE update_policy SET reshow_interval_hours = 24 WHERE reshow_interval_hours IS NULL;

-- 필드 추가 (이미 있으면 무시)
ALTER TABLE public.update_policy
ADD COLUMN IF NOT EXISTS reshow_interval_hours INT DEFAULT 24 NOT NULL,
ADD COLUMN IF NOT EXISTS reshow_interval_minutes INT DEFAULT NULL,
ADD COLUMN IF NOT EXISTS reshow_interval_seconds INT DEFAULT NULL,
ADD COLUMN IF NOT EXISTS max_later_count INT DEFAULT 3 NOT NULL;

-- 값 확인 (release + debug)
SELECT app_id, target_version_code, is_force_update,
       reshow_interval_hours, reshow_interval_minutes, reshow_interval_seconds, max_later_count
FROM update_policy
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```

### 3.3 우선순위 표

| 순위 | 필드 | 단위 | 조건 | 대표 용도 |
|------|------|------|------|-----------|
| 1 | reshow_interval_seconds | 초 | seconds NOT NULL | 초고속 디버그 |
| 2 | reshow_interval_minutes | 분 | seconds NULL AND minutes NOT NULL | 빠른 디버그 |
| 3 | reshow_interval_hours | 시간 | seconds, minutes 모두 NULL | 운영 기본 24시간 |

**30초 설정**:
```sql
UPDATE update_policy SET reshow_interval_seconds = 30 WHERE app_id='com.sweetapps.pocketchord.debug';
```
**해제**:
```sql
UPDATE update_policy SET reshow_interval_seconds = NULL WHERE app_id='com.sweetapps.pocketchord.debug';
```

### 3.4 초기값 설정

릴리즈:
```sql
UPDATE update_policy
SET reshow_interval_hours = 24,
    max_later_count = 3,
    is_force_update = false
WHERE app_id = 'com.sweetapps.pocketchord';
```
디버그 (60초):
```sql
DO $$
DECLARE v_exists BOOLEAN;
BEGIN
  SELECT EXISTS(SELECT 1 FROM update_policy WHERE app_id='com.sweetapps.pocketchord.debug') INTO v_exists;
  IF v_exists THEN
    UPDATE update_policy SET is_active=true,target_version_code=10,is_force_update=false,
      reshow_interval_seconds=60,max_later_count=3,release_notes='• [DEBUG] 테스트 업데이트',download_url='https://play.google.com/'
    WHERE app_id='com.sweetapps.pocketchord.debug';
  ELSE
    INSERT INTO update_policy(app_id,is_active,target_version_code,is_force_update,reshow_interval_seconds,max_later_count,release_notes,download_url)
    VALUES('com.sweetapps.pocketchord.debug',true,10,false,60,3,'• [DEBUG] 테스트 업데이트','https://play.google.com/');
  END IF;
END $$;

SELECT app_id,target_version_code,is_force_update,is_active,
       reshow_interval_hours,reshow_interval_minutes,reshow_interval_seconds,max_later_count
FROM update_policy
WHERE app_id='com.sweetapps.pocketchord.debug';
```

### 3.5 기대 결과 (디버그)

| app_id | target_version_code | is_force_update | is_active | hours | minutes | seconds | max_later_count |
|--------|---------------------|-----------------|-----------|-------|---------|---------|-----------------|
| com.sweetapps.pocketchord.debug | 10 | false | true | 24 | NULL | 60 | 3 |

(모든 interval NULL 불가 → 최소 24시간 안전 보장)

---
## ➡️ 다음 단계

Phase 2.4 시나리오 테스트 문서로 이동하여 실제 반복 재표시/강제 전환 동작을 검증하세요.
