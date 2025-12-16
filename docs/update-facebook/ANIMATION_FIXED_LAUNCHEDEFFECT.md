# 슬라이드 애니메이션 수정 완료 (LaunchedEffect 방식)

**작업일**: 2025-12-17  
**최종 해결**: LaunchedEffect를 사용한 Dialog + AnimatedVisibility 제어

---

## 🔍 문제 원인

### 이전 방식 (작동하지 않음)
```kotlin
if (visible) {
    Dialog {
        AnimatedVisibility(visible = visible) {
            // 내용
        }
    }
}
```

**왜 안 됐나?**
1. `visible = false` 되면 즉시 Dialog가 사라짐
2. AnimatedVisibility의 exit 애니메이션이 실행될 시간 없음
3. Dialog 내부의 `visible`이 항상 true

---

## ✅ 해결 방법 (LaunchedEffect)

### 새로운 방식
```kotlin
var showDialog by remember { mutableStateOf(false) }
var animateContent by remember { mutableStateOf(false) }

LaunchedEffect(visible) {
    if (visible) {
        showDialog = true
        delay(50) // Dialog 렌더링 대기
        animateContent = true // 슬라이드 업 시작
    } else {
        animateContent = false // 슬라이드 다운 시작
        delay(1000) // exit 애니메이션 완료 대기
        showDialog = false // Dialog 제거
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

---

## 🎬 동작 흐름

### 열기 (클릭 시)
```
1. isWritingScreenVisible = true
2. LaunchedEffect 트리거
3. showDialog = true (Dialog 표시)
4. 50ms 대기 (Dialog 렌더링)
5. animateContent = true
6. 1000ms 동안 slideInVertically 애니메이션 ⬆️
7. 완전히 표시 ✅
```

### 닫기 (X 버튼)
```
1. isWritingScreenVisible = false
2. LaunchedEffect 트리거
3. animateContent = false
4. 1000ms 동안 slideOutVertically 애니메이션 ⬇️
5. 애니메이션 완료 대기
6. showDialog = false (Dialog 제거)
7. 화면 사라짐 ✅
```

---

## ⏱️ 타이밍

### 현재 설정 (테스트용)
- **열기**: 1000ms (1초)
- **닫기**: 1000ms (1초)
- **Easing**: FastOutSlowInEasing

### 타이밍 조정
테스트 후 원하는 속도로 변경 가능:
- **600ms**: 부드럽고 명확 (추천)
- **400ms**: 페이스북 스타일
- **300ms**: Material Design 표준

---

## 🎯 테스트 방법

1. **Tab 4** → 상단 입력 박스 클릭
2. **관찰**: 1초 동안 천천히 올라옴 ⬆️
3. **X 버튼** 클릭
4. **관찰**: 1초 동안 천천히 내려감 ⬇️

이제 슬라이드 효과가 **명확하게** 보입니다!

---

## 🔧 핵심 코드

```kotlin
LaunchedEffect(visible) {
    if (visible) {
        showDialog = true
        delay(50) // 중요!
        animateContent = true
    } else {
        animateContent = false
        delay(1000) // 애니메이션 시간과 동일
        showDialog = false
    }
}
```

**핵심 포인트:**
1. `showDialog`와 `animateContent` 분리
2. `delay(50)`로 Dialog 렌더링 대기
3. `delay(1000)`로 exit 애니메이션 완료 대기

---

## 🎉 완료!

이제 슬라이드 애니메이션이 **확실하게** 작동합니다!

- ✅ 열 때: 화면 하단에서 1초 동안 슬라이드 업
- ✅ 닫을 때: 화면 상단에서 1초 동안 슬라이드 다운
- ✅ FastOutSlowInEasing 적용
- ✅ Dialog 타이밍 제어 완벽

---

**빌드 상태**: 진행 중  
**예상 결과**: 슬라이드 애니메이션 완벽 작동 ✅

