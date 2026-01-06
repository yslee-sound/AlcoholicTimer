# Undo 기능 리팩토링 이력 (2026-01-05)

## 📋 목차
1. [원래 구현 방식 (v1.1.6)](#원래-구현-방식-v116)
2. [리팩토링 중 발생한 문제](#리팩토링-중-발생한-문제)
3. [최종 해결 방안 (낙관적 업데이트)](#최종-해결-방안-낙관적-업데이트)
4. [코드 비교](#코드-비교)

---

## 원래 구현 방식 (v1.1.6)

### 🎯 핵심 아이디어: 직접 리스트 조작

**커밋**: `2ba152d` (rel 1.1.6 newUX 0.75)

원래 방식은 **Firestore와 독립적으로** 로컬 리스트를 직접 조작하는 단순한 구조였습니다.

### 📝 원래 코드

#### `hidePost(post: Post)` - 원래 방식
```kotlin
fun hidePost(post: Post) {
    // 1) 숨김 ID 목록에 추가
    _hiddenPostIds.value = _hiddenPostIds.value + post.id
    
    // 2) _posts 리스트에서 제거 (UI에서 즉시 사라짐)
    _posts.value = _posts.value.filter { it.id != post.id }
    
    // 3) Undo를 위해 Post 객체 전체 저장
    _recentlyHiddenPosts.value = _recentlyHiddenPosts.value + (post.id to post)
    
    // 4) (선택사항) Firestore에도 반영
    // ...
}
```

#### `undoHidePost(postId: String)` - 원래 방식
```kotlin
fun undoHidePost(postId: String) {
    // 1) 숨김 상태에서 제거
    _hiddenPostIds.value = _hiddenPostIds.value - postId

    // 2) 임시 저장된 Post를 _posts에 다시 추가 (UI에 즉시 나타남)
    val restoredPost = _recentlyHiddenPosts.value[postId]
    if (restoredPost != null) {
        _posts.value = listOf(restoredPost) + _posts.value  // 최상단에 삽입
        _recentlyHiddenPosts.value = _recentlyHiddenPosts.value - postId
    }
}
```

### ✅ 원래 방식의 장점

1. **단순함**: `_posts` 리스트를 직접 조작
2. **즉시 반응**: Firestore와 무관하게 UI 즉시 변경
3. **신뢰성**: `_recentlyHiddenPosts`에 Post 전체 저장으로 Undo 보장
4. **Race Condition 없음**: 모든 작업이 동기적

### ❌ 원래 방식의 문제점

1. **필터링 로직 무시**: `executeFiltering()`을 사용하지 않아 필터링 규칙 적용 안 됨
2. **Firestore와 동기화 어려움**: 로컬 리스트와 서버 데이터 불일치 가능
3. **확장성 부족**: 복잡한 필터링(시간 만료, 신고 등)을 추가하기 어려움

---

## 리팩토링 중 발생한 문제

### 📅 리팩토링 배경 (2026-01-05)

프로젝트가 커지면서 다음 기능들이 추가됨:
- `executeFiltering()`: 시간 기반 만료, 신고, 숨김 등 복합 필터링
- `_cachedPostList`: Firestore 실시간 동기화
- 필터링 기준 복잡화

이에 따라 **직접 리스트 조작 방식**에서 **필터링 기반 방식**으로 전환 시도

### ❌ 잘못된 리팩토링 시도 #1

```kotlin
fun hidePost(post: Post) {
    viewModelScope.launch {
        _hiddenPostIds.value = _hiddenPostIds.value + post.id
        
        // ❌ executeFiltering() 호출 누락!
        
        // Firestore 업데이트
        postRef.update("deleteAt", Timestamp.now()).await()
        
        // ❌ await() 이후에 저장 → Race Condition!
        _recentlyHiddenPosts.value = _recentlyHiddenPosts.value + (post.id to post)
    }
}

fun undoHidePost(postId: String) {
    viewModelScope.launch {
        val post = _recentlyHiddenPosts.value[postId] ?: return@launch
        _recentlyHiddenPosts.value = _recentlyHiddenPosts.value - postId
        _hiddenPostIds.value = _hiddenPostIds.value - postId
        
        // ❌ executeFiltering() 호출 누락!
        
        postRef.update("deleteAt", null).await()
    }
}
```

### 🐛 발생한 문제들

#### 문제 1: UI가 갱신되지 않음
- **증상**: X 버튼을 눌러도 게시글이 화면에 그대로 남음
- **원인**: `_hiddenPostIds` 변경 후 `executeFiltering()` 호출 누락
- **로그**:
  ```
  HideDebug: 숨김 후 _hiddenPostIds: [abc123]  ✅
  PostFilterDebug: 글ID: abc123... 숨김됨?: true → 최종결과: 보여줌(O)  ❌
  ```

#### 문제 2: Race Condition (타이밍 버그)
- **증상**: 사용자가 빠르게 Undo를 누르면 복원 실패
- **원인**: `_recentlyHiddenPosts` 저장이 `await()` 이후에 실행됨
- **타임라인**:
  ```
  T+0ms:   X 버튼 클릭
  T+1ms:   _hiddenPostIds 업데이트
  T+2ms:   Firestore 요청 시작
  T+100ms: 사용자가 Undo 클릭 ← 이 시점에 _recentlyHiddenPosts 비어있음!
  T+500ms: Firestore 응답 완료
  T+501ms: _recentlyHiddenPosts 저장 (너무 늦음!)
  ```

#### 문제 3: Firestore `null` 타입 오류
- **증상**: `postRef.update("deleteAt", null)` 컴파일 에러
- **원인**: `Post.deleteAt`이 `Timestamp` (non-nullable)
- **해결**: `FieldValue.delete()` 사용

---

## 최종 해결 방안 (낙관적 업데이트)

### 🎯 핵심 아이디어: Optimistic Update Pattern

**참고**: Facebook, Twitter 등 현대 앱에서 사용하는 표준 패턴

### 원칙

1. **로컬 상태를 먼저 업데이트** (네트워크 대기 X)
2. **UI 즉시 갱신** (`executeFiltering()` 즉시 호출)
3. **백그라운드로 서버 동기화**
4. **실패 시 자동 롤백** (Rollback)

### 📝 최종 코드

#### `hidePost(post: Post)` - 최종 버전

```kotlin
fun hidePost(post: Post) {
    viewModelScope.launch {
        // [OPTIMISTIC UPDATE] 1단계: 로컬 상태 즉시 변경
        _hiddenPostIds.value = _hiddenPostIds.value + post.id
        _recentlyHiddenPosts.value = _recentlyHiddenPosts.value + (post.id to post)  // ✅ 즉시!
        
        // [UI 즉시 갱신] executeFiltering() 호출
        executeFiltering()

        // [ASYNC] 2단계: Firestore 요청 (백그라운드)
        val firestore = FirebaseFirestore.getInstance()
        val postRef = firestore.collection("posts").document(post.id)

        try {
            postRef.update("deleteAt", Timestamp.now()).await()
            Log.d("CommunityViewModel", "게시글 숨기기 성공: ${post.id}")
        } catch (e: Exception) {
            Log.e("CommunityViewModel", "게시글 숨기기 실패 - 롤백", e)
            
            // [ROLLBACK] 실패 시 로컬 상태 원복
            _hiddenPostIds.value = _hiddenPostIds.value - post.id
            _recentlyHiddenPosts.value = _recentlyHiddenPosts.value - post.id
            
            // [UI 롤백 반영]
            executeFiltering()
        }
    }
}
```

#### `undoHidePost(postId: String)` - 최종 버전

```kotlin
fun undoHidePost(postId: String) {
    viewModelScope.launch {
        // [DATA 검증]
        val post = _recentlyHiddenPosts.value[postId]
        if (post == null) {
            Log.e("CommunityViewModel", "Undo 실패: 데이터 없음")
            return@launch
        }

        // [OPTIMISTIC UPDATE] 1단계: 로컬 상태 즉시 복원
        _hiddenPostIds.value = _hiddenPostIds.value - postId
        _recentlyHiddenPosts.value = _recentlyHiddenPosts.value - postId
        
        // [UI 즉시 갱신]
        executeFiltering()

        // [ASYNC] 2단계: Firestore 요청 (백그라운드)
        val firestore = FirebaseFirestore.getInstance()
        val postRef = firestore.collection("posts").document(postId)

        try {
            postRef.update("deleteAt", FieldValue.delete()).await()  // ✅ null 대신 FieldValue.delete()
            Log.d("CommunityViewModel", "게시글 복원 성공: $postId")
        } catch (e: Exception) {
            Log.e("CommunityViewModel", "게시글 복원 실패 - 롤백", e)
            
            // [ROLLBACK] 실패 시 로컬 상태 원복
            _hiddenPostIds.value = _hiddenPostIds.value + postId
            _recentlyHiddenPosts.value = _recentlyHiddenPosts.value + (postId to post)
            
            // [UI 롤백 반영]
            executeFiltering()
        }
    }
}
```

### ✅ 최종 방식의 장점

1. **즉시 반응**: UI 변경이 네트워크 속도와 무관
2. **Race Condition 제거**: `_recentlyHiddenPosts` 즉시 저장
3. **executeFiltering() 통합**: 복잡한 필터링 규칙 적용
4. **안정적 Rollback**: 네트워크 오류 시 자동 원복
5. **사용자 경험 극대화**: 네이티브 앱 수준의 반응성

### 📊 성능 비교

| 방식 | UI 반응 시간 | Race Condition | 필터링 지원 | 복잡도 |
|------|-------------|----------------|------------|--------|
| **원래 (v1.1.6)** | 즉시 | 없음 | ❌ | 낮음 |
| **잘못된 리팩토링** | 500ms+ | 발생 | ⚠️ | 중간 |
| **최종 (낙관적 업데이트)** | 즉시 | 없음 | ✅ | 중간 |

---

## 코드 비교

### 타임라인 비교

#### 원래 방식 (v1.1.6)
```
T+0ms:  X 버튼 클릭
T+1ms:  _posts 리스트에서 제거 → UI 즉시 변경 ⚡
T+2ms:  _recentlyHiddenPosts 저장
완료!
```

#### 잘못된 리팩토링
```
T+0ms:   X 버튼 클릭
T+1ms:   _hiddenPostIds 업데이트
T+2ms:   Firestore 요청 시작
T+500ms: Firestore 완료
T+501ms: _recentlyHiddenPosts 저장
T+502ms: UI 변경 (늦음!) ❌
```

#### 최종 방식 (낙관적 업데이트)
```
T+0ms:  X 버튼 클릭
T+1ms:  _hiddenPostIds, _recentlyHiddenPosts 업데이트 ⚡
T+2ms:  executeFiltering() → UI 즉시 변경 ⚡
T+3ms:  Firestore 요청 시작 (백그라운드)
T+500ms: Firestore 완료 ✅
```

### 핵심 차이점

| 단계 | 원래 방식 | 잘못된 리팩토링 | 최종 방식 |
|------|-----------|----------------|-----------|
| **상태 업데이트** | `_posts` 직접 조작 | `_hiddenPostIds` 업데이트 | `_hiddenPostIds` + `_recentlyHiddenPosts` 즉시 업데이트 |
| **UI 갱신** | 리스트 변경으로 자동 | ❌ 누락 | `executeFiltering()` 즉시 호출 |
| **Undo 데이터 저장** | 즉시 | ❌ await() 후 (늦음) | ✅ 즉시 |
| **Firestore 동기화** | 선택사항 | await() | 백그라운드 (await()) |
| **Rollback** | 불필요 | ❌ 없음 | ✅ 자동 |

---

## 학습 포인트

### 1. Optimistic Update Pattern

현대 앱의 표준 패턴:
- **즉시 반응**: 사용자 행동에 즉각 반응
- **비동기 동기화**: 서버와 백그라운드로 동기화
- **자동 롤백**: 실패 시 원복

### 2. Race Condition 방지

**원칙**: 사용자가 접근할 수 있는 데이터는 비동기 작업 **전에** 준비

```kotlin
// ❌ 나쁜 예
await someNetworkCall()  // 시간 걸림
userData = ... // 사용자가 이미 다음 액션 실행 가능

// ✅ 좋은 예
userData = ...  // 즉시 준비
await someNetworkCall()  // 백그라운드
```

### 3. UI 갱신 타이밍

**원칙**: 상태 변경 직후 UI 갱신 함수 호출

```kotlin
// ❌ 나쁜 예
_state.value = newValue
// executeFiltering() 호출 없음

// ✅ 좋은 예
_state.value = newValue
executeFiltering()  // 즉시 호출
```

---

## 관련 파일

### 수정된 파일
- `app/src/main/java/kr/sweetapps/alcoholictimer/ui/tab_03/viewmodel/CommunityViewModel.kt`
  - `hidePost(post: Post)` (610~632줄)
  - `hidePost(postId: String)` (634~658줄)
  - `undoHidePost(postId: String)` (660~688줄)

- `app/src/main/java/kr/sweetapps/alcoholictimer/ui/tab_03/CommunityScreen.kt`
  - `onHideClick` 이벤트 핸들러 (478줄)
  - 변경: `viewModel.hidePost(item.id)` → `viewModel.hidePost(item)`

### 참고 문서
- [Git 커밋 2ba152d](../../commit/2ba152d) - 원래 Undo 기능 구현
- [executeFiltering() 설계](./FILTERING_DESIGN.md)
- [낙관적 업데이트 패턴](https://redux.js.org/usage/optimistic-updates)

---

## 요약

### Before (원래 방식)
- ✅ 단순하고 즉각적
- ❌ 필터링 로직 무시

### After (최종 방식)
- ✅ 즉각적 반응성 유지
- ✅ 복잡한 필터링 지원
- ✅ Race Condition 제거
- ✅ 안정적 Rollback

**결론**: 원래 방식의 **즉시 반응 장점**을 유지하면서, **현대적인 필터링 아키텍처**와 통합하는 데 성공!

---

*작성일: 2026-01-05*  
*작성자: GitHub Copilot (with Human Developer)*  
*버전: v1.2.8 refactor 0.12*

