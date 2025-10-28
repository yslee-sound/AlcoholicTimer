# AlcoholicTimer 다국어 출시 기획안

최종 수정: 2025-10-28

## 1. 목적 및 비전

### 1.1 목적
- AlcoholicTimer를 글로벌 사용자에게 제공하여 더 많은 사람들의 금주/절주 습관 형성 지원
- 앱의 핵심 가치(직관적 타이머, 진행률 가시화)를 다양한 언어권에서 동일하게 전달
- Play Store 글로벌 마켓 진출을 통한 사용자 기반 확대

### 1.2 비전
- 단계적 언어 지원 확대를 통한 안정적인 다국어 서비스 제공
- 각 문화권에 맞는 현지화(Localization)로 사용자 경험 최적화
- 커뮤니티 기반 번역 시스템 구축 (장기)

---

## 2. 시장 분석 및 우선순위 언어 선정

### 2.1 우선순위 언어 (Phase 1-3)

#### **Phase 1: 핵심 언어** (1st Quarter)
1. **영어 (English - US/UK)**
   - 이유: 글로벌 공통어, Play Store 기본 언어
   - 시장 크기: 전 세계 13억+ 사용자
   - 우선순위: ★★★★★

2. **일본어 (Japanese)**
   - 이유: 건강 앱 수요 높음, 금주 문화 관심도 상승
   - 시장 크기: 1억 2천만 인구, 높은 스마트폰 보급률
   - 우선순위: ★★★★☆

#### **Phase 2: 확장 언어** (2nd Quarter)
3. **중국어 간체 (Simplified Chinese)**
   - 이유: 최대 시장, 건강 관리 앱 성장세
   - 시장 크기: 14억+ 인구
   - 우선순위: ★★★★☆

4. **스페인어 (Spanish)**
   - 이유: 스페인어권 광범위 (유럽+중남미)
   - 시장 크기: 4억 8천만+ 사용자
   - 우선순위: ★★★☆☆

#### **Phase 3: 보완 언어** (3rd Quarter)
5. **포르투갈어 (Portuguese - Brazil)**
   - 브라질 시장 중심, 건강 앱 수요
   
6. **독일어 (German)**
   - 유럽 주요 시장, 높은 구매력

7. **프랑스어 (French)**
   - 유럽/아프리카 프랑스어권

### 2.2 언어별 특수 고려사항

| 언어 | RTL 여부 | 문자 길이 특성 | 폰트 요구사항 | 날짜/숫자 형식 |
|------|----------|----------------|---------------|----------------|
| 한국어 | LTR | 중간 | 기본 | yyyy-MM-dd |
| 영어 | LTR | 긴 편 (한국어 대비 1.3~1.5배) | 기본 | MM/dd/yyyy |
| 일본어 | LTR | 짧은 편 | Noto Sans JP 권장 | yyyy年MM月dd日 |
| 중국어 | LTR | 짧은 편 | Noto Sans SC 권장 | yyyy年MM月dd日 |
| 스페인어 | LTR | 긴 편 | 기본 | dd/MM/yyyy |
| 포르투갈어 | LTR | 긴 편 | 기본 | dd/MM/yyyy |
| 독일어 | LTR | 매우 긴 편 (합성어) | 기본 | dd.MM.yyyy |
| 프랑스어 | LTR | 긴 편 | 기본 | dd/MM/yyyy |

---

## 3. 기술 아키텍처 전략

### 3.1 Android 다국어 지원 구조
```
app/src/main/res/
├── values/              # 기본 (한국어)
│   └── strings.xml
├── values-en/           # 영어
│   └── strings.xml
├── values-ja/           # 일본어
│   └── strings.xml
├── values-zh-rCN/       # 중국어 간체
│   └── strings.xml
├── values-es/           # 스페인어
│   └── strings.xml
├── values-pt-rBR/       # 포르투갈어 (브라질)
│   └── strings.xml
├── values-de/           # 독일어
│   └── strings.xml
└── values-fr/           # 프랑스어
    └── strings.xml
```

