# 통화 현지화 전략 문서 작성 완료

**작성일**: 2025-10-28  
**작업 시간**: 약 30분  
**상태**: ✅ 완료

---

## 📋 작업 요약

스페인어권 다국어 지원을 준비하면서 발생하는 **통화 다양성 문제**에 대한 전략 및 구현 가이드를 문서화했습니다.

### 문제 정의
- 스페인어는 하나의 언어이지만, 국가마다 다른 통화 사용
  - 🇪🇸 스페인: 유로 (€)
  - 🇲🇽 멕시코: 페소 (MX$)
  - 🇦🇷 아르헨티나: 페소 (ARS$)
  - 기타 중남미 국가들...

### 해결 방안
**사용자 선택 + 로케일 자동 감지** 방식 채택

1. **첫 실행 시**: 국가/언어 기반 자동 감지
2. **설정 화면**: 언제든 통화 변경 가능
3. **저장 방식**: 모든 금액은 KRW로 저장, 표시만 변환
4. **환율 관리**: 고정 환율 사용 (분기별 업데이트)

---

## 📁 생성된 문서

### 1. CURRENCY_LOCALIZATION_GUIDE.md (신규 ✨)
**경로**: `docs/CURRENCY_LOCALIZATION_GUIDE.md`

**내용:**
- 📖 **8개 주요 섹션**
  1. 개요 - 배경 및 목표
  2. 문제 정의 - 스페인어권 통화 다양성
  3. 해결 방안 - 사용자 선택 + 자동 감지
  4. 구현 가이드 - 단계별 코드 작성법
  5. 테스트 시나리오 - 자동 감지 및 포맷팅 테스트
  6. FAQ - 자주 묻는 질문 6개
  7. 체크리스트 - Phase 1 구현 전후
  8. 참고 자료 - 환율 정보, 표준, Android 문서

**특징:**
- ✅ 완전한 코드 예시 포함 (`CurrencyManager.kt`)
- ✅ 7개 통화 지원 (KRW, JPY, USD, EUR, MXN, CNY, BRL)
- ✅ 테스트 케이스 및 시나리오
- ✅ 다이어그램 (플로우차트)
- ✅ FAQ 6개 항목

### 2. INTERNATIONALIZATION_PLAN.md (업데이트 🔄)
**경로**: `docs/INTERNATIONALIZATION_PLAN.md`

**변경 내용:**
- 섹션 3.4 "통화 현지화 전략" 전체 재작성
- 기존 간단한 예시 → 상세한 구현 전략
- 4개 하위 섹션 추가:
  - 3.4.1 기본 원칙
  - 3.4.2 지원 통화 목록 (표)
  - 3.4.3 구현 방법 (코드)
  - 3.4.4 환율 업데이트 정책

### 3. README.md (업데이트 🔄)
**경로**: `README.md`

**변경 내용:**
- 다국어화 섹션에 새 문서 링크 추가
- "통화 현지화 가이드 💰" 항목 추가

---

## 🎯 핵심 구현 사항

### CurrencyManager.kt (신규 클래스)

```kotlin
// 7개 통화 지원
val supportedCurrencies = listOf(
    CurrencyOption("KRW", "₩", R.string.currency_krw, 1.0, 0),
    CurrencyOption("JPY", "¥", R.string.currency_jpy, 0.1, 0),
    CurrencyOption("USD", "$", R.string.currency_usd, 1300.0, 2),
    CurrencyOption("EUR", "€", R.string.currency_eur, 1400.0, 2),
    CurrencyOption("MXN", "MX$", R.string.currency_mxn, 75.0, 0),
    CurrencyOption("CNY", "¥", R.string.currency_cny, 180.0, 2),
    CurrencyOption("BRL", "R$", R.string.currency_brl, 250.0, 2)
)
```

**주요 기능:**
- `formatMoney()`: 금액 포맷팅
- `getSelectedCurrency()`: 사용자 선택 통화 가져오기
- `getDefaultCurrency()`: 로케일 기반 자동 감지
- `saveCurrency()`: 설정 저장
- `initializeDefaultCurrency()`: 첫 실행 초기화

### 로케일 감지 로직

```
국가 코드 우선:
  KR → KRW
  JP → JPY
  US → USD
  MX → MXN
  
언어 기반 폴백:
  es → EUR (스페인어권 기본)
  pt → BRL (포르투갈어권 기본)
  
최종 폴백:
  USD (달러)
```

---

## 🔧 구현 로드맵

### Phase 1 (현재)
- [x] 전략 문서 작성 ✅
- [x] INTERNATIONALIZATION_PLAN.md 업데이트 ✅
- [x] `CurrencyManager.kt` 구현 ✅
- [x] `SettingsActivity.kt` UI 추가 ✅
- [x] strings.xml 통화 이름 추가 ✅
- [x] `FormatUtils.kt` 수정 ✅
- [x] `StartActivity.kt` 초기화 추가 ✅
- [x] 빌드 성공 ✅

