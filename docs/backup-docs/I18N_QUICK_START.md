# 다국어화 빠른 시작 가이드 (Quick Start)

> 🎯 **목표**: AlcoholicTimer 앱을 영어로 출시하기까지 6주 완성 로드맵

최종 수정: 2025-10-27

---

## 📋 전체 문서 구조

```
docs/
├── INTERNATIONALIZATION_PLAN.md          # 📘 전체 기획안 (전략, 시장 분석, 3단계 계획)
├── I18N_IMPLEMENTATION_GUIDE.md          # 🛠️ 구현 상세 가이드 (코드, 테스트, 출시)
└── I18N_QUICK_START.md (이 문서)         # ⚡ 빠른 실행 요약
```

**읽는 순서**:
1. 이 문서 (Quick Start) - 전체 흐름 파악
2. INTERNATIONALIZATION_PLAN.md - 전략 이해
3. I18N_IMPLEMENTATION_GUIDE.md - 실제 작업 시작

---

## ⏱️ 6주 타임라인 (Phase 1: 영어 출시)

| 주차 | 핵심 작업 | 산출물 | 소요 시간 |
|------|----------|--------|----------|
| **Week 1-2** | 코드 리팩터링 | 모든 문자열 → strings.xml | 15시간 |
| **Week 3-4** | 영어 번역 | values-en/strings.xml 완성 | 10시간 |
| **Week 5-6** | 테스트 & 출시 | Play Store 영어 메타데이터 | 10시간 |
| **총계** | | | **35시간** |

---

## 🎯 Week 1-2: 준비 작업

### Day 1-2: 현황 파악 (4시간)
```bash
# 1. 하드코딩 문자열 검색
cd G:\Workspace\AlcoholicTimer
findstr /s /i /n /r "\"[가-힣]" app\src\main\java\*.kt > hardcoded_strings.txt

# 2. 결과 확인 (예상: 50~100개 문자열)
notepad hardcoded_strings.txt
```

**체크리스트**:
- [ ] `hardcoded_strings.txt` 생성됨
- [ ] 주요 파일 식별 (RunActivity, QuitActivity, DetailActivity 등)
- [ ] 예상 작업량 파악

### Day 3-5: strings.xml 이동 (8시간)
```xml
<!-- app/src/main/res/values/strings.xml에 추가 -->

<!-- 금주 진행 화면 -->
<string name="indicator_title_days">금주 일수</string>
<string name="indicator_title_time">시간</string>
<string name="saved_money_format">%1$s원</string>

<!-- 금주 종료 화면 -->
<string name="quit_confirm_title">정말 멈추시겠어요?</string>
<string name="quit_confirm_subtitle">지금까지 잘 해오셨는데&#8230;</string>
```

**Kotlin 코드 변경**:
```kotlin
// Before ❌
Text("금주 일수")
Toast.makeText(context, "금주 목표를 달성했습니다!", Toast.LENGTH_SHORT).show()

// After ✅
Text(stringResource(R.string.indicator_title_days))
Toast.makeText(context, getString(R.string.toast_goal_completed), Toast.LENGTH_SHORT).show()
```

**체크리스트**:
- [ ] 모든 하드코딩 문자열을 `R.string.*`로 변경
- [ ] 빌드 성공 (`./gradlew assembleDebug`)
- [ ] 앱 실행 정상 (한국어)

### Day 6-10: 날짜/숫자 포맷 유틸리티 (3시간)

**파일 생성**: `app/src/main/java/.../utils/LocaleFormatUtils.kt`

```kotlin
object LocaleFormatUtils {
    fun formatCurrency(amount: Double, locale: Locale = Locale.getDefault()): String {
        return when (locale.language) {
            "ko" -> String.format(locale, "%,.0f원", amount)
            "en" -> String.format(locale, "$%.2f", amount)
            "ja" -> String.format(locale, "¥%,.0f", amount)
            else -> String.format(locale, "%.2f", amount)
        }
    }
    
    fun getDateFormat(locale: Locale = Locale.getDefault()): SimpleDateFormat {
        val pattern = when (locale.language) {
            "ko" -> "yyyy년 M월 d일"
            "en" -> "MMM d, yyyy"
            "ja" -> "yyyy年M月d日"
            else -> "MMM d, yyyy"
        }
        return SimpleDateFormat(pattern, locale)
    }
}
```

**체크리스트**:
- [ ] `LocaleFormatUtils.kt` 생성
- [ ] 기존 `String.format(...원)` → `formatCurrency()` 변경
- [ ] 단위 테스트 작성 및 통과

---

## 🌐 Week 3-4: 영어 번역

### Day 11-13: values-en 생성 (5시간)

**디렉토리 생성**:
```bash
mkdir app\src\main\res\values-en
```