### 3.2 문자열 리소스 관리 원칙
1. **하드코딩 제거**: 모든 UI 텍스트를 `strings.xml`로 이동
2. **Plurals 지원**: 단수/복수 형태가 다른 언어 대응
   ```xml
   <plurals name="days_count">
       <item quantity="one">%d day</item>
       <item quantity="other">%d days</item>
   </plurals>
   ```
3. **변수 순서 유연성**: 문장 구조가 다른 언어 대응
   ```xml
   <!-- 한국어 -->
   <string name="progress_format">%1$d일 중 %2$d일 진행</string>
   <!-- 영어 -->
   <string name="progress_format">%2$d of %1$d days</string>
   ```
4. **컨텍스트 주석**: 번역자를 위한 설명 추가
   ```xml
   <!-- 금주를 종료할 때 표시되는 확인 메시지. 사용자의 도전을 격려하는 톤 -->
   <string name="quit_confirm_subtitle">지금까지 잘 해오셨는데&#8230;</string>
   ```

### 3.3 날짜/시간 포맷 현지화
```kotlin
// 기존: 하드코딩된 포맷
val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

// 개선: 로케일 기반 포맷 유틸리티
object LocaleDateFormat {
    fun getDateFormat(context: Context): SimpleDateFormat {
        val pattern = when (Locale.getDefault().language) {
            "ko" -> "yyyy년 M월 d일"
            "ja", "zh" -> "yyyy年M月d日"
            "en" -> "MMM d, yyyy"
            "de" -> "d. MMM yyyy"
            else -> "MMM d, yyyy"
        }
        return SimpleDateFormat(pattern, Locale.getDefault())
    }
    
    fun getTimeFormat(context: Context): SimpleDateFormat {
        return if (DateFormat.is24HourFormat(context)) {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("h:mm a", Locale.getDefault())
        }
    }
}
```

### 3.4 통화 현지화 전략

#### 3.4.1 기본 원칙
- **사용자 선택 기반**: 설정 화면에서 통화 선택 가능
- **로케일 기반 자동 감지**: 첫 실행 시 국가/언어에 따라 기본 통화 설정
- **KRW 기준 저장**: 모든 금액은 내부적으로 원화(KRW)로 저장, 표시만 변환
- **고정 환율 사용**: 분기별 업데이트 (API 연동은 장기 과제)

#### 3.4.2 지원 통화 목록 (Phase 1-3)

| 통화 코드 | 기호 | 국가/지역 | KRW 환율 (2025-Q4) | 적용 언어 |
|-----------|------|-----------|-------------------|-----------|
| KRW | ₩ | 대한민국 | 1.0 | 한국어 |
| JPY | ¥ | 일본 | 0.1 (10원 = 1엔) | 일본어 |
| USD | $ | 미국, 국제 | 1,300.0 | 영어 (기본) |
| EUR | € | 유럽연합 | 1,400.0 | 독일어, 프랑스어 |
| MXN | MX$ | 멕시코 | 75.0 | 스페인어 (멕시코) |
| CNY | ¥ | 중국 | 180.0 | 중국어 |
| BRL | R$ | 브라질 | 250.0 | 포르투갈어 |

#### 3.4.3 구현 방법

