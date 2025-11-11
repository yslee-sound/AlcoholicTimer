# 릴리즈 테스트 - Phase 1 (Emergency Policy)

**버전**: v3.0 | **최종 수정**: 2025-11-10 | **소요**: 약 10분

---

## 📋 목차
1. [팝업 시스템 개요](#1-팝업-시스템-개요)
2. [Phase 1 테스트](#2-phase-1-테스트)
3. [체크리스트](#3-체크리스트)

---

## 1 팝업 시스템 개요

### 1 구조

PocketChord는 **4개 독립 테이블**로 팝업을 관리합니다:

| 테이블 | 목적 | 우선순위 | 중요 |
|--------|------|----------|----------|
| **emergency_policy** | 긴급 상황 (앱 차단, 서비스 종료) | 1순위 (최우선) | 되도록 update popup을 사용할것, 이 기능을 사용하는 일은 없어야 함 |
| **update_policy** | 앱 업데이트 (강제/선택적) | 2순위 |
| **notice_policy** | 일반 공지 (이벤트, 신규 기능) | 3순위 |
| **ad_policy** | 광고 제어 | - |

### 2 우선순위 로직
```
앱 실행 시:
1. emergency_policy 확인 → 있으면 표시하고 종료
2. 없으면 → update_policy 확인 → 있으면 표시하고 종료
3. 없으면 → notice_policy 확인 → 있으면 표시하고 종료
4. 없으면 → 팝업 없이 정상 진입
```

### 3 emergency_policy 테이블 구조

| 핵심 필드 | 특징                                     |
|----------|----------------------------------------|
| `is_active`: 긴급 상황 ON/OFF | 최우선 순위 (다른 모든 팝업보다 먼저 표시)              |
| `is_dismissible`: `true` = X 버튼 있음, `false` = X 버튼 없음 (강제) | Google Play 정책 준수 (is_dismissible로 제어) |
| `redirect_url`: Play Store 링크 또는 웹 페이지 | https://play.google.com/                         |
| `button_text`: 버튼 텍스트 | 추적 없음 (매번 표시됨)                         |

---

## 2 Phase 1 테스트

### 1 목표

- 표시 우선순위 (최우선)
- X 버튼 있음/없음 동작
- 버튼 클릭 시 redirect 동작

---

### 2 시나리오 1: 긴급공지 (X 버튼 있음)

#### SQL
```sql
UPDATE emergency_policy
SET is_active = true,
    is_dismissible = true,
    title = '긴급공지',
    content = '긴급 테스트입니다. X 버튼으로 닫을 수 있습니다.',
    button_text = '확인'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

#### 검증
- [ ] 앱 실행 → 긴급 팝업 **즉시 표시**
- [ ] 제목: "🚨 긴급공지"
- [ ] **X 버튼 있음** (우측 상단)
- [ ] X 버튼 클릭 → 팝업 닫힘
- [ ] 앱 재실행 → 긴급 팝업 **다시 표시됨** (추적 없음)

---

### 3 시나리오 2: 긴급공지 (X 버튼 없음 - 강제)

#### SQL
```sql
UPDATE emergency_policy
SET is_active = true,
    is_dismissible = false,
    title = '긴급공지',
    content = '이 앱은 더 이상 지원되지 않습니다. 새 앱을 설치하세요.',
    button_text = '새 앱 설치',
    redirect_url = 'https://play.google.com/'
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

#### 검증
- [ ] 앱 실행 → 긴급 팝업 표시
- [ ] **X 버튼 없음** ⭐
- [ ] 뒤로가기 버튼 **차단됨**
- [ ] "새 앱 설치" 버튼만 있음
- [ ] 버튼 클릭 → Play Store로 이동

---

### 4 시나리오 3: 정리 (비활성화)

#### SQL
```sql
UPDATE emergency_policy
SET is_active = false
WHERE app_id = 'com.sweetapps.pocketchord.debug';
```

#### 검증
- [ ] 앱 재실행 → 팝업 **표시 안 됨**
- [ ] Phase 1 완료! ✅

---

## 3 체크리스트

### 1 테스트 완료 여부

| 시나리오 | 결과 | 비고 |
|----------|------|------|
| X 버튼 있음 | ☐ PASS / ☐ FAIL | |
| X 버튼 없음 (강제) | ☐ PASS / ☐ FAIL | |
| 정리 (비활성화) | ☐ PASS / ☐ FAIL | |

---

**문서 버전**: v3.0
**마지막 수정**: 2025-11-10

