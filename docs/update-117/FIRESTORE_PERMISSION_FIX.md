# Firestore 권한 에러 해결 가이드

## 🚨 문제 진단

**에러 메시지:**
```
PERMISSION_DENIED: Missing or insufficient permissions.
```

**원인:** Firestore Security Rules가 `posts` 컬렉션에 대한 쓰기 권한을 차단하고 있습니다.

---

## ✅ 해결 방법

### 1단계: Firebase Console 접속

1. [Firebase Console](https://console.firebase.google.com/) 접속
2. 프로젝트 선택 (AlcoholicTimer)
3. 좌측 메뉴에서 **"Firestore Database"** 클릭
4. 상단 탭에서 **"규칙(Rules)"** 클릭

---

### 2단계: Security Rules 수정

**현재 규칙 (차단됨):**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if false; // 모든 쓰기 차단
    }
  }
}
```

**수정 필요 규칙 (테스트용):**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // posts 컬렉션: 인증된 사용자만 읽기/쓰기 가능
    match /posts/{postId} {
      allow read: if true; // 모든 사용자 읽기 가능 (익명 피드)
      allow create: if request.auth != null; // 인증된 사용자만 생성
      allow update: if request.auth != null; // 인증된 사용자만 수정 (좋아요)
      allow delete: if request.auth != null; // 인증된 사용자만 삭제
    }
    
    // 기존 컬렉션들 (records, diaries 등)은 그대로 유지
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

**Phase 2 테스트용 (임시, 개발 단계만):**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // [임시] posts 컬렉션: 테스트를 위해 모든 권한 허용
    match /posts/{postId} {
      allow read, write: if true;
    }
    
    // 기존 컬렉션들
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

⚠️ **주의:** `allow read, write: if true;`는 보안상 위험하므로 **개발/테스트 단계에만** 사용하세요.

---

### 3단계: 규칙 게시

1. 코드 수정 후 우측 상단 **"게시(Publish)"** 버튼 클릭
2. 확인 팝업에서 **"게시"** 클릭
3. "규칙이 게시되었습니다" 메시지 확인

---

### 4단계: 앱에서 재테스트

1. 앱 실행
2. Tab 5 → Debug 메뉴
3. "📝 테스트 게시글 10개 생성" 버튼 클릭
4. Toast: "✅ 테스트 게시글 10개 생성 완료!" 확인
5. Tab 4 → 게시글 10개 표시 확인

---

## 🔒 Phase 3 배포 시 권장 규칙

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // posts 컬렉션: 익명 커뮤니티
    match /posts/{postId} {
      // 읽기: 모든 사용자 허용 (익명 피드)
      allow read: if true;
      
      // 생성: 인증된 사용자 + 24시간 이내 삭제 예정
      allow create: if request.auth != null
        && request.resource.data.deleteAt is timestamp
        && request.resource.data.deleteAt > request.time;
      
      // 수정: 좋아요 카운트만 증가 가능
      allow update: if request.auth != null
        && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['likeCount'])
        && request.resource.data.likeCount == resource.data.likeCount + 1;
      
      // 삭제: 작성자 또는 24시간 경과 시
      allow delete: if request.auth != null
        && (resource.data.userId == request.auth.uid 
            || request.time > resource.data.deleteAt);
    }
    
    // 기존 records, diaries 등
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## 🧪 테스트 체크리스트

- [ ] Firebase Console에서 Security Rules 수정
- [ ] "게시(Publish)" 버튼 클릭
- [ ] 앱에서 게시글 생성 버튼 클릭
- [ ] Toast: "✅ 테스트 게시글 10개 생성 완료!" 확인
- [ ] Tab 4에서 게시글 10개 표시 확인
- [ ] 좋아요 버튼 클릭 시 숫자 증가 확인

---

## 📚 참고 자료

- [Firestore Security Rules 문서](https://firebase.google.com/docs/firestore/security/get-started)
- [Firebase Console](https://console.firebase.google.com/)

---

**작성일**: 2025-12-17  
**작성자**: GitHub Copilot

