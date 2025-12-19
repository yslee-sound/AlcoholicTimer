# 📋 탭 구조 최종 정리 계획서

**작성일**: 2025-12-20  
**목표**: Tab 3으로 커뮤니티와 설정 통합

---

## 🎯 최종 목표 구조

```
Tab 1: Timer (타이머)
Tab 2: Records (나의 건강 분석)
Tab 3: Community (커뮤니티) ← 메인 화면
  └─ Settings 버튼 (우측 상단 톱니바퀴)
      └─ 모든 설정 화면들
```

---

## 📂 현재 상태 분석

### Tab 04 폴더 (tab_04/)
```
tab_04/
├── Tab04.kt (HabitActivity - 습관 설정 화면)
├── community/
│   ├── CommunityScreen.kt  ← 커뮤니티 메인
│   └── PostItem.kt
├── screens/
│   └── CurrencyScreen.kt (통화 설정)
└── viewmodel/
    ├── Tab04ViewModel.kt (습관 설정용)
    └── CommunityViewModel.kt
```

**혼재된 내용:**
- ✅ 커뮤니티 기능 (`community/` 폴더)
- ✅ 습관 설정 (Tab04.kt = HabitActivity)
- ✅ 통화 설정 (CurrencyScreen.kt)

### Tab 05 폴더 (tab_05/)
```
tab_05/
├── Tab05.kt (AboutScreen - 앱 정보)
├── screens/
│   ├── NicknameEditScreen.kt
│   ├── CustomerScreen.kt
│   ├── NotificationListScreen.kt
│   ├── debug/
│   │   ├── DebugScreen.kt
│   │   └── DemoData.kt
│   └── policy/
│       └── DocumentScreen.kt
├── components/
│   ├── AvatarSelectionDialog.kt
│   └── CustomerFeedbackBottomSheet.kt
└── viewmodel/
    ├── Tab05ViewModel.kt
    ├── DebugScreenViewModel.kt
    ├── CustomerScreenViewModel.kt
    ├── NotificationViewModel.kt
    └── DocumentViewModel.kt
```

**내용:**
- ✅ About (앱 정보)
- ✅ 닉네임 편집
- ✅ 고객 지원
- ✅ 알림
- ✅ 디버그
- ✅ 개인정보 정책

---

## 🎯 최종 목표 구조

```
ui/
├── tab_01/          # Timer (변경 없음)
├── tab_02/          # Records (변경 없음)
└── tab_03/          # Community (신규 생성 - 모든 기능 통합)
    ├── CommunityScreen.kt        ← tab_04/community/ 에서 이동
    ├── PostItem.kt                ← tab_04/community/ 에서 이동
    │
    ├── settings/                  ← 모든 설정 화면 통합
    │   ├── AboutScreen.kt         ← tab_05/Tab05.kt 이름 변경
    │   ├── HabitSettingsScreen.kt ← tab_04/Tab04.kt 이름 변경
    │   ├── CurrencyScreen.kt      ← tab_04/screens/ 에서 이동
    │   ├── NicknameEditScreen.kt  ← tab_05/screens/ 에서 이동
    │   ├── CustomerScreen.kt      ← tab_05/screens/ 에서 이동
    │   ├── NotificationListScreen.kt ← tab_05/screens/ 에서 이동
    │   ├── debug/                 ← tab_05/screens/debug/ 전체 이동
    │   │   ├── DebugScreen.kt
    │   │   └── DemoData.kt
    │   └── policy/                ← tab_05/screens/policy/ 전체 이동
    │       └── DocumentScreen.kt
    │
    ├── components/                ← tab_05/components/ 전체 이동
    │   ├── AvatarSelectionDialog.kt
    │   └── CustomerFeedbackBottomSheet.kt
    │
    └── viewmodel/
        ├── CommunityViewModel.kt  ← tab_04/viewmodel/ 에서 이동
        ├── HabitSettingsViewModel.kt ← tab_04/viewmodel/Tab04ViewModel.kt 이름 변경
        ├── AboutViewModel.kt      ← tab_05/viewmodel/Tab05ViewModel.kt 이름 변경
        ├── DebugScreenViewModel.kt ← tab_05/viewmodel/ 에서 이동
        ├── CustomerScreenViewModel.kt ← tab_05/viewmodel/ 에서 이동
        ├── NotificationViewModel.kt ← tab_05/viewmodel/ 에서 이동
        └── DocumentViewModel.kt   ← tab_05/viewmodel/ 에서 이동
```

---

## 📝 상세 이동 계획

### Phase 1: tab_03 폴더 생성 및 커뮤니티 메인 이동

#### 1.1 폴더 생성
```
ui/ 우클릭 → New → Package → "tab_03" 입력
```

