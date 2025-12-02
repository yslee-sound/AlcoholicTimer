# 타이머 완료 UX 및 광고 연동 구현 가이드

## 📋 개요

이 문서는 타이머 만료 시 사용자 경험(UX) 및 광고 연동 동작을 정의합니다.
**구체적인 함수명이나 파일명 없이**, 시스템이 따라야 할 **행동 및 상태 제약 조건**을 명시합니다.

## 🎯 핵심 원칙

1. **만료 상태 플래그**: 타이머 만료 여부를 나타내는 boolean 플래그로 관리
2. **1번째 탭 강제 점유**: 만료 상태가 `true`일 때, 1번째 탭은 "목표 달성 완료!" 전체 화면만 표시
3. **명확한 해제 경로**: "새 타이머 시작" 버튼만이 만료 상태를 해제할 수 있음

---

## 1️⃣ 탭 UI 및 상태 잠금 동작

| 항목 | 동작 제약 조건 |
|:---|:---|
| **만료 상태 진입** | 타이머 만료가 감지되면, **만료 상태 플래그**는 반드시 **`true`**로 설정되어야 합니다. |
| **1번째 탭 강제 점유** | 앱 실행 중이거나 다른 탭에서 1번째 탭으로 복귀할 때, **만료 상태 플래그가 `true`라면**, 1번째 탭은 **'목표 달성 완료!' 전체 화면 UI**를 **강제로 표시**해야 합니다. 일반 타이머 설정 UI는 절대 표시해서는 안 됩니다. |
| **탭 재선택 동작** | 만료 상태(`true`)인 동안, 사용자가 **1번째 탭 아이콘을 다시 선택하더라도** 만료 알림 화면에서 벗어나지 않고 **화면을 유지**해야 합니다. |

### 구현 상세

#### 1.1 만료 상태 플래그 관리
- `TimerStateRepository.isTimerFinished()`: 만료 상태 확인
- `TimerStateRepository.setTimerFinished(boolean)`: 만료 상태 설정
- SharedPreferences를 통해 영구 저장

#### 1.2 1번째 탭 화면 분기 로직
```kotlin
// BottomNavBar.kt의 1번째 탭 클릭 로직
if (index == 0) {
    val isFinished = TimerStateRepository.isTimerFinished()
    
    // [중요] 만료 상태가 true면 Finished 화면으로 강제 이동
    val targetRoute = if (isFinished) {
        Screen.Finished.route
    } else {
        // 일반 상태: 타이머 실행 여부에 따라 Run 또는 Start 화면
        val startTime = TimerStateRepository.getStartTime()
        if (startTime > 0) Screen.Run.route else Screen.Start.route
    }
    
    if (currentRoute != targetRoute) {
        navController.navigate(targetRoute) { ... }
    }
}
```

---

## 2️⃣ '결과 확인' 버튼 동작 (광고 및 이동)

| 항목 | 동작 제약 조건 |
|:---|:---|
| **광고 정책 확인** | **'결과 확인'** 버튼 클릭 시, 광고 정책 관리 모듈에 **현재 전면 광고 노출이 가능한지**를 문의해야 합니다. |
| **광고 노출** | 노출 가능 응답을 받으면 **전면 광고를 노출**합니다. 광고가 닫히거나 노출에 실패하면 다음 단계로 이동합니다. |
| **광고 예외** | 노출 불가능 응답을 받으면, **광고 없이** 다음 단계로 즉시 이동합니다. |
| **상태 유지 및 이동** | 이 동작 수행 시 **만료 상태 플래그는 `true`로 유지**해야 하며, 광고 후에는 **2번째 탭 (통계/기록 화면)**으로 **프로그래밍 방식**으로 이동해야 합니다. |

### 구현 상세

#### 2.1 광고 정책 체크
```kotlin
val shouldShowAd = AdPolicyManager.shouldShowInterstitialAd(context)
```

#### 2.2 광고 노출 및 화면 이동
```kotlin
onResultCheck = {
    Log.d("NavGraph", "결과 확인 클릭 -> 광고 정책 체크")
    
    val shouldShowAd = AdPolicyManager.shouldShowInterstitialAd(context)
    
    val proceedToRecords: () -> Unit = {
        // [중요] 만료 상태 플래그는 true로 유지
        Log.d("NavGraph", "Records 화면(2번째 탭)으로 이동")
        
        navController.navigate(Screen.Records.route) {
            launchSingleTop = true
        }
        recordsRefreshCounter++
    }
    
    if (shouldShowAd && activity != null) {
        if (InterstitialAdManager.isLoaded()) {
            InterstitialAdManager.show(activity) { success ->
                proceedToRecords()
            }
        } else {
            proceedToRecords()
        }
    } else {
        proceedToRecords()
    }
}
```

