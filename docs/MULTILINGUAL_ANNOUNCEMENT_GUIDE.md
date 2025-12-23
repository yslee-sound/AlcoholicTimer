# 공지사항 다국어 데이터베이스 구조

## 📋 Supabase notice_policy 테이블 구조

```sql
-- 기존 컬럼 유지 (하위 호환성)
ALTER TABLE notice_policy 
ADD COLUMN IF NOT EXISTS title TEXT,
ADD COLUMN IF NOT EXISTS content TEXT;

-- 다국어 컬럼 추가
ALTER TABLE notice_policy 
ADD COLUMN IF NOT EXISTS title_ko TEXT DEFAULT '',
ADD COLUMN IF NOT EXISTS content_ko TEXT DEFAULT '',
ADD COLUMN IF NOT EXISTS title_en TEXT DEFAULT '',
ADD COLUMN IF NOT EXISTS content_en TEXT DEFAULT '',
ADD COLUMN IF NOT EXISTS title_ja TEXT DEFAULT '',
ADD COLUMN IF NOT EXISTS content_ja TEXT DEFAULT '',
ADD COLUMN IF NOT EXISTS title_id TEXT DEFAULT '',
ADD COLUMN IF NOT EXISTS content_id TEXT DEFAULT '';

-- created_at은 이미 존재 (자동 생성됨)
-- 형식: "2025-12-23T10:30:00Z"
```

## 📋 Firestore app_notices 컬렉션 구조

```json
{
  "id": "notice_001",
  
  // [DEPRECATED] 하위 호환성을 위해 유지
  "title": "Important Update",
  "content": "Please update your app...",
  
  // [NEW] 다국어 필드
  "title_ko": "중요 업데이트",
  "content_ko": "앱을 업데이트해 주세요...",
  
  "title_en": "Important Update",
  "content_en": "Please update your app...",
  
  "title_ja": "重要なアップデート",
  "content_ja": "アプリを更新してください...",
  
  "title_id": "Pembaruan Penting",
  "content_id": "Silakan perbarui aplikasi Anda...",
  
  // [NEW] Firebase Timestamp (자동 생성)
  // Date 타입으로 저장되며, displayDate로 "2025.12.23" 형식 출력
  "timestamp": "2025-12-23T10:00:00Z",
  "isRead": false,
  "type": "NOTICE"
}
```

## 🌍 지원 언어

| 언어 코드 | 언어명 | 필드 접미사 |
|----------|--------|-----------|
| ko | 한국어 | `_ko` |
| en | English (기본) | `_en` |
| ja | 日本語 | `_ja` |
| id/in | Bahasa Indonesia | `_id` |

## 📱 앱 동작 방식

1. **데이터 로드**: Firebase/Supabase에서 모든 언어 필드를 포함한 데이터를 가져옴
2. **자동 언어 선택**: 
   - 사용자의 시스템 언어(Locale)를 확인
   - `displayTitle`과 `displayContent` 속성이 자동으로 적절한 언어 선택
3. **폴백(Fallback) 처리**:
   - 1순위: 사용자 언어의 텍스트
   - 2순위: 영어(en) 텍스트
   - 3순위: 레거시 `title`/`content` 필드

## 🔧 사용 예시

### Kotlin 코드에서 사용

```kotlin
// 자동으로 시스템 언어에 맞는 텍스트 표시
Text(text = announcement.displayTitle)
Text(text = announcement.displayContent)

// [NEW] 날짜 포맷 자동 변환 (2025-12-23)
Text(text = announcement.displayDate) // "2025.12.23" 형식

// 특정 언어 접근 (필요한 경우)
Text(text = announcement.title_ko)
Text(text = announcement.content_ja)
```

### NotificationItem 사용 예시

```kotlin
// Firestore Date 타입 자동 변환
Text(text = notification.displayTitle)
Text(text = notification.displayContent)
Text(text = notification.displayDate) // timestamp(Date?) -> "2025.12.23"
```

### Firebase Console에서 데이터 입력

```
title_ko: "새로운 기능이 추가되었습니다"
content_ko: "이제 다국어를 지원합니다..."

title_en: "New features added"
content_en: "We now support multiple languages..."

title_ja: "新機能が追加されました"
content_ja: "多言語をサポートするようになりました..."

title_id: "Fitur baru ditambahkan"
content_id: "Kami sekarang mendukung banyak bahasa..."
```

## ✅ 하위 호환성

- 기존 `title`/`content` 필드는 유지됨
- 다국어 필드가 비어있으면 자동으로 레거시 필드 사용
- 기존 데이터는 수정 없이 계속 작동함

## 🚀 마이그레이션 가이드

1. **기존 데이터 유지**: title/content 필드는 그대로 둠
2. **새 공지사항**: 4개 언어 모두 입력
3. **점진적 업데이트**: 기존 공지사항도 시간이 날 때 다국어 추가

---

**작성일**: 2025-12-23
**버전**: 1.1.6