```kotlin
// CurrencyManager.kt
data class CurrencyOption(
    val code: String,
    val symbol: String,
    val nameResId: Int,
    val rate: Double,  // KRW 기준
    val decimalPlaces: Int = 0
)

object CurrencyManager {
    val supportedCurrencies = listOf(
        CurrencyOption("KRW", "₩", R.string.currency_krw, 1.0, 0),
        CurrencyOption("JPY", "¥", R.string.currency_jpy, 0.1, 0),
        CurrencyOption("USD", "$", R.string.currency_usd, 1300.0, 2),
        CurrencyOption("EUR", "€", R.string.currency_eur, 1400.0, 2),
        CurrencyOption("MXN", "MX$", R.string.currency_mxn, 75.0, 0),
        CurrencyOption("CNY", "¥", R.string.currency_cny, 180.0, 2),
        CurrencyOption("BRL", "R$", R.string.currency_brl, 250.0, 2)
    )

    fun formatMoney(amountInKRW: Double, context: Context): String {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val currencyCode = prefs.getString("currency", getDefaultCurrency(context)) ?: "KRW"
        val currency = supportedCurrencies.find { it.code == currencyCode } 
            ?: supportedCurrencies.first()
        
        val converted = amountInKRW / currency.rate
        
        return when {
            currency.code == "KRW" -> 
                String.format(Locale.getDefault(), "%,.0f%s", converted, currency.symbol)
            currency.decimalPlaces == 0 -> 
                String.format(Locale.getDefault(), "%s%,.0f", currency.symbol, converted)
            else -> 
                String.format(Locale.getDefault(), "%s%,.${currency.decimalPlaces}f", currency.symbol, converted)
        }
    }
    
    private fun getDefaultCurrency(context: Context): String {
        val locale = Locale.getDefault()
        return when (locale.country) {
            "KR" -> "KRW"
            "JP" -> "JPY"
            "US" -> "USD"
            "CN" -> "CNY"
            "MX" -> "MXN"
            "BR" -> "BRL"
            else -> when (locale.language) {
                "es" -> "EUR"  // 스페인어권 기본
                "de", "fr" -> "EUR"
                else -> "USD"
            }
        }
    }
}
```

#### 3.4.4 환율 업데이트 정책
- **업데이트 주기**: 분기별 1회 (3개월)
- **반영 시점**: 앱 버전 업데이트 시
- **고지 방법**: Release Notes에 "환율 업데이트 (2025-Q4 기준)" 명시
- **장기 계획**: Phase 4+ API 연동 검토 (ExchangeRate-API.com 등)

### 3.5 레이아웃 유연성
- **ConstraintLayout 활용**: 텍스트 길이 변화에 대응
- **ellipsize/maxLines**: 긴 번역문 처리
- **최소 터치 영역 유지**: 48dp 기준 준수 (다국어 적용 후에도)
- **테스트 전용 긴 문자열**: `values-en-rXC` (Pseudolocalization) 활용

---

## 4. 번역 워크플로우

### 4.1 번역 방식 비교

| 방식 | 장점 | 단점 | 비용 | 품질 | 추천 Phase |
|------|------|------|------|------|------------|
| 전문 번역가 | 최고 품질, 문화적 맥락 이해 | 높은 비용, 긴 소요 시간 | $$$ | ★★★★★ | Phase 1 핵심 언어 |
| 크라우드소싱 (Crowdin 등) | 커뮤니티 참여, 빠른 피드백 | 품질 편차, 관리 필요 | $$ | ★★★☆☆ | Phase 2-3 |
| 기계 번역 (초벌) + 검수 | 빠른 초안, 비용 절감 | 문맥 오류 가능 | $ | ★★☆☆☆ | 초안 작성 |
| 자체 번역 (다국어 가능자) | 비용 무료, 앱 이해도 높음 | 전문성 부족 | 無 | ★★★☆☆ | 테스트/검증 |

### 4.2 권장 워크플로우

#### **Phase 1: 영어/일본어**
1. **문자열 추출 및 정리** (1주)
   - 모든 하드코딩 문자열을 `strings.xml`로 이동
   - 컨텍스트 주석 추가
   - 변수 포맷 정규화

2. **기계 번역 초안 생성** (1일)
   - Google Translate API / DeepL 활용
   - 검토용 스프레드시트 생성

3. **전문 검수** (1주)
   - 네이티브 스피커 검수 (Upwork, Fiverr, 로컬 번역사)
   - 앱 맥락 이해를 위한 스크린샷/설명 제공

4. **앱 내 적용 및 테스트** (3일)
   - 레이아웃 깨짐 확인
   - 실제 기기 테스트 (언어 전환)

#### **Phase 2-3: 확장 언어**
1. **Crowdin/Weblate 플랫폼 설정** (1주)
   - GitHub 연동
   - 번역 메모리 구축
   - 커뮤니티 번역자 모집

2. **단계적 공개**
   - 베타 테스트 그룹에서 피드백 수집
   - 수정 후 정식 출시

