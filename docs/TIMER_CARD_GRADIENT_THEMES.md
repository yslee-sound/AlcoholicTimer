# 🎨 타이머 카드 그라데이션 색상 테마 적용 완료

**작성일:** 2026-01-05  
**작업자:** 안드로이드 Compose UI 디자인 전문 개발자  
**목적:** HorizontalPager의 타이머 카드에 페이지별 서로 다른 그라데이션 적용

---

## 🎯 구현 목표

✅ **완료된 항목:**
1. ✅ 페이지 인덱스에 따라 서로 다른 그라데이션 적용
2. ✅ 3가지 색상 테마 구현 (Pink/Orange, Blue/Mint, Purple/Indigo)
3. ✅ 배경 이미지를 그라데이션으로 대체하여 시각적 구분 강화

---

## 🎨 적용된 색상 테마

### 카드 0: Soft Coral & Rose Pink (금주 타이머)
```kotlin
Brush.linearGradient(
    colors = listOf(
        Color(0xFFF48FB1), // Soft Pink (부드러운 핑크)
        Color(0xFFE57373)  // Coral Rose (코랄 로즈)
    )
)
```
**느낌:** 편안함, 따뜻함, 부드러운 에너지  
**연상:** 봄의 벚꽃, 따뜻한 햇살, 아늑한 분위기  
**변경 이유:** 
- 기존 Hot Pink/Magenta(0xFFEC4899, 0xFFDB2777)가 너무 강렬함
- 더 편안하고 부드러운 톤으로 변경하여 눈의 피로 감소
- 여전히 충분히 선명하여 흰색 텍스트가 잘 보임

---

### 카드 1: Deep Blue & Royal Blue (금연 타이머)
```kotlin
Brush.linearGradient(
    colors = listOf(
        Color(0xFF3B82F6), // Deep Blue (진한 파란색)
        Color(0xFF1D4ED8)  // Royal Blue (로열 블루)
    )
)
```
**느낌:** 깊이감, 신뢰, 깨끗한 하늘  
**연상:** 깊은 바다, 맑은 하늘, 청정 공기  
**변경 이유:** 기존 색상(0xFF4FACFE, 0xFF00F2FE)이 너무 연해서 우측 하단이 잘 보이지 않는 문제 해결

---

### 카드 2: Purple & Indigo (습관 타이머)
```kotlin
Brush.linearGradient(
    colors = listOf(
        Color(0xFF667EEA), // Purple
        Color(0xFF764BA2)  // Deep Violet
    )
)
```
**느낌:** 차분함, 집중, 신비로움  
**연상:** 저녁 하늘, 밤, 명상

---

## 📝 구현 내용

### 1️⃣ 그라데이션 생성 함수 추가

**파일:** `RunScreen.kt` (파일 끝)

```kotlin
/**
 * [NEW] 타이머 카드별 그라데이션 생성 함수 (2026-01-05)
 * [UPDATED] 첫 번째 카드 색상을 더 진하고 선명하게 변경 (2026-01-05)
 * [UPDATED] 두 번째 카드도 진하고 선명한 블루로 변경 (2026-01-05)
 * [UPDATED] 첫 번째 카드를 편안한 코랄/로즈 핑크로 변경 (2026-01-05)
 * 
 * @param page 페이지 인덱스 (0, 1, 2)
 * @return 페이지별 그라데이션 Brush
 */
private fun getCardGradient(page: Int): Brush {
    return when (page) {
        0 -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFF48FB1), // Soft Pink (부드러운 핑크)
                Color(0xFFE57373)  // Coral Rose (코랄 로즈)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
        1 -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF3B82F6), // Deep Blue (진한 파란색)
                Color(0xFF1D4ED8)  // Royal Blue (로열 블루)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
        else -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF667EEA), // Purple
                Color(0xFF764BA2)  // Deep Violet
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }
}
```