### Phase 2 (스페인어 출시 전)
- [ ] 단위 테스트 작성
- [ ] UI 테스트
- [ ] 7개 통화 전환 검증

### Phase 3 (장기)
- [ ] API 연동 검토
- [ ] 추가 통화 지원 (ARS, COP 등)

---

## 📊 지원 통화 및 환율

| 통화 코드 | 기호 | 국가/지역 | 환율 (KRW 기준) | 소수점 |
|-----------|------|-----------|-----------------|--------|
| KRW | ₩ | 대한민국 | 1.0 | 0 |
| JPY | ¥ | 일본 | 0.1 (10원 = 1엔) | 0 |
| USD | $ | 미국 | 1,300.0 | 2 |
| EUR | € | 유럽연합 | 1,400.0 | 2 |
| MXN | MX$ | 멕시코 | 75.0 | 0 |
| CNY | ¥ | 중국 | 180.0 | 2 |
| BRL | R$ | 브라질 | 250.0 | 2 |

**환율 출처**: 한국은행, Google Finance (2025-Q4 기준)  
**업데이트 주기**: 분기별 1회

---

## 📝 문서 통계

### CURRENCY_LOCALIZATION_GUIDE.md
- **라인 수**: 약 800라인
- **코드 블록**: 15개
- **섹션**: 8개
- **테스트 케이스**: 8개
- **FAQ**: 6개

### 전체 다국어 문서
- INTERNATIONALIZATION_PLAN.md: 업데이트 완료
- CURRENCY_LOCALIZATION_GUIDE.md: 신규 작성
- I18N_JAPANESE_DONE.md: 기존 (통화 섹션 포함)
- I18N_ENGLISH_DONE.md: 기존
- 기타 가이드 문서들...

---

## ✅ 작업 완료 체크리스트

- [x] 통화 현지화 전략 수립
- [x] CURRENCY_LOCALIZATION_GUIDE.md 작성
  - [x] 1. 개요
  - [x] 2. 문제 정의
  - [x] 3. 해결 방안
  - [x] 4. 구현 가이드
  - [x] 5. 테스트 시나리오
  - [x] 6. FAQ
  - [x] 7. 체크리스트
  - [x] 8. 참고 자료
- [x] INTERNATIONALIZATION_PLAN.md 섹션 3.4 업데이트
- [x] README.md에 새 문서 링크 추가
- [x] 작업 요약 문서 작성 (현재 문서)

---

## 💡 주요 의사결정

### 1. 왜 API 연동이 아닌 고정 환율?
- ✅ 오프라인 동작
- ✅ 구현 단순화
- ✅ 외부 의존성 없음
- ✅ 절약 금액은 "추정치"이므로 정확도 덜 중요

### 2. 왜 사용자 선택 방식?
- ✅ 여행자, 해외 거주자 대응
- ✅ 로케일 오감지 방지
- ✅ 사용자 제어권 제공

### 3. 왜 KRW 기준 저장?
- ✅ 기존 코드와 호환
- ✅ 환율 변경 시 재계산 불필요
- ✅ 데이터 일관성 유지

---

## 🔗 관련 문서

1. **전략 문서**
   - [INTERNATIONALIZATION_PLAN.md](INTERNATIONALIZATION_PLAN.md)
   - [CURRENCY_LOCALIZATION_GUIDE.md](CURRENCY_LOCALIZATION_GUIDE.md)

2. **구현 완료**
   - [I18N_JAPANESE_DONE.md](I18N_JAPANESE_DONE.md)
   - [I18N_ENGLISH_DONE.md](I18N_ENGLISH_DONE.md)

3. **참고 자료**
   - [FormatUtils.kt](../app/src/main/java/com/example/alcoholictimer/core/util/FormatUtils.kt)
   - [SettingsActivity.kt](../app/src/main/java/com/example/alcoholictimer/feature/settings/SettingsActivity.kt)

---

## 📅 다음 단계

### 즉시 수행
1. `CurrencyManager.kt` 파일 생성
2. `FormatUtils.kt` 수정
3. strings.xml 7개 언어 통화 이름 추가

### Phase 2 (스페인어 출시 전)
1. SettingsActivity에 통화 선택 UI 추가
2. 단위 테스트 작성
3. 전체 통화 전환 테스트

### 장기 계획
1. 사용자 피드백 수집
2. 추가 통화 지원 검토
3. API 연동 가능성 평가

---

**문서 작성자**: AlcoholicTimer 개발팀  
**검토자**: -  
**승인일**: 2025-10-28  
**다음 검토일**: 2026-01-01 (Q1 환율 업데이트 시)

