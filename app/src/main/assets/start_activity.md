# 금주 목표 설정 화면 상세 기획서

본 기획서는 사용자가 금주 목표 일수를 설정하는 시작 화면을 설계합니다.
Jetpack Compose 기반으로 구현하며, 미니멀하고 직관적인 UX를 제공합니다.

---

## 1. 화면 개요

### 목적
- 사용자가 금주 목표 일수를 간단하게 설정
- 설정 완료 후 금주 타이머 시작
- 최소한의 텍스트와 직관적인 인터페이션으로 사용 편의성 극대화

### 화면 진입 조건
- 앱 최초 실행 시
- 햄버거 메뉴에서 "금주" 선택 시 (금주가 시작되지 않았거나 완료된 경우)
- 기존 금주가 완료된 후 새로운 목표 설정 시

---

## 2. 화면 구성

### (1) 상단바 (BaseActivity 공통)
- **햄버거 메뉴**: 좌측 상단
- **화면 제목**: "금주 설정"
- **배경색**: 흰색 (#FFFFFF)
- **구분선**: 하단에 연한 회색 1dp 라인

### (2) 메인 컨텐츠 영역
**전체 레이아웃**:
- **배경색**: 흰색 (#FFFFFF)
- **패딩**: 좌우 32dp
- **정렬**: 세로 중앙 정렬 (verticalArrangement = Arrangement.Top)
- **상단 여백**: 80dp (상단바 아래)

#### A. 상단 아이콘 영역
```
🍃 (잎사귀 이모지)
- 크기: 100sp
- 위치: 중앙 정렬
- 하단 여백: 60dp
```

#### B. 목표 설정 영역
**"목표 설정" 라벨**:
- **텍스트**: "목표 설정"
- **크기**: 24sp
- **색상**: 회색 (Color.Gray)
- **하단 여백**: 24dp

**숫자 입력 필드**:
- **기본값**: "0" (사용자가 터치하면 자동으로 새 숫자로 교체)
- **크기**: 48sp, Bold, 중앙 정렬
- **너비**: 150dp (3자리 숫자에 적합한 크기)
- **색상**: 유효한 값(1 이상)이면 MaterialTheme.colorScheme.primary, 그 외 검은색
- **키보드**: 숫자 키패드만 표시
- **하단 여백**: 8dp

**입력 검증 로직**:
```kotlin
onValueChange = { newValue ->
    val filteredValue = newValue.filter { it.isDigit() }
    inputText = if (filteredValue.length > 1 && filteredValue.startsWith("0")) {
        filteredValue.substring(1) // "02" → "2"
    } else {
        filteredValue
    }
}
```

**밑줄**:
- **두께**: 2dp
- **색상**: 검은색 (Color.Black)
- **스타일**: 직선 (StrokeCap.Square)
- **너비**: 입력 필드와 동일 (150dp)

**단위 표시**:
- **텍스트**: "일"
- **크기**: 20sp
- **색상**: 회색 (Color.Gray)
- **위치**: 밑줄 아래 16dp
- **정렬**: 중앙

#### C. 시작 버튼 영역
**FloatingActionButton**:
- **크기**: 100dp (직경)
- **모양**: 완전한 원형 (CircleShape)
- **아이콘**: PlayArrow (▶️)
- **아이콘 크기**: 50dp
- **아이콘 색상**: 흰색
- **배경색**: 
  - 유효한 값(1 이상): MaterialTheme.colorScheme.primary
  - 무효한 값: 회색 (Color.Gray)
- **상단 여백**: 60dp

---

## 3. 상세 UI 스펙

### (1) 색상 시스템
```kotlin
// 주요 색상
- 배경: Color.White (#FFFFFF)
- 텍스트 라벨: Color.Gray
- 입력 숫자 (유효): MaterialTheme.colorScheme.primary
- 입력 숫자 (무효): Color.Black
- 밑줄: Color.Black
- 버튼 (활성): MaterialTheme.colorScheme.primary
- 버튼 (비활성): Color.Gray
```

### (2) 타이포그래피
```kotlin
// 텍스트 크기 및 스타일
- 아이콘: 100sp
- "목표 설정": 24sp, Normal
- 입력 숫자: 48sp, Bold, Center
- 단위 "일": 20sp, Normal
```

### (3) 여백 및 간격
```kotlin
// 주요 spacing 값
- 화면 좌우 패딩: 32dp
- 상단 여백: 80dp (상단바 제외)
- 아이콘 하단: 60dp
- 라벨 하단: 24dp
- 입력 필드 하단: 8dp
- 단위 상단: 16dp
- 버튼 상단: 60dp
```

### (4) 컴포넌트 크기
```kotlin
// 주요 컴포넌트 크기
- 입력 필드 너비: 150dp
- 밑줄 두께: 2dp
- 시작 버튼: 100dp (직경)
- 버튼 아이콘: 50dp
```

---

## 4. 사용자 상호작용

### (1) 입력 동작
**초기 상태**: "0" 표시
**사용자가 숫자 입력 시**:
- "0" + "2" → "2" (앞의 0 자동 제거)
- "1" + "5" → "15" (정상 연결)
- 숫자가 아닌 문자는 자동 필터링

### (2) 시각적 피드백
**유효한 값(1 이상)**:
- 입력 숫자가 파란색으로 변경
- 시작 버튼이 파란색으로 활성화
- 밑줄은 항상 검은색 유지

**무효한 값(0 또는 빈값)**:
- 입력 숫자가 검은색
- 시작 버튼이 회색으로 비활성화

### (3) 버튼 동작
**시작 버튼 클릭 시**:
```kotlin
onClick = {
    val targetTime = inputText.toIntOrNull() ?: 0
    if (targetTime > 0) {
        // SharedPreferences에 저장
        val sharedPref = context.getSharedPreferences("user_settings", MODE_PRIVATE)
        sharedPref.edit().apply {
            putInt("target_days", targetTime)
            putLong("start_time", System.currentTimeMillis())
            putBoolean("timer_completed", false)
            apply()
        }
        // StatusActivity로 이동
        val intent = Intent(context, StatusActivity::class.java)
        context.startActivity(intent)
    }
}
```

---

## 5. 레이아웃 구조

### Jetpack Compose 컴포넌트 계층
```kotlin
BaseScreen { // 공통 네비게이션 제공
    Column( // 메인 컨테이너
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(80.dp) // 상단 여백
        
        Text("🍃", 100.sp) // 아이콘
        
        Column { // 입력 영역
            Text("목표 설정", 24.sp)
            Box(150.dp width) {
                Column {
                    BasicTextField(...) // 숫자 입력
                    Canvas { drawLine(...) } // 밑줄
                }
            }
            Text("일", 20.sp) // 단위
        }
        
        Spacer(60.dp) // 버튼 위 여백
        
        FloatingActionButton(...) // 시작 버튼
    }
}
```

---

## 6. 기술 구현 요구사항

### (1) Jetpack Compose 컴포넌트
- **BaseActivity**: 공통 네비게이션 제공
- **BasicTextField**: 커스텀 숫자 입력 필드
- **Canvas**: 밑줄 그리기
- **FloatingActionButton**: 원형 시작 버튼

### (2) 상태 관리
```kotlin
var inputText by remember { mutableStateOf("0") }
val isValid = inputText.toIntOrNull()?.let { it > 0 } ?: false
```

### (3) 데이터 저장
- **SharedPreferences**: "user_settings"
- **저장 항목**:
  - `target_days`: 목표 일수 (Int)
  - `start_time`: 시작 시간 (Long, currentTimeMillis)
  - `timer_completed`: 완료 여부 (Boolean, false)

### (4) 네비게이션
- **완료 후 이동**: StatusActivity (금주 진행 화면)
- **전환 효과**: 없음 (즉시 전환)

---

## 7. UI 예시 레이아웃

```
┌─────────────────────────────────┐
│ ☰  금주 설정                     │ ← 상단바
├─────────────────────────────────┤
│                                 │
│                                 │
│            🍃                   │ ← 100sp 아이콘
│                                 │
│                                 │
│         목표 설정               │ ← 24sp 라벨
│                                 │
│           30                    │ ← 48sp Bold 숫자
│         ──────                  │ ← 검은색 밑줄
│           일                    │ ← 20sp 단위
│                                 │
│                                 │
│           ● ▶                   │ ← 100dp 원형 버튼
│                                 │
│                                 │
└─────────────────────────────────┘
```

---

## 8. 프리뷰 및 테스트

### (1) Compose 프리뷰
```kotlin
@Preview(showBackground = true)
@Composable
fun PreviewStartScreen() {
    BaseScreen {
        StartScreen()
    }
}
```

### (2) 테스트 케이스
- [ ] 초기값 "0" 표시 확인
- [ ] 숫자 입력 시 앞의 0 제거 동작
- [ ] 유효/무효값에 따른 색상 변화
- [ ] 버튼 활성화/비활성화 상태
- [ ] SharedPreferences 저장 확인
- [ ] StatusActivity 전환 확인

---

## 9. 접근성 고려사항

### (1) 시각적 접근성
- **충분한 색상 대비**: 검은색 텍스트 + 흰색 배경
- **큰 터치 영역**: 100dp 버튼으로 터치하기 쉬움
- **명확한 시각적 피드백**: 색상 변화로 상태 표시

### (2) 사용성
- **직관적인 플레이 버튼**: 보편적인 ▶️ 아이콘 사용
- **최소한의 텍스트**: "목표 설정", "일"만 표시
- **자동 입력 개선**: 앞의 0 자동 제거로 자연스러운 입력

---

이 기획서는 실제 구현된 StartActivity.kt 코드를 바탕으로 작성되었으며, 동일한 화면을 재구현할 때 완벽한 참고 문서로 활용할 수 있습니다.
