# 📋 탭 구조 리팩토링 계획서
**작성일**: 2025-12-19  
**완료일**: 2025-12-19  
**상태**: ✅ 완료
**목표**: 기능 손상 없이, 하위 호환성을 유지하면서 탭 구조 정리

---

## 🎯 리팩토링 목표

### 변경 사항 요약
```
Before (현재):
Tab 1: Timer (Start/Run)
Tab 2: Records + LevelDetail (하위메뉴)
Tab 3: Level (삭제됨, 현재는 Tab 2의 하위메뉴)
Tab 4: More (커뮤니티 - 익명 응원 챌린지)
Tab 5: About (설정)

After (목표):
Tab 1: Timer (Start/Run) - 변경 없음
Tab 2: Records (나의 건강 분석) - 변경 없음
  └─ LevelDetail (레벨 상세) - 유지
Tab 3: Community (익명 응원 챌린지) ← Tab 4에서 이동
  └─ Settings (설정) ← 신규 추가
      ├─ About (정보) ← Tab 5에서 이동
      ├─ HabitSettings ← Tab 4에서 이동
      ├─ Debug ← Tab 4에서 이동
      └─ 기타 모든 설정 화면들
```

---

## 🔍 현재 상태 분석

### 1. 네비게이션 구조 (현재)
```kotlin
// BottomNavBar.kt - 현재 3개 탭
bottomItems = [
  Tab 1: Start (Timer)
  Tab 2: Records (+ LevelDetail 하위)
  Tab 3: More (Community) ← Screen.More.route
]

// NavRoutes.kt - 라우트 정의
Screen.Start       // Tab 1
Screen.Records     // Tab 2
Screen.LevelDetail // Tab 2 하위
Screen.More        // Tab 3 (현재 커뮤니티)
Screen.About       // Tab 3 하위 (설정 버튼으로 진입)
Screen.HabitSettings, Debug, etc. // Tab 3 하위
```

### 2. 현재 파일 구조
```
ui/
├─ tab_01/  (Timer - 변경 없음)
├─ tab_02/  (Records - 변경 없음)
├─ tab_03/  (Level - 삭제됨, 현재 사용 안 함)
├─ tab_04/  (Community - 현재 More 탭)
│   ├─ CommunityScreen.kt ✅
│   ├─ PostItem.kt ✅
│   └─ viewmodel/
└─ tab_05/  (About/Settings - 현재 More 탭의 하위)
    └─ Tab05.kt (AboutScreen)
```

---

## 📐 리팩토링 전략 (하위 호환성 보장)

### 원칙
1. ✅ **라우트명 유지**: 기존 `Screen.More`, `Screen.About` 등 라우트명은 절대 변경하지 않음
2. ✅ **파일 구조 점진적 변경**: 기존 파일 위치를 유지하면서 점진적으로 정리
3. ✅ **기능 보존**: 모든 기존 기능은 그대로 작동해야 함
4. ✅ **데이터 호환성**: SharedPreferences, Firestore 등 데이터 구조 변경 없음

---

## 📝 단계별 실행 계획

### Phase 1: 네비게이션 라우트 정리 (개념적 재배치)
**목표**: 라우트의 의미를 명확히 하고, 주석으로 새 구조 표시

#### 1.1. `NavRoutes.kt` 주석 업데이트
```kotlin
// [REFACTORING] Tab 구조 재정의 (2025-12-19)
// Tab 1: Timer (Start/Run)
// Tab 2: Records + LevelDetail
// Tab 3: Community (More) + Settings (About)

/**
 * [REFACTORED] More screen - 이제 "커뮤니티(익명 응원 챌린지)" 메인 화면
 * 기존: Tab 4 (More)
 * 신규: Tab 3 (Community)
 * 라우트명 변경 없음 - 하위 호환성 유지
 */
data object More : Screen("more")  // 라우트명 유지!

/**
 * [REFACTORED] About screen - 이제 "설정" 진입점
 * 기존: Tab 5 (독립 탭)
 * 신규: Tab 3 하위 메뉴 (설정 버튼 클릭 시)
 * 라우트명 변경 없음 - 하위 호환성 유지
 */
data object About : Screen("about")  // 라우트명 유지!
```

**영향**: 없음 (주석만 변경)  
**하위 호환성**: ✅ 완벽 (라우트명 동일)

---

### Phase 2: 파일 및 폴더 이름 변경 (점진적)
**목표**: 폴더명을 새 구조에 맞게 정리하되, 빌드 오류 방지

#### 2.1. 폴더명 변경 계획
```
Before (현재):              After (목표):
ui/tab_01/  (Timer)   →    ui/tab_01/  (Timer) - 변경 없음
ui/tab_02/  (Records) →    ui/tab_02/  (Records) - 변경 없음
ui/tab_03/  (Level)   →    [삭제 예정] - 현재 사용 안 함
ui/tab_04/  (More)    →    ui/tab_03/  (Community) ← 이름 변경
ui/tab_05/  (About)   →    ui/tab_03/settings/  ← 하위 폴더로 이동
```

