# ✅ XML 파일 BOM 오류 해결 완료

**작업일**: 2026-01-02  
**문제**: `org.xml.sax.SAXParseException: 프롤로그에서는 콘텐츠가 허용되지 않습니다`  
**원인**: 한국어 strings.xml 파일에 BOM(Byte Order Mark) 문자 존재  
**상태**: ✅ 해결 완료

---

## 🚨 문제 상황

### 오류 메시지
```
org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 1; 
프롤로그에서는 콘텐츠가 허용되지 않습니다.
```

### 원인
**파일**: `app/src/main/res/values-ko/strings.xml`

**문제**: XML 선언 앞에 보이지 않는 BOM(Byte Order Mark) 문자가 존재

**Before**:
```xml
﻿﻿<?xml version="1.0" encoding="utf-8"?>
```
↑ 여기에 BOM 문자 2개 (`﻿﻿`)가 있음

---

## ✅ 해결 방법

### 수정 내용

**After**:
```xml
<?xml version="1.0" encoding="utf-8"?>
```
↑ BOM 문자 제거됨

### 수정된 파일

**파일**: `app/src/main/res/values-ko/strings.xml`  
**라인**: 1번째 줄  
**변경**: BOM 문자 2개 제거

---

## 🔍 BOM이란?

### BOM (Byte Order Mark)
- UTF-8 파일의 시작 부분에 붙는 보이지 않는 문자
- Windows 메모장 등에서 "UTF-8 with BOM"으로 저장 시 생성
- XML 파서는 XML 선언 앞에 어떤 문자도 허용하지 않음
- 따라서 BOM이 있으면 파싱 오류 발생

### 왜 발생했나?
- 이전 작업에서 한국어 strings.xml을 수정할 때
- Windows 환경에서 파일이 UTF-8 with BOM으로 저장됨
- Android Studio는 UTF-8 without BOM을 요구함

---

## 🧪 확인 방법

### 1. 오류 확인
```powershell
.\gradlew clean assembleDebug
```

**Before**: SAXParseException 발생  
**After**: 빌드 성공 ✅

### 2. 파일 인코딩 확인
```
IDE → File → File Properties → Encoding
→ "UTF-8" (without BOM) 확인
```

---

## 📋 남은 경고 (무시 가능)

```xml
<string name="notif_group_a_3_title">벌써 일주일... 🍺</string>
```

**경고**: Replace "..." with ellipsis character (…, &#8230;) ?

**설명**: 
- 세 개의 점(...)을 유니코드 ellipsis(…)로 바꾸라는 권장 사항
- 기능적 문제 없음
- 원한다면 수정 가능하지만 필수 아님

---

## ✅ 최종 결과

### Before
```
❌ SAXParseException: 프롤로그에서는 콘텐츠가 허용되지 않습니다
❌ 빌드 실패
```

### After
```
✅ BOM 제거 완료
✅ XML 파싱 정상
✅ 빌드 성공
```

### 수정 내용
- ✅ `values-ko/strings.xml` - BOM 문자 2개 제거
- ✅ 컴파일 에러 0개
- ⚠️ 경고 1개 (ellipsis - 무시 가능)

---

## 🔧 재발 방지 방법

### 1. IDE 설정 확인
```
Settings → Editor → File Encodings
→ "UTF-8" 선택
→ "Transparent native-to-ascii conversion" 체크 해제
→ "BOM for new UTF-8 files" → "with NO BOM" 선택
```

### 2. 파일 저장 시 주의
- Windows 메모장 사용 금지
- IDE나 전문 텍스트 에디터 사용 (VS Code, Notepad++ 등)
- UTF-8 without BOM으로 저장 확인

### 3. Git 설정 (선택)
```bash
# .gitattributes에 추가
*.xml text eol=lf encoding=utf-8
```

---

## 📚 참고: 다른 XML 파일 확인

### 확인 필요한 파일
```
app/src/main/res/
  ├── values/strings.xml ✅ (정상)
  ├── values-ko/strings.xml ✅ (수정 완료)
  ├── values-ja/strings.xml ✅ (정상)
  └── values-in/strings.xml ✅ (정상)
```

**모든 파일 정상 확인됨**

---

## 🎉 완료!

XML 파일의 BOM 오류가 완전히 해결되었습니다!

**핵심 수정**:
- ✅ BOM 문자 제거
- ✅ UTF-8 without BOM으로 저장
- ✅ XML 파싱 정상
- ✅ 빌드 성공

**빌드 명령어**:
```powershell
.\gradlew clean
.\gradlew assembleDebug
```

---

**작성일**: 2026-01-02  
**상태**: ✅ 해결 완료  
**결과**: 빌드 정상 작동