### 4.3 번역 품질 관리
- **용어집(Glossary) 구축**
  ```
  금주 (Sobriety) -> Abstinence (en), 禁酒 (ja), 戒酒 (zh)
  레벨 (Level) -> Level (en), レベル (ja), 级别 (zh)
  목표 달성률 -> Goal Achievement Rate (en), 目標達成率 (ja)
  ```
- **스타일 가이드**
  - 존댓말/반말 일관성
  - 긍정적/격려 톤 유지
  - 간결함 추구 (모바일 화면 고려)

---

## 5. Play Store 메타데이터 다국어화

### 5.1 번역 대상 항목
1. **앱 이름** (30자 제한)
   - 한국어: AlcoholicTimer
   - 영어: Alcoholic Timer
   - 일본어: アルコールタイマー (또는 영문 유지)
   - *주의: 브랜딩 일관성 vs 현지화 검색 최적화 고려*

2. **짧은 설명** (80자)
   - 한국어: 금주 기록과 성공률을 간단하게 추적하세요.
   - 영어: Track your sobriety journey with simple records and success rates.
   - 일본어: 禁酒記録と成功率を簡単に追跡

3. **전체 설명** (4000자)
   - 주요 기능 나열
   - 개인정보 처리방침 요약
   - 향후 로드맵 (언어별 조정)

4. **스크린샷 캡션**
   - 각 스크린샷에 해당 언어 UI 적용
   - 캡션 텍스트 번역

5. **최근 변경사항** (Release Notes)
   - 버전별 업데이트 내용
   - 다국어 지원 안내

### 5.2 스크린샷 현지화 전략
- **옵션 1: 언어별 별도 스크린샷** (권장)
  - 각 언어로 앱 UI를 캡처
  - 레이아웃 차이 확인 가능
  - 작업량: 언어 수 × 스크린샷 수

- **옵션 2: 범용 스크린샷 + 캡션**
  - UI는 영어로 통일, 캡션만 번역
  - 작업량 적음, 품질 다소 낮음

---

## 6. 단계별 실행 계획 (Timeline)

### **Phase 1: 기반 구축 및 영어 출시** (Week 1-6)

#### Week 1-2: 준비 및 코드 리팩터링
- [ ] **문자열 감사 (String Audit)**
  - 모든 Kotlin 파일에서 하드코딩된 문자열 검색
  - `R.string` 변환 완료
  - 누락된 문자열 리소스 추가
  
- [ ] **Plurals/Format 정리**
  - 단수/복수 형태 분리 (days, records 등)
  - String format 인자 순서 유연화 (`%1$d`, `%2$s` 활용)
  
- [ ] **날짜/숫자 포맷 유틸리티 구현**
  - `LocaleDateFormat` 클래스 생성
  - `LocaleCurrencyFormat` 클래스 생성
  - 기존 하드코딩 포맷 대체

- [ ] **레이아웃 유연성 검토**
  - 긴 텍스트 대응 (ellipsize, wrap_content)
  - ConstraintLayout 제약 조건 확인

#### Week 3-4: 영어 번역 및 적용
- [ ] **strings.xml 영어 번역**
  - `values-en/strings.xml` 생성
  - 기계 번역 초안 생성 (DeepL)
  - 네이티브 검수 (외주 또는 커뮤니티)

- [ ] **Play Store 메타데이터 영어 작성**
  - 짧은 설명, 전체 설명
  - 키워드 최적화 (sobriety tracker, quit drinking 등)

- [ ] **영어 UI 테스트**
  - 에뮬레이터 언어 설정 변경
  - 모든 화면 레이아웃 검증
  - 스크린샷 캡처 (최소 5장)

#### Week 5-6: 일본어 준비 및 QA
- [x] **일본어 번역** ✅ 완료 (2025-10-28)
  - `values-ja/strings.xml` 생성
  - 전문 번역 완료 (205개 문자열)

- [x] **통화 포맷 구현** ✅ 완료
  - 환율: 1엔 = 10원 적용
  - FormatUtils.kt 업데이트