#### 2.2. 실행 순서 (안전한 변경)
```
Step 1: tab_04 → tab_03_community (임시 이름)
Step 2: tab_05 → tab_03/settings
Step 3: tab_03_community → tab_03 (최종)
Step 4: 빈 tab_03 폴더 삭제
```

**주의사항**:
- 각 단계마다 빌드 테스트 필수
- import 문 자동 수정 확인
- Git에서 "Refactor > Rename" 사용 권장

**영향**: import 경로 변경  
**하위 호환성**: ✅ 완벽 (컴파일 타임에만 영향, 런타임 영향 없음)

---

### Phase 3: UI 구조 조정
**목표**: 커뮤니티 화면에 "설정" 버튼 추가

#### 3.1. `CommunityScreen.kt` 수정
```kotlin
// [NEW] 상단 TopAppBar에 설정 버튼 추가
TopAppBar(
    title = { Text("익명 응원 챌린지") },
    actions = {
        // 기존 설정 버튼 (우측 상단 톱니바퀴)
        IconButton(onClick = onSettingsClick) {  // ← 이미 존재!
            Icon(
                painter = painterResource(id = R.drawable.gearsix),
                contentDescription = "설정"
            )
        }
    }
)
```

**현재 상태 확인**: `onSettingsClick` 콜백이 이미 존재하는지 확인 필요  
**작업**: 콜백을 `Screen.About.route`로 네비게이션하도록 연결

#### 3.2. 네비게이션 연결
```kotlin
// AppNavHost.kt 또는 CommunityScreen 호출부
CommunityScreen(
    onSettingsClick = {
        navController.navigate(Screen.About.route)
    }
)
```

**영향**: UI에 버튼 하나 추가  
**하위 호환성**: ✅ 완벽 (기존 라우트 그대로 사용)

---

### Phase 4: BottomNavBar 아이콘 및 라벨 업데이트
**목표**: 탭 이름을 "커뮤니티"로 표시

#### 4.1. 리소스 파일 업데이트
```xml
<!-- strings.xml -->
<string name="drawer_menu_more">커뮤니티</string>
<!-- 또는 새로운 키 추가 -->
<string name="tab_community">커뮤니티</string>
```

#### 4.2. `BottomNavBar.kt` 수정
```kotlin
BottomItem(
    Screen.More,  // 라우트는 그대로!
    R.drawable.user,  // 아이콘 유지 또는 변경
    R.string.tab_community,  // ← 라벨만 변경
    R.string.tab_community,
    associatedRoutes = setOf(
        Screen.More.route,
        Screen.About.route,
        Screen.AboutLicenses.route,
        // ... 기존 하위 라우트들
    )
)
```

**영향**: 탭 라벨만 변경  
**하위 호환성**: ✅ 완벽 (라우트 동일)

---

### Phase 5: 문서 및 주석 업데이트
**목표**: 코드베이스 전체의 주석을 새 구조에 맞게 정리

#### 5.1. 업데이트 대상
- `README.md`: 탭 구조 설명 수정
- `docs/`: 관련 문서들 업데이트
- 각 파일의 헤더 주석: "Tab 4" → "Tab 3 (Community)" 등

**영향**: 문서만 변경  
**하위 호환성**: ✅ 완벽

---

## 🧪 테스트 계획

### 1. 빌드 테스트
```powershell
# 각 Phase 후 실행
.\gradlew assembleDebug
.\gradlew assembleRelease
```

### 2. 네비게이션 테스트
- [ ] Tab 1 → Tab 2 → Tab 3 이동 정상
- [ ] Tab 3에서 설정 버튼 클릭 → About 화면 진입
- [ ] About 화면에서 뒤로가기 → Tab 3 복귀
- [ ] Tab 2 → LevelDetail → 뒤로가기 정상
- [ ] 모든 하위 메뉴 진입 및 복귀 정상

### 3. 기능 테스트
- [ ] 커뮤니티 게시글 작성/조회
- [ ] 이미지 업로드
- [ ] 설정 화면 모든 기능 (닉네임, 통화, 습관, 디버그 등)
- [ ] 레벨 화면 조회

### 4. 데이터 무결성 테스트
- [ ] SharedPreferences 읽기/쓰기
- [ ] Firestore 읽기/쓰기
- [ ] 사용자 설정 유지
- [ ] 기존 앱 업데이트 시나리오 (v1.1.5 → v1.1.6)

---

## 🚨 위험 요소 및 대응 방안

### 위험 1: Import 경로 깨짐
**대응**:
- Android Studio의 "Refactor > Rename" 사용
- 단계별 빌드 테스트
- Git의 "Move" 기능 활용 (히스토리 유지)

### 위험 2: 네비게이션 오류
**대응**:
- 라우트명 절대 변경 금지
- `associatedRoutes` 정확히 업데이트
- 각 화면의 `onNavigateBack` 콜백 동작 확인

### 위험 3: 기존 사용자 혼란
**대응**:
- 탭 순서 유지 (1-2-3)
- 익숙한 아이콘 유지 또는 유사한 것 사용
- 첫 실행 시 "새로운 구조" 안내 (선택사항)

