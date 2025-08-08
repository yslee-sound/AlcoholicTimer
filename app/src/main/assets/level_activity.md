# 금주 레벨 화면 상세 기획서

본 기획서는 사용자의 금주 성공 기록을 기반으로, 금주 레벨을 시각적으로 표시하는 화면을 설계합니다.
Jetpack Compose 기반으로 구현하며, 미니멀하고 직관적인 UX를 제공합니다.

---

## 1. 금주 레벨 정의

| 레벨명           | 기간 (연속 금주 일수) | 색상                 | 표시 방법           |
|-----------------|---------------------|---------------------|-------------------|
| 작심 7일         | 0~6일               | Gray (#BDBDBD)      | 0~6일             |
| 의지의 2주        | 7~13일              | Yellow (#FFEB3B)    | 7~13일            |
| 한달의 기적       | 14~29일             | Orange (#FF9800)    | 14~29일           |
| 습관의 탄생       | 30~59일             | Green (#4CAF50)     | 30~59일           |
| 계속되는 도전     | 60~119일            | Blue (#2196F3)      | 60~119일          |
| 거의 1년         | 120~239일           | Purple (#9C27B0)    | 120~239일         |
| 금주 마스터       | 240~364일           | Black (#212121)     | 240~364일         |
| 절제의 레전드     | 365일 이상          | Gold (#FFD700)      | 1년 이상          |

**특별 표시 규칙:**
- "절제의 레전드" 레벨의 날짜는 "1년 이상"으로 표시
- 각 레벨은 해당 색상의 카드로 표현
- 달성한 레벨은 100% 투명도, 미달성 레벨은 20% 투명도(연하게 표시)

---

## 2. 화면 구성

### (1) 상단바 (BaseActivity 공통)
- **햄버거 메뉴**: 좌측 상단 (기존 네비게이션 패턴 유지)
- **화면 제목**: "금주 레벨" (중앙 정렬)
- **배경색**: 흰색 (#FFFFFF)
- **구분선**: 하단에 연한 회색 구분선

### (2) 현재 진행 영역 (상단 1/3)
- **배경색**: 흰색 (#FFFFFF)
- **레이아웃**: 중앙 정렬 세로 배치
- **구성 요소**:
  ```
  [현재 레벨명]
  24sp, Bold, 검은색
  
  [진행도 바]
  - 가로 70% 너비, 높이 10dp
  - 현재 레벨 색상으로 표시
  - 둥근 모서리
  
  [남은 일수 텍스트]
  16sp, 검은색
  "다음 레벨까지 X일 남음" 또는 "최고 레벨입니다!"
  ```

### (3) 구분선
- **두께**: 2dp
- **색상**: 연한 회색 (#E0E0E0)
- **위치**: 현재 진행 영역과 레벨 리스트 사이

### (4) 전체 레벨 리스트 (하단 2/3)
- **스크롤**: 세로 스크롤 가능
- **패딩**: 상단 16dp 여백
- **카드 간격**: 12dp

#### 레벨 카드 디자인
```
┌─────────────────────────────────┐
│ [레벨명]                        │ ← 18sp, 현재 레벨이면 Bold
│ [기간]                          │ ← 14sp, 회색
└─────────────────────────────────┘
```

**카드 속성**:
- **모서리**: 16dp 둥근 모서리
- **패딩**: 내부 16dp, 좌우 여백 16dp
- **배경색**: 각 레벨의 지정 색상
- **투명도**: 달성 레벨 100%, 미달성 레벨 20%
- **텍스트 색상**: 달성 시 검은색, 미달성 시 회색

---

## 3. 데이터 연동 규칙

### (1) 현재 일수 계산
```kotlin
val currentDays = 15 // 예시값, 실제로는 SharedPreferences에서 계산
```

### (2) 현재 레벨 판정
```kotlin
val currentLevelIndex = levels.indexOfFirst { currentDays in it.start..it.end }.coerceAtLeast(0)
```

### (3) 진행도 계산
```kotlin
val progress = when {
    currentDays < currentLevel.start -> 0f
    currentDays > currentLevel.end -> 1f
    else -> (currentDays - currentLevel.start + 1).toFloat() / (currentLevel.end - currentLevel.start + 1)
}
```

### (4) 다음 레벨까지 일수
```kotlin
val daysToNext = if (nextLevel != null) nextLevel.start - currentDays else 0
```

---

## 4. UI 상태 및 동작

### (1) 달성 상태 표시
- **달성된 레벨**: 원래 색상으로 선명하게 표시
- **미달성 레벨**: 20% 투명도로 연하게 표시
- **현재 레벨**: Bold 텍스트로 강조

### (2) 특별 케이스
- **최고 레벨 달성**: "최고 레벨입니다!" 메시지 표시
- **절제의 레전드**: 날짜를 "1년 이상"으로 표시

### (3) 반응형 디자인
- **세로 스크롤**: 레벨이 많을 때 스크롤 가능
- **카드 레이아웃**: 화면 너비에 맞춰 자동 조정
- **패딩**: 좌우 16dp 일관된 여백

---

## 5. 구현 시 주의사항

### (1) Jetpack Compose 컴포넌트
```kotlin
// BaseActivity 상속으로 공통 네비게이션 제공
class LevelActivity : BaseActivity()

// 메인 화면 구성
@Composable
fun LevelScreen(currentDays: Int, onBack: (() -> Unit)? = null)

// 레벨 카드 컴포넌트
@Composable
fun LevelCard(level: LevelInfo, currentDays: Int, enabled: Boolean)

// 데이터 클래스
data class LevelInfo(val name: String, val start: Int, val end: Int, val color: Color)
```

### (2) 색상 일관성
- Material3 테마 기반
- 각 레벨별 고유 색상 유지
- 흰색 배경으로 통일

### (3) 접근성
- 충분한 색상 대비
- 의미 있는 contentDescription
- 텍스트 크기 가독성 확보

### (4) 성능 최적화
- remember를 통한 상태 관리
- LazyColumn 대신 일반 Column + Scroll (레벨 수가 적음)
- 불필요한 리컴포지션 방지

---

## 6. 프리뷰 및 테스트

### (1) Compose 프리뷰
```kotlin
@Preview(showBackground = true)
@Composable
fun PreviewLevelScreen() {
    LevelActivity().LevelScreen(currentDays = 15)
}
```

### (2) 테스트 케이스
- 각 레벨 구간별 표시 확인
- 진행도 바 정확성 검증
- 최고 레벨 달성 시 UI 확인
- 스크롤 동작 테스트

---

이 기획서는 실제 구현된 코드를 바탕으로 작성되었으며, 동일한 화면을 재구현할 때 참고 문서로 활용할 수 있습니다.