#### 1.2 커뮤니티 메인 화면 이동
| 원본 | 목적지 |
|------|--------|
| `tab_04/community/CommunityScreen.kt` | `tab_03/CommunityScreen.kt` |
| `tab_04/community/PostItem.kt` | `tab_03/PostItem.kt` |

**방법:**
- Android Studio에서 파일 드래그 → `tab_03/` 폴더로 이동
- "Refactor" 선택하면 자동으로 import 경로 수정

#### 1.3 커뮤니티 ViewModel 이동
| 원본 | 목적지 |
|------|--------|
| `tab_04/viewmodel/CommunityViewModel.kt` | `tab_03/viewmodel/CommunityViewModel.kt` |

---

### Phase 2: settings 하위 폴더 생성

#### 2.1 폴더 생성
```
tab_03/ 우클릭 → New → Package → "settings" 입력
```

---

### Phase 3: 습관/통화 설정 이동 (tab_04 → tab_03/settings)

#### 3.1 습관 설정 이동 및 이름 변경
| 원본 | 목적지 |
|------|--------|
| `tab_04/Tab04.kt` | `tab_03/settings/HabitSettingsScreen.kt` |
| `tab_04/viewmodel/Tab04ViewModel.kt` | `tab_03/viewmodel/HabitSettingsViewModel.kt` |

**주의사항:**
- `Tab04.kt` 파일 내부에 `HabitActivity` 클래스와 `HabitScreen` 컴포저블이 있음
- 파일명만 변경하고 클래스명은 유지해도 됨
- 또는 클래스명도 `HabitSettingsActivity`, `HabitSettingsScreen`으로 변경 가능

#### 3.2 통화 설정 이동
| 원본 | 목적지 |
|------|--------|
| `tab_04/screens/CurrencyScreen.kt` | `tab_03/settings/CurrencyScreen.kt` |

---

### Phase 4: About 및 기타 설정 이동 (tab_05 → tab_03/settings)

#### 4.1 메인 About 화면 이동
| 원본 | 목적지 |
|------|--------|
| `tab_05/Tab05.kt` | `tab_03/settings/AboutScreen.kt` |
| `tab_05/viewmodel/Tab05ViewModel.kt` | `tab_03/viewmodel/AboutViewModel.kt` |

#### 4.2 설정 화면들 이동
| 원본 | 목적지 |
|------|--------|
| `tab_05/screens/NicknameEditScreen.kt` | `tab_03/settings/NicknameEditScreen.kt` |
| `tab_05/screens/CustomerScreen.kt` | `tab_03/settings/CustomerScreen.kt` |
| `tab_05/screens/NotificationListScreen.kt` | `tab_03/settings/NotificationListScreen.kt` |

#### 4.3 디버그 폴더 통째로 이동
```
tab_05/screens/debug/ → tab_03/settings/debug/
```
- `DebugScreen.kt`
- `DemoData.kt`

#### 4.4 정책 폴더 통째로 이동
```
tab_05/screens/policy/ → tab_03/settings/policy/
```
- `DocumentScreen.kt`

#### 4.5 컴포넌트 폴더 통째로 이동
```
tab_05/components/ → tab_03/components/
```
- `AvatarSelectionDialog.kt`
- `CustomerFeedbackBottomSheet.kt`

#### 4.6 ViewModel들 이동
| 원본 | 목적지 |
|------|--------|
| `tab_05/viewmodel/DebugScreenViewModel.kt` | `tab_03/viewmodel/DebugScreenViewModel.kt` |
| `tab_05/viewmodel/CustomerScreenViewModel.kt` | `tab_03/viewmodel/CustomerScreenViewModel.kt` |
| `tab_05/viewmodel/NotificationViewModel.kt` | `tab_03/viewmodel/NotificationViewModel.kt` |
| `tab_05/viewmodel/DocumentViewModel.kt` | `tab_03/viewmodel/DocumentViewModel.kt` |

---

### Phase 5: 빈 폴더 삭제

#### 5.1 tab_04 폴더 삭제
모든 파일 이동 후:
```
tab_04/ 우클릭 → Delete
```

#### 5.2 tab_05 폴더 삭제
모든 파일 이동 후:
```
tab_05/ 우클릭 → Delete
```

---

## 🔧 Android Studio 작업 순서 (단계별)

### Step 1: tab_03 폴더 생성
1. `ui` 폴더 우클릭
2. **New → Package**
3. `tab_03` 입력

### Step 2: 커뮤니티 파일 이동
1. `tab_04/community/CommunityScreen.kt` 선택
2. **드래그 앤 드롭** → `tab_03/`
3. "Move" 대화상자에서 **Refactor** 선택
4. 동일하게 `PostItem.kt` 이동

### Step 3: settings 폴더 생성
1. `tab_03` 폴더 우클릭
2. **New → Package**
3. `settings` 입력