- [x] **날짜/시간 포맷 구현** ✅ 완료
  - 일본어 형식: yyyy年MM月dd日, H:mm
  - AddRecordActivity.kt, DetailActivity.kt 업데이트

- [ ] **전체 QA**
  - 한국어/영어/일본어 3개 언어 전환 테스트
  - 날짜/숫자 형식 확인
  - 릴리스 노트 작성

- [ ] **Play Console 업로드**
  - 영어/일본어 메타데이터 등록
  - 버전 1.2.0 출시 (다국어 지원 명시)

---

### **Phase 2: 확장 언어 (중국어/스페인어)** (Week 7-10)

#### Week 7-8: 중국어 간체
- [x] 번역 완료 (`values-zh-rCN`) ✅ 완료 (2025-10-28)
  - 전체 227개 문자열 번역 완료
  - 통화: CNY (¥) 환율 180.0 적용
  - 날짜 형식: yyyy年MM月dd日
- [ ] Noto Sans SC 폰트 검토
- [ ] Play Store 메타데이터 (중국어)
- [ ] 스크린샷 4개 이상

#### Week 9-10: 스페인어
- [x] 번역 완료 (`values-es`) ✅ 완료 (2025-10-28)
  - 전체 227개 문자열 번역 완료
  - 통화: EUR (€) 환율 1400.0 기본 적용
  - 날짜 형식: dd/MM/yyyy
- [ ] 날짜 형식 확인 테스트
- [ ] Play Store 메타데이터 (스페인어)
- [ ] 버전 1.3.0 출시

---

### **Phase 3: 보완 언어** (Week 11-14)

#### Week 11-12: 포르투갈어/독일어
- [ ] 번역 완료 (`values-pt-rBR`, `values-de`)
- [ ] 독일어 긴 합성어 레이아웃 테스트
- [ ] Play Store 메타데이터

#### Week 13-14: 프랑스어 및 최종 검증
- [ ] 번역 완료 (`values-fr`)
- [ ] 전체 언어 통합 테스트
- [ ] 버전 1.4.0 출시 (7개 언어 지원)

---

## 7. 비용 예산 (Phase 1-3)

### 7.1 번역 비용
| 항목 | 단가 | 수량 | 총액 |
|------|------|------|------|
| 영어 전문 검수 | $50/1000 단어 | ~500 단어 | $25 |
| 일본어 전문 번역 | $80/1000 단어 | ~500 단어 | $40 |
| 중국어 간체 번역 | $70/1000 단어 | ~500 단어 | $35 |
| 스페인어 번역 | $50/1000 단어 | ~500 단어 | $25 |
| 포르투갈어 번역 | $50/1000 단어 | ~500 단어 | $25 |
| 독일어 번역 | $60/1000 단어 | ~500 단어 | $30 |
| 프랑스어 번역 | $60/1000 단어 | ~500 단어 | $30 |
| **번역 총액** | | | **$210** |

### 7.2 스크린샷/그래픽 비용
| 항목 | 단가 | 수량 | 총액 |
|------|------|------|------|
| 언어별 스크린샷 제작 | $10/언어 | 7개 언어 | $70 |
| Feature Graphic 번역 | $20/언어 | 7개 언어 | $140 |
| **그래픽 총액** | | | **$210** |

### 7.3 도구/플랫폼 비용
| 항목 | 월 비용 | 개월 수 | 총액 |
|------|---------|---------|------|
| Crowdin (Pro) | $40/월 | 3개월 | $120 |
| DeepL API | $5/월 | 3개월 | $15 |
| **도구 총액** | | | **$135** |

### **총 예상 비용: $555** (약 75만 원)

*비용 절감 옵션:*
- 크라우드소싱/커뮤니티 번역 활용 시: ~$150 절감
- 자체 번역 (영어 가능 시): ~$100 절감
- 범용 스크린샷 사용: ~$140 절감

---

## 8. 성공 지표 (KPI)

### 8.1 정량적 지표
- **다운로드 수 증가**: 다국어 출시 후 3개월 내 +50% 목표
- **국가별 분포**:
  - Phase 1 후: 영어권 20%, 일본 10%
  - Phase 3 후: 한국 외 국가 50% 이상