**특징:**
- `page` 인덱스를 받아서 해당하는 그라데이션 반환
- `linearGradient`의 `start`와 `end`를 대각선 방향으로 설정
- `else` 케이스로 3번째 이상의 카드도 자동 처리 (Purple 테마)

---

### 2️⃣ ExistingTimerCard에 backgroundBrush 파라미터 추가

**변경 전:**
```kotlin
@Composable
private fun ExistingTimerCard(
    timerData: TimerData,
    // ...other params...
    modifier: Modifier = Modifier
) {
    // 배경 이미지 사용
    Image(
        painter = painterResource(id = R.drawable.bg9),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}
```

**변경 후:**
```kotlin
@Composable
private fun ExistingTimerCard(
    timerData: TimerData,
    // ...other params...
    backgroundBrush: Brush, // [NEW] 그라데이션 배경
    modifier: Modifier = Modifier
) {
    // 그라데이션 배경 적용
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
    )
}
```

**주요 변경사항:**
- ✅ `backgroundBrush: Brush` 파라미터 추가
- ✅ `Image` (배경 이미지) 제거
- ✅ `Box` + `background(brush = ...)` 사용

---

### 3️⃣ HorizontalPager에서 그라데이션 전달

**변경 전:**
```kotlin
HorizontalPager(...) { page ->
    if (page < timers.size) {
        ExistingTimerCard(
            timerData = timers[page],
            // ...other params...
        )
    }
}
```

**변경 후:**
```kotlin
HorizontalPager(...) { page ->
    if (page < timers.size) {
        // [NEW] 페이지별 그라데이션 생성
        val cardGradient = getCardGradient(page)
        
        ExistingTimerCard(
            timerData = timers[page],
            // ...other params...
            backgroundBrush = cardGradient // [NEW] 그라데이션 전달
        )
    }
}
```

**주요 변경사항:**
- ✅ `getCardGradient(page)` 호출하여 현재 페이지 그라데이션 생성
- ✅ `backgroundBrush` 파라미터로 전달

---

## 📊 시각적 효과

### Before (변경 전)
```
[타이머 1] [타이머 2] [타이머 3]
   🌸        🌸        🌸
(모두 동일한 배경 이미지)
```

### After (변경 후)
```
[타이머 1] [타이머 2] [타이머 3]
   🌸        🌊        🌌
  Pink     Blue     Purple
  Orange   Mint     Indigo
```

**개선 효과:**
- ✅ **시각적 구분 명확:** 각 타이머를 색상으로 즉시 구분 가능
- ✅ **사용자 경험 향상:** 카드를 스와이프할 때 색상 변화로 페이지 전환 인식
- ✅ **테마별 정체성:** 금주/금연/습관 등 타이머 종류를 색상으로 표현

---

## 🎨 그라데이션 디자인 원칙

### 1. 대각선 방향
```kotlin
start = Offset(0f, 0f),                    // 좌상단
end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY) // 우하단
```
- 좌상단에서 우하단으로 자연스러운 흐름
- 카드에 입체감 부여

### 2. 2색 조합
- 너무 많은 색상은 복잡함
- 2색만 사용하여 깔끔하고 세련된 느낌

### 3. 색상 선택 기준
| 테마 | 주색상 | 보조색상 | 용도 |
|------|--------|----------|------|
| Pink/Orange | 부드러움 | 따뜻함 | 금주 (건강, 활력) |
| Blue/Mint | 맑음 | 청량함 | 금연 (깨끗한 공기) |
| Purple/Indigo | 차분함 | 집중 | 습관 (자기 계발) |

---

## 🔧 기술적 세부사항

### Brush 타입
```kotlin
Brush.linearGradient(
    colors: List<Color>,   // 그라데이션 색상 리스트
    start: Offset,         // 시작 위치
    end: Offset            // 끝 위치
)
```

