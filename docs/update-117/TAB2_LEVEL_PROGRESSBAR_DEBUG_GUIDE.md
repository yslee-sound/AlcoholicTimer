# 📊 탭2 레벨 카드 프로그레스바 문제 진단 가이드

**작성일**: 2025-12-20  
**상태**: ✅ 디버그 코드 추가 완료

---

## 🎯 문제 상황

탭2 상단의 레벨 요약 배너(LevelSummaryBanner)에 있는 **프로그레스바가 변형**되었다는 문제 발생.

---

## 📁 관련 파일 구조

### 핵심 파일 (우선순위 순)

| 순서 | 파일명 | 경로 | 역할 |
|------|--------|------|------|
| **1** | **LevelSummaryBanner.kt** ⭐ | `ui/tab_02/components/` | **프로그레스바 UI 렌더링** |
| **2** | **Tab03ViewModel.kt** | `ui/tab_03/viewmodel/` | **진행도 계산 로직** |
| **3** | **Tab02ListGraph.kt** | `ui/main/navigation/` | ViewModel → UI 데이터 전달 |
| **4** | **RecordsScreen.kt** | `ui/tab_02/screens/` | 레벨 배너 호출 |

---

## 🔍 데이터 흐름 추적

```
1. Tab03ViewModel.calculateProgress()
   ↓ (Float 값 계산: 0.0 ~ 1.0)
   
2. Tab02ListGraph.kt (93줄)
   levelProgress = tab03ViewModel.calculateProgress()
   ↓
   
3. Tab02Screen() → RecordsScreen()
   progress 파라미터로 전달
   ↓
   
4. LevelSummaryBanner(progress = ...)
   ↓
   
5. LinearProgressIndicator(progress = { progress })
   (최종 UI 렌더링)
```

---

## 🐛 프로그레스바 코드 분석

### 위치: LevelSummaryBanner.kt (157~169줄)

```kotlin
// [DEBUG] 프로그레스 값 로깅
android.util.Log.d("LevelSummaryBanner", "Progress value: $progress (${(progress * 100).toInt()}%)")

// [NEW] 표준 LinearProgressIndicator (얇고 둥글게)
LinearProgressIndicator(
    progress = { progress.coerceIn(0f, 1f) }, // [FIX] 범위 강제 제한 추가
    modifier = Modifier
        .fillMaxWidth()
        .height(8.dp), // 프로그레스바 높이
    color = Color(0xFF00E676), // 밝은 민트색
    trackColor = Color.White.copy(alpha = 0.3f), // 배경 트랙
    strokeCap = StrokeCap.Round // 둥근 끝
)
```

#### 주요 속성:
- **progress**: 0.0 ~ 1.0 범위 (0% ~ 100%)
- **height**: 8.dp (얇은 막대)
- **color**: `#00E676` (민트색)
- **strokeCap**: Round (둥근 모서리)

---

## 🔧 진행도 계산 로직

### 위치: Tab03ViewModel.kt (225~242줄)

```kotlin
fun calculateProgress(): Float {
    val nextLevel = getNextLevel() ?: return 1f
    val current = _currentLevel.value

    return if (nextLevel.start > current.start) {
        // 현재 레벨 내에서의 진행도 계산
        val progressInLevel = _totalElapsedDaysFloat.value - (current.start - 1)
        val totalNeeded = (nextLevel.start - current.start).toFloat()
        
        if (totalNeeded > 0f) {
            (progressInLevel / totalNeeded).coerceIn(0f, 1f) // 0~1 범위 강제
        } else {
            0f
        }
    } else {
        0f
    }
}
```

#### 계산 예시:
- **현재:** 5일 경과, Lv.2 (1~7일)
- **다음:** Lv.3 (7~21일)
- **계산:** (5 - 0) / (7 - 1) = 5 / 6 = **0.833 (83%)**

---

## 🛠️ 변형 증상별 진단

### 증상 1: 프로그레스바가 화면을 넘어감 🔴
**원인**: `progress > 1.0` (100% 초과)

