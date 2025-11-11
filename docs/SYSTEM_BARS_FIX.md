# 시스템 바 표시 문제 해결

## 문제 증상
- 금주 시작 화면(StartActivity)에서만 상단 시계/배터리 표시 영역이 보이지 않음
- 하단 3버튼 내비게이션도 보이지 않음
- 다른 화면들은 정상적으로 표시됨

## 근본 원인

### 1. 스플래시 테마의 시스템 바 색상 문제
- `StartActivity`가 `Theme.AlcoholicTimer.Splash` 테마를 사용
- 이 테마는 상태바와 내비게이션 바를 핑크색(#f28090)으로 설정
- `super.onCreate()` 호출 후에도 스플래시 테마의 시스템 바 설정이 유지됨
- 기존 코드에서는 "시스템바는 XML 테마에 일임"이라는 주석과 함께 코드에서 시스템 바를 설정하지 않았음

### 2. WindowCompat.setDecorFitsSystemWindows 일관성 부족
- `BaseActivity.onCreate()`에서는 `setDecorFitsSystemWindows(true)`를 호출했지만
- `StartActivity`에서 스플래시 처리로 인해 실행 순서가 달라져 효과가 없었음

### 3. 시스템 바가 숨겨질 때의 레이아웃 문제
- `StandardScreenWithBottomButton`은 내비게이션 바 인셋을 고려하여 레이아웃
- 시스템 바가 숨겨지면 인셋 값이 0이 되어 버튼이 화면 밖으로 밀려남

## 해결책

### 1. BaseActivity에서 시스템 바 명시적 설정
모든 Activity가 상속하는 `BaseActivity.onCreate()`에서 시스템 바를 명시적으로 설정:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // 시스템 바를 항상 표시하고 윈도우가 시스템 인셋에 맞춰 레이아웃되도록 설정
    WindowCompat.setDecorFitsSystemWindows(window, true)
    
    // 시스템 바 색상을 명시적으로 흰색으로 설정
    window.statusBarColor = android.graphics.Color.WHITE
    window.navigationBarColor = android.graphics.Color.WHITE
    
    // 시스템 바 아이콘을 어둡게 설정 (흰 배경에서 보이도록)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                isAppearanceLightNavigationBars = true
            }
        }
    }
    // ... 나머지 코드
}
```

### 2. StartActivity에서 스플래시 이후 시스템 바 복원
`StartActivity.onCreate()`에서 `super.onCreate()` 호출 직후 동일한 설정을 추가하여 스플래시 테마의 색상을 덮어씀:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    // 스플래시 스크린 설치 ...
    
    super.onCreate(savedInstanceState)
    
    // 시스템 바를 항상 표시하고 윈도우가 시스템 인셋에 맞춰 레이아웃되도록 설정
    androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
    
    // 시스템 바 색상을 명시적으로 흰색으로 설정 (스플래시 테마 색상 덮어쓰기)
    window.statusBarColor = android.graphics.Color.WHITE
    window.navigationBarColor = android.graphics.Color.WHITE
    
    // 시스템 바 아이콘을 어둡게 설정 (흰 배경에서 보이도록)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                isAppearanceLightNavigationBars = true
            }
        }
    }
    // ... 나머지 코드
}
```

## 핵심 포인트

1. **XML 테마만으로는 충분하지 않음**: 특히 스플래시 스크린을 사용하는 경우, 코드에서 명시적으로 시스템 바를 설정해야 함

2. **일관성 있는 시스템 바 관리**: 모든 Activity에서 동일한 방식으로 시스템 바를 설정하여 화면 간 이동 시 깜빡임이나 표시 오류를 방지

3. **WindowCompat.setDecorFitsSystemWindows(true)**: 이 설정이 있어야 레이아웃이 시스템 바를 고려하여 올바른 위치에 배치됨

4. **재발 방지**: 새로운 Activity를 추가할 때 `BaseActivity`를 상속받으면 자동으로 올바른 시스템 바 설정이 적용됨

## 테스트 확인 사항
- ✓ 금주 시작 화면에서 상단 시계/배터리 표시 영역이 정상 표시
- ✓ 하단 내비게이션 바가 정상 표시
- ✓ 하단 3버튼(목표 일수 입력, 시작 버튼 등)이 올바른 위치에 표시
- ✓ 다른 화면들도 여전히 정상 표시
- ✓ 화면 간 이동 시 시스템 바 깜빡임 없음

## 수정된 파일
- `app/src/main/java/kr/sweetapps/alcoholictimer/core/ui/BaseActivity.kt`
- `app/src/main/java/kr/sweetapps/alcoholictimer/feature/start/StartActivity.kt`