#### 2.3 동작 흐름
```
[결과 확인 버튼 클릭]
    ↓
[광고 정책 체크]
    ↓
[광고 가능?]
    ├─ YES → [전면 광고 노출] → [광고 종료] → [완료 기록 상세 화면으로 이동]
    └─ NO  → [즉시 완료 기록 상세 화면으로 이동]
    ↓
[만료 상태 플래그: true 유지]
```

---

## 3️⃣ '새 타이머 시작' 버튼 동작 (상태 해제 및 복귀)

| 항목 | 동작 제약 조건 |
|:---|:---|
| **상태 잠금 해제** | **'새 타이머 시작'** 버튼 클릭 시, 다른 어떤 로직보다 먼저 **만료 상태 플래그를 `false`로 설정**하여 상태 잠금을 해제해야 합니다. |
| **광고 배제** | 이 동작에서는 **전면 광고를 절대 노출해서는 안 됩니다.** |
| **UI 이동** | 잠금 해제 후, **타이머 설정 화면**으로 이동해야 합니다. (이후 사용자가 1번째 탭으로 돌아오면 일반 설정 UI가 표시됨) |

### 구현 상세

#### 3.1 상태 해제 로직
```kotlin
onNewTimerStart = {
    Log.d("NavGraph", "새 타이머 시작 -> 만료 상태 해제 및 Start 화면으로 이동")
    
    // [중요] 만료 상태 해제 (이 버튼이 유일한 해제 경로)
    TimerStateRepository.setTimerFinished(false)
    TimerStateRepository.setTimerActive(false)
    
    Log.d("NavGraph", "만료 상태 해제 완료: isFinished=false")
    
    navController.navigate(Screen.Start.route) {
        popUpTo(Screen.Finished.route) { inclusive = true }
        launchSingleTop = true
    }
}
```

#### 3.2 동작 흐름
```
[새 타이머 시작 버튼 클릭]
    ↓
[만료 상태 플래그: false 설정] ← 유일한 해제 경로
    ↓
[타이머 상태 초기화]
    ↓
[Start 화면으로 이동] ← 광고 없음
    ↓
[이후 1번째 탭 클릭 시 일반 UI 표시]
```

---

## 🎨 UI 구성

### FinishedScreen (목표 달성 완료 화면)

```
┌─────────────────────────┐
│                         │
│         ✓ 아이콘         │
│                         │
│   🎉 목표 달성 완료!     │
│                         │
│     축하합니다!         │
│  금주 목표를 성공적으로  │
│    완료했습니다.        │
│                         │
│  ┌─────────────────┐   │
│  │   결과 확인      │   │  ← 광고 O → 2번째 탭
│  └─────────────────┘   │
│                         │
│  ┌─────────────────┐   │
│  │ 새 타이머 시작  │   │  ← 광고 X → Start 화면
│  └─────────────────┘   │
│                         │
└─────────────────────────┘
```

---

## 🔍 테스트 시나리오

### 시나리오 1: 타이머 만료 후 1번째 탭 복귀
1. 타이머가 목표 시간에 도달하여 만료 상태(`true`) 진입
2. 사용자가 2번째 탭(Records)으로 이동
3. 사용자가 1번째 탭 아이콘 클릭
4. **예상 결과**: Finished 화면이 표시되어야 함 (Start/Run 화면 X)

### 시나리오 2: Finished 화면에서 1번째 탭 재클릭
1. Finished 화면 표시 중
2. 사용자가 1번째 탭 아이콘을 다시 클릭
3. **예상 결과**: 화면 변화 없이 Finished 화면 유지

### 시나리오 3: 결과 확인 버튼 → 광고 → 완료 기록 상세 화면
1. Finished 화면에서 "결과 확인" 버튼 클릭
2. 광고 정책 체크 통과 → 전면 광고 노출
3. 광고 종료
4. **예상 결과**: 완료된 기록의 상세 화면(Detail)으로 자동 이동
5. 만료 상태 플래그는 여전히 `true`
6. 백 버튼으로 돌아가면 Finished 화면 표시