**초벌 번역** (DeepL 또는 Google Translate 활용):
- `values/strings.xml` 복사 → `values-en/strings.xml`
- 각 문자열 번역
- 컨텍스트 주석 확인

**예시**:
```xml
<!-- values-en/strings.xml -->
<string name="run_title">Sobriety Progress</string>
<string name="indicator_title_days">Days Sober</string>
<string name="saved_money_format">$%1$s</string>
<string name="quit_confirm_title">Are you sure you want to stop?</string>
<string name="quit_confirm_subtitle">You\'ve been doing so well&#8230;</string>
```

**체크리스트**:
- [ ] `values-en/strings.xml` 파일 생성
- [ ] 모든 문자열 초벌 번역 완료 (약 80개)
- [ ] 빌드 오류 없음

### Day 14-16: 네이티브 검수 (3시간 + 외주 대기)

**외주 플랫폼**:
- Upwork: https://www.upwork.com
- Fiverr: https://www.fiverr.com
- Gengo: https://gengo.com

**의뢰 내용**:
```
Title: Android App String Translation Review (Korean → English)
Description:
- Word count: ~500 words
- Format: Android strings.xml
- App type: Sobriety tracker (habit-forming app)
- Tone: Friendly, encouraging
- Budget: $25-30
- Delivery: 2-3 days

Attachments:
- values-en/strings.xml (draft)
- App screenshots (5 images)
- Glossary (key terms)
```

**체크리스트**:
- [ ] 번역사 선정 및 의뢰
- [ ] 피드백 반영 (2-3일 소요)
- [ ] 최종 `values-en/strings.xml` 확정

### Day 17-20: Plurals & Format 정리 (2시간)

**Plurals 추가**:
```xml
<!-- values/strings.xml -->
<plurals name="days_count">
    <item quantity="one">%d일</item>
    <item quantity="other">%d일</item>
</plurals>

<!-- values-en/strings.xml -->
<plurals name="days_count">
    <item quantity="one">%d day</item>
    <item quantity="other">%d days</item>
</plurals>
```

**코드 적용**:
```kotlin
val daysText = LocalContext.current.resources.getQuantityString(
    R.plurals.days_count,
    days,
    days
)
```

---

## 🧪 Week 5-6: 테스트 & 출시

### Day 21-25: 레이아웃 테스트 (4시간)

**에뮬레이터 언어 변경**:
1. Settings → System → Languages → English
2. 앱 재시작
3. 모든 화면 확인

**체크리스트** (화면별):
- [ ] **Run**: Indicator 타이틀 줄바꿈 없음
- [ ] **Quit**: 다이얼로그 버튼 간격 적절
- [ ] **Detail**: 날짜 형식 "Oct 27, 2025"
- [ ] **Records**: 빈 상태 메시지 표시
- [ ] **About**: License 정보 정렬

**Pseudolocalization 테스트**:
- Settings → Languages → **English (XA)**
- 긴 텍스트 시뮬레이션 확인

### Day 26-28: Play Store 메타데이터 (3시간)

**짧은 설명** (80자):
```
English: Track your sobriety journey with simple records and success rates.
```

**전체 설명** (4000자):
```
AlcoholicTimer is a simple and focused sobriety tracker.

Key Features:
• Track daily sobriety/abstinence records
• View weekly, monthly, yearly statistics
• Monitor your longest streak
• 100% offline, no data collection

Privacy Policy:
No personal data collected. All records stored locally.

Contact: your-email@example.com
```

**스크린샷** (최소 5장):
1. 에뮬레이터를 영어로 설정
2. 각 화면 캡처:
   - Run (진행 중)
   - Records (주간 통계)
   - Detail (기록 상세)
   - All Records (전체 목록)
   - About (정보)

**체크리스트**:
- [ ] 영어 짧은 설명 작성
- [ ] 영어 전체 설명 작성
- [ ] 스크린샷 5장 준비 (1080x1920)

### Day 29-30: QA 및 출시 (3시간)

**Lint 검사**:
```bash
./gradlew lintDebug
# 결과: app\build\reports\lint-results-debug.html 확인
```

**주요 확인 사항**:
- [ ] MissingTranslation 경고 없음
- [ ] 크래시 없음 (한국어/영어 전환 반복)
- [ ] 메모리 누수 없음

**버전 업데이트**:
```kotlin
// app/build.gradle.kts
val releaseVersionCode = 2025110100
val releaseVersionName = "1.2.0"  // 다국어 지원
```

**CHANGELOG.md**:
```markdown
## [1.2.0] - 2025-11-01

### Added
- 🌐 English language support
- 🗓️ Localized date/time formats
- 💰 Currency conversion (USD)

### Fixed
- Layout issues with long text
```

