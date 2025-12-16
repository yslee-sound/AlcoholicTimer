# 애니메이션 작동하지 않는 이유 분석

## 🔍 문제 원인

### 현재 코드 구조
```kotlin
FullScreenWriteDialog(visible = isWritingScreenVisible, ...)

// 내부
if (visible) {
    Dialog {
        AnimatedVisibility(visible = visible) { // ← 문제!
            Scaffold { ... }
        }
    }
}
```

### 왜 작동하지 않나?

1. **Dialog가 즉시 제거됨**
   - `isWritingScreenVisible = false` 되면
   - `if (visible)` 조건이 false가 되어
   - Dialog가 **즉시 사라짐**
   - AnimatedVisibility의 exit 애니메이션이 실행될 시간이 없음

2. **AnimatedVisibility의 visible이 항상 true**
   - Dialog 내부에서 `visible = visible`
   - Dialog가 렌더링될 때는 항상 visible=true
   - exit 조건이 트리거되지 않음

## ✅ 해결 방법

### 방법 1: LaunchedEffect + Delay
```kotlin
var showDialog by remember { mutableStateOf(false) }
var animateContent by remember { mutableStateOf(false) }

LaunchedEffect(isWritingScreenVisible) {
    if (isWritingScreenVisible) {
        showDialog = true
        delay(50) // Dialog 렌더링 대기
        animateContent = true
    } else {
        animateContent = false
        delay(1000) // exit 애니메이션 완료 대기
        showDialog = false
    }
}

if (showDialog) {
    Dialog {
        AnimatedVisibility(visible = animateContent) {
            // 내용
        }
    }
}
```

### 방법 2: Box + zIndex (추천)
```kotlin
// Dialog 대신 Box 사용
Box(modifier = Modifier.fillMaxSize()) {
    // 기존 피드
    
    // 글쓰기 화면 오버레이
    AnimatedVisibility(
        visible = isWritingScreenVisible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = Modifier.zIndex(1f)
    ) {
        // 전체 화면 글쓰기 UI
    }
}
```

---

**결론**: Dialog + AnimatedVisibility 조합은 복잡합니다. Box + zIndex 방식이 더 간단하고 확실합니다.