### Step 4: 습관 설정 이동
1. `tab_04/Tab04.kt` 선택
2. **드래그 앤 드롭** → `tab_03/settings/`
3. **Refactor** 선택
4. 파일 우클릭 → **Refactor → Rename** → `HabitSettingsScreen.kt` 입력

### Step 5: About 및 기타 파일들 이동
1. `tab_05/Tab05.kt` → `tab_03/settings/` 드래그
2. 이름 변경: `AboutScreen.kt`
3. 나머지 파일들도 동일하게 이동

### Step 6: 폴더 통째로 이동
1. `tab_05/screens/debug/` 폴더 선택
2. **드래그 앤 드롭** → `tab_03/settings/`
3. 동일하게 `policy/`, `components/` 폴더 이동

### Step 7: ViewModel 정리
1. `tab_03/viewmodel/` 폴더 생성 (없다면)
2. tab_04, tab_05의 ViewModel들을 모두 `tab_03/viewmodel/`로 이동

### Step 8: 빈 폴더 삭제
1. `tab_04` 우클릭 → **Delete**
2. `tab_05` 우클릭 → **Delete**

---

## ⚠️ 주의사항

### 1. Refactor 사용 필수
- 파일 이동 시 반드시 **"Refactor"** 옵션 선택
- 이렇게 해야 import 경로가 자동으로 수정됨

### 2. 빌드 확인
각 Phase마다 빌드 테스트:
```
Build → Rebuild Project
```

### 3. 패키지명 확인
파일 이동 후 상단 `package` 선언 확인:
```kotlin
// 예시
package kr.sweetapps.alcoholictimer.ui.tab_03.settings
```

### 4. Git 커밋
각 Phase마다 커밋:
```bash
git add .
git commit -m "refactor: Phase 1 - 커뮤니티 메인 화면 tab_03으로 이동"
```

---

## 🧪 검증 체크리스트

### 빌드 확인
- [ ] `./gradlew assembleDebug` 성공
- [ ] `./gradlew assembleRelease` 성공

### 네비게이션 확인
- [ ] Tab 1 → Tab 2 → Tab 3 이동 정상
- [ ] Tab 3 커뮤니티 화면 정상 표시
- [ ] Tab 3 → 설정 버튼 → About 화면 진입
- [ ] About 화면에서 모든 하위 메뉴 진입 가능
- [ ] 뒤로가기 버튼 정상 작동

### 기능 확인
- [ ] 커뮤니티 게시글 작성/조회
- [ ] 이미지 업로드
- [ ] 습관 설정 변경 및 저장
- [ ] 통화 설정 변경
- [ ] 닉네임 변경
- [ ] 디버그 메뉴 진입
- [ ] 개인정보 정책 확인

---

## 📊 예상 소요 시간

| Phase | 작업 | 예상 시간 |
|-------|------|----------|
| Phase 1 | 커뮤니티 메인 이동 | 10분 |
| Phase 2 | settings 폴더 생성 | 2분 |
| Phase 3 | 습관/통화 설정 이동 | 10분 |
| Phase 4 | About 및 기타 이동 | 20분 |
| Phase 5 | 빈 폴더 삭제 | 2분 |
| **검증** | 빌드 및 테스트 | 20분 |
| **합계** | - | **약 1시간** |

---

## 🎯 성공 기준

1. ✅ `tab_04`, `tab_05` 폴더 완전히 삭제
2. ✅ `tab_03` 폴더에 모든 기능 통합
3. ✅ 빌드 오류 없음
4. ✅ 모든 화면 네비게이션 정상
5. ✅ 모든 기능 정상 작동

---

## 🚨 롤백 계획

문제 발생 시:

### 즉시 롤백
```bash
git reset --hard HEAD
```

### 특정 Phase만 롤백
```bash
git log --oneline
git revert <commit-hash>
```

---

## 📝 참고사항

### 패키지 경로 변경 요약

| 원본 | 최종 |
|------|------|
| `ui.tab_04.community` | `ui.tab_03` |
| `ui.tab_04` | `ui.tab_03.settings` |
| `ui.tab_04.screens` | `ui.tab_03.settings` |
| `ui.tab_05` | `ui.tab_03.settings` |
| `ui.tab_05.screens` | `ui.tab_03.settings` |
| `ui.tab_05.components` | `ui.tab_03.components` |

### 파일명 변경 권장

| 원본 | 권장 이름 |
|------|----------|
| `Tab04.kt` | `HabitSettingsScreen.kt` |
| `Tab05.kt` | `AboutScreen.kt` |
| `Tab04ViewModel.kt` | `HabitSettingsViewModel.kt` |
| `Tab05ViewModel.kt` | `AboutViewModel.kt` |

---

**작성일**: 2025-12-20  
**작성자**: GitHub Copilot  
**상태**: ✅ 실행 준비 완료