### 시나리오 4: 새 타이머 시작 버튼 → Start 화면
1. Finished 화면에서 "새 타이머 시작" 버튼 클릭
2. **예상 결과**:
   - 광고 노출 없음
   - 만료 상태 플래그 `false`로 변경
   - Start 화면으로 이동
3. 이후 1번째 탭 클릭 시 일반 타이머 설정 UI 표시

---

## 📝 구현 체크리스트

- [x] `FinishedScreen.kt` - 목표 달성 완료 UI 구현
- [x] `NavGraph.kt` - "결과 확인" 버튼 로직 구현 (광고 → Records 이동)
- [x] `NavGraph.kt` - "새 타이머 시작" 버튼 로직 구현 (상태 해제 → Start 이동)
- [x] `BottomNavBar.kt` - 1번째 탭 클릭 시 만료 상태 확인 로직 추가
- [x] `BottomNavBar.kt` - Finished 화면을 1번째 탭 그룹에 포함
- [x] 빌드 검증 완료

---

## 🚀 배포 전 확인사항

1. **광고 정책 준수**: `AdPolicyManager.shouldShowInterstitialAd()` 로직 검증
2. **상태 영속성**: 앱 재시작 시에도 만료 상태 플래그가 유지되는지 확인
3. **백 버튼 동작**: Finished 화면에서 백 버튼 동작 정의 필요 여부 검토
4. **애널리틱스**: 각 버튼 클릭 이벤트 추적 추가 고려

---

## 📚 관련 문서

- `TIMER_FINISHED_STATE_LOCK_GUIDE.md` - 타이머 완료 상태 잠금 가이드 (기존)
- `AD_ARCHITECTURE_REFACTORING_REPORT.md` - 광고 아키텍처 리팩토링 리포트
- `SEQUENTIAL_EXECUTION_GUIDE.md` - 순차 실행 가이드

---

## 📱 '모든 기록 보기' 화면 UI/UX 개선 사항 (2025-01-02 추가)

### 변경 내용 요약

| 변경 전 | 변경 후 |
|:---|:---|
| 우측 상단 'X' 버튼 | 점 3개 메뉴 (MoreVert) |
| 전체 삭제 버튼 직접 노출 | 메뉴 내 "모든 기록 삭제" 옵션 |
| 기록 추가 기능 없음 | FAB (+ 버튼) 추가 |

### UI 구성

```
┌─────────────────────────┐
│  ← 모든 기록        ⋮   │ ← 점 3개 메뉴
├─────────────────────────┤
│                         │
│   [기록 카드 1]         │
│   [기록 카드 2]         │
│   [기록 카드 3]         │
│        ...              │
│                         │
│                    ┌─┐  │
│                    │+│  │ ← FAB (기록 추가)
└────────────────────┴─┘──┘
```

### 구현 상세

#### 1. Floating Action Button (FAB)
- **위치**: 화면 우측 하단
- **아이콘**: `Icons.Filled.Add` (+ 모양)
- **기능**: Tab02의 기록 추가 기능과 동일 (`onAddRecord` 콜백 연결)
- **스타일**: Primary 색상, 스크롤 시에도 항상 최상단 표시

#### 2. 상단 앱바 메뉴
- **변경**: 'X' 버튼 → '점 3개' 아이콘 (`Icons.Filled.MoreVert`)
- **메뉴 항목**: "모든 기록 삭제"
- **동작**: 메뉴 클릭 → 확인 다이얼로그 표시 → 전체 삭제 로직 실행
- **안전 장치**: 삭제 전 확인 다이얼로그 유지

### String 리소스 추가

**한국어** (`values/strings.xml`):
```xml
<string name="cd_more_options">더보기</string>
<string name="menu_delete_all_records">모든 기록 삭제</string>
<string name="cd_add_record">기록 추가</string>
```

**영어** (`values-en/strings.xml`):
```xml
<string name="cd_more_options">More options</string>
<string name="menu_delete_all_records">Delete all records</string>
<string name="cd_add_record">Add record</string>
```

### 수정된 파일

1. **`AllRecords.kt`** - UI 구조 변경 (FAB 추가, 메뉴 변경)
2. **`NavGraph.kt`** - `onAddRecord` 콜백 연결
3. **`strings.xml` (한국어/영어)** - 필요한 string 리소스 추가

---

**작성일**: 2025-01-02  
**작성자**: GitHub Copilot (유지보수 담당 시니어 개발자)  
**버전**: 1.1.0

