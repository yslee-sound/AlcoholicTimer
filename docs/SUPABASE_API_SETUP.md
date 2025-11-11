# 🔑 Supabase API 설정 가이드

## 빠른 설정 (3단계)

### 1️⃣ Supabase 프로젝트에서 API 정보 가져오기

1. https://supabase.com 접속 및 로그인
2. 프로젝트 선택 (없으면 **New Project** 생성)
3. 왼쪽 메뉴 **Settings** ⚙️ 클릭
4. **API** 탭 선택
5. 다음 두 가지 정보 복사:
   - **Project URL**: `https://xxxxxxxxxxxxx.supabase.co`
   - **anon public key**: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` (매우 긴 문자열)

### 2️⃣ local.properties에 설정 (권장 방법)

프로젝트 루트 폴더 `g:\Workspace\AlcoholicTimer\`에 있는 `local.properties` 파일을 열고 다음 추가:

```properties
# Supabase 설정
supabase.url=https://xxxxxxxxxxxxx.supabase.co
supabase.key=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**✅ 이 파일은 `.gitignore`에 포함되어 Git에 업로드되지 않으니 안전합니다!**

### 3️⃣ app/build.gradle.kts 수정 (선택사항)

더 안전하게 하려면 `app/build.gradle.kts` 파일 상단에 다음 코드를 추가:

```kotlin
// 파일 맨 위에 추가
import java.util.Properties

// local.properties 읽기
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

plugins {
    // ... 기존 코드
}

// ...기존 코드...

android {
    // ...기존 설정...
    
    defaultConfig {
        // ...기존 설정...
        
        // Supabase 설정 수정 (local.properties 우선)
        val supabaseUrl = localProperties.getProperty("supabase.url") 
            ?: System.getenv("SUPABASE_URL") 
            ?: "https://your-project.supabase.co"
        val supabaseKey = localProperties.getProperty("supabase.key") 
            ?: System.getenv("SUPABASE_KEY") 
            ?: "your-anon-key"
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
    }
}
```

## 🧪 설정 확인

1. Android Studio에서 **Build** → **Clean Project**
2. **Build** → **Rebuild Project**
3. 앱 실행 (디버그 모드)
4. 디버그 메뉴 → "공지사항 (Supabase 연동)" 확인
5. 정책 로딩이 성공하면 ✅ 설정 완료!

## 🔄 대체 방법: 환경변수 사용

Windows에서 시스템 환경변수로 설정하려면:

**PowerShell (관리자 권한)**:
```powershell
[System.Environment]::SetEnvironmentVariable("SUPABASE_URL", "https://xxxxxxxxxxxxx.supabase.co", "User")
[System.Environment]::SetEnvironmentVariable("SUPABASE_KEY", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", "User")
```

설정 후 **IDE 재시작** 필요!

## ⚠️ 주의사항

1. **anon public key를 사용하세요** (`service_role` key는 서버 전용!)
2. `local.properties`는 절대 Git에 커밋하지 마세요
3. 공개 저장소에서 작업 시 더욱 주의하세요
4. 테이블에 RLS (Row Level Security) 정책 설정 권장

## 🛡️ 보안 체크리스트

- [ ] `local.properties`가 `.gitignore`에 포함되어 있는지 확인
- [ ] GitHub/GitLab 등에 API Key가 노출되지 않았는지 확인
- [ ] Supabase에서 RLS 정책 활성화
- [ ] `anon` key 사용 (절대 `service_role` key 사용 금지)

## 📚 더 알아보기

- **Supabase 인증**: https://supabase.com/docs/guides/auth
- **RLS 정책**: https://supabase.com/docs/guides/auth/row-level-security
- **API Key 관리**: https://supabase.com/docs/guides/api/api-keys

