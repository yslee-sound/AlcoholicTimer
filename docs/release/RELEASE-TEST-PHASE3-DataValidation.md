# 릴리즈 테스트 - Phase 3 (Notice Policy)

**버전**: v3.0 | **최종 업데이트**: 2025-11-10 | **소요**: 약 10-15분

---

## 📋 목차

1. [Notice Policy 개념](#1-notice-policy-개념)
2. [Phase 3 테스트](#2-phase-3-테스트)
3. [버전 관리 가이드](#3-버전-관리-가이드)
4. [체크리스트](#4-체크리스트)

---

## 1 Notice Policy 개념

### 1 테이블 구조

핵심 필드:
- `is_active`: 공지 ON/OFF
- `notice_version`: 버전 번호(추적)
- `title`, `content`: 공지 내용

### 2 버전 추적 메커니즘

한 줄 요약: 보여준 `notice_version`을 저장 → 동일 버전 미표시, 증가 시 재표시.

---

## 2 Phase 3 테스트

한 줄 목표/동작: 공지 표시 · 동일 버전 미표시 · 버전 증가 시 재표시를 간단 시나리오로 검증.

### 1 공지 활성화

```sql
UPDATE notice_policy
SET is_active = true,
    title = '서비스 안내',
    content = '중요 공지입니다. 앱을 최신 버전으로 유지해 주세요.',
    notice_version = 251109  -- YYMMDD 형식 권장
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```
검증: 앱 실행 → 공지 팝업 표시, X 버튼으로 닫힘, 재실행 → 미표시(추적됨)

### 2 내용 수정(버전 유지)

```sql
UPDATE notice_policy 
SET content = '수정된 내용입니다. 오타를 바로잡았습니다.'
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
-- notice_version은 그대로 유지
```
검증: 재실행 → 미표시(이미 본 버전)

### 3 새 공지(버전 증가)

```sql
UPDATE notice_policy
SET title = '🎉 11월 이벤트',
    content = '11월 특별 이벤트가 시작되었습니다! 참여하세요.',
    notice_version = notice_version + 1
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```
검증: 재실행 → 다시 표시(새 버전), 이후 재실행 → 미표시(추적됨)

---

## 3 버전 관리 가이드

- 권장 버전 형식: YYMMDD (예: 251109)
- 자주 변경 시: `notice_version = notice_version + 1` 안전
- 주의: 자리수 혼합 금지(6자리/7자리 섞지 말 것)

예시:
```sql
-- 날짜 기반
UPDATE notice_policy SET notice_version = 251110 WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
-- 자동 증가
UPDATE notice_policy SET notice_version = notice_version + 1 WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```

---

## 4 체크리스트

| 시나리오 | 결과 |
|----------|------|
| 공지 활성화 | ☐ PASS / ☐ FAIL |
| 내용 수정(버전 유지) | ☐ PASS / ☐ FAIL |
| 새 공지(버전 증가) | ☐ PASS / ☐ FAIL |
| 비활성화 | ☐ PASS / ☐ FAIL |

참고 - 비활성화 SQL:
```sql
UPDATE notice_policy SET is_active = false WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```
(검증) 재실행 → 팝업 미표시

### 발견된 이슈
```
1. _______________________________
2. _______________________________
```

---

**문서 버전**: v3.0  
**마지막 수정**: 2025-11-10