---

## 📅 예상 소요 시간

| Phase | 작업 내용 | 예상 시간 | 위험도 |
|-------|----------|----------|--------|
| Phase 1 | 주석 업데이트 | 10분 | 🟢 낮음 |
| Phase 2 | 폴더 이름 변경 | 20분 | 🟡 중간 |
| Phase 3 | UI 구조 조정 | 15분 | 🟢 낮음 |
| Phase 4 | BottomNavBar 업데이트 | 10분 | 🟢 낮음 |
| Phase 5 | 문서 업데이트 | 15분 | 🟢 낮음 |
| **테스트** | 전체 테스트 | 30분 | - |
| **합계** | - | **1시간 40분** | - |

---

## ✅ 체크리스트 (실행 전)

### 사전 준비
- [ ] 현재 코드 Git 커밋 완료
- [ ] 브랜치 생성 (`feature/tab-restructure`)
- [ ] 로컬 빌드 성공 확인
- [ ] 문서 백업 완료

### Phase 별 체크
- [ ] Phase 1 완료 + 빌드 테스트
- [ ] Phase 2 완료 + 빌드 테스트
- [ ] Phase 3 완료 + 빌드 테스트
- [ ] Phase 4 완료 + 빌드 테스트
- [ ] Phase 5 완료

### 최종 확인
- [ ] 전체 빌드 성공 (Debug + Release)
- [ ] 모든 네비게이션 경로 테스트
- [ ] 기존 앱 업데이트 시뮬레이션
- [ ] 문서 최종 검토
- [ ] PR 생성 및 리뷰

---

## 🎯 성공 기준

1. ✅ **빌드**: Debug/Release 모두 빌드 성공
2. ✅ **네비게이션**: 모든 화면 간 이동 정상 작동
3. ✅ **기능**: 커뮤니티, 설정, 레벨 등 모든 기능 정상 작동
4. ✅ **데이터**: 기존 사용자 데이터 손실 없음
5. ✅ **UI**: 탭 레이블/아이콘이 새 구조 반영
6. ✅ **문서**: 모든 문서가 새 구조 반영
7. ✅ **하위 호환**: 이전 버전에서 업데이트 시 오류 없음

---

## 📌 중요 참고 사항

### 절대 변경하지 말아야 할 것
1. ❌ **라우트명**: `Screen.More.route`, `Screen.About.route` 등
2. ❌ **데이터 구조**: Firestore 컬렉션명, SharedPreferences 키
3. ❌ **외부 연동**: Firebase, AdMob 등 설정

### 자유롭게 변경 가능한 것
1. ✅ **폴더명**: `ui/tab_04` → `ui/tab_03`
2. ✅ **주석**: 모든 코드 주석
3. ✅ **UI 텍스트**: 탭 라벨, 버튼 텍스트 등
4. ✅ **문서**: README, docs 등

---

## 🔄 롤백 계획

만약 문제가 발생하면:

### 즉시 롤백
```bash
git reset --hard HEAD
# 또는
git checkout main
```

### 부분 롤백 (특정 Phase만)
```bash
git revert <commit-hash>
```

### 긴급 핫픽스
- 기존 라우트명으로 임시 별칭 추가
- 폴더명 되돌리기
- BottomNavBar 라벨만 원복

---

## 📝 참고 문서

- `docs/update-117/TAB3_UX_COMPLETE.md`: 이전 탭 3 축소 작업 기록
- `docs/update-117/tab3ux.md`: 탭 구조 변경 가이드
- `app/src/main/java/kr/sweetapps/alcoholictimer/ui/components/BottomNavBar.kt`: 현재 네비게이션 바
- `app/src/main/java/kr/sweetapps/alcoholictimer/ui/main/NavRoutes.kt`: 모든 라우트 정의

---

## 🎉 완료 후 작업

1. **Git 커밋**
   ```bash
   git commit -m "refactor: 탭 구조 정리 - Tab 3을 커뮤니티로 재배치 (#issue-number)"
   ```

2. **PR 생성**
   - 제목: `[Refactoring] 탭 구조 정리 (Community를 Tab 3으로, Settings 통합)`
   - 설명: 이 문서 링크 첨부

3. **릴리즈 노트 작성**
   ```markdown
   ### UI 개선
   - 하단 탭 구조 개선: "커뮤니티" 탭 추가
   - 설정 메뉴를 커뮤니티 탭 내부로 이동하여 접근성 향상
   ```

4. **버전 번호 증가**
   - `versionCode`: +1
   - `versionName`: 1.1.6 → 1.1.7 (마이너 업데이트)

---

## 📞 문의 및 지원

이 리팩토링 계획에 대한 질문이나 문제가 발생하면:
1. 이 문서의 "위험 요소 및 대응 방안" 섹션 참조
2. Git 히스토리에서 이전 상태 확인
3. 롤백 계획 실행

---

**작성자**: GitHub Copilot  
**검토 필요**: 실제 적용 전 팀 리뷰 권장  
**최종 수정**: 2025-12-19

