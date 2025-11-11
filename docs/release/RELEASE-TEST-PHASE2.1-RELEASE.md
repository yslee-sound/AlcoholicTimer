# 릴리즈 테스트 - Phase 2 (Update Policy)

**버전**: v4.0 | **최종 업데이트**: 2025-11-10 | **소요**: 약 15분

## 📋 목차

1. [Update Policy 개념](#1-update-policy-개념)
2. [Phase 2 테스트](#2-phase-2-테스트)
3. [체크리스트](#3-체크리스트)

## 1 Update Policy 개념

### 1 target_version_code란?

다음 Play Store versionCode와 동일하게 설정: 현재 10 → 다음 11 ⇒ target_version_code = 11

### 2 업데이트 타입

| 타입 | 버튼 구성 | 강제 전환 조건 | 재표시 설정 | 용도 |
|------|-----------|----------------|------------|------|
| 강제 (`is_force_update=true`) | 업데이트만 | 즉시(DB) | 없음 | 보안/심각 버그 |
| 선택 (`is_force_update=false`) | 업데이트+나중에 | laterCount≥max (클라) | 간격+횟수 | 일반 업데이트 |

> 선택형: 1~max회 허용 → 초과 시 강제

**💡 핵심 차이점 요약**: 강제 = 서버 즉시 강제 / 선택 = 클라이언트 조건(횟수) 도달 시 강제 전환 (DB 값 불변)

---

## 2 Phase 2 테스트

한 줄 목표/동작: 강제(뒤로가기 불가)·선택(나중에→강제 전환)·재표시/횟수 동작을 간단 시나리오로 검증.

### 시나리오 1: 강제 업데이트

#### SQL
```sql
UPDATE update_policy
SET is_active = true,
    target_version_code = 4,
    is_force_update = true,
    release_notes = '중요 업데이트',
    download_url = 'https://play.google.com/'
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```

#### 검증
- [ ] 앱 실행 → 팝업 표시 & 나중에 없음
- [ ] 뒤로가기 불가(강제)
- [ ] 업데이트 클릭 → Play Store 이동

### 시나리오 2: 선택적 업데이트 (디버그 60초 간격)

#### SQL
```sql
UPDATE update_policy
SET is_active = true,
    target_version_code = 4,
    is_force_update = false,
    release_notes = '선택적 업데이트 3회 -> 강제',
    download_url = 'https://play.google.com/',
    reshow_interval_seconds = 60,
    max_later_count = 3
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```

#### 회차별 검증 요약
| 회차 | 사용자 행동 | 기대 결과 |
|------|-------------|-----------|
| 1 | 나중에 클릭 → 60초 전/후 재실행 | 전 미표시 / 60초 후 표시 |
| 2 | 동일 + 화면 전환만 후 홈 복귀 | 시간+재실행 충족 시 재표시 (화면 전환만→미표시) |
| 3 | 나중에 클릭 후 60초 경과 재실행 | 60초 후 표시 & 강제 전환 |

> laterCount=max 도달 시 클라이언트에서만 강제 전환(DB 값 불변). 운영: reshow_interval_hours=24, max_later_count=3 권장.

### 시나리오 3: SharedPreferences 초기화

#### 방법

| 방법 | 명령 |
|------|------|
| 전체 삭제(권장) | `adb -s emulator-5554 shell pm clear com.sweetapps.pocketchord.debug` |
| prefs만 삭제 | `adb -s emulator-5554 shell run-as com.sweetapps.pocketchord.debug rm -r /data/data/com.sweetapps.pocketchord.debug/shared_prefs/` |

#### 검증
- [ ] 초기화 후 재실행 → 재표시

## 3 체크리스트

### 1 테스트 완료 여부

| 시나리오 | 결과 |
|----------|------|
| 강제 | ☐ PASS / ☐ FAIL |
| 선택 | ☐ PASS / ☐ FAIL |
| 초기화 | ☐ PASS / ☐ FAIL |
| 비활성화 | ☐ PASS / ☐ FAIL |

참고 - 비활성화 SQL:
```sql
UPDATE update_policy
SET is_active = false,
    target_version_code = 1
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```
(검증) 재실행 → 재표시 없음

---

**문서 버전**: v4.0 (UPDATE-POLICY-GUIDE 통합)  
**마지막 수정**: 2025-11-10
