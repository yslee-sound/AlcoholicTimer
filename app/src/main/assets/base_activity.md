# 🍔 햄버거 메뉴 상세 기획서 (Jetpack Compose 구현)

금주 타이머 앱의 사이드(햄버거) 메뉴 상세 기획서입니다.  
Jetpack Compose 기반으로 구현하며, 실제 구현된 UI를 반영한 완전한 문서입니다.

---

## 1. 메뉴 구조 및 구성 요소

### [상단 프로필 영역]
- **동그란 원형 아바타**
    - **크기**: 80dp(가로) × 72dp(세로) Box 컨테이너
    - **모양**: CircleShape로 완전한 원형
    - **배경색**: Color(0xFF888888) - 진한 회색 톤
    - **아이콘**: Icons.Default.Person (사람 모양)
    - **아이콘 색상**: Color.White (밝은 흰색)
    - **아이콘 크기**: 40dp
    - **위치**: 좌측 시작점에서 8dp 패딩
    - **정렬**: wrapContentSize(Alignment.Center)로 중앙 배치

- **사용자 별명**
    - **기본값**: "알중이1"
    - **크기**: 18sp
    - **스타일**: FontWeight.Bold
    - **위치**: 아바타 아래 8dp 간격, 좌측 시작점에서 8dp 패딩
    - **클릭 가능**: clickable { onNicknameClick() }
    - **기능**: 클릭 시 별명 변경 기능 (현재 구현에서는 빈 함수)

### [구분선]
- **타입**: HorizontalDivider (Material3)
- **여백**: 상하 12dp 패딩
- **색상**: Material3 기본 색상

### [메인 메뉴 목록]
**메뉴 구성**:
```kotlin
val menuItems = listOf(
    "금주" to Icons.Default.PlayArrow,
    "기록" to Icons.Default.List,
    "레벨" to Icons.Default.Star
)
```

1. **금주**
   - **아이콘**: Icons.Default.PlayArrow (▶️)
   - **네비게이션 로직**: 
     - 금주 미시작/완료: StartActivity (목표 설정)
     - 금주 진행중: StatusActivity (진행 상황)
   - **판단 기준**: SharedPreferences의 "start_time", "timer_completed" 값

2. **기록**
   - **아이콘**: Icons.Default.List (목록)
   - **이동**: RecordsActivity

3. **레벨**
   - **아이콘**: Icons.Default.Star (별)
   - **이동**: LevelActivity

### [설정 구분선]
- **타입**: HorizontalDivider
- **여백**: 상하 8dp 패딩

### [설정 메뉴 목록]
**설정 구성**:
```kotlin
val settingsItems = listOf(
    "설정" to Icons.Default.Settings,
    "테스트" to Icons.Default.Build
)
```

1. **설정**
   - **아이콘**: Icons.Default.Settings (⚙️)
   - **이동**: TestActivity

2. **테스트**
   - **아이콘**: Icons.Default.Build (🔧)
   - **이동**: TestActivity
   - **용도**: 개발자/테스트용 기능

---

## 2. 상세 UI 스펙

### (1) 전체 레이아웃
```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    horizontalAlignment = Alignment.Start
)
```

### (2) 아바타 영역
```kotlin
Box(
    modifier = Modifier
        .width(80.dp)
        .height(72.dp)
        .align(Alignment.Start)
        .padding(start = 8.dp)
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF888888),
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "아바타",
            tint = Color.White,
            modifier = Modifier.size(40.dp).wrapContentSize(Alignment.Center)
        )
    }
}
```

### (3) 메뉴 아이템 구조
```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable { onItemSelected(title) }
        .padding(vertical = 12.dp, horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Icon(
        imageVector = icon,
        contentDescription = title,
        modifier = Modifier.size(24.dp)
    )
    Spacer(modifier = Modifier.width(16.dp))
    Text(
        text = title,
        fontSize = 16.sp
    )
}
```

---

## 3. 색상 시스템

### (1) 전체 색상 팔레트
```kotlin
// 메뉴 배경
drawerContainerColor = Color.White
modifier = Modifier.background(Color.White)

// 아바타
아바타 배경: Color(0xFF888888) // 진한 회색
아바타 아이콘: Color.White // 흰색

// 텍스트
별명: Color.Black (기본값)
메뉴 텍스트: Color.Black (기본값)

// 아이콘
메뉴 아이콘: Color.Black (기본값)

// 구분선
구분선: Material3 기본 색상 (연한 회색)
```

---

## 4. 상호작용 및 네비게이션

### (1) 메뉴 클릭 처리
```kotlin
private fun handleMenuSelection(menuItem: String) {
    when (menuItem) {
        "금주" -> {
            val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
            val startTime = sharedPref.getLong("start_time", 0L)
            val timerCompleted = sharedPref.getBoolean("timer_completed", false)
            if (startTime == 0L || timerCompleted) {
                // StartActivity로 이동 (목표 설정)
                if (this !is StartActivity) {
                    navigateToActivity(StartActivity::class.java)
                }
            } else {
                // StatusActivity로 이동 (진행 상황)
                if (this !is StatusActivity) {
                    navigateToActivity(StatusActivity::class.java)
                }
            }
        }
        "기록" -> RecordsActivity
        "레벨" -> LevelActivity  
        "설정" -> TestActivity
        "테스트" -> TestActivity
    }
}
```

