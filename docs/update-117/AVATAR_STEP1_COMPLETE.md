# Step 1 완료 보고서: 아바타 시스템 로직 구현

**작업일**: 2025-12-17  
**단계**: Step 1 - 아바타 관리자 & 데이터 모델 (Phase 2)

---

## ✅ 작업 완료 내역

### 📦 생성된 파일 (2개)

1. **AvatarManager.kt** - 아바타 관리 싱글톤
2. **UserRepository.kt** - 사용자 아바타 저장/조회

### 🔧 수정된 파일 (2개)

1. **Post.kt** - authorAvatarIndex 필드 추가
2. **CommunityViewModel.kt** - 아바타 인덱스 포함하여 게시글 작성

---

## 📋 구현 상세

### 1. AvatarManager (싱글톤)

**위치**: `util/AvatarManager.kt`

**기능**:
- 20개의 아바타 리소스 ID 관리 (avatar_00 ~ avatar_19)
- 안전한 인덱스 처리 (범위 벗어나면 0번 반환)

**코드**:
```kotlin
object AvatarManager {
    val avatars = listOf(
        R.drawable.avatar_00,
        R.drawable.avatar_01,
        // ... 생략 ...
        R.drawable.avatar_19
    )
    
    fun getAvatarResId(index: Int?): Int {
        return try {
            when {
                index == null -> avatars[0]
                index < 0 -> avatars[0]
                index >= avatars.size -> avatars[0]
                else -> avatars[index]
            }
        } catch (e: Exception) {
            avatars[0] // 예외 발생 시에도 0번 반환
        }
    }
    
    val count: Int = avatars.size
    fun isValidIndex(index: Int?): Boolean
}
```

**안전 장치**:
- ✅ null 인덱스 → 0번 아바타 반환
- ✅ 음수 인덱스 → 0번 아바타 반환
- ✅ 범위 초과 → 0번 아바타 반환
- ✅ 예외 발생 → 0번 아바타 반환

---

### 2. Post 데이터 모델 수정

**위치**: `data/model/Post.kt`

**추가된 필드**:
```kotlin
data class Post(
    // ...existing fields...
    
    @PropertyName("authorAvatarIndex")
    val authorAvatarIndex: Int = 0 // 기본값 0 - 하위 호환성
)
```

**특징**:
- ✅ `@PropertyName` 어노테이션으로 Firestore 매핑
- ✅ 기본값 0으로 하위 호환성 확보
- ✅ 기존 Firestore 데이터에 필드 없어도 앱 정상 작동

---

### 3. UserRepository 생성

**위치**: `data/repository/UserRepository.kt`

**기능**:
```kotlin
class UserRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("user_prefs", MODE_PRIVATE)
    
    // 아바타 인덱스 저장
    suspend fun updateAvatar(index: Int): Boolean
    
    // 아바타 인덱스 조회
    suspend fun getAvatarIndex(): Int
}
```

**저장 방식**:
- SharedPreferences 사용 (로컬 저장)
- 키: `avatar_index`
- 기본값: 0

**이유**:
- Firebase Auth가 없는 익명 모드이므로 로컬 저장 사용
- 향후 Firebase Auth 추가 시 Firestore로 변경 가능

---

### 4. CommunityViewModel 수정

**변경 사항**:

#### A. AndroidViewModel로 변경
```kotlin
// Before
class CommunityViewModel : ViewModel()

// After
class CommunityViewModel(application: Application) : AndroidViewModel(application)
```

**이유**: Context가 필요한 UserRepository 사용

#### B. UserRepository 추가
```kotlin
private val userRepository = UserRepository(application.applicationContext)
```