**Play Console 업로드**:
```bash
# 1. Keystore 환경변수 설정
set KEYSTORE_PATH=G:\keystore\alcoholictimer-release.jks
set KEYSTORE_STORE_PW=your_password
set KEY_ALIAS=alcoholictimer
set KEY_PASSWORD=your_key_password

# 2. Release 빌드
./gradlew bundleRelease

# 3. AAB 위치 확인
dir app\build\outputs\bundle\release\app-release.aab
```

**Play Console 단계**:
1. **릴리스** → **프로덕션** → **새 릴리스**
2. AAB 업로드
3. **출시 노트** 입력:
   - 한국어: "영어 지원 추가"
   - English: "Added English language support"
4. **저장** → **검토** → **단계적 출시 (10%)**
5. 48시간 모니터링 → 문제 없으면 100% 확대

---

## ✅ 최종 체크리스트

### 코드
- [ ] 모든 하드코딩 문자열 제거
- [ ] `values-en/strings.xml` 100% 번역 완료
- [ ] `LocaleFormatUtils` 구현 및 적용
- [ ] Plurals 정리
- [ ] 단위 테스트 통과

### UI/UX
- [ ] 한국어/영어 레이아웃 검증
- [ ] Pseudolocalization 테스트
- [ ] 접근성 (TalkBack) 확인
- [ ] 다크 모드 정상 표시

### Play Store
- [ ] 영어 짧은 설명 (80자)
- [ ] 영어 전체 설명 (500자+)
- [ ] 영어 스크린샷 5장 (1080x1920)
- [ ] Release Notes (한국어/영어)

### QA
- [ ] Lint 검사 통과 (MissingTranslation 없음)
- [ ] 크래시 없음 (Crashlytics 확인)
- [ ] 회귀 테스트 통과 (기존 기능)

### 출시
- [ ] 버전 1.2.0 (versionCode 증가)
- [ ] CHANGELOG.md 업데이트
- [ ] AAB 빌드 성공
- [ ] Play Console 업로드 완료
- [ ] 단계적 출시 시작 (10%)

---

## 💰 예산 요약

| 항목 | 비용 | 비고 |
|------|------|------|
| 영어 번역 검수 | $25-30 | Upwork/Fiverr |
| 스크린샷 제작 | $0 | 자체 제작 (에뮬레이터) |
| 도구 (DeepL API) | $0 | 무료 플랜 |
| **총계** | **$25-30** | |

---

## 📊 성공 지표 (출시 후 1개월)

| 지표 | 목표 | 측정 방법 |
|------|------|----------|
| 영어권 다운로드 | +30% | Play Console Analytics |
| 평균 평점 (영어) | 4.0+ | Play Console 리뷰 |
| 번역 오류 제보 | <5건 | 리뷰/이메일 |
| 크래시율 (영어) | <1% | Firebase Crashlytics |

---

## 🚀 다음 단계 (Phase 2)

### Week 7-10: 일본어 추가
- 전문 번역사 의뢰 (일본어)
- Noto Sans JP 폰트 검토
- Play Store 일본어 메타데이터

### Week 11+: 자동화
- Crowdin 플랫폼 연동
- 커뮤니티 번역 시스템
- 중국어/스페인어 확장

---

## 📚 참고 자료

### 필수 문서
- `INTERNATIONALIZATION_PLAN.md`: 전체 전략 (7개 언어 로드맵)
- `I18N_IMPLEMENTATION_GUIDE.md`: 상세 구현 가이드 (코드 샘플)
- `APP_SPEC.md`: 앱 전체 기획

### 외부 링크
- [Android Localization Guide](https://developer.android.com/guide/topics/resources/localization)
- [Material Design - Internationalization](https://m3.material.io/foundations/accessible-design/overview)
- [DeepL Translator](https://www.deepl.com/translator)

### 도구
- **Android Studio Translations Editor**: 내장
- **Lint**: `./gradlew lintDebug`
- **ADB**: 언어 변경 `adb shell setprop persist.sys.locale en-US`

---

## 🆘 문제 발생 시

### 1. 번역 누락 경고
```bash
./gradlew lintDebug
# → MissingTranslation 확인 후 추가
```

### 2. 레이아웃 깨짐
- `modifier = Modifier.weight(1f)` 사용
- `maxLines = 2` + `TextOverflow.Ellipsis`

### 3. 날짜 형식 오류
- `LocaleFormatUtils.getDateFormat()` 사용 확인

### 4. 긴급 롤백
- Play Console → 이전 버전 (1.1.0) 활성화

---

**작성일**: 2025-10-27  
**작성자**: AlcoholicTimer 개발팀  
**버전**: 1.0

**다음 액션**: `INTERNATIONALIZATION_PLAN.md` 읽고 전체 전략 이해 →  
`I18N_IMPLEMENTATION_GUIDE.md`로 실제 코드 작업 시작!

