# 릴리즈 테스트 - Phase 4 (우선순위 테스트)

**버전**: v3.0 | **최종 업데이트**: 2025-11-10 | **소요**: 약 10분

---
## 📋 목차
1. [우선순위 개념](#1-우선순위-개념)
2. [Phase 4 테스트](#2-phase-4-테스트)
3. [체크리스트](#3-체크리스트)

---
## 1 우선순위 개념

### 1 팝업 우선순위 규칙
```
1. emergency_policy
2. update_policy
3. notice_policy
4. 모두 없으면 정상 진입
```

### 2 우선순위 동작
| 상황 | 표시 팝업 | 설명 |
|------|----------|------|
| Emergency + Update ON | Emergency | 최우선 |
| Update + Notice ON | Update | Update가 우선 |
| 모두 OFF | 없음 | 정상 진입 |

핵심: 한 번에 1개 / 높은 우선순위 존재 시 낮은 팝업 미표시

---
## 2 Phase 4 테스트

한 줄 목표: Emergency > Update > Notice 우선순위 및 단일 표시 원칙 검증.

### 시나리오 1: Emergency가 Update보다 우선
```sql
UPDATE emergency_policy 
SET is_active = true, content = '🚨 긴급 우선순위 테스트', button_text = '확인'
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');

UPDATE update_policy 
SET is_active = true, target_version_code = 100, is_force_update = true, release_notes = '• 업데이트 테스트'
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```
검증: Emergency만 표시, Update 미표시, 로그 "EMERGENCY"

### 시나리오 2: Update가 Notice보다 우선
```sql
UPDATE emergency_policy SET is_active = false 
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');

UPDATE update_policy 
SET is_active = true, target_version_code = 100, is_force_update = false, release_notes = '• 선택적 업데이트'
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');

UPDATE notice_policy 
SET is_active = true, title = '공지 테스트', content = '공지 내용', notice_version = 251109 
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```
검증: Update만 표시, Notice 미표시, 로그 "UPDATE"

### 시나리오 3: 모두 비활성화
```sql
UPDATE emergency_policy SET is_active = false 
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');

UPDATE update_policy SET is_active = false, target_version_code = 1
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');

UPDATE notice_policy SET is_active = false 
WHERE app_id IN ('com.sweetapps.pocketchord','com.sweetapps.pocketchord.debug');
```
검증: 팝업 없음, 홈 화면 즉시, Phase 4 완료

---
## 3 체크리스트

| 시나리오 | 결과 |
|----------|------|
| Emergency > Update | ☐ PASS / ☐ FAIL |
| Update > Notice | ☐ PASS / ☐ FAIL |
| 모두 비활성화 | ☐ PASS / ☐ FAIL |

우선순위 확인:
- [ ] Emergency 활성화 시 다른 팝업 무시
- [ ] Update 활성화 시 Notice 무시
- [ ] 단일 팝업만 표시
- [ ] 모두 OFF 시 팝업 없음

### 발견된 이슈
```
1. _______________________________
2. _______________________________
```

---
**문서 버전**: v2.0.0 (구조 개선) | **마지막 수정**: 2025-11-10
