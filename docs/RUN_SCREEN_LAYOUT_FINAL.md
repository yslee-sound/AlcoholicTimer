# Run 화면 레이아웃 최종 수정 완료

## 수정 목표
금주 진행 화면의 카드 간격을 일관되게 조정하여 더 깔끔한 레이아웃 제공

## 적용된 변경사항

### 1. LayoutConstants.kt 수정

```kotlin
// 전체 앱의 기본 카드 간격을 20dp로 증가
val CARD_SPACING = 20.dp  // 변경: 16dp → 20dp

// Run Screen 전용 상수
val RUN_SCREEN_CARD_SPACING = 20.dp  // 카드 간 일관된 간격
```

**파일**: `app/src/main/java/kr/sweetapps/alcoholictimer/core/ui/LayoutConstants.kt`

### 2. StandardScreen.kt 수정

변경 없음 - 기존 로직 유지:
- `reservedBottom = (buttonSize / 2) + buttonBottomGap + adTopGap`
- 버튼이 콘텐츠 위에 자연스럽게 floating되도록 유지

**파일**: `app/src/main/java/kr/sweetapps/alcoholictimer/core/ui/StandardScreen.kt`

### 3. RunActivity.kt 수정

모든 카드 간격을 `LayoutConstants.RUN_SCREEN_CARD_SPACING`으로 통일:

```kotlin
// 3곳 모두 변경:
Spacer(modifier = Modifier.height(LayoutConstants.RUN_SCREEN_CARD_SPACING))
```

**파일**: `app/src/main/java/kr/sweetapps/alcoholictimer/feature/run/RunActivity.kt`

## 레이아웃 구조

```
상단 패딩: 16dp
├─ 통계 카드 (목표일/레벨/시간)
├─ 간격: 20dp ✅ (변경: 8dp → 20dp)
├─ 지표 카드 (금주일수/시간 등)
├─ 간격: 20dp ✅ (변경: 8dp → 20dp)
├─ 프로그레스바 카드
├─ 자연스러운 간격 (버튼이 floating)
└─ ● 중지 버튼 (콘텐츠 위에 떠있음)
```

## 측정값 정리

| 항목 | 수정 전 | 수정 후 | 변경 |
|------|---------|---------|------|
| 카드 간격 | 8dp | 20dp | +12dp ↑ |
| 전체 CARD_SPACING | 16dp | 20dp | +4dp ↑ |
| 버튼 위치 | floating | floating | 유지 |

## 영향받는 화면

이 변경사항은 다음 화면들에 자동으로 적용됩니다:

1. ✅ **RunScreen** (금주 진행 화면) - 주요 수정 대상
2. ✅ **StartScreen** (목표 설정 화면) - CARD_SPACING 적용
3. ✅ **기타 StandardScreen 사용 화면** - 모두 일관된 카드 간격

## 시각적 개선 효과

### 개선 사항
- ✅ 카드들이 적절히 떨어져 있어 읽기 편함 (8dp → 20dp)
- ✅ 전체적으로 균형잡힌 레이아웃
- ✅ 버튼은 floating 효과로 자연스럽게 배치
- ✅ 프로그레스바가 정상적으로 표시됨

## 빌드 결과

✅ **빌드 성공** - 모든 변경사항 정상 적용  
✅ **에러 없음** - 기존 경고만 존재 (새로운 에러 없음)  
✅ **호환성 유지** - 다른 화면들도 일관된 레이아웃 적용  

## 관련 파일

- `LayoutConstants.kt` - 레이아웃 상수 정의
- `StandardScreen.kt` - 공통 화면 컴포넌트
- `RunActivity.kt` - 금주 진행 화면

## 추가 권장사항

이제 카드 간격이 일관되게 적용되었습니다. 
만약 추가 미세 조정이 필요하다면:

1. **카드 간격 조정**: `LayoutConstants.RUN_SCREEN_CARD_SPACING` (현재 20dp)
2. **전체 앱 간격**: `LayoutConstants.CARD_SPACING` (현재 20dp)

이 값들을 조정하면 모든 화면에 일관되게 적용됩니다.