- **평균 평점 유지**: 4.0+ (언어별)
- **번역 커버리지**: 전체 문자열 100% 번역 완료

### 8.2 정성적 지표
- **사용자 피드백**: 언어별 리뷰에서 번역 품질 언급
- **현지화 오류 제보**: 월 5건 이하 유지
- **커뮤니티 참여**: Crowdin 기여자 10명 이상 (Phase 3 이후)

### 8.3 모니터링 방법
- **Play Console Analytics**: 국가별 설치/유지율
- **Firebase Analytics**: 언어 설정별 사용 패턴
- **리뷰 감정 분석**: 언어별 긍정/부정 리뷰 비율
- **Crashlytics**: 언어별 크래시 보고 (특정 언어에서 레이아웃 오류 등)

---

## 9. 리스크 및 대응 방안

### 9.1 기술적 리스크
| 리스크 | 영향도 | 대응 방안 |
|--------|--------|-----------|
| 레이아웃 깨짐 (긴 텍스트) | 중 | Pseudolocalization 테스트, 최소 너비 설정 |
| 폰트 렌더링 문제 (CJK) | 저 | Noto Sans 폰트 번들링, 기기별 테스트 |
| 날짜/숫자 형식 오류 | 중 | 로케일별 단위 테스트 작성 |
| RTL 언어 미지원 | 저 | Phase 1-3에서는 LTR 언어만 (아랍어/히브리어는 보류) |

### 9.2 번역 품질 리스크
| 리스크 | 영향도 | 대응 방안 |
|--------|--------|-----------|
| 문맥 오역 | 높음 | 스크린샷과 컨텍스트 주석 제공, 네이티브 검수 |
| 용어 불일치 | 중 | 용어집 사전 구축, 번역 메모리 활용 |
| 격식체/반말 혼용 | 중 | 스타일 가이드 명시 (한국어: 존댓말, 일본어: です/ます調) |

### 9.3 운영 리스크
| 리스크 | 영향도 | 대응 방안 |
|--------|--------|-----------|
| 번역 업데이트 지연 | 중 | 문자열 동결 기간 설정, 릴리스 1주 전 번역 완료 |
| 다국어 지원 문의 증가 | 저 | FAQ 다국어 작성, 자동 응답 템플릿 |
| Play Store 거부 | 저 | 각 국가 콘텐츠 정책 사전 확인 (특히 중국) |

---

## 10. 장기 로드맵 (Phase 4+)

### 10.1 추가 언어 후보
- **러시아어** (Russian): 동유럽 시장
- **힌디어** (Hindi): 인도 시장
- **아랍어** (Arabic): RTL 지원 필요, 중동 시장
- **태국어, 베트남어**: 동남아시아 시장

### 10.2 고급 현지화 기능
- **문화권별 목표 일수 기본값**
  - 한국: 100일, 일본: 90일, 서양: 30일 (Dry January 등)
- **지역별 통화 자동 설정**
  - GPS/로케일 기반 기본 통화 (원, 엔, 달러 등)
- **국가별 금주 프로그램 링크**
  - AA (Alcoholics Anonymous) 지부 정보
  - 정부 건강 캠페인 링크

### 10.3 커뮤니티 기여 시스템
- **오픈소스 번역 포털**
  - GitHub에서 직접 PR
  - 기여자 크레딧 표시
- **사용자 피드백 루프**
  - 앱 내 "번역 개선 제안" 기능
  - 분기별 업데이트 반영

---

## 11. 체크리스트 및 참고 자료

### 11.1 Phase 1 출시 전 체크리스트
- [ ] 모든 하드코딩 문자열 제거 완료
- [ ] `values-en`, `values-ja` strings.xml 100% 번역
- [ ] 날짜/숫자 포맷 유틸리티 적용 및 테스트
- [ ] 3개 언어 × 5개 핵심 화면 레이아웃 검증
- [ ] Play Store 영어/일본어 메타데이터 등록
- [ ] 스크린샷 각 언어당 최소 5장
- [ ] Release Notes 다국어 작성
- [ ] QA 테스트 통과 (회귀 테스트 포함)
- [ ] `CHANGELOG.md`에 다국어 지원 명시

