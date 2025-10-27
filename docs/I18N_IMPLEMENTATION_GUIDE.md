# 다국어화 구현 가이드 (Implementation Guide)

최종 수정: 2025-10-27

이 문서는 `INTERNATIONALIZATION_PLAN.md`의 Phase 1을 실제로 구현하기 위한 단계별 상세 가이드입니다.

---

## 목차
1. [환경 설정](#1-환경-설정)
2. [문자열 리소스 마이그레이션](#2-문자열-리소스-마이그레이션)
3. [날짜/숫자 포맷 현지화](#3-날짜숫자-포맷-현지화)
4. [영어 번역 작업](#4-영어-번역-작업)
5. [레이아웃 검증 및 테스트](#5-레이아웃-검증-및-테스트)
6. [Play Store 메타데이터](#6-play-store-메타데이터)
7. [QA 및 출시](#7-qa-및-출시)

---

## 1. 환경 설정

### 1.1 Android Studio 설정

#### Translations Editor 활성화
1. Android Studio에서 `app/src/main/res/values/strings.xml` 열기
2. 우측 상단 **Open editor** 클릭 (또는 `Alt+E`)
3. Translations Editor에서 언어 추가:
   - 좌상단 지구본 아이콘 → **Add Locale**
   - `en: English` 선택
   - `ja: Japanese` 선택

#### Pseudolocalization 테스트 활성화
```kotlin
// app/build.gradle.kts에 추가
android {
    defaultConfig {
        // ...existing code...
        
        // Pseudolocalization 활성화 (긴 텍스트 테스트용)
        resourceConfigurations += listOf("en", "ja", "en-rXA", "ar-rXB")
    }
}
```

### 1.2 Git 브랜치 전략
```bash
# 메인 작업 브랜치 생성
git checkout -b feature/i18n-phase1

# 세부 작업별 서브 브랜치
git checkout -b feature/i18n-string-extraction
git checkout -b feature/i18n-date-format
git checkout -b feature/i18n-english-translation
```

### 1.3 필수 도구 설치

#### DeepL API (번역 초안 생성용)
```bash
# Python 환경 (옵션)
pip install deepl

# 또는 웹 인터페이스 사용: https://www.deepl.com/translator
```

#### Android Lint 다국어 체크
```bash
# 누락된 번역 검출
./gradlew lintDebug

# 결과 확인: app/build/reports/lint-results-debug.html
```

---

## 2. 문자열 리소스 마이그레이션

### 2.1 하드코딩 문자열 검색

#### 자동 검색 스크립트
```bash
# 모든 Kotlin 파일에서 하드코딩 문자열 찾기
cd G:\Workspace\AlcoholicTimer
findstr /s /i /n /r "\"[가-힣]" app\src\main\java\*.kt > hardcoded_strings.txt

# 영문 하드코딩도 검색 (더 복잡하므로 수동 확인)
findstr /s /i /n /r "Text(\"[A-Za-z]" app\src\main\java\*.kt >> hardcoded_strings.txt
```

#### 주요 검색 대상 패턴
```kotlin
// ❌ 잘못된 예시
Text("금주 진행")
"목표: ${days}일"
contentDescription = "뒤로가기"

// ✅ 올바른 예시
Text(stringResource(R.string.run_title))
stringResource(R.string.goal_days_format, days)
contentDescription = stringResource(R.string.cd_navigate_back)
```

### 2.2 strings.xml 구조화

#### 기존 strings.xml 리팩터링
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- ========================================
         App Information
         ======================================== -->
    <string name="app_name">Alcoholic Timer</string>
    
    <!-- ========================================
         Common / Shared
         ======================================== -->
    <string name="cd_navigate_back">뒤로가기</string>
    <string name="action_cancel">취소</string>
    <string name="action_confirm">확인</string>
    <string name="action_delete">삭제</string>
    <string name="action_retry">다시 시도</string>
    <string name="action_copy">복사</string>
    
    <!-- ========================================
         Units (단위)
         ======================================== -->
    <string name="unit_day">일</string>
    <string name="unit_hour">시간</string>
    <string name="unit_currency_krw">원</string>
    
    <!-- Plurals for days -->
    <plurals name="days_count">
        <item quantity="one">%d일</item>
        <item quantity="other">%d일</item>
    </plurals>
    
    <!-- ========================================
         Run Screen (금주 진행 화면)
         ======================================== -->
    <string name="run_title">금주 진행</string>
    <string name="stat_goal_days">목표일</string>
    <string name="stat_level">Level</string>
    <string name="stat_time">시간</string>
    
    <!-- ...existing strings... -->
    
    <!-- ========================================
         Format Strings (변수 포함)
         ======================================== -->
    <!-- %1$d = current days, %2$d = target days -->
    <string name="progress_format">%1$d / %2$d일</string>
    
    <!-- %1$s = amount (formatted), %2$s = currency symbol -->
    <string name="saved_money_format">%1$s%2$s</string>
    
    <!-- ========================================
         Error Messages
         ======================================== -->
    <string name="error_loading_records">기록을 불러오는 데 실패했어요</string>
    <string name="error_network">네트워크 연결을 확인해주세요</string>
    
    <!-- ========================================
         Accessibility (Content Descriptions)
         ======================================== -->
    <string name="cd_stop">정지</string>
    <string name="cd_continue">계속</string>
    <string name="cd_delete_all_records">모든 기록 삭제</string>
    
    <!-- ========================================
         Toast Messages
         ======================================== -->
    <string name="toast_goal_completed">금주 목표를 달성했습니다!</string>
    <string name="toast_copied">주소를 복사했습니다.</string>
</resources>
```

#### 번역 컨텍스트 주석 추가
```xml
<!-- 
    금주를 종료할 때 표시되는 확인 메시지. 
    톤: 격려하고 아쉬워하는 느낌
    컨텍스트: 사용자가 '멈추기' 버튼을 눌렀을 때 다이얼로그 제목
-->
<string name="quit_confirm_title">정말 멈추시겠어요?</string>

<!-- 
    위 메시지의 부제목. 사용자의 도전을 인정하는 문구
    말줄임표(…)는 여운을 남기기 위함
-->
<string name="quit_confirm_subtitle">지금까지 잘 해오셨는데&#8230;</string>
```

### 2.3 코드 변경 체크리스트

#### 파일별 수정 목록 (예시)
```
[ ] app/src/main/java/com/example/alcoholictimer/feature/run/RunActivity.kt
    - Line 148: Toast 메시지 → R.string.toast_goal_completed
    - Line 183-185: Stat labels → R.string.stat_*
    - Line 202-206: Indicator titles → R.string.indicator_title_*
    
[ ] app/src/main/java/com/example/alcoholictimer/feature/quit/QuitActivity.kt
    - Line 106: 타이틀 → R.string.quit_confirm_title
    - Line 114: 서브타이틀 → R.string.quit_confirm_subtitle
    
[ ] app/src/main/java/com/example/alcoholictimer/feature/detail/DetailActivity.kt
    - [모든 하드코딩 확인]
    
[ ] ... (모든 Activity/Composable 파일)
```

---

## 3. 날짜/숫자 포맷 현지화

### 3.1 LocaleFormatUtils.kt 생성

```kotlin
// app/src/main/java/com/example/alcoholictimer/utils/LocaleFormatUtils.kt
package com.example.alcoholictimer.utils

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocaleFormatUtils {
    
    /**
     * 로케일에 맞는 날짜 포맷 반환
     * 
     * 예시:
     * - ko: 2025년 10월 27일
     * - en: Oct 27, 2025
     * - ja: 2025年10月27日
     */
    fun getDateFormat(locale: Locale = Locale.getDefault()): SimpleDateFormat {
        val pattern = when (locale.language) {
            "ko" -> "yyyy년 M월 d일"
            "ja" -> "yyyy年M月d日"
            "zh" -> "yyyy年M月d日"
            "en" -> "MMM d, yyyy"
            "es" -> "d 'de' MMM 'de' yyyy"
            "pt" -> "d 'de' MMM 'de' yyyy"
            "de" -> "d. MMM yyyy"
            "fr" -> "d MMM yyyy"
            else -> "MMM d, yyyy" // 기본 영어 형식
        }
        return SimpleDateFormat(pattern, locale)
    }
    
    /**
     * 날짜+시간 포맷
     */
    fun getDateTimeFormat(locale: Locale = Locale.getDefault()): SimpleDateFormat {
        val datePattern = when (locale.language) {
            "ko" -> "yyyy년 M월 d일"
            "ja", "zh" -> "yyyy年M月d日"
            "en" -> "MMM d, yyyy"
            else -> "MMM d, yyyy"
        }
        val timePattern = "HH:mm"
        return SimpleDateFormat("$datePattern $timePattern", locale)
    }
    
    /**
     * 시간만 포맷 (12시간/24시간 자동 선택)
     */
    fun getTimeFormat(context: Context, locale: Locale = Locale.getDefault()): SimpleDateFormat {
        val pattern = if (DateFormat.is24HourFormat(context)) {
            "HH:mm"
        } else {
            "h:mm a"
        }
        return SimpleDateFormat(pattern, locale)
    }
    
    /**
     * 날짜 포맷팅 헬퍼
     */
    fun formatDate(date: Date, locale: Locale = Locale.getDefault()): String {
        return getDateFormat(locale).format(date)
    }
    
    /**
     * 금액 포맷 (로케일별 통화)
     * 
     * @param amount 금액
     * @param locale 로케일 (기본: 시스템 로케일)
     * @return 포맷된 문자열 (예: "10,000원", "$100.00", "¥1,000")
     */
    fun formatCurrency(amount: Double, locale: Locale = Locale.getDefault()): String {
        return when (locale.language) {
            "ko" -> String.format(locale, "%,.0f원", amount)
            "ja" -> String.format(locale, "¥%,.0f", amount)
            "zh" -> String.format(locale, "¥%,.0f", amount)
            "en" -> {
                when (locale.country) {
                    "GB" -> String.format(locale, "£%.2f", amount)
                    "US" -> String.format(locale, "$%.2f", amount)
                    else -> String.format(locale, "$%.2f", amount)
                }
            }
            "es", "pt", "fr", "de" -> String.format(locale, "€%.2f", amount)
            else -> String.format(locale, "%.2f", amount) // 심볼 없는 기본값
        }
    }
    
    /**
     * 숫자 포맷 (천 단위 구분자)
     */
    fun formatNumber(number: Double, decimals: Int = 1, locale: Locale = Locale.getDefault()): String {
        val format = String.format(locale, "%%.${decimals}f", number)
        return String.format(locale, "%,.${decimals}f", number)
    }
}
```

### 3.2 기존 코드 적용

#### Before (하드코딩)
```kotlin
// RunActivity.kt
val savedMoneyText = String.format(Locale.getDefault(), "%,.0f원", savedMoney)
```

#### After (현지화)
```kotlin
// RunActivity.kt
import com.example.alcoholictimer.utils.LocaleFormatUtils

val savedMoneyText = LocaleFormatUtils.formatCurrency(savedMoney)
```

### 3.3 단위 테스트 작성

```kotlin
// app/src/test/java/com/example/alcoholictimer/LocaleFormatUtilsTest.kt
package com.example.alcoholictimer

import com.example.alcoholictimer.utils.LocaleFormatUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocaleFormatUtilsTest {
    
    @Test
    fun testCurrencyFormat_Korean() {
        val result = LocaleFormatUtils.formatCurrency(10000.0, Locale.KOREAN)
        assertEquals("10,000원", result)
    }
    
    @Test
    fun testCurrencyFormat_English() {
        val result = LocaleFormatUtils.formatCurrency(100.0, Locale.US)
        assertEquals("$100.00", result)
    }
    
    @Test
    fun testCurrencyFormat_Japanese() {
        val result = LocaleFormatUtils.formatCurrency(1000.0, Locale.JAPANESE)
        assertEquals("¥1,000", result)
    }
    
    @Test
    fun testDateFormat_Korean() {
        val locale = Locale.KOREAN
        val format = LocaleFormatUtils.getDateFormat(locale)
        val pattern = format.toPattern()
        assertEquals("yyyy년 M월 d일", pattern)
    }
    
    @Test
    fun testDateFormat_English() {
        val locale = Locale.US
        val format = LocaleFormatUtils.getDateFormat(locale)
        val pattern = format.toPattern()
        assertEquals("MMM d, yyyy", pattern)
    }
}
```

실행:
```bash
./gradlew test --tests LocaleFormatUtilsTest
```

---

## 4. 영어 번역 작업

### 4.1 values-en/strings.xml 생성

#### 디렉토리 생성
```bash
mkdir app\src\main\res\values-en
```

#### 초벌 번역 (DeepL/Google Translate)
```python
# scripts/translate_strings.py (옵션)
import deepl
import xml.etree.ElementTree as ET

translator = deepl.Translator("YOUR_DEEPL_API_KEY")

tree = ET.parse('app/src/main/res/values/strings.xml')
root = tree.getroot()

for string_elem in root.findall('string'):
    text = string_elem.text
    if text and any('\u3131' <= c <= '\u318F' or '\uAC00' <= c <= '\uD7A3' for c in text):
        # 한글 포함 시 번역
        result = translator.translate_text(text, target_lang="EN-US")
        print(f"<string name=\"{string_elem.get('name')}\">{result.text}</string>")
```

#### 수작업 번역 (권장)
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- ========================================
         App Information
         ======================================== -->
    <string name="app_name">Alcoholic Timer</string>
    
    <!-- ========================================
         Common / Shared
         ======================================== -->
    <string name="cd_navigate_back">Navigate back</string>
    <string name="action_cancel">Cancel</string>
    <string name="action_confirm">Confirm</string>
    <string name="action_delete">Delete</string>
    <string name="action_retry">Retry</string>
    <string name="action_copy">Copy</string>
    
    <!-- ========================================
         Units
         ======================================== -->
    <string name="unit_day">day</string>
    <string name="unit_hour">hour</string>
    <!-- Note: 통화 심볼은 코드에서 처리 -->
    
    <!-- Plurals for days -->
    <plurals name="days_count">
        <item quantity="one">%d day</item>
        <item quantity="other">%d days</item>
    </plurals>
    
    <!-- ========================================
         Run Screen
         ======================================== -->
    <string name="run_title">Sobriety Progress</string>
    <string name="stat_goal_days">Goal</string>
    <string name="stat_level">Level</string>
    <string name="stat_time">Time</string>
    
    <string name="indicator_title_days">Days Sober</string>
    <string name="indicator_title_time">Time</string>
    <string name="indicator_title_saved_money">Money Saved</string>
    <string name="indicator_title_saved_hours">Hours Saved</string>
    <string name="indicator_title_life_gain">Life Expectancy +</string>
    <string name="tap_to_switch_indicator">Tap to view other metrics</string>
    
    <string name="cd_stop">Stop</string>
    <string name="cd_continue">Continue</string>
    
    <string name="toast_goal_completed">You\'ve achieved your sobriety goal!</string>
    
    <!-- ========================================
         Update Snackbar
         ======================================== -->
    <string name="update_downloaded_restart_prompt">Update downloaded. Restart to install.</string>
    <string name="action_restart">Restart</string>
    
    <!-- ========================================
         Start Overlay
         ======================================== -->
    <string name="checking_update">Checking for updates&#8230;</string>
    
    <!-- ========================================
         Quit Screen
         ======================================== -->
    <string name="quit_title">End Sobriety</string>
    <string name="quit_confirm_title">Are you sure you want to stop?</string>
    <string name="quit_confirm_subtitle">You\'ve been doing so well&#8230;</string>
    <string name="stat_total_days">Total Sober Days</string>
    <string name="stat_saved_money_short">Money Saved</string>
    <string name="stat_saved_hours_short">Hours Saved</string>
    
    <!-- ========================================
         Detail Screen
         ======================================== -->
    <string name="detail_title">Record Details</string>
    <string name="dialog_delete_title">Delete Record</string>
    <string name="dialog_delete_message">Are you sure you want to delete this record?&#10;This action cannot be undone.</string>
    <string name="dialog_delete_confirm">Delete</string>
    <string name="dialog_cancel">Cancel</string>
    <string name="detail_start_label">Started:</string>
    <string name="detail_end_label">Ended:</string>
    <string name="detail_progress_rate">Goal Achievement</string>
    <string name="detail_progress_current">Progress: %1$s days</string>
    <string name="detail_progress_target">Goal: %1$d days</string>
    <string name="detail_today_time">Today - %1$s</string>
    <string name="unit_day">day</string>
    
    <!-- ========================================
         All Records Screen
         ======================================== -->
    <string name="all_records_title">All Records</string>
    <string name="error_loading_records">Failed to load records</string>
    <string name="retry">Retry</string>
    <string name="empty_records_title">No records yet</string>
    <string name="empty_records_subtitle">Start your first sobriety journey!</string>
    <string name="empty_records_cd">No records to display</string>
    <string name="cd_delete_all_records">Delete all records</string>
    <string name="all_records_delete_title">Delete All Records</string>
    <string name="all_records_delete_message">Are you sure you want to delete all records?&#10;This action cannot be undone.</string>
    <string name="all_records_delete_confirm">Delete All</string>
    
    <!-- ========================================
         About Screen
         ======================================== -->
    <string name="about_title">About</string>
    <string name="about_version_info">Version Info</string>
    <string name="about_open_license_notice">Open Source Licenses</string>
    <string name="about_section_app_icon">App Icon</string>
    <string name="about_label_original_work">Original Work</string>
    <string name="about_label_author">Author</string>
    <string name="about_label_source">Source</string>
    <string name="about_label_source_link">Source Link</string>
    <string name="about_label_license">License</string>
    <string name="about_label_changes">Changes Made</string>
    
    <string name="about_value_icon_name">Free Wayfinding vector icons - Guidance icon set</string>
    <string name="about_value_icon_author">Streamline and Vincent le moign</string>
    <string name="about_value_source_url">https://www.figma.com/community/file/1227184301417272677/free-wayfinding-vector-icons-guidance-icon-set</string>
    
    <string name="about_value_license_cc_by">Creative Commons Attribution 4.0 International (CC BY 4.0)</string>
    <string name="about_value_license_cc_by_url">https://creativecommons.org/licenses/by/4.0/</string>
    <string name="about_value_change_desc">Modified colors and shapes of the icon.</string>
    <string name="about_notice_compliance">This app complies with CC BY 4.0 by providing proper attribution, source link, license link, and notice of changes.</string>
    
    <string name="toast_copied">Address copied.</string>
</resources>
```

### 4.2 번역 품질 검토 체크리스트

#### 네이티브 검수 의뢰 시 제공 자료
1. **스크린샷**: 각 화면 캡처 (한국어 버전)
2. **컨텍스트 설명**: 
   ```
   "quit_confirm_subtitle": 사용자가 금주를 중단하려 할 때 표시됩니다.
   격려하면서도 아쉬움을 표현하는 톤이어야 합니다.
   ```
3. **용어집**:
   | 한국어 | 영어 | 비고 |
   |--------|------|------|
   | 금주 | Sobriety / Abstinence | "Sobriety" 권장 (일반적) |
   | 레벨 | Level | 그대로 사용 |
   | 성공률 | Success Rate | - |
   | 절약한 금액 | Money Saved / Savings | "Money Saved" 명확 |

---

## 5. 레이아웃 검증 및 테스트

### 5.1 Pseudolocalization 테스트

#### 활성화 방법
1. 기기/에뮬레이터 **Settings** → **System** → **Languages & input**
2. **English (XA)** 선택 (Pseudolocale - accented)
   - 결과: "Home" → "[Ĥöṁé one two]"
   - 목적: 텍스트가 약 30% 길어졌을 때 레이아웃 확인

3. **Arabic (XB)** 선택 (Pseudolocale - bidi)
   - 결과: RTL(오른쪽→왼쪽) 레이아웃 테스트
   - *현재 Phase 1에서는 LTR만 지원하므로 참고용*

#### 주요 확인 사항
- [ ] 텍스트가 잘리지 않는가?
- [ ] 버튼이 겹치지 않는가?
- [ ] 최소 터치 영역 48dp 유지되는가?
- [ ] 여백이 적절한가?

### 5.2 실제 언어 테스트

#### 에뮬레이터 언어 변경
```bash
# ADB로 언어 변경
adb shell "setprop persist.sys.locale en-US; setprop ctl.restart zygote"

# 또는 기기에서: Settings → Language → English
```

#### 테스트 시나리오
1. **Run 화면**:
   - [ ] Stat Chips (목표일/Level/시간) 텍스트 길이 확인
   - [ ] Indicator 타이틀 (Days Sober 등) 줄바꿈 여부
   - [ ] "Tap to switch indicator" 힌트 표시 확인

2. **Quit 화면**:
   - [ ] "Are you sure you want to stop?" 다이얼로그 너비
   - [ ] 버튼 텍스트 (Continue/Stop) 크기

3. **Detail 화면**:
   - [ ] "Progress: X days" / "Goal: X days" 정렬
   - [ ] Delete 다이얼로그 메시지 길이

4. **All Records 화면**:
   - [ ] "No records yet" 빈 상태 메시지
   - [ ] 삭제 확인 다이얼로그

5. **About 화면**:
   - [ ] License 라벨/값 정렬
   - [ ] URL 복사 토스트

### 5.3 레이아웃 수정 예시

#### 문제: 긴 영어 텍스트로 인한 버튼 겹침
```kotlin
// Before
Row {
    Button(onClick = { /*...*/ }) {
        Text("계속")  // 2글자
    }
    Spacer(modifier = Modifier.width(16.dp))
    Button(onClick = { /*...*/ }) {
        Text("멈추기")  // 3글자
    }
}

// After (영어: "Continue" 8글자, "Stop" 4글자)
Row(modifier = Modifier.fillMaxWidth()) {
    Button(
        onClick = { /*...*/ },
        modifier = Modifier.weight(1f)  // 비율로 분할
    ) {
        Text(stringResource(R.string.cd_continue))
    }
    Spacer(modifier = Modifier.width(16.dp))
    Button(
        onClick = { /*...*/ },
        modifier = Modifier.weight(1f)
    ) {
        Text(stringResource(R.string.cd_stop))
    }
}
```

#### 문제: 제목 텍스트 잘림
```kotlin
// Before
Text(
    text = stringResource(R.string.indicator_title_saved_money),
    fontSize = 20.sp,
    maxLines = 1  // 잘림 위험
)

// After
Text(
    text = stringResource(R.string.indicator_title_saved_money),
    fontSize = 20.sp,
    maxLines = 2,  // 2줄 허용
    overflow = TextOverflow.Ellipsis,
    modifier = Modifier.fillMaxWidth()
)
```

### 5.4 자동화 테스트 (Screenshot Test)

#### Gradle 태스크 추가
```kotlin
// app/build.gradle.kts
android {
    // ...
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Screenshot testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
}
```

#### 스크린샷 테스트 작성
```kotlin
// app/src/androidTest/java/com/example/alcoholictimer/ScreenshotTest.kt
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testRunScreen_English() {
        // 로케일 설정
        val locale = Locale("en")
        Locale.setDefault(locale)
        val config = Configuration().apply {
            setLocale(locale)
        }
        
        composeTestRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides config) {
                // RunScreen 렌더링
            }
        }
        
        // 스크린샷 캡처 (수동 확인)
        composeTestRule.onRoot().printToLog("RunScreen_EN")
    }
}
```

---

## 6. Play Store 메타데이터

### 6.1 짧은 설명 (Short Description)

| 언어 | 짧은 설명 (80자 제한) |
|------|----------------------|
| 한국어 | 금주 기록과 성공률을 간단하게 추적하세요. |
| English | Track your sobriety journey with simple records and success rates. |

### 6.2 전체 설명 (Full Description)

#### 영어 버전
```
AlcoholicTimer is a simple and focused sobriety tracker.

Key Features:
• Track daily sobriety/abstinence records
• View weekly, monthly, yearly, and lifetime statistics (success rate, goal progress)
• Monitor your longest streak and average duration
• 100% offline: All data stored locally
• No personal data collection (no ads or tracking)

Upcoming Features:
• Crashlytics-based stability monitoring
• Additional statistics and multi-language support (English, Japanese, etc.)
• Accessibility improvements (contrast, screen reader labels)

Privacy Policy Summary:
This app does not collect or transmit personal information such as email, location, or advertising ID. All records are stored only on your device.

Contact/Feedback: your-contact@example.com

Keywords: sobriety, quit drinking, habit tracker, abstinence, sober counter, recovery, health
```

### 6.3 스크린샷 캡처

#### 준비 작업
1. **기기 설정**:
   - 화면 해상도: 1080x1920 (권장) 또는 1440x2560
   - 언어: 영어
   - 시간: 10:00 (배터리/신호 아이콘 정리)

2. **데모 데이터 준비**:
   - 진행 중인 금주 기록 (50일차, 70% 진행률)
   - 완료된 기록 3개
   - 주간 통계 데이터

#### 캡처 대상 (최소 5장)
1. **Run 화면**: 진행 중인 타이머 (메인 indicator visible)
2. **Records 화면**: 주간/월간 통계 표시
3. **Detail 화면**: 개별 기록 상세
4. **All Records 화면**: 전체 기록 목록
5. **About 화면**: 라이선스 정보 (옵션)

#### 캡처 방법
```bash
# ADB로 스크린샷
adb shell screencap -p /sdcard/screenshot_run_en.png
adb pull /sdcard/screenshot_run_en.png screenshots/

# 또는 Android Studio Device File Explorer 사용
```

#### 편집 (옵션)
- **캡션 추가**: "Track your progress in real-time"
- **배경 제거**: 기기 프레임만 남기기 (Figma/Photoshop)
- **Device Art Generator**: https://developer.android.com/distribute/marketing-tools/device-art-generator

### 6.4 Release Notes (최근 변경사항)

#### 버전 1.2.0 (다국어 지원)
```
한국어:
• 영어 및 일본어 지원 추가
• 날짜/시간 표시 현지화
• 통화 단위 자동 변환 (USD, JPY 등)
• 레이아웃 안정성 개선

English:
• Added English and Japanese language support
• Localized date/time formats
• Automatic currency conversion (USD, JPY, etc.)
• Improved layout stability
```

---

## 7. QA 및 출시

### 7.1 QA 체크리스트

#### 기능 테스트 (언어별)
| 기능 | 한국어 | 영어 | 일본어 | 비고 |
|------|--------|------|--------|------|
| 금주 시작 | [ ] | [ ] | [ ] | 타이머 정확도 |
| 금주 종료 | [ ] | [ ] | [ ] | 다이얼로그 메시지 |
| 기록 추가 | [ ] | [ ] | [ ] | 날짜 형식 |
| 통계 조회 | [ ] | [ ] | [ ] | 숫자/퍼센트 |
| 설정 변경 | [ ] | [ ] | [ ] | 목표 일수 |
| 삭제 확인 | [ ] | [ ] | [ ] | 경고 메시지 |

#### 레이아웃 테스트
| 화면 | 한국어 | 영어 (긴 텍스트) | 일본어 | 비고 |
|------|--------|------------------|--------|------|
| Run | [ ] | [ ] | [ ] | Indicator 전환 |
| Quit | [ ] | [ ] | [ ] | 버튼 간격 |
| Detail | [ ] | [ ] | [ ] | 날짜 표시 |
| Records | [ ] | [ ] | [ ] | 빈 상태 |
| About | [ ] | [ ] | [ ] | URL 복사 |

#### 접근성 테스트
- [ ] **TalkBack** (한국어/영어): 모든 버튼/텍스트 읽기 확인
- [ ] **큰 글씨 설정**: 텍스트 스케일 200%에서 레이아웃 확인
- [ ] **다크 모드**: 각 언어에서 색상 대비 확인

### 7.2 Lint 검사

```bash
# 번역 누락 검사
./gradlew lintDebug

# 결과 확인
start app\build\reports\lint-results-debug.html
```

주요 경고:
- **MissingTranslation**: `values-en`에 없는 문자열
- **UnusedResources**: 사용하지 않는 문자열 (삭제 고려)
- **TypographyEllipsis**: `...` 대신 `&#8230;` 사용 권장

### 7.3 출시 전 최종 점검

#### 버전 정보 업데이트
```kotlin
// app/build.gradle.kts
val releaseVersionCode = 2025110100  // yyyymmdd + 00
val releaseVersionName = "1.2.0"  // 다국어 지원
```

#### CHANGELOG.md 업데이트
```markdown
## [1.2.0] - 2025-11-01

### Added
- 🌐 다국어 지원: 영어 (English), 일본어 (日本語)
- 🗓️ 로케일 기반 날짜/시간 형식 자동 변환
- 💰 통화 단위 현지화 (KRW, USD, JPY)

### Changed
- 모든 하드코딩 문자열을 리소스로 이동
- 레이아웃 유연성 개선 (긴 텍스트 대응)

### Fixed
- 일부 화면에서 텍스트 잘림 현상 수정
```

#### Play Console 업로드
1. **AAB 빌드**:
   ```bash
   # 환경변수 설정 (keystore)
   set KEYSTORE_PATH=G:\keystore\alcoholictimer-release.jks
   set KEYSTORE_STORE_PW=your_password
   set KEY_ALIAS=alcoholictimer
   set KEY_PASSWORD=your_key_password
   
   # Release 빌드
   ./gradlew bundleRelease
   ```

2. **Play Console**:
   - **릴리스** → **프로덕션** → **새 릴리스 만들기**
   - AAB 업로드: `app\build\outputs\bundle\release\app-release.aab`
   - **출시 노트** 입력 (한국어, 영어, 일본어)
   - **저장** → **검토** → **출시**

3. **단계적 출시** (권장):
   - 초기: 10% 사용자
   - 24시간 모니터링 (크래시/리뷰)
   - 문제 없으면: 50% → 100%

### 7.4 출시 후 모니터링

#### 첫 48시간 체크
- [ ] **Firebase Crashlytics**: 새 크래시 보고 확인
- [ ] **Play Console**: 언어별 평균 평점
- [ ] **리뷰 모니터링**: "translation", "language" 키워드 검색
- [ ] **ANR (App Not Responding)**: 언어별 발생률

#### 주간 리뷰
- [ ] 국가별 다운로드 분포 (영어권/일본 증가 확인)
- [ ] 번역 오류 피드백 수집 → 다음 패치에 반영
- [ ] 통계: 언어별 DAU/MAU

---

## 8. 트러블슈팅

### 8.1 자주 발생하는 문제

#### 문제 1: 일부 문자열만 영어로 표시됨
**원인**: `strings.xml`에서 누락  
**해결**:
```bash
# 누락된 키 찾기
./gradlew lintDebug
# → MissingTranslation 경고 확인
```

#### 문제 2: 날짜 형식이 여전히 한국어
**원인**: 하드코딩된 `SimpleDateFormat` 사용  
**해결**:
```kotlin
// Bad
SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())

// Good
LocaleFormatUtils.getDateFormat()
```

#### 문제 3: 레이아웃 깨짐 (영어에서만)
**원인**: 고정 너비 사용  
**해결**:
```kotlin
// Bad
Text(
    text = "Goal Achievement Rate",
    modifier = Modifier.width(100.dp)  // 잘림!
)

// Good
Text(
    text = stringResource(R.string.detail_progress_rate),
    modifier = Modifier.fillMaxWidth(),
    maxLines = 2,
    overflow = TextOverflow.Ellipsis
)
```

#### 문제 4: Plurals가 적용되지 않음
**원인**: `getString` 대신 `getQuantityString` 사용 필요  
**해결**:
```kotlin
// Bad
stringResource(R.string.days_count, count)

// Good
LocalContext.current.resources.getQuantityString(
    R.plurals.days_count,
    count,
    count
)
```

### 8.2 긴급 롤백 절차

#### Play Console에서 이전 버전 활성화
1. **릴리스** → **프로덕션** 
2. 이전 버전 (1.1.0) 선택
3. **이 릴리스로 롤백**
4. 사유 입력: "다국어 관련 크래시 발생, 수정 후 재출시 예정"

#### 핫픽스 준비
```bash
# 긴급 브랜치 생성
git checkout -b hotfix/i18n-crash-fix v1.1.0

# 문제 수정 후
./gradlew bundleRelease
# 버전: 1.2.1 (패치 버전 증가)
```

---

## 9. 다음 단계 (Phase 2 준비)

### 9.1 일본어 준비 (Week 5-6)
- [ ] 전문 번역사 의뢰 (Gengo/Upwork)
- [ ] Noto Sans JP 폰트 테스트
- [ ] 일본 앱 스토어 메타데이터 작성

### 9.2 자동화 도구 도입 (Phase 2)
- [ ] **Crowdin** 연동: GitHub PR 자동 생성
- [ ] **번역 메모리** 구축: 기존 번역 재사용
- [ ] **CI/CD 파이프라인**: 번역 완료율 체크

### 9.3 커뮤니티 피드백
- [ ] Reddit/Facebook 그룹에서 베타 테스터 모집
- [ ] 번역 오류 제보 양식 (Google Forms)
- [ ] 우수 기여자 보상 제도

---

## 부록

### A. 유용한 명령어 모음

```bash
# 언어 리소스 디렉토리 생성
mkdir app\src\main\res\values-en
mkdir app\src\main\res\values-ja
mkdir app\src\main\res\values-zh-rCN

# 문자열 개수 확인
findstr /c:"<string name=" app\src\main\res\values\strings.xml | find /c /v ""

# APK 크기 확인 (언어 추가 후)
dir app\build\outputs\apk\release\*.apk

# Lint 특정 이슈만 체크
./gradlew lintDebug -Dlint.check=MissingTranslation
```

### B. 추천 도구
- **Android Studio Translations Editor**: 내장 번역 UI
- **DeepL**: 고품질 기계 번역 (영어/일본어)
- **Crowdin**: 크라우드소싱 번역 플랫폼
- **Pseudolocales**: 레이아웃 테스트
- **Device Art Generator**: 스크린샷 프레임 추가

---

**문서 버전**: 1.0  
**마지막 업데이트**: 2025-10-27  
**작성자**: AlcoholicTimer 개발팀