### (2) 화면 전환
- **방식**: Intent를 통한 Activity 전환
- **효과**: 전환 애니메이션 없음 (즉시 전환)
- **중복 방지**: 현재 Activity와 동일한 경우 이동하지 않음

---

## 5. 레이아웃 구조도

### 시각적 레이아웃
```
┌─────────────────────────────────┐
│                                 │
│    ●  알중이1                   │ ← 80×72dp 원형 + 18sp Bold
│                                 │
├─────────────────────────────────┤ ← HorizontalDivider (12dp 여백)
│                                 │
│ ▶️ 금주                        │ ← 24dp 아이콘 + 16sp 텍스트
│ 📋 기록                        │
│ ⭐ 레벨                        │
│                                 │
├─────────────────────────────────┤ ← HorizontalDivider (8dp 여백)
│                                 │
│ ⚙️ 설정                        │
│ 🔧 테스트                      │
│                                 │
└─────────────────────────────────┘
```

### 컴포넌트 계층
```kotlin
ModalNavigationDrawer {
    ModalDrawerSheet(Color.White) {
        DrawerMenu(
            nickname = "알중이1",
            onNicknameClick = {},
            onItemSelected = { handleMenuSelection(it) }
        ) {
            Column(16dp padding) {
                // 아바타 + 별명 영역
                Box(80×72dp) { CircleShape Surface + Person Icon }
                Text("알중이1", 18sp Bold)
                
                // 첫 번째 구분선
                HorizontalDivider(12dp vertical padding)
                
                // 메인 메뉴
                forEach(menuItems) { 
                    Row { Icon(24dp) + Text(16sp) }
                }
                
                // 두 번째 구분선  
                HorizontalDivider(8dp vertical padding)
                
                // 설정 메뉴
                forEach(settingsItems) {
                    Row { Icon(24dp) + Text(16sp) }
                }
            }
        }
    }
}
```

---

## 6. 데이터 연동

### (1) SharedPreferences 키
```kotlin
// 금주 상태 판단용
"start_time": Long (금주 시작 시간, 0L이면 미시작)
"timer_completed": Boolean (금주 완료 여부)

// 사용자 설정 (미래 확장용)
"nickname": String (사용자 별명, 기본값: "알중이1")
```

### (2) 상태 판단 로직
```kotlin
val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
val startTime = sharedPref.getLong("start_time", 0L)
val timerCompleted = sharedPref.getBoolean("timer_completed", false)

// 금주 미시작 또는 완료 상태
if (startTime == 0L || timerCompleted) {
    // StartActivity (목표 설정 화면)
} else {
    // StatusActivity (진행 상황 화면)  
}
```

---

## 7. 구현 시 주의사항

### (1) Material3 컴포넌트 사용
- **ModalNavigationDrawer**: 최신 Material3 권장 방식
- **HorizontalDivider**: Divider 대신 사용 (deprecated 방지)
- **Icons.Default.***: Material Icons 활용

### (2) 상태 관리
```kotlin
val drawerState = rememberDrawerState(DrawerValue.Closed)
val scope = rememberCoroutineScope()

// 메뉴 클릭 시 Drawer 자동 닫기
scope.launch { drawerState.close() }
```

### (3) 접근성
- **contentDescription**: 모든 아이콘에 의미 있는 설명 추가
- **클릭 영역**: 충분한 터치 영역 확보 (vertical 12dp, horizontal 8dp 패딩)
- **색상 대비**: 흰색 배경 + 검은색 텍스트로 높은 가독성

---

## 8. 프리뷰 코드

### Compose 프리뷰
```kotlin
@Preview(showBackground = true)
@Composable
fun PreviewDrawerMenu() {
    MaterialTheme {
        ModalNavigationDrawer(
            drawerState = rememberDrawerState(DrawerValue.Open),
            drawerContent = {
                ModalDrawerSheet {
                    DrawerMenu(
                        nickname = "알중이1",
                        onNicknameClick = {},
                        onItemSelected = {}
                    )
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {}
        }
    }
}
```

---

## 9. 향후 확장 가능성

### (1) 별명 변경 기능
- AlertDialog + TextField로 별명 입력 받기
- SharedPreferences에 저장 후 UI 업데이트

### (2) 아바타 커스터마이징  
- 여러 아바타 이미지 선택 기능
- 색상 테마 변경 기능

### (3) 메뉴 개인화
- 자주 사용하는 메뉴 상단 고정
- 메뉴 순서 커스터마이징

---

이 기획서는 실제 구현된 BaseActivity.kt의 DrawerMenu 코드를 정확히 분석하여 작성되었으며, 동일한 햄버거 메뉴를 재구현할 때 완벽한 참고 문서로 활용할 수 있습니다.