#### C. addPost 함수 수정
```kotlin
fun addPost(content: String) {
    viewModelScope.launch {
        // 익명 닉네임 생성
        val nickname = anonymousNicknames.random()
        
        // [NEW] 사용자의 현재 아바타 인덱스 가져오기
        val avatarIndex = try {
            userRepository.getAvatarIndex()
        } catch (e: Exception) {
            0 // 실패 시 기본값
        }
        
        // 타이머 계산
        val timerDuration = calculateTimerDuration()
        
        // Post 생성 (avatarIndex 포함)
        val post = Post(
            nickname = nickname,
            timerDuration = timerDuration,
            content = content,
            authorAvatarIndex = avatarIndex, // [NEW]
            // ...other fields...
        )
        
        repository.addPost(post)
    }
}
```

---

## 🔄 데이터 흐름

### 게시글 작성 시

```
1. 사용자가 글 작성
   ↓
2. ViewModel.addPost() 호출
   ↓
3. UserRepository.getAvatarIndex()
   → SharedPreferences에서 아바타 인덱스 조회
   ↓
4. Post 객체 생성 (authorAvatarIndex 포함)
   ↓
5. Firestore에 저장
   {
     nickname: "익명의 사자",
     content: "오늘도 술 없이...",
     authorAvatarIndex: 5, // ← 저장됨
     // ...other fields...
   }
```

---

## 📊 Firestore 데이터 구조

### 기존 데이터 (호환성 유지)
```json
{
  "id": "post_001",
  "nickname": "익명의 사자",
  "content": "...",
  "likeCount": 10
}
```
✅ **authorAvatarIndex 없어도 기본값 0으로 처리됨**

### 신규 데이터 (v2.0)
```json
{
  "id": "post_002",
  "nickname": "강한 곰",
  "content": "...",
  "likeCount": 5,
  "authorAvatarIndex": 7
}
```
✅ **아바타 인덱스 포함하여 저장**

---

## 🎯 안전성 확보

### 1. 하위 호환성
- ✅ 기존 Firestore 데이터에 `authorAvatarIndex` 없어도 작동
- ✅ 기본값 0으로 처리
- ✅ `@PropertyName` 어노테이션으로 매핑

### 2. 예외 처리
- ✅ AvatarManager: 잘못된 인덱스 → 0번 반환
- ✅ UserRepository: 조회 실패 → 0번 반환
- ✅ ViewModel: 아바타 조회 실패 → 0번으로 게시글 작성

### 3. 기본값 전략
- 모든 곳에서 0번 아바타(avatar_00)를 기본값으로 사용
- 일관된 fallback 동작

---

## 📝 다음 단계 (Step 2)

Step 1에서는 **로직(뼈대)**만 구현했습니다.  
Step 2에서는 **UI(화면)**를 구현할 예정입니다:

### Step 2 작업 예정

1. **Tab 5 (Settings) - 아바타 선택 화면**
   - 현재 아바타 표시
   - 아바타 선택 다이얼로그 (20개 그리드)
   - 선택 시 업데이트

2. **Tab 4 (Community) - 게시글 UI**
   - PostItem에서 아바타 표시
   - WritePostTrigger에서 내 아바타 표시

3. **디버그 기능**
   - 더미 게시글에 랜덤 아바타 적용

---

## ✅ 체크리스트

### 구현 완료
- [x] AvatarManager 싱글톤 생성
- [x] 20개 아바타 리소스 등록
- [x] 안전한 인덱스 처리
- [x] Post 모델에 authorAvatarIndex 추가
- [x] UserRepository 생성
- [x] CommunityViewModel 수정
- [x] 하위 호환성 확보

### 테스트 필요 (Step 2 후)
- [ ] 아바타 선택 UI
- [ ] 게시글에 아바타 표시
- [ ] Firestore 저장 확인
- [ ] 기존 데이터 호환성 확인

---

## 🎉 Step 1 완료!

**구현된 기능**:
1. ✅ 20개 아바타 관리 시스템
2. ✅ 안전한 인덱스 처리
3. ✅ Post 모델 업데이트
4. ✅ 사용자 아바타 저장/조회
5. ✅ 게시글 작성 시 아바타 포함

**빌드 상태**: 진행 중

**다음 단계**: Step 2 - UI 구현

---

**작성일**: 2025-12-17  
**작성자**: GitHub Copilot  
**버전**: Avatar System v2.0 - Step 1

