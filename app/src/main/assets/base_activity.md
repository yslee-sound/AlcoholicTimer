# 🍔 햄버거 메뉴 상세 기획서 (Jetpack Compose 구현)
- Jetpack Compose 기반으로 구현하며, 미니멀하고 직관적인 UX를 제공합니다.

금주 타이머 앱의 사이드(햄버거) 메뉴 상세 기획서입니다.

---

## 1. 메뉴 구조 및 구성 요소

### [상단 프로필 영역]
- **동그란 원형 아바타**
    - **크기**: 80dp(가로) × 72dp(세로) Box 컨테이너
    - **모양**: CircleShape로 완전한 원형
    - **배경색**: Color(0xFF888888) - 진한 회색 톤
    - **아이콘**: Icons.Default.Person (사람 모양)
    - **아이콘 색상**: Color.White (밝은 흰색)
    - **아이콘 크기**: fillMaxSize(0.9f) - 원의 90% 크기
    - **위치**: 좌측 시작점에서 8dp 패딩
    - **정렬**: Box의 Alignment.Center로 중앙 배치

- **사용자 별명**
    - **기본값**: "알중이1"
    - **크기**: 18sp
    - **스타일**: FontWeight.Bold
    - **위치**: 아바타 아래 8dp 간격, 좌측 시작점에서 8dp 패딩
    - **클릭 가능**: clickable { onNicknameClick() }
    - **정렬**: Alignment.Start

### [구분선]
- **첫 번째 구분선**: 프로필 영역과 메인 메뉴 사이
    - **스타일**: HorizontalDivider
    - **패딩**: 상하 12dp

---

## 2. 메인 메뉴 영역

### [메인 기능 메뉴]
1. **금주**
    - **아이콘**: Icons.Default.PlayArrow
    - **동작**: 금주 상태에 따라 StartActivity 또는 RunActivity로 이동
    - **조건 분기**:
        - 금주 미시작/완료 상태 → StartActivity
        - 금주 진행 중 → RunActivity

2. **기록**
    - **아이콘**: Icons.Default.List
    - **동작**: RecordsActivity로 이동

3. **레벨**
    - **아이콘**: Icons.Default.Star
    - **동작**: LevelActivity로 이동

### [구분선]
- **두 번째 구분선**: 메인 메뉴와 설정 메뉴 사이
    - **스타일**: HorizontalDivider
    - **패딩**: 상하 8dp

---

## 3. 설정 메뉴 영역

### [설정 기능 메뉴]
1. **설정**
    - **아이콘**: Icons.Default.Settings
    - **동작**: SettingsActivity로 이동

> 참고: 2025-10-02 이후 버전에서 "테스트" 메뉴는 제거되었습니다.

---

## 4. 메뉴 아이템 공통 스타일

### [각 메뉴 아이템]
- **레이아웃**: Row (가로 배치)
- **정렬**: verticalAlignment = Alignment.CenterVertically
- **패딩**: 
    - 상하: 12dp
    - 좌우: 8dp
- **클릭 영역**: fillMaxWidth()
- **아이콘 크기**: 24dp
- **아이콘과 텍스트 간격**: 16dp (Spacer)
- **텍스트 크기**: 16sp

---

## 5. 전체 컨테이너 스타일

### [드로어 컨테이너]
- **배경색**: Color.White
- **전체 패딩**: 16dp
- **정렬**: horizontalAlignment = Alignment.Start
- **드로어 타입**: ModalNavigationDrawer
- **드로어 시트**: ModalDrawerSheet

### [탑 앱바]
- **배경색**: Color.White
- **제목 색상**: Color.Black
- **햄버거 아이콘**: Icons.Default.Menu (검은색)
- **구분선**: HorizontalDivider (밑줄, Color.LightGray, 1dp)

---

## 6. 네비게이션 로직

### [화면 전환 처리]
- **중복 방지**: 현재 액티비티와 동일한 메뉴 선택 시 이동하지 않음
- **전환 효과**: 없음 (overridePendingTransition 사용하지 않음)
- **드로어 닫기**: 메뉴 선택 시 자동으로 드로어 닫기

### [금주 상태별 분기]
```kotlin
when (menuItem) {
    "금주" -> {
        val sharedPref = getSharedPreferences("user_settings", MODE_PRIVATE)
        val startTime = sharedPref.getLong("start_time", 0L)
        val timerCompleted = sharedPref.getBoolean("timer_completed", false)
        
        if (startTime == 0L || timerCompleted) {
            // StartActivity로 이동
        } else {
            // RunActivity로 이동
        }
    }
}
```

---

## 7. 프리뷰 구성

### [개발자 프리뷰]
- **함수명**: PreviewDrawerMenu()
- **상태**: DrawerValue.Open (열린 상태로 프리뷰)
- **테마**: MaterialTheme 적용
- **배경**: showBackground = true

---

## 8. 제거된 기능들

### [삭제된 메뉴 항목들]
- **챌린지**: 사용하지 않음
- **응원메시지**: 사용하지 않음  
- **알림함**: 사용하지 않음
- **테스트**: 2025-10-02 이후 제거됨

---

## 9. 색상 팔레트

### [주요 색상]
- **배경색**: Color.White
- **텍스트**: Color.Black
- **아바타 배경**: Color(0xFF888888)
- **아바타 아이콘**: Color.White
- **구분선**: Color.LightGray

---

## 10. 반응형 고려사항

### [다양한 화면 크기 대응]
- **패딩**: dp 단위로 일관성 있게 적용
- **아이콘 크기**: 고정 크기 (24dp) 사용
- **텍스트 크기**: sp 단위로 접근성 고려
- **터치 영역**: fillMaxWidth()로 충분한 터치 영역 확보
