# 🎨 타이머 카드 그라데이션 색상 테마 (색채 심리학 기반)

**작성일:** 2026-01-05  
**최종 업데이트:** 2026-01-05 (색채 심리학 기반 재설계)  
**작업자:** 안드로이드 Compose UI 디자인 전문 개발자  
**목적:** 각 중독의 특성을 색채 심리학적으로 연결한 직관적 색상 배정

---

## 🎯 구현 목표

✅ **완료된 항목:**
1. ✅ 페이지 인덱스에 따라 서로 다른 그라데이션 적용
2. ✅ 색채 심리학 기반으로 각 중독 유형과 색상 매칭
3. ✅ 배경 이미지를 그라데이션으로 대체하여 시각적 구분 강화
4. ✅ 눈에 편안하고 세련된 색상 조합 구현

---

## 🎨 색채 심리학 기반 색상 테마

### 카드 0: 🌊 깊은 블루 (금주 - Alcohol)
```kotlin
Brush.linearGradient(
    colors = listOf(
        Color(0xFF3B82F6), // Deep Blue (딥 블루)
        Color(0xFF1D4ED8)  // Royal Blue (로열 블루)
    )
)
```
**색채 심리학적 의미:**
- 🧠 **맑고 깨끗한 정신:** 술에 취한 혼탁함에서 벗어남
- 💧 **정화와 투명성:** 물처럼 깨끗한 상태
- 🌊 **차분함과 자제력:** 충동 억제, 이성적 판단

**연상 이미지:**
- 맑은 물, 깨끗한 하늘
- 술이 없는 맑은 아침
- 투명한 정신 상태

**선택 이유:**
- 이전 2번 카드에서 검증된 진하고 선명한 블루
- 흰색 텍스트와의 가독성이 우수함

---

### 카드 1: 🌿 치유의 그린 (금연 - Smoking)
```kotlin
Brush.linearGradient(
    colors = listOf(
        Color(0xFF10B981), // Emerald Green (에메랄드 그린)
        Color(0xFF14B8A6)  // Teal (틸)
    )
)
```
**색채 심리학적 의미:**
- 🫁 **폐의 정화:** 담배 연기로부터 회복
- 🌱 **재생과 치유:** 건강한 호흡의 회복
- 💚 **생명력과 건강:** 자연의 치유력

**연상 이미지:**
- 싱그러운 숲속의 맑은 공기
- 녹색 식물의 광합성
- 건강한 숨, 깨끗한 폐

---

### 카드 2: 🔮 신비로운 퍼플 (사용자 정의 - Custom)
```kotlin
Brush.linearGradient(
    colors = listOf(
        Color(0xFF8B5CF6), // Vivid Purple (생생한 퍼플)
        Color(0xFF7C3AED)  // Deep Purple (딥 퍼플)
    )
)
```
**색채 심리학적 의미:**
- 🎯 **다양한 목표:** 도파민, 카페인, 게임, SNS 등
- ✨ **고급스러움:** 중립적이면서도 세련된 느낌
- 🧘 **집중과 명상:** 자기 계발, 습관 형성

**연상 이미지:**
- 신비로운 우주
- 명상하는 저녁
- 목표를 향한 집중

---

## 📝 구현 내용

### 1️⃣ 그라데이션 생성 함수 (색채 심리학 기반)

**파일:** `RunScreen.kt` (파일 끝)

```kotlin
/**
 * [NEW] 타이머 카드별 그라데이션 생성 함수 (2026-01-05)
 * [UPDATED] 색채 심리학 기반으로 재설계 (2026-01-05)
 * 
 * @param page 페이지 인덱스 (0, 1, 2)
 * @return 페이지별 그라데이션 Brush
 * 
 * 색상 테마 (색채 심리학 기반):
 * - Card 0 (금주): Clear Blue (맑고 깨끗한 정신)
 * - Card 1 (금연): Healing Green (폐의 정화, 건강한 숨)
 * - Card 2 (커스텀): Mystic Purple (다양한 목표, 고급스러움)
 */
private fun getCardGradient(page: Int): Brush {
    return when (page) {
        0 -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF3B82F6), // Royal Blue (로열 블루)
                Color(0xFF06B6D4)  // Cyan (시안)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
        1 -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF10B981), // Emerald Green (에메랄드 그린)
                Color(0xFF14B8A6)  // Teal (틸)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
        else -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF8B5CF6), // Vivid Purple (생생한 퍼플)
                Color(0xFF7C3AED)  // Deep Purple (딥 퍼플)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }
}
```

**특징:**
- ✅ 각 중독 유형의 특성을 색상으로 직관적으로 표현
- ✅ 금주 → 블루 (맑은 정신), 금연 → 그린 (폐 정화), 커스텀 → 퍼플 (다양성)
- ✅ 눈에 편안하고 세련된 색상 조합

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

### After (색채 심리학 기반 재설계)
```
[금주 타이머]  [금연 타이머]  [커스텀 타이머]
     🌊            🌿            🔮
   Clear         Healing       Mystic
    Blue          Green         Purple
```

**개선 효과:**
- ✅ **직관적 색상 매칭:** 색상만 봐도 어떤 타이머인지 즉시 인식
- ✅ **심리적 연결:** 금주=맑음(블루), 금연=치유(그린), 커스텀=다양성(퍼플)
- ✅ **세련된 디자인:** 눈에 편안하면서도 고급스러운 느낌

---

## 🎨 색채 심리학 원리

### 1. 금주 (Alcohol) → 블루 (Blue)
**심리학적 효과:**
- 💧 **맑은 물:** 술의 혼탁함 vs 물의 투명함
- 🧠 **이성과 판단력:** 차분한 사고, 자제력
- 🌊 **정화:** 깨끗함, 클린한 상태

### 2. 금연 (Smoking) → 그린 (Green)
**심리학적 효과:**
- 🫁 **폐의 회복:** 담배 연기 → 맑은 공기
- 🌱 **생명력:** 자연, 재생, 건강
- 💚 **치유:** 녹색은 가장 강력한 치유의 색

### 3. 사용자 정의 → 퍼플 (Purple)
**심리학적 효과:**
- 🔮 **신비와 다양성:** 여러 목표를 포용
- ✨ **고급스러움:** 특별함, 개성
- 🧘 **집중:** 명상, 자기 계발

---

## 🧪 테스트 시나리오

### 1. 카드 0 (금주): 🌊 Clear Blue
```
[시작 화면에서 확인]
- 좌상단: Royal Blue (0xFF3B82F6)
- 우하단: Cyan (0xFF06B6D4)
- 느낌: 맑고 깨끗한 블루, 정화된 느낌
- 직관성: "술이 없는 맑은 정신" 즉시 연상
```

### 2. 카드 1 (금연): 🌿 Healing Green
```
[오른쪽으로 스와이프]
- 좌상단: Emerald Green (0xFF10B981)
- 우하단: Teal (0xFF14B8A6)
- 느낌: 신선하고 치유적인 그린
- 직관성: "폐 정화, 건강한 숨" 즉시 연상
```

### 3. 카드 2 (커스텀): 🔮 Mystic Purple
```
[한 번 더 오른쪽으로 스와이프]
- 좌상단: Vivid Purple (0xFF8B5CF6)
- 우하단: Deep Purple (0xFF7C3AED)
- 느낌: 신비롭고 고급스러운 퍼플
- 직관성: "다양한 목표, 자유로운 선택" 연상
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

