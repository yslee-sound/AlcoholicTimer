# ✅ Supabase 설정 완료!

## 🎉 설정된 내용

### 1. API 정보 저장 (`local.properties`)
```properties
supabase.url=https://bajurdtglfaiqilnpamt.supabase.co
supabase.key=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 2. Gradle 빌드 스크립트 업데이트
- `local.properties` 읽기 로직 추가
- `BuildConfig.SUPABASE_URL` 및 `BuildConfig.SUPABASE_KEY` 자동 생성
- 우선순위: local.properties → 환경변수 → 기본값

### 3. SupabaseProvider 연동
- `BuildConfig`에서 자동으로 API 정보 로드
- 별도 설정 불필요

## 🧪 테스트 방법

### 1. 빌드 확인
```cmd
cd /d "g:\Workspace\AlcoholicTimer"
gradlew.bat :app:assembleDebug
```

### 2. 앱 실행
1. 디버그 APK 설치 또는 Android Studio에서 실행
2. 디버그 메뉴 진입
3. "공지사항 (Supabase 연동)" 카드 확인
4. 정책 로딩이 자동으로 시작됨

### 3. 확인 사항
- ✅ 로딩 인디케이터 표시
- ✅ 정책 데이터 표시 (있는 경우)
- ✅ "정책 없음" 메시지 (테이블이 비어있는 경우)
- ❌ 오류 메시지 (네트워크 문제 등)

## 📊 다음 단계: Supabase 테이블 생성

### 1. Supabase 대시보드 접속
https://supabase.com/dashboard/project/bajurdtglfaiqilnpamt

### 2. SQL Editor에서 테이블 생성

**emergency_policies 테이블:**
```sql
CREATE TABLE emergency_policies (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  is_active BOOLEAN NOT NULL DEFAULT true,
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  new_app_name TEXT,
  new_app_package TEXT,
  button_text TEXT NOT NULL,
  support_url TEXT,
  support_button_text TEXT,
  can_migrate_data BOOLEAN DEFAULT false,
  is_dismissible BOOLEAN DEFAULT false,
  badge_text TEXT,
  migration_message TEXT,
  priority INTEGER DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- RLS (Row Level Security) 활성화
ALTER TABLE emergency_policies ENABLE ROW LEVEL SECURITY;

-- 읽기 권한 (모두 허용)
CREATE POLICY "Allow public read access" ON emergency_policies
  FOR SELECT USING (true);
```

**notice_policies 테이블:**
```sql
CREATE TABLE notice_policies (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  is_active BOOLEAN NOT NULL DEFAULT true,
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  button_text TEXT NOT NULL,
  action_url TEXT,
  priority INTEGER DEFAULT 0,
  show_once BOOLEAN DEFAULT false,
  target_version_min TEXT,
  target_version_max TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  expires_at TIMESTAMP WITH TIME ZONE
);

ALTER TABLE notice_policies ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public read access" ON notice_policies
  FOR SELECT USING (true);
```

**update_policies 테이블:**
```sql
CREATE TABLE update_policies (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  is_active BOOLEAN NOT NULL DEFAULT true,
  version TEXT NOT NULL,
  version_code INTEGER NOT NULL,
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  update_button_text TEXT NOT NULL,
  later_button_text TEXT,
  features TEXT[] DEFAULT '{}',
  is_force_update BOOLEAN DEFAULT false,
  target_version_min TEXT,
  target_version_max TEXT,
  store_url TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

ALTER TABLE update_policies ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public read access" ON update_policies
  FOR SELECT USING (true);
```

### 3. 테스트 데이터 추가

**긴급 공지 예시:**
```sql
INSERT INTO emergency_policies (
  title, description, button_text, priority, is_dismissible
) VALUES (
  '테스트 긴급 공지',
  '이것은 테스트 긴급 공지입니다. 앱에서 정상적으로 표시되는지 확인해주세요.',
  '확인',
  100,
  true
);
```

**일반 공지 예시:**
```sql
INSERT INTO notice_policies (
  title, description, button_text, priority, show_once
) VALUES (
  '테스트 일반 공지',
  '이것은 테스트 일반 공지사항입니다.',
  '확인했습니다',
  50,
  false
);
```

**업데이트 공지 예시:**
```sql
INSERT INTO update_policies (
  version, version_code, title, description,
  update_button_text, later_button_text, is_force_update,
  features
) VALUES (
  '2.0.0',
  2000000,
  '새 버전 사용 가능',
  '더 나은 경험을 위해 최신 버전으로 업데이트해주세요.',
  '지금 업데이트',
  '나중에',
  false,
  ARRAY['새로운 기능 추가', 'UI 개선', '버그 수정']
);
```

## 🔍 문제 해결

### 빌드 오류
- Gradle Sync: `gradlew.bat --refresh-dependencies`
- Clean Build: `gradlew.bat clean :app:assembleDebug`

### "정책 없음" 표시
- Supabase 테이블 생성 확인
- 테이블에 데이터 추가
- `is_active = true` 확인

### 네트워크 오류
- 인터넷 연결 확인
- Supabase URL 확인
- RLS 정책 확인 (읽기 허용)

## 📱 앱에서 확인

1. **디버그 메뉴** → **공지사항 (Supabase 연동)**
2. 정책 로딩 완료 확인
3. 각 정책 토글로 팝업 미리보기
4. "우선순위대로 팝업 표시" 버튼 테스트

## ✨ 완료!

이제 Supabase와 완전히 연동되었습니다!
- ✅ API URL 및 Key 설정 완료
- ✅ BuildConfig 자동 생성
- ✅ DebugActivity 연동 완료
- 🔜 Supabase 테이블 생성 및 테스트 데이터 추가

**참고 문서:**
- `docs/SUPABASE_API_SETUP.md` - API 설정 상세 가이드
- `docs/SUPABASE_SETUP_CHECKLIST.md` - 전체 체크리스트
- `docs/SUPABASE_POPUP_INTEGRATION_GUIDE.md` - 통합 가이드