**확인 방법**:
```bash
adb -s emulator-5554 logcat | Select-String "LevelSummaryBanner"
```

**예상 로그**:
```
LevelSummaryBanner: Progress value: 1.2345 (123%)  ← 문제!
```

**해결**: 이미 `.coerceIn(0f, 1f)` 추가됨 ✅

---

### 증상 2: 프로그레스바가 보이지 않음 🔴
**원인**: `progress = 0.0` 또는 `NaN`

**확인 포인트**:
1. `_totalElapsedDaysFloat.value`가 0인가?
2. `getNextLevel()`이 null을 반환하는가?
3. 타이머가 시작되지 않았는가?

**로그 예시**:
```
LevelSummaryBanner: Progress value: 0.0 (0%)  ← 타이머 미시작
```

---

### 증상 3: 프로그레스바 높이/모양이 이상함 🔴
**원인**: UI 속성 문제

**수정 위치**: `LevelSummaryBanner.kt` (160~166줄)

**조정 가능한 속성**:
```kotlin
.height(8.dp)           // 높이 변경 (4dp ~ 16dp)
color = Color(0xFF00E676)  // 색상 변경
strokeCap = StrokeCap.Round  // Square로 변경 가능
```

---

### 증상 4: 프로그레스바가 깜빡임/버벅임 🟡
**원인**: 너무 자주 리컴포지션

**해결**:
```kotlin
val progress by remember { derivedStateOf { calculateProgress() } }
```

---

## 🧪 디버깅 방법

### Step 1: 로그 확인
```powershell
adb -s emulator-5554 logcat -v time | Select-String "LevelSummaryBanner"
```

### Step 2: Progress 값 확인
탭2로 이동하면 다음과 같은 로그가 출력됨:
```
12-20 01:23:45.678 D/LevelSummaryBanner: Progress value: 0.5 (50%)
```

### Step 3: 계산 로직 확인
`Tab03ViewModel.calculateProgress()`에 브레이크포인트 설정

### Step 4: UI 속성 조정
`LevelSummaryBanner.kt`에서 높이, 색상 등 조정

---

## ✅ 적용된 수정사항

### 1. 범위 강제 제한 추가 ✅
```kotlin
progress = { progress.coerceIn(0f, 1f) }
```
**효과**: 100% 초과 방지

### 2. 디버그 로그 추가 ✅
```kotlin
android.util.Log.d("LevelSummaryBanner", "Progress value: $progress")
```
**효과**: 실시간 값 모니터링

---

## 📝 추가 확인 사항

### Q1: 프로그레스바가 정확히 어떻게 변형되었나요?
- [ ] 너무 길게 늘어남
- [ ] 보이지 않음
- [ ] 높이가 이상함
- [ ] 색상이 이상함
- [ ] 위치가 틀림

### Q2: 언제 발생하나요?
- [ ] 타이머 시작 전
- [ ] 타이머 진행 중
- [ ] 특정 레벨에서만
- [ ] 항상

### Q3: 최근 변경사항이 있었나요?
- [ ] Tab 폴더 구조 변경 (tab_04 → tab_03)
- [ ] ViewModel 수정
- [ ] UI 디자인 변경

---

## 🎯 다음 단계

1. **로그 확인**: 앱을 실행하고 탭2로 이동하여 로그 확인
2. **값 범위 검증**: Progress 값이 0.0~1.0 범위 안에 있는지 확인
3. **UI 속성 조정**: 필요시 높이, 색상 등 수정
4. **ViewModel 로직 검증**: 계산 로직이 올바른지 확인

---

## 📞 추가 지원

문제가 계속되면 다음 정보를 제공해주세요:

1. **로그 출력 내용** (`LevelSummaryBanner` 태그)
2. **스크린샷** (변형된 프로그레스바)
3. **현재 일수** (`currentDays` 값)
4. **현재 레벨** (Lv.1, Lv.2 등)

---

**작성일**: 2025-12-20  
**수정 완료**: ✅ 디버그 코드 추가 및 범위 제한 적용  
**빌드 상태**: ✅ 성공 (설치 완료)