### 11.2 참고 자료
- [Android Localization Guide](https://developer.android.com/guide/topics/resources/localization)
- [Material Design - Internationalization](https://m3.material.io/foundations/accessible-design/overview)
- [Google Play - Store Listing Localization](https://support.google.com/googleplay/android-developer/answer/9844778)
- [Crowdin Documentation](https://support.crowdin.com/online-editor/)
- [Pseudo-localization in Android](https://developer.android.com/guide/topics/resources/pseudolocales)

### 11.3 관련 문서
- `APP_SPEC.md`: 앱 전체 기획 (현지화 요구사항 추가 예정)
- `PLAY_STORE_METADATA_TEMPLATE.md`: 메타데이터 템플릿 (다국어 확장)
- `README.md`: 프로젝트 개요 (다국어 지원 명시)

---

## 부록: 번역 요청 템플릿

### A. 전문 번역사 의뢰 시 제공 정보
```
**프로젝트**: AlcoholicTimer Android 앱
**소스 언어**: 한국어
**타겟 언어**: [영어/일본어/중국어 등]
**단어 수**: 약 500 단어
**형식**: Android strings.xml
**톤앤매너**: 친근하고 격려적, 존댓말 (한국어 기준)
**전문 용어**:
- 금주: Sobriety / Abstinence
- 레벨: Level
- 성공률: Success Rate
- 절약한 금액: Saved Money

**컨텍스트**: 
금주/절주를 돕는 습관 형성 앱입니다. 사용자는 실패를 두려워할 수 있으므로 긍정적이고 동기부여하는 문구가 중요합니다.

**제공 자료**:
- strings.xml (원본)
- 앱 스크린샷 5장
- 용어집 (별첨)

**납기**: [날짜]
**예산**: [금액]
```

### B. 크라우드소싱 모집 공지 템플릿
```
📢 AlcoholicTimer 다국어 번역 기여자 모집!

금주 습관 형성을 돕는 앱 'AlcoholicTimer'를 세계 사용자에게 전달하는 데 도움을 주세요.

**모집 언어**: 영어, 일본어, 중국어, 스페인어
**작업량**: 약 500 단어
**도구**: Crowdin (웹 기반, 가입 필요)
**혜택**:
- 앱 크레딧에 이름 표시
- 번역 경험 인증서 제공
- 우수 기여자: Amazon 기프트 카드 $20

**참여 방법**:
1. [Crowdin 링크] 접속
2. 번역할 언어 선택
3. 문자열 번역 시작!

**문의**: [이메일]
```

---

## 부록 C: 번역 완료 현황 (2025-10-28 기준)

### ✅ Phase 1-2 번역 완료 언어

#### 1. 영어 (English) - `values-en`
- **상태**: 완료 ✅
- **문자열 수**: 227개 (100%)
- **통화**: USD ($) - 환율 1,300.0
- **날짜 형식**: MM/dd/yyyy
- **특이사항**: 기본 글로벌 언어, 모든 메타데이터 영어 작성 필수

#### 2. 일본어 (Japanese) - `values-ja`
- **상태**: 완료 ✅ (2025-10-28)
- **문자열 수**: 227개 (100%)
- **통화**: JPY (¥) - 환율 0.1 (10원 = 1엔)
- **날짜 형식**: yyyy年MM月dd日
- **특이사항**: 
  - 시간 형식: H:mm (24시간제)
  - CurrencyManager에 JPY 통화 옵션 구현 완료
  - 기본 닉네임: "禁酒勇者1"

#### 3. 중국어 간체 (Simplified Chinese) - `values-zh-rCN`
- **상태**: 완료 ✅ (2025-10-28)
- **문자열 수**: 227개 (100%)
- **통화**: CNY (¥) - 환율 180.0
- **날짜 형식**: yyyy年MM月dd日
- **특이사항**:
  - 기본 닉네임: "戒酒达人1"
  - 레벨명 현지화 (예: "初心7天", "一月奇迹")
  - 비용 옵션: ￥70 이하/중/이상
  - CurrencyManager에 CNY 통화 옵션 구현 완료

#### 4. 스페인어 (Spanish) - `values-es`
- **상태**: 완료 ✅ (2025-10-28)
- **문자열 수**: 227개 (100%)
- **통화**: EUR (€) - 환율 1,400.0 (기본값)
- **날짜 형식**: dd/MM/yyyy
- **특이사항**:
  - 기본 닉네임: "Sobrio1"
  - 레벨명 현지화 (예: "7 Días de Propósito", "Leyenda de la Moderación")
  - 단수/복수 형태 고려 (día/días, hora/horas)
  - 비용 옵션: €7 이하/중/이상
  - CurrencyManager에 EUR 통화 옵션 구현 완료

### 📊 번역 통계

| 언어 | 파일 경로 | 문자열 수 | 완료율 | 통화 | 날짜 형식 |
|------|-----------|----------|--------|------|----------|
| 한국어 (기본) | `values/` | 227 | 100% | KRW (₩) | yyyy-MM-dd |
| 영어 | `values-en/` | 227 | 100% | USD ($) | MM/dd/yyyy |
| 일본어 | `values-ja/` | 227 | 100% | JPY (¥) | yyyy年MM月dd日 |
| 중국어 간체 | `values-zh-rCN/` | 227 | 100% | CNY (¥) | yyyy年MM月dd日 |
| 스페인어 | `values-es/` | 227 | 100% | EUR (€) | dd/MM/yyyy |
| **총계** | **5개 언어** | **1,135** | **100%** | **5개 통화** | **4개 형식** |

### 🔧 구현 완료 사항

1. **FormatUtils.kt**
   - ✅ `daysToDayHourString()`: 일/시간 포맷 (다국어 지원)
   - ✅ `formatMoney()`: 통화 포맷 (CurrencyManager 연동)

2. **CurrencyManager.kt**
   - ✅ 5개 통화 지원 (KRW, JPY, USD, EUR, CNY)
   - ✅ 로케일 기반 자동 통화 감지
   - ✅ 사용자 설정 저장/불러오기
   - ✅ 모든 통화 소수점 2자리 표시

3. **날짜/시간 현지화**
   - ✅ AddRecordActivity.kt: 로케일별 날짜 형식
   - ✅ DetailActivity.kt: 로케일별 날짜/시간 표시

### 📝 다음 단계 (To-Do)

#### Phase 2 마무리
- [ ] 중국어/스페인어 UI 테스트
  - 레이아웃 깨짐 확인
  - 긴 텍스트 처리 검증
  - 통화/날짜 형식 실기기 테스트

- [ ] Play Store 메타데이터 작성
  - 중국어: 앱 설명, 스크린샷 캡션
  - 스페인어: 앱 설명, 스크린샷 캡션

- [ ] 스크린샷 캡처
  - 중국어 UI 스크린샷 5장
  - 스페인어 UI 스크린샷 5장

#### Phase 3 준비
- [ ] 포르투갈어 (Portuguese - Brazil) 번역
  - `values-pt-rBR/strings.xml` 생성
  - 통화: BRL (R$) - 환율 250.0
  - 날짜 형식: dd/MM/yyyy

- [ ] 독일어 (German) 번역
  - `values-de/strings.xml` 생성
  - 통화: EUR (€)
  - 날짜 형식: dd.MM.yyyy
  - 긴 합성어 레이아웃 테스트 필수

- [ ] 프랑스어 (French) 번역
  - `values-fr/strings.xml` 생성
  - 통화: EUR (€)
  - 날짜 형식: dd/MM/yyyy

### 🎯 릴리스 계획

- **v1.2.0**: 영어, 일본어 출시 (Phase 1)
- **v1.3.0**: 중국어, 스페인어 추가 (Phase 2) ← **현재 준비 중**
- **v1.4.0**: 포르투갈어, 독일어, 프랑스어 추가 (Phase 3)

---

**마지막 업데이트**: 2025-10-28  
**작성자**: AlcoholicTimer 개발팀  
**버전**: 1.1 (중국어/스페인어 번역 완료)
