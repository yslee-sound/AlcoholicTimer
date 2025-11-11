# 릴리즈 테스트 SQL 스크립트 - Phase 2.4 고급 테스트 (Edge & Recovery)

**버전**: v3.1.2 | **최종 업데이트**: 2025-11-10 03:15:00 KST

---
## 0. 전제
- Phase 2.2 (필드/초기값) & Phase 2.3 (기본 시나리오 S1~S6) 완료 가정
- 목적: 극단값 / 제약 / 즉시강제 / 시간 조작 / 초기화 패턴 검증

---
## 1. 에지 케이스 요약

| 코드 | 설정(예) | 기대 동작 | 비고 |
|------|----------|-----------|------|
| E1 음수 간격 | hours = -1 | CHECK 제약으로 SQL 실패 | 음수 차단 |
| E2 0초 간격 | seconds = 0 | "나중에" 후 즉시 재표시 | 실사용 지양 |
| E3 과대 간격 | hours = 999 | 999시간 동안 스킵 (로그 반복) | 오버플로우 없음 |
| E4 0회 허용 | max_later_count = 0 | 첫 표시부터 강제 (나중에 없음) | laterCount>=max 즉시 강제 |
| E5 음수 횟수 | max_later_count = -1 | CHECK 제약 실패 | 음수 차단 |
| E6 타임존 변경 | TZ 변경 | UTC 기반 → 영향 없음 | 정상 스킵 유지 |
| E7 시계 뒤로 | 시스템 시간을 과거로 | elapsed < interval → 스킵 | 음수 경과 처리 정상 |

---
## 2. 에지 테스트 상세

### E1/E5 음수 값 차단
```sql
-- 실패해야 정상
UPDATE update_policy SET reshow_interval_hours = -1 WHERE app_id='com.sweetapps.pocketchord.debug';
UPDATE update_policy SET max_later_count = -1 WHERE app_id='com.sweetapps.pocketchord';
```
기대: "violates check constraint" 에러, 값 미변경.

### E2 0초 간격 (즉시 재표시)
```sql
UPDATE update_policy SET reshow_interval_seconds=0 WHERE app_id='com.sweetapps.pocketchord.debug';
```
순서: 표시 → "나중에" → 앱 재시작 → 즉시 재표시.
복구:
```sql
UPDATE update_policy SET reshow_interval_seconds=60 WHERE app_id='com.sweetapps.pocketchord.debug';
```

### E3 과대(999h) 간격
```sql
UPDATE update_policy SET reshow_interval_hours=999, reshow_interval_seconds=NULL, reshow_interval_minutes=NULL WHERE app_id='com.sweetapps.pocketchord.debug';
```
확인: 반복 재시작마다 `skipped` 로그, 크래시/오버플로우 없음. 복구:
```sql
UPDATE update_policy SET reshow_interval_hours=24, reshow_interval_seconds=60 WHERE app_id='com.sweetapps.pocketchord.debug';
```

### E4 0회 허용 → 즉시 강제
```sql
UPDATE update_policy SET max_later_count=0 WHERE app_id='com.sweetapps.pocketchord.debug';
```
SharedPreferences 초기화 후 첫 실행: `later count (0) >= max (0)` 로그, "나중에" 없음, 뒤로가기 불가.
복구:
```sql
UPDATE update_policy SET max_later_count=3 WHERE app_id='com.sweetapps.pocketchord.debug';
```

### E6 타임존 변경
절차: 나중에 클릭 → 디바이스 TZ 변경(UTC+9 → UTC-5) → 재시작.
기대: 동일 스킵 로그 (절대시간 기반).

### E7 시계 뒤로 조작
나중에 클릭 후 시스템 시간을 과거로 이동 → 재시작.
기대: elapsed 음수 → 스킵 (`skipped (dismissed version: X, target: X)`).

---
## 3. CHECK 제약 조건 관리

제약 생성(음수 차단):
```sql
ALTER TABLE update_policy ADD CONSTRAINT check_reshow_interval_positive
CHECK (
  (reshow_interval_hours IS NULL OR reshow_interval_hours >= 0) AND
  (reshow_interval_minutes IS NULL OR reshow_interval_minutes >= 0) AND
  (reshow_interval_seconds IS NULL OR reshow_interval_seconds >= 0) AND
  (max_later_count >= 0)
);
```
존재 확인:
```sql
SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint
WHERE conrelid='update_policy'::regclass AND conname='check_reshow_interval_positive';
```
삭제(테스트 목적):
```sql
ALTER TABLE update_policy DROP CONSTRAINT check_reshow_interval_positive; -- 복구 후 재생성 권장
```

---
## 4. 초기화 / 복구 SQL

### 4.1 디버그 기본값 복구 (60초)
```sql
UPDATE update_policy
SET target_version_code=10,is_force_update=false,
    reshow_interval_hours=24, reshow_interval_minutes=NULL, reshow_interval_seconds=60,
    max_later_count=3,is_active=true
WHERE app_id='com.sweetapps.pocketchord.debug';
```

### 4.2 릴리즈 기본값 복구 (24시간)
```sql
UPDATE update_policy
SET target_version_code=10,is_force_update=false,
    reshow_interval_hours=24, reshow_interval_minutes=NULL, reshow_interval_seconds=NULL,
    max_later_count=3,is_active=true
WHERE app_id='com.sweetapps.pocketchord';
```

### 4.3 두 버전 동시 복구 & 확인
```sql
-- 복구
UPDATE update_policy
SET target_version_code=10,is_force_update=false,
    reshow_interval_hours=24, reshow_interval_minutes=NULL, reshow_interval_seconds=60,
    max_later_count=3,is_active=true
WHERE app_id='com.sweetapps.pocketchord.debug';

UPDATE update_policy
SET target_version_code=10,is_force_update=false,
    reshow_interval_hours=24, reshow_interval_minutes=NULL, reshow_interval_seconds=NULL,
    max_later_count=3,is_active=true
WHERE app_id='com.sweetapps.pocketchord';

-- 확인
SELECT app_id,target_version_code,is_force_update,is_active,
       reshow_interval_hours,reshow_interval_minutes,reshow_interval_seconds,max_later_count
FROM update_policy WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug') ORDER BY app_id;
```
기대:

| app_id | hours | seconds | max |
|--------|-------|---------|-----|
| release | 24 | NULL | 3 |
| debug | 24 | 60 | 3 |

### 4.4 SharedPreferences 초기화
```cmd
adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug rm shared_prefs/update_preferences.xml
```
효과: `update_dismissed_time`, `update_later_count`, `dismissedVersionCode` 리셋 → 첫 상태 재표시.

---
## 5. 문제 해결 Quick SQL

| 상황 | SQL | 후속 |
|------|-----|------|
| 정책 비활성 | `UPDATE update_policy SET is_active=true WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');` | 재시작 |
| 팝업 미표시 | `UPDATE update_policy SET target_version_code=100 WHERE app_id='com.sweetapps.pocketchord.debug';` | 재시작 |
| 강제모드 해제 | `UPDATE update_policy SET is_force_update=false,max_later_count=3 WHERE app_id='com.sweetapps.pocketchord.debug';` | prefs 초기화 |

---
## 6. 완료 조건
- 음수 입력 차단 에러 확인
- 0초 즉시 재표시 동작 확인 후 복구
- 과대(999h) 스킵 반복 정상
- 0회 강제 모드 즉시 전환 확인
- 타임존/시계 조작 영향 없음 확인
- 초기화/복구 SQL 및 prefs 삭제로 재테스트 가능

---
**문서 버전**: v3.1.2 | **마지막 수정**: 2025-11-10