### Float.POSITIVE_INFINITY 사용 이유
- 카드 크기에 상관없이 대각선 방향 보장
- 고정된 픽셀 값 대신 무한대를 사용하여 유연성 확보

### Modifier.background(brush)
```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(brush = backgroundBrush) // Brush 사용
)
```
- `background(color = ...)` 대신 `background(brush = ...)` 사용
- 단색이 아닌 그라데이션 적용 가능

---

## ✅ 빌드 결과

**상태:** 성공 🎉  
**소요 시간:** 3초  
**경고:** 없음 (기존 경고만 존재)

```
BUILD SUCCESSFUL in 3s
43 actionable tasks: 5 executed, 7 from cache, 31 up-to-date
```

---

## 🧪 테스트 시나리오

### 1. 카드 0 (금주): Soft Coral & Rose Pink
```
[시작 화면에서 확인]
- 좌상단: Soft Pink (0xFFF48FB1)
- 우하단: Coral Rose (0xFFE57373)
- 느낌: 편안하고 따뜻한 핑크
```

### 2. 카드 1 (금연): Deep Blue & Royal Blue
```
[오른쪽으로 스와이프]
- 좌상단: Deep Blue (0xFF3B82F6)
- 우하단: Royal Blue (0xFF1D4ED8)
- 느낌: 깊고 신뢰감 있는 블루
```

### 3. 카드 2 (습관): Purple & Indigo
```
[한 번 더 오른쪽으로 스와이프]
- 좌상단: 퍼플 (0xFF667EEA)
- 우하단: 딥 바이올렛 (0xFF764BA2)
- 느낌: 차분하고 집중된 느낌
```

---

## 🎯 향후 개선 가능 사항

### 1. 애니메이션 추가 ⏳
```kotlin
// 페이지 전환 시 색상이 부드럽게 전환
val animatedBrush by animateBrushAsState(
    targetValue = getCardGradient(page)
)
```

### 2. 커스텀 색상 선택 ⏳
```kotlin
// 사용자가 직접 타이머 색상 선택 가능
data class TimerData(
    // ...existing fields...
    val customGradient: Brush? = null
)
```

### 3. 다크 모드 대응 ⏳
```kotlin
// 다크 모드일 때 어두운 톤의 그라데이션
fun getCardGradient(page: Int, isDarkMode: Boolean): Brush {
    if (isDarkMode) {
        // 어두운 버전의 그라데이션
    } else {
        // 밝은 버전의 그라데이션
    }
}
```

---

## 📚 참고 자료

### Compose Gradient 공식 문서
- [Brush.linearGradient](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/Brush#linearGradient(kotlin.collections.List,androidx.compose.ui.geometry.Offset,androidx.compose.ui.geometry.Offset,androidx.compose.ui.graphics.TileMode))
- [Modifier.background](https://developer.android.com/reference/kotlin/androidx/compose/foundation/package-summary#(androidx.compose.ui.Modifier).background(androidx.compose.ui.graphics.Brush,androidx.compose.ui.graphics.Shape,kotlin.Float))

### 색상 선택 참고
- [Gradient Hunt](https://gradienthunt.com/) - 인기 그라데이션 모음
- [uiGradients](https://uigradients.com/) - 아름다운 그라데이션 컬렉션

---

## 📝 수정된 파일

**파일:** `app/src/main/java/kr/sweetapps/alcoholictimer/ui/tab_01/screens/RunScreen.kt`

**변경 사항:**
1. ✅ `getCardGradient(page: Int): Brush` 함수 추가
2. ✅ `ExistingTimerCard`에 `backgroundBrush` 파라미터 추가
3. ✅ 배경 이미지 제거, 그라데이션으로 대체
4. ✅ `HorizontalPager`에서 페이지별 그라데이션 생성 및 전달

---

**구현 완료일:** 2026-01-05  
**빌드 상태:** ✅ 성공  
**다음 단계:** 실제 기기에서 시각적 효과 확인 및 사용자 피드백 수집

